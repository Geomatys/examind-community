angular.module('examind.components.metadata.editor.bbox.modal', [])
    .controller('BboxMetadataModalController', BboxMetadataModalController);

function BboxMetadataModalController($scope, $http, $translate, $modalInstance, $timeout, Growl, block) {
    var self = this;
    self.bboxOptions = {
        enableDragBoxControl: false,
        currentBboxBlock: block,
        olVectorLayer: null,
        olStyle: [
            new ol.style.Style({
                stroke: new ol.style.Stroke({
                    color: '#66AADD',
                    width: 2.25
                })
            })
        ],
        north: 90.0,
        south: -90.0,
        east: 180.0,
        west: -180.0
    };
    self.geonames = {
        loadingGeonames: false,
        value: '',
        list: []
    };

    self.initCtrl = function () {
        //get existing coordinates values if exists.
        self.retrieveBboxValues(self.bboxOptions.currentBboxBlock);
        //show the map
        //note we use here a setTimeout because this version of uibootstrap does not have the rendered promise yet
        //$modalInstance.rendered.then
        MetadataEditorViewer.enableDragBoxControl = self.bboxOptions.enableDragBoxControl;
        setTimeout(function () {
            self.showBboxViewer();
        }, 400);
    };

    self.toggleControlDragBox = function () {
        self.bboxOptions.enableDragBoxControl = !self.bboxOptions.enableDragBoxControl;
        MetadataEditorViewer.enableDragBoxControl = self.bboxOptions.enableDragBoxControl;
    };

    self.retrieveBboxValues = function (blockObj) {
        if (blockObj && blockObj.block && blockObj.block.children) {
            var fields = blockObj.block.children;
            for (var i = 0; i < fields.length; i++) {
                if (fields[i].field && fields[i].field.name.indexOf('westBoundLongitude') !== -1 && fields[i].field.value) {
                    self.bboxOptions.west = Number(fields[i].field.value);
                } else if (fields[i].field && fields[i].field.name.indexOf('eastBoundLongitude') !== -1 && fields[i].field.value) {
                    self.bboxOptions.east = Number(fields[i].field.value);
                } else if (fields[i].field && fields[i].field.name.indexOf('northBoundLatitude') !== -1 && fields[i].field.value) {
                    self.bboxOptions.north = Number(fields[i].field.value);
                } else if (fields[i].field && fields[i].field.name.indexOf('southBoundLatitude') !== -1 && fields[i].field.value) {
                    self.bboxOptions.south = Number(fields[i].field.value);
                }
            }
        }
    };

    /**
     * Show the bbox map in the modal for metadata editor.
     */
    self.showBboxViewer = function () {
        if (MetadataEditorViewer.map) {
            MetadataEditorViewer.map.setTarget(undefined);
        }
        MetadataEditorViewer.initConfig();
        MetadataEditorViewer.enableAttributions = false;
        var bboxExists = (self.bboxOptions.north && self.bboxOptions.south && self.bboxOptions.west && self.bboxOptions.east) &&
            (self.bboxOptions.north !== 90.0 || self.bboxOptions.south !== -90.0 || self.bboxOptions.west !== -180.0 || self.bboxOptions.east !== 180.0);
        if (bboxExists) {
            var minX = self.bboxOptions.west;
            var minY = self.bboxOptions.south;
            var maxX = self.bboxOptions.east;
            var maxY = self.bboxOptions.north;
            //For pseudo Mercator we need to check against the validity,
            // the bbox crs is always defined as EPSG:4326
            //if the viewer use pseudo Mercator then fix Latitude to avoid Infinity values
            if (MetadataEditorViewer.projection === 'EPSG:3857') {
                if (minY < -85) {
                    minY = -85;
                }
                if (maxY > 85) {
                    maxY = 85;
                }
            }
            var coordinates = [[[minX, minY], [minX, maxY], [maxX, maxY], [maxX, minY], [minX, minY]]];
            var polygon = new ol.geom.Polygon(coordinates);
            polygon = polygon.transform('EPSG:4326', MetadataEditorViewer.projection);
            var extentFeature = new ol.Feature(polygon);
            MetadataEditorViewer.initMap('bboxMap');
            self.bboxOptions.olVectorLayer = new ol.layer.Vector({
                map: MetadataEditorViewer.map,
                source: new ol.source.Vector({
                    features: new ol.Collection(),
                    useSpatialIndex: false // optional, might improve performance
                }),
                style: self.bboxOptions.olStyle,
                updateWhileAnimating: true, // optional, for instant visual feedback
                updateWhileInteracting: true // optional, for instant visual feedback
            });
            self.bboxOptions.olVectorLayer.getSource().addFeature(extentFeature);
            MetadataEditorViewer.zoomToExtent([minX, minY, maxX, maxY], MetadataEditorViewer.map.getSize(), false);
        } else {
            MetadataEditorViewer.initMap('bboxMap');
            MetadataEditorViewer.map.getView().setZoom(1);
        }

        //mouse coordinates
        $('#bboxMap').find('div.ol-scale-line')
            .after($('<p style="bottom:0;position:absolute;right:5px;">' +
                '<span id="mouse4326" class="label" style="background-color:rgba(0,60,136,0.3)"></span>' +
                '</p>'));
        MetadataEditorViewer.map.on('pointermove', function (event) {
            var eventCoords = event.coordinate;
            var coord4326 = ol.proj.transform(eventCoords, MetadataEditorViewer.projection, 'EPSG:4326');
            var template = 'Coord. {x} / {y}';
            var coordStr = ol.coordinate.format(coord4326, template, 3);
            $('#mouse4326').text(coordStr);
        });

        //drag bbox control
        var dragBox = new ol.interaction.DragBox({
            condition: function () {
                return MetadataEditorViewer.enableDragBoxControl;
            }
        });
        MetadataEditorViewer.map.addInteraction(dragBox);
        var startX, startY, endX, endY;
        dragBox.on('boxstart', function (ev) {
            var eventCoords = ev.coordinate;
            startX = eventCoords[0];
            startY = eventCoords[1];
            $('#bboxMap').css("cursor", "crosshair");
        });
        dragBox.on('boxend', function (ev) {
            $scope.$apply(function () {
                var eventCoords = ev.coordinate;
                endX = eventCoords[0];
                endY = eventCoords[1];
                var minX = Math.min(startX, endX);
                var minY = Math.min(startY, endY);
                var maxX = Math.max(startX, endX);
                var maxY = Math.max(startY, endY);
                var coordinates = [[[minX, minY], [minX, maxY], [maxX, maxY], [maxX, minY], [minX, minY]]];
                var polygon = new ol.geom.Polygon(coordinates);
                var feature = new ol.Feature(polygon);

                if (!self.bboxOptions.olVectorLayer) {
                    self.bboxOptions.olVectorLayer = new ol.layer.Vector({
                        map: MetadataEditorViewer.map,
                        source: new ol.source.Vector({
                            features: new ol.Collection(),
                            useSpatialIndex: false // optional, might improve performance
                        }),
                        style: self.bboxOptions.olStyle,
                        updateWhileAnimating: true, // optional, for instant visual feedback
                        updateWhileInteracting: true // optional, for instant visual feedback
                    });
                }
                self.bboxOptions.olVectorLayer.getSource().clear();
                self.bboxOptions.olVectorLayer.getSource().addFeature(feature);
                var extent4326 = ol.proj.transformExtent([minX, minY, maxX, maxY], MetadataEditorViewer.projection, 'EPSG:4326');
                self.bboxOptions.north = Number(extent4326[3].toFixed(4));
                self.bboxOptions.south = Number(extent4326[1].toFixed(4));
                self.bboxOptions.east = Number(extent4326[2].toFixed(4));
                self.bboxOptions.west = Number(extent4326[0].toFixed(4));
                $('#bboxMap').css("cursor", "");
            });
        });
    };

    /**
     * onchange function for inputs to apply changes of coordinates to viewer.
     */
    self.onChangeBboxCoords = function () {
        if (self.bboxOptions.north && self.bboxOptions.south &&
            self.bboxOptions.east && self.bboxOptions.west) {
            var minX = Number(self.bboxOptions.west);
            var minY = Number(self.bboxOptions.south);
            var maxX = Number(self.bboxOptions.east);
            var maxY = Number(self.bboxOptions.north);
            if (MetadataEditorViewer.projection === 'EPSG:3857') {
                if (minY < -85) {
                    minY = -85;
                }
                if (maxY > 85) {
                    maxY = 85;
                }
            }
            var coordinates = [[[minX, minY], [minX, maxY], [maxX, maxY], [maxX, minY], [minX, minY]]];
            var polygon = new ol.geom.Polygon(coordinates);
            polygon = polygon.transform('EPSG:4326', MetadataEditorViewer.projection);
            var feature = new ol.Feature(polygon);
            if (!self.bboxOptions.olVectorLayer) {
                self.bboxOptions.olVectorLayer = new ol.layer.Vector({
                    map: MetadataEditorViewer.map,
                    source: new ol.source.Vector({
                        features: new ol.Collection(),
                        useSpatialIndex: false // optional, might improve performance
                    }),
                    style: self.bboxOptions.olStyle,
                    updateWhileAnimating: true, // optional, for instant visual feedback
                    updateWhileInteracting: true // optional, for instant visual feedback
                });
            }
            self.bboxOptions.olVectorLayer.getSource().clear();
            self.bboxOptions.olVectorLayer.getSource().addFeature(feature);
            MetadataEditorViewer.zoomToExtent([minX, minY, maxX, maxY], MetadataEditorViewer.map.getSize(), false);
        }
    };

    self.onSelectGeonames = function (item, model, label) {
        $timeout(function () {
            self.bboxOptions.west = Number(item.source.bbox.west);
            self.bboxOptions.east = Number(item.source.bbox.east);
            if (self.bboxOptions.east < self.bboxOptions.west) {
                self.bboxOptions.west -= 360;
            }
            self.bboxOptions.north = Number(item.source.bbox.north);
            self.bboxOptions.south = Number(item.source.bbox.south);
            self.onChangeBboxCoords();
        });

    };

    /**
     * Return a promise object to load geonames candidates asynchronously for autocompleter.
     * @param val
     */
    self.resolveGeonames = function (val) {
        var params = "name_startsWith=" + encodeURIComponent(val) +
            "&lang=" + $translate.use() +
            "&style=MEDIUM" +
            "&orderby=population" +
            "&inclBbox=true" +
            "&maxRows=20" +
            "&username=examind";
        return $http.get('http://api.geonames.org/searchJSON?' + params)
            .then(function (response) {
                //on success
                var result = [];
                var alreadyStored = [];
                if (response.data && angular.isArray(response.data.geonames)) {
                    angular.forEach(response.data.geonames, function (candidat) {
                        if (candidat.bbox && result.length < 8) {
                            var countryName = '';
                            if (candidat.countryName) {
                                countryName = ' (' + candidat.countryName + ')';
                            }
                            var adminName1 = '';
                            if (candidat.adminName1) {
                                adminName1 = ', ' + candidat.adminName1;
                            }
                            var item = {
                                label: candidat.name + adminName1 + countryName,
                                source: candidat
                            };
                            if (alreadyStored.indexOf(item.label) === -1) {
                                result.push(item);
                                alreadyStored.push(item.label);
                            }
                        }
                    });
                }
                return result;
            }, function (response) {
                //on error
                Growl('error', 'Error', 'The Geonames server returned an error!');
            });
    };

    /**
     * Apply new coordinates to the metadata block and close the modal.
     */
    self.saveBboxValues = function () {
        if (self.bboxOptions.currentBboxBlock &&
            self.bboxOptions.currentBboxBlock.block &&
            self.bboxOptions.currentBboxBlock.block.children) {
            var fields = self.bboxOptions.currentBboxBlock.block.children;
            for (var i = 0; i < fields.length; i++) {
                if (fields[i].field && fields[i].field.name.indexOf('westBoundLongitude') !== -1) {
                    fields[i].field.value = self.bboxOptions.west;
                } else if (fields[i].field && fields[i].field.name.indexOf('eastBoundLongitude') !== -1) {
                    fields[i].field.value = self.bboxOptions.east;
                } else if (fields[i].field && fields[i].field.name.indexOf('northBoundLatitude') !== -1) {
                    fields[i].field.value = self.bboxOptions.north;
                } else if (fields[i].field && fields[i].field.name.indexOf('southBoundLatitude') !== -1) {
                    fields[i].field.value = self.bboxOptions.south;
                }
            }
        }
        $modalInstance.close();
    };
    self.initCtrl();
}


