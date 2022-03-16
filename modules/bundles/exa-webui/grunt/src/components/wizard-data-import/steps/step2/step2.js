angular.module('examind.components.wizardDataImport.step2', [
    'examind-instance',
    'examind.components.json.forms.builder',
    'examind.components.stores.tree.view',
    'examind.components.preview.map',
    'examind.shared.data.viewer.service'
])
    .controller('Step2WizardController', Step2WizardController)
    .directive('step2Wizard', Step2WizardDirective);

function Step2WizardDirective() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step2WizardController',
        controllerAs: 'step2WizCtrl',
        templateUrl: 'components/wizard-data-import/steps/step2/step2.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step2WizardController($scope, $location, $translate, Examind, Growl,
                               cfpLoadingBar, DataViewerService, WizardAddDataService) {

    var self = this;

    self.stepObject = WizardAddDataService.stepsConfig.stepsList[1];

    self.wizardValues = $scope.wizardValues;

    if (!self.wizardValues.step1) {
        $location.search('step', null);
        $location.path('/data');
        return;
    }

    self.getResourcesListPromise = function (dsId, schema) {
        if (self.wizardValues.step1.advConfig.batchMode) {
            return Examind.dataSources.getAnalysisSample(dsId, schema);
        } else {
            return Examind.dataSources.getAnalysisV3(dsId, schema);
        }
    };

    /**
     * Get the list of the resources after analysis
     */
    self.getResourcesList = function () {
        if (!self.wizardValues.step1.dataSource.id) {
            return;
        }
        var dsId = self.wizardValues.step1.dataSource.id;
        self.wizardValues.step2.storesRef.state.processing = true;
        self.getResourcesListPromise(dsId, self.wizardValues.step1.formSchema.schema)
            .then(function (response) {
                self.wizardValues.step2.storesRef.storeList = response.data.stores;
                self.wizardValues.step2.storesRef.state.processing = false;
            }, function (reason) {
                self.wizardValues.step2.storesRef.state.processing = false;
                self.wizardValues.step2.storesRef.state.analysisFailure = true;
                $translate('wiz.data.import.step2.msg.err.get.resources.list')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    self.showStoresTree = function () {
        return !self.wizardValues.step2.storesRef.state.processing && self.wizardValues.step2.storesRef.storeList.length > 0;
    };

    self.showAnalysisBlock = function () {
        return self.wizardValues.step2.storesRef.state.processing;
    };

    self.showFailureBlock = function () {
        return !self.wizardValues.step2.storesRef.state.processing && (self.wizardValues.step2.storesRef.state.analysisFailure || !self.wizardValues.step2.storesRef.storeList || self.wizardValues.step2.storesRef.storeList.length === 0);
    };

    self.getStoreSelection = function () {
        var selection = {selected: [], unselected: []};
        var selectedItems = angular.copy(self.wizardValues.step2.storesRef.selectedItemsList);
        angular.forEach(self.wizardValues.step2.storesRef.storeList, function (store) {
            var found = false;
            for (var i = 0; i < selectedItems.length; i++) {
                var item = selectedItems[i];
                if (store.id === item.id) {
                    selection.selected.push(store);
                    found = true;
                    // remove the element so we do not iterate again on it
                    selectedItems.splice(i, 1);
                    break;
                } else {
                    var stop = false;
                    for (var j = 0; j < store.resources.length; j++) {
                        var resource = store.resources[j];
                        if (resource.id === item.id) {
                            selection.selected.push(store);
                            // remove the resources of the store so we do not iterate again on it
                            selectedItems.filter(function (item) {
                                return store.resources.indexOf(item) !== -1;
                            });
                            found = true;
                            stop = true;
                            break;
                        }
                    }
                    if (stop) {
                        break;
                    }
                }
            }
            if (!found) {
                selection.unselected.push(store);
            }
        });
        return selection;
    };

    self.isSelectedResource = function (store, data) {
        for (var i = 0; i < store.resources.length; i++) {
            var resource = store.resources[i];
            if (resource.name === data.name) {
                for (var j = 0; j < self.wizardValues.step2.storesRef.selectedItemsList.length; j++) {
                    var selectResource = self.wizardValues.step2.storesRef.selectedItemsList[j];
                    if (resource.id === selectResource.id) {
                        return true;
                    }
                }
            }
        }
        return false;
    };

    /**
     * Display the data layer in the preview map.
     * @param resource
     */
    self.displayDataLayer = function (resource) {
        if (!resource || !resource.id || !resource.name) {
            self.preview.extent = self.preview.layer = null;
            return;
        }

        // Create the data layer instance.
        var layer = DataViewerService.createLayer(
            window.localStorage.getItem('cstlUrl'),
            resource.id,
            resource.name,
            null,
            resource.type.toLowerCase() !== 'vector');

        layer.get('params').ts = new Date().getTime();

        // get the extent of the data
        Examind.datas.getGeographicExtent(resource.id)
            .then(function (response) {
                    // Display the layer and zoom on its extent.
                    self.preview.extent = response.data.boundingBox;
                    self.preview.layer = layer;
                },
                function (reason) {
                    $translate('wiz.data.import.step2.msg.err.get.geographic.extent')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });

    };

    var goToNextStep = function (goToNextStepFn, stepNum) {
        if (angular.isFunction(goToNextStepFn) && stepNum) {
            goToNextStepFn(stepNum);
        }
    };

    var isDataSelectioned = function () {
        if (self.wizardValues.step2.storesRef.selectedItemsList) {
            for (var j = 0; j < self.wizardValues.step2.storesRef.selectedItemsList.length; j++) {
                // Return true if the selected item is a non-empty store or if its a single resource.
                if (self.wizardValues.step2.storesRef.selectedItemsList[j].resources && self.wizardValues.step2.storesRef.selectedItemsList[j].resources.length > 0 ||
                    self.wizardValues.step2.storesRef.selectedItemsList[j].name) {
                    return true;
                }
            }
        }
        return false;
    };
    /*
     * Implement the conditions to disable next button in this step,
     * return true if :
     * - we are in batch mode
     * - there is at least one existing store with data selectionned
     * 
     * @return {undefined}
     */
    self.stepObject.disableNextBtn = function () {
        return self.wizardValues.step1.advConfig.batchMode ? false : !isDataSelectioned();
    };

    self.stepObject.goToNextStep = function (goToNextStepFn, stepNum) {
        if (!self.wizardValues.step1.advConfig.batchMode) {
            var stores = self.getStoreSelection();

            var dataIds = stores.selected
                .map(function(store) {
                    return store.resources
                        .filter(function(data) {
                            if (store.indivisible || self.isSelectedResource(store, data)) {
                                return true;
                            }
                            self.wizardValues.step2.rejectedData.push(data.id);
                            return false;
                        })
                        .map(function(data) {
                            return data.id;
                        });
                })
                .filter(function(dataIds) {
                    return angular.isArray(dataIds) && dataIds.length > 0;
                })
                .reduce(function(accumulator, dataIds) {
                    return accumulator.concat(dataIds);
                }, []);

            stores.unselected
                .map(function (store) {
                    store.resources.map(function(data) {
                        return data.id;
                    });

                    store.resources.forEach(function (data) {
                        self.wizardValues.step2.rejectedData.push(data.id);
                    });
                })
                .reduce(function(accumulator, dataIds) {
                    accumulator.splice
                        .bind(accumulator, accumulator.length-1, 0)
                        .apply(accumulator, dataIds);
                    return accumulator;
                }, self.wizardValues.step2.rejectedData);

            if (dataIds.length > 0) {
                Examind.datas.acceptDatas(dataIds, true)
                    .then(function (response) {
                        // Add refused data id to rejected data list
                        var refused = response.data.refused;
                        if (angular.isArray(refused) && refused.length > 0) {
                            $translate('wiz.data.import.step2.msg.err.accept.data')
                                .then(function (translatedMsg) {
                                    Growl('error', 'Error', translatedMsg);
                                });
                            self.wizardValues.step2.rejectedData = self.wizardValues.step2.rejectedData.concat(refused);
                        }

                        // Add accepted data to accepted list
                        var accepted = response.data.accepted;
                        if (angular.isArray(accepted) && accepted.length > 0) {
                            accepted.forEach(function(data) {
                                self.wizardValues.step2.acceptedData.push(data);
                            });
                        }

                    })
                    .catch(function(err) {
                        $translate('wiz.data.import.step2.msg.err.accept.data')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    })
                    .finally(function() {
                        goToNextStep(goToNextStepFn, stepNum);
                    });
            } else {
                goToNextStep(goToNextStepFn, stepNum);
            }
        } else {
            goToNextStep(goToNextStepFn, stepNum);
        }
    };

    var acceptDataSuccess = function (promiseArray, i, response, oneFail, goToNextStepFn, stepNum) {
        self.wizardValues.step2.acceptedData.push(response.data);
        if (i < promiseArray.length) {
            promiseArray[i].promise.then(
                function (response) {
                    acceptDataSuccess(promiseArray, i + 1, response, oneFail, goToNextStepFn, stepNum);
                },
                function (error) {
                    acceptDataError(promiseArray, i + 1, error, goToNextStepFn, stepNum);
                });
        } else {
            cfpLoadingBar.complete();
            //if (!oneFail) {  // for now we continue even if one fail
            goToNextStep(goToNextStepFn, stepNum);
            //}
        }
    };

    var acceptDataError = function (promiseArray, i, error, goToNextStepFn, stepNum) {
        $translate('wiz.data.import.step2.msg.err.accept.data')
            .then(function (translatedMsg) {
                Growl('error', 'Error', translatedMsg);
            });
        self.wizardValues.step2.rejectedData.push(promiseArray[i - 1].data);
        if (i < promiseArray.length) {
            promiseArray[i].promise.then(
                function (response) {
                    acceptDataSuccess(promiseArray, i + 1, response, false, goToNextStepFn, stepNum);
                },
                function (error) {
                    acceptDataError(promiseArray, i + 1, error, goToNextStepFn, stepNum);
                });
        } else {
            cfpLoadingBar.complete();
            //if (!oneFail) { // for now we continue even if one fail
            goToNextStep(goToNextStepFn, stepNum);
            //}
        }
    };

    self.stepObject.goToPreviousStep = function (goToPreviousStepFn, stepNum) {
        // remove integrated stores 
        angular.forEach(self.wizardValues.step2.storesRef.storeList, function (store) {
            Examind.providers.delete(store.id);
        });
        // remove selected paths
        Examind.dataSources.addSelectedPaths(self.wizardValues.step1.dataSource.id, []);

        // Remove the data of this step
        delete self.wizardValues.step2;
        self.wizardValues.step1.backFlag = true;
        if (angular.isFunction(goToPreviousStepFn) && stepNum) {
            goToPreviousStepFn(stepNum);
        }
    };

    /**
     * This method used to initialize the component controller properties
     */
    self.init = function () {
        if (!self.wizardValues.step2) {
            // The case of the first initialization of this step
            self.wizardValues.step2 = {
                acceptedData: [],
                rejectedData: [],
                storesRef: {
                    storeList: [],
                    state: {
                        processing: false,
                        analysisFailure: false
                    },
                    selectedItemsList: [],
                    displayedResource: null,
                    sourceType: null
                },
                preview: {}
            };

            // Overview layer instance that used with preview-map component.
            self.preview = {layer: null, extent: null};

            self.wizardValues.step2.storesRef.sourceType = self.wizardValues.step1.dataSource.type;

            // Handle the case of S63
            if (!self.wizardValues.step1.isS63) {
                self.getResourcesList();
            }

        } else {
            self.preview = self.wizardValues.step2.preview;
            // Clear the accepted and rejected data
            self.wizardValues.step2.acceptedData = [];
            self.wizardValues.step2.rejectedData = [];
        }
    };

    self.init();
}

