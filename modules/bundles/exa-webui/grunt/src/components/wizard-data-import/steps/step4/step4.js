angular.module('examind.components.wizardDataImport.step4', [
    'examind.components.metadata.editor',
    'examind-instance'
])
    .controller('Step4WizardController', Step4WizardController)
    .directive('step4Wizard', Step4WizardDirective);

function Step4WizardDirective() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step4WizardController',
        controllerAs: 'step4WizCtrl',
        templateUrl: 'components/wizard-data-import/steps/step4/step4.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step4WizardController($scope, $location, $translate, Growl, Examind, WizardAddDataService) {
    var self = this;

    // Get the step object reference
    self.stepObject = WizardAddDataService.stepsConfig.stepsList[3];

    // Get the tree contains all the step variables
    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step3) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.metadataValues = [];

    // Lis of data layers
    self.dataLayers = [];

    self.loadHelper = {
        processing: false
    };

    self.getMetadataForCurrentLayer = function () {
        // Get the metadata of the current layer for the selected standard
        self.wizardValues.step4.selectedMd = self.getMdForStandard(self.wizardValues.step4.mdStandard);
        if (self.wizardValues.step4.selectedMd) {
            self.getIsoMetadataJson(self.wizardValues.step4.selectedMd.id);
            var cdl = self.wizardValues.step4.pagination.current;
            $translate('wiz.data.import.step4.msg.change.data.layer')
                .then(function (translatedMsg) {
                    Growl('info', 'Info', translatedMsg + " : " + self.dataLayers[cdl - 1].name);
                });
        }
    };

    self.canGoToPreviousDataLayer = function () {
        return self.wizardValues.step4.pagination.current - 1 >= 1;
    };

    self.goToPreviousDataLayer = function () {
        if (!self.canGoToPreviousDataLayer()) {
            return;
        }
        self.wizardValues.step4.pagination.current -= 1;
        self.getMetadataForCurrentLayer();
    };

    self.canGoToNextDataLayer = function () {
        return self.wizardValues.step4.pagination.current + 1 <= self.dataLayers.length;
    };

    self.goToNextDataLayer = function () {
        if (!self.canGoToNextDataLayer()) {
            return;
        }
        self.wizardValues.step4.pagination.current += 1;
        self.getMetadataForCurrentLayer();
    };

    self.getIsoMetadataJson = function (mdId) {
        if (!mdId) {
            return;
        }
        self.loadHelper.processing = true;
        // Get the new metadata json
        Examind.metadata.getIsoMetadataJson(mdId, false)
            .then(function (response) {
                if (response.data && response.data.root) {
                    self.metadataValues.pop();
                    self.metadataValues.push(response.data);
                    self.loadHelper.processing = false;
                }
            }, function (reason) {
                $translate('wiz.data.import.step4.msg.error.load.metadata')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    self.getMdForStandard = function (standard) {
        var profileIso19110 = "profile_default_feature_catalogue";
        var cdl = self.wizardValues.step4.pagination.current;
        if (angular.equals(standard, 'iso19110')) {
            return self.dataLayers[cdl - 1].metadatas.find(function (elt) {
                return elt.profile === profileIso19110;
            });
        }

        return self.dataLayers[cdl - 1].metadatas.find(function (elt) {
            return elt.profile !== profileIso19110;
        });
    };

    self.saveMetadata = function (mdId, mdProfile, metadataValues, callBack) {
        if (metadataValues && metadataValues.length > 0) {
            Examind.metadata.saveMetadata(mdId,
                mdProfile,
                metadataValues[0]).then(
                function (response) {
                    $translate('wiz.data.import.step4.msg.success.metadata.save')
                        .then(function (translatedMsg) {
                            Growl('success', 'Success', translatedMsg);
                        });
                    if (angular.isFunction(callBack)) {
                        callBack();
                    }
                },
                function (reason) {
                    $translate('wiz.data.import.step4.msg.error.metadata.save')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
        }
    };

    self.saveMdFunction = function (callBack) {
        var md = self.getMdForStandard(self.wizardValues.step4.mdStandard);
        if (md) {
            self.saveMetadata(md.id, md.profile, self.metadataValues, callBack);
        }
    };

    self.createMetadataModel = function (callBack) {
        if (self.metadataValues && self.metadataValues.length > 0) {
            Examind.metadata.create('profile_import', 'model', self.metadataValues[0])
                .then(function (response) {
                        $translate('wiz.data.import.step4.msg.success.metadata.save')
                            .then(function (translatedMsg) {
                                Growl('success', 'Success', translatedMsg);
                                self.wizardValues.step4.metadataModelId = response.data.id;
                                if (self.wizardValues.step4.batchMode) {
                                    var dataIds = self.wizardValues.step2.acceptedData.map(function (value) {
                                        return value.id;
                                    });
                                    Examind.datas.mergeModelDataMetadata(self.wizardValues.step4.metadataModelId, dataIds)
                                        .then(function () {
                                            callBack();
                                        });
                                } else {
                                    callBack();
                                }
                            });
                    },
                    function (reason) {
                        $translate('wiz.data.import.step4.msg.error.metadata.save')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    });
        }
    };

    self.changeMDStandard = function (standard) {
        if (!self.wizardValues.step4) {
            return;
        }
        var md = self.getMdForStandard(standard);
        if (md) {
            self.wizardValues.step4.mdStandard = standard;
            self.getIsoMetadataJson(md.id);
        } else {
            Growl('error', 'Error', "This data did't have any ISO19110 metadata");
        }
    };

    self.isSelectedMDStandard = function (standard) {
        return self.wizardValues.step4 && self.wizardValues.step4.mdStandard && angular.equals(self.wizardValues.step4.mdStandard, standard);
    };

    self.showMdStandard = function () {
        if (self.dataLayers.length === 0 || self.wizardValues.step1.advConfig.batchMode) return false;
        var cdl = self.wizardValues.step4.pagination.current;
        return self.dataLayers[cdl - 1].type.toLowerCase() === "vector";
    };

    self.getNewMetadataJson = function () {
        self.loadHelper.processing = true;
        Examind.metadata.getNewMetadataJson('profile_import')
            .then(function (response) {
                if (response.data && response.data.root) {
                    self.metadataValues.pop();
                    self.metadataValues.push(response.data);
                    self.loadHelper.processing = false;
                }
            }, function (reason) {
                $translate('wiz.data.import.step4.msg.error.load.metadata')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    self.changeBatchMode = function () {
        if (self.wizardValues.step4.batchMode) {
            self.getNewMetadataJson();
        } else {
            self.getMetadataForCurrentLayer();
        }
    };

    var goToNextStep = function (goToNextStepFn, stepNum) {
        if (angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        if (self.wizardValues.step1.advConfig.batchMode || self.wizardValues.step4.batchMode) {
            self.createMetadataModel(function () {
                goToNextStep(goToNextStepFn, stepNum);
            });
        } else {
            self.saveMdFunction(function () {
                goToNextStep(goToNextStepFn, stepNum);
            });
        }
    };

    self.stepObject.goToPreviousStep = function (goToPreviousStepFn, stepNum) {
        // Delete created metadataModel
        if (self.wizardValues.step4 && self.wizardValues.step4.metadataModelId) {
            Examind.metadata.delete([self.wizardValues.step4.metadataModelId]);
        }
        // Remove the informations of this step
        delete self.wizardValues.step4;

        //unlink data and dataset
        if (self.wizardValues.step3.selection.dataset && self.wizardValues.step2.acceptedData) {
            var dataIds = self.wizardValues.step2.acceptedData.map(function (value) {
                return value.id;
            });
            Examind.datas.updateDataDataset(dataIds, -1);
        }

        if (angular.isFunction(goToPreviousStepFn) && stepNum) {
            goToPreviousStepFn(stepNum);
        }
    };

    function finishHandler(callback) {
        // unhide dataset metadata
        if (self.wizardValues.step3.selection.dataset && self.wizardValues.step3.selection.dataset.metadatas &&
            self.wizardValues.step3.selection.dataset.metadatas.length === 1) {
            Examind.metadata.changeHiddenProperty(false, self.wizardValues.step3.selection.dataset.metadatas[0].id);
        }

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

        self.wizardValues.step4.finished = true;

        if (angular.isFunction(callback)) {
            callback();
        }
    }

    self.stepObject.finish = function (callback) {
        self.wizardValues.step4.finished = true;
        if (self.wizardValues.step1.advConfig.batchMode || self.wizardValues.step4.batchMode) {
            self.createMetadataModel(function () {
                if (self.wizardValues.step1.advConfig.batchMode) {
                    var params = {
                        'datasetId': self.wizardValues.step3.selection.dataset.id,
                        'modelId': self.wizardValues.step4.metadataModelId,
                        'storeParams': self.wizardValues.step1.formSchema.schema
                    };

                    Examind.dataSources.getAnalysisBatch(self.wizardValues.step1.dataSource.id, params)
                        .then(function () {
                            if (angular.isFunction(callback)) {
                                callback();
                            }
                        }, function (error) {
                            console.error(error);
                        });

                    // NORMAL MODE
                } else {
                    finishHandler(callback);
                }
            });
        } else {
            self.saveMdFunction(function () {
                if (self.wizardValues.step1.advConfig.batchMode) {
                    var params = {
                        'datasetId': self.wizardValues.step3.selection.dataset.id,
                        'modelId': self.wizardValues.step4.metadataModelId,
                        'storeParams': self.wizardValues.step1.formSchema.schema
                    };

                    EExamind.dataSources.getAnalysisBatch(self.wizardValues.step1.dataSource.id, params)
                        .then(function () {
                            if (angular.isFunction(callback)) {
                                callback();
                            }
                        }, function (error) {
                            console.error(error);
                        });

                    // NORMAL MODE
                } else {
                    finishHandler(callback);
                }
            });
        }
    };

    self.init = function () {
        /**
         * The initialise Mode of this step
         * @type {string}
         */
        if (!self.wizardValues.step4) {
            self.wizardValues.step4 = {
                mdStandard: 'iso19115',
                selectedMd: {},
                metadataModelId: null,
                batchMode: false,
                pagination: {
                    show: false,
                    current: 1,
                    total: 0
                }
            };
            self.dataLayers = self.wizardValues.step2.acceptedData;
            self.wizardValues.step4.pagination.show = self.dataLayers.length > 1 && !self.wizardValues.step1.advConfig.batchMode;
            self.wizardValues.step4.pagination.total = self.dataLayers.length;

            // Handle the batch mode
            if (self.wizardValues.step1.advConfig.batchMode) {
                // Get the new metadata json
                self.getNewMetadataJson();
            } else if (self.dataLayers.length > 0) {
                self.wizardValues.step4.selectedMd = self.getMdForStandard('iso19115');
                if (self.wizardValues.step4.selectedMd) {
                    self.getIsoMetadataJson(self.wizardValues.step4.selectedMd.id);
                }
            }
        } else {
            if (self.wizardValues.step1.advConfig.batchMode || self.wizardValues.step4.batchMode) {
                self.getIsoMetadataJson(self.wizardValues.step4.metadataModelId);
            } else {
                // Get the data layers from the step2
                self.dataLayers = self.wizardValues.step2.acceptedData;
                self.getMetadataForCurrentLayer();
            }
        }
    };

    self.init();
}


