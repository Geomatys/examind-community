angular.module('examind.components.style.editor.new.vector.point', [])
    .controller('PointSymbolizerController', PointSymbolizerController)
    .directive('pointSymbolizer', pointSymbolizerDirective);

function pointSymbolizerDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/style-editor/new-style/vector-style/point-symbolizer/point-symbolizer.html',
        controller: 'PointSymbolizerController',
        controllerAs: 'pointSymCtrl',
        scope: {
            attributesTypeNumber: '=',
            symbolizer: "="
        }
    };
}

function PointSymbolizerController($scope, NewStyleServices) {
    var self = this;

    self.symbolizer = $scope.symbolizer;

    self.attributesTypeNumber = $scope.attributesTypeNumber;

    self.isTTFValue = NewStyleServices.isTTFValue;

    self.fontsMapping = NewStyleServices.fontsMapping;

    self.resolveClassForCode = NewStyleServices.resolveClassForCode;

    self.fontsCodes = NewStyleServices.getFontsCodes();

    self.affectAlpha = NewStyleServices.affectAlpha;

    self.isExpressionNumber = NewStyleServices.isExpressionNumber;

    self.setAttrToInputSize = NewStyleServices.setAttrToInputSize;

    self.setAttrToInputRotation = NewStyleServices.setAttrToInputRotation;

    self.setAttrToInputOpacity = NewStyleServices.setAttrToInputOpacity;

}