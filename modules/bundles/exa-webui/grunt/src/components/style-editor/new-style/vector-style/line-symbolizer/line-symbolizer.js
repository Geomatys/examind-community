angular.module('examind.components.style.editor.new.vector.line', [])
    .controller('LineSymbolizerController', LineSymbolizerController)
    .directive('lineSymbolizer', lineSymbolizerDirective);

function lineSymbolizerDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/style-editor/new-style/vector-style/line-symbolizer/line-symbolizer.html',
        controller: 'LineSymbolizerController',
        controllerAs: 'lineSymCtrl',
        scope: {
            attributesTypeNumber: "=",
            symbolizer: "="
        }
    };
}

function LineSymbolizerController($scope, NewStyleServices) {
    var self = this;

    self.symbolizer = $scope.symbolizer;

    self.attributesTypeNumber = $scope.attributesTypeNumber;

    self.affectAlpha = NewStyleServices.affectAlpha;

    self.isExpressionNumber = NewStyleServices.isExpressionNumber;

    self.setAttrToInputWidth = NewStyleServices.setAttrToInputWidth;

    self.addStrokeDashArray = NewStyleServices.addStrokeDashArray;
}