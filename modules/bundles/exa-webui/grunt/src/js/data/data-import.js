/*
 * Constellation - An open source and standard compliant SDI
 *
 *     http://www.constellation-sdi.org
 *
 *     Copyright 2014 Geomatys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

angular.module('cstl-data-import', [
    'cstl-restapi',
    'cstl-services',
    'pascalprecht.translate',
    'ui.bootstrap.modal',
    'examind-instance']
)

    .controller('ModalImportDataController', function($scope, $modalInstance, firstStep,
                                                      importType, UploadFiles, Growl, Examind) {
        $scope.import = {
            importType: importType,
            currentStep: firstStep,
            dataPath: null,
            mdPath: null,
            fillMetadata:false,
            uploadType: null,
            allowNext: true,
            allowSubmit: false,
            allowSensorChoose: false,
            next: angular.noop,
            finish: angular.noop,
            metadata: null,
            providerId: null,
            layer: null,
            db: {},
            currentPath:null,
            currentMDPath:null
        };
        $scope.enableSelectEPSGCode = false;

        $scope.sensor = {
            mode : "existing",
            checked : false
        };

        $scope.close = function() {
            $modalInstance.close({type: $scope.import.uploadType,
                                  file: $scope.import.providerId,
                                  metadataFile:$scope.import.metadata,
                                  completeMetadata:$scope.import.fillMetadata});
        };

        $scope.showAssociate = function() {
            $scope.import.currentStep = 'step4Sensor';
            $scope.import.allowSensorChoose = false;
            $scope.import.allowSubmit = true;
        };

        $scope.importDb = function() { 
            var providerId = $scope.import.identifier;
            if(!providerId) {
                providerId = $scope.import.metaIdentifier;
            }
            Examind.providers.create(providerId, true, {
                type: "data-store",
                subType: "postgresql",
                parameters: {
                    host: $scope.import.db.url,
                    port: $scope.import.db.port,
                    user: $scope.import.db.user,
                    password: $scope.import.db.password,
                    database: $scope.import.db.name
                }
            }).then(function() {
                //success
                if ($scope.import.mdPath) {
                    Examind.datas.saveUploadedMetadata({values: {'providerId': providerId,
                                                        'mdPath': $scope.import.mdPath}})
                           .then(angular.noop,
                        function(){//error
                            Growl('error','Error','Fail to save the uploaded metadata.');
                        }
                    );
                }
                Growl('success','Success','Postgis database successfully added');
                $modalInstance.close({type: "vector",
                                      file: $scope.import.identifier,
                                      metadataFile:$scope.import.metadata,
                                      completeMetadata:$scope.import.fillMetadata});
            });
        };

        $scope.uploaded = function() {
            if ($scope.import.importType === 'empty' && $scope.import.dataName) {
                //empty dataset case
                Examind.datas.createDataset($scope.import.dataName, $scope.import.mdPath).then(
                        function(response){//success
                            Growl('success','Success','Data set '+ $scope.import.dataName +' successfully created');
                            $modalInstance.close({file: $scope.import.dataName, type: "import"});
                        },
                        function(response){//error
                            Growl('error','Error','Fail to create dataset '+ $scope.import.dataName);
                            $modalInstance.close();
                });

            } else if ($scope.import.dataPath && $scope.import.dataPath.indexOf('failed') === -1) {
                // dataset creation with data
                var upFile = $scope.import.dataPath;
                var upMdFile = null;
                if ($scope.import.mdPath && $scope.import.mdPath.indexOf('failed') === -1) {
                    upMdFile = $scope.import.mdPath;
                }
                // Stores uploaded files in session for further use
                var upFiles = UploadFiles.files;
                upFiles.file = $scope.import.dataName;
                upFiles.mdFile = upMdFile;

                var justFile = upFile.substring(upFile.lastIndexOf("/")+1);
                var fileName = justFile;
                var fileExtension;
                if (fileName.indexOf(".") !== -1) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    fileExtension = justFile.substring(justFile.lastIndexOf(".")+1);
                }
                Examind.datas.proceedToImport($scope.import.dataPath, 
                                              $scope.import.mdPath,
                                              $scope.import.uploadType,
                                              $scope.import.dataName,
                                              fileExtension,
                                              $scope.import.fsserver).then(
                    function(response) {//success
                        if(response && response.data){
                            var importedMetaData = response.data.metadataFile;
                            $scope.import.metadata = importedMetaData;
                            $scope.import.providerId = $scope.import.dataName;
                            $scope.import.uploadType = response.data.dataType;
                            if ($scope.import.uploadType === "vector") {
                                if('success' === response.data.verifyCRS){
                                    UploadFiles.files = {
                                        file: $scope.import.providerId,
                                        mdFile: importedMetaData,
                                        providerId: $scope.import.providerId
                                    };
                                    Growl('success','Success','Vector data '+ $scope.import.providerId +' successfully added');
                                    if ($scope.sensor.checked) {
                                        $scope.showAssociate();
                                    } else {
                                        $modalInstance.close({type: $scope.import.uploadType,
                                                              file: $scope.import.providerId,
                                                              metadataFile:$scope.import.metadata,
                                                              completeMetadata:$scope.import.fillMetadata});
                                    }
                                }else if('error' === response.data.verifyCRS) {
                                    Growl('warning','CRS','Data '+ $scope.import.providerId +' without Projection');
                                    $scope.import.allowSubmit = false;
                                    $scope.import.enableSelectEPSGCode = true;
                                    if(response.data.codes){
                                        $scope.epsgList = response.data.codes;
                                        $scope.import.fileName = $scope.import.providerId;
                                    }else {
                                        Growl('error','Error','Impossible to get all EPSG codes');
                                    }
                                }
                            }else if ($scope.import.uploadType === "raster") {
                                if('success' === response.data.verifyCRS){
                                    UploadFiles.files = {
                                        file: $scope.import.providerId,
                                        mdFile: importedMetaData,
                                        providerId: $scope.import.providerId
                                    };
                                    if (!fileExtension || fileExtension !== "nc") {
                                        Growl('success','Success','Raster data '+ $scope.import.providerId +' successfully added');
                                        $modalInstance.close({type: $scope.import.uploadType,
                                                              file: $scope.import.providerId,
                                                              metadataFile:$scope.import.metadata,
                                                              completeMetadata:$scope.import.fillMetadata});
                                    } else {
                                        $scope.showAssociate();
                                    }
                                }else if('error' === response.data.verifyCRS) {
                                    Growl('warning','CRS','Data '+ $scope.import.providerId +' without Projection');
                                    $scope.import.allowSubmit = false;
                                    $scope.import.enableSelectEPSGCode = true;
                                    if(response.data.codes){
                                        $scope.epsgList = response.data.codes;
                                        $scope.import.fileName = $scope.import.providerId;
                                    } else {
                                        Growl('error','Error','Impossible to get all EPSG codes');
                                    }
                                }
                            }else if ($scope.import.uploadType === "observation" && fileExtension === "xml") {
                                Growl('success','Success','Observation data '+ fileName +' successfully added');
                                $scope.showAssociate();
                            } else if ($scope.import.uploadType === "observation") {
                                Growl('success','Success','Observation data '+ fileName +' successfully added');
                                $scope.showAssociate();
                            }
                        }
                    },function(response){//error
                        Growl('error','Error','An error occurred during data import. Please contact an administrator for more information.');
                        $modalInstance.close();
                });
            } else {
                Growl('error','Error','An error occurred during data import. Please contact an administrator for more information.');
                $modalInstance.close();
            }
        };

        $scope.import.finish = function() {
            if ($scope.import.uploadType || $scope.import.importType === 'empty') {
                $scope.uploaded();
            } else {
                Growl('error','Error','Select Data Type');
            }
        };

        $scope.addProjection = function () {
            var codeEpsg = $scope.import.epsgSelected.trim();
            if(codeEpsg.indexOf(' ')!== -1){
                codeEpsg = 'EPSG:'+codeEpsg.substring(0,codeEpsg.indexOf(' '));
            }
            Examind.providers.createPRJ($scope.import.fileName, codeEpsg).then(
                function(){//success
                    UploadFiles.files = {
                        file: $scope.import.providerId,
                        mdFile: $scope.import.metadata,
                        providerId: $scope.import.providerId
                    };
                    $modalInstance.close({type: $scope.import.uploadType,
                                          file: $scope.import.providerId,
                                          metadataFile:$scope.import.metadata,
                                          completeMetadata:$scope.import.fillMetadata});
                },
                function(response){//error
                    var msgError = '';
                    if(response && response.data && response.data.message) {
                        msgError = response.data.message;
                    }
                    Growl('error','Error','Impossible to set projection : '+msgError);
                }
            );
        };
    })

    .controller('ModalImportDataStep1LocalController', function($scope, Growl, cfpLoadingBar, Examind) {
        $scope.loader = {
            upload: false
        };

        $scope.import.allowNext = false;
        $scope.import.next = function() {
            $scope.uploadData();
        };

        $scope.uploadData = function() {
            var $form = $('#uploadDataForm');
            var fileInput = $form.find('input:file');
            if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                return;
            }
            var fileSize = fileInput.get(0).files[0].size/1000000;
            if(fileSize > 200){
                Growl('error', 'Error', 'The file size exceed the limitation of 200Mo per file.');
                return;
            }

            var formData = new FormData($form[0]);
            $scope.loader.upload = true;
            var beforeSend = function(){
                cfpLoadingBar.start();
                cfpLoadingBar.inc();
            };
            beforeSend();
            Examind.datas.uploadData(formData).then(
                function (response) {
                    $scope.import.dataPath = response.data.dataPath;
                    $scope.loader.upload = false;
                    $scope.import.currentStep = 'step2Metadata';
                    $scope.import.allowNext = true;
                    cfpLoadingBar.complete();
                },
                function(){
                    Growl('error', 'Error', 'error while uploading data');
                    cfpLoadingBar.complete();
                }
            );
        };

        $scope.verifyExtension = function(path) {
            var lastPointIndex = path.lastIndexOf(".");
            var extension = path.substring(lastPointIndex+1, path.length);
            Examind.datas.testExtension(extension).then(
                function(response) {//success
                    if (response && response.data && response.data.dataType) {
                        $scope.import.uploadType = response.data.dataType;
                    }
                    $scope.import.allowNext = true;
                }
            );
        };
    })

    .controller('ModalImportDataStep1ServerController', function($scope, Examind, Growl) {
        $scope.import.allowNext = false;
        $scope.import.fsserver = true;

        $scope.columns = [];

        $scope.load = function(){
            $scope.import.allowNext = false;
            var path = $scope.import.currentPath;
            Examind.datas.getDataFolder(path, true).then(
                function(files) {
                    if(files.data.length>0) {
                        $scope.import.currentPath = files.data[0].parentPath;
                    }
                    $scope.columns = files.data;
                },
                function(resp){//error
                    var msg = 'The file path is invalid';
                    if(resp.data && resp.data.errorMessage){
                        //@TODO use i18n key returned by the server resp.data.errorMessageI18nCode
                        //msg = $filter('translate')(resp.data.errorMessageI18nCode);
                        msg = resp.data.errorMessage;
                    }
                    Growl('error','Error',msg);
                }
            );
        };

        $scope.open = function(path, depth) {
            $scope.load(path);
        };

        $scope.select = function(item) {
            $scope.import.currentPath = item.path;
            if(item.folder) {
                $scope.load();
            }
            $scope.import.allowNext = true;
        };

        $scope.startWith = function(path) {
            return $scope.import.currentPath.indexOf(path) === 0;
        };


        $scope.import.next = function() {
            // Use selected data
            $scope.import.dataPath = $scope.import.currentPath;
            $scope.import.currentStep = 'step2Metadata';
            $scope.import.allowNext = true;
        };
    })

    .controller('ModalImportDataStep1DatabaseController', function($scope, Examind, Growl) {
        $scope.import.allowNext = false;
        $scope.import.testConnected = false;

        $scope.import.next = function() {
            $scope.import.currentStep = 'step2Metadata';

        };
        $scope.testDB = function(){
            var providerId = "postgis-"+ $scope.import.db.name;
            Examind.providers.test(providerId,
            {
                type: "data-store",
                subType: "postgresql",
                parameters: {
                    host: $scope.import.db.url,
                    port: $scope.import.db.port,
                    user: $scope.import.db.user,
                    password: $scope.import.db.password,
                    database: $scope.import.db.name
                }
            }).then(function(response) {//success
                Growl('success', 'Success', 'Connected to database');
                $scope.import.testConnected = true;
                $scope.import.allowNext = true;

            },function(response){//error
                Growl('error','Error',response.data);
            });
        };

})

    .controller('ModalImportDataStep2MetadataController', function($scope, Growl, cfpLoadingBar, Examind) {

        $scope.columns = [];

        $scope.load = function(){
            $scope.import.allowNext = false;
            $scope.import.allowSubmit = false;
            var path = $scope.import.currentMDPath;
            Examind.datas.getMetaDataFolder(path, true).then(
                function(files) {//success
                    if(files.data.length>0) {
                        $scope.import.currentMDPath = files.data[0].parentPath;
                    }
                    $scope.columns = files.data;
                },
                function(resp){//error
                    var msg = 'The file path is invalid';
                    if(resp.data && resp.data.msg){
                        msg = resp.data.msg;
                    }
                    Growl('error','Error',msg);
                });
        };

        $scope.open = function(path, depth) {
            $scope.load(path);
        };

        $scope.select = function(item) {

            if (!item.folder) {
                $scope.import.metadata = item.path;
                $scope.import.currentMDPath = item.path;
                $scope.import.identifier = null;
                $scope.verifyAllowNext();
            }else{
                $scope.import.currentMDPath = item.path;
                $scope.load();
            }

        };

        $scope.startWith = function(path) {
            return $scope.import.currentMDPath.indexOf(path) === 0;
        };

        $scope.import.allowNext = false;
        if ($scope.import.dataPath && $scope.import.dataPath.length > 0){
            $scope.import.identifier = $scope.import.dataPath.replace(/^.*(\\|\/|\:)/, '').substr(0,$scope.import.dataPath.replace(/^.*(\\|\/|\:)/, '').lastIndexOf('.'));
        }
        if ($scope.import.identifier && $scope.import.identifier.length > 0) {

            //final step if empty dataset creation is selected
            if ($scope.import.importType === 'empty') {
                $scope.import.allowSubmit = true;
            } else {
                $scope.import.allowNext = true;
            }
        }

        $scope.verifyAllowNext = function(){
                $scope.import.allowNext = false;
                $scope.import.allowSubmit = false;
                if (($scope.import.identifier && $scope.import.identifier.length > 0) ) {
                    var letters = /^[A-Za-zàèìòùáéíóúäëïöüñãõåæøâêîôû0-9\-_]+$/;
                    var id = $scope.import.identifier;
                    if(!id.match(letters)) {
                        Growl('error','Error','fill identifier without special chars like space');
                    }else {
                        if ($scope.import.importType === 'empty') {
                            $scope.import.allowSubmit = true;
                        } else {
                            $scope.import.allowNext = true;
                        }
                    }
                }else if ($scope.import.metadata && $scope.import.metadata.length > 0) {
                    if ($scope.import.importType === 'empty') {
                        $scope.import.allowSubmit = true;
                    } else {
                        $scope.import.allowNext = true;
                    }
                }
        };

        $scope.import.next = function() {
            if ($scope.import.metadata || $scope.import.identifier) {
                $scope.uploadMetadata();
            } else {
                $scope.selectType();
            }
        };

        $scope.import.finish = function() {

            var finishUpload = false;
            if ($scope.import.importType === 'empty') {
                if ($scope.import.metadata || $scope.import.identifier) {
                    $scope.uploadMetadata();
                } else {
                    finishUpload = true;
                }
            } else {
                if ($scope.import.uploadType) {
                    finishUpload = true;
                } else {
                    Growl('error', 'Error', 'Select Data Type');
                }
            }

            if (finishUpload) {
                $scope.uploaded();
            }
        };

        $scope.selectType = function(){
            $scope.import.allowNext = false;
            if ($scope.import.db.url) {
                $scope.importDb();
            } else if ($scope.import.importType === 'empty') {
                //skip datatype fragment when we're on empty dataset creation.
                $scope.uploaded();
            } else if (!$scope.import.uploadType) {
                $scope.import.currentStep = 'step3Type';
                $scope.import.allowSubmit = true;
            } else {
                $scope.uploaded();
            }
        };

        $scope.uploadMetadata = function() {
            var $form = $('#uploadMetadataForm');
            var formData = new FormData($form[0]);
            var beforeSend = function(){
                cfpLoadingBar.start();
                cfpLoadingBar.inc();
            };
            beforeSend();
            Examind.datas.uploadMetadata(formData).then(
                function(result) {
                    $scope.import.mdPath = result.data.metadataPath;
                    $scope.import.dataName = result.data.dataName;
                    $scope.import.dataTitle = result.data.metatitle;
                    $scope.import.metaIdentifier = result.data.metaIdentifier;
                    $scope.selectType();
                    cfpLoadingBar.complete();
                },
                function(response){
                    if(response && response.data) {
                        Growl('error','Error',response.data.errorMessage);
                    }
                    cfpLoadingBar.complete();
                }
            );
        };

        $scope.metadataChosen = function(md) {
            $scope.$apply(function() {
                $scope.import.metadata = md.value;
                $scope.import.identifier = null;
                if ($scope.import.metadata && $scope.import.metadata.length > 0){
                    $scope.import.allowNext = true;
                }
            });
        };

    })

    .controller('ModalImportDataStep3TypeController', function($scope) {
        $scope.changeAssociationState = function() {
            if ($scope.sensor.checked) {
                $scope.import.allowSensorChoose = true;
                $scope.import.allowSubmit = false;
            } else {
                $scope.import.allowSensorChoose = false;
                $scope.import.allowSubmit = true;
            }
        };
    })

    .controller('ModalImportDataStep4SensorController', function($scope, Dashboard, Growl,
                                                                 cfpLoadingBar, Examind) {
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.selectedSensorsChild = null;

        $scope.selectSensorsChild = function(item) {
            if ($scope.selectedSensorsChild === item) {
                $scope.selectedSensorsChild = null;
            } else {
                $scope.selectedSensorsChild = item;
            }
        };

        $scope.initDashboardSensor = function() {
            Examind.sensors.list().then(function(response) {
                Dashboard($scope, response.data.children, false);
                $scope.wrap.nbbypage = 5;
            });
        };

        $scope.import.finish = function() {
            if ($scope.sensor.mode === 'existing') {
                var sensorId = ($scope.selectedSensorsChild) ? $scope.selectedSensorsChild.id : $scope.selected.id;
                Examind.datas.getDataListForProvider($scope.import.providerId).then(
                    function(response){//success
                        var datas = response.data;
                        for (var i=0; i<datas.length; i++) {
                            Examind.datas.linkDataToSensor(datas[i].id, sensorId);
                        }
                });
            } else if ($scope.sensor.mode === 'automatic') {
                Examind.datas.getDataListForProvider($scope.import.providerId).then(
                    function(response){//success
                        var datas = response.data;
                        for (var i=0; i<datas.length; i++) {
                            Examind.sensors.generateSML(datas[i].id);
                        }
                });
            } else {
                // Import sensorML
                $scope.uploadImportAndLinkSensor();
            }

            $scope.close();
        };

        $scope.uploadImportAndLinkSensor = function() {
            var $form = $('#uploadSensor');

            var formData = new FormData($form[0]);

            var beforeSend = function(){
                cfpLoadingBar.start();
                cfpLoadingBar.inc();
            };
            beforeSend();
            Examind.datas.uploadData(formData).then(
                function (response) {
                    importAndLinkSensor(response.data.dataPath);
                    cfpLoadingBar.complete();
                },
                function (data){
                    Growl('error','Error','Unable to upload sensor');
                    cfpLoadingBar.complete();
                }
            );
        };

        //success
        function linkSensorImported(sensorId,list) {
            for (var i=0; i<list.length; i++) {
                Examind.datas.linkDataToSensor(list[i].id, sensorId);
            }
        }

        function importAndLinkSensor(path) {
            Examind.sensors.add({values: {'path' : path}}).then(function(sensors) {
                Growl('success','Success','Sensor correctly imported');
                Examind.providers.getDataList($scope.import.providerId).then(
                    function(response) {
                        for (var s=0; s<sensors.data.length; s++) {
                            var sensorId = sensors.data[s].id;
                            linkSensorImported(sensorId,response.data);
                        }
                    }
                );
            }, function() {
                Growl('error','Error','Unable to import sensor');
            });
        }
    })

    .controller('ModalImportCustomStep1Controller', function($scope, Growl, Examind) {
        var self = $scope;

        self.import.allowNext = false;
        self.import.allowSubmit = true;

        self.options = {
            config : null,
            types : [],
            epsgCodes : [],
            selectedType : null
        };

        self.init = function() {
            Examind.datas.getAllDataStoreConfigurations('all').then(
                function(response){
                    self.options.config = response.data;
                    if(response.data.types) {
                        self.options.types = response.data.types;
                    }else {
                        self.options.types = [];
                    }
                },
                function(response){
                    Growl('error', 'Error', 'An error happen when getting types');
                    self.options.types = [];
                }
            );
            Examind.crs.listAll().then(
                function(response){
                    self.options.epsgCodes = response.data;
            });

        };
        self.init();

        self.import.finish = function() {
            if(self.options.selectedType) {
                Examind.datas.putDataStoreConfiguration(self.options.selectedType, 'false').then(
                    function(response){
                        Growl('success','Success','Configuration saved successfully!');
                        self.close();
                    },
                    function(response){
                        var msg='';
                        if(response && response.data && response.data.errorMessage) {
                            msg = response.data.errorMessage;
                        }
                        Growl('error', 'Error', 'An error happen when saving configuration! '+msg);
                    }
                );
            } else {
                self.close();
            }
        };


    });