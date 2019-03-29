angular.module('examind.components.style.editor.new.vector.polygon', [])
    .controller('PolygonSymbolizerController', PolygonSymbolizerController)
    .directive('polygonSymbolizer', polygonSymbolizerDirective);

function polygonSymbolizerDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/new-style/vector-style/polygon-symbolizer/polygon-symbolizer.html",
        controller: 'PolygonSymbolizerController',
        controllerAs: "polygonSymCtrl",
        scope: {
            attributesTypeNumber: '=',
            symbolizer: "="
        }
    };
}

function PolygonSymbolizerController($scope, NewStyleServices) {
    var self = this;

    self.symbolizer = $scope.symbolizer;

    self.attributesTypeNumber = $scope.attributesTypeNumber;

    self.affectAlpha = NewStyleServices.affectAlpha;

    self.setAttrToInputWidth = NewStyleServices.setAttrToInputWidth;

    self.addStrokeDashArray = NewStyleServices.addStrokeDashArray;

    self.isExpressionNumber = NewStyleServices.isExpressionNumber;
}