angular.module('examind.components.wizardDataImport.s63.step3', [
    'examind-instance'
])
    .controller('Step3S63Controller', Step3S63Controller)
    .directive('step3S63', step3S63Directive);

function step3S63Directive() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step3S63Controller',
        controllerAs: 'ctrl',
        templateUrl: 'components/wizard-data-import/s63-steps/step3-s63/step3-s63.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step3S63Controller($scope, $location, Growl, WizardAddDataService, Examind) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[2];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step2S63) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.showExchangeSets = function (procedureId) {
        Examind.s63.stepMediaSelection(self.s63.dataSet.name, procedureId)
            .then(function (response) {
                self.s63.mediaExchangeSetList = response.data.status;
            });
    };

    self.startProcedureUpload = function () {
        Examind.s63.startProcedureUpload(
            self.s63.dataSet.name,
            self.s63.dsPath)
            .then(function (response) {
                self.processing = false;
                self.s63.dataExchange = response.data.status;
                self.s63.procedureId = self.s63.dataExchange.procedureId;
                if (response.data.status.type === 'media') {
                    self.showExchangeSets(self.s63.procedureId);
                } else if (response.data.status.type === 'exchangeset') {
                    self.mediaExchange.isNotMedia = true;
                    self.installExchangeSet(self.s63.procedureId)
                }
            }, function (reason) {
                self.processing = false;
                self.failure = true;
                self.failureMsg = reason.data;
                Growl('error', 'Error', 'cannot start upload procedure');
            });
    };

    self.saveMediaExchangeConfig = function (goToNextStep, goToNextStepFn, stepNum) {
        Examind.s63.stepExchangeSetConfigurationSet(
            self.s63.dataSet.name,
            self.s63.procedureId,
            self.mediaExchange.configuration).then(function () {
            Examind.s63.stepExchangeSetListing(self.s63.dataSet.name, self.s63.procedureId)
                .then(function (response) {
                    self.mediaExchange.cells = response.data.status.cells;
                    goToNextStep(goToNextStepFn, stepNum);
                });
        });
    };

    self.showAnalysisBlock = function () {
        return self.processing;
    };

    self.showFailureBlock = function () {
        return self.failure;
    };

    var goToNextStep = function (goToNextStepFn, stepNum) {
        self.wizardValues.step3S63.s63 = self.s63;
        self.wizardValues.step3S63.mediaExchange = self.mediaExchange;
        if (goToNextStepFn && angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    // Implement the conditions to disable next button in this step
    self.stepObject.disableNextBtn = function () {
        return self.currentView !== 'media-set-config';
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        self.wizardValues.step3S63.s63 = self.s63;
        self.saveMediaExchangeConfig(goToNextStep, goToNextStepFn, stepNum);
    };

    self.isCurrentView = function (view) {
        return self.currentView === view;
    };

    self.setCurrentView = function (view) {
        self.currentView = view;
    };

    self.installingInProgress = function (exchangeSetId) {
        return self.mediaExchange.exchangeSetId === exchangeSetId;
    };

    self.installExchangeSet = function () {
        Examind.s63.stepExchangeSetConfiguration(self.s63.dataSet.name, self.s63.procedureId, null)
            .then(function (response) {
                self.mediaExchange.configuration = response.data.configuration;
                self.setCurrentView('media-set-config');
            });
    };

    self.installMediaExchangeSet = function (exchangeSetId) {
        Examind.s63.stepExchangeSetConfiguration(self.s63.dataSet.name, self.s63.procedureId, exchangeSetId)
            .then(function (response) {
                self.mediaExchange.exchangeSetId = exchangeSetId;
                self.mediaExchange.configuration = response.data.configuration;
                self.setCurrentView('media-set-config');
            });
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        self.mediaExchange = {
            exchangeSetId: null,
            configuration: null,
            cells: null,
            isNotMedia: false
        };

        self.currentView = 'media-exchange-set';

        if (!self.wizardValues.step3S63) {
            self.wizardValues.step3S63 = {
                s63: null,
                mediaExchange: null
            };
            self.s63 = {
                dsPath: self.wizardValues.step1.dataSource.url,
                dataSet: self.wizardValues.step2S63.selection.dataset,
                dataExchange: null,
                procedureId: null,
                mediaExchangeSetList: []

            };
            self.processing = true;
            self.failure = false;
            self.startProcedureUpload();
        } else {
            self.s63 = self.wizardValues.step3S63.s63;
            self.showExchangeSets(self.s63.procedureId);
        }
    };

    self.init();

}