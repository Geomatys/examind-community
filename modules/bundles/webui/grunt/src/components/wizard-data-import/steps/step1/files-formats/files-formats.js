angular.module('examind.components.wizardDataImport.step1.files.formats', [
    'examind.components.file.explorer',
    'examind.components.json.forms.builder'
])
    .controller('FilesFormatsController', FilesFormatsController)
    .directive('filesFormats', filesFormatsDirective);

function filesFormatsDirective() {
    return {
        restrict: "E",
        require: '^step1Wizard',
        controller: 'FilesFormatsController',
        controllerAs: 'ctrl',
        templateUrl: 'components/wizard-data-import/steps/step1/files-formats/files-formats.html',
        scope: {
            stores: "=",
            fileListRef: "=",
            fileExplorerState: "=",
            advConfig: "=",
            formSchema: "=",
            selectStore: "&",
            getFileList: "&",
            upDir: "&",
            openDir: "&",
            removeFilesFromDataSource: "&",
            disableUpDir: "&",
            isDisabledFile: "&",
            canShowProviderConfigProperties: "&",
            selectAll: "&",
            showAdvancedConfigBlock: "&",
            hideField: "&"
        }
    };
}

function FilesFormatsController($scope) {
    var self = this;

    self.stores = $scope.stores;

    self.fileListRef = $scope.fileListRef;

    self.fileExplorerState = $scope.fileExplorerState;

    self.advConfig = $scope.advConfig;

    self.formSchema = $scope.formSchema;

    self.selectStore = $scope.selectStore();

    self.getFileList = $scope.getFileList();

    self.upDir = $scope.upDir();

    self.openDir = $scope.openDir();

    self.removeFilesFromDataSource = $scope.removeFilesFromDataSource();

    self.disableUpDir = $scope.disableUpDir();

    self.isDisabledFile = $scope.isDisabledFile();

    self.canShowProviderConfigProperties = $scope.canShowProviderConfigProperties();

    self.selectAll = $scope.selectAll();

    self.showAdvancedConfigBlock = $scope.showAdvancedConfigBlock();

    self.hideField = $scope.hideField();

}