angular.module('examind.components.wizardDataImport.s63.step5', [
    'examind-instance',
    'examind.components.preview.map'
])
    .controller('Step5S63Controller', Step5S63Controller)
    .directive('step5S63', step5S63Directive);

function step5S63Directive() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step5S63Controller',
        controllerAs: 'ctrl',
        templateUrl: 'components/wizard-data-import/s63-steps/step5-s63/step5-s63.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step5S63Controller($scope, $location, WizardAddDataService, Examind) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[4];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step4S63) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.getLayerGroup = function (cells, preview) {
        var layerGroup = new ol.layer.Group();
        var features = [];
        var extent = ol.extent.createEmpty();

        angular.forEach(cells, function (cell) {

            var geomatry = ol.geom.Polygon.fromExtent(cell.bbox);

            var feature = new ol.Feature({
                name: cell.name,
                geometry: geomatry
            });

            feature.getGeometry().transform('EPSG:4326', 'EPSG:3857');

            ol.extent.extend(extent, cell.bbox);

            features.push(feature);

        });

        var vectorSource = new ol.source.Vector({
            features: features
        });

        var vectorLayer = new ol.layer.Vector({
            source: vectorSource,
            style: new ol.style.Style({
                stroke: new ol.style.Stroke({
                    color: '#66AADD',
                    width: 2.25
                })
            })
        });

        layerGroup.getLayers().push(vectorLayer);

        preview.layer = layerGroup;

        preview.extent = extent;
    };

    var notAllDataInstalled = function (items) {
        var find = false;
        for (var i = 0; i < items.length; i++) {
            if (!items[i].isInstalled) {
                find = true;
                break;
            }
        }
        return find;
    };

    self.stepObject.finish = function (returnToDataDashboard) {
        if (self.wizardValues.step3S63.mediaExchange.exchangeSetId) {
            Examind.s63.stepMediaSelection(self.s63.dataSet.name, self.s63.procedureId)
                .then(function (response) {
                    if (notAllDataInstalled(response.data.status)) {
                        delete self.wizardValues.step4S63;
                        delete self.wizardValues.step5S63;
                        WizardAddDataService.goToStep(3);
                    } else {
                        if (angular.isFunction(returnToDataDashboard)) {
                            returnToDataDashboard();
                        }
                    }

                });
        } else {
            if (angular.isFunction(returnToDataDashboard)) {
                returnToDataDashboard();
            }
        }
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        if (!self.wizardValues.step5S63) {
            self.wizardValues.step5S63 = {
                mediaExchange: null
            };

            self.s63 = self.wizardValues.step3S63.s63;
            self.ExchangeSetReport = self.wizardValues.step4S63.ExchangeSetReport;

            self.preview = {
                layer: undefined,
                extent: undefined,
                projection: 'EPSG:3857',
                layerOnly: false
            };

            self.getLayerGroup(self.ExchangeSetReport.cells, self.preview);
        }
    };

    self.init();

}