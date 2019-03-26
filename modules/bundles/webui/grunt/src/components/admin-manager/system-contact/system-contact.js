angular.module('examind.components.admin.manager.system.contact', [
    'examind-instance',
])
    .controller('SystemContactController', SystemContactController)
    .directive('systemContact', systemContactDirective);

function systemContactDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-contact/system-contact.html",
        controller: 'SystemContactController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function SystemContactController(Examind) {
    var self = this;

    Examind.admin.getContact()
        .then(function (response) {
            self.data = response.data;
        });

    self.save = function () {
        Examind.admin.updateContact(self.data)
            .then(onSuccess, onError);
    };

    function onSuccess() {
        self.error = null;
        self.success = 'OK';
        Examind.admin.getContact()
            .then(function (response) {
                self.data = response.data;
            });
    }

    function onError() {
        self.success = null;
        self.error = "ERROR";
    }

}
