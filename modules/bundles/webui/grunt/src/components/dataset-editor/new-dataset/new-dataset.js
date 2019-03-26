angular.module('examind.components.dataset.editor.new', [
    'examind-instance',
    'examind.components.metadata.editor'
])
    .controller('NewDatasetController', NewDatasetController)
    .directive('newDataset', newDatasetDirective);

function newDatasetDirective() {
    return {
        restrict: 'E',
        require: '^datasetEditor',
        templateUrl: 'components/dataset-editor/new-dataset/new-dataset.html',
        controller: 'NewDatasetController',
        controllerAs: 'ctrl',
        scope: {
            datasetReturnObject: "=",
            selection: "="
        }
    };
}

function NewDatasetController($scope, $translate, Growl, Examind) {
    var self = this;

    self.datasetName = null;

    self.datasetReturnObject = $scope.datasetReturnObject;

    self.selection = $scope.selection;

    self.errors = {
        nameErr: false
    };

    self.metadataValues = [];

    self.checkDatasetName = function () {
        if (!self.datasetName || self.datasetName === '') {
            self.errors.nameErr = true;
            return false;
        }
        Examind.datas.existDatasetName(self.datasetName)
            .then(function (response) {
                self.errors.nameErr = response.data === "true";
                return response.data === "false";
            }, function (reason) {
                self.errors.nameErr = true;
                return false;
            });
    };

    self.createDataset = function (callback, goToNextStepFn, stepNum) {
        if (!self.datasetName || self.datasetName === '') {
            self.errors.nameErr = true;
            return;
        }
        Examind.datas.existDatasetName(self.datasetName)
            .then(function (response) {
                if (response.data === "false") {
                    if ($('#metadataform').hasClass('ng-invalid')) {
                        $translate('dataset.editor.new.msg.error.fill.mandatory.fields')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    } else {
                        Examind.datas.createDatasetNew(self.datasetName, self.metadataValues[0], 'true')
                            .then(function (response) {
                                self.selection.dataset = response.data;
                                self.selection.newDataset = true;
                                $translate('dataset.editor.new.dataset.msg.success.dataset.creation')
                                    .then(function (translatedMsg) {
                                        Growl('success', 'Success', translatedMsg);
                                        if (callback && goToNextStepFn && stepNum && angular.isFunction(callback) && angular.isFunction(goToNextStepFn)) {
                                            callback(goToNextStepFn, stepNum);
                                        }
                                    });
                            });
                    }
                } else {
                    self.errors.nameErr = response.data === "true";
                    return;
                }
            }, function (reason) {
                self.errors.nameErr = true;
                return;
            });

        if (!self.metadataValues || self.metadataValues.length === 0) {
            return;
        }
    };

    self.getNewMetadataJson = function () {
        Examind.metadata.getNewMetadataJson('profile_import')
            .then(function (response) {
                self.metadataValues.push(response.data);
            }, function (reason) {
                $translate('dataset.editor.new.dataset.msg.error.get.metadata')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    self.init = function () {
        self.getNewMetadataJson();
        self.datasetReturnObject.createDataset = self.createDataset;
    };

    self.init();

    $scope.$on("$destroy", function () {
        self.datasetReturnObject.createDataset = null;
    });

}