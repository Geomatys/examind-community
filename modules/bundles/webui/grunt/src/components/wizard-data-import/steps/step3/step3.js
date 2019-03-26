angular.module('examind.components.wizardDataImport.step3', [
    'examind.components.dataset.editor',
    'examind-instance'
])
    .controller('Step3WizardController', Step3WizardController)
    .directive('step3Wizard', Step3WizardDirective);

function Step3WizardDirective() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step3WizardController',
        controllerAs: 'step3WizCtrl',
        templateUrl: 'components/wizard-data-import/steps/step3/step3.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step3WizardController($scope, $location, $translate, Growl, Examind, WizardAddDataService) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[2];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step2) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.datasetReturnObject = {
        createDataset: null
    };

    var goToNextStep = function (goToNextStepFn, stepNum) {
        if (!self.wizardValues.step1.advConfig.batchMode && self.wizardValues.step3.selection.dataset && self.wizardValues.step2.acceptedData) {
            var dataIds = self.wizardValues.step2.acceptedData.map(function (value) {
                return value.id;
            });
            Examind.datas.updateDataDataset(dataIds,
                self.wizardValues.step3.selection.dataset.id)
                .then(angular.noop, function () {
                    $translate('wiz.data.import.step3.msg.error.link.data.dataset')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
        }

        if (angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    // Implement the conditions to disable next button in this step
    self.stepObject.disableNextBtn = function () {
        return !self.datasetReturnObject.createDataset ? !self.wizardValues.step3.selection.dataset : false;
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        if (self.datasetReturnObject.createDataset && angular.isFunction(self.datasetReturnObject.createDataset)) {
            self.datasetReturnObject.createDataset(goToNextStep, goToNextStepFn, stepNum);
        } else {
            goToNextStep(goToNextStepFn, stepNum);
        }
    };

    self.stepObject.goToPreviousStep = function (goToPreviousStepFn, stepNum) {
        // Remove the information of this step
        delete self.wizardValues.step3;

        if (angular.isFunction(goToPreviousStepFn) && stepNum) {
            goToPreviousStepFn(stepNum);
        }
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        if (!self.wizardValues.step3) {
            self.wizardValues.step3 = {
                selection: {
                    dataset: null,
                    datasetType: 'ALL',
                    newDataset: false
                }
            };
        }
    };

    self.init();

}

