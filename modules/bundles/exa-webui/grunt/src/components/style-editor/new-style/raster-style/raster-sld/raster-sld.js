angular.module('examind.components.style.editor.new.raster.sld', [])
    .controller('RasterSldController', RasterSldController)
    .directive('rasterSld', rasterSldDirective);

function rasterSldDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/new-style/raster-style/raster-sld/raster-sld.html",
        controller: 'RasterSldController',
        controllerAs: "rasterSldCtrl",
        scope: {
            rasterPalette: "=",
            selectedDataProperties: "=",
            helper: "=",
            drawThresholds: "&",
            displayNewStyle: "&",
            generateRasterPalette: "&"
        }
    };
}

function RasterSldController($scope, $timeout, $modal) {
    var self = this;

    self.rasterPalette = $scope.rasterPalette;

    self.selectedDataProperties = $scope.selectedDataProperties;

    self.helper = $scope.helper;

    self.drawThresholds = $scope.drawThresholds();

    self.displayNewStyle = $scope.displayNewStyle();

    self.generateRasterPalette = $scope.generateRasterPalette();

    /**
     * Fix rzslider bug with angular on value changed for band selector.
     */
    self.fixRZSlider = function () {
        self.rasterPalette.palette.rasterMinValue = Number(self.rasterPalette.band.selected.minValue);
        self.rasterPalette.palette.rasterMaxValue = Number(self.rasterPalette.band.selected.maxValue);
    };

    self.choosePalette = function (index) {
        self.rasterPalette.palette.img_palette = 'img/palette' + index + '.png';
        self.rasterPalette.palette.index = index;
    };

    /**
     * Remove repartition entry and apply this change on the histogram.
     * @param point
     */
    self.removeRepartitionEntry = function (point) {
        if (self.rasterPalette.repartition) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function () {
                        return "dialog.message.confirm.delete.repartitionEntry";
                    }
                }
            });

            dlg.result.then(function (cfrm) {
                if (cfrm) {
                    var indexToRemove = self.rasterPalette.repartition.indexOf(point);
                    if (indexToRemove > -1) {
                        self.rasterPalette.repartition.splice(indexToRemove, 1);
                    }
                    //remove threshold vertical line on graph.
                    if (point.data) {
                        for (var j = 0; j < self.rasterPalette.dataXArray.length; j++) {
                            if (self.rasterPalette.dataXArray[j] >= point.data) {
                                window.c3chart.xgrids.remove({value: j});
                                break;
                            }
                        }
                    } else {
                        self.helper.selectedRule.symbolizers[0].colorMap.function.nanColor = null;
                    }
                }
            });
        }
    };

    /**
     * Action to add new value in colorMap
     */
    self.addColorMapEntry = function () {
        self.rasterPalette.repartition.push({data: 255, color: '#000000'});
    };

    /**
     * Apply RGB composition for current style and clean up colormap and rasterPalette.repartition.
     */
    self.applyRGBComposition = function () {
        var rgbChannels = self.rasterPalette.rgbChannels;
        var isValid = true;
        //@TODO confirm with sld conformance, is it necessary to check channel's band not empty?
        for (var i = 0; i < rgbChannels.length; i++) {
            if (rgbChannels[i].name === '') {
                isValid = false;
                break;
            }
        }
        if (!isValid) {
            alert('Please select a band for all channels!');
            return;
        } else {
            //Apply rgb channels to selected rule
            self.helper.selectedRule.symbolizers[0].channelSelection = {
                greyChannel: null,
                rgbChannels: self.rasterPalette.rgbChannels
            };
            //clean colorMap for selected rule
            self.rasterPalette.repartition = undefined;
            self.helper.selectedRule.symbolizers[0].colorMap = undefined;
        }
    };

    /**
     * Apply grayscale channel for current style and clean up colormap and rasterPalette.repartition.
     */
    self.applyGrayscaleComposition = function () {
        self.helper.selectedRule.symbolizers[0].channelSelection = {
            greyChannel: self.rasterPalette.greyChannel,
            rgbChannels: null
        };
        //clean colorMap for selected rule
        self.rasterPalette.repartition = undefined;
        self.helper.selectedRule.symbolizers[0].colorMap = undefined;
    };

    /**
     * Hack to fix color picker problem with transparent value
     * empty is transparent but examind not support empty color
     * so we need to replace empty string by #00000000
     * after we need to change the reference object to trigger angular watcher
     * @param point
     * @param index
     */
    self.checkColor = function (point, index) {
        $timeout(function () {
            point.color = !point.color ? '#00000000' : point.color;
            $scope.optionsSLD.rasterPalette.repartition[index] = Object.assign({}, point);
        }, 200);
    };

}
