angular.module('examind.components.style.editor.new', [
    'examind.components.style.editor.new.vector',
    'examind.components.style.editor.new.raster',
    'examind.shared.data.viewer.service'
])
    .controller('NewStyleController', NewStyleController)
    .factory('NewStyleServices', NewStyleServicesFactory)
    .directive('newStyle', newStyleDirective);

function newStyleDirective() {
    return {
        restrict: 'E',
        require: '^styleEditor',
        templateUrl: 'components/style-editor/new-style/new-style.html',
        controller: 'NewStyleController',
        controllerAs: 'newCtrl',
        scope: {
            selectedDataRef: "=",
            styleToEdit: "=?",
            afterCreateStyle: "&?"
        }
    };
}

function NewStyleController($scope, $translate, Growl, Examind, DataViewerService) {
    var self = this;

    self.selectedDataRef = $scope.selectedDataRef || {};

    /**
     * In the case of edit or replicate use the content of styleToEdit else new style
     *
     * @type {string|*|{name: string, rules: Array}}
     */
    self.newStyle = $scope.styleToEdit || {
        name: "",
        rules: []
    };

    // The type of the style vector or raster
    self.selectedStyleType = null;

    // Temporary style to handle the styling in the preview map
    self.temporaryStyle = {
        id: -1,
        name: ''
    };

    self.afterCreateStyle = $scope.afterCreateStyle() || angular.noop;

    // The boundingBox of the data layer to show in the new style map
    self.boundingBox = [];

    /**
     * In the case of using the component to create new vector style without data
     */
    self.initVectorType = function () {
        self.selectedStyleType = 'vector';
        var timestamp = new Date().getTime();
        self.newStyle.name = "default-sld" + timestamp;

        // The default data for new vector style
        self.selectedDataRef.dataLayer = {
            id: 1,
            name: 'CNTR_RG_60M_2006',
            provider: "generic_shp",
            type: "VECTOR"
        };

        self.initDataLayerProperties(self.selectedDataRef.dataLayer.id);
    };

    /**
     * In the case of using the component to create new raster style without data
     */
    self.initRasterType = function () {
        self.selectedStyleType = 'coverage';
        var timestamp = new Date().getTime();
        self.newStyle.name = "default-sld" + timestamp;

        // The default data for new raster style
        self.selectedDataRef.dataLayer = {
            id: 4,
            name: 'cloudsgrey',
            provider: "generic_world_tif",
            type: "COVERAGE"
        };

        self.initDataLayerProperties(self.selectedDataRef.dataLayer.id);
    };

    self.displayNewStyle = function (mapId, callbackAfterCreate) {
        //skip if layerName is undefined
        if (!self.selectedDataRef.dataLayer && !self.selectedDataRef.dataLayer.name) {
            return;
        }

        DataViewerService.initConfig();

        var callback = function (response) {
            self.temporaryStyle.id = response.data;
            var layerData;

            if (self.selectedDataRef.dataLayer) {
                if (self.newStyle.rules.length === 0) {
                    layerData = DataViewerService.createLayer(window.localStorage.getItem('cstlUrl'), self.selectedDataRef.dataLayer.id,
                        self.selectedDataRef.dataLayer.name, null, false);
                } else {
                    layerData = DataViewerService.createLayerWithStyle(window.localStorage.getItem('cstlUrl'),
                        self.selectedDataRef.dataLayer.id, self.selectedDataRef.dataLayer.name,
                        self.newStyle.name, "sld_temp", null, false);
                }
            }

            //To force the browser cache reloading styled layer.
            layerData.get('params').ts = new Date().getTime();

            DataViewerService.layers = [layerData];

            setTimeout(function () {
                DataViewerService.initMap(mapId);

                if (self.boundingBox) {
                    var extent = [self.boundingBox[0],
                        self.boundingBox[1],
                        self.boundingBox[2],
                        self.boundingBox[3]];
                    DataViewerService.zoomToExtent(extent, DataViewerService.map.getSize(), true);
                }

                DataViewerService.map.on('moveend', setCurrentScale, DataViewerService.map);

                setCurrentScale();

                if (angular.isFunction(callbackAfterCreate)) {
                    callbackAfterCreate(response.data);
                }
            }, 200);
        };

        if (angular.isNumber(self.temporaryStyle.id) && self.temporaryStyle.id > 0) {
            Examind.styles.updateStyle(self.temporaryStyle.id, self.newStyle)
                .then(callback);
        } else {
            Examind.styles.createStyle(self.newStyle, 'sld_temp')
                .then(callback);
        }
    };

    self.initDataLayerProperties = function (dataId) {
        if (!dataId) {
            return;
        }
        Examind.datas.getGeographicExtent(dataId)
            .then(function (response) {
                    self.boundingBox = response.data.boundingBox;
                },
                function () {
                    $translate('style.editor.msg.error.data.descriptions')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
    };

    var setCurrentScale = function () {
        if (DataViewerService.map) {
            var currentScale = calcCurrentScale(DataViewerService.map);
            currentScale = Math.round(currentScale);
            jQuery('.currentScale').html("1 : " + currentScale);
        }
    };

    /**
     * Calculate and returns the map scale.
     * OL3 does not have any getScale() function.
     * @returns {number}
     */
    var calcCurrentScale = function (map) {
        var view = map.getView();
        var resolution = view.getResolution();
        var mpu = view.getProjection().getMetersPerUnit();
        var dpi = 25.4 / 0.28;
        var scale = resolution * mpu * 39.37 * dpi;
        return scale;
    };

    self.initVectorPlot = function () {
        self.loadPlot({
            json: {
                x: [],
                data1: []
            }
        }, '', true, 460, 250, '#chart', {top: 20, right: 10, bottom: 6, left: 50});
    };

    self.loadPlot = function (data, attr, useCategories, width, height, bindTo, padding) {
        window.c3chart = c3.generate({
            bindto: bindTo,
            size: {
                height: height,
                width: width
            },
            padding: padding,
            data: {
                x: 'x',
                json: data.json,
                types: {
                    data1: 'bar'
                },
                names: {
                    data1: attr
                }
            },
            color: {
                pattern: ['#9edae5']
            },
            zoom: {
                enabled: true
            },
            bar: {
                width: {
                    ratio: 0.8
                }
            },
            axis: {
                x: {
                    type: useCategories ? 'category' : null
                },
                y: {
                    label: {
                        text: "Count",
                        position: 'outer-middle'
                    }
                }
            }
        });
        $(window).resize(function () {
            if (window.c3chart) {
                window.c3chart.resize();
            }
        });
    };

    /**
     * Delete temporary style created with sld_temp provider.
     */
    self.deleteTemporaryStyle = function () {
        if (!self.temporaryStyle || !self.temporaryStyle.id || !angular.isNumber(self.temporaryStyle.id) || self.temporaryStyle.id < 1) {
            return;
        }

        Examind.styles.deleteStyle(self.temporaryStyle.id);
    };

    self.createStyle = function () {
        if (!self.newStyle.name || self.newStyle.name === '') {
            return;
        }

        //write style in server side.
        Examind.styles.createStyle(self.newStyle, 'sld').then(
            function (response) {
                $translate('style.editor.msg.success.style.creation')
                    .then(function (translatedMsg) {
                        Growl('success', 'Success', translatedMsg);
                        self.afterCreateStyle(response.data);
                    });
            }, function (response) {
                var msgKey = 'style.editor.msg.error.style.creation';
                if (response && response.data && response.data.errorMessageI18nCode) {
                    msgKey = response.data.errorMessageI18nCode;
                }
                $translate(msgKey)
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            }
        );
    };

    self.init = function () {
        // The case of create new style for existing data
        if (self.selectedDataRef && self.selectedDataRef.dataLayer && self.selectedDataRef.dataLayer.type) {
            self.selectedStyleType = self.selectedDataRef.dataLayer.type.toLowerCase();
            self.initDataLayerProperties(self.selectedDataRef.dataLayer.id);
        }
    };

    self.init();

    $scope.$on("$destroy", function () {
        self.deleteTemporaryStyle();
    });

}

function NewStyleServicesFactory() {
    var self = {};

    /**
     * Affect alpha from colorpicker into param.opacity
     * @param value
     * @param param
     */
    self.affectAlpha = function (value, param) {
        param.opacity = value.getAlpha();
    };

    /**
     * utility function that returns true if the expression is a number.
     * otherwise return false.
     * @param expr
     * @returns {boolean}
     */
    self.isExpressionNumber = function (expr) {
        var n = Number(expr);
        return isFinite(n);
    };

    self.setAttrToInputWidth = function (attrName, symbolizerStroke) {
        symbolizerStroke.width = '"' + attrName + '"';
    };

    /**
     * function called for symbolizer line or polygon for stroke type
     * @param symbolizer
     * @param traitType
     */
    self.addStrokeDashArray = function (symbolizer, traitType) {
        if (traitType === 'pointille') {
            if (!symbolizer.stroke) {
                symbolizer.stroke = {};
            }
            symbolizer.stroke.dashArray = [6, 6];
            symbolizer.stroke.dashed = true;
        } else {
            symbolizer.stroke.dashArray = null;
            symbolizer.stroke.dashed = false;
        }
    };

    /**
     * Returns true if the given string value is like ttf:fontName?char=code.
     * @param value
     * @returns {*|boolean}
     */
    self.isTTFValue = function (value) {
        return (value && value.indexOf('ttf:') !== -1);
    };

    /**
     * This is the mapping code->css class for awesome font in symbolizer point selection.
     */
    self.fontsMapping = {
        '0xf105': 'fa-angle-right',
        '0xf101': 'fa-angle-double-right',
        '0xf061': 'fa-arrow-right',
        '0xf178': 'fa-long-arrow-right',
        '0xf124': 'fa-location-arrow',
        '0xf1ae': 'fa-child',
        '0xf1b0': 'fa-paw',
        '0xf087': 'fa-thumbs-o-up',
        '0xf043': 'fa-tint',
        '0xf072': 'fa-plane',
        '0xf0e7': 'fa-bolt',
        '0xf06e': 'fa-eye',
        '0xf024': 'fa-flag',
        '0xf112': 'fa-reply',
        '0xf0e9': 'fa-umbrella',
        '0xf041': 'fa-map-marker',
        '0xf06d': 'fa-fire',
        '0xf002': 'fa-search',
        '0xf007': 'fa-user',
        '0xf071': 'fa-warning',
        '0xf0ad': 'fa-wrench',
        '0xf09e': 'fa-rss',
        '0xf13d': 'fa-anchor',
        '0xf06c': 'fa-leaf',
        '0xf0c2': 'fa-cloud',
        '0xf118': 'fa-smile-o'
    };

    /**
     * Returns FontAwesome css class for code.
     * @param value
     * @returns {*}
     */
    self.resolveClassForCode = function (value) {
        if (self.isTTFValue(value)) {
            return self.fontsMapping[value.substring(value.indexOf('=') + 1)];
        }
        return '';
    };

    self.getFontsCodes = function () {
        var fontsCodes = [];
        for (var code in self.fontsMapping) {
            if (self.fontsMapping.hasOwnProperty(code)) {
                fontsCodes.push(code);
            }
        }
        return fontsCodes;
    };

    self.setAttrToInputSize = function (attrName, symbolizerGraphicOrFont) {
        symbolizerGraphicOrFont.size = '"' + attrName + '"';
    };

    self.setAttrToInputRotation = function (attrName, symbolizerGraphic) {
        symbolizerGraphic.rotation = '"' + attrName + '"';
    };

    self.setAttrToInputOpacity = function (attrName, symbolizerGraphic) {
        symbolizerGraphic.opacity = '"' + attrName + '"';
    };

    /**
     * init the font model for symbolizer text.
     */
    self.initFontFamilies = function (symbolizer) {
        if (!symbolizer.font) {
            symbolizer.font = {};
        }
        if (!symbolizer.font.family) {
            symbolizer.font.family = [];
        }
    };

    return self;
}
