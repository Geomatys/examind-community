angular.module('examind.components.wizardDataImport', [
    'examind-instance',
    'examind.components.wizardDataImport.step1',
    'examind.components.wizardDataImport.step2',
    'examind.components.wizardDataImport.step3',
    'examind.components.wizardDataImport.step4',
    'examind.components.wizardDataImport.step5',
    'examind.components.wizardDataImport.s63.step2',
    'examind.components.wizardDataImport.s63.step3',
    'examind.components.wizardDataImport.s63.step4',
    'examind.components.wizardDataImport.s63.step5'
])
    .controller('WizardDataImportController', WizardDataImportController)
    .factory('StepObject', StepObjectFactory)
    .factory('WizardAddDataService', WizardAddDataServiceFactory)
    .directive('wizardDataImport', wizardDataImportDirective);

function wizardDataImportDirective() {
    return {
        restrict: "E",
        controller: 'WizardDataImportController',
        controllerAs: 'wizDataImportCtrl',
        templateUrl: 'components/wizard-data-import/wizard-data-import.html'
    };
}

function WizardDataImportController($scope, $location, $interval, $modal, StepObject, Examind, WizardAddDataService) {
    var self = this;

    self.currentStep = Number($location.search().step) || 1;

    self.wizardValues = {};

    self.stepsConfig = WizardAddDataService.stepsConfig;

    self.stepsConfig.stepsList = [
        new StepObject('step-1', '1', 1, 'wiz.data.import.label.select.source', '<step1-wizard></step1-wizard>'),
        new StepObject('step-2', '2', 2, 'wiz.data.import.label.data.visualization', '<step2-wizard></step2-wizard>'),
        new StepObject('step-3', '3', 3, 'wiz.data.import.label.select.dataset', '<step3-wizard></step3-wizard>'),
        new StepObject('step-4', '4', 4, 'wiz.data.import.label.fill.metadata', '<step4-wizard></step4-wizard>'),
        new StepObject('step-5', '5', 5, 'wiz.data.import.label.select.style', '<step5-wizard></step5-wizard>')
    ];

    self.goToStep = function (stepNumber) {
        if (stepNumber < 1 || stepNumber > self.stepsConfig.stepsList.length) {
            return;
        }

        $location.search('step', stepNumber);
        self.currentStep = stepNumber;
    };

    self.isSelectedStep = function (stepId) {
        return angular.equals(self.stepsConfig.stepsList[self.currentStep - 1].id, stepId);
    };

    self.goToNextStep = function () {
        if (self.currentStep + 1 > self.stepsConfig.stepsList.length) {
            return;
        }

        var goToNextStep = self.stepsConfig.stepsList[self.currentStep - 1].goToNextStep;
        if (goToNextStep && angular.isFunction(goToNextStep)) {
            window.setTimeout(function () {
                goToNextStep(self.goToStep, self.currentStep + 1);
            }, 400);
        }
    };

    self.goToPreviousStep = function () {
        if (self.currentStep - 1 < 1) {
            return;
        }

        var goToPreviousStep = self.stepsConfig.stepsList[self.currentStep - 1].goToPreviousStep;
        if (goToPreviousStep && angular.isFunction(goToPreviousStep)) {
            goToPreviousStep(self.goToStep, self.currentStep - 1);
        }
    };

    self.isFinishedStep = function (step) {
        return self.currentStep > step;
    };

    self.returnToDataDashboard = function () {
        $location.search('step', null);
        $location.path('/data');
    };

    self.cancel = function () {
        var dlg = $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                'keyMsg': function () {
                    return "wiz.data.import.msg.confirm.cancel";
                }
            }
        });
        dlg.result.then(function (cfrm) {
            if (cfrm) {
                self.returnToDataDashboard();
            }
        });

    };

    self.finish = function () {
        var finish = self.stepsConfig.stepsList[self.stepsConfig.stepsList.length - 1].finish;
        if (finish && angular.isFunction(finish)) {
            finish(self.returnToDataDashboard);
        }
    };

    self.showBtn = function (btnName) {
        if (btnName === 'cancel') {
            return true;
        } else if (btnName === 'finish') {
            return self.currentStep === self.stepsConfig.stepsList.length;
        } else if (btnName === 'next') {
            return self.currentStep !== self.stepsConfig.stepsList.length && self.stepsConfig.stepsList[self.currentStep - 1].showNextBtn;

        } else if (btnName === 'previous') {
            return self.currentStep !== 1 && self.stepsConfig.stepsList[self.currentStep - 1].showPreviousBtn;
        }
    };

    self.disableNextBtn = function () {
        var stepsList = self.stepsConfig.stepsList;
        var index = self.currentStep - 1;
        return stepsList[index].disableNextBtn();
    };

    self.disableFinishBtn = function () {
        var stepsList = self.stepsConfig.stepsList;
        var index = self.currentStep - 1;
        return stepsList[index].disableFinishBtn();
    };

    self.clear = function () {

        if (!self.wizardValues.step5 || !self.wizardValues.step5.finished) {
            // Remove datasource
            if (self.wizardValues.step1 && self.wizardValues.step1.dataSource.id) {
                $interval.cancel(self.wizardValues.step1.stores.asyncComputeStoresInterval);
                Examind.dataSources.delete(self.wizardValues.step1.dataSource.id);
            }
            // Remove stores
            if (self.wizardValues.step2 && self.wizardValues.step2.storesRef &&
                self.wizardValues.step2.storesRef.storeList) {
                angular.forEach(self.wizardValues.step2.storesRef.storeList, function (store) {
                    Examind.providers.delete(store.id);
                });
            }

            // Delete created dataset
            if (self.wizardValues.step3 && self.wizardValues.step3.selection.newDataset && self.wizardValues.step3.selection.dataset) {
                Examind.datas.deleteDataset(self.wizardValues.step3.selection.dataset.id);
            }

            // Delete created metadataModel
            if (self.wizardValues.step4 && self.wizardValues.step4.metadataModelId) {
                Examind.metadata.deleteById(self.wizardValues.step4.metadataModelId);
            }
        }
    };

    WizardAddDataService.goToStep = self.goToStep;

    $scope.$on("$destroy", function () {
        self.clear();
    });

}

