angular.module('examind.components.admin.manager.system.logs', [
    'webui-utils',
    'examind-instance'
])
    .controller('SystemLogsController', SystemLogsController)
    .directive('systemLogs', systemLogsDirective);

function systemLogsDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-logs/system-logs.html",
        controller: 'SystemLogsController',
        //controllerAs: "ctrl",
        self: {}
    };
}

function SystemLogsController($scope,Examind,Growl,OldDashboard) {
    
    $scope.wrap = {};
    $scope.wrap.ordertype = 'name';

    $scope.init = function () {
        Examind.admin.getLoggers()
            .then(function (response) {
                    OldDashboard($scope, response.data, true);
                },
                function (response) {
                    Growl('error', 'Error', 'Search failed:');
                }
            );
    };

    $scope.changeLevel = function (name, level) {
        Examind.admin.updateLogger({name: name, level: level})
            .then($scope.init);
    };

}
