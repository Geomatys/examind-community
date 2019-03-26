angular.module('examind.components.wizardDataImport.step1.files.source', [
    'examind.components.file.uploader',
    'examind.components.wizardDataImport.step1.files.formats'
])
    .controller('FilesSourceController', FilesSourceController)
    .directive('filesSource', filesSourceDirective);

function filesSourceDirective() {
    return {
        restrict: "E",
        require: '^step1Wizard',
        controller: 'FilesSourceController',
        controllerAs: 'filesCtrl',
        templateUrl: 'components/wizard-data-import/steps/step1/files-source/files-source.html',
        scope: {
            processing: "=",
            stores: "=",
            formSchema: "=",
            advConfig: "=",
            fileListRef: "=",
            fileExplorerState: "=",
            getDataSourceId: "&",
            setDataSourceId: "&",
            setDataSource: "&",
            getFileList: "&",
            getStores: "&",
            selectStore: "&",
            selectAll: "&",
            canShowProviderConfigProperties: "&",
            showAdvancedConfigBlock: "&",
            hideField: "&",
            upDir: "&",
            removeFilesFromDataSource: "&",
            openDir: "&",
            disableUpDir: "&",
            isDisabledFile: "&"
        }
    };
}

function FilesSourceController($scope, $rootScope, $q, $translate, Growl, cfpLoadingBar, Examind) {
    var self = this;

    self.processing = $scope.processing;

    self.stores = $scope.stores;

    self.formSchema = $scope.formSchema;

    self.advConfig = $scope.advConfig;

    self.fileListRef = $scope.fileListRef;

    self.fileExplorerState = $scope.fileExplorerState;

    self.getDataSourceId = $scope.getDataSourceId();

    self.setDataSourceId = $scope.setDataSourceId();

    self.setDataSource = $scope.setDataSource();

    self.getFileList = $scope.getFileList();

    self.getStores = $scope.getStores();

    self.selectStore = $scope.selectStore();

    self.selectAll = $scope.selectAll();

    self.canShowProviderConfigProperties = $scope.canShowProviderConfigProperties();

    self.showAdvancedConfigBlock = $scope.showAdvancedConfigBlock();

    self.hideField = $scope.hideField();

    self.upDir = $scope.upDir();

    self.removeFilesFromDataSource = $scope.removeFilesFromDataSource();

    self.openDir = $scope.openDir();

    self.disableUpDir = $scope.disableUpDir();

    self.isDisabledFile = $scope.isDisabledFile();

    self.preRequisiteFilesUpload = function () {
        if (!self.getDataSourceId()) {
            Examind.dataSources.create({type: 'local_files'}).then(function (response) {
                self.setDataSourceId(response.data);
                cfpLoadingBar.start();
                cfpLoadingBar.inc();

                Examind.dataSources.get(self.getDataSourceId())
                    .then(function (response) {
                        self.setDataSource(response.data);
                        cfpLoadingBar.complete();
                    }, function (reason) {//error
                        $translate('wiz.data.import.step1.msg.err.get.data.source.info')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    });
            }, function (reason) {
                Growl('error', 'Error', 'Cannot create data source for upload files');
            });
        }
    };

    /**
     * The promise used to upload file in the file-uploader component
     * @param formData
     * @returns {*|Promise}
     */
    self.uploadFilesPromise = function (formData, timeoutPromise) {
        if (!self.processing.fileUpload) {
            self.processing.fileUpload = true;
            $rootScope.$broadcast('notAllFilesUploaded');
        }

        return Examind.dataSources.uploadFileToDataSource(self.getDataSourceId(), formData, timeoutPromise);
    };

    /**
     * The success callback of upload file in the file-uploader component
     * @param response
     */
    self.uploadFilesPromiseSuccess = function (response) {
        // The case of upload file
    };

    self.lastUploadFilesPromiseSuccess = function () {
        self.processing.fileUpload = false;
        $rootScope.$broadcast('allFilesUploaded');
        // Once all files are uploaded, we ask Examind to scan the datasource.
        self.getFileList('/');
        self.getStores();
    };

    /**
     * The error callback of upload file in the file-uploader component
     * @param reason
     */
    self.uploadFilesPromiseError = function (reason) {
        Growl('error', 'Error', 'Cannot upload file');
    };

    self.init = function () {
        self.preRequisiteFilesUpload();
    };

    $scope.$on('dataSourceDeleted', function () {
        self.preRequisiteFilesUpload();
    });

    self.init();

}
