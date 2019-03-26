angular.module('examind.components.admin.manager.system.state', [
    'examind-instance',
    'cstl-restapi'
]).controller('SystemStateController', SystemStateController)
    .directive('systemState', systemStateDirective);

function systemStateDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-state/system-state.html",
        controller: 'SystemStateController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function SystemStateController(Metrics, Examind) {
    var self = this;

    self.init = function () {
        self.metrics = Metrics.get();
    };

    self.rungc = function () {
        Examind.admin.runGC()
            .then(self.init);
    };

}
