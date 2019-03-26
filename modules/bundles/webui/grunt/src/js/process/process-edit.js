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

angular.module('cstl-process-edit', ['cstl-restapi', 'cstl-services',
    'ui.bootstrap.modal', 'processParamEditorEngine', 'examind-instance'])

    .config(function(processParamEditorProvider) {
        processParamEditorProvider
            .put('java.lang.Boolean', {
                templateUrl: 'views/tasks/editor/boolean.html'
            })
            .put('java.lang.Double', {
                templateUrl: 'views/tasks/editor/number.html'
            })
            .put('java.lang.Integer', {
                templateUrl: 'views/tasks/editor/number.html'
            })
            .put('java.lang.Long', {
                templateUrl: 'views/tasks/editor/number.html'
            })
            .put('java.lang.Character', {
                templateUrl: 'views/tasks/editor/string.html'
            })
            .put('java.lang.String', {
                templateUrl: 'views/tasks/editor/string.html'
            })
            .put('java.net.URL', {
                templateUrl: 'views/tasks/editor/url.html'
            })
            .put('java.io.File', {
                templateUrl: 'views/tasks/editor/file.html'
            })
            .put('org.constellation.dto.process.StyleProcessReference', {
                templateUrl: 'views/tasks/editor/style.html',
                controller:'ProcessStyleEditorController',
                controllerAs: 'ec',
                resolve : {
                    'styles': ['StyleService', function(StyleService) {
                        return StyleService.getStyles();
                    }]
                }
            })
            .put('org.constellation.dto.StyleReference', {
                templateUrl: 'views/tasks/editor/style.html',
                controller:'ProcessStyleEditorController',
                controllerAs: 'ec',
                resolve : {
                    'styles': ['StyleService', function(StyleService) {
                        return StyleService.getStyles();
                    }]
                }
            })
            .put('org.constellation.dto.process.UserProcessReference', {
                templateUrl: 'views/tasks/editor/user.html',
                controller:'ProcessUserEditorController',
                controllerAs: 'ec',
                resolve : {
                    'users': ['UserService', function(UserService) {
                        return UserService.getUsers();
                    }]
                }
            })
            .put('org.constellation.dto.process.ServiceProcessReference', {
                templateUrl: 'views/tasks/editor/service.html',
                controller:'ProcessServiceEditorController',
                controllerAs: 'ec',
                resolve : {
                    'services': ['OGCWSService', function(OGCWSService) {
                        return OGCWSService.getAllServices();
                    }]
                }
            })
            .put('org.constellation.dto.process.CRSProcessReference', {
                templateUrl: 'views/tasks/editor/crs.html',
                controller:'ProcessCRSEditorController',
                controllerAs: 'ec',
                resolve : {
                    'epsgCodes': ['Examind', function(Examind) {
                        return Examind.crs.listAll();
                    }]
                }
            })
            .put('org.opengis.referencing.crs.CoordinateReferenceSystem', {
                templateUrl: 'views/tasks/editor/crs.html',
                controller:'ProcessCRSEditorController',
                controllerAs: 'ec',
                resolve : {
                    'epsgCodes': ['Examind', function(Examind) {
                        return Examind.crs.listAll();
                    }]
                }
            })
            .put('org.constellation.dto.process.DataProcessReference', {
                templateUrl: 'views/tasks/editor/data.html',
                controller:'ProcessDataEditorController',
                controllerAs: 'ec',
                resolve : {
                    'datas': ['DataService', function(DataService) {
                        return DataService.getAllDatas();
                    }]
                }
            })
            .put('org.geotoolkit.map.MapContext', {
                templateUrl: 'views/tasks/editor/mapcontext.html',
                controller:'ProcessMapContextEditorController',
                controllerAs: 'ec',
                resolve : {
                    'mapcontexts': ['Examind', function(Examind) {
                        return Examind.mapcontexts.getMapLayers();
                    }]
                }
            })
            .put('org.constellation.dto.MapContextLayersDTO', {
                templateUrl: 'views/tasks/editor/mapcontext.html',
                controller:'ProcessMapContextEditorController',
                controllerAs: 'ec',
                resolve : {
                    'mapcontexts': ['Examind', function(Examind) {
                        return Examind.mapcontexts.getMapLayers();
                    }]
                }
            })
            .put('org.constellation.dto.process.DatasetProcessReference', {
                templateUrl: 'views/tasks/editor/dataset.html',
                controller:'ProcessDatasetEditorController',
                controllerAs: 'ec',
                resolve : {
                    'datasets': ['DataService', function(DataService) {
                        return DataService.getAllDatasets();
                    }]
                }
            });
    })

    // -------------------------------------------------------------------------
    //  Style service : get available styles with a cache support
    // -------------------------------------------------------------------------
    .service('StyleService', function(Examind) {

        var self = this;
        self.styles = null;

        function init() {
            self.styles = Examind.tasks.getStyles();
        }

        self.getStyles = function() {
            if (self.styles === null) {
                init();
            }
            return self.styles;
        };

        self.refresh = function() {
            init();
        };

    })

    // -------------------------------------------------------------------------
    //  User service : get available users with a cache support
    // -------------------------------------------------------------------------
    .service('UserService', function(Examind) {

        var self = this;
        self.users = null;

        function init() {
            self.users = Examind.tasks.getUsers();
        }

        self.getUsers = function() {
            if (self.users === null) {
                init();
            }
            return self.users;
        };

        self.refresh = function() {
            init();
        };

    })

    // -------------------------------------------------------------------------
    //  OGC WS service
    // -------------------------------------------------------------------------
    .service('OGCWSService', function(Examind) {

        var self = this;
        self.services = null;

        function init() {
            self.services = Examind.tasks.getServices();
        }

        self.getAllServices = function() {
            if (self.services === null) {
                init();
            }
            return self.services;
        };

        self.refresh = function() {
            init();
        };
    })

    // -------------------------------------------------------------------------
    //  Data service
    // -------------------------------------------------------------------------
    .service('DataService', function(Examind) {

        var self = this;
        self.dataset = null;
        self.datas = null;

        function initDatasets() {
            self.dataset = Examind.tasks.getDatasets();
        }
        function initDatas() {
            self.datas = Examind.tasks.getDatas();
        }

        self.getAllDatasets = function() {
            if (self.dataset === null) {
                initDatasets();
            }
            return self.dataset;
        };

        self.getAllDatas = function() {
            if (self.datas === null) {
                initDatas();
            }
            return self.datas;
        };

        self.refresh = function() {
            initDatasets();
            initDatas();
        };
    })

    .service('SelectionWPS', function() {
        var self = this;
        self.wps = null;
    })

    .controller('ProcessStyleEditorController', function(parameter, valueIndex, styles, $filter) {

        var self = this;

        //full list
        self.styles = styles.data;

        //apply filter
        if (parameter.ext && parameter.ext.filter) {
            self.styles = $filter('filter')(self.styles, parameter.ext.filter);
        }

        // add undefined if parameter optional
        self.styles = (parameter.mandatory ? [] : [undefined]).concat(self.styles);

        //initialize parameter saved value
        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.styles[0] : undefined;
        }
    })

    .controller('ProcessUserEditorController', function(parameter, valueIndex, users, $filter) {

        var self = this;

        //full list
        self.users = users.data;

        //apply filter
        if (parameter.ext && parameter.ext.filter) {
            self.users = $filter('filter')(self.users, parameter.ext.filter);
        }

        // add undefined if parameter optional
        self.users = (parameter.mandatory ? [] : [undefined]).concat(self.users);

        //initialize parameter saved value
        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.users[0] : undefined;
        }
    })

    .controller('ProcessServiceEditorController', function(parameter, valueIndex, services, $filter) {

        var self = this;
        //full list
        self.services = services.data.filter(function(s){
            return s.name;
        });

        //apply filter
        if (parameter.ext && parameter.ext.filter) {
            self.services = $filter('filter')(self.services, parameter.ext.filter);
        }

        // add undefined if parameter optional
        self.services = (parameter.mandatory ? [] : [undefined]).concat(self.services);

        //initialize parameter saved value
        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.services[0] : undefined;
        }
    })

    .controller('ProcessCRSEditorController', function(parameter, valueIndex, epsgCodes) {

        var self = this;

        self.epsgCodes = (parameter.mandatory ? [] : [undefined]).concat(epsgCodes.data);

        if (parameter.save[valueIndex] === undefined) {
            var value = parameter.default ? parameter.default : "EPSG:3857";
            parameter.save[valueIndex] = parameter.mandatory ? value : undefined;
        }
    })

    .controller('ProcessMapContextEditorController',function(parameter, valueIndex, mapcontexts){
        var self = this;
        self.mapcontexts = (parameter.mandatory ? [] : [undefined]).concat(mapcontexts.data);

        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.mapcontexts[0] : undefined;
        }
    })

    .controller('ProcessDatasetEditorController', function(parameter, valueIndex, datasets) {

        var self = this;
        self.datasets = (parameter.mandatory ? [] : [undefined]).concat(datasets.data);

        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.datasets[0] : undefined;
        }
    })

    .controller('ProcessDataEditorController', function(parameter, valueIndex, datas, $filter) {

        var self = this;

        //full list
        self.datas = datas.data;

        //apply filter
        if (parameter.ext && parameter.ext.filter) {
            self.datas = $filter('filter')(self.datas, parameter.ext.filter);
        }

        // add undefined if parameter optional
        self.datas = (parameter.mandatory ? [] : [undefined]).concat(self.datas);

        //initialize parameter saved value
        if (parameter.save[valueIndex] === undefined) {
            parameter.save[valueIndex] = parameter.mandatory ? self.datas[0] : undefined;
        }
    })

    .controller('ModalAddTaskController', function($scope, $modalInstance, Growl, processes, task, displayStep,
                                                   StyleService, OGCWSService, DataService, processParamEditor,
                                                   SelectionWPS, Examind){
        //init services
        StyleService.refresh();
        OGCWSService.refresh();
        DataService.refresh();

        // Scope variables
        $scope.canManage = false;
        $scope.option = {
            selectedProcess : undefined, //for wps process
            authIndex : 0,  //for internal process
            processIndex : 0  //for internal process
        };
        $scope.describeProcess = undefined;
        $scope.parameters = [];
        $scope.task = task.data ? task.data : task;
        $scope.styles = [];

        // handle display step for this modal popup
        $scope.step = {
            display: displayStep
        };

        //reset selection
        SelectionWPS.wps = null;
        $scope.selectionWPS = SelectionWPS;

        function parseProcessDefaultName(processName) {
            var authEnd = processName.indexOf(':');
            return [processName.substring(0, authEnd), processName.substring(authEnd+1)];
        }

        function createProcesses(processesList) {
            var tree = {};
            for (var p in processesList) {
                if(processesList.hasOwnProperty(p)){
                    var process = parseProcessDefaultName(processesList[p]);
                    tree[process[0]] = tree[process[0]] || [];
                    var codeInd = tree[process[0]].push(process[1])-1;
                }
            }
            var procTree = [];
            for (var auth in tree) {
                if(tree.hasOwnProperty(auth)){
                    var indAuth = procTree.push({
                            'auth' : auth,
                            'processes' : tree[auth]
                        })-1;
                }
            }
            return procTree;
        }
        $scope.processes = createProcesses(processes.data.list);

        // scope functions
        $scope.close = $scope.cancel = $modalInstance.close;

        $scope.onSelectProcess = function() {
            Examind.tasks.getProcessDescriptor({values:{'authority': $scope.selectionWPS.wps.serviceUrl, 'code': $scope.option.selectedProcess}})
                .then(function (response) { // On success
                    $scope.describeProcess = response.data;
                    $scope.computeParameters(response.data);
                }).catch(function (data) { // On error
                Growl('error', 'Error', 'Unable to get the process description');
            });
        };

        $scope.addGroupOccurrence = function (groupParam) {
            if (groupParam.type === 'group') {
                var firstOccur = groupParam.inputs[0];

                var newOccur = [];
                var nbParam = firstOccur.length;
                for (var i = 0; i < nbParam; i++) {
                    newOccur.push(copyParam(firstOccur[i]));
                }
                groupParam.inputs.push(newOccur);
            }
        };

        $scope.removeGroupOccurrence = function (groupParam, index) {
            if (groupParam.type === 'group' && groupParam.inputs.length > index) {
                groupParam.inputs.splice(index, 1);
            }
        };

        $scope.save = function(form) {
            if(form.$invalid) {
                Growl('error', 'Error', 'Form is invalid, make sure you have entered all fields.');
                return false;
            }
            if (!$scope.task.processAuthority && !$scope.task.processCode) {
                //we are in add mode
                if($scope.selectionWPS.wps) {
                    // add mode with wps process
                    $scope.task.processAuthority = $scope.selectionWPS.wps.serviceUrl;
                    $scope.task.processCode = $scope.option.selectedProcess;
                } else {
                    //add mode with internal process
                    $scope.task.processAuthority = $scope.processes[$scope.option.authIndex].auth;
                    $scope.task.processCode = $scope.processes[$scope.option.authIndex].processes[$scope.option.processIndex];
                }
            }
            $scope.task.inputs = {};
            fillInputsValues($scope.task.inputs, $scope.parameters);
            //convert to JSON
            $scope.task.inputs = angular.toJson($scope.task.inputs);
            if ($scope.task.id != null){
                Examind.tasks.updateParamsTask($scope.task)
                    .then(function(response) {
                        Growl('success', 'Success', 'the task was updated successfully.');
                        $modalInstance.close();
                    }).catch(function(response){
                        var message = 'Error to save the task';
                        if (response.data && response.data.message) {
                            message = response.data.message;
                        }
                        Growl('error', 'Error', message);
                    });
            } else {
                Examind.tasks.createParamsTask($scope.task)
                    .then(function(response) {
                        Growl('success', 'Success', 'new task created with success.');
                        $modalInstance.close();
                    }).catch(function(response){
                        var message = 'Error to save the new task';
                        if (response.data && response.data.message) {
                            message = response.data.message;
                        }
                        Growl('error', 'Error', message);
                    });
            }
        };

        $scope.isValid = function(elementName) {
            return !jQuery("#"+$scope.replaceStr(elementName)).hasClass("ng-invalid");
        };

        $scope.replaceStr = function(str) {
            var tmp = str.replace(/:/g, '_');
            return tmp.replace(/\./g, '_');
        };

        $scope.simplifyStr = function(str) {
            if(str.lastIndexOf(':') !== -1) {
                return str.substring(str.lastIndexOf(':')+1);
            }
            return str;
        };

        $scope.nextStep = function() {
            $scope.step.display = 'configParameters';
        };

        $scope.processAuthorityChanged = function() {
            $scope.option.processIndex = 0;
            $scope.getDescribeProcess();
        };

        $scope.processCodeChanged = function() {
            $scope.getDescribeProcess();
        };

        $scope.getDescribeProcess = function() {
            var auth = null;
            var code = null;
            if ($scope.task.processAuthority && $scope.task.processCode) {
                //edit mode
                auth = $scope.task.processAuthority;
                code = $scope.task.processCode;
            } else {
                //add mode
                var authority = $scope.processes[$scope.option.authIndex];
                auth = authority.auth;
                code = authority.processes[$scope.option.processIndex];
            }
            Examind.tasks.getProcessDescriptor({values:{'authority': auth, 'code': code}})
                .then(function (response) { // On success
                    $scope.describeProcess = response.data;
                    $scope.computeParameters(response.data);
                }).catch(function (data) { // On error
                Growl('error', 'Error', 'Unable to get the process description');
            });
        };
        if ($scope.task.id != null){
            $scope.getDescribeProcess();
        }

        $scope.computeParameters = function(descProc) {
            $scope.canManage = true;
            var inputs = [];
            var isArray = function (string) {
                return string.indexOf('[]', string.length - 2) !== -1;
            };
            var parseParameterDescriptor = function (elem, idPrefix) {
                var parameter = {};
                parameter.name = elem.name;
                parameter.id = idPrefix != null ? idPrefix + '_' + elem.name : elem.name;
                parameter.minOccurs = elem.minOccurs !== undefined ? elem.minOccurs : 1;
                parameter.maxOccurs = elem.maxOccurs !== undefined ? elem.maxOccurs : 1;
                parameter.mandatory = parameter.minOccurs > 0;
                parameter.description = elem.description;
                //Simple parameter
                var javaClass = elem.class;
                var simple = javaClass !== undefined;
                if (simple) {
                    parameter.type = "simple";
                    parameter.isArray = false;
                    parameter.binding = javaClass;
                    if (isArray(javaClass)) {
                        parameter.isArray = true;
                        parameter.binding = javaClass.substring(0, javaClass.length - 2);
                    }
                    parameter.default = convertValue(elem.defaultValue, parameter.binding);
                    parameter.unit = simple.unit;
                    //default values
                    parameter.save = [];
                    for (var j = 0; j < parameter.minOccurs; j++) {
                        parameter.save.push(parameter.default);
                    }
                    //check if parameter is handled
                    if (parameter.mandatory && !processParamEditor.hasEditor(parameter.binding)) {
                        $scope.canManage = false;
                    }
                    if (elem.restriction) {
                        var restriction = elem.restriction;
                        //inputElement.base = restriction.base;
                        //extract valid value range
                        parameter.restriction = {};
                        var minValue = restriction.minValue;
                        var maxValue = restriction.maxValue;
                        if (minValue !== null && maxValue !== null) {
                            parameter.restriction.range = [minValue, maxValue];
                        }
                        //extract valid values
                        parameter.restriction.enumeration = extractEnumeration(restriction.validValues, parameter.binding);
                    } else {
                        parameter.restriction = {};
                        parameter.restriction.enumeration = [];
                    }
                    if (elem.ext) {
                        parameter.ext = elem.ext;
                    }
                } else {
                    //Group parameters
                    parameter.type = "group";
                    parameter.inputs = [[]];
                    parseElements(elem.descriptors, parameter.inputs[0], parameter.id);
                }
                return parameter;
            };

            var parseElements = function (elements, inputsDesc, idPrefix) {
                if (elements) {
                    if (Array.isArray(elements)) {
                        var i = 0;
                        elements.forEach(function (elem) {
                            var pref = idPrefix != null ? idPrefix + '_' + i : null;
                            inputsDesc.push(parseParameterDescriptor(elem, pref));
                            i++;
                        });
                    } else {
                        inputsDesc.push(parseParameterDescriptor(elements, idPrefix));
                    }
                }
            };
            parseElements(descProc.descriptors, inputs, null);
            $scope.parameters = inputs;
            restoreInputs();
        };

        // Private function
        function restoreInputs(){
            if ($scope.task.inputs) {
                //convert to object
                if (angular.isString($scope.task.inputs)) {
                    $scope.task.inputs = angular.fromJson($scope.task.inputs);
                }
                fillParametersValues($scope.task.inputs, $scope.parameters);
            }
        }

        /**
         * Fill form parameters save attributes from task inputs
         * @param inputs
         * @param parameters
         */
        function fillParametersValues(inputs, parameters) {
            for (var param in inputs) {
                if(inputs.hasOwnProperty(param)) {
                    var value = inputs[param];
                    var scopeParam = getParameterByName(parameters, param);
                    if (scopeParam.type === 'simple') {
                        scopeParam.save = value;
                    } else {
                        if (angular.isArray(value)) {
                            var nbOccurs = value.length;
                            //create occurs
                            for (var i = 0; i < nbOccurs-1; i++) {
                                $scope.addGroupOccurrence(scopeParam);
                            }
                            //fill occurences parameters
                            for (i = 0; i < nbOccurs; i++) {
                                fillParametersValues(value[i], scopeParam.inputs[i]);
                            }
                        }
                    }
                }
            }
        }

        function getParameterByName(inputArray, param) {
            var filter = inputArray.filter(function (elem) {
                return elem.name === param;
            });
            return filter[0];
        }

        /**
         * Rebuild task input from form parameter save attributes
         * @param inputs
         * @param parameters
         */
        function fillInputsValues(inputs, parameters) {
            var valid = true;
            var nbParam = parameters.length;
            for (var i = 0; i < nbParam; i++) {
                var param = parameters[i];
                if (param.type === 'simple') {
                    valid = isValid(param);
                    inputs[param.name] = param.save;
                } else {
                    inputs[param.name] = [];
                    var nbOccurs = param.inputs.length;

                    for (var j = 0; j < nbOccurs; j++) {
                        var supInputs = {};
                        fillInputsValues(supInputs, param.inputs[j]);
                        inputs[param.name].push(supInputs);
                    }
                }
            }
            return valid;
        }

        /**
         * Check if a simple parameter value is valid
         * @param parameter
         */
        function isValid(parameter) {
            if (parameter.type === 'simple') {
                //test emptyness
                if (parameter.mandatory && (parameter.save === null || parameter.save.length === 0)) {
                    Growl('error', 'Error', 'Parameter '+parameter.name+' is mandatory');
                    return false;
                }
                var length = parameter.save.length;
                for (var i = 0; i < length; i++) {
                    //test cast
                    switch (parameter.binding) {
                        case "java.lang.Integer" : //fall trough
                        case "java.lang.Long" : //fall trough
                        case "java.lang.Double" :
                            if (!angular.isNumber(parameter.save[i])) {
                                Growl('error', 'Error', 'Parameter ' + parameter.name + ' is not a Number');
                                return false;
                            }
                            break;
                    }
                    //test restrictions
                    if (parameter.restriction) {
                        var enumeration = parameter.restriction.enumeration;
                        if (enumeration && enumeration.length > 0) {
                            //only test primitive enumeration
                            if (!angular.isObject(enumeration[0])) {
                                if (enumeration.indexOf(parameter.save[i]) === -1) {
                                    Growl('error', 'Error', 'Value of parameter ' + parameter.name + ' not valid.');
                                    return false;
                                }
                            }
                        }
                        var range = parameter.restriction.range;
                        if (range) {
                            if (parameter.save[i] < range[0] || parameter.save[i] > range[1]) {
                                Growl('error', 'Error', 'Value of parameter ' + parameter.name + ' not valid. ' +
                                    'Should be within range [' + range[0] + ',' + range[1] + ']');
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        }

        /**
         * Recursively copy a parameter without save value
         * @param param
         */
        function copyParam(param) {
            var copy = {};
            copy.name = param.name;
            copy.id = param.id;
            copy.minOccurs = param.minOccurs;
            copy.maxOccurs = param.maxOccurs;
            copy.mandatory = param.mandatory;
            copy.description = param.description;
            copy.type = param.type;
            copy.isArray = param.isArray;
            if (param.type === 'simple') {
                copy.default = param.default;
                copy.binding = param.binding;
                copy.unit = param.unit;
                copy.restriction = param.restriction;
                copy.ext = param.ext;
                copy.save = [];
                for (var i = 0; i < copy.minOccurs; i++) {
                    copy.save.push(copy.default);
                }
            } else {
                copy.inputs = [];
                var paramInputs = param.inputs;
                //only duplicate minOccurs number of group occurrences.
                for (var j = 0; j < copy.minOccurs; j++) {
                    var grpInputs = [];
                    var params = paramInputs[j];
                    var nbParams = params.length;
                    for (var k = 0; k <nbParams; k++) {
                        grpInputs.push(copyParam(params[k]));
                    }
                    copy.inputs.push(grpInputs);
                }
            }
            return copy;
        }

        function extractEnumeration(enumList, binding) {
            var enumerationList = [];
            if (enumList && Array.isArray(enumList)) {
                enumList.forEach(function (val) {
                    enumerationList.push(convertValue(val, binding));
                });
            }
            return enumerationList;
        }

        function convertValue(value, binding) {
            if ("java.lang.Integer" === binding || "java.lang.Long" === binding) {
                return parseInt(value);
            }
            if ("java.lang.Double" === binding) {
                return parseFloat(value);
            }
            if ("java.lang.Boolean" === binding) {
                return Boolean(value);
            }
            return value;
        }

    })
    .controller('WPSSourceTaskController', function($cookieStore,$translate,Growl,SelectionWPS, Examind) {
        var self = this;
        self.chosenTab = 'cstlWPS';
        self.servicesArray = [];

        self.external = {
            serviceUrl: null,
            processList: [],
            filtertext:''
        };
        self.cstlUrl = $cookieStore.get('cstlUrl');

        self.switchTab = function(tab) {
            if(tab !== self.chosenTab) {
                self.chosenTab = tab;
                //reset selection when switching tabs
                SelectionWPS.wps = null;
                for(var i =0;i<self.servicesArray.length;i++) {
                    self.servicesArray[i].selected = false;
                }
            }
        };

        self.updateSelectedWPS = function() {
            SelectionWPS.wps = null;
            for(var i =0;i<self.servicesArray.length;i++) {
                if(self.servicesArray[i].selected) {
                    SelectionWPS.wps = self.servicesArray[i];
                    break;
                }
            }
        };

        self.searchAndDisplayWpsProcesses = function() {
            if (self.external.serviceUrl) {
                Examind.wps.getExternalProcessList(self.external.serviceUrl).then(
                    function(response) {//on success
                        self.external.processList = response.data.sort(function(a, b){return a.id.localeCompare(b.id);});
                        SelectionWPS.wps = self.external;
                    }, function(response) {//on error
                        Growl('error', 'Error', 'Unable to get capabilities, cause: '+response.data.message);
                    }
                );
            }
        };

        self.getCurrentLang = function() {
            return $translate.use();
        };

        self.initWpsSourceTask = function() {
            Examind.services.getInstances('wps',self.getCurrentLang()).then(function(response) {
                self.servicesArray = response.data;
            });
            SelectionWPS.wps = null;
        };

        self.loadProcessList = function(wps) {
            if(!angular.isDefined(wps)) {
                return;
            }
            if(angular.isArray(wps.processList)) {
                return;
            }
            wps.serviceUrl = self.cstlUrl+"WS/wps/"+wps.identifier;
            Examind.wps.getExternalProcessList(wps.serviceUrl).then(
                function(response) {//on success
                    wps.processList = response.data.sort(function(a, b){return a.id.localeCompare(b.id);});
                }, function(response) {//on error
                    Growl('error', 'Error', 'Unable to get capabilities, cause: '+response.data.message);
                }
            );
        };

        self.initWpsSourceTask();

    });
