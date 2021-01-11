
/**
 * Angular login app.
 */
var cstlLoginApp = angular.module("cstlLoginApp",
        ['pascalprecht.translate',
        'ui.bootstrap.modal',
        'cstl-directives',
        'webui-utils',
        'webui-config',
        'examind-factory']);

cstlLoginApp.config(['$translateProvider', '$translatePartialLoaderProvider',
    function ($translateProvider, $translatePartialLoaderProvider) {
        // Initialize angular-translate
        $translateProvider.useLoader('$translatePartialLoader', {
            urlTemplate: 'i18n/{lang}/{part}.json'
        });
        $translatePartialLoaderProvider.addPart('ui-menu');
        $translatePartialLoaderProvider.addPart('ui');
        $translateProvider.preferredLanguage('en');

        // remember language
        $translateProvider.useLocalStorage();
    }
]);

/**
 * Directive to fix a bug with form auto filling by navigator and angular.
 */
cstlLoginApp.directive('formAutofillFix', function() {
    return function(scope, elem, attrs) {
        elem.prop('method', 'POST');
        // Fix autofill issues where Angular doesn't know about autofilled inputs
        if(attrs.ngSubmit) {
            setTimeout(function() {
                elem.unbind('submit').bind('submit',function(e) {
                    e.preventDefault();
                    elem.find('input, textarea, select').trigger('input').trigger('change').trigger('keydown');
                    scope.$apply(attrs.ngSubmit);
                });
            }, 0);
        }
    };
});

/**
 * Login controller.
 */
cstlLoginApp.controller("login", function($scope, $http, $modal, AppConfigService, ExamindFactory){

    var cstlUrl;
    $scope.formInputs = {
        username:undefined,
        password:undefined
    };

    $scope.login = function(target){
        
        var exa = ExamindFactory.create(window.localStorage.getItem('cstlUrl'));
        
        exa.authentication.login($scope.formInputs.username,$scope.formInputs.password)
            .then(function(resp){
                jQuery('#msg-error').hide();
                if (resp.status === 200) {
                    window.location.href= target ? target : "admin.html";
                }
            }, function(resp){
                jQuery(".msg_auth_error").hide();
                if(resp) {
                    if(resp.status === 401 ) {
                        jQuery('#msg-error').show('fade');
                    }else if (resp.status === 403) {
                        jQuery('#msg-error2').show('fade');
                    }
                }else {
                    jQuery('#msg-error').show('fade');
                }
            });
    };

    $scope.forgotPassword = function(){
        $modal.open({
            templateUrl: 'views/forgot-password.html',
            controller: 'forgotPasswordController as fpCtrl',
            size: 'sm'
        });
    };

    AppConfigService.getConfigProperty('cstl', function (val) {
        cstlUrl = val;
        window.localStorage.setItem('cstlUrl', val);
    });
})
.controller("forgotPasswordController", function($scope, $modalInstance, $http, $translate, Growl, ExamindFactory){
        var self = this;
        self.userEmail = '';

        self.validate = function(){
            
            var exa = ExamindFactory.create(window.localStorage.getItem('cstlUrl'));
            
            exa.authentication.forgotPassword(self.userEmail).then(
                function(response){
                   $translate(['Success', 'password.forgot.success']).then(function (translations) {
                       Growl('success', translations.Success, translations['password.forgot.success']);
                   });

                   $modalInstance.close();
               },
               function(response) {
                   $translate(['Error', 'password.forgot.error']).then(function (translations) {
                       Growl('error', translations.Error, translations['password.forgot.error']);
                   });
               }
            );
   };
});
