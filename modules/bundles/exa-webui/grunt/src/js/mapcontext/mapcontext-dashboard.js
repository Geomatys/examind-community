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

angular.module('cstl-mapcontext-dashboard', ['cstl-restapi', 'cstl-services', 'ui.bootstrap.modal', 'examind-instance'])

    .controller('MapcontextController', function($scope, Dashboard, Growl, $modal, $window, Examind){

        var DEFAULT_PREVIEW = {
            layer: undefined,
            extent: undefined,
            projection: 'EPSG:3857',
            layerOnly: false
        };

        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.preview = angular.copy(DEFAULT_PREVIEW);

        $scope.cstlUrl = window.localStorage.getItem('cstlUrl');
        $scope.hideScroll = true;

        $scope.values = {
            selectedLayer : null
        };

        $scope.initMapContextDashboard = function() {
            Examind.mapcontexts.getMapLayers().then(
                function(response) {//success
                    Dashboard($scope, response.data, true);
                    $scope.wrap.ordertype='name';
                    $scope.wrap.filtertext='';
                    if($scope.selected) {
                        for(var i=0;i<response.data.length;i++){
                            if($scope.selected.id === response.data[i].id){
                                $scope.selected = response.data[i];
                                break;
                            }
                        }
                    }else {
                        $scope.selected = null;
                    }
                    //display dashboard map
                    setTimeout(function(){
                        $scope.showMapContextDashboardMap();
                    },300);
                }, 
                function() {//error
                    Growl('error','Error','Unable to get list of map context!');
                });
            angular.element($window).bind("scroll", function() {
                $scope.hideScroll = (this.pageYOffset < 220);
                $scope.$apply();
            });
        };

        /**
         * Reset filters for dashboard
         */
        $scope.resetFilters = function(){
            Examind.mapcontexts.getMapLayers().then(
                function(response) {//success
                    Dashboard($scope, response.data, true);
                    $scope.wrap.ordertype='name';
                    $scope.wrap.orderreverse=false;
                    $scope.wrap.filtertext='';
                }, 
                function() {//error
                    Growl('error','Error','Unable to restore list of map context!');
                });
        };

        $scope.selectContextChild = function(item) {
            if (item && $scope.values.selectedLayer && $scope.values.selectedLayer.id === item.id) {
                $scope.values.selectedLayer = null;
            } else {
                $scope.values.selectedLayer = item;
            }
        };


        $scope.toggleUpDownSelected = function() {
            var $header = $('#MapcontextDashboard').find('.selected-item').find('.block-header');
            $header.next().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        $scope.addMapContext = function() {
            var modal = $modal.open({
                templateUrl: 'views/mapcontext/modalAddContext.html',
                controller: 'MapContextModalController',
                resolve: {
                    ctxtToEdit: function () { return null; },
                    layersForCtxt: function () { return null; }
                }
            });

            modal.result.then(function() {
                $scope.initMapContextDashboard();
            });
        };

        $scope.deleteMapContext = function() {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.mapcontext";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    var ctxtName = $scope.selected.name;
                    Examind.mapcontexts.deleteMapContext($scope.selected.id).then(
                        function () {
                            Growl('success', 'Success', 'Map context ' + ctxtName + ' successfully removed');
                            $scope.selected=null;
                            $scope.initMapContextDashboard();
                        }, 
                        function () {
                            Growl('error', 'Error', 'Unable to remove map context ' + ctxtName);
                        });
                }
            });
        };

        $scope.editMapContext = function() {
            var modal = $modal.open({
                templateUrl: 'views/mapcontext/modalAddContext.html',
                controller: 'MapContextModalController',
                resolve: {
                    ctxtToEdit: function () { return angular.copy($scope.selected); },
                    layersForCtxt: function() { return $scope.resolveLayers(); }
                }
            });

            modal.result.then(function() {
                $scope.initMapContextDashboard();
            });
        };

        $scope.showMapContextDashboardMap = function() {
            var selectedContext = $scope.selected;
            if(selectedContext) {
                var mapcontextLayers = $scope.resolveLayers();
                if (mapcontextLayers && mapcontextLayers.length>0) {
                    var cstlUrl = window.localStorage.getItem('cstlUrl');
                    var layerGroup = new ol.layer.Group();
                    for (var i=0; i<mapcontextLayers.length; i++) {
                        var layObj = mapcontextLayers[i];
                        if (layObj.visible) {
                            var layerData;
                            
                            //external wms layer
                            if (layObj.layer.externalServiceUrl) {
                                if (layObj.layer.externalStyle) {
                                    var exStyleName = layObj.layer.externalStyle.split(',')[0];
                                    layerData = MapContextDashboardViewer.createLayerExternalWMSWithStyle(layObj.layer.externalServiceUrl, layObj.layer.externalLayer, exStyleName);
                                } else {
                                    layerData = MapContextDashboardViewer.createLayerExternalWMS(layObj.layer.externalServiceUrl, layObj.layer.externalLayer);
                                }
                            
                            //internal wms layer
                            } else if (layObj.layer.layerId) {
                                var serviceName = (layObj.layer.serviceIdentifier) ? layObj.layer.serviceIdentifier : layObj.service.identifier;
                                var versions    = layObj.layer.serviceVersions;
                                if (layObj.layer.styleName) {
                                    layerData = MapContextDashboardViewer.createLayerWMSWithStyle(cstlUrl, layObj.layer.name, serviceName, layObj.layer.styleName, versions);
                                } else {
                                    layerData = MapContextDashboardViewer.createLayerWMS(cstlUrl, layObj.layer.name, serviceName,versions);
                                }
                            //internal data layer
                            } else {
                                var layerName;
                                var dataItem = layObj.layer;
                                var type = dataItem.type?dataItem.type.toLowerCase():null;
                                if (dataItem.namespace) {
                                    layerName = '{' + dataItem.namespace + '}' + dataItem.name;
                                } else {
                                    layerName = dataItem.name;
                                }
                                if (layObj.styleObj || dataItem.styleName) {
                                    var inDStyleName = layObj.styleObj ? layObj.styleObj.name : dataItem.styleName;
                                    layerData = MapContextDashboardViewer.createLayerWithStyle(cstlUrl, dataItem.dataId, layerName, inDStyleName, null, null, type!=='vector');
                                } else {
                                    layerData = MapContextDashboardViewer.createLayer(cstlUrl, dataItem.dataId, layerName, null, type!=='vector');
                                }
                            }
                            layerData.setOpacity(layObj.opacity / 100);
                            layerGroup.getLayers().push(layerData);
                        }
                    }
                    $scope.preview.layer = layerGroup;
                    $scope.preview.projection = selectedContext.crs;
                    $scope.preview.layerOnly = (selectedContext.crs !== 'EPSG:3857' && selectedContext.crs !== 'EPSG:900913');
                } else {
                    $scope.preview = angular.copy(DEFAULT_PREVIEW);
                }
                if(selectedContext.west && selectedContext.south && selectedContext.east && selectedContext.north && selectedContext.crs) {
                    var extent = [selectedContext.west,selectedContext.south,selectedContext.east,selectedContext.north];
                    //because zoomToExtent take extent in EPSG:4326 we need to reproject the zoom extent
                    if(selectedContext.crs !== 'EPSG:4326' && selectedContext.crs !=='CRS:84'){
                        var projection = ol.proj.get(selectedContext.crs);
                        extent = ol.proj.transformExtent(extent, projection,'EPSG:4326');
                    }
                    $scope.preview.extent = extent;
                } else {
                    $scope.preview.extent = undefined;
                }
            }else {
                $scope.preview = angular.copy(DEFAULT_PREVIEW);
            }
        };

        $scope.resolveLayers = function() {
            var lays = [];
            var styleObj;
            for (var i=0; i<$scope.selected.layers.length; i++) {
                var lay = $scope.selected.layers[i];
                styleObj = undefined;
                //external wms layer
                if (lay.externalServiceUrl && lay.externalStyle) {
                     styleObj = {"name":lay.externalStyle.split(',')[0]};
                     
                // internal wms layer and internal Data     
                } else if (lay.styleId && lay.styleName) {
                    styleObj = {"id": lay.styleId, "name": lay.styleName};
                }
                lays.push({
                    "layer": lay,
                    "visible": lay.visible,
                    "opacity": lay.opacity,
                    "styleObj": styleObj
                });
            }
            lays.sort(function (a, b) {
                return a.layer.order - b.layer.order;
            });
            return lays;
        };

        $scope.truncate = function(small, text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (small === true && text.length > 30) {
                        return text.substr(0, 30) + "...";
                    } else if (small === false && text.length > 40) {
                        return text.substr(0, 40) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (small === true && text.length > 22) {
                        return text.substr(0, 22) + "...";
                    } else if (small === false && text.length > 29) {
                        return text.substr(0, 29) + "...";
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

        $scope.initMapContextDashboard();
    });