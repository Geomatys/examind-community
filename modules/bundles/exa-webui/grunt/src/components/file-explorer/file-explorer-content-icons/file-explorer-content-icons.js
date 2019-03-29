angular.module('examind.components.file.explorer.content.icons', [])
    .controller('FileExplorerContentIconsController', FileExplorerContentIconsController)
    .directive('fileExplorerIcons', fileManagerIconsDirective);

function fileManagerIconsDirective() {
    return {
        restrict: "E",
        require: "^fileExplorer",
        controller: 'FileExplorerContentIconsController',
        controllerAs: 'iconsCtrl',
        templateUrl: 'components/file-explorer/file-explorer-content-icons/file-explorer-content-icons.html',
        scope: {
            fileListRef: "=",
            toggleFileSelection: "&",
            isSelectedFile: "&",
            openDir: "&",
            checkListingState: '&',
            isDisabledFile: "&",
            fileExplorerState: "="
        }
    };
}

function FileExplorerContentIconsController($scope) {
    var self = this;

    self.fileListRef = $scope.fileListRef;

    self.toggleFileSelection = $scope.toggleFileSelection();

    self.openDir = $scope.openDir();

    self.isSelectedFile = $scope.isSelectedFile();

    self.checkListingState = $scope.checkListingState();

    self.isDisabledFile = $scope.isDisabledFile() || angular.noop;

    self.fileExplorerState = $scope.fileExplorerState;
}

