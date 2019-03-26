angular.module('examind.components.style.editor.new.raster.cells', [])
    .controller('RasterCellsController', RasterCellsController)
    .directive('rasterCells', rasterCellsDirective);

function rasterCellsDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/new-style/raster-style/raster-cells/raster-cells.html",
        controller: 'RasterCellsController',
        controllerAs: "rasterCellsCtrl",
        scope: {
            selectedRule: "=",
            rasterCells: "="
        }
    };
}

function RasterCellsController($scope) {

    self.selectedRule = $scope.selectedRule;

    self.rasterCells = $scope.rasterCells;

    /**
     * Returns true if there is a cell symbolizer in given array.
     * Used to identify cellSymbolizers rule against Palette/colors rule
     * @param symbolizers
     * @returns {boolean}
     */
    self.getCellSymbolizerCell = function (symbolizers) {
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

    self.styleBtnSelected = {"color": '#ffffff', "background-color": '#c1c1c1'};

    self.styleBtnDefault = {"color": '#333333', "background-color": '#ffffff'};

    /**
     * init the font model for symbolizer text.
     */
    self.initFontFamilies = function(symbolizer) {
        if (!symbolizer.font) {
            symbolizer.font = {};
        }
        if (!symbolizer.font.family) {
            symbolizer.font.family = [];
        }
    };

    /**
     * Returns true if there is a cell symbolizer in given array.
     * Used to identify cellSymbolizers rule against Palette/colors rule
     * @param symbolizers
     * @returns {boolean}
     */
    var existsCellSymbolizer = function(symbolizers){
        if(symbolizers) {
            for(var i=0;i<symbolizers.length;i++){
                var symb = symbolizers[i];
                if(symb['@symbol']==='cell'){
                    return symb;
                }
            }
        }
        return null;
    };

    /**
     * Apply and bind cell point symbolizer for current style
     */
    self.applyCellPointSymbolizer = function(){
        var cellSymbol = existsCellSymbolizer(self.selectedRule.symbolizers);
        cellSymbol.rule.symbolizers[0] = self.rasterCells.pointSymbol;
    };

    /**
     * Apply and bind cell text symbolizer for current style
     */
    self.applyCellTextSymbolizer = function(){
        var cellSymbol = existsCellSymbolizer(self.selectedRule.symbolizers);
        cellSymbol.rule.symbolizers[0] = self.rasterCells.textSymbol;
    };

}