function StepObjectFactory() {
    // Prototype function for step object
    function WizardStep(id, name, index, desc, template) {
        this.id = id;
        this.name = name;
        this.index = index;
        this.desc = desc;
        this.template = template;
        this.showNextBtn = true;
        this.showPreviousBtn = true;
    }

    // Expose the methods
    WizardStep.prototype.goToNextStep = angular.noop;
    WizardStep.prototype.goToPreviousStep = angular.noop;
    WizardStep.prototype.finish = angular.noop;
    WizardStep.prototype.disableNextBtn = angular.noop;
    WizardStep.prototype.disableFinishBtn = angular.noop;

    /**
     * Return the constructor function
     */
    return WizardStep;

}

function WizardAddDataServiceFactory() {
    var self = this;

    self.stepsConfig = {
        stepsList: []
    };

    self.s63InitWizard = function () {
        self.stepsConfig.stepsList[1].id = 'step-2-s63';
        self.stepsConfig.stepsList[1].desc = 'wiz.data.import.label.s63.select.dataset';
        self.stepsConfig.stepsList[1].showPreviousBtn = false;
        self.stepsConfig.stepsList[2].id = 'step-3-s63';
        self.stepsConfig.stepsList[2].desc = 'wiz.data.import.label.s63.install.data';
        self.stepsConfig.stepsList[2].showPreviousBtn = false;
        self.stepsConfig.stepsList[3].id = 'step-4-s63';
        self.stepsConfig.stepsList[3].desc = 'wiz.data.import.label.s63.cell.details';
        self.stepsConfig.stepsList[3].showPreviousBtn = false;
        self.stepsConfig.stepsList[4].id = 'step-5-s63';
        self.stepsConfig.stepsList[4].desc = 'wiz.data.import.label.s63.import.details';
        self.stepsConfig.stepsList[4].showPreviousBtn = false;
    };

    return self;

}

