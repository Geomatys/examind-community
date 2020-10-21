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

/* Controllers */
/*jshint -W079 */
var dataNotReady = function(){alert("data not ready");};

angular.module('cstl-main', ['cstl-restapi', 'cstl-services', 'pascalprecht.translate', 'ui.bootstrap.modal', 'examind-instance'])

    .controller('HeaderController', function ($rootScope, $scope, $http, Examind, $idle, $modal, CstlConfig,
                                              AppConfigService, Permission) {

        $scope.navigationArray = CstlConfig['cstl.navigation'];

        $scope.permissionService = Permission;

        function closeModals() {
            if ($scope.warning) {
                $scope.warning.close();
                $scope.warning = null;
            }

            if ($scope.timedout) {
                $scope.timedout.close();
                $scope.timedout = null;
            }
        }

        $scope.$on('$idleStart', function() {
            // the user appears to have gone idle
            closeModals();
            $scope.warning = $modal.open({
                templateUrl: 'views/idle/warning-dialog.html',
                windowClass: 'modal-warning'
            });
        });

        $scope.$on('$idleTimeout', function() {
            // the user has timed out (meaning idleDuration + warningDuration has passed without any activity)
            // this is where you'd log them
            closeModals();
            $scope.timedout = $modal.open({
                templateUrl: 'views/idle/timedout-dialog.html',
                windowClass: 'modal-danger'
            });
        });

        $scope.$on('$idleEnd', function() {
            // the user has come back from AFK and is doing stuff. if you are warning them, you can use this to hide the dialog
            closeModals();
            //renew token
            //Examind.authentication.renewToken();
        });

        $scope.$on('$keepalive', function() {
            // keep the user's session alive
            //renew token
            Examind.authentication.renewToken();
        });

        $scope.logout = function(){
            if ($scope.cstlLogoutURL) {
                window.location.href = $scope.cstlLogoutURL;
            } else {
                Examind.authentication.logout().then(
                    function(response){
                        window.location.href="index.html";
                    }
                );
            }
        };

        $scope.canShow = function (nav) {
            return $scope.config && $scope.config.navigationListVisibility && $scope.config.navigationListVisibility[nav];
        };

        AppConfigService.getConfig(function(config) {
            $scope.cstlProfileURL = config.cstlProfileURL || '#/profile';

            if (config.cstlLogoutURL) {
                $scope.cstlLogoutURL =  config.cstlLogoutURL;
            }
            $scope.config = config;

            var cstlRefreshURL = config.cstlRefreshURL;
            if (cstlRefreshURL) {
                Examind.authentication.setTokenRefreshURL(cstlRefreshURL);
            }

            if (config["token.life"]) {
                Examind.authentication.setTokenLifespan(config["token.life"]);
            }

            Examind.authentication.account().then(function(response){
                $scope.firstname = response.data.firstname;
                $scope.lastname = response.data.lastname;
            });
        });
    })

    .controller('FooterController', function($scope,CstlConfig,BuildService) {
        var self = this;
        self.cstlVersion=CstlConfig['cstl.version'];
        $scope.buildInfo = BuildService;
    })

    .controller('MainController', function($scope, Growl, Examind) {
        $scope.countStats = function() {

            Examind.services.getInstances().then(
                function(response) {
                    var instances = response.data;
                    var count = 0;
                    for (var i=0; i<instances.length; i++) {
                        if (instances[i].status === 'STARTED' && instances[i].type !== 'WEBDAV') {
                            count++;
                        }
                    }
                    $scope.nbservices = count;
                },
                function() {
                    $scope.nbservices = 0;
                    Growl('error', 'Error', 'Unable to count services');
                }
            );

            Examind.datas.getCount().then(
                function(response) {
                    $scope.nbdata = response.data.count;
                },
                function() {
                    $scope.nbdata = 0;
                    Growl('error', 'Error', 'Unable to count data');
                }
            );

            Examind.tasks.getCount().then(
                function(response) {
                    $scope.nbprocess = response.data.count;
                },
                function() {
                    $scope.nbprocess = 0;
                    Growl('error', 'Error', 'Unable to count process');
                }
            );

            Examind.users.getCount().then(
                function(response) {
                    $scope.nbusers = response.data.count;
                },
                function() {
                    $scope.nbusers = 1;
                    Growl('error', 'Error', 'Unable to count users');
                }
            );
        };
    })

    .controller('UserAccountController', function($scope, $rootScope, $location, $cookieStore, $translate, Growl, cfpLoadingBar, user, roles, Examind) {
        $scope.user = user.data;
        $scope.disableEditLogin = true;
        $scope.roles = roles.data;
        $scope.userRole = $scope.user.roles[0];
        
        $scope.password = "";
        $scope.password2 = "";

        //disabled role select
        $scope.enableRole = false;

        //update language when update select tag
        $scope.shouldUpdateLanguage = true;

        $scope.save = function(){
            var formData = new FormData(document.getElementById('userForm'));
            if (!formData.has("role")) {
                formData.append("role", $scope.userRole);
            }
            $.ajax({
                headers: {
                    'access_token': Examind.authentication.getToken()
                },
                url: $cookieStore.get('cstlUrl') + 'API/internal/users/current',
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
                success: function(result) {
                    Growl('success', 'Success', 'The changes have been successfully applied!');
                    cfpLoadingBar.complete();
                    $location.url('/');
                },
                error: function(result){
                    Growl('error', 'Error', 'Unable to edit user!');
                    cfpLoadingBar.complete();
                }
            });
        };

        $scope.changeLanguage = function(){
            $translate.use($scope.user.locale);
        };
    })

    .controller('LanguageController', function($scope, $translate) {

        $scope.currentLang = $translate.use();

        $scope.changeLanguage = function () {
            $translate.use($scope.currentLang);
        };
    })

    .controller('MenuController', function($scope) {

    })

    .controller('ModalConfirmController', function($scope,keyMsg) {
        $scope.keyMsg = keyMsg;
    })

    .controller('LoginController', function($scope, $location, AuthService) {
        $scope.rememberMe = true;
        $scope.login = function () {
            AuthService.login({
                username: $scope.username,
                password: $scope.password,
                rememberMe: $scope.rememberMe,
                success: function () {
                    $location.path('');
                }
            });
        };
    })

    .controller('ModalInstanceCtrl', function($scope, $modalInstance){
        $scope.ok = function () {
            $modalInstance.close();
        };

        $scope.cancel = function () {
            $modalInstance.dismiss();
        };
    })

    .controller('navCtrl', function($scope, $location) {
        $scope.navClass = function (page) {
            var currentRoute = $location.path().split('/')[1] || 'home';
            return page === currentRoute ? 'menu-selected' : '';
        };
    });

