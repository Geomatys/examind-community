angular.module('examind.components.footer.bar', [])
    .controller('FooterBarController', FooterBarController)
    .directive('footerBar', footerBarDirective);

function footerBarDirective() {
    return {
        restrict: "E",
        controller: 'FooterBarController',
        controllerAs: "footerBarCtrl",
        templateUrl: 'components/footer-bar/footer-bar.html',
        scope: {
            projectName: '@?'
        }
    };
}

function FooterBarController($scope, CstlConfig, BuildService) {
    var self = this;

    self.projectName = $scope.projectName || '';

    self.cstlVersion = CstlConfig['cstl.version'];

    self.year = new Date().getFullYear();

    self.buildInfo = BuildService;

}