angular.module('examind.components.dataset.editor.existing', [
    'examind-instance',
    'examind.paged.search.service'
])
    .controller('ExistingDatasetsController', ExistingDatasetsController)
    .directive('existingDatasets', existingDatasetsDirective);

function existingDatasetsDirective() {
    return {
        restrict: 'E',
        require: '^datasetEditor',
        templateUrl: 'components/dataset-editor/existing-datasets/existing-datasets.html',
        controller: 'ExistingDatasetsController',
        controllerAs: 'ctrl',
        scope: {
            selection: "=",
            searchFilter: "=?",
            pagedSearch: '=?',
            searchParams: "=?"
        }
    };
}

function ExistingDatasetsController($scope, $translate, Growl, Examind, PagedSearchService, defaultQuery) {
    var self = this;

    self.selection = $scope.selection || {
        dataset: null,
        datasetType: 'ALL',
        newDataset: false
    };

    self.searchFilter = $scope.searchFilter;

    self.searchParams = $scope.searchParams;

    // Create an instance of PagedSearchService to handle the search and filter
    self.pagedSearchService = new PagedSearchService();

    self.setTypeFilter = function (type) {
        self.selection.datasetType = type;
    };

    self.isSelectedDatasetType = function (type) {
        return self.selection.datasetType === type;
    };

    self.isSelectedDataset = function (dataset) {
        if (dataset && dataset.id && self.selection.dataset && self.selection.dataset.id) {
            return self.selection.dataset.id === dataset.id;
        }
        return false;
    };

    self.toggleSelectDataset = function (dataset) {
        if (!self.isSelectedDataset(dataset)) {
            self.selection.dataset = dataset;
        } else {
            self.selection.dataset = null;
        }
    };

    /**
     * Handle the case of overriding the default behavior of dataSet editor
     */
    self.handleSearchConfig = function () {
        if (self.searchFilter && self.searchFilter.field && self.searchFilter.value) {
            self.pagedSearchService.filterBy(self.searchFilter.field, self.searchFilter.value);
        }
        if (self.searchParams) {
            angular.forEach(self.searchParams, function (paramObj) {
                self.pagedSearchService.query[paramObj.param] = paramObj.value;
            });
        }
    };

    // Override the default paged search service search method
    self.pagedSearchService.search = function () {
        Examind.datas.searchDataset(self.pagedSearchService.query)
            .then(function (response) {
                self.page = response.data;
            }, function () {
                $translate('dataset.editor.existing.msg.error.get.dataset.list')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    // Override the default paged search service reset method
    self.pagedSearchService.reset = function () {
        self.pagedSearchService.query = angular.copy(defaultQuery);
        self.handleSearchConfig();
        self.pagedSearchService.search();
    };

    self.init = function () {
        if ($scope.pagedSearch) {
            $scope.pagedSearch.search = self.pagedSearchService.search;
        }
        self.handleSearchConfig();
        self.pagedSearchService.search();
    };

    self.init();
}