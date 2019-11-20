angular.module('cstl-data-dashboard')
    .controller('DataModalController', DataModalController);

/**
 * This controller is used by webservice-edit.js to open modal to add data into services
 */
function DataModalController($scope, Dashboard, $modalInstance, service, exclude, Growl,Examind,$cookieStore,$filter) {
    /**
     * To fix angular bug caused by nested scope issue in modal.
     */
    $scope.wrap = {};

    $scope.wrap.nbbypage = 5;

    $scope.dataSelect = {
        all : false,
        mergeData : false
    };

    $scope.service = service;

    $scope.exclude = exclude;

    $scope.values = {
        listSelect : [],
        selectedSensor : null,
        selectedSensorsChild : null,
        userLayerName : '',
    };

    $scope.dismiss = function() {
        $modalInstance.dismiss('close');
    };

    $scope.close = function() {
        $modalInstance.close();
    };

    $scope.clickFilter = function(ordType){
        $scope.wrap.ordertype = ordType;
        $scope.wrap.orderreverse = !$scope.wrap.orderreverse;
    };

    $scope.initData = function() {
        if ($scope.service.type.toLowerCase() === 'sos' || $scope.service.type.toLowerCase() === 'sts') {
            Examind.sensors.list().then(function(response) {
                var list = response.data.children;
                list = list.map(function(item){
                    if(!item.date && item.createDate) {
                        item.date = item.createDate;
                    }
                    if(!item.name && item.identifier) {
                        item.name = item.identifier;
                    }
                    return item;
                });
                Dashboard($scope, list, false);
            });
        } else {
            Examind.datas.getDataListLight(false).then(function (response) {
                var dataList = response.data;
                if ($scope.service.type.toLowerCase() === 'wcs') {
                    dataList = dataList.map(function(item){
                        if (item.type.toLowerCase() === 'coverage') {
                            return item;
                        }
                    });
                } else if ($scope.service.type.toLowerCase() === 'wfs') {
                    dataList = dataList.map(function(item){
                        if (item.type.toLowerCase() === 'vector' || item.type.toLowerCase() === 'sensor') {
                            return item;
                        }
                    });
                }
                Dashboard($scope, dataList, false);
            });
            setTimeout(function(){
                $scope.previewData();
            },300);
        }
    };

    $scope.previewData = function() {
        //clear the map
        if (DataViewer.map) {
            DataViewer.map.setTarget(undefined);
        }
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
                    layerData = DataViewer.createLayerWithStyle($cookieStore.get('cstlUrl'),dataItem.id,layerName,
                        dataItem.targetStyle[0].name,null,null,type!=='vector');
                } else {
                    layerData = DataViewer.createLayer($cookieStore.get('cstlUrl'), dataItem.id,layerName, null,type!=='vector');
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
                    DataViewer.initMap('dataChooseMapPreview');
                    if(response.data && response.data.boundingBox) {
                        var bbox = response.data.boundingBox;
                        var extent = [bbox[0],bbox[1],bbox[2],bbox[3]];
                        DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                    }
                }, function() {//on error
                    // failed to calculate an extent, just load the full map
                    DataViewer.initMap('dataChooseMapPreview');
                }
            );
        }else {
            DataViewer.initMap('dataChooseMapPreview');
            DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
        }
    };

    $scope.toggleSelectSensor = function(item) {
        if (item && $scope.values.selectedSensor && $scope.values.selectedSensor.id === item.id) {
            $scope.values.selectedSensor = null;
        } else {
            $scope.values.selectedSensor = item;
        }
    };

    $scope.selectSensorsChild = function(item) {
        if (item && $scope.values.selectedSensorsChild && $scope.values.selectedSensorsChild.id === item.id) {
            $scope.values.selectedSensorsChild = null;
        } else {
            $scope.values.selectedSensorsChild = item;
        }
    };

    /**
     * Proceed to select all items of modal dashboard
     * depending on the property of checkbox selectAll.
     */
    $scope.selectAllData = function() {
        var array = $filter('filter')($scope.wrap.fullList, {'type':$scope.wrap.filtertype, '$': $scope.wrap.filtertext},$scope.wrap.matchExactly);
        $scope.values.listSelect = ($scope.dataSelect.all) ? array.slice(0) : [];
        if ($scope.service.type.toLowerCase() !== 'sos' && $scope.service.type.toLowerCase() !== 'sts') {
            $scope.previewData();
        }
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
        if ($scope.service.type.toLowerCase() !== 'sos' && $scope.service.type.toLowerCase() !== 'sts') {
            $scope.previewData();
        }

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
     * function to add data to service
     */
    $scope.choose = function() {
        if ($scope.service.type.toLowerCase() === 'sos' || $scope.service.type.toLowerCase() === 'sts') {
            if (!$scope.values.selectedSensor) {
                Growl('warning', 'Warning', 'No data selected');
                return;
            }
            var sensorId = ($scope.values.selectedSensorsChild) ? $scope.values.selectedSensorsChild.identifier : $scope.values.selectedSensor.identifier;
            Examind.sensorServices.importSensor($scope.service.id, sensorId).then(
                function () {//success
                    Growl('success', 'Success', 'Sensor ' + sensorId + ' imported in service ' + $scope.service.name+' successfully!');
                    $scope.close();
                }, function () {
                    Growl('error', 'Error', 'Unable to import sensor ' + sensorId + ' in service ' + $scope.service.name);
                    $scope.dismiss();
                }
            );
        }else {
            if ($scope.values.listSelect.length === 0) {
                Growl('warning', 'Warning', 'No data selected!');
                return;
            }
            if($scope.service.type === 'wms' && $scope.dataSelect.mergeData) {
                var crs = "EPSG:3857";
                var dataIds = $scope.values.listSelect.map(function (data) {
                    return data.id;
                });
                if(!$scope.values.userLayerName) {
                    $scope.values.userLayerName = $scope.values.listSelect[0].name;
                }
                Examind.datas.pyramidData(crs, $scope.values.userLayerName, dataIds).then(
                    function(response){//success
                        response = response.data;
                        if(response.dataId && response.providerId) {
                            Examind.map.addLayer($scope.service.type, $scope.service.identifier,
                                {layerAlias: response.dataId,
                                    layerId: response.dataId,
                                    serviceType: $scope.service.type,
                                    serviceId: $scope.service.identifier,
                                    providerId: response.providerId}).then(
                                function () {//success
                                    Growl('success','Success','Layer successfully added to service '+$scope.service.name);
                                    $scope.close();
                                },
                                function () {
                                    Growl('error','Error','Layer failed to be added to service '+$scope.service.name);
                                    $scope.dismiss();
                                }
                            );
                        }
                    },function(response){//error
                        Growl('error', 'Error', 'Failed to generate pyramid data');
                    }
                );
            } else {
                //using angular.forEach to avoid jsHint warning when declaring function in loop
                angular.forEach($scope.values.listSelect, function(value, key){
                    var providerId = value.provider;
                    if($scope.service.type.toLowerCase() === 'wms'){
                        providerId = value.pyramidConformProviderId?value.pyramidConformProviderId:value.provider;
                    }
                    Examind.map.addLayer($scope.service.type, $scope.service.identifier,
                        {layerAlias: value.name,
                            layerId: value.name,
                            serviceType: $scope.service.type,
                            serviceId: $scope.service.identifier,
                            providerId: providerId,
                            layerNamespace: value.namespace}).then(
                        function(response) {//on success
                            Growl('success', 'Success', response.data.message);
                            $scope.close();
                        },
                        function(response) {//on error
                            Growl('error', 'Error', response.data.message);
                            $scope.dismiss();
                        }
                    );
                });
            }
        }
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

    $scope.initData();
}

