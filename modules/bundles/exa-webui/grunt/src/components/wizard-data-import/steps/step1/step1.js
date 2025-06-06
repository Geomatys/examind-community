angular.module('examind.components.wizardDataImport.step1', [
    'examind-instance',
    'examind.components.file.uploader',
    'examind.components.file.explorer',
    'examind.components.wizardDataImport.step1.db.source',
    'examind.components.wizardDataImport.step1.remote.source',
    'examind.components.wizardDataImport.step1.files.source'
])
    .controller('Step1WizardController', Step1WizardController)
    .directive('step1Wizard', Step1WizardDirective);

function Step1WizardDirective() {
    return {
        restrict: "E",
        require: '^wizardDataImport',
        controller: 'Step1WizardController',
        controllerAs: 'step1WizCtrl',
        templateUrl: 'components/wizard-data-import/steps/step1/step1.html',
        scope: {
            wizardValues: "="
        }
    };
}

function Step1WizardController($scope, $rootScope, $translate, $interval, $modal, Growl, cfpLoadingBar, Examind, WizardAddDataService, $filter, AppConfigService) {
    var self = this;

    // The reference of the object define this step
    self.stepObject = WizardAddDataService.stepsConfig.stepsList[0];

    self.wizardValues = $scope.wizardValues;

    // List of the supported data sources
    self.dataSources = [
        {
            name: "files",
            translateKey: "wiz.data.import.step1.label.files"
        },
        {
            name: "url",
            translateKey: "wiz.data.import.step1.label.cloud"
        }
    ];

    AppConfigService.getConfig(function (config) {
        if (config['showDatabaseImportData']) {
            self.dataSources.splice(1, 0, {
                name: "database",
                translateKey: "wiz.data.import.step1.label.database"
            })
        }
    });

    // Flags to know the state of Asynchronous processing
    self.processing = {
        notEmptyDataSource: false,
        fileUpload: false,
        getFiles: false
    };

    // Clear all the variables used by the data source after deleting
    self.clear = function () {
        self.wizardValues.step1.dataSource = {};
        self.fileListRef.fileList = [];
        self.fileListRef.listingState = 'no_files';
        self.stores.detectedStores = [];
        self.stores.selectedStore = null;
        self.formSchema.schema = null;
        self.advConfig.selectAllFlag = false;
        self.advConfig.batchMode = false;
        self.advConfig.showAdvConfig = false;
        $interval.cancel(self.stores.asyncComputeStoresInterval);
    };

    // Initialize the properties used with the local files data source
    self.initLocalFiles = function () {
        if (self.wizardValues.step1.backFlag) {
            self.wizardValues.step1.backFlag = false;
        } else {
            self.deleteDataSource();
        }
    };

    // Initialize the properties used with the remote data source
    self.initRemote = function () {
        if (self.wizardValues.step1.backFlag) {
            self.wizardValues.step1.backFlag = false;
        } else {
            self.remote = {
                url: '',
                protocol: null
            };
            self.deleteDataSource();
        }
    };

    // Initialize the properties used with the database source
    self.initDBForm = function () {
        if (self.wizardValues.step1.backFlag) {
            self.wizardValues.step1.backFlag = false;
        } else {
            self.db = {
                type: "",
                url: "",
                port: "",
                name: "",
                user: "",
                password: ""
            };
            self.deleteDataSource();
        }
    };

    /**
     * Remove the dataSource
     */
    self.deleteDataSource = function () {
        var dsId = self.wizardValues.step1.dataSource.id;
        if (dsId) {
            cfpLoadingBar.start();
            cfpLoadingBar.inc();
            Examind.dataSources.delete(dsId)
                .then(function () {
                    self.clear();
                    cfpLoadingBar.complete();
                    $rootScope.$broadcast('dataSourceDeleted');
                }, function (reason) {
                    $translate('wiz.data.import.step1.msg.err.delete.data.source')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg + ' : ' + reason.errorMessage);
                        });
                });
        }
    };

    self.removeFilesFromDataSource = function () {
        var dlg = $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                'keyMsg': function () {
                    return "wiz.data.import.step1.msg.confirm.delete.datasource";
                }
            }
        });
        dlg.result.then(function (cfrm) {
            if (cfrm) {
                self.deleteDataSource();
            }
        });
    };

    self.isSelectedSource = function (sourceName) {
        return self.wizardValues.step1.dataSourceType && angular.equals(self.wizardValues.step1.dataSourceType, sourceName);
    };

    self.selectSource = function (sourceName) {
        self.wizardValues.step1.dataSourceType = sourceName;
    };

    /**
     * Get the detected stores of the selected data source
     *
     */
    self.getStores = function () {
        self.stores.selectedStore = null;
        var dsId = self.wizardValues.step1.dataSource.id;
        if (!dsId) {
            return;
        }
        cfpLoadingBar.start();
        cfpLoadingBar.inc();

        self.stores.computeStoresCompleted = false;

        /**
         * deep analysis is deactivated by default.
         * activate it only for local files for now
         */
        var deep = self.wizardValues.step1.deepAnalysis;
        if (self.wizardValues.step1.dataSource.type === 'local_files') {
            deep = true;
        }
        /**
         * store the interval promise to cancel
         *
         */
        var cancelled = $interval.cancel(self.stores.asyncComputeStoresInterval);
        self.stores.asyncComputeStoresInterval = $interval(function () {
            self.stores.computeStoresFailed = false;
            Examind.dataSources.computeDatasourceStores(dsId, true, deep)
                .then(function (response) {
                    angular.forEach(response.data, function (item) {
                        item.title = $filter('translate')(item.store + '-' + item.format);
                        var present = false;
                        for (var i = 0; i < self.stores.detectedStores.length; i++) {
                            if (angular.equals(self.stores.detectedStores[i], item)) {
                                present = true;
                                break;
                            }
                        }
                        if (!present) {
                            self.stores.detectedStores.push(item);
                        }
                    });
                    if (self.stores.detectedStores.length === 1) {
                        var currentStore = self.stores.detectedStores[0];
                        // do not call this twice
                        if (currentStore !== self.stores.selectedStore) {
                            self.stores.selectedStore = currentStore;
                            self.selectStore();
                        }
                    }
                    cfpLoadingBar.complete();
                }, function (reason) {
                    $interval.cancel(self.stores.asyncComputeStoresInterval);
                    self.stores.computeStoresFailed = true;
                    $translate('wiz.data.import.step1.msg.err.analyse.data.source')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });

            /**
             * Check the state of data stores analysing
             */
            Examind.dataSources.getAnalysisState(dsId)
                .then(function (response) {
                        if (response.data === "ERROR") {
                            $interval.cancel(self.stores.asyncComputeStoresInterval);
                            self.stores.computeStoresFailed = true;
                            $translate('wiz.data.import.step1.msg.err.analyse.data.state')
                                .then(function (translatedMsg) {
                                    Growl('error', 'Error', translatedMsg);
                                });
                        } else if (response.data === "COMPLETED") {
                            $interval.cancel(self.stores.asyncComputeStoresInterval);
                            self.stores.computeStoresCompleted = true;
                            if (self.stores.detectedStores.length === 1) {
                                var currentStore = self.stores.detectedStores[0];
                                // do not call this twice
                                if (currentStore !== self.stores.selectedStore) {
                                    self.stores.selectedStore = currentStore;
                                    self.selectStore();
                                }
                            }

                            $translate('wiz.data.import.step1.msg.success.analyse.data.state')
                                .then(function (translatedMsg) {
                                    Growl('success', 'Success', translatedMsg);
                                });
                        }
                    },
                    function (reason) {
                        $interval.cancel(self.stores.asyncComputeStoresInterval);
                        self.stores.computeStoresFailed = true;
                        $translate('wiz.data.import.step1.msg.err.analyse.data.state')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    });
        }, 1000);
        // Save the reference to use in the case of close button
        self.wizardValues.step1.stores.asyncComputeStoresInterval = self.stores.asyncComputeStoresInterval;
    };

    // Method to disable files in the file explorer component
    self.isDisabledFile = function (file) {
        if (!self.stores.selectedStore || file.folder) {
            return false;
        }
        for (var i = 0; i < file.types.length; i++) {
            if (angular.equals(file.types[i].store, self.stores.selectedStore.store) &&
                angular.equals(file.types[i].format, self.stores.selectedStore.format)) {
                return false;
            }
        }
        return true;
    };

    // Create the data source
    self.createDataSource = function (dataSource, sourceType, explore) {
        cfpLoadingBar.start();
        cfpLoadingBar.inc();
        
        // if a previous source has been created we delete it
        if (self.wizardValues.step1.dataSource.id){
            Examind.dataSources.delete(self.wizardValues.step1.dataSource.id)
                   .then(testAndCreateDatasource(dataSource, sourceType, explore));
        } else {
            testAndCreateDatasource(dataSource, sourceType, explore);
        }
        
    };
    
    function testAndCreateDatasource(dataSource, sourceType, explore) {
        Examind.dataSources.test(dataSource)
            .then(function (response) {
                cfpLoadingBar.complete();
                if (response.data === "OK") {
                    self.wizardValues.step1.dataSource = dataSource;
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                    Examind.dataSources.create(dataSource)
                        .then(function (response) {
                                cfpLoadingBar.complete();
                                self.wizardValues.step1.dataSource.id = response.data;
                                if (explore) {
                                    self.getFileList('/');
                                    self.getStores();
                                }
                            },
                            function (response) {
                                cfpLoadingBar.complete();
                                $translate('wiz.data.import.step1.msg.error.failed.add.data.source')
                                    .then(function (translatedMsg) {
                                        Growl('error', 'Error', translatedMsg);
                                    });
                            }
                        );

                    $translate('wiz.data.import.step1.msg.success.' + sourceType + '.connection')
                        .then(function (translatedMsg) {
                            Growl('success', 'Success', translatedMsg);
                        });
                } else {
                    $translate('wiz.data.import.step1.msg.error.' + sourceType + '.connection')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg + ':<BR>' + response.data);
                        });
                }
            }, function (reason) {//error
                cfpLoadingBar.complete();
                $translate('wiz.data.import.step1.msg.error.' + sourceType + '.connection')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    }

    /**
     * The method to get the files list to explore in the file explorer component
     * @param path
     * @param obj
     */
    self.getFileList = function (path) {
        if (!self.wizardValues.step1.dataSource.id) {
            return;
        }
        var dsId = self.wizardValues.step1.dataSource.id;
        self.processing.getFiles = true;
        self.fileListRef.listingState = 'requesting';
        cfpLoadingBar.start();
        cfpLoadingBar.inc();
        Examind.dataSources.explore(dsId, path)
            .then(function (response) {
                self.fileListRef.fileList = response.data;
                self.fileListRef.listingState = response.data.length === 0 ? 'no_files' : 'listing';
                self.processing.getFiles = false;
                cfpLoadingBar.complete();
            }, function () {
                self.fileListRef.listingState = 'server_error';
                self.processing.getFiles = false;
                $translate('wiz.data.import.step1.msg.err.get.files')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
                cfpLoadingBar.complete();
            });
    };

    self.openDir = function (file) {
        if (!file.folder) {
            return;
        }
        self.pathStack.push(file.path);
        self.getFileList(file.path);
    };

    self.upDir = function () {
        if (self.pathStack.length > 1) {
            // remove the current dir
            self.pathStack.pop();
            // load the parent dir
            self.getFileList(self.pathStack[self.pathStack.length - 1]);
        }
    };

    self.disableUpDir = function () {
        return self.pathStack.length === 1;
    };

    self.clearSelectedFiles = function () {
        self.fileListRef.selectedItemsList = [];
    };

    /**
     * function to hide the fields (read only)
     * @param property
     * @returns {boolean}
     */
    self.hideField = function (property) {
        // hide datasource identifier.
        if (angular.equals(property.id, "datasourceId")) {
            return true;
        }
        
        // hide provider identifier.
        if (angular.equals(property.id, "identifier")) {
            return true;
        }
        // for local files hide path field.
        if (angular.equals(property.id, "path") ||
            angular.equals(property.id, "inspirepath") ||
            angular.equals(property.id, "location")) {
            return true;
        }
        // for url hide path field.
        if (angular.equals(property.id, "url")) {
            return true;
        }
        /* for database hide auto filled fields :
         * - host
         * - port
         * - database
         * - user / password
         */
        return angular.equals(property.id, "host") ||
            angular.equals(property.id, "port") ||
            angular.equals(property.id, "database") ||
            angular.equals(property.id, "user") ||
            angular.equals(property.id, "password");

    };

    /**
     * Method to fill automatically the fields of dataStore form with custom values.
     */
    self.autoFillFields = function () {
        var properties = self.formSchema.schema.property.properties;
        angular.forEach(properties, function (property) {
            if (angular.equals(self.wizardValues.step1.dataSourceType, "database")) {
                // decompose database url
                var url = self.wizardValues.step1.dataSource.url;
                var parts = url.split('/');
                var dbName = parts[parts.length - 1];
                var hostPort = parts[parts.length - 2].split(':');
                if (angular.equals(property.id, "host")) {
                    property.value = hostPort[0];
                }
                if (angular.equals(property.id, "port")) {
                    property.value = hostPort[1];
                }
                if (angular.equals(property.id, "database")) {
                    property.value = dbName;
                }
                if (angular.equals(property.id, "user")) {
                    property.value = self.wizardValues.step1.dataSource.username;
                }
                if (angular.equals(property.id, "password")) {
                    property.value = self.wizardValues.step1.dataSource.pwd;
                }
            }
            if (angular.equals(self.wizardValues.step1.dataSourceType, "files")) {
                if (angular.equals(property.id, "path") || angular.equals(property.id, "location")) {
                    property.value = self.wizardValues.step1.dataSource.url;
                }
            }
            if (angular.equals(self.wizardValues.step1.dataSourceType, "url")) {
                if (angular.equals(property.id, "url")) {
                    property.value = self.wizardValues.step1.dataSource.url;
                }
            }
        });
    };

    self.getAllCRS = function () {
        Examind.crs.listAll()
            .then(function (response) {
                self.formSchema.epsgCodes = response.data;
            });
    };

    self.selectStore = function () {
        self.getStoreAdvancedConfiguration();
        self.clearSelectedFiles();
        self.advConfig.showAdvConfig = false;
        self.advConfig.selectAllFlag = false;
    };

    /**
     * Select all files in the data source belongs the selected format
     */
    self.selectAllFilesBelongsFormat = function () {
        if (self.advConfig.selectAllFlag) {
            self.fileListRef.selectAllFiles();
        } else {
            self.fileListRef.selectedItemsList = [];
        }
    };

    /**
     * Get the data source Id
     * @returns {null|*}
     */
    self.getDataSourceId = function () {
        return self.wizardValues.step1.dataSource.id;
    };

    /**
     * Set the data source Id
     * @param dataSourceId
     */
    self.setDataSourceId = function (dataSourceId) {
        self.wizardValues.step1.dataSource.id = dataSourceId;
    };

    /**
     * Set the dataSource object
     * @param dataSource
     */
    self.setDataSource = function (dataSource) {
        self.wizardValues.step1.dataSource = dataSource;
    };

    /**
     * Set the dataSource object
     * @param dataSource
     */
    self.getDataSource = function () {
        return self.wizardValues.step1.dataSource;
    };

    // Fill the advanced configuration of the selected store
    self.getStoreAdvancedConfiguration = function () {
        if (!self.stores.selectedStore) {
            return;
        }

        Examind.datas.getDataStoreConfiguration(self.stores.selectedStore.store)
            .then(function (response) {
                    if (response.data) {
                        // auto select the single store
                        self.formSchema.schema = response.data;
                        self.autoFillFields();
                    }
                },
                function (response) {
                    Growl('error', 'Error', 'An error happen when getting type:' + self.wizardValues.step1.dataSource.storeId);
                });
    };

    /**
     * Check if the provider configuration form have at least one fields not hidden
     * @returns {boolean}
     */
    self.canShowProviderConfigProperties = function () {
        if (!self.formSchema.schema || !self.formSchema.schema.property || !self.formSchema.schema.property.properties) {
            return false;
        }
        var properties = self.formSchema.schema.property.properties;
        for (var i = 0; i < properties.length; i++) {
            if (!self.hideField(properties[i])) {
                return true;
            }
        }
        return false;
    };

    self.showAdvancedConfigBlock = function () {
        return self.advConfig.showAdvConfig && self.canShowProviderConfigProperties();
    };

    self.disableNextBtnFiles = function () {
        if (self.advConfig.batchMode || (self.stores.selectedStore && self.stores.selectedStore.store === 'S63')) {
            return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore;
        }
        if (self.advConfig.selectAllFlag) {
            return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore || self.processing.getFiles || self.processing.fileUpload;
        }
        return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore
            || self.processing.getFiles || self.processing.fileUpload || self.fileListRef.selectedItemsList.length === 0;
    };

    self.disableNextBtnUrl = function () {
        if (self.remote && self.remote.protocol && self.remote.protocol.isDefaultProtocol) {
            if (self.advConfig.batchMode) {
                return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore;
            }
            if (self.advConfig.selectAllFlag) {
                return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore || self.processing.getFiles;
            }
            return !self.wizardValues.step1.dataSource.id || !self.stores.selectedStore || self.processing.getFiles || self.fileListRef.selectedItemsList.length === 0;
        }
        return !self.wizardValues.step1.dataSource.id;
    };

    // Implement the conditions to disable next button in this step
    self.stepObject.disableNextBtn = function () {
        if (self.wizardValues.step1.dataSourceType === 'files') {
            return self.disableNextBtnFiles();
        } else if (self.wizardValues.step1.dataSourceType === 'url') {
            return self.disableNextBtnUrl();
        } else {
            return !self.wizardValues.step1.dataSource.id;
        }
    };

    var goToNextStep = function (goToStepFn, stepNum) {
        if (angular.isFunction(goToStepFn) && stepNum) {
            // Fill the variable to use in back case
            self.wizardValues.step1.stores = self.stores;
            self.wizardValues.step1.pathStack = self.pathStack;
            self.wizardValues.step1.fileListRef = self.fileListRef;
            self.wizardValues.step1.fileExplorerState = self.fileExplorerState;
            self.wizardValues.step1.formSchema = self.formSchema;
            self.wizardValues.step1.advConfig = self.advConfig;
            self.wizardValues.step1.db = self.db;
            self.wizardValues.step1.remote = self.remote;
            if (self.wizardValues.step1.dataSourceType === 'files' ||
                (self.wizardValues.step1.dataSourceType === 'url' && self.remote.protocol && self.remote.protocol.isDefaultProtocol)) {
                // Update the data source with the selected store
                var dataSource = self.wizardValues.step1.dataSource;
                dataSource.storeId = self.stores.selectedStore.store;
                dataSource.format = self.stores.selectedStore.format;
                cfpLoadingBar.start();
                cfpLoadingBar.inc();
                Examind.dataSources.update(dataSource)
                    .then(function (response) {
                        window.setTimeout(function () {
                            cfpLoadingBar.complete();
                            goToStepFn(stepNum);
                        }, 400);
                    }, function () {
                        $translate('wiz.data.import.step1.msg.err.update.data.source')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    });
                $interval.cancel(self.stores.asyncComputeStoresInterval);
            } else if (self.stores.selectedStore && self.stores.selectedStore.store === 'S63') {
                // Handle the case of S63
                self.wizardValues.step1.isS63 = true;
                WizardAddDataService.s63InitWizard();
                
            } else if (self.wizardValues.step1.dataSourceType === 'database' ) {
                angular.forEach(self.wizardValues.step1.formSchema.schema.property.properties, function (property) {
                    if (angular.equals(property.id, "datasourceId")) {
                        property.value = self.wizardValues.step1.dataSource.id;
                    }
                });
                goToStepFn(stepNum);
            } else {
                goToStepFn(stepNum);
            }
        }
    };

    /**
     * Implement the next button of this step
     * The process to handle before going to the next step
     */
    self.stepObject.goToNextStep = function (goToStepFn, stepNum) {
        if (self.fileListRef.selectedItemsList && self.fileListRef.selectedItemsList.length > 0 && !self.advConfig.selectAllFlag) {
            cfpLoadingBar.start();
            cfpLoadingBar.inc();
            Examind.dataSources.addSelectedPaths(self.wizardValues.step1.dataSource.id, self.fileListRef.selectedItemsList)
                .then(function (response) {
                    cfpLoadingBar.complete();
                    goToNextStep(goToStepFn, stepNum);
                }, function (reason) {
                    $translate('wiz.data.import.step1.msg.err.add.selected.files')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
        } else {
            goToNextStep(goToStepFn, stepNum);
        }
    };

    // Initialize the component controller
    self.init = function () {
        if (!self.wizardValues.step1) {
            // The case of the first initialization of this step
            /** Init the values that will be save in this step
             * dataSourceId is the Id of the data source created in this step
             *
             * @type {{dataSource: null, dataSourceId: null, dataSourceType: null, dataStoreType: null}}
             */
            self.wizardValues.step1 = {
                dataSource: {},
                dataSourceId: null,
                dataSourceType: null,
                dataStoreType: null,
                db: null,
                remote: null,
                files: false,
                pathStack: null,
                fileListRef: null,
                stores: {},
                fileExplorerState: {},
                advConfig: {},
                formSchema: {},
                backFlag: false,
                isS63: false,
                deepAnalysis: false
            };

            // The database source connection info
            self.db = {
                type: null,
                host: "",
                port: "",
                name: "",
                user: "",
                password: ""
            };

            // The remote data source
            self.remote = {
                url: '',
                protocol: null
            };

            // Implementation of the file-explorer component methods
            self.pathStack = ["/"];

            self.fileListRef = {
                fileList: [],
                selectedItemsList: [],
                listingState: 'no_files',
                selectAllFiles: angular.noop
            };

            self.stores = {
                detectedStores: [],
                selectedStore: null,
                asyncComputeStoresInterval: null,
                computeStoresCompleted: false,
                computeStoresFailed: false
            };

            self.fileExplorerState = {
                sort: null,
                viewTemplate: null
            };

            self.formSchema = {
                schema: null,
                epsgCodes: []
            };

            self.advConfig = {
                showAdvConfig: false,
                selectAllFlag: false,
                batchMode: false
            };

            self.getAllCRS();

        } else {
            // Get all the saved properties
            self.stores = self.wizardValues.step1.stores;
            self.pathStack = self.wizardValues.step1.pathStack;
            self.fileListRef = self.wizardValues.step1.fileListRef;
            self.fileExplorerState = self.wizardValues.step1.fileExplorerState;
            self.formSchema = self.wizardValues.step1.formSchema;
            self.advConfig = self.wizardValues.step1.advConfig;
            self.db = self.wizardValues.step1.db;
            self.remote = self.wizardValues.step1.remote;
            if (self.wizardValues.step1.dataSourceType === 'files') {
                self.getFileList(self.pathStack[self.pathStack.length - 1]);
                if (!self.stores.computeStoresCompleted) {
                    self.getStores();
                }
            }
            if (self.wizardValues.step1.dataSourceType === 'url' && self.remote.protocol && self.remote.protocol.isDefaultProtocol) {
                self.getFileList(self.pathStack[self.pathStack.length - 1]);
                if (!self.stores.computeStoresCompleted) {
                    self.getStores();
                }
            }
        }
    };

    self.init();

}
