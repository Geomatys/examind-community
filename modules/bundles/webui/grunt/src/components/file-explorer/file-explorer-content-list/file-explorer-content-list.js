angular.module('examind.components.file.explorer.content.list', [])
    .controller('FileExplorerContentListController', FileExplorerContentListController)
    .directive('fileExplorerList', fileExplorerListDirective);

function fileExplorerListDirective() {
    return {
        restrict: "E",
        require: "^fileExplorer",
        controller: 'FileExplorerContentListController',
        controllerAs: 'lsCtrl',
        templateUrl: 'components/file-explorer/file-explorer-content-list/file-explorer-content-list.html',
        scope: {
            fileListRef: "=",
            toggleFileSelection: "&",
            isSelectedFile: "&",
            config: "=",
            openDir: "&",
            checkListingState: '&',
            isDisabledFile: "&",
            fileExplorerState: "="
        }
    };
}

function FileExplorerContentListController($scope) {
    var self = this;

    self.fileListRef = $scope.fileListRef;

    self.toggleFileSelection = $scope.toggleFileSelection();

    self.openDir = $scope.openDir();

    self.isSelectedFile = $scope.isSelectedFile();

    self.config = $scope.config;

    self.checkListingState = $scope.checkListingState();

    self.isDisabledFile = $scope.isDisabledFile() || angular.noop;

    self.fileExplorerState = $scope.fileExplorerState;

}

