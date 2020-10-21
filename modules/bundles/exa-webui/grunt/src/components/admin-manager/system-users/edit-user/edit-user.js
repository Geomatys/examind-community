angular.module('examind.components.admin.manager.system.users.edit', [])
    .controller('SystemUsersEditController', SystemUsersEditController)
    .directive('systemUsersEdit', systemUsersEditDirective);

function systemUsersEditDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-users/edit-user/edit-user.html",
        controller: 'SystemUsersEditController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function SystemUsersEditController($rootScope, $scope, $modalInstance, $cookieStore, Growl,
                                   cfpLoadingBar, currentAccount, user, roles, Examind) {
    var self = this;

    $scope.user = user.data;
    
    $scope.userRole = $scope.user.roles[0];

    $scope.roles = roles.data;

    $scope.isNewUser = !user.data.id;

    $scope.disableEditLogin = (currentAccount.login === $scope.user.login);

    $scope.password = "";

    $scope.password2 = "";

    //set password required : true case create user, false otherwise
    $scope.passwordRequired = Boolean(!$scope.user.id);

    //enable role
    $scope.enableRole = true;

    $scope.close = function () {
        $modalInstance.dismiss('close');
    };

    $scope.save = function () {
        var formData = new FormData(document.getElementById('userForm'));
        if (!formData.has("role")) {
            formData.append("role", $scope.userRole);
        }
        if ($scope.user.id) {
            //edit
            $.ajax({
                headers: {
                    'access_token': Examind.authentication.getToken()
                },
                url: $cookieStore.get('cstlUrl') + 'API/internal/users',
                type: 'POST',
                data: formData,
                async: false,
                cache: false,
                contentType: false,
                processData: false,
                beforeSend: function () {
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                },
                success: function (result) {
                    cfpLoadingBar.complete();
                    $modalInstance.close();
                },
                error: function (result) {
                    Growl('error', 'Error', 'Unable to edit user!');
                    cfpLoadingBar.complete();
                }
            });
        } else {
            //add
            $.ajax({
                headers: {
                    'access_token': Examind.authentication.getToken()
                },
                url: $cookieStore.get('cstlUrl') + 'API/internal/users/add',
                type: 'POST',
                data: formData,
                async: false,
                cache: false,
                contentType: false,
                processData: false,
                beforeSend: function () {
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                },
                success: function (result) {
                    cfpLoadingBar.complete();
                    $modalInstance.close();
                },
                error: function (result) {
                    Growl('error', 'Error', 'Unable to add user!');
                    cfpLoadingBar.complete();
                }
            });
        }
    };

}
