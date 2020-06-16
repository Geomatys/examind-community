angular.module('examind.components.admin.manager.build.info', [
    'examind-instance',
    'cstl-restapi'
]).controller('BuildInfoController', BuildInfoController)
    .directive('buildInfo', BuildInfoDirective);

function BuildInfoDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/build-info/build-info.html",
        controller: 'BuildInfoController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function BuildInfoController(BuildService) {
    var self = this;

    self.buildInfo = BuildService;

}
