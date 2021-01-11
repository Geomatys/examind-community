angular.module('cstl-metadata-dashboard', [
    'cstl-restapi',
    'cstl-services',
    'ui.bootstrap.modal',
    'examind-instance',
    'webui-utils'])

    .constant('metadataQuery', { page: 1, size: 10 ,sort: { field: 'title', order: 'ASC' } })

    .controller('MetadataDashboardController', function($scope,$routeParams,DashboardHelper, metadataQuery,
                                                        Growl,$window,$modal,$translate,SelectionApi,
                                                        $timeout,Permission,Examind) {

        var self = this;

        self.permission = Permission;
        self.userAccount = null;

        self.currentTab = $routeParams.tab || 'dashboard';
        self.ownerFilter = $routeParams.owner || null;
        self.idFilter = Number($routeParams.id) || null;
        self.selectionApi = SelectionApi;
        self.cstlUrl = window.localStorage.getItem('cstlUrl');
        self.hideScroll = true;
        self.searchMetadataTerm = "";
        self.alphaPattern = /^([0-9A-Za-z\u00C0-\u017F\*\?]+|\s)*$/;
        self.smallMode = false;
        self.hideGroupFilter = true;//always true in this project since groups are not used in this project
        self.searchMD = {};
        self.profileList = [];
        self.allProfileList = [];
        self.allFilteredIds = {total:0,list:[]};
        self.displayCount=10; //this is the limit count of records to show in selection blocks
        self.selectedAll=false;
        self.batch = {
            user : {
                selected : null,
                list : []
            },
            validation : {
                selected : '',
                comment : ''
            },
            publication : {
                selected : ''
            }
        };

        /**
         * Array of options for select input to set validation and published state
         */
        self.booleanOptions = [{value:true},{value:false}];

        /**
         * Stats binding object to show metadata count in block in bottom-right side of the page
         */
        self.stats = {
            total:0,
            validated:0,
            notValid:0,
            waitingToValidate:0,
            published:0,
            notPublish:0,
            waitingToPublish:0
        };

        /**
         * array of all possible actions to add new metadata.
         * @type {*[]}
         */
        self.waysToAddMetadata = [
            {   name:'newMetadata',
                idHTML:'newMetadata',
                translateKey:'label.metadata.new.file',
                defaultTranslateValue:'New metadata',
                bindFunction:function(){self.openCreateNewMetadata();}
            },
            {   name:'importMetadata',
                idHTML:'importMetadata',
                translateKey:'label.metadata.upload.file',
                defaultTranslateValue:'Import metadata',
                bindFunction:function(){self.openUploadMetadata();}
            }
        ];

        self.filterCollection = {
            name:'',
            matchedList:[]
        };

        self.updateCollectionMatched = function() {
            Examind.metadata.search(
                {
                    page: 1,
                    size: 10,
                    sort: { field: 'title', order: 'ASC' },
                    text: self.filterCollection.name,
                    filters: [{field:'profile',value:'profile_collection'}]
                }, {type:'DOC'}).then(
                function success(response) {
                    self.filterCollection.matchedList = response.data.content;
                },
                function error(response) {
                    Growl('error','Error','An error occurred when getting collection list!');
                    self.filterCollection.matchedList = [];
                }
            );
        };

        /**
         * Init function called once the page loaded.
         */
        self.init = function() {
            self.searchMetadataTerm="";

            //init autocompletion for collection
            self.filterCollection.name='';
            self.filterCollection.matchedList=[];
            self.updateCollectionMatched();

            //get list of used profiles
            Examind.metadata.getProfiles(false, null, 'DOC').then(
                function success(response) {
                    self.profileList = response.data.filter(function(p){
                        return p.name !== null;
                    }).map(function(p){
                        return p;
                    });
                    self.profileList.sort(function (p1, p2) {
                        return p1.name.localeCompare(p2.name);
                    });
                },
                function error() {
                    Growl('error','Error','An error occurred when getting used profiles list!');
                    self.profileList = [];
                }
            );

            //get list of all profiles
            Examind.metadata.getProfiles(true, null, 'DOC').then(
                function success(response) {
                    self.allProfileList = response.data.filter(function(p){
                        return p.name !== null;
                    }).map(function(p){
                        return p;
                    });
                    self.allProfileList.sort(function (p1, p2) {
                        return p1.name.localeCompare(p2.name);
                    });
                },
                function error() {
                    Growl('error','Error','An error occurred when getting all profiles list!');
                    self.allProfileList = [];
                }
            );

            //get list of users
            self.batch.user.list = [];
            Examind.metadata.getUsersList()
                .then(function success(response){
                        self.batch.user.list = response.data;
                    },
                    function error() {
                        Growl('error','Error','An error occurred when getting users list!');
                        self.batch.user.list = [];
                    });

            Permission.promise.then(function(){
                self.userAccount = Permission.getAccount();
            });

            //get stats counts
            calculateStats();

            //init with owner filter if param exists in the url
            var toSend = {"filters":[]};
            if(self.ownerFilter) {
                toSend.filters.push({"field":"owner","value":self.ownerFilter});
            }
            if(self.idFilter) {
                toSend.filters.push({"field":"id","value":self.idFilter});
            }

            DashboardHelper.call(self, Examind.metadata.search, angular.extend(toSend,angular.copy(metadataQuery)), null, {type:'DOC'});
            self.search();

            //update array of filtered ids
            Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                function success(response) {
                    self.allFilteredIds = response.data;
                }
            );

            //display button that allow to scroll to top of the page from a certain height.
            angular.element($window).bind("scroll", function() {
                self.hideScroll = this.pageYOffset < 220;
                $scope.$apply();
            });
        };

        self.init();

        /**
         * Select appropriate tab 'dashboard' or 'metadata'.
         * @param item
         */
        self.selectTab = function(item) {
            self.currentTab = item;
        };

        /**
         * Function to calculate stats of metadata for bottom right side block.
         * this function must be called in following cases :
         *  -init of the page
         *  -when creating new metadata
         *  -when importing new metadata
         *  -when deleting metadata
         *  -when changing validation state
         *  -when changing publication state
         */
        function calculateStats() {
            Examind.metadata.getStats('DOC').then(
                function success(response){
                    self.stats.total = response.data.total;
                    self.stats.validated = response.data.validated;
                    self.stats.notValid = response.data.notValid;
                    self.stats.waitingToValidate = response.data.waitingToValidate;
                    self.stats.published = response.data.published;
                    self.stats.notPublish = response.data.notPublish;
                    self.stats.waitingToPublish = response.data.waitingToPublish;
                }
            );
        }

        /**
         * *************************************************
         * ********* Main actions to add metadata **********
         * *************************************************
         */

        /**
         * Opening modal to create new metadata.
         */
        self.openCreateNewMetadata = function() {
            var modal = $modal.open({
                templateUrl: 'views/metadata/modalCreateMetadata.html',
                controller: 'CreateMetadataModalController',
                resolve: {
                    'profileList':function(){
                        return self.allProfileList;
                    }
                }
            });
            modal.result.then(function() {
                //reload filtered list
                self.setPage(self.page.number);
                //update array of ids
                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                    function success(response) {
                        self.allFilteredIds = response.data;
                    }
                );
                calculateStats();
            });
        };

        /**
         * Opening modal to upload metadata.
         */
        self.openUploadMetadata = function() {
            var modal = $modal.open({
                templateUrl: 'views/metadata/modalUploadMetadata.html',
                controller: 'UploadMetadataModalController',
                resolve: {
                    'profileList':function(){
                        return self.allProfileList;
                    }
                }
            });
            modal.result.then(function() {
                //reload filtered list
                self.setPage(self.page.number);
                //update array of ids
                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                    function success(response) {
                        self.allFilteredIds = response.data;
                    }
                );
                calculateStats();
            });
        };

        /**
         * *************************************************
         * ********* Actions when selecting rows ***********
         * *************************************************
         */

        self.unselectAll = function() {
            self.selectionApi.clear();
            self.selectedAll=false;
        };

        /**
         * Function to call when opening selection block to show attributes.
         * load metadata attributes for given item.
         * @param item
         */
        self.loadMDItem = function(item) {
            Examind.metadata.get(item.id).then(function success(response){
                item = jQuery.extend(true,item, response.data);
            });
            self.toggleUpDownSelectedMD('block-single-action'+item.id);
        };

        /**
         * Apply batch change of owner for selected metadata list
         */
        self.applyOwnerMulti = function() {
            if(self.batch.user.selected){
                Examind.metadata.changeOwner(self.batch.user.selected,self.selectionApi.getList())
                    .then(function success(response) {
                        Growl('success', 'Success', 'The changes have been successfully applied!');
                        //reload filtered list
                        self.setPage(self.page.number);
                        //update array of ids
                        Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                            function success(response) {
                                self.allFilteredIds = response.data;
                            }
                        );
                    },function error(response) {
                        Growl('error','Access denied','Cannot apply owner for the selected records! '+(response.data?response.data.msg:''));
                    });
            }
        };

        /**
         * Set owner for given selected metadata.
         * @param item given metadata
         */
        self.applyOwnerSelected = function(item) {
            var toChange = [];
            toChange.push(item);

            Examind.metadata.changeOwner(item.user.id,toChange)
                .then(function success(response) {
                    Growl('success', 'Success', 'Owner changed successfully!');
                    //reload filtered list
                    self.setPage(self.page.number);

                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                },function error(response) {
                    Growl('error','Access denied','Cannot apply owner for the selected metadata! '+(response.data?response.data.msg:''));
                    self.setPage(self.page.number);
                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                });
        };

        /**
         * Apply batch change of validation state for selected metadata list
         */
        self.applyValidationMulti = function() {
            if(self.batch.validation.selected){
                var validationList = {
                    metadataList : self.selectionApi.getList().map(function(md){
                        return md.id;
                    }),
                    comment : self.batch.validation.comment
                };

                Examind.metadata.changeValidation('valid' === self.batch.validation.selected,validationList)
                    .then(function success(response) {
                        Growl('success', 'Success', 'The changes have been successfully applied!');
                        //reload filtered list
                        self.setPage(self.page.number);
                        //update array of ids
                        Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                            function success(response) {
                                self.allFilteredIds = response.data;
                            }
                        );
                        calculateStats();
                    },function error(response) {
                        if(response.data) {
                            if(response.data.notLevel === 'true') {
                                Growl('error','Access denied','Please make sure that selected metadata have at least a sufficient completion level!');
                            } else {
                                Growl('error','Access denied','Cannot apply validation state for the selected records! '+response.data.msg);
                            }
                        }else {
                            Growl('error','Access denied','Cannot apply validation state for the selected records!');
                        }
                    });
            }
        };

        /**
         * Set validation state for given selected metadata.
         * @param item given metadata
         */
        self.applyValidationSelected = function(item) {
            var toChange = [];
            toChange.push(item.id);

            var validationList = {
                metadataList : toChange,
                comment : item.comment
            };

            Examind.metadata.changeValidation(item.isValidated, validationList)
                .then(function success(response) {
                    Growl('success', 'Success', 'Validation changed successfully!');
                    //reload filtered list
                    self.setPage(self.page.number);
                    calculateStats();
                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                },function error(response) {
                    if(response.data) {
                        if(response.data.notLevel === 'true') {
                            Growl('error','Access denied','Please make sure that the metadata have at least a sufficient completion level!');
                        } else {
                            Growl('error','Access denied','Cannot apply validation state for this metadata! '+response.data.msg);
                        }
                    }else {
                        Growl('error','Access denied','Cannot apply validation state for this metadata!');
                    }
                    self.setPage(self.page.number);
                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                });
        };

        self.acceptValidationSelected = function(item) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.validation.selected.accept";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    var toChange = [];
                    toChange.push(item.id);
                    var validationList = {
                        metadataList : toChange,
                        comment : null
                    };

                    Examind.metadata.changeValidation(true, validationList)
                        .then(function success(response) {
                            Growl('success', 'Success', 'Validation changed successfully!');
                            //reload filtered list
                            self.setPage(self.page.number);
                            calculateStats();
                            //update selected item in selection block
                            Examind.metadata.get(item.id).then(function success(response){
                                item = jQuery.extend(true,item, response.data);
                            });
                        },function error(response) {
                            if(response.data) {
                                if(response.data.notLevel === 'true') {
                                    Growl('error','Access denied','Please make sure that the metadata have at least a sufficient completion level!');
                                } else {
                                    Growl('error','Access denied','Cannot apply validation state for this metadata! '+response.data.msg);
                                }
                            }else {
                                Growl('error','Access denied','Cannot apply validation state for this metadata!');
                            }
                        });
                }
            });
        };

        self.rejectValidationSelected = function(item) {
            var modalRejectValidation = $modal.open({
                templateUrl: 'views/metadata/modalRejectValidation.html',
                controller: 'MDDenyValidationModalController as validCtrl',
                resolve: {
                    'record':function(){return item;}
                }
            });
            modalRejectValidation.result.then(function(cfrm){
                if(cfrm) {
                    var toChange = [];
                    toChange.push(item.id);
                    var validationList = {
                        metadataList : toChange,
                        comment : item.comment
                    };

                    Examind.metadata.changeValidation(false, validationList)
                        .then(function success(response) {
                            Growl('success', 'Success', 'Validation refused successfully!');
                            //reload filtered list
                            self.setPage(self.page.number);
                            calculateStats();
                            //update selected item in selection block
                            Examind.metadata.get(item.id).then(function success(response){
                                item = jQuery.extend(true,item, response.data);
                            });
                        },function error(response) {
                            if(response.data) {
                                if(response.data.notLevel === 'true') {
                                    Growl('error','Access denied','Please make sure that the metadata have at least a sufficient completion level!');
                                } else {
                                    Growl('error','Access denied','Cannot apply validation state for this metadata! '+response.data.msg);
                                }
                            }else {
                                Growl('error','Access denied','Cannot apply validation state for this metadata!');
                            }
                        });
                }
            });
        };

        self.showCommentModerator = function(item) {
            var modalComments = $modal.open({
                templateUrl: 'views/metadata/modalCommentsValidation.html',
                controller: 'MDCommentsValidationController as commentsCtrl',
                resolve: {
                    'record':function(){return item;}
                }
            });
        };

        /**
         * Apply batch change of publication state for selected metadata list
         */
        self.applyPublicationMulti = function() {
            if(self.batch.publication.selected){
                Examind.metadata.changePublication('published' === self.batch.publication.selected,self.selectionApi.getList())
                    .then(function success(response) {
                        Growl('success', 'Success', 'The changes have been successfully applied!');
                        //reload filtered list
                        self.setPage(self.page.number);
                        //update array of ids
                        Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                            function success(response) {
                                self.allFilteredIds = response.data;
                            }
                        );
                        calculateStats();
                    },function error(response) {
                        if(response.data) {
                            if(response.data.notValidExists === 'true') {
                                Growl('error','Access denied','There are not valid metadata, Please make sure that selected metadata are valid!');
                            } else {
                                Growl('error','Access denied','Cannot apply publication state for the selected records! '+response.data.msg);
                            }
                        }else {
                            Growl('error','Access denied','Cannot apply publication state for the selected records!');
                        }
                    });
            }
        };

        /**
         * Set published state for given selected metadata.
         * @param item given metadata
         */
        self.applyPublicationSelected = function(item) {
            var toChange = [];
            toChange.push(item);
            Examind.metadata.changePublication(item.isPublished,toChange)
                .then(function success(response) {
                        Growl('success', 'Success', 'Record published successfully!');
                        //reload filtered list
                        self.setPage(self.page.number);
                        calculateStats();
                        //update selected item in selection block
                        Examind.metadata.get(item.id).then(function success(response){
                            item = jQuery.extend(true,item, response.data);
                        });
                    },
                    function error(response) {
                        if(response.data) {
                            if(response.data.notValidExists === 'true') {
                                Growl('error','Access denied','Please make sure that the metadata is valid first before changing the publication state!');
                            } else {
                                Growl('error','Access denied','Cannot apply publication state for the selected record! '+response.data.msg);
                            }
                        }else {
                            Growl('error','Access denied','Cannot apply publication state for the selected record!');
                        }
                        self.setPage(self.page.number);
                        //update selected item in selection block
                        Examind.metadata.get(item.id).then(function success(response){
                            item = jQuery.extend(true,item, response.data);
                        });
                    });
        };

        self.deleteMetadata = function(item) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.selected.onemetadata";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    var toDelete = [];
                    toDelete.push(item);
                    Examind.metadata.delete(toDelete)
                        .then(function success(response) {
                                Growl('success','Success','Record successfully deleted');
                                //reload filtered list
                                self.setPage(1);
                                //update array of ids
                                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                                    function success(response) {
                                        self.allFilteredIds = response.data;
                                    }
                                );
                                self.selectionApi.remove(item);
                                calculateStats();
                            },
                            function error(response) {
                                Growl('error','Access denied','Cannot delete the selected metadata! '+(response.data?response.data.msg:''));
                            });
                }
            });
        };

        self.deleteMultiple = function() {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.selected.metadata";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    Examind.metadata.delete(self.selectionApi.getList())
                        .then(function success(response) {
                                Growl('success','Success','Records successfully deleted');
                                //reload filtered list
                                self.setPage(1);
                                //update array of ids
                                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                                    function success(response) {
                                        self.allFilteredIds = response.data;
                                    }
                                );
                                self.selectedAll = false;
                                self.selectionApi.clear();
                                calculateStats();
                            },
                            function error(response) {
                                Growl('error','Access denied','Cannot delete the selected records! '+(response.data?response.data.msg:''));
                            });
                }
            });
        };

        self.exportMetadata = function(item) {
            var toExport = [];
            toExport.push(item.id);
            Examind.metadata.exportMetadata(toExport)
                .then(function success(response) {
                    response = response.data;
                    if(response.directory && response.file){
                        //start download in callback
                        window.open(self.cstlUrl+'API/metadatas/download/'+response.directory+'/'+response.file, '_self', '');
                    }
                });
        };

        self.exportMultiple = function() {
            //send in post the list of metadata ids
            var ids = self.selectionApi.getList().map(function(md){
               return md.id;
            });
            Examind.metadata.exportMetadata(ids)
                .then(function success(response) {
                    response = response.data;
                    if(response.directory && response.file){
                        //start download in callback
                        window.open(self.cstlUrl+'API/metadatas/download/'+response.directory+'/'+response.file, '_self', '');
                    }
                });
        };

        self.askForValidationMultiple = function() {
            Examind.metadata.askForValidation(self.selectionApi.getList())
                .then(function success(response) {
                    Growl('success', 'Success', 'the validation request has been sent.');
                    //reload filtered list
                    self.setPage(self.page.number);
                    //update array of ids
                    Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                        function success(response) {
                            self.allFilteredIds = response.data;
                        }
                    );
                    calculateStats();
                },function error(response) {
                    if(response.data) {
                        if(response.data.notOwner === 'true' && response.data.validExists === 'true' ) {
                            Growl('error','Access denied','Please make sure you are the owner of these metadata with status not valid!');
                        } else if(response.data.notOwner === 'true') {
                            Growl('error','Access denied','Please make sure you are the owner of these metadata!');
                        } else if(response.data.validExists ==='true' ) {
                            Growl('error','Access denied','Please make sure there are metadata with status not valid in the selection!');
                        } else {
                            Growl('error','Access denied','Cannot send validation request for the selection! '+response.data.msg);
                        }
                    }else {
                        Growl('error','Access denied','Cannot send validation request for the selection!');
                    }
                });
        };

        self.askForValidationSelected = function(item) {
            var toSend = [];
            toSend.push(item);
            Examind.metadata.askForValidation(toSend)
                .then(function success(response) {
                    Growl('success', 'Success', 'the validation request has been sent.');
                    //reload filtered list
                    self.setPage(self.page.number);
                    calculateStats();
                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                },function error(response) {
                    if(response.data) {
                        if(response.data.notOwner === 'true' && response.data.validExists === 'true' ) {
                            Growl('error','Access denied','Please make sure you are the owner of this metadata with status not valid!');
                        } else if(response.data.notOwner === 'true') {
                            Growl('error','Access denied','Please make sure you are the owner of this metadata!');
                        } else if(response.data.validExists ==='true' ) {
                            Growl('error','Access denied','the metadata is already valid!');
                        } else {
                            Growl('error','Access denied','Cannot send validation request for this metadata! '+response.data.msg);
                        }
                    } else {
                        Growl('error','Access denied','Cannot send validation request for the selection!');
                    }
                    self.setPage(self.page.number);
                    //update selected item in selection block
                    Examind.metadata.get(item.id).then(function success(response){
                        item = jQuery.extend(true,item, response.data);
                    });
                });
        };

        /**
         * Open metadata viewer popup and display metadata
         * in appropriate template depending on metadata profile.
         */
        self.displayMetadata = function(item) {
            $modal.open({
                templateUrl: 'views/data/modalViewMetadata.html',
                controller: 'ViewMetadataModalController',
                resolve: {
                    'dashboardName':function(){return 'dataset';},
                    'metadataValues':function(){
                        return  Examind.metadata.getIsoMetadataJson(item.id, true);
                    }
                }
            });
        };

        /**
         * Open metadata editor in modal for given item record.
         * @param item
         */
        self.displayMetadataEditor = function(item) {
            var modalEditor = $modal.open({
                templateUrl: 'views/data/modalEditMetadata.html',
                controller: 'MDEditionModalController',
                resolve: {'record':function(){return item;}}
            });
            modalEditor.result.then(function(){
                //reload filtered list
                self.setPage(self.page.number);
                //update array of ids
                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                    function success(response) {
                        self.allFilteredIds = response.data;
                    }
                );
                calculateStats();

                //update selected item in selection block
                Examind.metadata.get(item.id).then(function success(response){
                    SelectionApi.update(response.data);
                    $timeout(function(){
                        var elem = $('#block-single-action'+item.id);
                        elem.next().show();
                        elem.find('i.pull-left').toggleClass('fa-chevron-down fa-chevron-right');
                    });
                });
            });
        };

        /**
         * Proceed to open modal with profiles list
         * to allow user to choice one of them to convert the selected metadata.
         * @param item
         */
        self.convertMetadata = function(item) {
            var modalConvert = $modal.open({
                templateUrl: 'views/metadata/modalConvertMetadata.html',
                controller: 'MDConvertModalController',
                resolve: {
                    'record':function(){return item;},
                    'profileList':function(){
                        return self.allProfileList;
                    }
                }
            });
            modalConvert.result.then(function(){
                //reload filtered list
                self.setPage(self.page.number);
                //update array of ids
                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                    function success(response) {
                        self.allFilteredIds = response.data;
                    }
                );
                calculateStats();
                //update selected item in selection block
                Examind.metadata.get(item.id).then(function success(response){
                    SelectionApi.update(response.data);
                    $timeout(function(){
                        var elem = $('#block-single-action'+item.id);
                        elem.next().show();
                        elem.find('i.pull-left').toggleClass('fa-chevron-down fa-chevron-right');
                    });
                });
            });
        };

        /**
         * Duplicate given metadata record.
         * @param item
         */
        self.duplicateMetadata = function(item) {
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.duplicate.selected.onemetadata";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    Examind.metadata.duplicateMetadata(item.id, null, null)
                        .then(function success(response) {
                                Growl('success','Success','Record successfully duplicated');
                                //reload filtered list
                                self.setPage(self.page.number);
                                //update array of ids
                                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                                    function success(response) {
                                        self.allFilteredIds = response.data;
                                    }
                                );
                                calculateStats();
                            },
                            function error(response) {
                                Growl('error','Error','Cannot duplicate the selected record! '+response.data.msg);
                            });
                }
            });
        };

        /**
         * *************************************************
         * *************** Selection functions**************
         * *************************************************
         */

        self.isThereSelectedItems = function() {
            return self.selectionApi.getLength() > 0;
        };

        self.toggleSelectAll = function() {
            if(self.selectedAll) {
                self.selectedAll = false;
                self.selectionApi.clear();
            }else {
                Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                    function success(response) {
                        self.selectedAll = true;
                        self.allFilteredIds = response.data;
                        if(response.data.list){
                            angular.forEach(response.data.list, function(item) {
                                self.selectionApi.add(item);
                            });
                        }
                    }
                );
            }
        };

        self.toggleItemSelection = function(item) {
            self.selectionApi.toggle(item);
            self.selectedAll = (self.selectionApi.getLength() > 0) && (self.selectionApi.getLength() === self.allFilteredIds.total);
        };

        self.isSelectedItem = function(item) {
            return self.selectionApi.isExist(item);
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
            self.updateFilterBy(field,value);
            self.setPage(1);
            //update array of ids
            Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                function success(response) {
                    self.allFilteredIds = response.data;
                }
            );
        };

        self.updateFilterBy = function(field,value) {
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
        };

        self.resetFilters = function(){
            self.query = angular.copy(metadataQuery);
            self.setPage(1);
            //update array of ids
            Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                function success(response) {
                    self.allFilteredIds = response.data;
                }
            );
            self.selectedAll = false;
            self.selectionApi.clear();
            self.searchMetadataTerm = "";
        };

        self.resetSearchMD = function(){
            self.searchMD = {};
        };

        self.checkIsValid = function(isInvalid){
            if (isInvalid){
                Growl('error','Error','Invalid Chars');
            }
        };

        self.callSearchMDForTerm = function(term){
            self.query.text = term;
            self.setPage(1);
            Examind.metadata.searchIds(self.query,{type:'DOC'}).then(
                function success(response) {
                    self.allFilteredIds = response.data;
                }
            );
        };

        self.callSearchMD = function(){
            self.callSearchMDForTerm(self.searchMetadataTerm);
        };

        self.toggleUpDownSelectedMD = function(id) {
            var elem = $('#'+id);
            elem.next().slideToggle(200);
            elem.find('i.pull-left').toggleClass('fa-chevron-down fa-chevron-right');
        };

        self.openDetails = function(item) {
            if(self.selectionApi.isExist(item)) {
                setTimeout(function(){
                    $('.blockDetails').slideUp();
                    $('.block-header-md').find('.fa-chevron-down').toggleClass('fa-chevron-down fa-chevron-right');
                    self.toggleUpDownSelectedMD('block-single-action'+item.id);
                },200);
            }
        };

    })

    .controller('MDDenyValidationModalController', function($scope,record) {
        var self = this;
        self.record = record;
        self.record.comment = '';
    })

    .controller('MDCommentsValidationController', function($scope,record) {
        var self = this;
        self.record = record;
    })

    .controller('MDEditionModalController', function($scope, $controller,$modalInstance,Growl,record,
                                                     $rootScope,$http, Examind) {

        //FIXME change self=$scope by self=this when cstl angular will be refactored
        var self = $scope;

        self.record = record;
        self.theme = 'csw';
        self.typeLabelKey = self.record.type; //@TODO translate, get profile key
        self.metadataValues = [];
        self.nodeTypes = null;
        self.contentError = false;

        $scope.uploadImage = function(value,field) {
            var cstlUrl = window.localStorage.getItem('cstlUrl');
            if(value) {
                var $form = $('#metadataform');
                var fileInput = $form.find('.uploadimage');
                if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                    return;
                }
                var fileSize = fileInput.get(0).files[0].size/1000000;
                if(fileSize > 2){
                    Growl('error', 'Error', 'The image size exceed the limitation of 2Mo.');
                    return;
                }
                var formData = new FormData($form[0]);
                Examind.metadata.uploadImage(formData)
                    .then(function (response) {
                        if(response.data.attachmentId) {
                            field.value = cstlUrl+'API/attachments/view/'+response.data.attachmentId;
                        }
                    },function(){
                        Growl('error', 'Error', 'error while uploading image');
                    });
            } else {
                field.value = "";
            }
        };

        self.loadMetadataValues = function(){
            Examind.metadata.getIsoMetadataJson(self.record.id, false)
                .then(function success(response) {
                        response = response.data;
                        if (response && response.root) {
                            self.nodeTypes = response.nodeTypes;
                            self.metadataValues.push({"root":response.root});
                            self.contentError = false;
                        }else {
                            self.contentError = true;
                        }
                    },
                    function error(response) {
                        self.contentError = true;
                        Growl('error','Error','The server returned an error! '+response.data);
                    });
        };

        self.save2 = function() {
            if(self.metadataValues && self.metadataValues.length>0){
                Examind.metadata.saveMetadata(self.record.id, self.record.type,
                    $.extend({},{"nodeTypes":self.nodeTypes},self.metadataValues[0]))
                    .then(function(response) {//success
                            self.close();
                            Growl('success','Success','Metadata saved with success!');
                        },
                        function(response) {//error
                            Growl('error','Error','Failed to save metadata because the server returned an error! '+response.data);
                        });
            }
        };

        self.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        self.close = function () {
            $modalInstance.close();
        };

        $controller('EditMetadataController', {$scope: self});

    })

    .controller('MDConvertModalController', function($scope, $controller,$modalInstance,Growl,record,
                                                     profileList,$rootScope,$http, Examind) {
        //FIXME change self=$scope by self=this when cstl angular will be refactored
        var self = $scope;

        self.record = record;
        self.theme = 'csw';
        self.typeLabelKey = null; //will be set after profile selection
        self.metadataValues = [];
        self.nodeTypes = null;
        self.contentError = false;

        self.options = {
            'profileList' : profileList,
            'mode' : {'editorView':false,'profileListView':true},
            'selectedProfile' : null,
            'wizard':'convert'
        };

        self.nextStep = function() {
            self.typeLabelKey = self.options.selectedProfile;
            self.options.mode.profileListView = false;
            self.options.mode.editorView = true;
            self.loadMetadataValues();
        };

        $scope.uploadImage = function(value,field) {
            var cstlUrl = window.localStorage.getItem('cstlUrl');
            if(value) {
                var $form = $('#metadataform');
                var fileInput = $form.find('.uploadimage');
                if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                    return;
                }
                var fileSize = fileInput.get(0).files[0].size/1000000;
                if(fileSize > 2){
                    Growl('error', 'Error', 'The image size exceed the limitation of 2Mo.');
                    return;
                }
                var formData = new FormData($form[0]);

                Examind.metadata.uploadImage(formData)
                    .then(function (response) {
                        if(response.data.attachmentId) {
                            field.value = cstlUrl+'API/attachments/view/'+response.data.attachmentId;
                        }
                    },
                        function(){
                            Growl('error', 'Error', 'error while uploading image');
                        });
            } else {
                field.value = "";
            }
        };

        self.loadMetadataValues = function(){
            if(self.options.selectedProfile) {
                Examind.metadata.convertMetadataJson(self.record.id,false,self.options.selectedProfile)
                    .then(function success(response) {
                            if (response.data && response.data.root) {
                                self.nodeTypes = response.data.nodeTypes;
                                self.metadataValues.push({"root":response.data.root});
                                self.contentError = false;
                            }else {
                                self.contentError = true;
                            }
                        },
                        function error(response) {
                            self.contentError = true;
                            Growl('error','Error','The server returned an error! '+response.data);
                        });
            }
        };

        self.save2 = function() {
            if(self.metadataValues && self.metadataValues.length>0){
                Examind.metadata.saveMetadata(self.record.id, self.options.selectedProfile,
                    $.extend({},{"nodeTypes":self.nodeTypes},self.metadataValues[0]))
                    .then(function(response) {//success
                            self.close();
                            Growl('success','Success','Metadata saved with success!');
                        },
                        function(response) {//error
                            Growl('error','Error','Failed to save metadata because the server returned an error! '+response.data);
                        });
            }
        };

        self.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        self.close = function () {
            $modalInstance.close();
        };

        $controller('EditMetadataController', {$scope: self});

    })

    .controller('UploadMetadataModalController', function($rootScope,$scope,$controller,$modalInstance,Growl,
                                                          cfpLoadingBar,profileList,$http, Examind){
        //FIXME change self=$scope by self=this when cstl angular will be refactored
        var self = $scope;

        self.record = null; //will be set after uploading metadata
        self.theme = 'csw';
        self.typeLabelKey = null;//will be set after uploading metadata, and can be modified if another profile is selected
        self.metadataValues = [];
        self.nodeTypes = null;
        self.contentError = false;

        self.options = {
            'profileList' : profileList,
            'mode' : {'uploadView':true,'profileListView':false,'editorView':false},
            'selectedProfile' : null,
            'extensionError' : false,
            'wizard':'upload',
            'usedDefaultProfile':false
        };

        self.uploadAndNextStepProfiles = function() {
            var $form = $('#uploadMetadata');
            var fileInput = $form.find('input:file');
            if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                Growl('warning', 'Warning', 'You need to specify a metadata file');
                return;
            }
            var fileSize = fileInput.get(0).files[0].size/1000000;
            if(fileSize > 20){
                Growl('error', 'Error', 'The file size exceed the limitation of 20Mo.');
                return;
            }
            var formData = new FormData($form[0]);

            cfpLoadingBar.start();
            cfpLoadingBar.inc();

            Examind.metadata.uploadMetadata(formData,"DOC", null)
                .then(function (response) {
                    cfpLoadingBar.complete();
                    self.record = response.data.record;
                    self.options.selectedProfile = response.data.record.type;
                    self.options.usedDefaultProfile = response.data.usedDefaultProfile;
                    if(response.data.renewId) {
                        Growl('info','Info','a metadata with the same identifier already exists, the identifier was changed!');
                    }
                    if(self.options.usedDefaultProfile) {
                        //ask user before opening editor if he want to change the profile.
                        self.options.mode.profileListView = true;
                        self.options.mode.uploadView = false;
                        self.options.mode.editorView = false;
                    } else {
                        self.nextStepEditor();
                    }
                },
                    function (response){
                        cfpLoadingBar.complete();
                        var msg = response.data.responseJSON ? response.data.responseJSON.msg : '';
                        Growl('error','Error','Unable to import metadata file! '+msg);
                    }
                );
        };

        self.nextStepEditor = function() {
            self.typeLabelKey = self.options.selectedProfile;
            self.options.mode.uploadView = false;
            self.options.mode.profileListView = false;
            self.options.mode.editorView = true;
            self.loadMetadataValues();
        };

        self.verifyExtension = function(path) {
            if(path && path.indexOf('.') !== -1) {
                var lastPointIndex = path.lastIndexOf(".");
                var extension = path.substring(lastPointIndex+1, path.length);
                extension = extension.toLowerCase();
                self.options.extensionError = ("xml" !== extension);
            }else {
                self.options.extensionError = false;
            }
        };

        $scope.uploadImage = function(value,field) {
            var cstlUrl = window.localStorage.getItem('cstlUrl');
            if(value) {
                var $form = $('#metadataform');
                var fileInput = $form.find('.uploadimage');
                if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                    return;
                }
                var fileSize = fileInput.get(0).files[0].size/1000000;
                if(fileSize > 2){
                    Growl('error', 'Error', 'The image size exceed the limitation of 2Mo.');
                    return;
                }
                var formData = new FormData($form[0]);

                Examind.metadata.uploadImage(formData)
                    .then(function (response) {
                        if(response.data.attachmentId) {
                            field.value = cstlUrl+'API/attachments/view/'+response.data.attachmentId;
                        }

                    },
                        function(){
                            Growl('error', 'Error', 'error while uploading image');
                        });
            } else {
                field.value = "";
            }
        };

        self.loadMetadataValues = function(){
            if(self.options.selectedProfile) {
                //call always convert to ensure that the metadata will contains all fields.
                Examind.metadata.convertMetadataJson(self.record.id,false,self.options.selectedProfile)
                    .then(function success(response) {
                            if (response.data && response.data.root) {
                                self.nodeTypes = response.data.nodeTypes;
                                self.metadataValues.push({"root":response.data.root});
                                self.contentError = false;
                            }else {
                                self.contentError = true;
                            }
                        },
                        function error(response) {
                            self.contentError = true;
                            Growl('error','Error','The server returned an error! '+response.data);
                        });
            }
        };

        self.save2 = function() {
            if(self.metadataValues && self.metadataValues.length>0){
                Examind.metadata.saveMetadata(self.record.id,self.options.selectedProfile,
                    $.extend({},{"nodeTypes":self.nodeTypes},self.metadataValues[0]))
                    .then(function(response) {//success
                            self.close();
                            Growl('success','Success','Metadata saved with success!');
                        },
                        function(response) {//error
                            Growl('error','Error','Failed to save metadata because the server returned an error! '+response.data);
                        });
            }
        };

        self.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        self.close = function () {
            $modalInstance.close();
        };

        $controller('EditMetadataController', {$scope: self});

    })

    .controller('CreateMetadataModalController', function($rootScope,$scope, $controller,$modalInstance,Growl,profileList,Examind){
        var self = $scope;

        self.theme = 'csw';
        self.typeLabelKey = null; //will be set after profile selection
        self.metadataValues = [];
        self.nodeTypes = null;
        self.contentError = false;

        self.options = {
            'profileList' : profileList,
            'mode' : {'editorView':false,'profileListView':true},
            'selectedProfile' : null,
            'wizard':'createnew'
        };

        /**
         * Proceed to next step metadata editor.
         * called only if selected profile is not null
         */
        self.nextStep = function() {
            self.typeLabelKey = self.options.selectedProfile;
            self.options.mode.profileListView = false;
            self.options.mode.editorView = true;
            self.loadMetadataValues();
        };

        $scope.uploadImage = function(value,field) {
            var cstlUrl = window.localStorage.getItem('cstlUrl');
            if(value) {
                var $form = $('#metadataform');
                var fileInput = $form.find('.uploadimage');
                if(!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length===0){
                    return;
                }
                var fileSize = fileInput.get(0).files[0].size/1000000;
                if(fileSize > 2){
                    Growl('error', 'Error', 'The image size exceed the limitation of 2Mo.');
                    return;
                }
                var formData = new FormData($form[0]);

                Examind.metadata.uploadImage(formData)
                    .then(function (response) {
                        if(response.data.attachmentId) {
                            field.value = cstlUrl+'API/attachments/view/'+response.data.attachmentId;
                        }
                    },
                        function(){
                            Growl('error', 'Error', 'error while uploading image');
                        }
                    );
            } else {
                field.value = "";
            }
        };

        self.loadMetadataValues = function(){
            if(self.options.selectedProfile) {
                Examind.metadata.getNewMetadataJson(self.options.selectedProfile)
                    .then(function success(response) {
                            if (response.data && response.data.root) {
                                self.nodeTypes = response.data.nodeTypes;
                                self.metadataValues.push({"root":response.data.root});
                                self.contentError = false;
                            }else {
                                self.contentError = true;
                            }
                        },
                        function error(response) {
                            self.contentError = true;
                            Growl('error','Error','The server returned an error! '+response.data);
                        });
            }
        };

        self.save2 = function() {
            if(self.metadataValues && self.metadataValues.length>0){
                Examind.metadata.create(self.options.selectedProfile, 'DOC',
                    $.extend({},{"nodeTypes":self.nodeTypes},self.metadataValues[0])).then(
                    function(response) {//success
                        self.close();
                        Growl('success','Success','Metadata created with success!');
                    },
                    function(response) {//error
                        var msg='';
                        if(response.data && response.data.errorMessage) {
                            msg = response.data.errorMessage;
                        }
                        Growl('error','Error','Failed to create metadata because the server returned an error! '+msg);
                    }
                );
            }
        };

        self.dismiss = function () {
            $modalInstance.dismiss('close');
        };

        self.close = function () {
            $modalInstance.close();
        };

        $controller('EditMetadataController', {$scope: self});
    })

    .controller('MetadataManagerController', function($scope,Growl,$window,$modal,
                                                      $translate,SelectionApi,metadataQuery,
                                                      Dashboard,Permission,$filter,Examind) {

        var self = this;

        self.permission = Permission;

        self.selectionApi = SelectionApi;

        self.options = {
            'selectedPeriod' : '_all',
            'groupsList' : [], //must be empty since groups implementation is not present in this project.
            'selectedGroup':null,
            'fullStats':{
                'general':{
                    total:0,
                    validated:0,
                    notValid:0,
                    waitingToValidate:0,
                    notPublish:0,
                    published:0,
                    waitingToPublish:0
                },
                'repartitionProfiles':[],
                'completionPercents':[],
                'groupsStatList':[],
                'contributorsStatList':[]
            },
            'scopeGroups':null,
            'scopeContributors' : null
        };

        self.getUserGroupName = function() {
            if(self.options.groupsList && Permission.getAccount()){
                var groupId = Permission.getAccount().userGroupId;
                if(groupId) {
                    for(var i=0;i<self.options.groupsList.length;i++) {
                        var group= self.options.groupsList[i];
                        if(group.id === groupId) {
                            return group.name;
                        }
                    }
                }
            }
            return null;
        };

        /**
         * Init of all objects with default values.
         */
        self.init = function() {
            // load full stats with default params
            Permission.promise.then(function(){
                var account = Permission.getAccount();
                if (Permission.hasPermission('admin')) {
                    computeFullStats({});
                } else {
                    var toSend = {"filters":[]};
                    if(account.userGroupId) {
                        toSend.filters.push({"field":"group","value":account.userGroupId});
                        self.options.selectedGroup = account.userGroupId;
                    }
                    computeFullStats(toSend);
                }
            });
        };

        self.init();

        function computeFullStats(params) {
            Examind.metadata.getFilteredStats(params,{type:'DOC'}).then(
                function success(response){
                    //GENERAL stats
                    if(response.data.general) {
                        self.options.fullStats.general = response.data.general;
                    }

                    //PROFILES chart repartition
                    self.options.fullStats.repartitionProfiles = [];
                    if(response.data.repartitionProfiles) {
                        var profileList = response.data.repartitionProfiles.filter(function(p){
                            return p.name !== null;
                        }).map(function(p){
                            return p;
                        });
                        profileList.sort(function (p1, p2) {
                            return p1.name.localeCompare(p2.name);
                        });
                        for(var i=0;i<profileList.length;i++){
                            var p = profileList[i];
                            if(p.name) {
                                var translatedProfile = $filter('translate')(p.name);
                                self.options.fullStats.repartitionProfiles.push([translatedProfile, p.count]);
                            }
                        }
                    }

                    //Completion chart categories percent count
                    if(response.data.completionPercents) {
                        self.options.fullStats.completionPercents = response.data.completionPercents;
                        self.options.fullStats.completionPercents.unshift('fiches');
                    }else {
                        self.options.fullStats.completionPercents = [];
                    }

                    //init dashboard table for groups and contributors
                    self.options.scopeGroups = $scope.$new();
                    self.options.scopeGroups.wrap = {'ordertype':'name','nbbypage':5};
                    if(response.data.groupsStatList) {
                        self.options.fullStats.groupsStatList = response.data.groupsStatList.map(function(item){
                            return {
                                "id":item.group.id,
                                "name":item.group.name,
                                "tovalidate":item.toValidate,
                                "topublish":item.toPublish,
                                "published":item.published
                            };
                        });
                    }else {
                        self.options.fullStats.groupsStatList = [];
                    }
                    Dashboard(self.options.scopeGroups, self.options.fullStats.groupsStatList, true);

                    self.options.scopeContributors = $scope.$new();
                    self.options.scopeContributors.wrap = {'ordertype':'name','nbbypage':5};
                    if(response.data.contributorsStatList) {
                        self.options.fullStats.contributorsStatList = response.data.contributorsStatList.map(function(item){
                            return {
                                "id":item.contributor.id,
                                "name":item.contributor.lastname,
                                "group":item.contributor.group?item.contributor.group.name:'',
                                "tovalidate":item.toValidate,
                                "topublish":item.toPublish,
                                "published":item.published
                            };
                        });
                    }else {
                        self.options.fullStats.contributorsStatList = [];
                    }
                    Dashboard(self.options.scopeContributors, self.options.fullStats.contributorsStatList, true);


                    //load chart plots
                    self.initPlots();
                },
                function error(response){
                    Growl('error','Error','An error occurred when getting stats from server!');
                }
            );
        }

        self.initPlots = function() {
            //profiles distributions
            var chart1 = c3.generate({
                bindto: '#chart1',
                size: {height: 170,width: 230},
                color: {
                    pattern: ['#1f77b4', '#aec7e8', '#ff7f0e', '#ffbb78', '#2ca02c',
                        '#98df8a', '#d62728', '#ff9896', '#9467bd', '#c5b0d5',
                        '#8c564b', '#c49c94', '#e377c2', '#f7b6d2', '#7f7f7f',
                        '#c7c7c7', '#bcbd22', '#dbdb8d', '#17becf', '#9edae5']
                },
                data: {
                    columns: self.options.fullStats.repartitionProfiles,
                    type : 'pie'
                },
                legend: {show: false}
            });

            // chart for completion percentages
            var chart2 = c3.generate({
                bindto: '#chart2',
                size: {height: 170,width: 270},
                padding: {left: 35},
                color: {
                    pattern: ['#1f77b4']
                },
                data: {
                    columns: [self.options.fullStats.completionPercents],
                    type: 'bar'
                },
                axis: {
                    x: {
                        type: 'category',
                        categories: ['10 %', '20 %', '30 %', '40 %', '50 %', '60 %', '70 %', '80 %', '90 %', '100 %'],
                        tick: {rotate: 75,multiline: false},
                        height: 60
                    }
                },
                grid: {
                    y: {show: true}
                },
                legend: {show: false}
            });

            // chart for published and not published metadata
            var chart3 = c3.generate({
                bindto: '#chart3',
                size: {height: 170,width: 280},
                padding: {left: 35,right:110},
                bar: {width: 15},
                color: {
                    pattern: ['#1f77b4', '#f45a5a', '#82d760']
                },
                grid: {
                    y: {show: true}
                },
                legend: {position: 'right'},
                data: {
                    columns: [
                        ['Published', self.options.fullStats.general.published,0],
                        ['Not valid', 0,self.options.fullStats.general.notValid],
                        ['Valid', 0,(self.options.fullStats.general.total - self.options.fullStats.general.published - self.options.fullStats.general.notValid)]
                    ],
                    type: 'bar'
                },
                tooltip: {show: false},
                axis: {
                    x: {
                        type: 'category',
                        categories: ['published', 'not published']
                    }
                }
            });
        };

        self.changePeriod = function() {
            var toSend = {"filters":[]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            if(self.options.selectedGroup) {
                toSend.filters.push({"field":"group","value":self.options.selectedGroup});
            }
            computeFullStats(toSend);
        };

        self.changeGroup = function() {
            var toSend = {"filters":[]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            if(self.options.selectedGroup) {
                toSend.filters.push({"field":"group","value":self.options.selectedGroup});
            }
            computeFullStats(toSend);
        };

        self.showToValidate = function() {
            //reset query to default and add new filters
            var toSend = {"filters":[{"field":"validated","value":"false"},{"field":"validation_required","value":"REQUIRED"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            if(self.options.selectedGroup) {
                toSend.filters.push({"field":"group","value":self.options.selectedGroup});
            }
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showToValidateGroup = function(groupId) {
            var toSend = {"filters":[{"field":"validated","value":"false"},{"field":"validation_required","value":"REQUIRED"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"group","value":groupId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showToValidateContrib = function(contribId) {
            var toSend = {"filters":[{"field":"validated","value":"false"},{"field":"validation_required","value":"REQUIRED"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"owner","value":contribId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showToPublish = function() {
            //reset query to default and add new filters
            var toSend = {"filters":[{"field":"published","value":"false"},{"field":"validated","value":"true"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            if(self.options.selectedGroup) {
                toSend.filters.push({"field":"group","value":self.options.selectedGroup});
            }
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showToPublishGroup = function(groupId) {
            var toSend = {"filters":[{"field":"published","value":"false"},{"field":"validated","value":"true"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"group","value":groupId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showToPublishContrib = function(contribId) {
            var toSend = {"filters":[{"field":"published","value":"false"},{"field":"validated","value":"true"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"owner","value":contribId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showPublishedGroup = function(groupId) {
            var toSend = {"filters":[{"field":"published","value":"true"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"group","value":groupId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        self.showPublishedContrib = function(contribId) {
            var toSend = {"filters":[{"field":"published","value":"true"}]};
            if(self.options.selectedPeriod){
                toSend.filters.push({"field":"period","value":self.options.selectedPeriod});
            }
            toSend.filters.push({"field":"owner","value":contribId});
            $scope.mdCtrl.query = angular.extend(toSend,angular.copy(metadataQuery));
            sendQueryAndSwitchTab();
        };

        /**
         * Send query and apply selection to metadata tab.
         */
        function sendQueryAndSwitchTab() {
            self.selectionApi.clear();
            $scope.mdCtrl.setPage(1);
            Examind.metadata.searchIds($scope.mdCtrl.query,{type:'DOC'}).then(
                function success(response) {
                    $scope.mdCtrl.selectedAll = true;
                    $scope.mdCtrl.allFilteredIds = response.data;
                    if(response.data.list){
                        angular.forEach(response.data.list, function(item) {
                            self.selectionApi.add(item);
                        });
                    }
                    $scope.mdCtrl.currentTab = 'metadata';
                }
            );
        }

        self.sortBy = function(wraper, field) {
            wraper.ordertype=field;
            wraper.orderreverse=!wraper.orderreverse;
        };

    });
