angular.module('examind.components.style.editor.new.vector', [
    'webui-config',
    'examind.shared.data.viewer.service',
    'examind.components.style.editor.new.vector.line',
    'examind.components.style.editor.new.vector.point',
    'examind.components.style.editor.new.vector.polygon',
    'examind.components.style.editor.new.vector.text'
])
    .controller('VectorStyleController', VectorStyleController)
    .directive('vectorStyle', vectorStyleDirective);

function vectorStyleDirective() {
    return {
        restrict: 'E',
        require: '^newStyle',
        templateUrl: 'components/style-editor/new-style/vector-style/vector-style.html',
        controller: 'VectorStyleController',
        controllerAs: 'vectorCtrl',
        scope: {
            displayNewStyle: "&",
            newStyle: "=",
            selectedDataRef: "=?",
            createStyle: "&"
        }
    };
}

function VectorStyleController($scope, $modal, $translate, $timeout, AppConfigService, Growl, Examind, DataViewerService) {
    var self = this;

    self.helper = {
        autoPreview: true,
        enableRuleEditor: false,
        enableAutoIntervalEditor: false,
        enabledVectorChart: false,
        noNameErr: false,
        selectedRule: null,
        selectedSymbolizerType: '',
        selectedSymbolizer: null
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

    self.selectedDataProperties = {
        dataBbox: null,
        dataProperties: null,
        attributesTypeNumber: [],
        attributesExcludeGeometry: []
    };

    self.symbolizerTypes = [{
        id: 1,
        name: "Point",
        value: "point",
        translateKey: "style.editor.new.vector.label.point"
    }, {
        id: 2,
        name: "Line",
        value: "line",
        translateKey: "style.editor.new.vector.label.line"
    }, {
        id: 3,
        name: "Polygon",
        value: "polygon",
        translateKey: "style.editor.new.vector.label.polygon"
    }, {
        id: 4,
        name: "Text",
        value: "text",
        translateKey: "style.editor.new.vector.label.text"
    }];

    self.comparatorList = [{
        id: 1,
        name: "!=",
        value: "<>"
    }, {
        id: 2,
        name: ">",
        value: ">"
    }, {
        id: 3,
        name: ">=",
        value: ">="
    }, {
        id: 4,
        name: "<",
        value: "<"
    }, {
        id: 5,
        name: "<=",
        value: "<="
    }, {
        id: 6,
        name: "BETWEEN",
        value: "BETWEEN"
    }, {
        id: 7,
        name: "LIKE",
        value: "LIKE"
    }, {
        id: 8,
        name: "ILIKE",
        value: "ILIKE"
    }];

    self.displayNewStyle = $scope.displayNewStyle();

    // The new style object
    self.newStyle = $scope.newStyle;

    self.autoIntervalValues = {
        "attr": "",
        "nbIntervals": 5,
        "method": "equidistant",
        "symbol": "polygon",
        "palette": {
            index: 1,
            img_palette: 'img/palette1.png',
            colors: [],
            reverseColors: false
        },
        "customPalette": {
            "enabled": false,
            "color1": '#ffffff',
            "color2": '#0022fc'
        }
    };

    self.autoUniqueValues = {
        "attr": "",
        "symbol": "polygon",
        "palette": {
            index: 1,
            img_palette: 'img/palette1.png',
            colors: [],
            reverseColors: false
        },
        "customPalette": {
            "enabled": false,
            "color1": '#ffffff',
            "color2": '#0022fc'
        }
    };

    self.chart = {
        "widget": null,
        "attribute": "",
        "min": null,
        "max": null
    };

    self.selectedDataRef = $scope.selectedDataRef;

    self.createStyle = $scope.createStyle();

    /**
     * Set the selected rule object.
     * @param rule
     */
    self.setSelectedRule = function (rule) {
        self.helper.selectedRule = rule;
    };

    self.editSelectedRule = function () {
        if (self.helper.selectedRule) {
            self.helper.enableRuleEditor = true;
            self.helper.enableAutoIntervalEditor = false;
            self.helper.enableAutoUniqueEditor = false;
        }
    };

    self.editAutoIntervalPanel = function () {
        if (self.autoIntervalValues) {
            self.helper.enableAutoIntervalEditor = true;
            self.helper.enableRuleEditor = false;
            self.helper.enableAutoUniqueEditor = false;
        }
    };

    self.editAutoUniquePanel = function () {
        if (self.autoUniqueValues) {
            self.helper.enableAutoUniqueEditor = true;
            self.helper.enableAutoIntervalEditor = false;
            self.helper.enableRuleEditor = false;
        }
    };

    self.createManualRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            var manualRule = {
                "name": 'default',
                "title": '',
                "description": '',
                "maxScale": 5000000000,
                "symbolizers": [],
                "filter": null
            };

            // Add the new rule to the new style object
            self.newStyle.rules.push(manualRule);

            self.setSelectedRule(manualRule);

            self.editSelectedRule();

        }

    };

    self.createAutoIntervalRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            self.autoIntervalValues = {
                "attr": "",
                "nbIntervals": 5,
                "method": "equidistant",
                "symbol": "polygon",
                "palette": {
                    index: 1,
                    img_palette: 'img/palette1.png',
                    colors: [],
                    reverseColors: false
                },
                "customPalette": {
                    "enabled": false,
                    "color1": '#ffffff',
                    "color2": '#0022fc'
                }
            };

            self.editAutoIntervalPanel();
        }

    };

    self.createAutoValuesRule = function () {
        if (self.newStyle.name === "") {
            //The style the name is required
            self.helper.noNameErr = true;
            return;
        } else {
            self.helper.noNameErr = false;

            self.autoUniqueValues = {
                "attr": "",
                "symbol": "polygon",
                "palette": {
                    index: 1,
                    img_palette: 'img/palette1.png',
                    colors: [],
                    reverseColors: false
                },
                "customPalette": {
                    "enabled": false,
                    "color1": '#ffffff',
                    "color2": '#0022fc'
                }
            };

            self.editAutoUniquePanel();
        }
    };

    self.isSelectedRule = function (rule) {
        return angular.equals(rule, self.helper.selectedRule);
    };

    /**
     * Function to move item in array with given indexes from and to.
     * @param array
     * @param from index in array
     * @param to index in array
     */
    function move(array, from, to) {
        if (to === from) {
            return;
        }
        var target = array[from];
        var increment = to < from ? -1 : 1;
        for (var k = from; k !== to; k += increment) {
            array[k] = array[k + increment];
        }
        array[to] = target;
    }

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

    self.toggleVectorChart = function () {
        self.helper.enabledVectorChart = !self.helper.enabledVectorChart;
    };

    self.disableVectorChart = function () {
        self.helper.enabledVectorChart = false;
    };

    /**
     * Binding function to control ng-if for displaying mode buttons to switch between carto or chart.
     * the view mode must be activated only if the layer data exists.
     * @returns {boolean}
     */
    self.shouldDisplayVectorChart = function () {
        if (self.selectedDataRef.dataLayer && self.selectedDataRef.dataLayer.id) {
            return true;
        } else {
            return false;
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
     * Set the selected symbolizer into the scope.
     * @param symbol
     */
    self.setSelectedSymbolizer = function (symbol) {
        self.helper.selectedSymbolizer = symbol;
    };

    /**
     * Add new symbolizer for current rule.
     * the geometry type is given from the select element.
     */
    self.addSymbolizer = function () {
        if (!self.helper.selectedRule || !self.helper.selectedSymbolizerType || self.helper.selectedSymbolizerType === '') {
            return;
        }

        var symbol;

        // Fill the symbol with the values of the selected type
        switch (self.helper.selectedSymbolizerType) {
            case 'point':
                symbol = {
                    "@symbol": 'point',
                    "name": 'symbol point',
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
                };
                break;
            case 'line':
                symbol = {
                    "@symbol": 'line',
                    "name": self.helper.selectedSymbolizerType,
                    "stroke": {
                        "color": "#000000",
                        "dashArray": null,
                        "dashOffset": 0,
                        "dashed": false,
                        "lineCap": "square",
                        "lineJoin": "bevel",
                        "opacity": 1,
                        "width": 1
                    },
                    "perpendicularOffset": 0
                };
                break;
            case 'polygon':
                symbol = {
                    "@symbol": 'polygon',
                    "name": self.helper.selectedSymbolizerType,
                    "fill": {
                        "color": "#c1c1c1",
                        "opacity": 1
                    },
                    "stroke": {
                        "color": "#000000",
                        "dashArray": null,
                        "dashOffset": 0,
                        "dashed": false,
                        "lineCap": "square",
                        "lineJoin": "bevel",
                        "opacity": 1,
                        "width": 1
                    },
                    "perpendicularOffset": 0
                };
                break;
            case 'text':
                symbol = {
                    "@symbol": 'text',
                    "name": self.helper.selectedSymbolizerType,
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
                    },
                    "halo": {
                        "radius": 1,
                        "fill": {
                            "color": "#FFFFFF",
                            "opacity": 1.0
                        }
                    }
                };
                break;
        }

        self.helper.selectedRule.symbolizers.push(symbol);

        self.setSelectedSymbolizer(symbol);
    };

    /**
     * Remove given symbolizer from the current rule.
     * @param symbolizer
     */
    self.removeSymbolizer = function (symbolizer) {
        var dlg = $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                'keyMsg': function () {
                    return "dialog.message.confirm.delete.symbolizer";
                }
            }
        });
        dlg.result.then(function (cfrm) {
            if (cfrm) {
                var indexToRemove = self.helper.selectedRule.symbolizers.indexOf(symbolizer);
                if (indexToRemove > -1) {
                    self.helper.selectedRule.symbolizers.splice(indexToRemove, 1);
                }
            }
        });
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

    self.setFilterMode = function () {
        self.filter.showFilterTextArea = (self.filter.filterMode !== 'simple');
    };

    /**
     * Binding action for checkbox to enable or disable filter in current rule.
     *
     */
    self.applyFilter = function () {
        if (self.filter.filtersEnabled && self.filter.filterMode === 'simple') {
            //apply current filter to the model
            var strQuery = '';
            var operator = '';
            for (var i = 0; i < self.filter.filters.length; i++) {
                var filter = self.filter.filters[i];
                if (filter.attribute !== '') {
                    if (filter.comparator === 'BETWEEN') {
                        if (filter.value.indexOf(',') !== -1) {
                            var arr = filter.value.split(',');
                            if (arr.length === 2 && arr[0].trim() !== '' && arr[1].trim() !== '') {
                                strQuery += operator + '\"' + filter.attribute + '\"' + ' ' + filter.comparator + ' ' + arr[0] + ' AND ' + arr[1];
                            }
                        }
                    } else {
                        var strFilter = filter.value;
                        //escape CQL quote from the ui value before apply
                        if (isNaN(strFilter) && strFilter.indexOf("'") !== -1) {
                            var find = "'";
                            var re = new RegExp(find, 'g');
                            strFilter = strFilter.replace(re, "\\'");
                        }
                        strQuery += operator + '\"' + filter.attribute + '\"' + ' ' + filter.comparator + ' \'' + strFilter + '\'';
                    }
                    if (filter.operator !== '') {
                        operator = ' ' + filter.operator + ' ';
                    }
                }
            }
            if (strQuery !== '') {
                self.helper.selectedRule.filter = strQuery;
            }
        } else {
            //remove filter for current model
            self.helper.selectedRule.filter = null;
        }
    };

    /**
     * Utility function to convert OpenLayers comparison type to CQL comparator.
     *
     * This is the list of type of the comparison in OpenLayers
     *
     olext.Filter.Comparison.EQUAL_TO = “==”;
     olext.Filter.Comparison.NOT_EQUAL_TO = “!=”;
     olext.Filter.Comparison.LESS_THAN = “<”;
     olext.Filter.Comparison.GREATER_THAN = “>”;
     olext.Filter.Comparison.LESS_THAN_OR_EQUAL_TO = “<=”;
     olext.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO = “>=”;
     olext.Filter.Comparison.BETWEEN = “..”;
     olext.Filter.Comparison.LIKE = “~”;
     olext.Filter.Comparison.ILIKE = “ILIKE”;
     olext.Filter.Comparison.IS_NULL = “NULL”;
     */
    var convertOLComparatorToCQL = function (olType) {
        var comparator;
        if (olType === '==') {
            comparator = '=';
        } else if (olType === '..') {
            comparator = 'BETWEEN';
        } else if (olType === '~') {
            comparator = 'LIKE';
        } else if (olType === '!=') {
            comparator = '<>';
        } else {
            comparator = olType;
        }
        return comparator;
    };

    /**
     * Utility function to convert OpenLayers operator
     * @param olType
     * @returns {*}
     */
    var convertOLOperatorToCQL = function (olType) {
        var operator;
        if (olType === '&&') {
            operator = 'AND';
        } else if (olType === '||') {
            operator = 'OR';
        } else if (olType === '!') {
            operator = 'NOT';
        }
        return operator;
    };

    /**
     * recursive function to resolve OpenLayers filter to current model.
     * @param obj
     * @param arrayRes
     */
    var recursiveResolveFilter = function (obj, arrayRes) {
        if (obj.CLASS_NAME === 'olext.Filter.Logical') {
            if (obj.filters && obj.filters.length === 2) {
                if (obj.filters[0].CLASS_NAME === 'olext.Filter.Comparison' &&
                    obj.filters[1].CLASS_NAME === 'olext.Filter.Comparison') {
                    var comparator1 = convertOLComparatorToCQL(obj.filters[0].type);
                    var value1;
                    if (comparator1 === 'BETWEEN') {
                        value1 = obj.filters[0].lowerBoundary + ',' + obj.filters[0].upperBoundary;
                    } else {
                        value1 = obj.filters[0].value;
                    }
                    var comparator2 = convertOLComparatorToCQL(obj.filters[1].type);
                    var value2;
                    if (comparator2 === 'BETWEEN') {
                        value2 = obj.filters[1].lowerBoundary + ',' + obj.filters[1].upperBoundary;
                    } else {
                        value2 = obj.filters[1].value;
                    }
                    var operator = convertOLOperatorToCQL(obj.type);
                    arrayRes.push({
                        "attribute": obj.filters[0].property,
                        "comparator": comparator1,
                        "value": value1,
                        "operator": operator
                    });
                    arrayRes.push({
                        "attribute": obj.filters[1].property,
                        "comparator": comparator2,
                        "value": value2,
                        "operator": ''
                    });
                } else if (obj.filters[0].CLASS_NAME === 'olext.Filter.Logical' &&
                    obj.filters[1].CLASS_NAME === 'olext.Filter.Comparison') {
                    recursiveResolveFilter(obj.filters[0], arrayRes);
                    var op = convertOLOperatorToCQL(obj.type);
                    arrayRes[arrayRes.length - 1].operator = op;
                    var comparator = convertOLComparatorToCQL(obj.filters[1].type);
                    var value;
                    if (comparator === 'BETWEEN') {
                        value = obj.filters[1].lowerBoundary + ',' + obj.filters[1].upperBoundary;
                    } else {
                        value = obj.filters[1].value;
                    }
                    arrayRes.push({
                        "attribute": obj.filters[1].property,
                        "comparator": comparator,
                        "value": value,
                        "operator": ''
                    });
                }
            }
        }
    };

    /**
     * build an array of query filters for given OpenLayers Filter object.
     * @param olfilter
     * @returns {Array}
     */
    var convertOLFilterToArray = function (olfilter) {
        var resultArray = [];
        if (olfilter.CLASS_NAME === 'olext.Filter.Comparison') {
            var comparator = convertOLComparatorToCQL(olfilter.type);
            var value;
            if (comparator === 'BETWEEN') {
                value = olfilter.lowerBoundary + ',' + olfilter.upperBoundary;
            } else {
                value = olfilter.value;
            }
            var q = {
                "attribute": olfilter.property,
                "comparator": comparator,
                "value": value,
                "operator": ''
            };
            resultArray.push(q);
        } else if (olfilter.CLASS_NAME === 'olext.Filter.Logical') {
            recursiveResolveFilter(olfilter, resultArray);
        }
        return resultArray;
    };

    /**
     * Extract and returns all numeric fields from data properties.
     * @param properties
     * @returns {Array}
     */
    var getOnlyNumbersFields = function (properties) {
        var arrayRes = [];
        if (properties && properties.length > 0) {
            for (var i = 0; i < properties.length; i++) {
                if (properties[i].type === 'java.lang.Double' ||
                    properties[i].type === 'java.lang.Integer' ||
                    properties[i].type === 'java.lang.Float' ||
                    properties[i].type === 'java.lang.Number' ||
                    properties[i].type === 'java.lang.Long' ||
                    properties[i].type === 'java.lang.Short') {
                    arrayRes.push(properties[i]);
                }
            }
        }
        return arrayRes;
    };

    /**
     * Returns all fields excepts the geometry properties.
     * @param properties
     * @returns {Array}
     */
    var getFieldsExcludeGeometry = function (properties) {
        var arrayRes = [];
        if (properties && properties.length > 0) {
            for (var i = 0; i < properties.length; i++) {
                //skip geometry field
                if (properties[i].type.indexOf('com.vividsolutions') === -1) {
                    arrayRes.push(properties[i]);
                }
            }
        }
        return arrayRes;
    };

    /**
     * Called at init the ng-repeat for filters to read the current rule's filter and affect the local variable.
     */
    self.restoreFilters = function () {
        if (self.helper.selectedRule.filter) {
            var cql = self.helper.selectedRule.filter;
            if (cql.indexOf('\\\'') !== -1) {
                var find = "\\\\\'";
                var re = new RegExp(find, 'g');
                cql = cql.replace(re, "''");
            }

            //@TODO ol3 does not have any cql formatter, so needs to write one. good luck.
            var format = new olext.Format.CQL();
            var olfilter;
            var readfailed = false;
            try {
                olfilter = format.read(cql);
            } catch (err) {
                console.error(err);
                readfailed = true;
            }
            if (olfilter) {
                self.filter.filters = convertOLFilterToArray(olfilter);
                self.filter.filtersEnabled = true;
                self.filter.filterMode = 'simple';
            } else {
                self.filter.filtersEnabled = true;
                self.filter.filterMode = 'expert';
                self.filter.filters = [{
                    "attribute": "",
                    "comparator": "=",
                    "value": "",
                    "operator": ''
                }];
                //show textarea instead of auto form inputs in case of read fail
                self.filter.showFilterTextArea = readfailed;
            }
        } else {
            self.filter.filtersEnabled = false;
            self.filter.filters = [{
                "attribute": "",
                "comparator": "=",
                "value": "",
                "operator": ''
            }];
        }
    };

    /**
     * Binding action for select in filter expression to add a new filter object.
     * @param operator
     */
    self.addNewFilter = function (operator, index) {
        if (operator !== '' && (index + 1) === self.filter.filters.length) {
            var filter = {
                "attribute": "",
                "comparator": "=",
                "value": "",
                "operator": ''
            };
            self.filter.filters.push(filter);
        } else if (operator === '') {
            self.filter.filters = self.filter.filters.slice(0, index + 1);
        }
    };

    self.loadDataProperties = function () {
        if (!self.selectedDataRef.dataLayer && self.selectedDataRef.dataLayer.id) {
            return;
        }

        Examind.datas.getDataDescription(self.selectedDataRef.dataLayer.id)
            .then(function (response) {
                self.selectedDataProperties.dataProperties = response.data;
                self.selectedDataProperties.attributesTypeNumber = getOnlyNumbersFields(response.data.properties);
                self.selectedDataProperties.attributesExcludeGeometry = getFieldsExcludeGeometry(response.data.properties);
                if (self.selectedDataProperties.attributesTypeNumber.length > 0) {
                    self.autoIntervalValues.attr = self.selectedDataProperties.attributesTypeNumber[0].name;
                }
                if (self.selectedDataProperties.attributesExcludeGeometry.length > 0) {
                    self.autoUniqueValues.attr = self.selectedDataProperties.attributesExcludeGeometry[0].name;
                }
            }, function () {//error
                $translate('style.editor.msg.error.data.properties')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    var choosePaletteVector = function (index, paletteObj) {
        paletteObj.img_palette = 'img/palette' + index + '.png';
        paletteObj.index = index;

        paletteObj.colors = [];
        switch (index) {
            case 1:
                paletteObj.colors.push('#e52520', '#ffde00', '#95c11f', '#1d71b8', '#662483');
                break;
            case 2:
                paletteObj.colors.push('#3F3460', '#EC1876');
                break;
            case 3:
                paletteObj.colors.push('#036531', '#FDF01A');
                break;
            case 4:
                paletteObj.colors.push('#2d2e83', '#1d71b8', '#ffde00', '#e52520');
                break;
            case 5:
                paletteObj.colors.push('#000000', '#FFFFFF');
                break;
            default:
                break;
        }
    };

    self.choosePaletteVectorInterval = function (index) {
        choosePaletteVector(index, self.autoIntervalValues.palette);
    };

    self.choosePaletteVectorUnique = function (index) {
        choosePaletteVector(index, self.autoUniqueValues.palette);
    };

    /**
     * Restore the default palette value in select component in case of custom palette.
     */
    self.affectDefaultPalette = function () {
        if (self.autoIntervalValues.customPalette.enabled) {
            self.choosePaletteVectorInterval(0);
        }
        if (self.autoUniqueValues.customPalette.enabled) {
            self.choosePaletteVectorUnique(0);
        }
    };

    /**
     * Function to allow the user to go back to rules list using the breadcrumb after opening a rule.
     *
     */
    self.goBackToRulesList = function () {
        self.helper.enableRuleEditor = false;
        self.helper.enableAutoIntervalEditor = false;
        self.helper.enableAutoUniqueEditor = false;
    };

    /**
     * proceed to generate rules automatically for intervals and apply on current style.
     */
    self.generateAutoInterval = function () {

        if (!self.selectedDataRef.dataLayer || self.autoIntervalValues.attr === "") {
            return;
        }

        //current data id
        var dataId = self.selectedDataRef.dataLayer.id;
        // selected numeric field
        var fieldName = self.autoIntervalValues.attr;

        //intervals count
        var nbIntervals = self.autoIntervalValues.nbIntervals;
        //method
        var method = self.autoIntervalValues.method;
        //symbol
        var symbol = self.autoIntervalValues.symbol;

        //palette colors
        var customPalette = self.autoIntervalValues.customPalette.enabled;

        var colors = [];

        if (customPalette) {
            colors.push(self.autoIntervalValues.customPalette.color1,
                self.autoIntervalValues.customPalette.color2);
        } else {
            colors = self.autoIntervalValues.palette.colors;
        }

        if (colors.length === 0) {
            colors.push('#e52520', '#ffde00', '#95c11f', '#1d71b8', '#662483');
        }

        var reverseColors = self.autoIntervalValues.palette.reverseColors;
        if (reverseColors) {
            colors = colors.reverse();
        }

        var autoInterval = {
            "attr": fieldName,
            "nbIntervals": nbIntervals,
            "method": method,
            "symbol": symbol,
            "colors": colors
        };

        var wrapper = {
            "dataId": dataId,
            "style": self.newStyle,
            "intervalValues": autoInterval
        };

        //Now send all params to server and it will create the temporary style and returns the full style as json object.
        Examind.styles.generateAutoInterval(wrapper, self.newStyle.id)
            .then(function (response) {
                //push rules array in current newStyle object to trigger the changes on the map.
                if (response.data.rules && response.data.rules.length > 0) {
                    self.newStyle.rules = response.data.rules;
                    self.goBackToRulesList();
                }
            }, function (reason) {
                $translate('style.editor.msg.error.generate.auto.interval')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    /**
     * proceed to generate rules automatically for unique values and apply on current style.
     */
    self.generateAutoUnique = function () {
        if (!self.selectedDataRef.dataLayer || !self.selectedDataRef.dataLayer.id || self.autoUniqueValues.attr === "") {
            return;
        }

        //current data id
        var dataId = self.selectedDataRef.dataLayer.id;

        //selected field
        var fieldName = self.autoUniqueValues.attr;

        //symbol
        var symbol = self.autoUniqueValues.symbol;

        //palette colors
        var customPalette = self.autoUniqueValues.customPalette.enabled;

        var colors = [];

        if (customPalette) {
            colors.push(self.autoUniqueValues.customPalette.color1, self.autoUniqueValues.customPalette.color2);
        } else {
            colors = self.autoUniqueValues.palette.colors;
        }

        if (colors.length === 0) {
            colors.push('#e52520', '#ffde00', '#95c11f', '#1d71b8', '#662483');
        }

        var reverseColors = self.autoUniqueValues.palette.reverseColors;

        if (reverseColors) {
            colors = colors.reverse();
        }

        var autoUnique = {
            "attr": fieldName,
            "symbol": symbol,
            "colors": colors
        };

        var wrapper = {
            "dataId": dataId,
            "style": self.newStyle,
            "uniqueValues": autoUnique
        };

        //Now send all params to server and it will create the temporary style and returns the full style as json object.
        Examind.styles.generateAutoUniqueStyle(wrapper, self.newStyle.id)
            .then(function (response) {
                    //push rules array in current newStyle object to trigger the changes on the map.
                    if (response.data.rules && response.data.rules.length > 0) {
                        self.newStyle.rules = response.data.rules;
                        self.goBackToRulesList();
                    }
                }
            );
    };

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

    self.initVectorPlot = function () {
        loadPlot({
            json: {
                x: [],
                data1: []
            }
        }, '', true, 460, 250, '#chart', {top: 20, right: 10, bottom: 6, left: 50});
    };

    self.loadPlotForAttribute = function () {
        if (!self.selectedDataRef.dataLayer || !self.selectedDataRef.dataLayer.id) {
            return;
        }

        if (self.chart.attribute === '') {
            self.initVectorPlot();
            return;
        }
        // Prepare the params to get the chart data
        var parameters = {
            "values": {
                "dataId": self.selectedDataRef.dataLayer.id,
                "attribute": self.chart.attribute,
                "intervals": 20
            }
        };

        //Now send all params to server and it will create the temporary style
        // and returns the full style as json object.

        Examind.styles.getChartData(parameters)
            .then(function (response) {
                self.chart.min = response.data.minimum;
                self.chart.max = response.data.maximum;
                if (response.data.mapping) {
                    var xarray = [];
                    var yarray = [];
                    for (var key in response.data.mapping) {
                        if (response.data.mapping.hasOwnProperty(key)) {
                            xarray.push(key === '' ? 'empty' : key);
                            yarray.push(response.data.mapping[key]);
                        }
                    }
                    var dataRes = {
                        json: {
                            x: xarray,
                            data1: yarray
                        }
                    };
                    loadPlot(dataRes,
                        self.chart.attribute,
                        true, 460, 250, '#chart', {top: 20, right: 10, bottom: 6, left: 50});
                }
            });
    };

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