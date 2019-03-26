angular.module('examind.components.file.explorer', [
    'examind.components.file.explorer.header',
    'examind.components.file.explorer.content.icons',
    'examind.components.file.explorer.content.list'
])
    .controller('FileExplorerController', FileExplorerController)
    .directive('fileExplorer', fileExplorerDirective)
    .filter('bytes', bytesFilter);

function fileExplorerDirective() {
    return {
        restrict: 'E',
        controller: 'FileExplorerController',
        controllerAs: 'explorerCtrl',
        templateUrl: 'components/file-explorer/file-explorer.html',
        scope: {
            config: "=",
            fileListRef: "=",
            getFileList: '&',
            openDir: '&',
            upDir: '&',
            clear: "&",
            disableUpDir: "&",
            isDisabledFile: "&",
            fileExplorerState: "="
        }
    };
}

function FileExplorerController($scope) {
    var self = this;

    // The configuration object for file-explorer component
    self.defaultConfig = {
        showHeader: true,
        viewTemplate: 'icons',
        hideSize: false,
        hideType: false,
        hideResetBtn: false,
        showSearchText: false
    };

    // Create the config object of this component
    self.config = angular.extend({}, self.defaultConfig, $scope.config);

    // The reference object that contain the file list array
    self.fileListRef = $scope.fileListRef || {
        fileList: [],
        selectedItemsList: [],
        listingState: 'requesting',
        selectAllFiles: angular.noop
    };

    // The method used to get the file list
    self.getFileList = $scope.getFileList() || angular.noop;

    // The method used to open directory in the file explorer
    self.openDir = $scope.openDir() || angular.noop;

    // The method used to go back the previous level in the file explorer
    self.upDir = $scope.upDir() || angular.noop;

    self.clear = $scope.clear();

    self.disableUpDir = $scope.disableUpDir();

    self.isDisabledFile = $scope.isDisabledFile() || angular.noop;

    self.fileExplorerState = $scope.fileExplorerState || {
        sort: {sortType: 'name', sortReverse: false},
        viewTemplate: 'list'
    };

    if (!self.fileExplorerState.sort) {
        self.fileExplorerState.sort = {sortType: 'name', sortReverse: false};
    }

    if (!self.fileExplorerState.viewTemplate) {
        self.fileExplorerState.viewTemplate = 'list';
    }

    self.setViewTemplate = function (view) {
        self.fileExplorerState.viewTemplate = view;
    };

    self.isSelectedViewTemplate = function (view) {
        return self.fileExplorerState.viewTemplate === view;
    };

    function getIndexForItem(list, item) {
        for (var i = 0; i < list.length; i++) {
            if (list[i].name === item.name) {
                return i;
            }
        }
        return -1;
    }

    self.toggleFileSelection = function (item) {
        // Prevent to select disabled files and folders
        if (self.isDisabledFile(item) || item.folder) {
            return;
        }

        var i = getIndexForItem(self.fileListRef.selectedItemsList, item);
        if (i !== -1) {
            self.fileListRef.selectedItemsList.splice(i, 1);
        }
        else {
            self.fileListRef.selectedItemsList.push(item);
        }
    };

    /**
     * Select all files that check conditions
     */
    self.selectAllFiles = function () {
        angular.forEach(self.fileListRef.fileList, function (file) {
            if (!self.isDisabledFile(file) && !file.folder && !self.isExist(self.fileListRef.selectedItemsList, file)) {
                self.fileListRef.selectedItemsList.push(file);
            }
        });
    };

    self.isSelectedFile = function (file) {
        return self.isExist(self.fileListRef.selectedItemsList, file);
    };

    self.isExist = function (list, item) {
        return list.length && getIndexForItem(list, item) !== -1;
    };

    /**
     *
     * @param state
     * @returns {boolean}
     */
    self.checkListingState = function (state) {
        return self.fileListRef.listingState === state;
    };

    self.sortBy = function (sortType) {
        if (angular.equals(self.fileExplorerState.sort.sortType, sortType)) {
            self.fileExplorerState.sort.sortReverse = !self.fileExplorerState.sort.sortReverse;
        } else {
            self.fileExplorerState.sort.sortType = sortType;
            self.fileExplorerState.sort.sortReverse = false;
        }
    };

    self.getSortIcon = function () {
        if (self.fileExplorerState.sort.sortReverse) {
            return 'fa-caret-down';
        } else {
            return 'fa-caret-up';
        }
    };

    self.isSortedBy = function (sortType) {
        return angular.equals(self.fileExplorerState.sort.sortType, sortType);
    };

    /**
     * Expose the function reference to call outside the component
     * @type {FileExplorerController.selectAllFiles|*}
     */
    self.fileListRef.selectAllFiles = self.selectAllFiles;
}

function bytesFilter() {
    return function (bytes, precision) {
        if (bytes === 0) {
            return '0 octets';
        }
        if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
            return '-';
        }
        if (typeof precision === 'undefined') {
            precision = 1;
        }
        var units = ['octets', 'Ko', 'Mo', 'Go', 'To', 'Po'];
        var number = Math.floor(Math.log(bytes) / Math.log(1000));
        return (bytes / Math.pow(1000, Math.floor(number))).toFixed(precision) + ' ' + units[number];
    };
}

