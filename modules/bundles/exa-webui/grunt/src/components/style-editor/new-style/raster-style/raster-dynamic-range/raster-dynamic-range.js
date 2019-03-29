angular.module('examind.components.style.editor.new.raster.dynamic.range', [])
    .controller('RasterDynamicController', RasterDynamicController)
    .directive('rasterDynamicRange', rasterDynamicDirective);

function rasterDynamicDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/new-style/raster-style/raster-dynamic-range/raster-dynamic-range.html",
        controller: 'RasterDynamicController',
        controllerAs: "dynamicRangeCtrl",
        scope: {
            rasterDynamic: "=",
            selectedDataProperties: "=",
            helper: "="
        }
    };
}

function RasterDynamicController($scope) {
    var self = this;

    self.rasterDynamic = $scope.rasterDynamic;

    self.selectedDataProperties = $scope.selectedDataProperties;

    self.helper = $scope.helper;

    /**
     * Binding action to apply dynamic range
     */
    self.generateDynamicRange = function () {
        self.helper.selectedRule.symbolizers[0].channels = self.rasterDynamic.channels;
    };

}