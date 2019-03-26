angular.module('examind.components.wizardDataImport.s63.step2', [
    'examind.components.dataset.editor'
])
    .controller('Step2S63Controller', Step2S63Controller)
    .directive('step2S63', step2S63Directive);

function step2S63Directive() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step2S63Controller',
        controllerAs: 'ctrl',
        templateUrl: 'components/wizard-data-import/s63-steps/step2-s63/step2-s63.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step2S63Controller($scope, WizardAddDataService) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[1];

    self.wizardValues = $scope.wizardValues;

    self.dataSetEdConfig = {
        showHeaderBtn: false
    };

    self.searchFilter = {
        field: 'type',
        value: 'S63'
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        if (goToNextStepFn && angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    // Implement the conditions to disable next button in this step
    self.stepObject.disableNextBtn = function () {
        return !self.wizardValues.step2S63.selection.dataset;
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        if (!self.wizardValues.step2S63) {
            self.wizardValues.step2S63 = {
                selection: {
                    dataset: null
                }
            };
        }
    };

    self.init();

}