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
function DatasetListingController($rootScope, $scope, $modal, $q, DashboardHelper, Examind, defaultDatasetQuery, Dataset, DatasetDashboard) {

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
    self.selectAllFlag = false;

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

    /**
     * Delete multi selection data
     * @returns {Promise<any>}
     */
    self.deleteMultiData = function () {
        return Dataset.deleteMultiData(self.data,
            function () {
                self.data = [];
                //refresh the map
                $rootScope.$broadcast("examind:dashboard:data:refresh:map");
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

    self.associateStyle = function () {
        if (!self.data || self.data.length === 0) {
            return;
        }
        $modal.open({
            templateUrl: 'views/style/modalStyleChoose.html',
            controller: 'StyleModalController',
            resolve: {
                exclude: function () {
                    return null;
                },
                selectedLayer: function () {
                    return {
                        id: self.data[self.data.length - 1].id,
                        name: self.data[self.data.length - 1].name,
                        namespace: self.data[self.data.length - 1].namespace,
                        provider: self.data[self.data.length - 1].provider,
                        type: self.data[self.data.length - 1].type
                    };
                },
                selectedStyle: function () {
                    return null;
                },
                serviceName: function () {
                    return null;
                },
                newStyle: function () {
                    return null;
                },
                stylechooser: function () {
                    return null;
                }
            }
        }).result.then(function (sld) {
            if (angular.isObject(sld)) {
                var promises = [];


                self.data.forEach(function (item) {
                    promises.push(Examind.styles.link(sld.id, item.id));
                });

                $q.all(promises).then(function (responses) {
                    var style = {
                        id: sld.id,
                        name: sld.name,
                        providerIdentifier: sld.provider
                    };

                    for (var i = 0; i < responses.length; i++) {
                        self.data[i].targetStyle.push(style);
                    }
                    $rootScope.$broadcast("examind:dashboard:data:refresh:map");
                });
            }
        });

    };

    self.canStyleMultiData = function () {
        if (!self.data || self.data.length === 0) {
            return false;
        } else {
            var res = true;
            for (var i = 1; i < self.data.length; i++) {
                if (self.data[i].type !== self.data[0].type) {
                    res = false;
                    break;
                }
            }
            return res;
        }
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

    self.selectAllData = function () {
        self.selectAllFlag = !self.selectAllFlag;
        $rootScope.$broadcast('select-all-data', self.selectAllFlag);
    };

    $scope.$on('unselect-dataset', function (evt) {
        self.selectAllFlag = false;
    });
}
