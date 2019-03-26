angular.module('examind.components.style.editor.new.vector.text', [])
    .controller('TextSymbolizerController', TextSymbolizerController)
    .directive('textSymbolizer', textSymbolizerDirective);

function textSymbolizerDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/new-style/vector-style/text-symbolizer/text-symbolizer.html",
        controller: 'TextSymbolizerController',
        controllerAs: "textSymCtrl",
        scope: {
            attributesExcludeGeometry: "=",
            attributesTypeNumber: "=",
            loadDataProperties: "&",
            symbolizer: "="
        }
    };
}

function TextSymbolizerController($scope, NewStyleServices) {
    var self = this;

    self.symbolizer = $scope.symbolizer;

    self.attributesExcludeGeometry = $scope.attributesExcludeGeometry;

    self.attributesTypeNumber = $scope.attributesTypeNumber;

    self.loadDataProperties = $scope.loadDataProperties();

    self.affectAlpha = NewStyleServices.affectAlpha;

    self.initFontFamilies = NewStyleServices.initFontFamilies;

    self.isExpressionNumber = NewStyleServices.isExpressionNumber;

    self.setAttrToInputSize = NewStyleServices.setAttrToInputSize;

}