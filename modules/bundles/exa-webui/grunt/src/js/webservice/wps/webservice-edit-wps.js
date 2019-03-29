angular.module('cstl-webservice-edit-wps', ['cstl-restapi', 'cstl-services', 'pascalprecht.translate', 'ui.bootstrap.modal'])

    .controller('WPSEditController', function($scope, $routeParams,
                                              $modal, Dashboard, Growl, Examind,
                                              $cookieStore) {
        $scope.wrap = {};
        $scope.type = $routeParams.type;
        $scope.serviceIdentifier = $routeParams.id;
        $scope.cstlUrl = $cookieStore.get('cstlUrl');
        $scope.url = $scope.cstlUrl + "WS/" + $scope.type + "/" + $routeParams.id;

        $scope.values = {
            selectedChild : null
        };

        $scope.initCtrl = function() {
            Examind.ogcServices.getConfig($scope.type, $routeParams.id).then(function(response){
                $scope.config = response.data;
            });
            $scope.processList = Examind.wps.getProcess($routeParams.id).then(
                function(response) {
                    Dashboard($scope, response.data, false);
                }
            );
        };

        $scope.selectChild = function(item) {
            if (item && $scope.values.selectedChild && $scope.values.selectedChild.id === item.id) {
                $scope.values.selectedChild = null;
            } else {
                $scope.values.selectedChild = item;
            }
        };

        $scope.clearFilters = function(){
            $scope.wrap.ordertype= 'name';
            $scope.wrap.orderreverse=false;
            $scope.wrap.filtertext='';
            $scope.selected=null;
        };

        $scope.showProcessToAdd = function() {
            var modal = $modal.open({
                templateUrl: 'views/webservice/wps/modalAddProcess.html',
                controller: 'WPSAddProcessModalController',
                resolve: {
                    exclude: function() { return $scope.processList; },
                    service: function() { return $scope.serviceIdentifier; },
                    processList : function(Examind){ 
                        return Examind.wps.getAllProcess();
                    }
                }
            });
            modal.result.then(function() {
                Examind.ogcServices.restart($scope.type, $routeParams.id, true).then(
                    function() {
                        Growl('success','Success','Service '+ $scope.serviceIdentifier +' successfully reloaded');
                        $scope.initCtrl();
                    },
                    function() {
                        Growl('error','Error','Service '+ $scope.serviceIdentifier +' reload failed');
                    }
                );
            });
        };

        $scope.removeCategory = function() {
            if (!$scope.selected) {
                return;
            }
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.processCategory";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    Examind.wps.removeAuthority($routeParams.id, $scope.selected.name).then(
                    function(){
                        Growl('success', 'Success', 'Category of process ' + $scope.selected.name + ' successfully removed from this service.');
                        $scope.selected=null;
                        $scope.values.selectedChild = null;
                        //reload service then reload list of processes for this wps
                        Examind.ogcServices.restart($scope.type, $routeParams.id, true).then(
                            function() {
                                Growl('success','Success','Service '+ $scope.serviceIdentifier +' successfully reloaded');
                                $scope.initCtrl();
                            },
                            function() {
                                Growl('error','Error','Service '+ $scope.serviceIdentifier +' reload failed');
                            }
                        );
                    }, function(){
                            Growl('error', 'Error', 'Unable to remove the category of processes ' + $scope.selected.name);
                        }
                    );
                }
            });
        };

        $scope.removeProcess = function() {
            if (!$scope.values.selectedChild) {
                return;
            }
            var dlg = $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg':function(){return "dialog.message.confirm.delete.process";}
                }
            });
            dlg.result.then(function(cfrm){
                if(cfrm){
                    Examind.wps.removeProcess($routeParams.id, $scope.selected.name, $scope.values.selectedChild.id).then(
                        function(){
                            Growl('success', 'Success', 'Process ' + $scope.values.selectedChild.id + ' successfully removed from this service.');
                            $scope.values.selectedChild = null;
                            //reload service then reload list of processes for this wps
                            Examind.ogcServices.restart($scope.type, $routeParams.id, true).then(
                                function() {
                                    Growl('success','Success','Service '+ $scope.serviceIdentifier +' successfully reloaded');
                                    $scope.initCtrl();
                                },
                                function() {
                                    Growl('error','Error','Service '+ $scope.serviceIdentifier +' reload failed');
                                }
                            );
                        }, function(){
                            Growl('error', 'Error', 'Unable to remove the process ' + $scope.values.selectedChild.id);
                        }
                    );
                }
            });
        };

        $scope.initCtrl();

    })
    .controller('WPSAddProcessModalController', function($scope, Dashboard, $modalInstance,
                                                         service, exclude, Growl, $filter, processList, Examind) {
        $scope.wrap = {};
        $scope.wrap.nbbypage = 5;
        $scope.dataSelect={all:false};
        $scope.serviceIdentifier = service;
        $scope.exclude = exclude;
        $scope.values = {
            listSelect : []
        };

        $scope.options = {
            all : {name : "label.all", _name:"", processes : []},
            allProcesses : [],
            selectedAuthority : null
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

        $scope.initProcessList = function() {
            $scope.options.allProcesses = processList.data.map(function(authObj){
                authObj._name = authObj.name;
                return authObj;
            });
            $scope.options.all.processes = [];
            angular.forEach(processList.data, function(authObj){
                var authName = authObj.name;
                var procList = authObj.processes.map(function(p){
                    return {
                        name : p.id,
                        description : p.description,
                        auth : authName
                    };
                });
                $scope.options.all.processes = $scope.options.all.processes.concat(procList);
            });
            $scope.options.allProcesses.push($scope.options.all);
            $scope.options.selectedAuthority = $scope.options.all;
            $scope.initDashboardList();
        };

        $scope.initDashboardList = function() {
            if($scope.options.selectedAuthority){
                var dashList;
                if($scope.options.selectedAuthority === $scope.options.all) {
                    dashList = $scope.options.selectedAuthority.processes;
                }else {
                    dashList = $scope.options.selectedAuthority.processes.map(function(p) {
                        return {
                            name : p.id,
                            description : p.description,
                            auth : $scope.options.selectedAuthority.name
                        };
                    });
                }
                Dashboard($scope, dashList, true);
                $scope.values.listSelect = [];
                $scope.dataSelect.all = false;
                $scope.wrap.ordertype='name';
                $scope.wrap.orderreverse=false;
                $scope.wrap.filtertext='';
            }
        };


        /**
         * Proceed to select all items of modal dashboard
         * depending on the property of checkbox selectAll.
         */
        $scope.selectAllData = function() {
            if($scope.wrap.fullList) {
                var array = $filter('filter')($scope.wrap.fullList, {'$': $scope.wrap.filtertext},$scope.wrap.matchExactly);
                $scope.values.listSelect = ($scope.dataSelect.all) ? array.slice(0) : [];
            }
        };

        /**
         * binding call when clicking on each row item.
         */
        $scope.toggleDataInArray = function(item){
            var itemExists = false;
            for (var i = 0; i < $scope.values.listSelect.length; i++) {
                if ($scope.values.listSelect[i] === item) {
                    itemExists = true;
                    $scope.values.listSelect.splice(i, 1);//remove item
                    break;
                }
            }
            if(!itemExists){
                $scope.values.listSelect.push(item);
            }
            $scope.dataSelect.all=($scope.values.listSelect.length === $scope.wrap.fullList.length);
        };

        /**
         * Returns true if item is in the selected items list.
         */
        $scope.isInSelected = function(item){
            for(var i=0; i < $scope.values.listSelect.length; i++){
                if($scope.values.listSelect[i] === item){
                    return true;
                }
            }
            return false;
        };

        /**
         * function to add data to service
         */
        $scope.choose = function() {
            if ($scope.values.listSelect.length === 0) {
                return;
            }
            var toSend = {registries : []};
            toSend.registries = buildList($scope.values.listSelect);
            Examind.wps.addProcess($scope.serviceIdentifier,toSend).then(
                function(){//on success
                    Growl('success', 'Success', 'Process added with success!');
                    $scope.close();
                },
                function(){//on error
                    Growl('error', 'Error', 'Failed to add process to wps.');
                    $scope.dismiss();
                }
            );
        };

        // Private function
        function buildList(list) {
            var tree = {};
            for (var p=0; p< list.length; p++) {
                var process = list[p];
                var authName = process.auth;
                var processName = process.name;
                var processDesc = process.description;
                if(!tree[authName]){
                    tree[authName] = [];
                }
                tree[authName].push({
                    id:processName,
                    description:processDesc
                });
            }
            var result = [];
            for (var auth in tree) {
                if(tree.hasOwnProperty(auth)){
                    result.push({
                        'name' : auth,
                        'processes' : tree[auth]
                    });
                }
            }
            return result;
        }

        $scope.initProcessList();
    });