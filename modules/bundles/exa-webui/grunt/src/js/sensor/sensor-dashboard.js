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

angular.module('cstl-sensor-dashboard', ['cstl-restapi', 'cstl-services', 'ui.bootstrap.modal'])

    .controller('SensorsController', function($scope, $routeParams, $filter, Dashboard,
                                              $modal, Growl, $window, Examind) {
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};
        $scope.wrap.ordertype = 'id';
        $scope.sensorCtrl = {
            cstlUrl : window.localStorage.getItem('cstlUrl'),
            selectedSensor : null,
            selectedSensorsChild : null,
            smallMode : false,
            hideScroll : true,
            highlightSensor:$routeParams.id || null
        };

        /**
         * main function of dashboard that loads the list of objects from server.
         */
        $scope.init = function() {
            Examind.sensors.list().then(function(response) {//success
                Dashboard($scope, response.data.children, false);
                if($scope.sensorCtrl.highlightSensor){
                    var resArray = $filter('filter')(response.children, {'id': $scope.sensorCtrl.highlightSensor});
                    if(resArray && resArray.length>0){
                        $scope.sensorCtrl.selectedSensor=resArray[0];
                        $scope.wrap.filtertext = $scope.sensorCtrl.highlightSensor;
                    }
                }else {
                    $scope.sensorCtrl.selectedSensor=null;
                    $scope.wrap.filtertext='';
                }
            }, function() {//error
                Growl('error','Error','Unable to show sensor list!');
            });
            angular.element($window).bind("scroll", function() {
                if (this.pageYOffset < 220) {
                    $scope.sensorCtrl.hideScroll = true;
                } else {
                    $scope.sensorCtrl.hideScroll = false;
                }
                $scope.$apply();
            });
        };

        /**
         * Reset filters for dashboard
         */
        $scope.resetFilters = function(){
            Examind.sensors.list().then(function(response) {//success
                Dashboard($scope, response.data.children, false);
                $scope.wrap.ordertype='id';
                $scope.wrap.orderreverse=false;
                $scope.wrap.filtertext='';
                $scope.sensorCtrl.selectedSensorsChild = null;
                $scope.sensorCtrl.selectedSensor = null;
            }, function() {//error
                Growl('error','Error','Unable to show sensor list!');
            });
        };

        $scope.toggleUpDownSelected = function() {
            var $header = $('#sensorDashboard').find('.selected-item').find('.block-header');
            $header.next().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        $scope.addSensor = function() {
            var modal = $modal.open({
                templateUrl: 'views/sensor/modalAddSensor.html',
                controller: 'SensorAddModalController'
            });
            modal.result.then(function() {
                Examind.sensors.list().then(function(response) {
                    Dashboard($scope, response.data.children, false);
                });
            });
        };

        $scope.toggleSelectSensor = function(item) {
            if (item && $scope.sensorCtrl.selectedSensor && $scope.sensorCtrl.selectedSensor.id === item.id) {
                $scope.sensorCtrl.selectedSensor = null;
            } else {
                $scope.sensorCtrl.selectedSensor = item;
            }
        };

        $scope.selectSensorsChild = function(item) {
            if ($scope.sensorCtrl.selectedSensorsChild === item) {
                $scope.sensorCtrl.selectedSensorsChild = null;
            } else {
                $scope.sensorCtrl.selectedSensorsChild = item;
            }
        };

        $scope.deleteSensor = function() {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.sensor";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){

                    var rmd = $modal.open({
                        templateUrl: 'views/modal-confirm.html',
                        controller: 'ModalConfirmController',
                        resolve: {
                            'keyMsg':function(){return "dialog.message.confirm.delete.sensor.linkeddata";}
                        }
                    });
                    
                    var removeData = false;
                    rmd.result.then(function(cfrm2) {
                        if (cfrm2) {
                           removeData = true;
                        }

                        var idToDel = ($scope.sensorCtrl.selectedSensorsChild) ? $scope.sensorCtrl.selectedSensorsChild.id : $scope.sensorCtrl.selectedSensor.id;
                        Examind.sensors.delete(idToDel, removeData).then(function () {
                            Growl('success', 'Success', 'Sensor ' + idToDel + ' successfully removed');
                            Examind.sensors.list().then(function(response) {
                                Dashboard($scope, response.data.children, false);
                                $scope.sensorCtrl.selectedSensor = null;
                                $scope.sensorCtrl.selectedSensorsChild = null;
                            });
                        }, function () {
                            Growl('error', 'Error', 'Unable to remove sensor ' + idToDel);
                        });
                    });
                }
            });
        };

        /**
         * Open metadata viewer popup and display metadata sensorML
         * this function is called from sensor dashboard.
         */
        $scope.displayMetadataSensorML = function() {
            var idToView,typeToSend;
            if(($scope.sensorCtrl.selectedSensorsChild)){
                idToView = $scope.sensorCtrl.selectedSensorsChild.id;
                typeToSend = $scope.sensorCtrl.selectedSensorsChild.type;
            }else {
                idToView = $scope.sensorCtrl.selectedSensor.id;
                typeToSend = $scope.sensorCtrl.selectedSensor.type;
            }
            $modal.open({
                templateUrl: 'views/data/modalViewMetadata.html',
                controller: 'ViewMetadataModalController',
                resolve: {
                    'dashboardName':function(){return 'sensor';},
                    'metadataValues':function(){
                        return Examind.sensors.getMetadata(idToView,typeToSend,true);
                    }
                }
            });
        };

        /**
         * Open metadata editor in modal popup.
         */
        $scope.displayMetadataSensorMLEditor = function() {
            var sensorId,typeToSend;
            if($scope.sensorCtrl.selectedSensorsChild){
                sensorId = $scope.sensorCtrl.selectedSensorsChild.id;
                typeToSend = $scope.sensorCtrl.selectedSensorsChild.type;
            }else {
                sensorId = $scope.sensorCtrl.selectedSensor.id;
                typeToSend = $scope.sensorCtrl.selectedSensor.type;
            }
            openModalEditor(sensorId,typeToSend,typeToSend);
        };

        /**
         * Open modal for metadata editor
         * for given provider id, data type and template.
         * @param id
         * @param type
         * @param template
         */
        function openModalEditor(id,type,template){
            $modal.open({
                templateUrl: 'views/data/modalEditMetadata.html',
                controller: 'EditSensorMLController',
                resolve: {
                    'id':function(){return id;},
                    'type':function(){return type;},
                    'template':function(){return template;}
                }
            });
        }

        $scope.truncate = function(small, text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (small === true && text.length > 30) {
                        return text.substr(0, 30) + "...";
                    } else if (small === false && text.length > 60) {
                        return text.substr(0, 60) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (small === true && text.length > 22) {
                        return text.substr(0, 22) + "...";
                    } else if (small === false && text.length > 42) {
                        return text.substr(0, 42) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 992) {
                    if (text.length > 22) {
                        return text.substr(0, 22) + "...";
                    } else {return text;}
                }
            }
        };
        $scope.truncateTitleBlock = function(text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (text.length > 40) {
                        return text.substr(0, 40) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (text.length > 30) {
                        return text.substr(0, 30) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 992) {
                    if (text.length > 20) {
                        return text.substr(0, 20) + "...";
                    } else {return text;}
                }
            }
        };
    })

    .controller('SensorModalChooseController', function($scope, $modalInstance, Dashboard, Examind, selectedData,
                                                        cfpLoadingBar,Growl){
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.sensorModalOpts = {
            sensorMode : 'existing',
            selectedSensor : null,
            selectedSensorsChild : null,
            hideScroll : true
        };

        $scope.close = function() {
            $modalInstance.dismiss('close');
        };

        $scope.initDashboardSensor = function() {
            Examind.sensors.list().then(function(response) {
                Dashboard($scope, response.data.children, false);
                $scope.wrap.nbbypage = 5;
            });
        };

        $scope.toggleSelectSensor = function(item) {
            if (item && $scope.sensorModalOpts.selectedSensor &&
                $scope.sensorModalOpts.selectedSensor.id === item.id) {
                $scope.sensorModalOpts.selectedSensor = null;
            } else {
                $scope.sensorModalOpts.selectedSensor = item;
            }
        };

        $scope.selectSensorsChild = function(item) {
            if (item && $scope.sensorModalOpts.selectedSensorsChild &&
                $scope.sensorModalOpts.selectedSensorsChild.id === item.id) {
                $scope.sensorModalOpts.selectedSensorsChild = null;
            } else {
                $scope.sensorModalOpts.selectedSensorsChild = item;
            }
        };

        $scope.choose = function() {
            if ($scope.sensorModalOpts.sensorMode === 'existing') {
                var sensor = ($scope.sensorModalOpts.selectedSensorsChild) ? $scope.sensorModalOpts.selectedSensorsChild : $scope.sensorModalOpts.selectedSensor;
                Examind.datas.linkDataToSensor(selectedData.id,
                    sensor.id).then(
                    function () {
                        selectedData.targetSensor.push(sensor);
                    });
            } else if ($scope.sensorModalOpts.sensorMode === 'automatic') {
                Examind.sensors.generateSML(selectedData.id);
            } else {
                // Import sensorML
                $scope.uploadImportAndLinkSensor();
            }
            $modalInstance.dismiss('close');
        };

        $scope.truncate = function(text){
            if(text) {
                if (text.length > 30) {
                    return text.substr(0, 30) + "...";
                } else {return text;}
            }
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

        function importAndLinkSensor(path) {
            Examind.sensors.add({values: {'path' : path}}).then(function(sensors) {
                Growl('success','Success','Sensor correctly imported');
                for (var s=0; s<sensors.data.length; s++) {
                    var sensorId = sensors.data[s].id;
                    Examind.datas.linkDataToSensor(selectedData.id, sensorId);
                }
            }, function() {
                Growl('error','Error','Unable to import sensor');
            });
        }

    });