angular.module('examind.components.style.editor.new.raster', [
    'examind.shared.data.viewer.service',
    'examind.components.style.editor.new.raster.cells',
    'examind.components.style.editor.new.raster.dynamic.range',
    'examind.components.style.editor.new.raster.sld'
])
    .constant('RASTER_STYLE_TYPE', {
        'none': 'none',
        'palette': 'palette',
        'cells': 'cell',
        'dynamic': 'dynamic'
    })
    .controller('RasterStyleController', RasterStyleController)
    .directive('rasterStyle', rasterStyleDirective);

function rasterStyleDirective() {
    return {
        restrict: 'E',
        require: '^newStyle',
        templateUrl: 'components/style-editor/new-style/raster-style/raster-style.html',
        controller: 'RasterStyleController',
        controllerAs: 'rasterCtrl',
        scope: {
            displayNewStyle: "&",
            newStyle: '=',
            selectedDataRef: "=",
            temporaryStyle: "=",
            createStyle: "&"
        }
    };
}

function RasterStyleController($scope, $modal, $translate, $timeout, Growl, RASTER_STYLE_TYPE, Examind, DataViewerService) {
    var self = this;

    self.RASTER_STYLE_TYPE = angular.copy(RASTER_STYLE_TYPE);

    self.helper = {
        autoPreview: true,
        enableRaster: RASTER_STYLE_TYPE.none,
        noNameErr: false,
        selectedRule: null,
        enabledRasterChart: false
    };

    self.filter = {
        filtersEnabled: false,
        filterMode: 'simple',
        showFilterTextArea: false,
        filters: [{
            "attribute": "",
            "comparator": "=",
            "value": "",
            "operator": ''
        }]
    };

    // The new style object
    self.newStyle = $scope.newStyle;

    self.temporaryStyle = $scope.temporaryStyle;

    self.rasterPalette = {
        "symbolPills": 'color',
        "colorModel": 'palette',
        band: {
            "selected": {name: "0", indice: "0", minValue: 0, maxValue: 255}
        },
        palette: {
            "index": undefined,
            "img_palette": 'img/palette0.png',
            "rasterMinValue": 0,
            "rasterMaxValue": 255,
            "intervalsCount": 5,
            "channelSelection": undefined,
            nan: {
                "color": undefined,
                "selected": false
            },
            "inverse": false,
            "method": 'interpolate',
            "open": false
        },
        repartition: undefined,
        dataXArray: [],
        rgbChannels: [{name: ''}, {name: ''}, {name: ''}],
        greyChannel: {
            name: ''
        }
    };

    self.rasterCells = {
        "cellSize": 20,
        "cellType": 'point',
        pointSymbol: {
            "@symbol": 'point',
            "name": '',
            "graphic": {
                "size": 15,
                "rotation": 0,
                "opacity": 1,
                "mark": {
                    "geometry": 'circle',
                    "stroke": {
                        "color": '#000000',
                        "opacity": 1
                    },
                    "fill": {
                        "color": '#808080',
                        "opacity": 0.7
                    }
                }
            }
        },
        textSymbol: {
            "@symbol": 'text',
            "name": '',
            "label": '',
            "font": {
                "size": 12,
                "bold": false,
                "italic": false,
                "family": ['Arial']
            },
            "fill": {
                "color": "#000000",
                "opacity": 1
            }
        }
    };

    self.rasterDynamic = {
        "channels": [
            {
                "band": "",
                "colorSpaceComponent": "R",
                "lower": {
                    "value": 0
                },
                "upper": {
                    "value": 255
                }
            },
            {
                "band": "",
                "colorSpaceComponent": "G",
                "lower": {
                    "value": 0
                },
                "upper": {
                    "value": 255
                }
            },
            {
                "band": "",
                "colorSpaceComponent": "B",
                "lower": {
                    "value": 0
                },
                "upper": {
                    "value": 255
                }
            },
            {
                "band": "",
                "colorSpaceComponent": "A",
                "lower": {
                    "value": 0
                },
                "upper": {
                    "value": 255
                }
            }
        ],
        "symbolPills": 'color'
    };

    self.selectedDataProperties = {
        dataBbox: null,
        dataProperties: null,
        dataBands: null,
        dataBandsRepartition: null
    };

    self.displayNewStyle = $scope.displayNewStyle();

    self.selectedDataRef = $scope.selectedDataRef;

    self.createStyle = $scope.createStyle();

    /**
     * Set the selected rule object into the scope.
     * @param rule
     */
    self.setSelectedRule = function (rule) {
        self.helper.selectedRule = rule;
    };

    /**
     * For Raster : make raster palette panel to visible
     */
    self.editRasterPalette = function () {
        if (self.rasterPalette) {
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.palette;
        }
    };

    /**
     * For Raster : make raster cells panel to visible
     */
    self.editRasterCells = function () {
        if (self.rasterCells) {
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.cell;
        }
    };

    /**
     * For Raster : make Dynamic range panel to visible
     */
    self.editRasterDynamic = function () {
        if (self.rasterDynamic) {
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.dynamic;
        }
    };

    /**
     * Returns true if the given choice matches the stylechooser.
     * @param choice
     * @returns {boolean}
     */
    self.isSelectedChooser = function (choice) {
        //TODO ...
        // return choice === self.helper.styleChooser;
    };

    self.createColorAndPaletteRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            self.rasterPalette = {
                "symbolPills": 'color',
                "colorModel": 'palette',
                band: {
                    "selected": {name: "0", indice: "0", minValue: 0, maxValue: 255}
                },
                palette: {
                    "index": undefined,
                    "img_palette": 'img/palette0.png',
                    "rasterMinValue": 0,
                    "rasterMaxValue": 255,
                    "intervalsCount": 5,
                    "channelSelection": undefined,
                    nan: {
                        "color": undefined,
                        "selected": false
                    },
                    "inverse": false,
                    "method": 'interpolate',
                    "open": false
                },
                repartition: undefined,
                dataXArray: [],
                rgbChannels: [{name: ''}, {name: ''}, {name: ''}],
                greyChannel: {
                    name: ''
                }
            };

            var paletteRule = {
                "name": 'palette-rule-' + new Date().getTime(),
                "title": '',
                "description": '',
                "maxScale": 5000000000,
                "symbolizers": [{'@symbol': 'raster'}],
                "filter": null
            };

            self.newStyle.rules.push(paletteRule);

            self.setSelectedRule(paletteRule);

            self.editRasterPalette();
        }
    };

    self.createCellsRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            self.rasterCells = {
                "cellSize": 20,
                "cellType": 'point',
                pointSymbol: {
                    "@symbol": 'point',
                    "name": '',
                    "graphic": {
                        "size": 15,
                        "rotation": 0,
                        "opacity": 1,
                        "mark": {
                            "geometry": 'circle',
                            "stroke": {
                                "color": '#000000',
                                "opacity": 1
                            },
                            "fill": {
                                "color": '#808080',
                                "opacity": 0.7
                            }
                        }
                    }
                },
                textSymbol: {
                    "@symbol": 'text',
                    "name": '',
                    "label": '',
                    "font": {
                        "size": 12,
                        "bold": false,
                        "italic": false,
                        "family": ['Arial']
                    },
                    "fill": {
                        "color": "#000000",
                        "opacity": 1
                    }
                }
            };

            var cellRule = {
                "name": 'cell-rule-' + new Date().getTime(),
                "title": '',
                "description": '',
                "maxScale": 5000000000,
                "symbolizers": [{
                    '@symbol': 'cell',
                    "cellSize": 20,
                    rule: {
                        "name": 'default',
                        "title": '',
                        "description": '',
                        "maxScale": 5000000000,
                        "symbolizers": [],
                        "filter": null
                    }
                }],
                "filter": null
            };

            self.newStyle.rules.push(cellRule);

            self.setSelectedRule(cellRule);

            self.editRasterCells();
        }
    };

    self.createDynamicRangesRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            self.rasterDynamic = {
                "channels": [
                    {
                        "band": "",
                        "colorSpaceComponent": "R",
                        "lower": {
                            "value": 0
                        },
                        "upper": {
                            "value": 255
                        }
                    },
                    {
                        "band": "",
                        "colorSpaceComponent": "G",
                        "lower": {
                            "value": 0
                        },
                        "upper": {
                            "value": 255
                        }
                    },
                    {
                        "band": "",
                        "colorSpaceComponent": "B",
                        "lower": {
                            "value": 0
                        },
                        "upper": {
                            "value": 255
                        }
                    },
                    {
                        "band": "",
                        "colorSpaceComponent": "A",
                        "lower": {
                            "value": 0
                        },
                        "upper": {
                            "value": 255
                        }
                    }
                ],
                "symbolPills": 'color'
            };

            var dynamicRule = {
                "name": 'dynamic-rule-' + new Date().getTime(),
                "title": '',
                "description": '',
                "maxScale": 5000000000,
                "symbolizers": [{'@symbol': 'dynamicrange'}],
                "filter": null
            };

            self.newStyle.rules.push(dynamicRule);

            self.setSelectedRule(dynamicRule);

            self.editRasterDynamic();
        }
    };

    self.isSelectedRule = function (rule) {
        return angular.equals(rule, self.helper.selectedRule);
    };

    /**
     * Returns true if there is a cell symbolizer in given array.
     * Used to identify cellSymbolizers rule against Palette/colors rule
     * @param symbolizers
     * @returns {boolean}
     */
    var existsCellSymbolizer = function (symbolizers) {
        if (symbolizers) {
            for (var i = 0; i < symbolizers.length; i++) {
                var symb = symbolizers[i];
                if (symb['@symbol'] === 'cell') {
                    return symb;
                }
            }
        }
        return null;
    };

    /**
     * Returns true if there is a cell symbolizer in given array.
     * Used to identify cellSymbolizers rule against Palette/colors rule
     * @param symbolizers
     * @returns {boolean}
     */
    var existsDynamicSymbolizer = function (symbolizers) {
        if (symbolizers) {
            for (var i = 0; i < symbolizers.length; i++) {
                var symb = symbolizers[i];
                if (symb['@symbol'] === 'dynamicrange') {
                    return true;
                }
            }
        }
        return false;
    };

    /**
     * load histogram c3 chart for given data and attribute.
     * @param data
     * @param attr
     * @param useCategories
     * @param width
     * @param height
     * @param bindTo
     * @param padding
     */
    var loadPlot = function (data, attr, useCategories, width, height, bindTo, padding) {
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
     * Draw xgrids thresholds.
     */
    self.drawThresholds = function () {
        if (self.rasterPalette.dataXArray && self.rasterPalette.dataXArray.length > 0) {
            var gridsArray = [];
            var paletteRepartition = self.rasterPalette.repartition;
            if (paletteRepartition) {
                for (var i = 0; i < paletteRepartition.length; i++) {
                    var threshold = paletteRepartition[i].data;
                    for (var j = 0; j < self.rasterPalette.dataXArray.length; j++) {
                        if (self.rasterPalette.dataXArray[j] >= threshold) {
                            gridsArray.push(
                                {value: j, text: threshold});
                            break;
                        }
                    }
                }
                window.c3chart.xgrids(gridsArray);
            }
        }
    };

    /**
     * Edit rule for raster.
     * and restore $scope.helper.rasterPalette with the selected rule properties.
     */
    self.editSelectedRasterRule = function () {
        //restore scope for channel selections (rgb / grayscale).
        var symbolizers = self.helper.selectedRule.symbolizers;

        if (symbolizers && symbolizers.length > 0 && symbolizers[0].channelSelection) {
            if (symbolizers[0].channelSelection.greyChannel) {
                self.rasterPalette.greyChannel = symbolizers[0].channelSelection.greyChannel;
                if (symbolizers[0].colorMap) {
                    self.rasterPalette.colorModel = 'palette';
                } else {
                    self.rasterPalette.colorModel = 'grayscale';
                }
            } else if (symbolizers[0].channelSelection.rgbChannels) {
                self.rasterPalette.rgbChannels = symbolizers[0].channelSelection.rgbChannels;
                self.rasterPalette.colorModel = 'rgb';
            }
        }

        var cellsymbolizer = existsCellSymbolizer(symbolizers);

        if (cellsymbolizer) {
            //open raster cells panel
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.cell;

            if (symbolizers.length > 0) {
                var symb = cellsymbolizer;
                self.rasterCells.cellSize = symb.cellSize;
                if (symb.rule && symb.rule.symbolizers && symb.rule.symbolizers.length > 0) {
                    var cellType = symb.rule.symbolizers[0]['@symbol'];
                    self.rasterCells.cellType = cellType;
                    if (cellType === 'point') {
                        self.rasterCells.pointSymbol = symb.rule.symbolizers[0];
                    } else if (cellType === 'text') {
                        self.rasterCells.textSymbol = symb.rule.symbolizers[0];
                    }
                }
            }
        } else if (existsDynamicSymbolizer(symbolizers)) {
            //open raster panel for dynamic symbolizer
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.dynamic;
            if (symbolizers && symbolizers.length > 0 && symbolizers[0].channels) {
                self.rasterDynamic.channels = symbolizers[0].channels;
            }
        } else {
            self.helper.enableRaster = self.RASTER_STYLE_TYPE.palette;

            //init sld editor values with selected rule.
            var channelSelection = symbolizers[0].channelSelection;
            if (channelSelection && channelSelection.greyChannel && self.rasterPalette.band && self.selectedDataProperties.dataBands) {
                var bandIdentified = null;
                for (var i = 0; i < self.selectedDataProperties.dataBands.length; i++) {
                    if (self.selectedDataProperties.dataBands[i].indice === channelSelection.greyChannel.name) {
                        bandIdentified = self.selectedDataProperties.dataBands[i];
                        break;
                    }
                }
                if (!bandIdentified) {
                    bandIdentified = self.selectedDataProperties.dataBands[0];
                }
                if (bandIdentified) {
                    self.rasterPalette.band.selected = bandIdentified;
                }
                self.rasterPalette.palette.rasterMinValue =
                    Number(self.rasterPalette.band.selected.minValue);
                self.rasterPalette.palette.rasterMaxValue =
                    Number(self.rasterPalette.band.selected.maxValue);
            }

            var colorMap = symbolizers[0].colorMap;

            if (colorMap) {
                self.rasterPalette.palette.method = colorMap.function['@function'];
                self.rasterPalette.palette.intervalsCount = colorMap.function.interval;

                if (colorMap.function.nanColor && colorMap.function.nanColor) {
                    self.rasterPalette.palette.nan.selected = true;
                    self.rasterPalette.palette.nan.color = colorMap.function.nanColor;
                }
                self.rasterPalette.repartition = colorMap.function.points;
            }

            //Load the selected band on the graph, the repartition of statistics is already present.
            if (self.selectedDataProperties.dataBandsRepartition && self.rasterPalette.band.selected) {
                var selectedBand = self.rasterPalette.band.selected.indice;
                if (!selectedBand) {
                    selectedBand = 0;
                }
                var xArray = [], yArray = [];
                if (self.selectedDataProperties.dataBandsRepartition[selectedBand]) {
                    var repartitionBand = self.selectedDataProperties.dataBandsRepartition[selectedBand].distribution;
                    for (var key in repartitionBand) {
                        if (repartitionBand.hasOwnProperty(key)) {
                            xArray.push(key);
                            yArray.push(repartitionBand[key]);
                        }
                    }
                }

                self.rasterPalette.dataXArray = xArray;

                var dataRes = {
                    json: {
                        x: xArray,
                        data1: yArray
                    }
                };

                //load data on graph
                loadPlot(dataRes, 'Band ' + selectedBand,
                    true, 460, 205, '#chartRaster', {}
                );
            }

            //Add on graph the vertical thresholds
            self.drawThresholds();
            self.rasterPalette.palette.open = true;
        }
    };

    /**
     * Function to move item in array with given indexes from and to.
     * @param array
     * @param from index in array
     * @param to index in array
     */
    var move = function (array, from, to) {
        if (to === from) {
            return;
        }
        var target = array[from];
        var increment = to < from ? -1 : 1;
        for (var k = from; k !== to; k += increment) {
            array[k] = array[k + increment];
        }
        array[to] = target;
    };

    /**
     * Move rule position to previous index in rules array
     */
    self.moveUpRule = function () {
        if (self.helper.selectedRule) {
            var indexPos = self.newStyle.rules.indexOf(self.helper.selectedRule);
            if (indexPos > 0) {
                move(self.newStyle.rules, indexPos, indexPos - 1);
            }
        }
    };

    /**
     * Move rule position to next index in rules array
     */
    self.moveDownRule = function () {
        if (self.helper.selectedRule) {
            var indexPos = self.newStyle.rules.indexOf(self.helper.selectedRule);
            if (indexPos < self.newStyle.rules.length - 1) {
                move(self.newStyle.rules, indexPos, indexPos + 1);
            }
        }
    };

    /**
     * Remove the selected rule from the current style's rules array.
     */
    self.deleteSelectedRule = function () {
        if (self.helper.selectedRule) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function () {
                        return "dialog.message.confirm.delete.rule";
                    }
                }
            });
            dlg.result.then(function (cfrm) {
                if (cfrm) {
                    var indexToRemove = self.newStyle.rules.indexOf(self.helper.selectedRule);
                    if (indexToRemove > -1) {
                        self.newStyle.rules.splice(indexToRemove, 1);
                        self.helper.selectedRule = null;
                    }
                }
            });
        }
    };

    /**
     * Remove all rules from the current style and set selected rule to null.
     */
    self.deleteAllRules = function () {
        if (self.newStyle.rules.length > 0) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function () {
                        return "dialog.message.confirm.delete.allrules";
                    }
                }
            });
            dlg.result.then(function (cfrm) {
                if (cfrm) {
                    self.newStyle.rules = [];
                    self.helper.selectedRule = null;
                }
            });
        }
    };

    self.initSelectedRuleTab = function () {
        self.selectedRuleTab = 'description';
    };

    self.isSelectedRuleTab = function (tabName) {
        return self.selectedRuleTab === tabName;
    };

    self.changeSelectedRuleTab = function (tabName) {
        self.selectedRuleTab = tabName;
    };

    /**
     * Calculate and returns the map scale.
     * OL3 does not have any getScale() function.
     * @returns {number}
     */
    var calcCurrentScale = function () {
        var map = DataViewerService.map;
        var view = map.getView();
        var resolution = view.getResolution();
        var mpu = view.getProjection().getMetersPerUnit();
        var dpi = 25.4 / 0.28;
        var scale = resolution * mpu * 39.37 * dpi;
        return scale;
    };

    /**
     * Binding action to set the map's scale as filter min scale.
     */
    self.setMinScale = function () {
        if (DataViewerService.map) {
            var currentScale = calcCurrentScale();
            currentScale = Math.round(currentScale);
            self.helper.selectedRule.minScale = currentScale;
        }
    };

    /**
     * Binding action to set the map's scale as filter max scale.
     */
    self.setMaxScale = function () {
        if (DataViewerService.map) {
            var currentScale = calcCurrentScale();
            currentScale = Math.round(currentScale);
            self.helper.selectedRule.maxScale = currentScale;
        }
    };

    /**
     * Binding action to show or display the raster's histogram chart
     */
    self.toggleRasterChart = function () {
        self.helper.enabledRasterChart = !self.helper.enabledRasterChart;

        if (self.helper.enabledRasterChart) {
            //fix bug for graph resize.
            setTimeout(function () {
                window.c3chart.resize();
            }, 200);
        }
    };

    self.initRasterPlot = function () {
        loadPlot({
                json: {
                    x: [],
                    data1: []
                }
            }, '', true, 460, 205,
            '#chartRaster', {}
        );

        if (self.selectedDataRef.dataLayer && self.selectedDataRef.dataLayer.id) {
            var dataId = self.selectedDataRef.dataLayer.id;

            $('#chart_ajax_loader').show();
            ///show histogram
            Examind.styles.getHistogram(dataId).then(
                function (response) {
                    if (response.data.bands && response.data.bands.length > 0) {
                        self.selectedDataProperties.dataBandsRepartition = response.data.bands;
                        var repartition = response.data.bands[0].distribution;
                        var xArray = [], yArray = [];
                        for (var key in repartition) {
                            if (repartition.hasOwnProperty(key)) {
                                xArray.push(key);
                                yArray.push(repartition[key]);
                            }
                        }
                        self.rasterPalette.dataXArray = xArray;
                        var dataRes = {
                            json: {
                                x: xArray,
                                data1: yArray
                            }
                        };
                        var bandName = 'Band 0';

                        if (self.selectedDataProperties.dataBands && self.selectedDataProperties.dataBands.length > 0) {
                            bandName = self.selectedDataProperties.dataBands[0].name;
                        }
                        loadPlot(dataRes, bandName,
                            true, 460, 205, '#chartRaster', {}
                        );
                        $('#chart_ajax_loader').hide();
                    }
                },
                function (reason) {
                    $translate('style.editor.msg.error.get.statistics')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                }
            );
        }

    };

    self.goBackToRulesList = function () {
        self.helper.enableRaster = self.RASTER_STYLE_TYPE.none;
    };

    self.loadDataProperties = function () {
        if (!self.selectedDataRef.dataLayer && self.selectedDataRef.dataLayer.id) {
            return;
        }
        Examind.datas.getDataDescription(self.selectedDataRef.dataLayer.id)
            .then(function (response) {
                self.selectedDataProperties.dataProperties = response.data;
                self.selectedDataProperties.dataBands = response.data.bands;
                if (self.selectedDataProperties.dataBands && self.selectedDataProperties.dataBands.length > 0) {
                    self.rasterPalette.band.selected = self.selectedDataProperties.dataBands[0];
                    self.rasterPalette.palette.rasterMinValue = Number(self.rasterPalette.band.selected.minValue);
                    self.rasterPalette.palette.rasterMaxValue = Number(self.rasterPalette.band.selected.maxValue);
                }
            }, function () {//error
                $translate('style.editor.msg.error.data.properties')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    /**
     * Apply colorMap on rule for selected palette.
     */
    self.addPalette = function (rule) {
        var palette = self.rasterPalette.palette;
        if (!palette.index) {
            return;
        }

        //set channel selection
        rule.symbolizers[0].channelSelection = {
            greyChannel: {
                name: self.rasterPalette.band.selected.indice
            },
            rgbChannels: null
        };

        var colorMap = rule.symbolizers[0].colorMap;

        if (!colorMap || !colorMap.function ||
            colorMap.function['@function'] !== palette.method) {
            colorMap = {'function': {'@function': palette.method}};
        }

        colorMap.function.interval = palette.intervalsCount;

        if (palette.nan.selected) {
            if (palette.nan.color) {
                colorMap.function.nanColor = palette.nan.color;
            } else {
                colorMap.function.nanColor = '#00ffffff';
            }
        } else {
            colorMap.function.nanColor = null;
        }

        //prevent against string number from input value of slider
        palette.rasterMinValue = Number(palette.rasterMinValue);
        palette.rasterMaxValue = Number(palette.rasterMaxValue);

        switch (palette.index) {
            case 1:
                var delta1 = palette.rasterMaxValue - palette.rasterMinValue;
                if (!palette.inverse) {
                    if (!colorMap.function) {
                        colorMap.function = {};
                    }
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#e52520'},
                            {data: delta1 * 0.25 + palette.rasterMinValue, color: '#ffde00'},
                            {data: delta1 * 0.5 + palette.rasterMinValue, color: '#95c11f'},
                            {data: delta1 * 0.75 + palette.rasterMinValue, color: '#1d71b8'},
                            {data: palette.rasterMaxValue, color: '#662483'}
                        ];
                } else {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#662483'},
                            {data: delta1 * 0.25 + palette.rasterMinValue, color: '#1d71b8'},
                            {data: delta1 * 0.5 + palette.rasterMinValue, color: '#95c11f'},
                            {data: delta1 * 0.75 + palette.rasterMinValue, color: '#ffde00'},
                            {data: palette.rasterMaxValue, color: '#e52520'}
                        ];
                }
                break;
            case 2:
                if (!palette.inverse) {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#3F3460'},
                            {data: palette.rasterMaxValue, color: '#EC1876'}
                        ];
                } else {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#EC1876'},
                            {data: palette.rasterMaxValue, color: '#3F3460'}
                        ];
                }
                break;
            case 3:
                if (!palette.inverse) {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#036531'},
                            {data: palette.rasterMaxValue, color: '#FDF01A'}
                        ];
                } else {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#FDF01A'},
                            {data: palette.rasterMaxValue, color: '#036531'}
                        ];
                }
                break;
            case 4:
                var delta4 = palette.rasterMaxValue - palette.rasterMinValue;
                if (!palette.inverse) {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#2d2e83'},
                            {data: delta4 * 0.25 + palette.rasterMinValue, color: '#1d71b8'},
                            {data: delta4 * 0.5 + palette.rasterMinValue, color: '#ffde00'},
                            {data: palette.rasterMinValue, color: '#e52520'}
                        ];
                } else {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#e52520'},
                            {data: delta4 * 0.5 + palette.rasterMinValue, color: '#ffde00'},
                            {data: delta4 * 0.75 + palette.rasterMinValue, color: '#1d71b8'},
                            {data: palette.rasterMinValue, color: '#2d2e83'}
                        ];
                }
                break;
            case 5:
                if (!palette.inverse) {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#000000'},
                            {data: palette.rasterMaxValue, color: '#FFFFFF'}
                        ];
                } else {
                    colorMap.function.points =
                        [
                            {data: palette.rasterMinValue, color: '#FFFFFF'},
                            {data: palette.rasterMaxValue, color: '#000000'}
                        ];
                }
                break;
            default:
                break;
        }

        rule.symbolizers[0].colorMap = colorMap;
    };

    /**
     * Binding action to generate raster palette.
     */
    self.generateRasterPalette = function () {
        if (self.helper.selectedRule) {
            //first of all, add Palette and ensure that the temporary style exists in server.
            self.addPalette(self.helper.selectedRule);
            self.displayNewStyle('styledMapOL', function (createdTmpStyle) {
                //get interpolation points for ui
                if (self.rasterPalette.palette.index) {
                    //show palette
                    Examind.styles.getPalette(self.temporaryStyle.id,
                        self.helper.selectedRule.name,
                        self.rasterPalette.palette.intervalsCount).then(
                        function (response) {
                            if (response.data.points) {
                                self.helper.selectedRule.symbolizers[0].colorMap.function.points = response.data.points;
                                self.rasterPalette.repartition = self.helper.selectedRule.symbolizers[0].colorMap.function.points;

                                //Load the selected band on the graph, the repartition of statistics is already present.
                                if (self.selectedDataProperties.dataBandsRepartition) {
                                    var loader = $('#chart_ajax_loader');
                                    loader.show();
                                    var selectedBand = self.rasterPalette.band.selected.indice;
                                    var xArray = [], yArray = [];
                                    if (self.selectedDataProperties.dataBandsRepartition[selectedBand]) {
                                        var repartition = self.selectedDataProperties.dataBandsRepartition[selectedBand].distribution;
                                        for (var key in repartition) {
                                            if (repartition.hasOwnProperty(key)) {
                                                xArray.push(key);
                                                yArray.push(repartition[key]);
                                            }
                                        }
                                    }
                                    self.rasterPalette.dataXArray = xArray;
                                    var dataRes = {
                                        json: {
                                            x: xArray,
                                            data1: yArray
                                        }
                                    };
                                    //load data on graph
                                    loadPlot(dataRes, 'Band ' + selectedBand,
                                        true, 460, 205, '#chartRaster', {}
                                    );
                                    loader.hide();
                                }

                                //Add on graph the vertical thresholds
                                self.drawThresholds();
                            }
                        },
                        function () {
                            $translate('style.editor.msg.error.get.palette')
                                .then(function (translatedMsg) {
                                    Growl('error', 'Error', translatedMsg);
                                });
                        }
                    );
                    self.rasterPalette.palette.open = true;
                }
            });
        }
    };

    self.init = function () {
        self.loadDataProperties();
    };

    self.init();

    /**
     * Adding watcher for newStyle variable to enable the auto preview on the map.
     */
    $scope.$watch(function () {
        return self.newStyle.rules;
    }, function () {
        if (self.helper.autoPreview) {
            //using $timeout to fix Angular bug :
            $timeout(function () {
                self.displayNewStyle("styledMapOL", null);
            }, 100);
        }
    }, true);

}