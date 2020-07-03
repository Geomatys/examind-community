angular.module("examind.components.dataset.explorer")
    .directive("datasetExplorer", datasetExplorer)
    .controller("DatasetExplorerController", DatasetExplorerController);

function datasetExplorer() {
    var self = {};

    self.restrict = "EA";

    self.scope = {
        dataset: "=dataset",
        selected: "=selected",
        onSelect: "&onSelect"
    };

    self.templateUrl = "components/dataset/dataset-explorer/dataset-explorer.html";

    self.controller = "DatasetExplorerController";
    self.controllerAs = "$ctrl";

    return self;
}

/**
 *
 * @param $scope
 * @param $element
 * @param $timeout
 * @param {Examind} Examind
 * @param {DatasetExplorerStyle} DatasetExplorerStyle
 * @constructor
 */
function DatasetExplorerController($scope, $rootScope, $element, $timeout, $filter, Examind, DatasetExplorerStyle) {
    var self = this;

    var _updater = null, _loader = null, _error = null;

    var _dataset = null,
        _hover = null,
        _filtered = [],
        _stats = {},
        _filters = {};

    var _olView = new ol.View({
        center: [0, 0],
        zoom: 1,
        projection: "EPSG:3857"
    });

    var _featuresCollection = new ol.Collection();

    var _vectorLayer = new ol.layer.Vector({
        source: new ol.source.Vector({
            source: _featuresCollection
        }),
        style: function (feature, resolution) {
            var data = feature.get("data");
            if (angular.isObject(data) && angular.isNumber(data.id)) {
                var isVisible = self.filtered.reduce(function (accumulator, element) {
                    return (accumulator || data.id === element.id);
                }, false);

                if (self.filtered.length <= 0 || !isVisible) {
                    return null;
                }

                if (self.selected) {
                    var find = self.selected.find(function (item) {
                        return item.id === data.id;
                    });

                    if (find && find.id === data.id) {
                        return DatasetExplorerStyle.styleFunction.selected(feature, resolution);
                    } else if (angular.isObject(self.hover) && self.hover.id === data.id) {
                        return DatasetExplorerStyle.styleFunction.hover(feature, resolution);
                    }
                }

                return DatasetExplorerStyle.styleFunction.default(feature, resolution);
            }

            // Not a valid feature for this component
            return null;
        }
    });

    var _interactionSelect = new ol.interaction.Select({
        condition: ol.events.condition.pointerMove,
        layers: [_vectorLayer],
        style: DatasetExplorerStyle.styleFunction.hover
    });

    var _olMap = new ol.Map({
        view: _olView,
        layers: [
            new ol.layer.Tile({
                source: new ol.source.OSM()
            }),
            _vectorLayer
        ],
        target: $element.find(".ol-container")[0]
    });

    _olMap.addInteraction(_interactionSelect);

    $scope.selected = [];

    self.selected = $scope.selected;

    Object.defineProperties(self, {
        dataset: {
            enumerable: true,
            get: function () {
                return _dataset;
            }
        },
        data: {
            enumerable: true,
            get: function () {
                return (angular.isObject(_dataset) && angular.isArray(_dataset.data)) ? _dataset.data : [];
            }
        },
        hover: {
            enumerable: true,
            get: function () {
                return _hover;
            },
            set: function (hover) {
                if (hover !== _hover) {
                    _hover = hover;
                    _vectorLayer.changed();
                }
                return _hover;
            }
        },
        filtered: {
            enumerable: true,
            get: function () {
                return _filtered;
            },
            set: function (filtered) {
                _filtered.splice.bind(_filtered, 0, _filtered.length).apply(_filtered, [].concat(filtered));
                return _filtered;
            }
        },
        isLoading: {
            enumerable: true,
            get: function () {
                return _loader !== null;
            }
        },
        error: {
            enumerable: true,
            get: function () {
                return _error;
            }
        },
        stats: {
            enumerable: true,
            get: function () {
                return _stats;
            }
        }
    });

    function computeFiltersFor(name) {
        var stats = getStatsFor(name);
        return (stats.filters = {
            min: stats.min,
            max: stats.max
        });
    }

    function getStatsFor(name, initial_values) {
        if (angular.isObject(self.stats[name])) {
            return self.stats[name];
        }
        self.stats[name] = {
            min: initial_values.min || Number.POSITIVE_INFINITY,
            max: initial_values.max || Number.NEGATIVE_INFINITY
        };

        return self.stats[name];
    }

    function _updateFiltered() {
        if (_updater !== null) {
            $timeout.cancel(_updater);
        }
        _updater = $timeout(function () {
            var extent = _olView.calculateExtent(_olMap.getSize());

            self.filtered = _vectorLayer.getSource().getFeaturesInExtent(extent)
                .map(function (feature) {
                    return feature.get("data");
                })
                .filter(function (datum) {
                    return angular.isObject(datum) && angular.isNumber(datum.id);
                })
                .filter(function (datum) {
                    // filter by dimensions
                    var isFiltered = false;

                    var names = Object.keys(datum.dimensions);
                    for (var i = 0, iMax = names.length; i < iMax && !isFiltered; i++) {
                        var name = names[i];
                        var dimension = datum.dimensions[name];
                        var filter = self.stats[name].filters;
                        isFiltered = filter.max < dimension.stats.min || dimension.stats.max < filter.min;
                    }
                    return !isFiltered;
                }).sort(function (a, b) {
                    return a.name < b.name ? -1 : 1;
                });
        }, 0);

        _updater.finally(function () {
            _updater = null;
            _vectorLayer.changed();
        });

        return _updater;
    }

    function _select(data, evt) {
        var _data = data;
        if (_data instanceof ol.Feature) {
            _data = _data.get("data");
        }

        if (angular.isObject(_data) && angular.isNumber(_data.id)) {
            var findIndex = self.selected.findIndex(function (item) {
                return item.id === _data.id;
            });

            if (findIndex === -1) {
                Examind.datas.getData(_data.id).then(function (response) {
                    if(!evt.ctrlKey){
                        self.selected.splice(0);
                    }
                    self.selected.push(angular.extend(_data, response.data));

                    if (angular.isDefined($scope.onSelect)) {
                        var selectFunc = $scope.onSelect($scope);

                        if (angular.isFunction(selectFunc)) {
                            selectFunc(_data);
                        }
                    }

                    _vectorLayer.changed();
                });
            } else {
                self.selected.splice(findIndex, 1);
                //refresh the map
                $rootScope.$broadcast("examind:dashboard:data:refresh:map");
            }
        }
    }

    function computeStatsOf(dimension, isGlobal) {
        var stats = {}, global;
        switch (dimension.type) {
            case "date" :
                dimension.values = dimension.values.sort();
                stats.min = dimension.values[0];
                stats.max = dimension.values[dimension.values.length - 1];

                if (isGlobal === true) {
                    global = getStatsFor(dimension.name, stats);
                    angular.extend(global, {
                        type: dimension.type,
                        min: Math.min(stats.min, global.min),
                        max: Math.max(stats.max, global.max)
                    });
                }

                break;
            case "number" :
                dimension.values = dimension.values.sort();
                stats.min = dimension.values[0];
                stats.max = dimension.values[result.values.length - 1];

                // Heavy and not very usefull
                //stats.avg = dimension.values.reduce(function(accumulator, element) {
                //    return accumulator + element;
                //}, 0);
                //stats.avg = stats.avg / values.length;

                if (isGlobal === true) {
                    global = getStatsFor(dimension.name, stats);
                    angular.extend(global, {
                        type: dimension.type,
                        min: Math.min(stats.min, global.min),
                        max: Math.max(stats.max, global.max)
                    });
                }

                break;
        }

        return stats;
    }

    function extractValuesOf(dimension) {
        var result = {
            name: dimension.name.toLowerCase(),
            values: [],
            type: null,
            stats: {
                min: Number.POSITIVE_INFINITY,
                max: Number.NEGATIVE_INFINITY,
                avg: Number.NaN
            }
        };

        switch (result.name) {
            case "time" :
                result.type = "date";
                result.values = dimension.value.split(",")
                    .reduce(function (accumulator, value) {
                        var periods = value.split("/");
                        if (periods.length === 3) {
                            var start = moment.utc(periods[0]);
                            var end = moment.utc(periods[1]);
                            var duration = moment.duration(periods[2]);
                            do {
                                accumulator.push(start.valueOf());
                            } while (start.add(duration) <= end);
                        } else {
                            accumulator.push(moment.utc(value).valueOf());
                        }

                        return accumulator;
                    }, [])
                    .filter(function (candidate) {
                        return angular.isNumber(candidate) && !Number.isNaN(candidate);
                    });
                break;
            default :
                result.type = "enum";
                result.values = dimension.value.split(",");
                break;
        }

        return result;
    }

    self.select = _select;

    self.isSelectedData = function (data) {
        if (self.selected && self.selected.length > 0) {
            var find = self.selected.find(function (item) {
                return item.id === data.id;
            });
            return find;
        } else {
            return false;
        }
    };

    self.translate = {
        date: function (value) {
            return moment.utc(value).toISOString();
        },
        number: function (value) {
            return $filter('number')(value);
        }
    };

    self.filterChange = function () {
        _updateFiltered();
    };

    _olMap.on("moveend", _updateFiltered);
    _olMap.on("singleclick", function (evt) {
        if (_interactionSelect.getFeatures().getLength() > 0) {
            _select(_interactionSelect.getFeatures().item(0));
        } else {
            self.selected = [];
        }
    });

    _interactionSelect.on("select", function (evt) {
        if (evt.selected.length > 0) {
            var selected = evt.selected[0].get("data");
            if (angular.isObject(selected) && angular.isNumber(selected.id)) {
                $scope.$apply(function () {
                    self.hover = selected;
                });
            }
        }

        if (evt.deselected.length > 0 && angular.isObject(self.hover)) {
            var deselected = evt.deselected[0].get("data");
            if (angular.isObject(deselected) && deselected.id === self.hover.id) {
                $scope.$apply(function () {
                    self.hover = null;
                });
            }
        }
    });

    $scope.$watch(function () {
        return $scope.dataset;
    }, function (newValue, oldValue) {
        _dataset = newValue;
        _error = null;

        if (angular.isObject(_dataset) && angular.isNumber(_dataset.id)) {
            _loader = Examind.datas.getDatasetDataSummary(_dataset.id).then(function (response) {
                _dataset.data = response.data;

                _featuresCollection.clear();
                var features = _dataset.data
                    .filter(function (datum) {
                        return angular.isObject(datum.dataDescription) && angular.isArray(datum.dataDescription.boundingBox);
                    })
                    .map(function (datum) {
                        // parse dimensions and extract stats.
                        if (angular.isArray(datum.dimensions)) {
                            datum.dimensions = datum.dimensions
                                .map(function (dimension) {
                                    return angular.extend(dimension, extractValuesOf(dimension))
                                })
                                .filter(function (candidate) {
                                    return angular.isDefined(candidate) && angular.isArray(candidate.values) && candidate.values.length > 0;
                                })
                                .map(function (dimension) {
                                    dimension.stats = computeStatsOf(dimension, true);
                                    return dimension;
                                })
                                .reduce(function (accumulator, dimension, index, array) {
                                    accumulator[dimension.name] = dimension;

                                    return accumulator;
                                }, {});
                        }

                        return datum;
                    })
                    .map(function (datum) {
                        var bbox = ol.extent.getIntersection(datum.dataDescription.boundingBox, ol.proj.get("CRS:84").getExtent());
                        var proj = _olView.getProjection();
                        var _bbox = ol.extent.getIntersection(ol.proj.transformExtent(bbox, "CRS:84", proj), proj.getExtent());

                        var geom = new ol.geom.Polygon([
                            [
                                [_bbox[0], _bbox[3]],
                                [_bbox[0], _bbox[1]],
                                [_bbox[2], _bbox[1]],
                                [_bbox[2], _bbox[3]],
                                [_bbox[0], _bbox[3]]
                            ]
                        ]);

//                        geom.transform("CRS:84", _olView.getProjection());

                        return new ol.Feature({
                            geometry: geom,
                            data: datum
                        });
                    });

                Object.keys(self.stats).forEach(function (element) {
                    computeFiltersFor(element);
                });

                if (features.length > 0) {
                    _vectorLayer.getSource().addFeatures(features);

                    var bbox = _vectorLayer.getSource().getExtent();
                    var width = ol.extent.getWidth(bbox);
                    _olView.fit(ol.extent.buffer(bbox, width * 0.05), _olMap.getSize());

                    _updateFiltered();

                    if (features.length === 1) {
                        _select(features[0]);
                    }
                } else {
                    //console.error("There is no valid data in this dataset", _dataset);
                    _error = {
                        msg: "dataset.explorer.error.nodata"
                    };
                }
            }).catch(function (err) {
                console.error("An error occur when try to get dataset data : ", _dataset, err);
                _error = {
                    msg: "dataset.explorer.error.getdata"
                };
            }).finally(function () {
                _loader = null;
            });
        } else {
            console.error("The given dataset is not a correct dataset", _dataset);
            _error = {
                msg: "dataset.explorer.error.invalid"
            };
        }
    });
}
