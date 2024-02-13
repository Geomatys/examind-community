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

angular.module('cstl-webservice-create', [
    'cstl-restapi',
    'cstl-services',
    'pascalprecht.translate',
    'examind-instance']
)

    .controller('WebServiceCreateController', function($routeParams,Examind, $filter, $location, Growl, $translate, webserviceFactory, $modal) {
        var self = this;
        self.type = $routeParams.type;
        self.tonext = true;
        self.serviceInfo = true;
        self.serviceContact = false;
        self.metadata = {
            keywords: []
        };
        self.newService = {
            tagText: ''
        };

        self.currentInstances = [];

        self.versions = [];

        self.isValidField = function(input){
            if(input){
                return (input.$valid || input.$pristine);
            }
            return true;
        };

        self.isValidRequired = function(input){
            if(input){
                return ! input.$error.required;
            }
            return true;
        };

        self.isValidForm = function(form) {
            if(form.$error.required){
                for(var i=0;i<form.$error.required.length;i++){
                    form.$error.required[i].$pristine=false;
                }
                return false;
            }
            return true;
        };

        self.isValidVersion = function() {
            var checked=false;
            angular.forEach(self.versions,function(v){
                if(v.checked) {
                    checked=true;
                }
            });
            return checked;
        };

        self.isValidIdentifier = function(input) {
            if(self.metadata.identifier) {
                for(var i=0;i<self.currentInstances.length;i++) {
                    if(self.metadata.identifier === self.currentInstances[i].identifier) {
                        if(input) {
                            input.$setValidity('custom',false);
                        }
                        return false;
                    }
                }
            }
            if(input) {
                input.$setValidity('custom', true);
            }
            return true;
        };

        self.getCurrentLang = function() {
            return $translate.use();
        };


        self.getVersionsForType = function() {
            if (self.type === 'wms') {
                return [{ 'id': '1.1.1'}, { 'id': '1.3.0', 'checked': true }];
            }
            if (self.type === 'wfs') {
                return [{ 'id': '1.1.0', 'checked': true}, { 'id': '2.0.0' }];
            }
            if (self.type === 'wcs') {
                return [{ 'id': '1.0.0'}, { 'id': '2.0.1', 'checked': true}];
            }
            if (self.type === 'wmts') {
                return [{ 'id': '1.0.0', 'checked': true}];
            }
            if (self.type === 'tms') {
                return [{ 'id': '1.0.0', 'checked': true}];
            }
            if (self.type === 'csw') {
                return [{ 'id': '2.0.2', 'checked': true}, { 'id': '3.0.0', 'checked': true}];
            }
            if (self.type === 'sos') {
                return [{ 'id': '1.0.0'}, { 'id': '2.0.0', 'checked': true}];
            }
            if (self.type === 'sts') {
                return [{ 'id': '1.0.0', 'checked': true}];
            }
            if (self.type === 'wps') {
                return [{ 'id': '1.0.0', 'checked': true}, { 'id': '2.0.0'}];
            }
            if (self.type === 'thw') {
                return [{ 'id': '1.0.0', 'checked': true}];
            }
            return [];
        };

        self.goToServiceContact = function(form) {
            if(self.isValidForm(form) && self.isValidVersion() && self.isValidIdentifier()){
                self.serviceContact = true;
                self.serviceInfo = false;
            }
        };

        self.goToServiceInfo = function() {
            self.serviceContact = false;
            self.serviceInfo = true;
        };

        self.addTag = function() {
            if (!self.newService.tagText || self.newService.tagText === '' || self.newService.tagText.length === 0) {
                return;
            }

            self.metadata.keywords.push(self.newService.tagText);
            self.newService.tagText = '';
        };

        self.deleteTag = function(key) {
            if (self.metadata.keywords.length > 0 &&
                self.newService.tagText.length === 0 && !key) {
                self.metadata.keywords.pop();
            } else if (key) {
                self.metadata.keywords.splice(key, 1);
            }
        };

        // define which version to set
        self.selectedVersion = function (){
            var selVersions = $filter('filter')(self.versions, {checked: true});
            var strVersions = [];
            for(var i=0; i < selVersions.length; i++) {
                strVersions.push(selVersions[i].id);
            }
            self.metadata.versions = strVersions;
        };

        // define which version is Selected
        self.versionIsSelected = function(currentVersion){
            return $.inArray(currentVersion, self.metadata.versions) > -1;
        };

        self.saveServiceMetadata = function() {
            // Ensures both name and identifier are filled
            if ((!self.metadata.identifier || self.metadata.identifier === '') && self.metadata.name && self.metadata.name !== '') {
                self.metadata.identifier = self.metadata.name;
            }
            if ((!self.metadata.name || self.metadata.name === '') && self.metadata.identifier && self.metadata.identifier !== '') {
                self.metadata.name = self.metadata.identifier;
            }

            self.metadata.lang = self.getCurrentLang();

            Examind.ogcServices.create(self.type, self.metadata).then(
                function(response) {
                    webserviceFactory.serviceId = response.data.id;
                    Growl('success', 'Success', 'Service ' + self.metadata.name + ' successfully created');
                    if (self.type === 'csw' || self.type === 'sos' || self.type === 'sts' || self.type === 'wfs') {
                        $location.path('/webservice/'+ self.type +'/'+ self.metadata.identifier +'/source');
                    } else {
                        $location.path('/webservice');
                    }
                },

                function() { Growl('error','Error','Service '+ self.metadata.name +' creation failed'); }
            );
        };

        self.close = function () {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function () {
                        return "dialog.message.confirm.cancel.operation";
                    }
                }
            });
            dlg.result.then(function (cfrm) {
                if (cfrm) {
                    $location.path('/webservice');
                }
            });
        };

        self.initCtrl = function() {
            self.versions = self.getVersionsForType();
            Examind.services.getInstances(self.type).then(function(response) {
                self.currentInstances = response.data;
            });
        };

        self.initCtrl();
    })

    .controller('WebServiceChooseSourceController', function($routeParams , Growl, $location, Examind, webserviceFactory) {
        var self = this;
        self.type = $routeParams.type;
        self.id = $routeParams.id;
        self.guiConfig = {
            'url': 'localhost',
            'port': '5432',
            'className': 'org.postgresql.Driver',
            'name': '',
            'user':'',
            'password':'',
            'schema':'',
            'enableDirectory':false,
            'transactional':false,
            'dataDirectory':'',
            'cswMode':null,
            'cswPartial':false,
            'cswOnlyPublished':false,
            'cswEsURL':'',
            'cswDataDirectory':'',
            'cswIndexType':'lucene-node',
            'createData':false,
            'generateSensor': false,
            'directProvider':false,
            'readOnly':false
        };

        self.initSource = function() {
            if (self.type === 'csw') {
                self.source = {
                        'type':'Automatic',
                        'format': null,
                        'indexType' : 'lucene-node',
                        'profile': 'discovery',
                        'customparameters' : {
                               "partial" : "false",
                               "collection" : "false",
                               "onlyPublished" : "false",
                               "es-url" : ''
                        }
                    };
            } else if (self.type === 'sos' || self.type === 'sts') {
                self.source = {
                    'type':'SOSConfiguration',
                    'profile': 'discovery',
                    'parameters' : {
                        "directProvider" : "false"
                    }
                };
            } else if (self.type === 'wfs') {
                Examind.ogcServices.getConfig(self.type, self.id).then(function(response) {
                    self.source = response.data;
                });
            }
        };

        self.saveServiceSource = function() {
            if(self.type === 'csw') {
                self.source.format = self.guiConfig.cswMode;
                self.source.customparameters.partial = self.guiConfig.cswPartial;
                self.source.customparameters.collection = self.guiConfig.cswCollection;
                self.source.customparameters.onlyPublished = self.guiConfig.cswOnlyPublished;
                self.source.customparameters["es-url"] = self.guiConfig.cswEsURL;
                if(self.guiConfig.cswMode === 'filesystem') {
                    self.source.dataDirectory = self.guiConfig.cswDataDirectory;
                    self.source.customparameters.partial = true;
                }
                self.source.indexType = self.guiConfig.cswIndexType;

            }
            if (self.type === 'sos' || self.type === 'sts') {
                self.source.parameters.directProvider = self.guiConfig.directProvider;
            }
            if (self.guiConfig.transactional) {
                if (self.type === 'sos' || self.type === 'csw' || self.type === 'sts') {
                    self.source.profile = 'transactional';
                } else if(self.type === 'wfs') {
                    self.source.customParameters = {
                        "transactional": "true"
                    };
                }
            }
            Examind.ogcServices.setConfig(self.type, self.id, self.source).then(function() {
                Growl('success','Success','Service '+ self.id +' successfully updated');
                if (self.type === 'sos' || self.type === 'sts') {
                    createProviders();
                }
                $location.path('/webservice');
            }, function() {
                Growl('error','Error','Service configuration update error');
            });
        };

        function createProviders() {
            var body;
            if (self.guiConfig.className === 'org.postgresql.Driver') {
                body = {
                        type: "observation-store",
                        subType: "observationSOSDatabase",
                        parameters: {
                            port: self.guiConfig.port,
                            host: self.guiConfig.url,
                            database: self.guiConfig.name,
                            user: self.guiConfig.user,
                            password: self.guiConfig.password,
                            'schema-prefix':self.guiConfig.schema,
                            timescaledb: self.guiConfig.timescaledb,
                            sgbdtype: 'postgres',
                            'database-readonly': self.guiConfig.readOnly,
                            'phenomenon-id-base':"urn:ogc:def:phenomenon:GEOM:",
                            'observation-template-id-base':"urn:ogc:object:observation:template:GEOM:",
                            'observation-id-base':"urn:ogc:object:observation:GEOM:",
                            'sensor-id-base':"urn:ogc:object:sensor:GEOM:"
                        }
                    };
            } else if (self.guiConfig.className === 'org.duckdb.DuckDBDriver') {
                body = {
                        type: "observation-store",
                        subType: "observationSOSDatabase",
                        parameters: {
                            derbyurl: self.guiConfig.url,
                            'schema-prefix':self.guiConfig.schema,
                            sgbdtype: 'duckdb',
                            'database-readonly': self.guiConfig.readOnly,
                            'phenomenon-id-base':"urn:ogc:def:phenomenon:GEOM:",
                            'observation-template-id-base':"urn:ogc:object:observation:template:GEOM:",
                            'observation-id-base':"urn:ogc:object:observation:GEOM:",
                            'sensor-id-base':"urn:ogc:object:sensor:GEOM:"
                        }
                    };
            } else if (self.guiConfig.className === 'org.apache.derby.jdbc.EmbeddedDriver') {
                body = {
                        type: "observation-store",
                        subType: "observationSOSDatabase",
                        parameters: {
                            derbyurl: self.guiConfig.url,
                            'schema-prefix':self.guiConfig.schema,
                            sgbdtype: 'derby',
                            'database-readonly': self.guiConfig.readOnly,
                            'phenomenon-id-base':"urn:ogc:def:phenomenon:GEOM:",
                            'observation-template-id-base':"urn:ogc:object:observation:template:GEOM:",
                            'observation-id-base':"urn:ogc:object:observation:GEOM:",
                            'sensor-id-base':"urn:ogc:object:sensor:GEOM:"
                        }
                    };
            }
            Examind.providers.create(self.id + '-' + self.type +'-om', self.guiConfig.createData, body).then(function() {
                 createSensorProvider(body);
            }, function() {
                Growl('error','Error','Unable to create OM2 provider');
            });
        }
        
        function createSensorProvider(omProviderBody) {
            var sensorProviderId = self.id + '-' + self.type +'-sensor';
            // SML file system mode
            if (self.guiConfig.enableDirectory) {
                var sensorProviderJson = {
                    type: "sensor-store",
                    subType: "filesensor",
                    parameters: {
                        data_directory: self.guiConfig.dataDirectory
                    }
                };
                Examind.providers.create(sensorProviderId, true, sensorProviderJson).then(
                    function() {
                        linkProviders(sensorProviderId, true);
                    }, function() {
                        Growl('error','Error','Unable to create SML provider');
                    }
                );
        
            // direct provider mode 
            } else if (self.guiConfig.directProvider) {
                omProviderBody.type = "sensor-store";
                omProviderBody.subType = "om2sensor";
                Examind.providers.create(sensorProviderId, false, omProviderBody).then(
                    function() {
                        linkProviders(sensorProviderId, true);
                    }, function() {
                        Growl('error','Error','Unable to create SML provider');
                    }
                );
            
            // default internal mode
            } else {
                linkProviders('default-internal-sensor', false);
            }
        }
        
        function linkProviders(sensorProviderId, sensorFullLink) {
            Examind.sensorServices.linkSensorProvider(webserviceFactory.serviceId, sensorProviderId, sensorFullLink).then(
            function() {},
            function() {
                Growl('error','Error','Unable to link SML provider');
            });
            Examind.sensorServices.linkSensorProvider(webserviceFactory.serviceId,  self.id + '-' + self.type + '-om', true).then(
            function() {
                // do not generate sensor in direct provider mode
                if (self.guiConfig.generateSensor && !self.guiConfig.directProvider) {
                    Examind.sensorServices.generateSensorFromOMProvider(webserviceFactory.serviceId).then(
                    function() {},
                    function() {
                        Growl('error','Error','Unable to generate sensors');
                    });
                }
            },
            function() {
                Growl('error','Error','Unable to link O&M provider');
            });
        }

        //init the source
        self.initSource();
    })

    .factory('webserviceFactory', function() {
        return {serviceId:null};
    });
