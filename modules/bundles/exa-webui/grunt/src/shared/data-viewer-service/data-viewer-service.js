angular.module('examind.shared.data.viewer.service', [])
    .factory('DataViewerService', DataViewerServiceFactory);

/**
 *
 * @returns {DataViewerServiceFactory}
 * @constructor
 */
function DataViewerServiceFactory() {

    var self = this;
    self.map = undefined;
    self.target = undefined;
    self.layers = []; //array of layers to display
    self.extent = [-180, -85, 180, 85]; //extent array of coordinates always in 4326
    self.projection = 'EPSG:3857'; //given projection used to display layers
    self.maxExtent = undefined; //the maximum extent for the given projection
    self.addBackground = true;
    self.fullScreenControl = false;
    self.enableAttributions = true;

    self.initConfig = function () {
        self.layers = [];
        self.extent = [-180, -85, 180, 85];
        self.projection = 'EPSG:3857';
        self.addBackground = true;
        self.fullScreenControl = false;
        self.enableAttributions = true;
    };

    self.initMap = function (mapId) {
        //unbind the old map
        if (self.map) {
            self.map.setTarget(undefined);
        }

        //get projection extent
        var projection = ol.proj.get(self.projection);
        self.maxExtent = projection.getExtent();
        //calculate reprojected extent for given projection
        var reprojExtent = ol.proj.transformExtent(self.extent, 'EPSG:4326', self.projection);
        //if the projected extent contains Infinity then the extent will be the projection extent.
        if (Array.isArray(reprojExtent)) {
            for (var i = 0; i < reprojExtent.length; i++) {
                var coord = reprojExtent[i];
                if (self.isNotNumber(coord)) {
                    reprojExtent = projection.getExtent();
                    break;
                }
            }
        }
        //clip the projected extent that should never be out of default projection extent
        if (reprojExtent[0] < self.maxExtent[0]) {
            reprojExtent[0] = self.maxExtent[0];
        }
        if (reprojExtent[1] < self.maxExtent[1]) {
            reprojExtent[1] = self.maxExtent[1];
        }
        if (reprojExtent[2] > self.maxExtent[2]) {
            reprojExtent[2] = self.maxExtent[2];
        }
        if (reprojExtent[3] > self.maxExtent[3]) {
            reprojExtent[3] = self.maxExtent[3];
        }

        if (self.addBackground) {
            var backgroundLayer;
            if (JSON.parse(window.localStorage.getItem('map-background-offline-mode'))) {
                backgroundLayer = new ol.layer.Tile({
                    source: new ol.source.TileWMS({
                        url: window.localStorage.getItem('map-background-url'),
                        params: {
                            'LAYERS': window.localStorage.getItem('map-background-layer'),
                            'VERSION': '1.3.0',
                            'SLD_VERSION': '1.1.0',
                            'FORMAT': 'image/png',
                            'TRANSPARENT': 'true'
                        }
                    })
                });

            } else {
                //adding background layer by default OSM
                var osmOpts = {};
                if (!self.enableAttributions) {
                    osmOpts.attributions = null;
                }
                var sourceOSM = new ol.source.OSM(osmOpts);
                backgroundLayer = new ol.layer.Tile({
                    source: sourceOSM
                });
            }
            self.layers.unshift(backgroundLayer);
        }

        var controlsArray = [
            new ol.control.ScaleLine({
                units: 'metric'
            }),
            new ol.control.Zoom({
                zoomInTipLabel: 'Zoom in',
                zoomOutTipLabel: 'Zoom out'
            })
        ];
        if (self.fullScreenControl) {
            controlsArray.push(new ol.control.FullScreen());
        }
        self.map = new ol.Map({
            controls: ol.control.defaults().extend(controlsArray),
            layers: self.layers,
            target: mapId,
            view: new ol.View({
                projection: self.projection
            }),
            logo: false
        });

        // Zoom on specified extent
        self.map.updateSize();
        var size = self.map.getSize();
        self.map.getView().fit(reprojExtent, size);

        //mouse cursor for pan
        self.map.on("pointerdrag", function (evt) {
            var target = this.getTarget();
            var jTarget = typeof target === "string" ? $("#" + target) : $(target);
            if (jTarget.css("cursor") !== 'crosshair') {
                jTarget.css("cursor", "move");
            }
        });
        self.map.on("moveend", function (evt) {
            var target = this.getTarget();
            var jTarget = typeof target === "string" ? $("#" + target) : $(target);
            jTarget.css("cursor", "");
        });

    };

    self.isNotNumber = function (n) {
        return (n === Number.POSITIVE_INFINITY || n === Number.NEGATIVE_INFINITY || isNaN(n));
    };

    self.zoomToExtent = function (extent, size, postZoom) {
        var projection = ol.proj.get(self.projection);
        var reprojExtent = ol.proj.transformExtent(extent, 'EPSG:4326', self.projection);
        if (Array.isArray(reprojExtent)) {
            for (var i = 0; i < reprojExtent.length; i++) {
                var coord = reprojExtent[i];
                if (self.isNotNumber(coord)) {
                    reprojExtent = projection.getExtent();
                    break;
                }
            }
        }
        //clip the projected extent that should never be out of default projection extent
        if (reprojExtent[0] < self.maxExtent[0]) {
            reprojExtent[0] = self.maxExtent[0];
        }
        if (reprojExtent[1] < self.maxExtent[1]) {
            reprojExtent[1] = self.maxExtent[1];
        }
        if (reprojExtent[2] > self.maxExtent[2]) {
            reprojExtent[2] = self.maxExtent[2];
        }
        if (reprojExtent[3] > self.maxExtent[3]) {
            reprojExtent[3] = self.maxExtent[3];
        }
        self.map.getView().fit(reprojExtent, size);
        if (postZoom) {
            self.map.getView().setZoom(self.map.getView().getZoom() + 1);
        }
    };

    self.createLayer = function (cstlUrlPrefix, dataId, dataName, filter, tiled) {
        var params = {
            'DATA_ID': dataId,
            'VERSION': '1.3.0',
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png'
        };
        if (filter) {
            params.CQLFILTER = filter;
        }
        var layer;
        if (tiled) {
            layer = new ol.layer.Tile({
                source: new ol.source.TileWMS({
                    url: cstlUrlPrefix + 'API/portray',
                    params: params
                })
            });
        } else {
            layer = new ol.layer.Image({
                source: new ol.source.ImageWMS({
                    url: cstlUrlPrefix + 'API/portray',
                    params: params
                })
            });
        }
        layer.set('params', params);
        layer.set('name', dataName);
        return layer;
    };

    self.createLayerWithStyle = function (cstlUrlPrefix, dataId, dataName, style, sldProvider, filter, tiled) {
        var sldProvName = (sldProvider) ? sldProvider : "sld";
        var params = {
            'DATA_ID': dataId,
            'VERSION': '1.3.0',
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png',
            'SLDID': (style) ? style : '',
            'SLDPROVIDER': sldProvName
        };
        if (filter) {
            params.CQLFILTER = filter;
        }
        var layer;
        if (tiled) {
            layer = new ol.layer.Tile({
                source: new ol.source.TileWMS({
                    url: cstlUrlPrefix + 'API/portray/style',
                    params: params
                })
            });
        } else {
            layer = new ol.layer.Image({
                source: new ol.source.ImageWMS({
                    url: cstlUrlPrefix + 'API/portray/style',
                    params: params
                })
            });
        }
        layer.set('params', params);
        layer.set('name', dataName);
        return layer;
    };

    self.createLayerWMS = function (cstlUrlPrefix, layerName, instance, versions) {
        var version = '1.3.0';//default version
        if (versions) {
            version = versions[versions.length - 1];
        }
        var params = {
            'LAYERS': layerName,
            'VERSION': version,
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png',
            'TRANSPARENT': 'true'
        };
        var layer = new ol.layer.Tile({
            source: new ol.source.TileWMS({
                url: cstlUrlPrefix + 'WS/wms/' + instance,
                params: params
            })
        });
        layer.set('params', params);
        layer.set('name', layerName);
        return layer;
    };

    self.createLayerWMSWithStyle = function (cstlUrlPrefix, layerName, instance, style, versions) {
        var version = '1.3.0';//default version
        if (versions) {
            version = versions[versions.length - 1];
        }
        var params = {
            'LAYERS': layerName,
            'VERSION': version,
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png',
            'STYLES': (style) ? style : '',
            'TRANSPARENT': 'true'
        };
        var layer = new ol.layer.Tile({
            source: new ol.source.TileWMS({
                url: cstlUrlPrefix + 'WS/wms/' + instance,
                params: params
            })
        });
        layer.set('params', params);
        layer.set('name', layerName);
        return layer;
    };

    self.createLayerExternalWMS = function (url, layerName) {
        var params = {
            'LAYERS': layerName,
            'VERSION': '1.3.0',
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png',
            'TRANSPARENT': 'true'
        };
        var layer = new ol.layer.Tile({
            source: new ol.source.TileWMS({
                url: url,
                params: params
            })
        });
        layer.set('params', params);
        layer.set('name', layerName);
        return layer;
    };

    self.createLayerExternalWMSWithStyle = function (url, layerName, style) {
        var params = {
            'LAYERS': layerName,
            'VERSION': '1.3.0',
            'SLD_VERSION': '1.1.0',
            'FORMAT': 'image/png',
            'STYLES': (style) ? style : '',
            'TRANSPARENT': 'true'
        };
        var layer = new ol.layer.Tile({
            source: new ol.source.TileWMS({
                url: url,
                params: params
            })
        });
        layer.set('params', params);
        layer.set('name', layerName);
        return layer;
    };

    self.createSensorsLayer = function (layerName) {
        var stylesMap = {
            'default': [new ol.style.Style({
                image: new ol.style.Icon(({
                    src: 'img/marker_normal.png'
                }))
            })],
            'select': [new ol.style.Style({
                image: new ol.style.Icon(({
                    src: 'img/marker_selected.png'
                }))
            })]
        };

        var layer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: []
            }),
            style: function (feature, resolution) {
                if (window.selectClick) {
                    var selectedFeatures = window.selectClick.getFeatures();
                    if (selectedFeatures && feature === selectedFeatures[0]) {
                        return stylesMap.select;
                    }
                }
                return stylesMap.default;
            }
        });
        layer.set('name', layerName);
        return layer;
    };

    self.setSensorStyle = function (type, layer) {
        var stylesMap = {};
        if (type && type === 'polygon') {
            stylesMap = {
                'default': [new ol.style.Style({
                    fill: new ol.style.Fill({
                        color: 'rgba(57, 179, 215, 0.25)'
                    }),
                    stroke: new ol.style.Stroke({
                        color: '#000000',
                        width: 1
                    })
                })],
                'select': [new ol.style.Style({
                    fill: new ol.style.Fill({
                        color: 'rgba(145, 0, 0, 0.25)'
                    }),
                    stroke: new ol.style.Stroke({
                        color: '#000000',
                        width: 1
                    })
                })]
            };
        } else if (type && type === 'line') {
            stylesMap = {
                'default': [new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#39B3D7',
                        width: 4
                    })
                })],
                'select': [new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: '#BE1522',
                        width: 6
                    })
                })]
            };
        } else {
            stylesMap = {
                'default': [new ol.style.Style({
                    image: new ol.style.Icon(({
                        anchor: [0.5, 26],
                        anchorXUnits: 'fraction',
                        anchorYUnits: 'pixels',
                        opacity: 0.75,
                        src: 'img/marker_normal.png'
                    }))
                })],
                'select': [new ol.style.Style({
                    image: new ol.style.Icon(({
                        anchor: [0.5, 26],
                        anchorXUnits: 'fraction',
                        anchorYUnits: 'pixels',
                        opacity: 0.75,
                        src: 'img/marker_selected.png'
                    }))
                })]
            };
        }

        layer.setStyle(function (feature, resolution) {
            var selectedFeatures = window.selectClick.getFeatures();
            if (selectedFeatures && feature === selectedFeatures[0]) {
                return stylesMap.select;
            } else {
                return stylesMap.default;
            }
        });
    };

    return self;
}
