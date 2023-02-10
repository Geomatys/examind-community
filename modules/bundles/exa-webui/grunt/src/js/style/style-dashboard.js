/*
 * Constellation - An open source and standard compliant SDI
 *      http://www.constellation-sdi.org
 *   (C) 2014, Geomatys
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details..
 */

angular.module('cstl-style-dashboard', [
    'cstl-restapi',
    'cstl-services',
    'examind.components.preview.map',
    'ui.bootstrap.modal',
    'examind-instance',
    'webui-utils'])

    .constant('STYLE_DEFAULT_QUERY', {
        page: 1,
        size: 10 ,
        sort: { field: 'name', order: 'ASC' }
    })

    .controller('StylesController', function($scope, Paging,Growl,StyleSharedService,$modal,$window,
                                             Examind,STYLE_DEFAULT_QUERY) {
        var self = this;

        self.preview = { layer: undefined, extent: undefined };

        self.styleOpts = {
            cstlUrl: window.localStorage.getItem('cstlUrl'),
            currentStyleId:null,
            currentDataId:null,
            currentLayerId:null
        };

        self.selected = null;

        self.smallMode = false;
        
        self.defaultData = {
            "default_point" : null,
            "default_line" : null,
            "default_polygon" : null,
            "default_raster" : null
        };
        
        self.initDefaultStyles = function() {
            var query = {
                    "page": 1,
                    "size": 10,
                    "filters": [
                        {
                            "field": "hidden",
                            "value": "true"
                        },{
                            "operator": "OR",
                            "filters": [{
                                    "field": "term",
                                    "value": "CNTR_"
                                },{
                                    "field": "term",
                                    "value": "cloudsgrey"
                                }]
                        }
                    ]
                };

            Examind.datas.searchDatas(query).then(
                function (response) {
                    if (response.data.content) {
                        response.data.content.forEach(
                            function (data) {
                                if ('CNTR_LB_2006' === data.name) {
                                    self.defaultData.default_point = data.id;
                                } else if ('CNTR_BN_60M_2006' === data.name) {
                                    self.defaultData.default_line = data.id;
                                } else if ('CNTR_RG_60M_2006' === data.name) {
                                    self.defaultData.default_polygon = data.id;
                                } else if ('cloudsgrey' === data.namespace) {
                                    self.defaultData.default_raster = data.id;
                                }
                            });
                    }
                },
                function() {
                    Growl('error', 'Error', 'Errow whle searching for default data');
                }
            );
        };

        self.initStyleDashboard = function() {
            // Apply Paging features on the controller instance.
            Paging.call(self, Examind.styles.searchStyles, angular.copy(STYLE_DEFAULT_QUERY));
            // Immediate content loading.
            self.search();
            setTimeout(function(){
                self.previewStyledData(null,false);
            },300);
        };

        /**
         * Reset filters for dashboard
         */
        self.resetFilters = function(){
            self.query = angular.copy(STYLE_DEFAULT_QUERY);
            self.search();
        };

        self.getFilter = function(field) {
            if(self.query.filters){
                for(var i=0;i<self.query.filters.length;i++) {
                    if(self.query.filters[i].field === field) {
                        return self.query.filters[i].value;
                    }
                }
            }
            return null;
        };

        self.filterBy = function(field,value) {
            if(self.query.filters){
                var filterExists=false;
                for(var i=0;i<self.query.filters.length;i++) {
                    if(self.query.filters[i].field === field) {
                        self.query.filters[i].value = value;
                        filterExists=true;
                        break;
                    }
                }
                if(!filterExists){
                    self.query.filters.push({"field":field,"value":value});
                }
            }else {
                self.query.filters = [];
                self.query.filters.push({"field":field,"value":value});
            }
            self.setPage(1);
        };

        self.toggleItemSelection = function(item) {
            if(self.selected && self.selected.id === item.id) {
                self.selected = null;
            } else {
                self.selected = item;
            }
        };

        /**
         * Proceed to remove the selected styles from dashboard.
         */
        self.deleteStyle = function() {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.style";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    var styleId = self.selected.id;
                    var styleName = self.selected.name;
                    Examind.styles.deleteStyle(styleId).then(
                        function() {
                            Growl('success', 'Success', 'Style ' + styleName + ' successfully deleted');
                            self.setPage(1);
                            self.selected=null;
                            self.previewStyledData(null,false);
                        },
                        function() {
                            Growl('error', 'Error', 'Style ' + styleName + ' deletion failed');
                        }
                    );
                }
            });
        };

        /**
         * Proceed to open modal SLD editor to edit the selected style
         */
        self.editStyle = function() {
            var styleId = self.selected.id;
            Examind.styles.getStyle(styleId).then(
                function(response) {
                    StyleSharedService.showStyleEdit(self, response.data);
                }
            );
        };

        self.editStyleWithLinkedData = function(selectedData) {
            Examind.styles.getStyle(self.selected.id).then(
                function(response) {
                    StyleSharedService.editLinkedStyle(self, response.data, selectedData);
                }
            );
        };

        /**
         * Toggle up and down the selected item
         */
        self.toggleUpDownSelected = function() {
            var $header = $('#stylesDashboard').find('.selected-item').find('.block-header');
            $header.next().slideToggle(200);
            $header.find('i').toggleClass('fa-chevron-down fa-chevron-up');
        };

        /**
         * Open sld editor modal to create a new style.
         */
        self.showStyleCreate = function() {
            StyleSharedService.showStyleCreate(self);
        };

        /**
         * Open sld editor modal to upload new style.
         */
        self.showStyleImport = function() {
            StyleSharedService.showStyleImport(self);
        };

        self.previewStyledData = function(item,isLayer) {
            var selectedStyle = self.selected;
            if(selectedStyle) {
                self.styleOpts.currentStyleId=selectedStyle.id;
                var dataToShow;
                if(item){
                    dataToShow = item;
                    if(isLayer){
                        //item is a layer from service
                        //property item.dataId already exists if it is layer
                        self.styleOpts.currentLayerId=item.id;
                        self.styleOpts.currentDataId=null;
                    }else {
                        //item is a data not layer
                        dataToShow.dataId = dataToShow.id;
                        self.styleOpts.currentDataId=item.id;
                        self.styleOpts.currentLayerId=null;
                    }
                }else if(selectedStyle.dataList && selectedStyle.dataList.length>0){
                    dataToShow = selectedStyle.dataList[0];
                    dataToShow.dataId = dataToShow.id; // filling dataId with data.id
                    self.styleOpts.currentDataId=dataToShow.id;
                    self.styleOpts.currentLayerId=null;
                }else if(selectedStyle.layersList && selectedStyle.layersList.length>0) {
                    dataToShow = selectedStyle.layersList[0];
                    //property dataToShow.dataId already exists if it is layer
                    self.styleOpts.currentLayerId=dataToShow.id;
                    self.styleOpts.currentDataId=null;
                }else {
                    // the style is not used by any data or layer.
                    // So we should use a default data depending on style type vector or raster.
                    self.styleOpts.currentLayerId=null;
                    self.styleOpts.currentDataId=null;
                    dataToShow = {
                        dataId:null,
                        namespace:null,
                        name:null,
                        provider:null,
                        type:selectedStyle.type
                    };
                    if(selectedStyle.type && selectedStyle.type.toLowerCase() === 'vector'){
                        dataToShow.name = 'CNTR_RG_60M_2006';
                        dataToShow.dataId = self.defaultData.default_polygon;
                    }else {
                        dataToShow.name = 'cloudsgrey';
                        dataToShow.dataId = self.defaultData.default_raster;
                    }
                }
                if(dataToShow) {
                    var layerName = dataToShow.name;
                    var namespace = dataToShow.namespace;
                    if (namespace) {
                        layerName = '{' + namespace + '}' + layerName;
                    }
                    var type = dataToShow.type.toLowerCase();
                    var layerData = StyleDashboardViewer.createLayerWithStyle(self.styleOpts.cstlUrl,dataToShow.dataId,
                        layerName,
                        selectedStyle.name,
                        null,null,type!=='vector');
                    //to force the browser cache reloading styled layer.
                    layerData.get('params').ts=new Date().getTime();
                    self.preview.layer = layerData;

                    Examind.datas.getGeographicExtent(dataToShow.dataId).then(
                        function(response){
                            self.preview.extent = response.data.boundingBox;
                        },
                        function() {
                            self.preview.extent = undefined;
                        });
                }
            }else {
                self.preview.extent = self.preview.layer = undefined;
                self.styleOpts.currentStyleId = null;
            }
        };

        self.duplicateStyle = function () {
            Examind.styles.getStyle(self.selected.id)
                .then(function (response) {
                    var data = {
                        name: self.selected.name + '-copy-' + new Date().getTime(),
                        rules: response.data.rules
                    };
                    Examind.styles.createStyle(data, 'sld')
                        .then(function () {
                            Growl('success', 'Success', 'Style ' + self.selected.name + ' successfully duplicated');
                            self.initStyleDashboard();
                        }, function () {
                            Growl('error', 'Error', 'Style ' + self.selected.name + ' duplication failed');
                        });
                }, function () {
                    Growl('error', 'Error', 'Get Style ' + self.selected.name + ' data failed');
                });
        };

        $scope.$on('update-style-data', function (evt, args) {
            self.selected.name = args;
        });
        
        
        self.initDefaultStyles();
    })

    .controller('StyleImportModalController', function ($rootScope, $scope, $modalInstance,
                                                        Growl, cfpLoadingBar) {

        $scope.import = {
            styleName : '',
            allowSubmit : false,
            badExtension : false,
            alreadyExistsName : null
        };

        $scope.isValidField = function(input){
            if(input){
                return (input.$valid || input.$pristine);
            }
            return true;
        };

        $scope.isValidRequired = function(input){
            if(input){
                return ! input.$error.required;
            }
            return true;
        };

        $scope.verifyExtension = function(path) {
            $scope.$apply( function (){
                var lastPointIndex = path.lastIndexOf(".");
                var extension = path.substring(lastPointIndex+1, path.length);
                if (extension && (extension.toLowerCase() === 'xml' || extension.toLowerCase() === 'sld' || extension.toLowerCase() === 'cpt' || extension.toLowerCase() === 'clr' || extension.toLowerCase() === 'pal') ) {
                    //ok to sumbit the form
                    $scope.import.allowSubmit = true;
                    $scope.import.badExtension = false;
                    if(!$scope.import.styleName) {
                        $scope.import.styleName = path.substring(path.lastIndexOf("\\")+1,lastPointIndex);
                    }
                } else {
                    //bad extension then disable submitting the form
                    $scope.import.allowSubmit = false;
                    $scope.import.badExtension = true;
                }
            });
        };

        $scope.close = function() {
            $modalInstance.dismiss('close');
        };

        $scope.uploadStyle = function() {

            //ensure that styleName is never empty, otherwise generate one.
            var styleName = $scope.import.styleName;
            if(! styleName){
                styleName = 'SLD_import_'+new Date().getTime();
            }

            var $form = $('#uploadSLDform');
            var formData = new FormData($form[0]);
            $.ajax({
                url: window.localStorage.getItem('cstlUrl') + "API/internal/styles/import",
                type: 'POST',
                data: formData,
                async: false,
                cache: false,
                contentType: false,
                processData: false,
                beforeSend: function(){
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                },
                success: function (response) {
                    Growl('success','Success','Style imported with success.');
                    $modalInstance.close();
                    cfpLoadingBar.complete();
                },
                error: function (data){
                    if(data.responseJSON && (data.responseJSON.errorMessage || data.responseJSON.errorMessageI18nCode) ){
                        Growl('error','Error','Style with name: '+styleName+' already exists!');
                        $scope.import.alreadyExistsName = styleName;
                    }else {
                        Growl('error','Error','Unable to import style, please contact an administrator for more details.');
                    }
                    cfpLoadingBar.complete();
                }
            });
        };
    });
