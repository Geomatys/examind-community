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

angular.module('cstl-webservice-dashboard', ['cstl-restapi', 'cstl-services', 'pascalprecht.translate', 'ui.bootstrap.modal', 'examind-instance'])

    .controller('WebServiceController', function($modal, Growl, $translate, Examind) {
        var self = this;
        self.typeFilter = {type: '!WEBDAV'};

        self.getCurrentLang = function() {
            return $translate.use();
        };

        Examind.services.getInstances(null,self.getCurrentLang()).then(
            function(response){//success
                self.services = response.data;
            },
            function() {//error
                Growl('error','Error','Unable to show services list!');
            }
        );

        // Show Capa methods
        self.showCapa = function(service) {
            if (service.versions.length > 1) {
                var modal = $modal.open({
                    templateUrl: 'views/webservice/modalChooseVersion.html',
                    controller: 'WebServiceVersionsController',
                    resolve: {
                        service: function() { return service; }
                    }
                });
                modal.result.then(function(result) {
                    showModalCapa(service, result);
                });
            } else {
                showModalCapa(service, service.versions[0]);
            }
        };

        function showModalCapa(service, version) {
            $modal.open({
                templateUrl: 'views/webservice/modalCapa.html',
                controller: 'WebServiceUtilsController',
                resolve: {
                    'details': function(textService){
                        return textService.capa(service.type.toLowerCase(), service.identifier, version);
                    }
                }
            });
        }

        // Show Logs methods
        self.showLogs = function(service) {
            $modal.open({
                templateUrl: 'views/webservice/modalLogs.html',
                controller: 'WebServiceUtilsController',
                resolve: {
                    'details': function(textService){
                        return Examind.services.getLogs(service.type.toLowerCase(), service.identifier);
                    }
                }
            });
        };

        self.reload = function(service){
            Examind.ogcServices.restart(service.type, service.identifier, true).then(
                function() { Growl('success','Success','Service '+ service.name +' successfully reloaded'); },
                function() { Growl('error','Error','Service '+ service.name +' reload failed'); }
            );
        };

        self.startOrStop = function(service){
            if(service.status==='STARTED'){
                Examind.ogcServices.stop(service.type, service.identifier).then(function(response) {
                    if (response.data.status==="Success") {
                        Examind.services.getInstances(null,self.getCurrentLang()).then(
                                function(response){self.services = response.data;});
                        Growl('success','Success','Service '+ service.name +' successfully stopped');
                    }
                }, function() { Growl('error','Error','Service '+ service.name +' stop failed'); });
            }else{
                Examind.ogcServices.start(service.type, service.identifier).then(function(response) {
                    if (response.data.status==="Success") {
                        Examind.services.getInstances(null,self.getCurrentLang()).then(
                                function(response){self.services = response.data;});
                        Growl('success','Success','Service '+ service.name +' successfully started');
                    }
                }, function() { Growl('error','Error','Service '+ service.name +' start failed'); });
            }
        };

        self.deleteService = function(service) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.service";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    Examind.ogcServices.delete(service.type, service.identifier).then(
                        function() { 
                            Growl('success','Success','Service '+ service.name +' successfully deleted');
                            
                            Examind.services.getInstances(null,self.getCurrentLang()).then(
                                function(response){self.services = response.data;});
                        },
                        function() { Growl('error','Error','Service '+ service.name +' deletion failed'); }
                    );
                }
            });
        };

        self.refreshIndex = function(service) {
            Examind.csw.refresh(service.identifier, false, false).then(
                function() { Growl('success','Success','Search index for the service '+ service.name +' successfully refreshed'); },
                function() { Growl('error','Error','Search index for the service '+ service.name +' failed to be updated'); }
            );
        };
    });