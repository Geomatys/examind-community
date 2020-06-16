angular.module('examind.components.admin.manager', [
    'examind.components.admin.manager.system.state',
    'examind.components.admin.manager.system.logs',
    'examind.components.admin.manager.system.contact',
    'examind.components.admin.manager.system.providers',
    'examind.components.admin.manager.system.users',
    'examind.components.admin.manager.build.info'
])
    .controller('AdminManagerController', AdminManagerController)
    .directive('adminManager', adminManagerDirective);

function adminManagerDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/admin-manager.html",
        controller: 'AdminManagerController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function AdminManagerController() {
    var self = this;

    // Config to show the S63 options
    self.config = {
        s63Options: false
    };

    self.currentView = 'system_state';

    self.changeView = function (view) {
        self.currentView = view;
    };
}
