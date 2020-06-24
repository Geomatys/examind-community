angular.module('cstl-data-dashboard')
    .controller('DatasetListingController', DatasetListingController);

/**
 * @param $rootScope
 * @param $scope
 * @param DashboardHelper
 * @param Examind
 * @param defaultDatasetQuery
 * @param {Dataset} Dataset
 * @param {DatasetDashboard} DatasetDashboard
 * @constructor
 */
function DatasetListingController($rootScope, $scope, DashboardHelper, Examind, defaultDatasetQuery, Dataset, DatasetDashboard) {

    var self = this;

    // Apply DashboardHelper features on the controller instance.
    DashboardHelper.call(self, Examind.datas.searchDataset, angular.copy(defaultDatasetQuery));

    Object.defineProperties(self, {
        dataset: {
            enumerable: true,
            get: function () {
                return DatasetDashboard.dataset;
            },
            set: function (dataset) {
                return (DatasetDashboard.dataset = dataset);
            }
        },
        data: {
            enumerable: true,
            get: function () {
                return DatasetDashboard.data;
            },
            set: function (data) {
                return (DatasetDashboard.data = data);
            }
        }
    });

    //reset selection
    self.dataset = null;
    self.data = null;

    // Immediate content loading.
    self.search();

    // Method used to modify a query filter and launch the search.
    self.setFilter = function (key, value) {
        self.removeFilter(self.query, key);
        self.query.filters.push({"field": key, "value": value});
    };

    self.existFilter = function (field) {
        return angular.isDefined(self.getFilter(field));
    };

    self.checkFilter = function (field, value) {
        var filter = self.getFilter(field);
        return angular.isDefined(filter) && filter.value === value;
    };

    self.removeFilter = function (query, field) {
        if (query.filters) {
            for (var i = 0; i < query.filters.length; i++) {
                if (query.filters[i].field === field) {
                    query.filters.splice(i, 1);
                    break;
                }
            }
        }
    };

    self.getFilter = function (field) {
        if (self.query.filters) {
            for (var i = 0; i < self.query.filters.length; i++) {
                if (self.query.filters[i].field === field) {
                    return self.query.filters[i];
                }
            }
        }
        return undefined;
    };


    // Method used to filter dataset on their data types.
    self.setTypeFilter = function (type) {
        switch (type) {
            case 'VECTOR':
                self.query.filters.push({"field": "hasVectorData", "value": "true"});
                self.removeFilter(self.query, "hasCoverageData");
                break;
            case 'COVERAGE':
                self.query.filters.push({"field": "hasCoverageData", "value": "true"});
                self.removeFilter(self.query, "hasVectorData");
                break;
            default:
                self.removeFilter(self.query, "hasVectorData");
                self.removeFilter(self.query, "hasCoverageData");

        }
    };

    // Method used to reset the search criteria.
    self.resetCriteria = function () {
        //reset selection
        self.dataset = null;
        self.data = null;
        //refresh the map
        $rootScope.$broadcast("examind:dashboard:data:refresh:map");

        //reset query filters
        self.query = angular.copy(defaultDatasetQuery);
        self.search();
    };

    // Observe the 'reloadDatasets' event to re-launch the search.
    $scope.$on('reloadDatasets', self.search);

    self.deleteData = function () {
        return Dataset.deleteData(self.data[0],
            function () {
                self.data.splice(0, 1);
                return self.setPage(1);
            })
            .catch(function (err) {
                if (err === 'cancel') {
                    return;
                }
                // TODO : Move real error management (Growl???) here
                console.error("Unable to delete this data.", self.data[0], err);
            });
    };

    self.deleteMultiData = function () {
        return Dataset.deleteMultiData(self.data,
            function () {
                self.data = [];
                return self.setPage(1);
            })
            .catch(function (err) {
                if (err === 'cancel') {
                    return;
                }
                // TODO : Move real error management (Growl???) here
                console.error("Unable to delete this data.", err);
            });
    };

    self.deleteDataset = function () {
        return Dataset.deleteDataset(self.dataset,
            function () {
                self.dataset = null;
                self.data = [];
                return self.setPage(1);
            })
            .catch(function (err) {
                if (err === 'cancel') {
                    return;
                }
                // TODO : Move real error management (Growl???) here
                console.error("Unable to delete this dataset.", self.dataset, err);
            });
    };
}
