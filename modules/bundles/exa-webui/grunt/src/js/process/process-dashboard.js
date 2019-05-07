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

angular.module('cstl-process-dashboard', ['cstl-restapi', 'cstl-services', 'ui.bootstrap.modal', 'examind-instance'])

    .controller('TasksController', function ($scope, Dashboard, Growl, $modal, StompService, $window, Examind){
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};
        $scope.wrap.nbbypage = 10; // Default value at 10
        $scope.wrap.ordertype='date';
        $scope.wrap.orderreverse=true;
        $scope.historySize = 100;
        $scope.hideScroll = true;

        $scope.init = function() {
            Examind.tasks.listTaskParams()
                .then(function(response){

                    var filterList = response.data.filter(function (task) {
                        return task.type !== 'INTERNAL';
                    });

                    // On success
                    Dashboard($scope, filterList, false);
                    //connect to websocket
                    StompService.connect(function(){
                        if($scope.selected){
                            $scope.subscribe($scope.selected);
                        }
                    });
                }).catch(function(){
                    // On error
                    Growl('error', 'Error', 'Unable to get tasks list');
                })['finally'](function(){
                // On all case
            });

            angular.element($window).bind("scroll", function() {
                if (this.pageYOffset < 220) {
                    $scope.hideScroll = true;
                } else {
                    $scope.hideScroll = false;
                }
                $scope.$apply();
            });
        };

        $scope.resetFilters = function(){
            Examind.tasks.listTaskParams().then(
                function(response) {//success
                    var filterList = response.data.filter(function (task) {
                        return task.type !== 'INTERNAL';
                    });
                    Dashboard($scope, filterList, false);
                    $scope.wrap.ordertype='date';
                    $scope.wrap.orderreverse=true;
                    $scope.wrap.filtertext='';
                },
                function() {//error
                    Growl('error','Error','Unable to restore list of process!');
                });
        };

        $scope.subscribe = function (task) {
            Examind.tasks.getTaskHistory(task.id, $scope.historySize)
              .then(function(response){
                  // On success
                  task.statusList = response.data;
                  var topicPath = '/topic/taskevents/'+task.id;
                  task.topic = StompService.subscribe(topicPath, function(data) {
                      var event = JSON.parse(data.body);

                      var filter = task.statusList.filter(function (elem) {
                          return elem.id === event.id;
                      });
                      var status = filter[0];
                      if (status) {
                          $scope.$apply( function (){
                              status.percent = event.percent;
                              status.status = event.status;
                              status.message = event.message;
                              status.end = event.end;
                              status.output = event.output;
                          });
                      } else {
                          // New execution
                          $scope.$apply( function (){
                              task.statusList.unshift(event);
                          });
                      }
                  });
              }).catch(function(){
                  // On error
                  Growl('error', 'Error', 'Unable to get task running list');
                  return;
              });
        };

        $scope.$on('$destroy', function() {
            if ($scope.selected !== null) {
                $scope.selected.topic.unsubscribe();
            }
            StompService.disconnect();
        });

        $scope.select = function(item) {
            var oldSelect = $scope.selected;
            $scope.selected = null;
            if (oldSelect !== null) {
                oldSelect.topic.unsubscribe();
                oldSelect.topic = null;
            }

            if (oldSelect !== item) {
                $scope.selected = item;
                $scope.subscribe($scope.selected);
            }
        };

        $scope.deleteTask = function(idTask) {
            Examind.tasks.deleteParamsTask(idTask).then(function(){
                $scope.init();
                $scope.selected=null;
            });
        };

        $scope.executeTask = function(idTask) {
            Examind.tasks.executeParamsTask(idTask).then(function(){
                Growl('success', 'Success', 'The task is currently execute');
            }).catch(function(){
                Growl('error', 'Error', "Can't execute this task");
            });
        };

        $scope.duplicateTask = function(idTask) {
            Examind.tasks.duplicateParamsTask(idTask).then(function(){
                Growl('success', 'Success', 'The task is currently duplicated');
                $scope.init();
            }).catch(function(){
                Growl('error', 'Error', "Can't duplicate this task");
            });
        };

        $scope.cancelTask = function(idTask) {
            Examind.tasks.cancelTaskParam(idTask).then(function(){
                Growl('success', 'Success', 'The tasks have been cancelled');
                $scope.init();
            }).catch(function(){
                Growl('error', 'Error', "Can't cancel this tasks");
            });
        };


        $scope.showPlanningTaskPopup = function(idTask) {

            var modal = $modal.open({
                templateUrl: 'views/tasks/modalPlanningTask.html',
                controller: 'ModalPlanTaskController',
                resolve : {
                    'task'      : function(){
                        if (idTask>=0){
                            return Examind.tasks.getParamsTask(idTask);
                        } else {
                            return {
                                'id' : null,
                                'name' : ""
                            };
                        }
                    }
                }
            });
            modal.result.then(function(){
                $scope.init();
            });
        };

        $scope.toggleUpDownSelected = function() {
            var $header = $('#ProcessDashboard').find('.selected-item').find('.block-header');
            $header.nextAll().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        // Open the add task modal
        $scope.showAddTaskPopup = function(idTask) {
            var modal = $modal.open({
                templateUrl: 'views/tasks/modalAddTask.html',
                controller: 'ModalAddTaskController',
                resolve : {
                    'processes' : function(){return Examind.tasks.listProcesses();},
                    'displayStep':function(){
                        return angular.isDefined(idTask) ? 'configParameters' :'chooseSource';
                    },
                    'task'      : function(){
                        if (angular.isDefined(idTask)){
                            return Examind.tasks.getParamsTask(idTask);
                        } else {
                            return {
                                'id' : null,
                                'name' : "",
                                'inputs': {}
                            };
                        }
                    }
                }
            });
            modal.result.then(function(){
                $scope.init();
            });
        };

        // Open task status message popup
        $scope.showMessage = function(taskStatus) {
            var modal = $modal.open({
                templateUrl: 'views/tasks/modalStatusMessage.html',
                controller: 'ModalStatusMessageController',
                resolve : {
                    'taskStatus' : function () {return taskStatus;}
                }
            });
        };

        $scope.statusClass = function(status) {
            if (status.status === 'FAILED' || status.status === 'CANCELLED') {
                return "danger";
            }
            if (status.status === 'WARNING') {
                return "warning";
            }
            if (status.status === 'SUCCEED') {
                return "success";
            }
        };

        $scope.statusHistoryFilter = function(status) {
            return (status.status === 'FAILED' ||
                status.status === 'CANCELLED' ||
                status.status === 'WARNING' ||
                status.status === 'SUCCEED');
        };

        $scope.messageType = function(status) {
            if (status.status === 'FAILED' || status.status === 'CANCELLED') {
                return 'tasks.history.show.error';
            }
            if (status.status === 'SUCCEED' || status.status === 'WARNING') {
                return 'tasks.history.show.success';
            }
        };

        $scope.statusLabelKey = function(status) {
            switch(status.status) {
                case 'SUCCEED' : return 'task.status.succeed';
                case 'WARNING' : return 'task.status.warning';
                case 'FAILED' : return 'task.status.failed';
                case 'CANCELLED' : return 'task.status.cancelled';
                default : return status.status;
            }
        };

        $scope.truncate = function(small, text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (small === true && text.length > 30) {
                        return text.substr(0, 30) + "...";
                    } else if (small === false && text.length > 65) {
                        return text.substr(0, 65) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (small === true && text.length > 22) {
                        return text.substr(0, 22) + "...";
                    } else if (small === false && text.length > 50) {
                        return text.substr(0, 50) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 992) {
                    if (text.length > 40) {
                        return text.substr(0, 40) + "...";
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
    });