angular.module('examind.components.admin.manager.system.providers', [
    'webui-utils',
    'examind-instance',
    'examind.components.admin.manager.system.providers.modal.config'
])
    .controller('SystemProvidersController', SystemProvidersController)
    .directive('systemProviders', systemProvidersDirective);

function systemProvidersDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-providers/system-providers.html",
        controller: 'SystemProvidersController',
        scope: {}
    };
}

function SystemProvidersController($scope,$modal, Examind, Growl,OldDashboard) {


    $scope.wrap = {};
    $scope.wrap.ordertype = 'id';

    $scope.init = function() {
        Examind.providers.getProviders().then(
            function(response) {
                OldDashboard($scope, response.data, true);
            },
            function(response){
                Growl('error','Error','Search failed:');
            }
        );
    };

    $scope.showConfig = function (provider) {
        $modal.open({
            templateUrl: 'components/admin-manager/system-providers/modal-config/modal-config.html',
            controller: 'ModalProviderConfigController',
            resolve: {
                'details': function () {
                    return provider.config;
                }
            }
        });
    };

    $scope.reloadProvider = function (provider) {
        provider.reloading = true;
        Examind.providers.reload(provider.id)
            .finally(function () {
                provider.reloading = false;
            });
    };

}
