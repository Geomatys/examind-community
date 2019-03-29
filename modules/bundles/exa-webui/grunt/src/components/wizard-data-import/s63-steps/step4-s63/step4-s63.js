angular.module('examind.components.wizardDataImport.s63.step4', [
    'examind-instance',
    'examind.components.preview.map'
])
    .controller('Step4S63Controller', Step4S63Controller)
    .directive('step4S63', step4S63Directive);

function step4S63Directive() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step4S63Controller',
        controllerAs: 'ctrl',
        templateUrl: 'components/wizard-data-import/s63-steps/step4-s63/step4-s63.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step4S63Controller($scope, $location, WizardAddDataService, Examind) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[3];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step3S63) {
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

    self.stepExchangeSetReport = function () {
        Examind.s63.stepExchangeSetReport(self.s63.dataSet.name, self.s63.procedureId)
            .then(function (response) {
                self.wizardValues.step4S63.ExchangeSetReport.cells = response.data.status.cells;
            });
    };

    var goToNextStep = function (goToNextStepFn, stepNum) {
        if (goToNextStepFn && angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        goToNextStep(goToNextStepFn, stepNum);
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        if (!self.wizardValues.step4S63) {
            self.wizardValues.step4S63 = {
                ExchangeSetReport: {
                    cells: []
                }
            };

            self.s63 = self.wizardValues.step3S63.s63;
            self.mediaExchange = self.wizardValues.step3S63.mediaExchange;
            self.preview = {
                layer: undefined,
                extent: undefined,
                projection: 'EPSG:3857',
                layerOnly: false
            };

            self.getLayerGroup(self.mediaExchange.cells, self.preview);

            self.stepExchangeSetReport();
        } else {
            self.s63 = self.wizardValues.step3S63.s63;
            self.showExchangeSets(self.s63.procedureId);
        }
    };

    self.init();

}