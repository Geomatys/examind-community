angular.module('examind.components.file.uploader', [])
    .controller('FileUploaderController', FileUploaderController)
    .controller('FileDragNDropController', FileDragNDropController)
    .factory('FileItem', FileItemFactory)
    .directive('fileUploader', fileUploaderDirective)
    .directive('fileDragNDrop', fileDragNDropDirective);

function fileUploaderDirective() {
    return {
        restrict: "E",
        controller: 'FileUploaderController',
        controllerAs: 'fileUplCtrl',
        templateUrl: 'components/file-uploader/file-uploader.html',
        scope: {
            uploadFilesPromise: '&',
            uploadFilesPromiseSuccess: '&',
            uploadFilesPromiseError: '&',
            lastUploadFilesPromiseSuccess: '&'
        }
    };
}

function fileDragNDropDirective() {
    return {
        restrict: 'E',
        scope: false,
        controller: 'FileDragNDropController'
    };
}

function FileUploaderController($scope, $q, Growl, FileItem) {
    var self = this;

    // The list of the selected files
    self.selectedFiles = [];

    self.collapsed = true;

    // function to return the promise to call for the file uploading
    self.uploadFilesPromise = $scope.uploadFilesPromise();

    self.uploadFilesPromiseSuccess = $scope.uploadFilesPromiseSuccess();

    self.uploadFilesPromiseError = $scope.uploadFilesPromiseError();

    self.lastUploadFilesPromiseSuccess = $scope.lastUploadFilesPromiseSuccess();

    self.removeFile = function (fileItem) {
        self.selectedFiles = self.selectedFiles.filter(function (item) {
            return item !== fileItem;
        });
    };

    self.uploadFile = function (fileItem) {
        var formData = new FormData();
        formData.append("file", fileItem.file);
        fileItem.canceller = $q.defer();
        fileItem.isCanceled = false;
        fileItem.isError = false;

        if (self.uploadFilesPromise && angular.isFunction(self.uploadFilesPromise)) {
            self.uploadFilesPromise(formData, fileItem.canceller.promise)
                .then(function (response) {
                    fileItem.isUploaded = true;
                    self.calculateProgress();
                    if (self.uploadFilesPromiseSuccess && angular.isFunction(self.uploadFilesPromiseSuccess)) {
                        self.uploadFilesPromiseSuccess(response);
                    }
                    // Clear the fielInput Html element
                    document.getElementById("files").value = "";
                    self.removeFile(fileItem);
                    if (self.selectedFiles.length === 0) {
                        self.lastUploadFilesPromiseSuccess();
                    }
                }, function (reason) {
                    if (self.uploadFilesPromiseError && angular.isFunction(self.uploadFilesPromiseError)) {
                        self.uploadFilesPromiseError(reason);
                    }
                    fileItem.isError = true;
                    self.lastUploadFilesPromiseSuccess();
                });
        }
    };

    $scope.selectFiles = function (element) {
        var files = element.files;
        /**
         * Fill the selectedFiles array with the files
         * because element.files return FileList and not array
         */
        angular.forEach(files, function (file) {
            if (file.type === "application/x-7z-compressed" || file.type === "application/gzip") {
                Growl('error', 'Error', 'This format of the compressed file' + file.name + 'is not supported (use .zip files)');
            } else {
                var fileItem = new FileItem(file);
                self.selectedFiles.push(fileItem);
                self.uploadFile(fileItem);
            }
        });
    };

    self.calculateProgress = function () {
        return self.getUploadedFiles().length / self.selectedFiles.length * 100;
    };

    self.getUploadedFiles = function () {
        return self.selectedFiles.filter(function (item) {
            return item.isUploaded;
        });
    };

    self.AbortRequest = function (fileItem) {
        fileItem.canceller.resolve('cancelled');
        fileItem.isCanceled = true;
    };

    self.removeRequest = function (fileItem) {
        self.AbortRequest(fileItem);
        self.removeFile(fileItem);
        self.calculateProgress();
    };

    self.AbortAllRequests = function () {
        angular.forEach(self.selectedFiles, function (fileItem) {
            self.AbortRequest(fileItem);
        });
    };

    self.removeAllRequests = function () {
        angular.forEach(self.selectedFiles, function (fileItem) {
            self.AbortRequest(fileItem);
            self.removeFile(fileItem);
        });
    };

    self.reUploadFile = function (fileItem) {
        self.uploadFile(fileItem);
    };

    self.reUploadAll = function () {
        angular.forEach(self.selectedFiles, function (fileItem) {
            self.reUploadFile(fileItem);
        });
    };

    self.getRemainingRequestsNumber = function () {
        return self.selectedFiles.filter(function (fileItem) {
            return !fileItem.isCanceled;
        }).length;
    };

}

function FileDragNDropController($scope, FileItem, $element) {
    var self = this;

    $element.on("dragover", function (e) {
        e.preventDefault();
    });

    $element.on("drop", function (e) {
        e.preventDefault();
        e.stopPropagation();

        // var files = e.target.files || e.dataTransfer.files;
        if (e.originalEvent.dataTransfer) {
            /**
             * Fill the selectedFiles array with the files
             * because element.files return FileList and not array
             */
            angular.forEach(e.originalEvent.dataTransfer.files, function (file) {
                if (file.type === "application/x-7z-compressed" || file.type === "application/gzip") {
                    Growl('error', 'Error', 'This format of the compressed file' + file.name + 'is not supported (use .zip files)');
                } else {
                    var fileItem = new FileItem(file);
                    $scope.fileUplCtrl.selectedFiles.push(fileItem);
                    $scope.fileUplCtrl.uploadFile(fileItem);
                }
            });

            // Clear the dataTransfer object
            e.originalEvent.dataTransfer.clearData();
        }
        return false;
    });

}

/**
 * The fileItem Prototype
 * @returns {FileItem}
 * @constructor
 */
function FileItemFactory() {
    // Prototype for the file items
    function FileItem(file) {
        this.file = file;
        this.isCanceled = false;
        this.isUploaded = false;
        this.isError = false;
        this.canceller = null;
    }

    /**
     * Return the constructor function
     */
    return FileItem;
}



