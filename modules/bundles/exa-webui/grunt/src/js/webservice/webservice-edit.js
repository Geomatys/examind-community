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

angular.module('cstl-webservice-edit', [
    'cstl-restapi',
    'cstl-services',
    'pascalprecht.translate',
    'ui.bootstrap.modal',
    'examind-instance']
)

    .controller('WebServiceEditController', function($scope, $routeParams ,
                                                     $modal, textService, Dashboard, Growl, $filter,
                                                     StyleSharedService, $cookieStore, $translate, Examind) {
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.preview = { layer: undefined, extent: undefined };

        $scope.tagText = '';
        $scope.type = $routeParams.type;
        $scope.cstlUrl = $cookieStore.get('cstlUrl');
        $scope.url = $scope.cstlUrl + "WS/" + $routeParams.type + "/" + $routeParams.id;
        $scope.urlBoxSize = Math.min($scope.url.length,100);

        $scope.getCurrentLang = function() {
            var lang = $translate.use();
            if(!lang){
                lang = 'en';
            }
            return lang;
        };

        $scope.getVersionsForType = function() {
            if ($scope.type === 'wms') {
                return [{ 'id': '1.1.1','checked':false}, { 'id': '1.3.0','checked':false}];
            }
            if ($scope.type === 'wfs') {
                return [{ 'id': '1.1.0','checked':false}, { 'id': '2.0.0','checked':false}];
            }
            if ($scope.type === 'wcs') {
                return [{ 'id': '1.0.0','checked':false}, { 'id': '2.0.1','checked':false}];
            }
            if ($scope.type === 'wmts') {
                return [{ 'id': '1.0.0','checked':false}];
            }
            if ($scope.type === 'tms') {
                return [{ 'id': '1.0.0','checked':false}];
            }
            if ($scope.type === 'csw') {
                return [{ 'id': '2.0.2','checked':false}, { 'id': '3.0.0', 'checked': true}];
            }
            if ($scope.type === 'sos') {
                return [{ 'id': '1.0.0','checked':false}, { 'id': '2.0.0','checked':false}];
            }
            if ($scope.type === 'sts') {
                return [{ 'id': '1.0.0','checked':true}];
            }
            if ($scope.type === 'wps') {
                return [{ 'id': '1.0.0','checked':false}, { 'id': '2.0.0','checked':false}];
            }
            return [];
        };

        Examind.ogcServices.getMetadata($scope.type,
                                        $routeParams.id,
                                        $scope.getCurrentLang()).then(
            function(response){//on success
                $scope.metadata = response.data;
                $scope.versions = $scope.getVersionsForType();
                if($scope.versions.length>0){
                    for(var i=0;i<$scope.versions.length;i++){
                        var version = $scope.versions[i];
                        version.checked = ($scope.metadata.versions.indexOf(version.id)!==-1);
                    }
                }
            },function(response){//on error
                Growl('error','Error','Unable to get service metadata');
            }
        );

        $scope.tabdata = true;
        $scope.tabdesc = false;
        $scope.tabmetadata = false;

        $scope.selectTab = function(item) {
            if (item === 'tabdata') {
                $scope.tabdata = true;
                $scope.tabdesc = false;
                $scope.tabmetadata = false;
            } else if (item === 'tabdesc') {
                $scope.tabdata = false;
                $scope.tabdesc = true;
                $scope.tabmetadata = false;
            } else if (item === 'tabmetadata') {
                $scope.tabdata = false;
                $scope.tabdesc = false;
                $scope.tabmetadata = true;
            } else {
                $scope.tabdata = false;
                $scope.tabdesc = false;
                $scope.tabmetadata = false;
            }
        };

        $scope.initScope = function() {
            Examind.ogcServices.get($scope.type, $routeParams.id, $scope.getCurrentLang()).then(function (service) {
                $scope.service = service.data;

                if ($scope.type === 'csw') {
                    Examind.csw.getRecordsCount($routeParams.id).then(function(max) {
                        Examind.csw.getRecords($routeParams.id, max.data.value, 0).then(function(response) {
                            Dashboard($scope, response.data, false);
                            $scope.wrap.filtertype = "";

                            var mdIds = [];
                            for (var i=0; i<response.data.length; i++) {
                                mdIds.push(response.data[i].identifier);
                            }
                            Examind.metadata.getAssociatedData(mdIds).then(
                                function(response) { $scope.relatedDatas = response.data;},
                                function() { Growl('error','Error','Unable to get related data for metadata'); }
                            );
                        });
                    });
                } else if ($scope.type === 'sos' || $scope.type === 'sts' ) {
                    Examind.sensorServices.getSensorsTree($scope.service.id).then(
                    function(sensors) {
                        Dashboard($scope, sensors.data.children, false);
                        $scope.layers = sensors.data.children;

                    }, function() { Growl('error','Error','Unable to list sensors'); });
                } else {
                    Examind.ogcServices.getConfig($scope.type, $routeParams.id).then(function(response) {
                        $scope.config = response.data;
                    });
                    Examind.map.getLayers($scope.type, $routeParams.id).then(function(response) {
                        $scope.layers = response.data;
                        Dashboard($scope, response.data, true);
                        $scope.wrap.filtertype = "";
                        setTimeout(function(){
                            $scope.showLayerDashboardMap();
                        },300);
                    });
                }
            });
        };

        /**
         * Reset filters for dashboard
         */
        $scope.resetFilters = function(){
            $scope.wrap.ordertype= ($scope.service && $scope.service.type && ($scope.service.type.toLowerCase()==='sos' || $scope.service.type.toLowerCase()==='sts')) ? 'id' : ($scope.service && $scope.service.type && $scope.service.type.toLowerCase==='csw') ? 'title' : 'name';
            $scope.wrap.orderreverse=false;
            $scope.wrap.filtertext='';
            $scope.selected=null;
            if($scope.type !== 'csw' && $scope.type !== 'sos' && $scope.type !== 'sts') {
                $scope.showLayerDashboardMap();
            }
        };


        // define which version to set
        $scope.selectedVersion = function (){
            var selVersions = $filter('filter')($scope.versions, {checked: true});
            $scope.metadata.versions = [];
            for(var i=0; i < selVersions.length; i++) {
                $scope.metadata.versions.push(selVersions[i].id);
            }
        };

        $scope.addTag = function() {
            if (!$scope.tagText || $scope.tagText === '' || $scope.tagText.length === 0) {
                return;
            }
            if ($scope.metadata.keywords ===null){
                $scope.metadata.keywords = [];
            }
            $scope.metadata.keywords.push($scope.tagText);
            $scope.tagText = '';
        };

        $scope.deleteTag = function(key) {
            if ($scope.metadata.keywords.length > 0 &&
                $scope.tagText.length === 0 && !key) {
                $scope.metadata.keywords.pop();
            } else if (key) {
                $scope.metadata.keywords.splice(key, 1);
            }
        };

        $scope.selectedMetadataChild = null;
        $scope.selectedSensorsChild = null;

        $scope.selectMetadataChild = function(item) {
            if ($scope.selectedMetadataChild === item) {
                $scope.selectedMetadataChild = null;
            } else {
                $scope.selectedMetadataChild = item;
            }
        };
        $scope.selectSensorsChild = function(item) {
            if ($scope.selectedSensorsChild === item) {
                $scope.selectedSensorsChild = null;
            } else {
                $scope.selectedSensorsChild = item;
            }
        };

        $scope.saveServiceMetadata = function() {
            Examind.ogcServices.setMetadata($scope.service.type, $scope.service.identifier, $scope.metadata).then(
                function(response) {
                    if (response.data.status==="Success") {
                        Growl('success','Success','Service description successfully updated');
                    }else{
                        Growl('error','Error','Service description update failed due to :'+response.data.status);
                    }
                },
                function() {
                    Growl('error','Error','Service description update failed');
                }
            );
        };

        // Show Capa methods
        $scope.showCapa = function(service) {
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
        $scope.showLogs = function(service) {
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

        $scope.reload = function(service){
            Examind.ogcServices.restart(service.type, service.identifier, true).then(
                function() { Growl('success','Success','Service '+ service.name +' successfully reloaded'); },
                function() { Growl('error','Error','Service '+ service.name +' reload failed'); }
            );
        };

        $scope.startOrStop = function(service){
            if(service.status==='STARTED'){
                Examind.ogcServices.stop(service.type, service.identifier).then(function(response) {
                    if (response.data.status==="Success") {
                        $scope.service.status = "NOT_STARTED";
                        Growl('success','Success','Service '+ service.name +' successfully stopped');
                        $scope.showLayerDashboardMap();
                    }
                }, function() { Growl('error','Error','Service '+ service.name +' stop failed'); });
            }else{
                Examind.ogcServices.start(service.type, service.identifier).then(function(response) {
                    if (response.data.status==="Success") {
                        $scope.service.status = "STARTED";
                        Growl('success','Success','Service '+ service.name +' successfully started');
                        $scope.showLayerDashboardMap();
                    }
                }, function() { Growl('error','Error','Service '+ service.name +' start failed'); });
            }
        };

        // Allow to choose data to add for this service
        $scope.showDataToAdd = function() {
            var modal = $modal.open({
                templateUrl: 'views/data/modalDataChoose.html',
                controller: 'DataModalController',
                resolve: {
                    exclude: function() { return $scope.layers; },
                    service: function() { return $scope.service; }
                }
            });
            modal.result.then(function() {
                if ($scope.type.toLowerCase() !== 'sos' && $scope.type.toLowerCase() !== 'sts') {
                    Examind.map.getLayers($scope.type, $routeParams.id).then(function (response) {
                        $scope.layers = response.data;
                        Dashboard($scope, response.data, true);
                        $scope.selected = null;
                        $scope.showLayerDashboardMap();
                    });
                } else {
                    $scope.initScope();
                }
            });
        };

        $scope.showDataToAddWMTS = function() {
            var modal = $modal.open({
                templateUrl: 'views/webservice/wmts/modalAddLayer.html',
                controller: 'WMTSAddLayerModalController',
                resolve: {
                    service: function() { return $scope.service; },
                    epsgCodes: function(Examind) {
                        return Examind.crs.listAll();
                    }
                }
            });
            modal.result.then(function() {
                Examind.map.getLayers($scope.type,
                                      $routeParams.id).then(
                    function (response) {//success
                        $scope.layers = response.data;
                        Dashboard($scope, response.data, true);
                        $scope.selected = null;
                        $scope.showLayerDashboardMap();
                    }
                );
            });
        };

        $scope.deleteLayer = function() {
            var keymsg = ($scope.service.type.toLowerCase() === 'wmts') ? "dialog.message.confirm.delete.tiledLayer" : "dialog.message.confirm.delete.layer";
            if ($scope.selected) {
                var dlg = $modal.open({
                    templateUrl: 'views/modal-confirm.html',
                    controller: 'ModalConfirmController',
                    resolve: {
                        'keyMsg':function(){return keymsg;}
                    }
                });
                dlg.result.then(function(cfrm){
                    if(cfrm){
                        if ($scope.service.type.toLowerCase() === 'sos' || $scope.service.type.toLowerCase() === 'sts') {
                            var idToDel = ($scope.selectedSensorsChild) ? $scope.selectedSensorsChild.identifier : $scope.selected.identifier;
                            Examind.sensorServices.removeSensor($scope.service.id, idToDel).then(
                              function() {
                                Growl('success', 'Success', 'Sensor ' + idToDel + ' successfully removed from service ' + $scope.service.name);
                                $scope.initScope();
                                $scope.selected=null;
                                $scope.selectedSensorsChild = null;
                            },function () {
                                Growl('error', 'Error', 'Unable to remove sensor ' + idToDel + ' from service ' + $scope.service.name);
                            });
                        } else {
                            Examind.map.deleteLayer($scope.service.type,
                                                    $scope.service.identifier,
                                                    $scope.selected.name,
                                                   {value: $scope.selected.namespace}).then(
                                function () {//on success
                                    if ($scope.service.type.toLowerCase() === 'wmts') {
                                        $scope.deleteTiledData($scope.selected.name, $scope.selected.providerId);
                                    }
                                    Growl('success', 'Success', 'Layer ' + $scope.selected.name + ' successfully deleted from service ' + $scope.service.name);
                                    Examind.map.getLayers($scope.type, $routeParams.id).then(
                                    function (response) {
                                        $scope.layers = response.data;
                                        Dashboard($scope, response.data, true);
                                        $scope.selected=null;
                                        $scope.showLayerDashboardMap();
                                    });
                                },
                                function () {
                                    Growl('error', 'Error', 'Layer ' + $scope.selected.name + ' failed to be deleted from service ' + $scope.service.name);
                                }
                            );
                        }
                    }
                });
            }
        };

        $scope.deleteTiledData = function(layerName, providerId) {
            Examind.datas.deletePyramidFolder(providerId).then(function(response) {
                if(response.data.isPyramid){
                    Examind.providers.delete(providerId).then(function() {}, function() {
                        Growl('error','Error','Unable to delete data for layer '+ layerName);
                    });
                }
            });
        };

        /**
         * Open metadata viewer popup and display metadata
         * in appropriate template depending on data type property.
         * this function is called from metadata dashboard.
         */
        $scope.displayMetadataFromCSW = function() {
            var type = 'import';
            if($scope.selectedMetadataChild){
                type = $scope.selectedMetadataChild.type.toLowerCase();
            }
            if(type.toLowerCase() === 'coverage'){
                type = 'raster';
            }
            $modal.open({
                templateUrl: 'views/data/modalViewMetadata.html',
                controller: 'ViewMetadataModalController',
                resolve: {
                    'dashboardName':function(){return 'dataset';},
                    'metadataValues':function(Examind){
                        return Examind.csw.getJsonMetadata($scope.service.identifier,$scope.selected.identifier,type,true);
                    }
                }
            });
        };

        /**
         * Open metadata editor in modal popup.
         */
        $scope.displayMetadataEditor = function() {
            var typeToSend;
            if($scope.selectedMetadataChild){
                typeToSend = $scope.selectedMetadataChild.type.toLowerCase();
            }else {
                typeToSend = 'import';
            }
            if(typeToSend.toLowerCase() === 'coverage'){
                typeToSend = 'raster';
            }
            openModalEditor($scope.service.identifier,$scope.selected.identifier,typeToSend,typeToSend);
        };

        /**
         * Open modal for metadata editor
         * for given provider id, data type and template.
         * @param serviceId
         * @param recordId
         * @param type
         * @param template
         */
        function openModalEditor(serviceId,recordId,type,template){
            $modal.open({
                templateUrl: 'views/data/modalEditMetadata.html',
                controller: 'EditCSWMetadataModalController',
                resolve: {
                    'serviceId':function(){return serviceId;},
                    'recordId':function(){return recordId;},
                    'type':function(){return type;},
                    'template':function(){return template;}
                }
            });
        }

        /**
         * Open modal to edit layer title.
         * in future this modal can be used to edit other attributes.
         */
        $scope.editLayerInfo = function() {
            var modal = $modal.open({
                templateUrl: 'views/data/layerInfo.html',
                controller: 'LayerInfoModalController',
                resolve: {
                    'serviceType':function(){return $scope.service.type;},
                    'serviceIdentifier':function(){return $scope.service.identifier;},
                    'selectedLayer':function(){return $scope.selected;}
                }
            });
            modal.result.then(function() {
                Examind.map.getLayers($scope.type, $routeParams.id).then(function (response) {
                    $scope.layers = response.data;
                    Dashboard($scope, response.data, true);
                    $scope.wrap.ordertype='name';
                    $scope.showLayerDashboardMap();
                });
            });
        };

        /**
         * binding action to delete metadata from csw service dashboard page.
         */
        $scope.deleteMetadata = function() {
            if ($scope.selected) {
                var dlg = $modal.open({
                    templateUrl: 'views/modal-confirm.html',
                    controller: 'ModalConfirmController',
                    resolve: {
                        'keyMsg':function(){return "dialog.message.confirm.delete.metadata";}
                    }
                });
                dlg.result.then(function(cfrm){
                    if(cfrm){
                        Examind.csw.deleteRecord($scope.service.identifier, $scope.selected.identifier).then(
                            function() {
                                Growl('success','Success','Metadata deleted');
                                Examind.csw.getRecordsCount($routeParams.id).then(function(max) {
                                    Examind.csw.getRecords($routeParams.id, max.data.value, 0).then(function(response) {
                                        Dashboard($scope, response.data, false);
                                        $scope.wrap.filtertype = "";
                                    });
                                });
                            }, function() { Growl('error','Error','Failed to delete metadata'); }
                        );
                    }
                });
            }
        };

        $scope.showLayerDashboardMap = function() {
            if($scope.type !== 'sos' && $scope.type !== 'csw' && $scope.type !== 'wps' && $scope.type !== 'sts') {
                if($scope.type === 'wmts') {
                    if($scope.service.status !== "STARTED"){
                        return;
                    }
                    if($scope.selected) {
                        var wmtslayerName = $scope.selected.name;
                        // Get wmts values: resolutions, extent, matrixSet and matrixIds
                        textService.capa($scope.service.type.toLowerCase(),
                                $scope.service.identifier,
                                $scope.service.versions[0])
                            .then(function successCallback(response) {
                                var data = response.data;
                                Examind.map.extractWMTSLayerInfo(
                                    $scope.service.type,
                                    $scope.service.identifier,
                                    wmtslayerName,
                                    WmtsLayerDashboardViewer.projection,
                                    data).then(
                                    function(response){//success
                                        var wmtsValues = {
                                            "url":$scope.cstlUrl +'WS/wmts/'+ $scope.service.identifier,
                                            "resolutions": response.data.resolutions,
                                            "matrixSet":response.data.matrixSet,
                                            "matrixIds":response.data.matrixIds,
                                            "style":response.data.style,
                                            "dataExtent":response.data.dataExtent
                                        };
                                        $scope.preview.layer = WmtsLayerDashboardViewer.createLayer(wmtslayerName, $scope.service.identifier, wmtsValues);
                                        $scope.preview.extent = ol.proj.transformExtent(wmtsValues.dataExtent, WmtsLayerDashboardViewer.projection, 'CRS:84');
                                    },
                                    function(response){//error
                                        Growl('warning','Warning','Unable to show this layer cause: '+response.data.message);
                                        $scope.preview.extent = undefined;
                                        $scope.preview.layer = undefined;
                                    }
                                );
                            }, function errorCallback(response) {
                                Growl('warning','Warning','Unable to show this layer cause: '+response.data.message);
                                $scope.preview.extent = undefined;
                                $scope.preview.layer = undefined;
                            });
                    }else {
                        $scope.preview.extent = undefined;
                        $scope.preview.layer = undefined;
                    }
                }else {
                    if($scope.service.status !== "STARTED"){
                        return;
                    }
                    if($scope.selected) {
                        var layerName = $scope.selected.alias ? $scope.selected.alias : $scope.selected.namespace ? $scope.selected.namespace+':'+$scope.selected.name: $scope.selected.name;
                        var layerData;
                        var providerId = $scope.selected.provider;
                        var type = $scope.selected.type?$scope.selected.type.toLowerCase():null;
                        if ($scope.selected.targetStyle && $scope.selected.targetStyle.length > 0) {
                            if($scope.service.type.toLowerCase() === 'wms') {
                                layerData = LayerDashboardViewer.createLayerWMSWithStyle($scope.cstlUrl, layerName,$scope.service.identifier,$scope.selected.targetStyle[0].name,$scope.service.versions);
                            }else {
                                layerData = LayerDashboardViewer.createLayerWithStyle($scope.cstlUrl,$scope.selected.dataId,layerName,
                                    $scope.selected.targetStyle[0].name,null,null,type!=='vector');
                            }
                        } else {
                            if($scope.service.type.toLowerCase() === 'wms') {
                                layerData = LayerDashboardViewer.createLayerWMS($scope.cstlUrl, layerName, $scope.service.identifier,$scope.service.versions);
                            }else {
                                layerData = LayerDashboardViewer.createLayer($scope.cstlUrl,$scope.selected.dataId,layerName,null,type!=='vector');
                            }
                        }
                        $scope.preview.layer = layerData;
                        Examind.datas.getGeographicExtent($scope.selected.dataId).then(
                            function(response){
                                $scope.preview.extent = response.data.boundingBox;
                            },
                            function() {
                                $scope.preview.extent = undefined;
                            });
                    }else {
                        $scope.preview.extent = undefined;
                        $scope.preview.layer = undefined;
                    }
                }
            }
        };

        $scope.showSensor = function() {
            var sensorId = ($scope.selectedSensorsChild) ? $scope.selectedSensorsChild.identifier : $scope.selected.identifier;
            $modal.open({
                templateUrl: 'views/sensor/modalSensorView.html',
                controller: 'SensorModalController',
                resolve: {
                    service: function() { return $scope.service; },
                    sensorId: function() { return sensorId; }
                }
            });
        };

        $scope.toggleUpDownSelected = function() {
            var $header = $('#serviceDashboard').find('.selected-item').find('.block-header');
            $header.next().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        // Style methods
        $scope.showStyleList = function() {
            StyleSharedService.showStyleList($scope,$scope.selected);
        };

        $scope.unlinkStyle = function(style,layer) {
            var styleId = style.id;
            var styleProvider = style.provider;
            var styleName = style.name;
            var layerId = layer.id;
            var layerProvider = layer.provider;
            var layerName = layer.name;

            StyleSharedService.unlinkStyle($scope,styleProvider, styleId, styleName, layerProvider, layerId,layerName, $scope.selected);
        };

        $scope.truncate = function(small, text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (small === true && text.length > 22) {
                        return text.substr(0, 22) + "...";
                    } else if (small === false && text.length > 65) {
                        return text.substr(0, 65) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (small === true && text.length > 15) {
                        return text.substr(0, 15) + "...";
                    } else if (small === false && text.length > 55) {
                        return text.substr(0, 55) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 992) {
                    if (text.length > 35) {
                        return text.substr(0, 35) + "...";
                    } else {return text;}
                }
            }
        };
        $scope.truncateTitleBlock = function(text){
            if(text) {
                if (window.innerWidth >= 1200) {
                    if (text.length > 30) {
                        return text.substr(0, 30) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 1200 && window.innerWidth >= 992) {
                    if (text.length > 25) {
                        return text.substr(0, 25) + "...";
                    } else {return text;}
                } else if (window.innerWidth < 992) {
                    if (text.length > 17) {
                        return text.substr(0, 17) + "...";
                    } else {return text;}
                }
            }
        };
    })
    .controller('LayerInfoModalController', function($scope, $modalInstance,Examind,Growl,
                                                     serviceType,serviceIdentifier,selectedLayer){
        $scope.serviceType = serviceType;
        $scope.serviceIdentifier = serviceIdentifier;
        $scope.selectedLayer = selectedLayer;
        $scope.layerForm = {
            "title": $scope.selectedLayer.title
        };

        $scope.close = function() {
            $modalInstance.dismiss('close');
        };

        $scope.save = function() {
            $scope.selectedLayer.title = $scope.layerForm.title;
            Examind.map.updateLayerTitle($scope.serviceType, $scope.serviceIdentifier, $scope.selectedLayer).then(
                function(response) {//success
                    Growl('success','Success','Layer information saved with success!');
                    $modalInstance.close();
                },
                function(response) {//error
                    Growl('error','Error','Layer name already exists!');
                }
            );
        };
    })
    .controller('EditCSWMetadataModalController', function($scope, $modalInstance, $controller,Growl,Examind,serviceId,recordId,type,template) {
        //$scope.provider = id;
        $scope.serviceId = serviceId;
        $scope.recordId = recordId;
        $scope.type = type;
        $scope.template = template;
        $scope.theme = 'csw';
        $scope.contentError = false;

        $scope.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        $scope.close = function () {
            $modalInstance.close();
        };

        $scope.loadMetadataValues = function(){
            Examind.csw.getJsonMetadata($scope.serviceId,
                                 $scope.recordId,
                                 $scope.type,
                                 false).then(
                function(response){//success
                    if (response && response.data && response.data.root) {
                        $scope.metadataValues.push({"root":response.data.root});
                        $scope.contentError = false;
                    }else {
                        $scope.contentError = true;
                    }
                },
                function(response){//error
                    $scope.contentError = true;
                    Growl('error','Error','The server returned an error!');
                }
            );
        };

        /**
         * Save for metadata in modal editor mode.
         */
        $scope.save2 = function() {
            if($scope.metadataValues && $scope.metadataValues.length>0){
                Examind.csw.saveMetadata($scope.serviceId, $scope.recordId, $scope.template, $scope.metadataValues[0]).then(
                    function(response) {//success
                        $scope.close();
                        Growl('success','Success','Metadata saved with success!');
                    },
                    function(response) {//error
                        Growl('error','Error','Failed to save metadata because the server returned an error!');
                    }
                );
            }
        };

        $controller('EditMetadataController', {$scope: $scope});

    })

    .controller('WMTSAddLayerModalController', function($scope, $modalInstance, service,
                                                        Growl, epsgCodes, Examind) {

        $scope.pyramidFlag = false;
        $scope.crsList = [];

        //the wmts service object
        $scope.service = service;

        // handle display mode for this modal popup
        $scope.mode = {
            display: 'sourceSelection',
            previous: undefined
        };
        // for SDI this params are hardcoded
        $scope.tileFormat = 'PNG'; //PNG will be used as default
        var DEFAULT_PROJECTION = {
            code:"EPSG:3857",
            desc:"3857 - WGS 84 / Pseudo-Mercator"
        };
        $scope.crs = DEFAULT_PROJECTION.code;
        $scope.values = {
            epsgList : epsgCodes.data,
            selectedProjection : DEFAULT_PROJECTION,
            userLayerName : '',
            listSelect : [],
            listWMTSLayers : [],
            selectedContext : null
        };

        $scope.dismiss = function() {
            $modalInstance.dismiss('close');
        };

        $scope.close = function() {
            $modalInstance.close();
        };

        $scope.isValidWMTSLayerName = function(){
            var letters = /^[A-Za-zàèìòùáéíóúäëïöüñãõåæøâêîôû0-9\-_]+$/;
            var name = $scope.values.userLayerName;
            var passRegEx = false;
            if(name && name.match(letters)) {
                passRegEx = true;
            }
            return passRegEx;
        };

        $scope.isLayerNameExists = function() {
            return checkLayerName($scope.values.userLayerName);
        };

        function checkLayerName(name) {
            if(name && name.length>0 &&
                $scope.values.listWMTSLayers && $scope.values.listWMTSLayers.length>0){
                for(var i=0;i<$scope.values.listWMTSLayers.length;i++){
                    var lay=$scope.values.listWMTSLayers[i];
                    if(lay.name === name || lay.alias === name) {
                        return true;
                    }
                }
            }
            return false;
        }

        $scope.goToLastStep = function() {
            //get all layers in this wmts service to compare for existing layer names.
            Examind.map.getLayers($scope.service.type,
                                  $scope.service.identifier).then(
                function (response) {//success
                    $scope.values.listWMTSLayers = response.data;
                    if($scope.mode.display==='internal' && $scope.values.userLayerName === '' && $scope.values.listSelect.length===1){
                        //set the layerName for singleton list
                        var name = $scope.values.listSelect[0].name;
                        if(!checkLayerName(name+'_pyramid')){
                            $scope.values.userLayerName = name+'_pyramid';
                        }else {
                            $scope.values.userLayerName = '';
                        }
                    }else {
                        $scope.values.userLayerName = '';
                    }
                    $scope.mode.previous=$scope.mode.display;
                    $scope.mode.display='lastStep';
                },function(){//error
                    Growl('warning', 'Warning', 'An error occurred!');
                }
            );
        };

        $scope.listcrs = function () {
            $scope.pyramidFlag = !$scope.pyramidFlag;
            if ($scope.pyramidFlag) {
                var dataId = $scope.values.listSelect.map(function (value) {
                    return value.id;
                })[0];
                Examind.datas.describePyramid(dataId).then(
                    function (response) {//success
                        if ($scope.crsList && $scope.crsList.length > 0) {
                            $scope.crsList = $scope.crsList.concat(response.data.crs);
                        } else {
                            $scope.crsList = response.data.crs;
                        }
                    }, function () {//error
                        Growl('warning', 'Warning', 'An error occurred!');
                    }
                );
            }
        };

        var loadedCRS = null;

        $scope.canShowUseExistingPyramid = function () {
            if ($scope.mode.previous !== 'internal' || $scope.values.listSelect.length !== 1) {
                return false;
            } else if ($scope.values.listSelect[0].subtype === 'pyramid') {
                return true;
            } else {
                if ($scope.values.listSelect[0].linkedDatas && $scope.values.listSelect[0].linkedDatas.length > 0) {
                    var find = $scope.values.listSelect[0].linkedDatas.find(function (item) {
                        return item.subtype === 'pyramid';
                    });

                    if (find) {
                        if (!loadedCRS) {
                            $scope.values.listSelect[0].linkedDatas.forEach(function (d) {
                                loadedCRS = {};
                                Examind.datas.describePyramid(d.id).then(
                                    function (response) {//success
                                        loadedCRS[response.data.crs[0]] = d;
                                        if ($scope.crsList && $scope.crsList.length > 0) {
                                            $scope.crsList = $scope.crsList.concat(response.data.crs);
                                        } else {
                                            $scope.crsList = response.data.crs;
                                        }
                                    }, function () {//error
                                        Growl('warning', 'Warning', 'An error occurred!');
                                    }
                                );
                            });
                        }
                        return true;
                    } else {
                        return false;
                    }

                } else {
                    return false;
                }
            }
        };


        $scope.submitWMTSLayer = function () {
            if ($scope.pyramidFlag) {
                var dataId = $scope.values.listSelect.map(function (value) {
                    return value.id;
                })[0];
                var providerId = $scope.values.listSelect.map(function (value) {
                    return value.providerId;
                })[0];

                Examind.map.addLayer($scope.service.type, $scope.service.identifier,
                    {
                        layerAlias: $scope.values.userLayerName,
                        layerId: loadedCRS ? loadedCRS[$scope.values.selectedProjection.code].id : dataId,
                        serviceType: $scope.service.type,
                        serviceId: $scope.service.identifier,
                        providerId: loadedCRS ? loadedCRS[$scope.values.selectedProjection.code].providerId : providerId
                    }).then(
                    function () {//success
                        Growl('success', 'Success', 'Layer successfully added to service ' + $scope.service.name);
                        $scope.close();
                    },
                    function () {
                        Growl('error', 'Error', 'Layer failed to be added to service ' + $scope.service.name);
                        $scope.dismiss();
                    }
                );
            } else {
                if ($scope.values.selectedProjection && $scope.values.selectedProjection.code) {
                    $scope.crs = $scope.values.selectedProjection.code;
                } else {
                    $scope.values.selectedProjection = DEFAULT_PROJECTION;
                }
                if ($scope.mode.previous === 'internal') {
                    if ($scope.values.listSelect.length === 0) {
                        Growl('warning', 'Warning', 'No data selected!');
                        return;
                    }
                    var dataIds = $scope.values.listSelect.map(function (value) {
                        return value.id;
                    });
                    Examind.datas.pyramidData($scope.crs, $scope.values.userLayerName, dataIds).then(
                        function (response) {//success
                            response = response.data;
                            if (response.dataId && response.providerId) {
                                Examind.map.addLayer($scope.service.type, $scope.service.identifier,
                                    {
                                        layerAlias: response.dataId,
                                        layerId: response.dataId,
                                        serviceType: $scope.service.type,
                                        serviceId: $scope.service.identifier,
                                        providerId: response.providerId
                                    }).then(
                                    function () {//success
                                        Growl('success', 'Success', 'Layer successfully added to service ' + $scope.service.name);
                                        $scope.close();
                                    },
                                    function () {
                                        Growl('error', 'Error', 'Layer failed to be added to service ' + $scope.service.name);
                                        $scope.dismiss();
                                    }
                                );
                            }
                        }, function (response) {//error
                            Growl('error', 'Error', 'Failed to generate pyramid data');
                        }
                    );
                } else if ($scope.mode.previous === 'mapcontext') {
                    if ($scope.values.selectedContext === null) {
                        Growl('warning', 'Warning', 'No map context selected!');
                        return;
                    }
                    Examind.mapcontexts.pyramidMapContext($scope.values.selectedContext.id,
                        $scope.crs,
                        $scope.values.userLayerName).then(
                        function (response) {//on success
                            response = response.data;
                            if (response.dataId && response.providerId) {
                                Examind.map.addLayer($scope.service.type, $scope.service.identifier,
                                    {
                                        layerAlias: response.dataId,
                                        layerId: response.dataId,
                                        serviceType: $scope.service.type,
                                        serviceId: $scope.service.identifier,
                                        providerId: response.providerId
                                    }).then(
                                    function () {//success
                                        Growl('success', 'Success', 'Layer successfully added to service ' + $scope.service.name);
                                        $scope.close();
                                    },
                                    function () {
                                        Growl('error', 'Error', 'Layer failed to be added to service ' + $scope.service.name);
                                        $scope.dismiss();
                                    }
                                );
                            }
                        },
                        function (response) {//on error
                            Growl('error', 'Error', 'Failed to generate pyramid data');
                        }
                    );
                }
            }
        };

    })

    .controller('Step1WMTSInternalDataController', function($scope, Dashboard,
                                                            Examind, $cookieStore,$filter) {
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.wrap.nbbypage = 5;

        $scope.dataSelect={all:false};

        $scope.clickFilter = function(ordType){
            $scope.wrap.ordertype = ordType;
            $scope.wrap.orderreverse = !$scope.wrap.orderreverse;
        };

        $scope.initInternalDataWMTS = function() {
            Examind.datas.getDataListLight(false).then(function (response) {
                Dashboard($scope, response.data, true);
            });
            if($scope.mode.previous!=='lastStep') {
                $scope.values.listSelect.splice(0, $scope.values.listSelect.length);//clear array
            }
            setTimeout(function(){
                $scope.previewData();
            },200);
        };

        $scope.previewData = function() {
            //clear the map
            if (DataViewer.map) {
                DataViewer.map.setTarget(undefined);
            }
            var cstlUrl = $cookieStore.get('cstlUrl');
            DataViewer.initConfig();
            if($scope.values.listSelect.length >0){
                var layerName,providerId;
                for(var i=0;i<$scope.values.listSelect.length;i++){
                    var dataItem = $scope.values.listSelect[i];
                    if (dataItem.namespace) {
                        layerName = '{' + dataItem.namespace + '}' + dataItem.name;
                    } else {
                        layerName = dataItem.name;
                    }
                    providerId = dataItem.provider;
                    var layerData;
                    var type = dataItem.type?dataItem.type.toLowerCase():null;
                    if (dataItem.targetStyle && dataItem.targetStyle.length > 0) {
                        layerData = DataViewer.createLayerWithStyle(cstlUrl,dataItem.id,layerName,
                                                                    dataItem.targetStyle[0].name,null,null,type!=='vector');
                    } else {
                        layerData = DataViewer.createLayer(cstlUrl,dataItem.id,layerName,null,type!=='vector');
                    }
                    //to force the browser cache reloading styled layer.
                    layerData.get('params').ts=new Date().getTime();
                    DataViewer.layers.push(layerData);
                }
                var dataIds = $scope.values.listSelect.map(function (value) {
                    return value.id;
                });
                Examind.datas.mergedDataExtent(dataIds).then(
                    function(response) {// on success
                        DataViewer.initMap('styledMapPreviewForWMTS');
                        if(response.data && response.data.boundingBox) {
                            var bbox = response.data.boundingBox;
                            var extent = [bbox[0],bbox[1],bbox[2],bbox[3]];
                            DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                        }
                    }, function() {//on error
                        // failed to calculate an extent, just load the full map
                        DataViewer.initMap('styledMapPreviewForWMTS');
                    }
                );
            }else {
                DataViewer.initMap('styledMapPreviewForWMTS');
                DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
            }
        };

        /**
         * Proceed to select all items of dashboard
         * depending on the property binded to checkbox.
         */
        $scope.selectAllData = function() {
            var array = $filter('filter')($scope.wrap.fullList, {'type':$scope.wrap.filtertype, '$': $scope.wrap.filtertext},$scope.wrap.matchExactly);
            $scope.values.listSelect = ($scope.dataSelect.all) ? array.slice(0) : [];
            $scope.previewData();
        };
        /**
         * binding call when clicking on each row item.
         */
        $scope.toggleDataInArray = function(item){
            var itemExists = false;
            for (var i = 0; i < $scope.values.listSelect.length; i++) {
                if ($scope.values.listSelect[i].id === item.id) {
                    itemExists = true;
                    $scope.values.listSelect.splice(i, 1);//remove item
                    break;
                }
            }
            if(!itemExists){
                $scope.values.listSelect.push(item);
            }
            $scope.dataSelect.all=($scope.values.listSelect.length === $scope.wrap.fullList.length);
            $scope.previewData();

        };
        /**
         * Returns true if item is in the selected items list.
         * binding function for css purposes.
         * @param item
         * @returns {boolean}
         */
        $scope.isInSelected = function(item){
            for(var i=0; i < $scope.values.listSelect.length; i++){
                if($scope.values.listSelect[i].id === item.id){
                    return true;
                }
            }
            return false;
        };

        $scope.setTargetStyle = function(data,index) {
            var tmp = data.targetStyle.splice(index,1);
            data.targetStyle.unshift(tmp[0]);
            $scope.previewData();
        };

        /**
         * truncate text with JS.
         * Why not use CSS for this?
         *
         * css rule is
         * {
         *  width: 100px
         *  white-space: nowrap
         *  overflow: hidden
         *  text-overflow: ellipsis // This is where the magic happens
         *  }
         *
         * @param text
         * @param length
         * @returns {string}
         */
        $scope.truncate = function(text,length){
            if(text) {
                return (text.length > length) ? text.substr(0, length) + "..." : text;
            }
        };

        $scope.initInternalDataWMTS();
    })

    .controller('Step1WMTSMapContextController', function($scope, Dashboard,Growl, $cookieStore, Examind) {
        /**
         * To fix angular bug with nested scope.
         */
        $scope.wrap = {};

        $scope.wrap.nbbypage = 5;

        $scope.clickFilter = function(ordType){
            $scope.wrap.ordertype = ordType;
            $scope.wrap.orderreverse = !$scope.wrap.orderreverse;
        };

        $scope.initMapContextWMTS = function() {
            Examind.mapcontexts.getMapLayers().then(
                function(response) {//success
                    Dashboard($scope, response.data, true);
                    $scope.wrap.ordertype='name';
                    $scope.wrap.filtertext='';
                },
                function() {//error
                    Growl('error','Error','Unable to show layers list!');
            });

            if($scope.mode.previous!=='lastStep') {
                $scope.values.selectedContext = null;
            }
            setTimeout(function(){
                $scope.previewMapContext();
            },200);
        };

        $scope.previewMapContext = function() {
            //clear the map
            if (DataViewer.map) {
                DataViewer.map.setTarget(undefined);
            }
            DataViewer.initConfig();
            if($scope.values.selectedContext !== null){
                var minX,minY,maxX,maxY;
                minX = $scope.values.selectedContext.west;
                minY = $scope.values.selectedContext.south;
                maxX = $scope.values.selectedContext.east;
                maxY = $scope.values.selectedContext.north;
                var crsCode = $scope.values.selectedContext.crs;
                DataViewer.projection = crsCode;
                DataViewer.addBackground= crsCode==='EPSG:3857';
                if(crsCode === 'EPSG:4326' || crsCode === 'CRS:84') {
                    DataViewer.extent=[-180, -90, 180, 90];
                }
                var cstlUrl = $cookieStore.get('cstlUrl');
                if($scope.values.selectedContext.layers && $scope.values.selectedContext.layers.length>0){
                    var layersToView = [];
                    for (var i=0; i<$scope.values.selectedContext.layers.length; i++) {
                        var layer = $scope.values.selectedContext.layers[i];
                        if (layer.visible) {
                            var layerData;
                            if(layer.iswms){
                                if (layer.externalServiceUrl) {//external wms layer
                                    layerData = (layer.externalStyle) ?
                                        DataViewer.createLayerExternalWMSWithStyle(layer.externalServiceUrl, layer.externalLayer, layer.externalStyle.split(',')[0]) :
                                        DataViewer.createLayerExternalWMS(layer.externalServiceUrl, layer.externalLayer);
                                } else {//internal wms layer
                                    var versions = [];
                                    if(layer.serviceVersions){
                                        var arry = layer.serviceVersions.split('µ');
                                        versions.push(arry[arry.length-1]);
                                    }
                                    layerData = (layer.styleName) ?
                                        DataViewer.createLayerWMSWithStyle(cstlUrl, layer.name, layer.serviceIdentifier, layer.styleName,versions) :
                                        DataViewer.createLayerWMS(cstlUrl, layer.name, layer.serviceIdentifier,versions);
                                }
                            }else {
                                var layerName,providerId;
                                if (layer.namespace) {
                                    layerName = '{' + layer.namespace + '}' + layer.name;
                                } else {
                                    layerName = layer.name;
                                }
                                providerId = layer.provider;
                                var type = layer.type?layer.type.toLowerCase():null;
                                if (layer.externalStyle || layer.styleName) {
                                    layerData = DataViewer.createLayerWithStyle(cstlUrl,layer.dataId,layerName,
                                        layer.externalStyle?layer.externalStyle:layer.styleName,null,null,type!=='vector');
                                } else {
                                    layerData = DataViewer.createLayer(cstlUrl,layer.dataId,layerName,null,type!=='vector');
                                }
                            }
                            layerData.setOpacity(layer.opacity / 100);
                            layersToView.push(layerData);
                        }
                    }
                    DataViewer.layers = layersToView;
                    DataViewer.initMap('mapPreviewMapContextForWMTS');
                    var extent = [minX,minY,maxX,maxY];
                    if(crsCode !== 'EPSG:4326' && crsCode !=='CRS:84'){
                        var projection = ol.proj.get(crsCode);
                        extent = ol.proj.transformExtent(extent, projection,'EPSG:4326');
                    }
                    DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),true);
                } else {
                    DataViewer.initMap('mapPreviewMapContextForWMTS');
                    DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
                }
            }else {
                DataViewer.initMap('mapPreviewMapContextForWMTS');
                DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
            }
        };

        /**
         * binding call when clicking on each row item.
         */
        $scope.toggleContextSelection = function(item){
            if (item && $scope.values.selectedContext && $scope.values.selectedContext.id === item.id) {
                $scope.values.selectedContext = null;
            } else {
                $scope.values.selectedContext = item;
            }
            $scope.previewMapContext();
        };

        /**
         * truncate text with JS.
         * Why not use CSS for this?
         *
         * css rule is
         * {
         *  width: 100px
         *  white-space: nowrap
         *  overflow: hidden
         *  text-overflow: ellipsis // This is where the magic happens
         *  }
         *
         * @param text
         * @param length
         * @returns {string}
         */
        $scope.truncate = function(text,length){
            if(text) {
                return (text.length > length) ? text.substr(0, length) + "..." : text;
            }
        };

        $scope.initMapContextWMTS();
    });
