angular.module('examind.components.admin.manager.system.users', [
    'examind.paged.search.service',
    'examind.components.admin.manager.system.users.edit',
    'examind-instance'
])
    .controller('SystemUsersController', SystemUsersController)
    .directive('systemUsers', systemUsersDirective);

function systemUsersDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-users/system-users.html",
        controller: 'SystemUsersController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function SystemUsersController($modal, PagedSearchService, Examind, Permission) {
    var self = this;

    // Create an instance of PagedSearchService to handle the search and filter
    self.pagedSearchService = new PagedSearchService();

    /**
     * Define the search function of the service instance
     */
    self.pagedSearchService.search = function () {
        Examind.users.searchUsers(self.pagedSearchService.query)
            .then(function (response) {
                self.page = response.data;
            });
    };

    self.edit = function (id) {
        $modal.open({
            templateUrl: 'components/admin-manager/system-users/edit-user/edit-user.html',
            controller: 'SystemUsersEditController',
            resolve: {
                'currentAccount': function (Permission) {
                    return Permission.getAccount();
                },
                'user': function () {
                    return Examind.users.getUser(id);
                },
                'roles': function (Examind) {
                    return Examind.roles.getRoles();
                }
            }
        }).result.then(function () {
            self.pagedSearchService.search();
        });
    };

    self.add = function () {
        $modal.open({
            templateUrl: 'components/admin-manager/system-users/edit-user/edit-user.html',
            controller: 'SystemUsersEditController',
            resolve: {
                'currentAccount': function (Permission) {
                    return Permission.getAccount();
                },
                'user': function () {
                    return {data: {roles: [""]}};
                },
                'roles': function (Examind) {
                    return Examind.roles.getRoles();
                }
            }
        }).result.then(function () {
            self.pagedSearchService.search();
        });
    };

    self.updateValidation = function (id) {
        Examind.users.activate(id)
            .then(function () {
                self.pagedSearchService.search();
            });
    };

    self.init = function () {
        self.pagedSearchService.query.size = 5;
        self.pagedSearchService.query.sort.field = 'cstl_user.login';
        self.pagedSearchService.query.sort.order = 'ASC';
        self.pagedSearchService.search();
    };

    self.init();

}
