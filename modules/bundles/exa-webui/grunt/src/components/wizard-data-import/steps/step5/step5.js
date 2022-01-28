angular.module('examind.components.wizardDataImport.step5', [
    'examind.components.style.editor',
    'examind-instance'
])
    .controller('Step5WizardController', Step5WizardController)
    .directive('step5Wizard', Step5WizardDirective);

function Step5WizardDirective() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step5WizardController',
        controllerAs: 'step5WizCtrl',
        templateUrl: 'components/wizard-data-import/steps/step5/step5.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step5WizardController($scope, $location, $translate, $q, Growl, Examind, WizardAddDataService) {
    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[4];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step4) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.wizardValues.step5 = {
        styleId: null
    };

    self.dataLayersPagination = {
        show: true,
        current: 1,
        total: 5
    };

    // Lis of data layers
    self.dataLayers = [];

    self.batchMode = false;

    self.canGoToPreviousDataLayer = function () {
        return self.dataLayersPagination.current - 1 >= 1;
    };

    self.goToPreviousDataLayer = function () {
        if (!self.canGoToPreviousDataLayer()) {
            return;
        }
        self.dataLayersPagination.current -= 1;
        var index = self.dataLayersPagination.current - 1;
        self.selectedDataRef.dataLayer = self.dataLayers[index];
        $translate('wiz.data.import.step5.msg.change.data.layer')
            .then(function (translatedMsg) {
                Growl('info', 'Info', translatedMsg + " : " + self.selectedDataRef.dataLayer.name);
            });
    };

    self.canGoToNextDataLayer = function () {
        return self.dataLayersPagination.current + 1 <= self.dataLayers.length;
    };

    self.goToNextDataLayer = function () {
        if (!self.canGoToNextDataLayer()) {
            return;
        }
        self.dataLayersPagination.current += 1;
        var index = self.dataLayersPagination.current - 1;
        self.selectedDataRef.dataLayer = self.dataLayers[index];
        $translate('wiz.data.import.step5.msg.change.data.layer')
            .then(function (translatedMsg) {
                Growl('info', 'Info', translatedMsg + " : " + self.selectedDataRef.dataLayer.name);
            });
    };

    self.associateStyleToDataLayer = function () {
        if (!self.wizardValues.step5.styleId) {
            return;
        }
        var styleId = self.wizardValues.step5.styleId;

        if (self.batchMode) {
            var promises = [];
            angular.forEach(self.dataLayers, function (dataLayer) {
                promises.push(self.linkStyleToDataLayer(styleId, dataLayer.id));
            });

            $q.all(promises)
                .then(function () {
                    $translate('wiz.data.import.step5.msg.success.link.data.style.batch')
                        .then(function (translatedMsg) {
                            Growl('success', 'Success', translatedMsg);
                        });
                }, function () {
                    $translate('wiz.data.import.step5.msg.error.link.data.style')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });

        } else {
            self.linkStyleToDataLayer(styleId, self.selectedDataRef.dataLayer.id)
                .then(function () {
                    $translate('wiz.data.import.step5.msg.success.link.data.style')
                        .then(function (translatedMsg) {
                            Growl('success', 'Success', translatedMsg);
                        });
                }, function () {
                    $translate('wiz.data.import.step5.msg.error.link.data.style')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
        }
    };

    self.linkStyleToDataLayer = function (styleId, dataId) {
        return Examind.styles.link(styleId, dataId);
    };

    /** function to handle the output of style editor
     *
     * @param style : style object returned by the style editor
     */
    self.returnStyleEditor = function (style) {
        if (style) {
            self.wizardValues.step5.styleId = style.id;
        } else {
            self.wizardValues.step5.styleId = null;
        }
    };

    self.disableAssociateBtn = function () {
        return !self.wizardValues.step5.styleId || self.wizardValues.step1.advConfig.batchMode;
    };

    // Implement the conditions to disable next button in this step
    self.stepObject.disableFinishBtn = function () {
        return false;
    };

    self.stepObject.goToPreviousStep = function (goToPreviousStepFn, stepNum) {
        // Remove the information of this step
        delete self.wizardValues.step5;

        // remove style associations
        if (self.wizardValues.step2.acceptedData) {
            var dataIds = self.wizardValues.step2.acceptedData.map(function (value) {
                return value.id;
            });
            Examind.datas.deleteStyleAssociationsMulti(dataIds);
        }

        if (angular.isFunction(goToPreviousStepFn) && stepNum) {
            goToPreviousStepFn(stepNum);
        }
    };

    self.stepObject.finish = function (returnToDataDashboard) {

        // unhide dataset metadata
        if (self.wizardValues.step3.selection.dataset && self.wizardValues.step3.selection.dataset.metadatas &&
            self.wizardValues.step3.selection.dataset.metadatas.length === 1) {
            Examind.metadata.changeHiddenProperty(false, self.wizardValues.step3.selection.dataset.metadatas[0].id);
        }

        // BATCH MODE    
        if (self.wizardValues.step1.advConfig.batchMode) {
            var params = {
                "datasetId": self.wizardValues.step3.selection.dataset.id,
                "modelId": self.wizardValues.step4.metadataModelId,
                "storeParams": self.wizardValues.step1.formSchema.schema,
                "styleId": self.wizardValues.step5.styleId
            };

            Examind.dataSources.getAnalysisBatch(self.wizardValues.step1.dataSource.id, params);

            // NORMAL MODE
        } else {
            // unhide data
            if (self.wizardValues.step2.acceptedData) {
                var dataIds = self.wizardValues.step2.acceptedData.map(function (value) {
                    return value.id;
                });
                Examind.datas.changeHiddenFlag(dataIds, false);
            }

            // remove unselected data
            if (angular.isArray(self.wizardValues.step2.rejectedData) && self.wizardValues.step2.rejectedData.length > 0) {
                Examind.datas.removeDatas(self.wizardValues.step2.rejectedData, false);
            }

            // unhide data metadata
            if (self.wizardValues.step2.acceptedData) {
                var metadatas = [];
                self.wizardValues.step2.acceptedData.forEach(function (data) {
                    data.metadatas.forEach(function (metadata) {
                        metadatas.push(metadata);
                    });
                });
                Examind.metadata.changeHiddenPropertyMulti(false, metadatas);
            }

            // remove datasource
            if (self.wizardValues.step1.dataSource) {
                Examind.dataSources.delete(self.wizardValues.step1.dataSource.id);
            }
        }

        self.wizardValues.step5.finished = true;

        if (angular.isFunction(returnToDataDashboard)) {
            returnToDataDashboard();
        }
    };

    self.init = function () {
        // The ref object of the selected data layer to apply the style
        self.selectedDataRef = {};
        // Get the array of the selected data layers
        
        // in batch mode use the first sample data
        if (self.wizardValues.step1.advConfig.batchMode) {
            self.dataLayers = [];
            var storeList = self.wizardValues.step2.storesRef.storeList;
            if (storeList.length > 0 && storeList[0].resources.length > 0) {
                self.dataLayers.push(storeList[0].resources[0]);
            }
        // else we use only the accepted data
        } else {
            self.dataLayers = self.wizardValues.step2.acceptedData;
        }

        self.dataLayersPagination.show = self.dataLayers.length > 1;

        self.dataLayersPagination.total = self.dataLayers.length;

        if (self.dataLayers.length !== 0) {
            self.selectedDataRef.dataLayer = self.dataLayers[0];
        }

    };

    self.init();

}

