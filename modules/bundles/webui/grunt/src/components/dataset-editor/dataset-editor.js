angular.module('examind.components.dataset.editor', [
    'examind.components.dataset.editor.existing',
    "examind.components.dataset.editor.new"
])
    .controller('DatasetEditorController', DatasetEditorController)
    .directive('datasetEditor', datasetEditorDirective);

function datasetEditorDirective() {
    return {
        restrict: "E",
        templateUrl: "components/dataset-editor/dataset-editor.html",
        controller: 'DatasetEditorController',
        controllerAs: "ctrl",
        scope: {
            datasetReturnObject: "=",
            selection: "=",
            config: "=?",
            searchFilter: "=?",
            pagedSearch: '=?',
            searchParams: "=?"
        }
    };
}

function DatasetEditorController($scope) {
    var self = this;

    self.datasetReturnObject = $scope.datasetReturnObject;

    self.selection = $scope.selection || {
        dataset: null,
        datasetType: 'ALL',
        newDataset: false
    };

    self.datasetSelectionMode = "existing_dataset";

    self.searchFilter = $scope.searchFilter;

    self.config = $scope.config || {
        showHeaderBtn: true,
        showHeader: true
    };

    self.pagedSearch = $scope.pagedSearch;

    self.searchParams = $scope.searchParams;

    self.changeDatasetSelectionMode = function (mode) {
        self.datasetSelectionMode = mode;
    };

    self.isDatasetSelectionMode = function (mode) {
        return self.datasetSelectionMode === mode;
    };

}
