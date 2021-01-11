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

angular.module('cstl-mapcontext-edit', ['cstl-restapi', 'cstl-services', 'pascalprecht.translate', 'ui.bootstrap.modal', 'examind-instance'])

    .controller('MapContextModalController', function($scope, $modalInstance, Growl,
                                                      ctxtToEdit, layersForCtxt, Examind) {
        // item to save in the end
        $scope.ctxt = ctxtToEdit || {"crs":'EPSG:3857',"west":-35848354.76952138,
            "south":-35848354.76952139,
            "east":35848354.76952138,
            "north":35848354.76952136};
        // defines if we are in adding or edition mode
        $scope.addMode = !ctxtToEdit;

        Examind.crs.listAll()
            .then(function (response) {
                $scope.crsList = response.data;
            }, function (error) {
                console.error(error);
            });

        $scope.tag = {
            text: '',
            keywords: (ctxtToEdit && ctxtToEdit.keywords) ? ctxtToEdit.keywords.split(','): []
        };

        // handle display mode for this modal popup
        $scope.mode = {
            selTab: 'tabInfo',
            display: 'general',
            dispWmsLayers: false,
            errorNoGivenName: false
        };

        $scope.layers = {
            toAdd: layersForCtxt || [], // Stores temp layers, selected to be added at the saving time
            toSend: [], // List of layers really sent
            toStyle: null // Layer on which to apply the selected style
        };

        $scope.external = {
            serviceUrl: null
        };

        $scope.selection = {
            layer:null,
            service:null,
            item:null,
            extLayer:null,
            internalData:[]
        };

        $scope.styles = {
            existing: [],
            selected: null
        };

        $scope.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        $scope.close = function () {
            $modalInstance.close();
        };


        $scope.initScopeMapContextEditor = function() {
            setTimeout(function(){
                $scope.viewMap(true,null);
            },500);
        };

        $scope.goToInternalSource = function() {
            $scope.mode.source='interne';
            $scope.mode.display='chooseLayer';
        };

        $scope.selectLayer = function(layer,service) {
            $scope.selection.extLayer = null; //reset selection for extLayer
            if (layer && $scope.selection.layer && $scope.selection.layer.id === layer.id) {
                $scope.selection.layer = null;
                $scope.selection.service = null;
            } else {
                $scope.selection.layer = layer;
                $scope.selection.service = service;
            }
        };

        $scope.selectItem = function(item) {
            if ($scope.selection.item === item) {
                $scope.selection.item = null;
            } else {
                $scope.selection.item = item;
            }
        };

        $scope.selectExtLayer = function(extLayer) {
            //reset selection for wms cstl layer
            $scope.selection.layer = null;
            $scope.selection.service = null;
            if (extLayer && $scope.selection.extLayer && $scope.selection.extLayer.name === extLayer.name) {
                $scope.selection.extLayer = null;
            } else {
                $scope.selection.extLayer = extLayer;
            }
        };

        $scope.selectStyle = function(item) {
            if (item && $scope.styles.selected && $scope.styles.selected.name === item.name) {
                $scope.styles.selected = null;
            } else {
                $scope.styles.selected = item;
            }
            fillLayersToSend(null);
        };

        $scope.showMapWithStyle = function(styleObj) {
            $scope.viewMap(false,{"layer":$scope.layers.toStyle,"style":styleObj});
        };

        $scope.addTag = function() {
            if (!$scope.tag.text || $scope.tag.text === '' || $scope.tag.text.length === 0) {
                return;
            }
            $scope.tag.keywords.push($scope.tag.text);
            $scope.tag.text = '';
        };

        $scope.deleteTag = function(key) {
            if ($scope.tag.keywords.length > 0 &&
                $scope.tag.text.length === 0 &&
                !key) {
                $scope.tag.keywords.pop();
            } else if (key) {
                $scope.tag.keywords.splice(key, 1);
            }
        };

        $scope.updateNamePresent = function() {
            $scope.mode.errorNoGivenName = (!$scope.ctxt.name || $scope.ctxt.name === null);
        };

        $scope.validate = function () {
            // Verify on which step the user is.
            if ($scope.mode.display==='general') {
                if (!$scope.ctxt.name) {
                    $scope.mode.errorNoGivenName = true;
                    Growl('warning', 'Warning', 'You must specify a name');
                    return;
                }
                if ($scope.tag.keywords) {
                    var str = '';
                    for (var i = 0; i < $scope.tag.keywords.length; i++) {
                        if (i > 0) {
                            str += ',';
                        }
                        str += $scope.tag.keywords[i];
                    }
                    $scope.ctxt.keywords = str;
                }
                // On the general panel, it means saving the whole context
                if ($scope.addMode) {
                    fillLayersToSend($scope.ctxt);
                    $scope.ctxt.layers = $scope.layers.toSend;
                    Examind.mapcontexts.createMapContext($scope.ctxt).then(
                        function () {
                            Growl('success', 'Success', 'Map context created with success');
                            $scope.close();
                        },
                        function () {
                            Growl('error', 'Error', 'Unable to create map context');
                        });
                } else {
                    fillLayersToSend($scope.ctxt);
                    $scope.ctxt.layers = $scope.layers.toSend;
                    Examind.mapcontexts.updateMapContext($scope.ctxt).then(
                        function () {
                            Growl('success', 'Success', 'Map context successfully updated');
                            $scope.close();
                        }, function () {
                            Growl('error', 'Error', 'Unable to update map context');
                        });
                }
            } else if ($scope.mode.display==='chooseLayer') {
                // Add the selected layer to the current map context
                if ($scope.selection.extLayer) {
                    // External WMS layer
                    var llExtent = '';
                    if ($scope.selection.extLayer.boundingBox) {
                        var bbox = $scope.selection.extLayer.boundingBox;
                        llExtent = bbox.minx +','+ bbox.miny +','+ bbox.maxx +','+ bbox.maxy;
                    }

                    var extStyle = '';
                    if ($scope.selection.extLayer.styles) {
                        for (var j=0; j < $scope.selection.extLayer.styles.length; j++) {
                            if (j > 0) {
                                extStyle += ',';
                            }
                            var capsStyle = $scope.selection.extLayer.styles[j];
                            extStyle += capsStyle.name;
                        }
                    }
                    var layerExt = {
                        externalLayer: $scope.selection.extLayer.name,
                        externalLayerExtent: llExtent,
                        externalServiceUrl: $scope.external.serviceUrl,
                        externalServiceVersion: $scope.selection.extLayer.version,
                        externalStyle: extStyle
                    };
                    var styleExtArray = $scope.selection.extLayer.styles;
                    var layerExtToAdd = {
                        layer: layerExt,
                        visible: true,
                        isWms:true,
                        opacity: 100,
                        styleObj:styleExtArray && styleExtArray.length>0?styleExtArray[0]:null
                    };
                    $scope.layers.toAdd.push(layerExtToAdd);
                } else if ($scope.selection.layer) {
                    // Internal WMS layer
                    var styleArray = $scope.selection.layer.targetStyle;
                    var layerToAdd = {
                        layer: $scope.selection.layer,
                        service: $scope.selection.service,
                        visible: true,
                        isWms:true,
                        opacity:100,
                        styleObj:styleArray && styleArray.length>0?styleArray[0]:null
                    };
                    layerToAdd.layer.layerId = $scope.selection.layer.id;
                    $scope.layers.toAdd.push(layerToAdd);
                }else if ($scope.selection.internalData.length > 0) {
                    // Internal Data layer
                    angular.forEach($scope.selection.internalData, function(data){
                        var styleArr = data.targetStyle;
                        var layerObjToAdd = {
                            layer: data,
                            visible: true,
                            isWms:false,
                            opacity:100,
                            styleObj:styleArr && styleArr.length>0?styleArr[0]:null
                        };
                        layerObjToAdd.layer.dataId = data.id;
                        $scope.layers.toAdd.push(layerObjToAdd);
                    });
                }
                fillLayersToSend(null);
                $scope.selection = {};
                // Go back to first screen
                $scope.mode.display = 'general';
                setTimeout(function(){
                    $scope.viewMap(false,null);
                },200);

            } else if ($scope.mode.display==='addChooseStyle') {
                $scope.layers.toStyle.styleObj=$scope.styles.selected;
                if ($scope.layers.toStyle.layer.externalServiceUrl && $scope.layers.toStyle.layer.externalStyle) {
                    // It's an external WMS style, put the one chosen in first, as the default one
                    var possibleStyles = $scope.layers.toStyle.layer.externalStyle.split(',');
                    if (possibleStyles[0] !== $scope.styles.selected.name) {
                        var indexForStyle;
                        for (var k=0; k<possibleStyles.length; k++) {
                            var s = possibleStyles[k];
                            if (s === $scope.styles.selected.name) {
                                indexForStyle = k;
                            }
                        }
                        if (indexForStyle) {
                            // Remove it from its old place
                            possibleStyles.splice(indexForStyle, 1);
                            // Put it in first
                            possibleStyles.splice(0, 0, $scope.styles.selected.name);
                        }
                        var finalStyles = '';
                        for (var l=0; l<possibleStyles.length; l++) {
                            if (l > 0) {
                                finalStyles += ',';
                            }
                            finalStyles += possibleStyles[l];
                        }
                        $scope.layers.toStyle.layer.externalStyle = finalStyles;
                    }
                }
                $scope.mode.display = 'general';
                setTimeout(function(){
                    $scope.viewMap(false,null);
                },200);
            }
        };

        $scope.cancel = function() {
            if ($scope.mode.display==='general') {
                $scope.dismiss();
            } else {
                $scope.mode.display = 'general';
                fillLayersToSend(null);
                setTimeout(function(){
                    $scope.viewMap(false,null);
                },200);
            }
        };


        function fillLayersToSend(ctxt) {
            $scope.layers.toSend = [];
            for (var i = 0; i < $scope.layers.toAdd.length; i++) {
                var layObj = $scope.layers.toAdd[i];
                var externalStyle;
                if(layObj.layer.externalServiceUrl){
                    externalStyle = (layObj.styleObj) ? layObj.styleObj.name : (layObj.layer.externalStyle)?layObj.layer.externalStyle:null;
                }
                $scope.layers.toSend.push({
                    mapcontextId: (ctxt) ? ctxt.id : null,
                    layerId: (layObj.isWms)? (layObj.layer.layerId) ? layObj.layer.layerId : null : null,
                    dataId: layObj.layer.dataId ? layObj.layer.dataId : null,
                    iswms: layObj.isWms,
                    styleId: (layObj.styleObj && layObj.styleObj.id) ? layObj.styleObj.id : null,
                    order: i,
                    opacity: layObj.opacity,
                    visible: layObj.visible,
                    externalServiceUrl: layObj.layer.externalServiceUrl,
                    externalServiceVersion: layObj.layer.externalServiceVersion,
                    externalLayer: layObj.layer.externalLayer,
                    externalLayerExtent: layObj.layer.externalLayerExtent,
                    externalStyle: externalStyle
                });
            }
        }

        $scope.goToAddLayerToContext = function() {
            $scope.mode.display = 'addChooseSource';
            $scope.selection = {};
        };

        $scope.toggleUpDownExtSelected = function() {
            var $header = $('#selectionExtLayer').find('.selected-item').find('.block-header');
            $header.next().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        $scope.orderUp = function(i) {
            var skipBg = (DataViewer.addBackground)?i+1:i;
            if (i > 0) {
                var previous = $scope.layers.toAdd[i - 1];
                $scope.layers.toAdd[i - 1] = $scope.layers.toAdd[i];
                $scope.layers.toAdd[i] = previous;
                // Now switch layer order for the map
                var item0 = DataViewer.map.getLayers().getArray()[skipBg-1];
                var item1 = DataViewer.map.getLayers().getArray()[skipBg];
                DataViewer.map.getLayers().getArray()[skipBg-1]=item1;
                DataViewer.map.getLayers().getArray()[skipBg]=item0;
                DataViewer.map.render();
            }
        };
        $scope.orderDown = function(i) {
            var skipBg = (DataViewer.addBackground)?i+1:i;
            if (i < $scope.layers.toAdd.length - 1) {
                var next = $scope.layers.toAdd[i + 1];
                $scope.layers.toAdd[i + 1] = $scope.layers.toAdd[i];
                $scope.layers.toAdd[i] = next;
                // Now switch layer order for the map
                var item0 = DataViewer.map.getLayers().getArray()[skipBg];
                var item1 = DataViewer.map.getLayers().getArray()[skipBg+1];
                DataViewer.map.getLayers().getArray()[skipBg]=item1;
                DataViewer.map.getLayers().getArray()[skipBg+1]=item0;
                DataViewer.map.render();
            }
        };

        $scope.goToStyleMapItem = function(item) {
            $scope.styles.selected = null;
            $scope.styles.existing = [];
            if (item.layer.externalLayer) {
                var styleItems = [];
                if (item.layer.externalStyle) {
                    var styleNames = item.layer.externalStyle.split(',');
                    for (var i = 0; i < styleNames.length; i++) {
                        styleItems.push({name: styleNames[i]});
                    }
                }
                $scope.styles.existing = styleItems;
                if($scope.styles.existing && $scope.styles.existing.length>0){
                    for(var s=0;s<$scope.styles.existing.length;s++) {
                        var candidat = $scope.styles.existing[s];
                        if(item.styleObj){
                            if(item.styleObj.name && candidat.name === item.styleObj.name){
                                $scope.styles.selected = candidat;
                                break;
                            }
                        } else if(item.layer.styleName && candidat.name === item.layer.styleName){
                            $scope.styles.selected = candidat;
                            break;
                        }else if(item.layer.externalStyle && candidat.name === item.layer.externalStyle.split(',')[0]){
                            $scope.styles.selected = candidat;
                            break;
                        }
                    }
                }
            } else {
                // for Internal data layer and for internal wms layer
                Examind.styles.getStyles().then(
                    function (response) {
                        if(response.data) {
                            for (var j = 0; j < item.layer.targetStyle.length; j++) {
                                var tgStyle = item.layer.targetStyle[j];
                                for (var i = 0; i < response.data.length; i++) {
                                    var style = response.data[i];
                                    if ((style.id === tgStyle.id && angular.isDefined(style.id) && angular.isDefined(tgStyle.id)) ||
                                        (style.name === tgStyle.name && style.provider === tgStyle.provider)) {
                                        $scope.styles.existing.push(style);
                                        break;
                                    }
                                }
                            }
                            if($scope.styles.existing && $scope.styles.existing.length>0){
                                for(var s=0;s<$scope.styles.existing.length;s++) {
                                    var candidat = $scope.styles.existing[s];
                                    if(item.styleObj){
                                        if(item.styleObj.name && candidat.name === item.styleObj.name) {
                                            $scope.styles.selected = candidat;
                                            break;
                                        }
                                    } else if(item.layer.styleName && candidat.name === item.layer.styleName){
                                        $scope.styles.selected = candidat;
                                        break;
                                    }else if(item.layer.externalStyle && candidat.name === item.layer.externalStyle){
                                        $scope.styles.selected = candidat;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                );
            }
            $scope.layers.toStyle = item;
            $scope.mode.display = 'addChooseStyle';
        };

        $scope.deleteMapItem = function(item) {
            var index = $scope.layers.toAdd.indexOf(item);
            if (index !== -1) {
                $scope.layers.toAdd.splice(index, 1);
            }
            for (var i=0; i<DataViewer.layers.length; i++) {
                var candidat = DataViewer.layers[i];
                var candidatName = candidat.get('name');
                if(item.isWms) {
                    if ((item.layer.name && candidatName === item.layer.name) ||
                        (item.layer.externalLayer && candidatName === item.layer.externalLayer)) {
                        DataViewer.map.removeLayer(candidat);
                        return;
                    }
                }else {
                    var layerName;
                    if (item.layer.namespace) {
                        layerName = '{' + item.layer.namespace + '}' + item.layer.name;
                    } else {
                        layerName = item.layer.name;
                    }
                    if (layerName && candidatName === layerName) {
                        DataViewer.map.removeLayer(candidat);
                        return;
                    }
                }
            }
        };

        $scope.changeVisibility = function(item) {
            item.visible=!(item.visible);
            for (var i=0; i<DataViewer.layers.length; i++) {
                var candidat = DataViewer.layers[i];
                var candidatName = candidat.get('name');
                if(item.isWms) {
                    if ((item.layer.name && candidatName === item.layer.name) ||
                        (item.layer.externalLayer && candidatName === item.layer.externalLayer)) {
                        candidat.setVisible(item.visible);
                        return;
                    }
                }else {
                    var layerName;
                    if (item.layer.namespace) {
                        layerName = '{' + item.layer.namespace + '}' + item.layer.name;
                    } else {
                        layerName = item.layer.name;
                    }
                    if (layerName && candidatName === layerName) {
                        candidat.setVisible(item.visible);
                        return;
                    }
                }
            }
        };

        $scope.updateOpacity = function(item) {
            for (var i=0; i<DataViewer.layers.length; i++) {
                var candidat = DataViewer.layers[i];
                var candidatName = candidat.get('name');
                if(item.isWms) {
                    if ((item.layer.name && candidatName === item.layer.name) ||
                        (item.layer.externalLayer && candidatName === item.layer.externalLayer)) {
                        candidat.setOpacity(item.opacity / 100);
                        return;
                    }
                }else {
                    var layerName;
                    if (item.layer.namespace) {
                        layerName = '{' + item.layer.namespace + '}' + item.layer.name;
                    } else {
                        layerName = item.layer.name;
                    }
                    if (layerName && candidatName === layerName) {
                        candidat.setOpacity(item.opacity / 100);
                        return;
                    }
                }
            }
        };

        $scope.viewMap = function(zoomOnMapContextExtent,layerStyleObj) {
            DataViewer.initConfig();
            if ($scope.layers.toAdd && $scope.layers.toAdd.length>0) {
                var cstlUrl = window.localStorage.getItem('cstlUrl');
                var layersToView = [];
                for (var i=0; i<$scope.layers.toAdd.length; i++) {
                    var layObj = $scope.layers.toAdd[i];
                    if (layObj.visible) {
                        var layerData;
                        if (layObj.isWms) {//external wms layer
                            if(layObj.layer.externalServiceUrl) {
                                if(layerStyleObj && layerStyleObj.layer.layer.name === layObj.layer.name){
                                    if(layerStyleObj.style && layerStyleObj.style.name){
                                        layerData = DataViewer.createLayerExternalWMSWithStyle(layObj.layer.externalServiceUrl,
                                            layObj.layer.externalLayer,
                                            layerStyleObj.style.name);
                                    }else {
                                        layerData = DataViewer.createLayerExternalWMS(layObj.layer.externalServiceUrl, layObj.layer.externalLayer);
                                    }
                                }else {
                                    if(layObj.styleObj || layObj.layer.externalStyle){
                                        layerData = DataViewer.createLayerExternalWMSWithStyle(layObj.layer.externalServiceUrl,
                                            layObj.layer.externalLayer,
                                            layObj.styleObj?layObj.styleObj.name:layObj.layer.externalStyle.split(',')[0]);
                                    }else {
                                        layerData = DataViewer.createLayerExternalWMS(layObj.layer.externalServiceUrl, layObj.layer.externalLayer);
                                    }
                                }
                            }else {//internal wms layer
                                var serviceName = (layObj.layer.serviceIdentifier) ? layObj.layer.serviceIdentifier : layObj.service.identifier;
                                var versions=[];
                                var arry;
                                if(layObj.service && layObj.service.versions) {
                                    arry = layObj.service.versions.split('µ');
                                    versions.push(arry[arry.length-1]);
                                }else if(layObj.layer.serviceVersions) {
                                    arry = layObj.layer.serviceVersions.split('µ');
                                    versions.push(arry[arry.length-1]);
                                }

                                if(layerStyleObj && layerStyleObj.layer.layer.name === layObj.layer.name){
                                    if(layerStyleObj.style && layerStyleObj.style.name){
                                        layerData = DataViewer.createLayerWMSWithStyle(cstlUrl, layObj.layer.name, serviceName,
                                            layerStyleObj.style.name,versions);
                                    }else {
                                        layerData = DataViewer.createLayerWMS(cstlUrl, layObj.layer.name, serviceName,versions);
                                    }
                                }else {
                                    if(layObj.styleObj || layObj.layer.externalStyle){
                                        layerData = DataViewer.createLayerWMSWithStyle(cstlUrl, layObj.layer.name, serviceName,
                                            layObj.styleObj?layObj.styleObj.name:layObj.layer.externalStyle.split(',')[0],versions);
                                    }else {
                                        layerData = DataViewer.createLayerWMS(cstlUrl, layObj.layer.name, serviceName,versions);
                                    }
                                }
                            }
                        } else {//internal data layer
                            var layerName,providerId;
                            var dataItem = layObj.layer;
                            if (dataItem.namespace) {
                                layerName = '{' + dataItem.namespace + '}' + dataItem.name;
                            } else {
                                layerName = dataItem.name;
                            }
                            providerId = dataItem.provider;
                            var type = dataItem.type?dataItem.type.toLowerCase():null;
                            if(layerStyleObj && layerStyleObj.layer.layer.name === dataItem.name){
                                if(layerStyleObj.style && layerStyleObj.style.name){
                                    layerData = DataViewer.createLayerWithStyle(cstlUrl,dataItem.dataId,layerName,
                                        layerStyleObj.style.name,null,null,type!=='vector');
                                }else {
                                    layerData = DataViewer.createLayer(cstlUrl, dataItem.dataId,layerName,null,type!=='vector');
                                }
                            }else {
                                if (layObj.styleObj || dataItem.styleName) {
                                    layerData = DataViewer.createLayerWithStyle(cstlUrl,dataItem.dataId,layerName,
                                        layObj.styleObj?layObj.styleObj.name:dataItem.styleName,null,null,type!=='vector');
                                } else {
                                    layerData = DataViewer.createLayer(cstlUrl,dataItem.dataId,layerName,null,type!=='vector');
                                }
                            }
                        }
                        layerData.setOpacity(layObj.opacity / 100);
                        layersToView.push(layerData);
                    }
                }
                DataViewer.layers = layersToView;
            }
            if($scope.ctxt && $scope.ctxt.crs){
                var crsCode = $scope.ctxt.crs;
                DataViewer.projection = crsCode;
                DataViewer.addBackground= crsCode==='EPSG:3857';
                if(crsCode === 'EPSG:4326' || crsCode === 'CRS:84') {
                    DataViewer.extent=[-180, -90, 180, 90];
                }
            }
            DataViewer.initMap('mapContextMap');
            if (zoomOnMapContextExtent) {
                if($scope.ctxt && $scope.ctxt.west && $scope.ctxt.south && $scope.ctxt.east && $scope.ctxt.north && $scope.ctxt.crs) {
                    var extent = [$scope.ctxt.west, $scope.ctxt.south, $scope.ctxt.east, $scope.ctxt.north];
                    DataViewer.map.updateSize();
                    //because zoomToExtent take extent in EPSG:4326 we need to reproject the zoom extent
                    if($scope.ctxt.crs !== 'EPSG:4326' && $scope.ctxt.crs !=='CRS:84'){
                        var projection = ol.proj.get($scope.ctxt.crs);
                        extent = ol.proj.transformExtent(extent, projection,'EPSG:4326');
                    }
                    DataViewer.zoomToExtent(extent, DataViewer.map.getSize(),true);
                }
            } else {
                if($scope.layers.toSend.length===0){
                    fillLayersToSend(null);
                } else {
                    Examind.mapcontexts.getMapLayersExtent($scope.layers.toSend).then(
                        function (response) {
                            useExtentForLayers(response.data.values);
                        });
                }
            }
        };

        function useExtentForLayers(values) {
            var west = Number(values.west);
            var south = Number(values.south);
            var east = Number(values.east);
            var north = Number(values.north);
            var extent = [west,south,east,north];
            DataViewer.map.updateSize();
            DataViewer.zoomToExtent(extent, DataViewer.map.getSize(),true);
            $scope.applyExtent();
        }

        $scope.applyExtent = function() {
            var extent = DataViewer.map.getView().calculateExtent(DataViewer.map.getSize());
            var crsCode = DataViewer.map.getView().getProjection().getCode();
            if(crsCode) {
                $scope.ctxt.crs = crsCode;
            }
            $scope.ctxt.west = extent[0];
            $scope.ctxt.south = extent[1];
            $scope.ctxt.east = extent[2];
            $scope.ctxt.north = extent[3];
        };

        $scope.zoomToLayerExtent = function(item) {
            if(item.isWms) {
                if(item.layer.externalLayerExtent){
                    var extentArr = item.layer.externalLayerExtent.split(',');
                    if(extentArr.length===4){
                        var extent = [Number(extentArr[0]),Number(extentArr[1]),Number(extentArr[2]),Number(extentArr[3])];
                        DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                    }
                }else if(item.layer.dataId) {

                    Examind.datas.getGeographicExtent(item.layer.dataId).then(
                        function(response){
                            var bbox = response.data.boundingBox;
                            if (bbox) {
                                var extent = [bbox[0],bbox[1],bbox[2],bbox[3]];
                                DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                            }
                        });
                }
            } else {
                Examind.datas.getGeographicExtent(item.layer.dataId).then(
                    function(response){
                        var bbox = response.data.boundingBox;
                        if (bbox) {
                            var extent = [bbox[0],bbox[1],bbox[2],bbox[3]];
                            DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                        }
                    });
            }
        };

        $scope.truncate = function(text,length){
            if(text) {
                return (text.length > length) ? text.substr(0, length) + "..." : text;
            }
        };

        $scope.initScopeMapContextEditor();
    })
    .controller('InternalSourceMapContextController', function($scope,$filter, Dashboard, Examind) {
        $scope.wrap = {};
        $scope.wrap.nbbypage = 5;
        $scope.dataSelect = { all : false };
        $scope.searchVisible=false;

        $scope.clickFilter = function(ordType){
            $scope.wrap.ordertype = ordType;
            $scope.wrap.orderreverse = !$scope.wrap.orderreverse;
        };

        /**
         * Proceed to select all items of modal dashboard
         * depending on the property of checkbox selectAll.
         */
        $scope.selectAllData = function() {
            var array = $filter('filter')($scope.wrap.fullList, {'type':$scope.wrap.filtertype, '$': $scope.wrap.filtertext},$scope.wrap.matchExactly);
            $scope.selection.internalData = ($scope.dataSelect.all) ? array.slice(0) : [];
            $scope.previewData();
        };

        /**
         * binding call when clicking on each row item.
         */
        $scope.toggleDataInArray = function(item){
            var itemExists = false;
            for (var i = 0; i < $scope.selection.internalData.length; i++) {
                if ($scope.selection.internalData[i].id === item.id) {
                    itemExists = true;
                    $scope.selection.internalData.splice(i, 1);//remove item
                    break;
                }
            }
            if(!itemExists){
                $scope.selection.internalData.push(item);
            }
            $scope.dataSelect.all=($scope.selection.internalData.length === $scope.wrap.fullList.length);
            $scope.previewData();
        };

        /**
         * Returns true if item is in the selected items list.
         * binding function for css purposes.
         * @param item
         * @returns {boolean}
         */
        $scope.isInSelected = function(item){
            for(var i=0; i < $scope.selection.internalData.length; i++){
                if($scope.selection.internalData[i].id === item.id){
                    return true;
                }
            }
            return false;
        };

        $scope.initInternalSourceMapContext = function() {
            Examind.datas.getDataListLight(false).then(function (response) {
                Dashboard($scope, response.data, true);
            });
            $scope.selection.internalData = [];
            setTimeout(function(){
                $scope.previewData();
            },200);
        };

        $scope.previewData = function() {
            //clear the map
            if (DataViewer.map) {
                DataViewer.map.setTarget(undefined);
            }
            DataViewer.initConfig();
            if ($scope.selection.internalData.length >0) {

                var cstlUrl = window.localStorage.getItem('cstlUrl');
                var layerName;
                for(var i=0;i<$scope.selection.internalData.length;i++){
                    var dataItem = $scope.selection.internalData[i];
                    if (dataItem.namespace) {
                        layerName = '{' + dataItem.namespace + '}' + dataItem.name;
                    } else {
                        layerName = dataItem.name;
                    }

                    var type = dataItem.type?dataItem.type.toLowerCase():null;
                    var layerData;
                    if (dataItem.targetStyle && dataItem.targetStyle.length > 0) {
                        layerData = DataViewer.createLayerWithStyle(cstlUrl,dataItem.id,layerName,
                            dataItem.targetStyle[0].name,null,null,type!=='vector');
                    } else {
                        layerData = DataViewer.createLayer(cstlUrl, dataItem.id,layerName, null,type!=='vector');
                    }
                    //to force the browser cache reloading styled layer.
                    layerData.get('params').ts=new Date().getTime();
                    DataViewer.layers.push(layerData);
                }

                var dataIds = $scope.selection.internalData.map(function (value) {
                    return value.id;
                });
                Examind.datas.mergedDataExtent(dataIds).then(
                    function(response) {// on success
                        DataViewer.initMap('internalDataSourcePreview');
                        if(response.data && response.data.boundingBox) {
                            var bbox = response.data.boundingBox;
                            var extent = [bbox[0],bbox[1],bbox[2],bbox[3]];
                            DataViewer.zoomToExtent(extent,DataViewer.map.getSize(),false);
                        }
                    }, function() {//on error
                        // failed to calculate an extent, just load the full map
                        DataViewer.initMap('internalDataSourcePreview');
                    }
                );
            }else {
                DataViewer.initMap('internalDataSourcePreview');
                DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
            }
        };

        $scope.setTargetStyle = function(data,index) {
            var tmp = data.targetStyle.splice(index,1);
            data.targetStyle.unshift(tmp[0]);
            $scope.previewData();
        };

        $scope.truncate = function(text,length){
            if(text) {
                return (text.length > length) ? text.substr(0, length) + "..." : text;
            }
        };

        $scope.initInternalSourceMapContext();

    })
    .controller('WMSSourceMapContextController', function($scope, Growl, $translate, Examind) {

        $scope.servicesLayers = [];

        $scope.getCurrentLang = function() {
            return $translate.use();
        };

        $scope.initWmsSourceMapContext = function() {
            Examind.services.listServiceLayers('wms', $scope.getCurrentLang()).then(
                function(response) {
                    $scope.servicesLayers = response.data;
                });
            $scope.selection.layer = null;
            $scope.selection.service = null;
            $scope.selection.extLayer = null;
            setTimeout(function(){
                $scope.previewWMSLayer();
            },200);
        };

        $scope.previewWMSLayer = function() {
            //clear the map
            if (DataViewer.map) {
                DataViewer.map.setTarget(undefined);
            }
            DataViewer.initConfig();
            var layerData;
            if($scope.selection.layer && $scope.selection.service){
                var cstlUrl = window.localStorage.getItem('cstlUrl');
                var serviceName = ($scope.selection.layer.serviceIdentifier) ? $scope.selection.layer.serviceIdentifier : $scope.selection.service.identifier;
                var versions = [];
                if($scope.selection.service.versions){
                    var arry = $scope.selection.service.versions.split('µ');
                    versions.push(arry[arry.length-1]);
                }
                if ($scope.selection.layer.targetStyle && $scope.selection.layer.targetStyle.length > 0) {
                    layerData = DataViewer.createLayerWMSWithStyle(cstlUrl, $scope.selection.layer.name, serviceName, $scope.selection.layer.targetStyle[0].name,versions);
                } else {
                    layerData = DataViewer.createLayerWMS(cstlUrl, $scope.selection.layer.name, serviceName,versions);
                }
                //to force the browser cache reloading styled layer.
                layerData.get('params').ts=new Date().getTime();
                DataViewer.layers.push(layerData);
                DataViewer.initMap('wmsDataSourcePreview');
                var arrayLayer = [];
                arrayLayer.push({
                    mapcontextId:null,
                    layerId: $scope.selection.layer.id,
                    dataId: $scope.selection.layer.dataId,
                    iswms: true,
                    styleId: $scope.selection.layer.styleId,
                    order: 0,
                    opacity: 100,
                    visible: true,
                    externalServiceUrl: null,
                    externalServiceVersion: null,
                    externalLayer: null,
                    externalLayerExtent: null,
                    externalStyle: null
                });
                zoomToLayer(arrayLayer);
            } else if($scope.selection.extLayer) {
                var llExtent = '';
                if ($scope.selection.extLayer.boundingBox) {
                    var bbox = $scope.selection.extLayer.boundingBox;
                    llExtent = bbox.minx +','+ bbox.miny +','+ bbox.maxx +','+ bbox.maxy;
                }
                var extStyle = '';
                if ($scope.selection.extLayer.styles && $scope.selection.extLayer.styles.length>0) {
                    for (var j=0; j < $scope.selection.extLayer.styles.length; j++) {
                        if (j > 0) {
                            extStyle += ',';
                        }
                        var capsStyle = $scope.selection.extLayer.styles[j];
                        extStyle += capsStyle.name;
                    }
                }
                var layerExt = {
                    externalLayer: $scope.selection.extLayer.name,
                    externalLayerExtent: llExtent,
                    externalServiceUrl: $scope.external.serviceUrl,
                    externalServiceVersion: $scope.selection.extLayer.version,
                    externalStyle: extStyle
                };
                if($scope.selection.extLayer.styles && $scope.selection.extLayer.styles.length>0){
                    layerData = DataViewer.createLayerExternalWMSWithStyle(layerExt.externalServiceUrl,
                        layerExt.externalLayer,
                        $scope.selection.extLayer.styles[0].name);
                }else {
                    layerData = DataViewer.createLayerExternalWMS(layerExt.externalServiceUrl,layerExt.externalLayer);
                }
                //to force the browser cache reloading styled layer.
                layerData.get('params').ts=new Date().getTime();
                DataViewer.layers.push(layerData);

                //zoom to layer extent
                DataViewer.initMap('wmsDataSourcePreview');
                var arrayExtLayer = [];
                arrayExtLayer.push({
                    mapcontextId:null,
                    layerId: null,
                    dataId: null,
                    iswms: true,
                    styleId: null,
                    order: 0,
                    opacity: 100,
                    visible: true,
                    externalServiceUrl: layerExt.externalServiceUrl,
                    externalServiceVersion: layerExt.externalServiceVersion,
                    externalLayer: layerExt.externalLayer,
                    externalLayerExtent: layerExt.externalLayerExtent,
                    externalStyle: layerExt.externalStyle
                });
                zoomToLayer(arrayExtLayer);
            } else {
                DataViewer.initMap('wmsDataSourcePreview');
                DataViewer.map.getView().setZoom(DataViewer.map.getView().getZoom()+1);
            }
        };

        $scope.searchAndDisplayWmsLayers = function() {
            if ($scope.external.serviceUrl) {
                // Try in WMS version 1.3.0
                Examind.mapcontexts.getExternalMapLayers($scope.external.serviceUrl, "1.3.0").then(
                    function(response) {//on success
                        $scope.external.layers = response.data;
                        $scope.mode.dispWmsLayers = true;
                    },
                    function() {//on error
                        // If it fails try it in version 1.1.1
                        Examind.mapcontexts.getExternalMapLayers($scope.external.serviceUrl, "1.1.1").then(
                            function(response) {//on success
                                $scope.external.layers = response.data;
                                $scope.mode.dispWmsLayers = true;
                            },
                            function() {//on error
                                Growl('error', 'Error', 'Unable to find layers for this url');
                            }
                        );
                    }
                );
            }
        };

        function zoomToLayer(layerObj) {
            Examind.mapcontexts.getMapLayersExtent(layerObj).then(
                function(response) {//on success
                    var values = response.data.values;
                    var crs = values.crs;
                    var west = Number(values.west);
                    var south = Number(values.south);
                    var east = Number(values.east);
                    var north = Number(values.north);
                    var extent = [west,south,east,north];
                    DataViewer.map.updateSize();
                    //because zoomToExtent take extent in EPSG:4326 we need to reproject the zoom extent
                    if(crs !== 'EPSG:4326' && crs !=='CRS:84'){
                        var projection = ol.proj.get(crs);
                        extent = ol.proj.transformExtent(extent, projection,'EPSG:4326');
                    }
                    DataViewer.zoomToExtent(extent, DataViewer.map.getSize(),true);
                }
            );
        }

        $scope.setTargetStyle = function(layer,index) {
            var tmp = layer.targetStyle.splice(index,1);
            layer.targetStyle.unshift(tmp[0]);
            $scope.previewWMSLayer();
        };

        $scope.setExtTargetStyle = function(extLayer,index) {
            var tmp = extLayer.styles.splice(index,1);
            extLayer.styles.unshift(tmp[0]);
            $scope.previewWMSLayer();
        };

        $scope.truncate = function(text,length){
            if(text) {
                return (text.length > length) ? text.substr(0, length) + "..." : text;
            }
        };

        $scope.isSelectedExtLayer = function (extLayer) {
            return $scope.selection.extLayer && $scope.selection.extLayer.name === extLayer.name;
        };

        $scope.initWmsSourceMapContext();

    });
