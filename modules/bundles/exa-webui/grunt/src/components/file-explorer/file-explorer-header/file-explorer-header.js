angular.module('examind.components.file.explorer.header', [])
    .controller('FileExplorerHeaderController', FileExplorerHeaderController)
    .directive('fileExplorerHeader', fileExplorerHeaderDirective);

function fileExplorerHeaderDirective() {
    return {
        restrict: "E",
        require: "^fileExplorer",
        controller: 'FileExplorerHeaderController',
        controllerAs: 'explorerHeaderCtrl',
        templateUrl: 'components/file-explorer/file-explorer-header/file-explorer-header.html',
        scope: {
            setViewTemplate: "&",
            isSelectedViewTemplate: "&",
            upDir: "&",
            config: "=",
            clear: "&",
            disableUpDir: "&",
            fileExplorerState: "=",
            sortBy: "&",
            getSortIcon: "&",
            isSortedBy: "&"
        }
    };
}

function FileExplorerHeaderController($scope) {
    var self = this;

    self.config = $scope.config;

    self.setViewTemplate = $scope.setViewTemplate();

    self.isSelectedViewTemplate = $scope.isSelectedViewTemplate();

    self.upDir = $scope.upDir();

    self.disableUpDir = $scope.disableUpDir();

    self.clear = $scope.clear();

    self.fileExplorerState = $scope.fileExplorerState;

    self.sortBy = $scope.sortBy();

    self.getSortIcon = $scope.getSortIcon();

    self.isSortedBy = $scope.isSortedBy();

    self.getSortTypeBundle = function () {
        return 'file.explorer.label.' + self.fileExplorerState.sort.sortType;
    };

    self.disableResetBtn = false;

    $scope.$on('notAllFilesUploaded', function () {
        self.disableResetBtn = true;
    });

    $scope.$on('allFilesUploaded', function () {
        self.disableResetBtn = false;
    });

}

