/*
 * Constellation - An open source and standard compliant SDI
 *
 *     http://www.constellation-sdi.org
 *
 *     Copyright 2014 Geomatys
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @require angular.js
 * @require angular-cookies.js
 * @require app-restapi.js
 */
angular.module('cstl-services', ['webui-config','webui-utils','cstl-restapi','examind-instance'])

    // -------------------------------------------------------------------------
    //  Enum raster sld type
    // -------------------------------------------------------------------------
    .constant('rasterstyletype', {
        'none':'none',
        'palette':'palette',
        'cells':'cell',
        'dynamic':'dynamic'
    })

    // -------------------------------------------------------------------------
    //  Constant to resolve permission
    // -------------------------------------------------------------------------
    .constant('PermissionResolver', {
        'factory' : function(permName) {
            return {
                'continue' : function($q, Permission,$location,Growl){
                    var defer = $q.defer();
                    Permission.promise.then(function(){
                        if (Permission.hasPermission(permName)) {
                            defer.resolve();
                        } else {
                            defer.reject();
                            //redirect to root path
                            $location.path('/');
                            Growl('error', 'Error', 'Access denied!');
                        }
                    }).catch(defer.reject);
                    return defer.promise;
                }
            };
        }
    })

    // -------------------------------------------------------------------------
    //  Constellation Utilities
    // -------------------------------------------------------------------------

    .factory('CstlUtils', function(CstlConfig) {
        return {
            /**
             * Return the webapp context path.
             *
             * @return {String} the webapp context path
             */
            getContextPath: function() {
                var path = window.location.pathname;
                if (path === '/') {
                    return path;
                }
                return path.substring(0, path.indexOf('/', 1));
            },

            /**
             * Injects contextual values into the specified url template.
             *
             * Available value expression for contextual values injection could
             * be defined using the 'CstlConfig' service using following properties :
             *  - inject.expr.ctrl.url
             *
             * @param url {String} the url template to compile
             * @returns {String} the complied url
             */
            compileUrl: function(config, url) {
                var cstlUrl = window.localStorage.getItem('cstlUrl');

                // Inject cstl-service webapp url.
                if (angular.isDefined(cstlUrl)) {
                    url = url.replace(CstlConfig['inject.expr.ctrl.url'], cstlUrl);
                }else if (/@cstl/.test(url)){
                  window.location.href="index.html";
                }

                return url;
            }
        };
    })

    // -------------------------------------------------------------------------
    //  Authentication HTTP Interceptor
    // -------------------------------------------------------------------------

    .factory('AuthInterceptor', function($rootScope, $q, CstlConfig, CstlUtils) {
        /**
         * Checks if the request url destination is the Constellation REST API.
         *
         * @return {Boolean}
         */
        function isCstlRequest(url) {
            return url.indexOf(CstlConfig['inject.expr.ctrl.url']) === 0;
        }

        return {
            'request': function(config) {
                // Intercept request to Constellation REST API.
                if (isCstlRequest(config.url)) {
                    $rootScope.$broadcast('event:auth-cstl-request');

                    // Inject contextual values into request url.
                    config.url = CstlUtils.compileUrl(config, config.url);
                }
                return config;
            },
            'responseError': function(response) {
                if (response.status === 401) {
                    // Broadcast 'event:auth-loginRequired' event.
                    $rootScope.$broadcast('event:auth-loginRequired');
                }
                return $q.reject(response);
            }
        };
    })

    // -------------------------------------------------------------------------
    //  Stomp WebSocket Service
    // -------------------------------------------------------------------------

    .factory('StompService', function($timeout,CstlUtils) {

        function Topic(path) {
            var self = this;
            self.path = path;
            self.unsubscribe = function() {
                if(self.tunnel) {
                    self.tunnel.unsubscribe();
                    console.log('Unsubscribed from '+path,self.tunnel);
                }
            };
        }


        function Stomper(url){

            var self = this;
            self.open = false;
            self.stompClient = null;

            self.connect = function (onOpen,onClose) {
                if (self.stompClient === null) {
                    var socket = new SockJS(url);
                    self.stompClient = Stomp.over(socket);
                    self.stompClient.debug = angular.noop; //disable messages logging in console
                }
                self.stompClient.connect({},
                    function(frame) {
                        //onOpen function
                        self.open = true;
                        console.log('Stomp Client Connected to ' + url);
                        if (onOpen && typeof(onOpen) === "function") {
                            onOpen(frame);
                        }
                    },function(reason) {
                        console.log(reason);
                        //onClose function
                        self.open = false;
                        if (onClose && typeof(onClose) === "function") {
                            onClose(reason);
                        }
                        self.reconnect(onOpen,onClose);
                    });
            };

            self.reconnect = function(onOpen,onClose) {
                console.log('Stomp Client Trying to reconnect to websockets...');
                var socket = new SockJS(url);
                self.stompClient = Stomp.over(socket);
                self.stompClient.debug = angular.noop; //disable messages logging in console
                self.stompClient.connect({},function(frame){
                    self.open = true;
                    console.log('Stomp Client ReConnected to ' + url);
                    if (onOpen && typeof(onOpen) === "function") {
                        onOpen(frame);
                    }
                },function(reason) {
                    $timeout(function(){
                        self.reconnect(onOpen,onClose);
                    },3000);
                });
            };

            self.disconnect = function () {
                if (self.stompClient !== null) {
                    self.stompClient.disconnect(function () {
                        console.log('Stomp Client Disconnected from ' + url);
                    });
                    self.stompClient = null;
                }
            };

            self.subscribe = function(path, callback){
                var topic = new Topic(path);
                if (self.stompClient && self.stompClient.connected) {
                    topic.tunnel = self.stompClient.subscribe(path, callback);
                    console.log('Subscribed to '+topic.path,topic.tunnel);
                } else {
                    self.connect(function() {
                        topic.tunnel = self.stompClient.subscribe(path, callback);
                        console.log('Subscribed to '+topic.path,topic.tunnel);
                    });
                }
                return topic;
            };

            self.isConnected = function() {
                return self.stompClient && self.stompClient.connected &&
                    self.stompClient.subscriptions && self.stompClient.subscriptions['sub-0'];
            };

        }

        return new Stomper(CstlUtils.compileUrl(null, '@cstl/API/ws/adminmessages'));
    })

    // -------------------------------------------------------------------------
    //  Dashboard Helper
    // -------------------------------------------------------------------------

    .factory('Dashboard', function($filter) {
        return function(scope, fullList, filterOnType) {
            scope.wrap = scope.wrap || {};
            scope.service = scope.service || null;
            scope.wrap.fullList = fullList || [];
            scope.wrap.dataList = scope.wrap.dataList || [];
            scope.wrap.matchExactly = scope.wrap.matchExactly || false;
            scope.wrap.filtertext = scope.wrap.filtertext || "";
            scope.wrap.filtertype = scope.wrap.filtertype || undefined;
            scope.wrap.ordertype = scope.wrap.ordertype || ((scope.service && scope.service.type && (scope.service.type.toLowerCase()==='sos' || scope.service.type.toLowerCase()==='sts')) ? "id" : (scope.service && scope.service.type && scope.service.type.toLowerCase==='csw') ? "title" : "name");
            scope.wrap.orderreverse = scope.wrap.orderreverse || false;
            scope.wrap.countdata = scope.wrap.countdata || 0;
            scope.wrap.nbbypage = scope.wrap.nbbypage || 10;
            scope.wrap.currentpage = scope.wrap.currentpage || 1;
            scope.selected = scope.selected || null;
            scope.selectedDS = scope.selectedDS || null;
            scope.exclude = scope.exclude || [];

            // Dashboard methods
            scope.displayPage = function(page) {
                var array;
                if (filterOnType) {
                    var match = false;
                    if(scope.wrap.filtertext){
                        match=scope.wrap.matchExactly;
                    }
                    array = $filter('filter')(scope.wrap.fullList, {'type':scope.wrap.filtertype, '$': scope.wrap.filtertext},match);
                } else {
                    array = $filter('filter')(scope.wrap.fullList, {'$': scope.wrap.filtertext});
                }
                array = $filter('orderBy')(array, scope.wrap.ordertype, scope.wrap.orderreverse);

                var list = [];
                for (var i = 0; i < array.length; i++) {
                    var found = false;
                    for (var j = 0; j < scope.exclude.length; j++) {
                        if (scope.service && (scope.service.type.toLowerCase() === 'sos' || scope.service.type.toLowerCase() === 'sts')) {
                            if (scope.exclude[j].id === array[i].name) {
                                found = true;
                                break;
                            }
                        } else {
                            if (scope.exclude[j].name === array[i].name) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        list.push(array[i]);
                    }
                }

                var start = (page - 1) * scope.wrap.nbbypage;

                scope.wrap.currentpage = page;
                scope.wrap.countdata = list.length;
                scope.wrap.dataList = list.splice(start, scope.wrap.nbbypage);
            };

            scope.select = scope.select || function(item) {
                if (scope.selected === item) {
                    scope.selected = null;
                } else {
                    scope.selected = item;
                }
            };

            scope.selectDS = function(item) {
                if (item && scope.selectedDS && scope.selectedDS.id === item.id) {
                    scope.selectedDS = null;
                } else {
                    scope.selectedDS = item;
                }
            };

            scope.$watch('wrap.nbbypage+wrap.filtertext+wrap.filtertype+wrap.fullList', function() {
                scope.displayPage(1);
            },true);

            scope.$watch('wrap.ordertype+wrap.orderreverse', function() {
                scope.displayPage(scope.wrap.currentpage);
            },true);
        };
    })

    // -------------------------------------------------------------------------
    //  Style Service
    // -------------------------------------------------------------------------

    .factory('StyleSharedService', function($modal, Growl, Examind) {
        return {
            showStyleList : function($scope,selected) {
                var modal = $modal.open({
                    templateUrl: 'views/style/modalStyleChoose.html',
                    controller: 'StyleModalController',
                    resolve: {
                        exclude: function() { return selected.targetStyle; },
                        selectedLayer: function() { return selected; },
                        selectedStyle: function() { return null; },
                        serviceName: function() {
                            if ($scope.service) {
                                // In WMS mode
                                return $scope.service.name;
                            }
                            // For portraying
                            return null;
                        },
                        newStyle: function() { return null; },
                        stylechooser: function(){return null;}
                    }
                });

                modal.result.then(function(item) {
                    if (item) {
                        if ($scope.service) {
                            Examind.map.updateLayerStyle($scope.service.type, $scope.service.identifier,
                                {layerId: selected.id, styleId: item.id}).then(
                                function() {
                                    selected.targetStyle.push(item);
                                    $scope.showLayerDashboardMap();
                                    Growl('success','Success','Style updated for layer '+ selected.name);
                                }, function() { Growl('error','Error','Unable to update style for layer '+ selected.name); }
                            );
                        } else {
                            Examind.styles.link(item.id,selected.id).then(
                                function () {
                                    selected.targetStyle.push(item);
                                    $scope.showDataDashboardMap();
                                }
                            );
                        }
                    }
                });
            },

            unlinkStyle : function($scope,styleProvider,styleId,styleName,layerProvider,layerId,layerName,selected) {
                if ($scope.service) {
                    Examind.map.removeLayerStyle($scope.service.type, $scope.service.identifier,
                        {layerId: layerId, styleId: styleId}).then(
                        function() {
                            for (var i=0; i<selected.targetStyle.length; i++) {
                                var s = selected.targetStyle[i];
                                if (s.name === styleName) {
                                    selected.targetStyle.splice(i, 1);
                                    break;
                                }
                            }
                            $scope.showLayerDashboardMap();
                        }, function() { Growl('error','Error','Unable to update style for layer '+ layerName); }
                    );
                } else {

                    //@FIXME this case is never used since datadahboard have it once function to unlink style for data
                    //show in data-dashboard.js function dissociateStyle which use Examind.datas.deleteStyleAssociation
                    // issue CSTL-1913 in comments section

                    Examind.styles.unlink(styleId,layerId).then(
                        function () {
                            var index = -1;
                            for (var i = 0; i < selected.targetStyle.length; i++) {
                                var item = selected.targetStyle[i];
                                var itemProvider = item.provider;
                                var itemName = item.name;
                                if (itemProvider === styleProvider && itemName === styleName) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index >= 0) {
                                selected.targetStyle.splice(index, 1);
                            }
                            $scope.showDataDashboardMap();
                        }
                    );
                }
            },

            showStyleCreate : function(scope) {
                var modal = $modal.open({
                    templateUrl: 'views/style/modalStyleCreate.html',
                    controller: 'StyleModalController',
                    resolve: {
                        newStyle: function() { return null; },
                        pageSld: function() {  return 'views/style/chooseType.html'; },
                        selectedLayer: function() {  return null; },
                        selectedStyle: function() { return null; },
                        serviceName: function() {  return null; },
                        exclude: function() {  return null; },
                        stylechooser: function(){return null;}
                    }
                });
                modal.result.then(function(item) {
                    if (scope) {
                        scope.search();
                    }
                });
            },

            showStyleImport : function(scope) {
                var modal = $modal.open({
                    templateUrl: 'views/style/modalStyleImport.html',
                    controller: 'StyleImportModalController'
                });
                modal.result.then(function(item) {
                    if (scope) {
                        scope.search();
                    }
                });
            },

            showStyleEdit : function(scope, response) {
                var modal = $modal.open({
                    templateUrl: 'views/style/modalStyleEdit.html',
                    controller: 'StyleModalController',
                    resolve: {
                        newStyle: function() { return response;},
                        selectedLayer: function() {  return null; },
                        selectedStyle: function() { return scope.selected; },
                        serviceName: function() {  return null; },
                        exclude: function() {  return null; },
                        stylechooser: function(){return null;}
                    }
                });
                modal.result.then(function(item) {
                    if (scope) {
                        scope.search();
                        scope.previewStyledData(null,false);
                    }
                });
            },

            editLinkedStyle : function(scope, response, selectedData) {
                var modal = $modal.open({
                    templateUrl: 'views/style/modalStyleEdit.html',
                    controller: 'StyleModalController',
                    resolve: {
                        newStyle: function() { return response;},
                        selectedLayer: function() {  return selectedData; },
                        selectedStyle: function() { return null; },
                        serviceName: function() {  return null; },
                        exclude: function() {  return null; },
                        stylechooser: function(){return 'edit';}
                    }
                });
                modal.result.then(function(item) {
                    if(typeof scope.showDataDashboardMap === 'function'){
                        scope.showDataDashboardMap();
                    }
                    if(typeof scope.previewStyledData === 'function'){
                        scope.previewStyledData(null,false);
                    }
                });
            }
        };
    })

    .factory('interval', function() {
        /**
         * An helper service to call an action on demand with a fixed time
         * interval between two calls.
         *
         * @param fn {function} the action to call
         * @param interval {number} the interval in milliseconds
         */
        return function(fn, interval) {

            // The last execution timestamp.
            var lastTime = null;

            // Calls the specified function if the configured interval is passed.
            return function() {
                var time = new Date().getTime();
                if (!lastTime || (time - lastTime) > interval) {
                    fn.apply(null, arguments);
                    lastTime = time;
                }
            };
        };
    })

    // -------------------------------------------------------------------------
    //  Upload File
    // -------------------------------------------------------------------------

    .service('UploadFiles', function() {
        return {
            files : {file: null, mdFile: null}
        };
    })

    // -------------------------------------------------------------------------
    //  Upload File
    // -------------------------------------------------------------------------

    .filter('cstlContext', function() {
        return function(value, putAuth) {
            value = window.localStorage.getItem('cstlUrl') + value;
            return value;
        };
    })


    // -------------------------------------------------------------------------
    //  DashboardHelper
    // -------------------------------------------------------------------------

    .factory('DashboardHelper', function($timeout) {
        /**
         * An helper service to manage dashboard features.
         *
         * Provides methods and variables to manage :
         *  - text filter
         *  - column sorting
         *  - request status
         *  - pagination
         *
         * Query structure : { page: 1, size: 20, text: 'mytext', sort: { order: 'ASC', field: 'myfield' } }
         *
         * @constructor
         * @param {Function} searchMethod The search method to call.
         * @param {Object} [initialQuery] The initial query for search (default is { page: 1, size: 20 }).
         * @param {Object} [initialPage] The initial data page (default is null).
         * @param {Object} [opts] optional object to pass other params into searchMethod.
         */
        function DashboardHelper(searchMethod, initialQuery, initialPage, opts) {

            var self = this;

            var timeout = null;


            // States on the search request status (-1 pending, 0 not sent, 1 success, 2 error).
            self.searchStatus = 0;

            // Search request criteria.
            self.query = initialQuery || { page: 1, size: 20 };

            // Search result page.
            self.page = initialPage;


            // Sends the search query and gets the results.
            self.search = function() {
                self.searchStatus = -1;
                var $q = angular.isDefined(opts) ? searchMethod(self.query,opts) : searchMethod(self.query);
                var promise = $q.$promise || $q;
                promise.then(
                    function searchSuccess(response) {
                        self.searchStatus = 1;
                        self.page = response.data ? response.data : response;
                    },
                    function searchError() {
                        self.searchStatus = 2;
                        self.page = null;
                    }
                );
            };

            // Avoids too much HTTP requests on 'keyup' event for text filter.
            self.searchDebounce = function(ms) {
                self.searchStatus = -1;
                self.query.page = 1;
                $timeout.cancel(timeout);
                timeout = $timeout(self.search, ms || 300);
            };

            // Modify the sort order for result items. Like SQL order we use the ascending order
            // by default.
            self.sortBy = function(field) {
                if (self.isSortedBy(field)) {
                    switch (self.query.sort.order) {
                        case 'ASC':
                            self.query.sort.order = 'DESC';
                            break;
                        default:
                            self.query.sort.order = 'ASC';
                    }
                } else {
                    self.query.sort = { field: field, order: 'ASC' };
                }
            };

            // Checks if the result items are sorted on the specified field.
            self.isSortedBy = function(field) {
                return self.query.sort && (self.query.sort.field === field);
            };

            // Returns the icon class to apply according the current sort order.
            self.getOrderIcon = function(field) {
                if (self.isSortedBy(field)) {
                    switch (self.query.sort.order) {
                        case 'ASC':
                            return 'fa-caret-up';
                        case 'DESC':
                            return 'fa-caret-down';
                    }
                }
                return null;
            };

            // Changes the page index for results.
            self.setPage = function(page) {
                self.query.page = page;
                self.search();
            };
        }

        return DashboardHelper;
    });

