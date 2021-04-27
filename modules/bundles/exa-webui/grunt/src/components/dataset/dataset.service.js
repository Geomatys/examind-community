angular.module("examind.components.dataset")
    .service("Dataset", DatasetService);

/**
 * @name Dataset
 *
 * @param $rootScope
 * @param $q
 * @param $modal
 * @param Growl
 * @param {Examind} Examind
 * @constructor
 */
function DatasetService($rootScope, $q, $modal, Growl, Examind) {
    var self = this;

    // Dataset deletion success callback.
    function onDatasetDeleteSuccess(dataset) {
        var evtObj = {
            target: dataset,
            name: "examind:dataset:delete",
            title: "Success",
            message: 'Dataset ' + dataset.name + ' successfully deleted',
            status: "success"
        };

        Growl(evtObj.status, evtObj.title, evtObj.message);
        $rootScope.$broadcast(evtObj.name, evtObj);

        // TODO : track this event to delete this
        $rootScope.$broadcast('reloadDatasets', evtObj);
    }

    // Dataset deletion error callback.
    function onDatasetDeleteError(dataset) {
        var evtObj = {
            target: dataset,
            name: "examind:dataset:delete",
            title: "Error",
            message: 'Dataset ' + dataset.name + ' deletion failed',
            status: "error"
        };

        Growl(evtObj.status, evtObj.title, evtObj.message);
        $rootScope.$broadcast(evtObj.name, evtObj);
    }


    // Data deletion success callback.
    function onDataDeleteSuccess(data) {
        var evtObj = {
            target: data,
            name: "examind:data:delete",
            title: "Success",
            message: 'Data ' + data.name + ' successfully deleted',
            status: "success"
        };

        Growl(evtObj.status, evtObj.title, evtObj.message);
        $rootScope.$broadcast(evtObj.name, evtObj);
    }

    // Data deletion error callback.
    function onDataDeleteError(data) {
        var evtObj = {
            target: data,
            name: "examind:data:delete",
            title: "Error",
            message: 'Data ' + data.name + ' deletion failed',
            status: "error"
        };

        Growl(evtObj.status, evtObj.title, evtObj.message);
        $rootScope.$broadcast(evtObj.name, evtObj);
    }

    /**
     * Open a modal to confirm deletion
     * Delete the selected dataset.
     * @param dataset
     * @param successCallback
     * @returns Promise
     */
    self.deleteDataset = function (dataset, successCallback) {
        if (!dataset) {
            return $q.reject("No dataset");
        }
        return $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                keyMsg: function () {
                    return 'dialog.message.confirm.delete.dataset';
                }
            }
        }).result.then(function (confirmation) {
            if (confirmation) {
                return Examind.datas.deleteDataset(dataset.id)
                    .then(function (result) {
                        if (angular.isFunction(successCallback)) {
                            successCallback();
                        }
                        onDatasetDeleteSuccess(dataset);
                        return result;
                    })
                    .catch(function (err) {
                        onDatasetDeleteError(dataset);
                        throw err;
                    });
            }
            return $q.reject("cancel");
        });
    };

    self.deleteData = function (data, successCallback) {
        if (!data) {
            return $q.reject("No data");
        }
        return $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                'keyMsg': function () {
                    return 'dialog.message.confirm.delete.data';
                }
            }
        }).result.then(function (confirmation) {
            if (confirmation) {

                var deleteDataHandler = function (data) {
                    return Examind.datas.removeData(data.id, false)
                        .then(function (result) {
                            if (angular.isFunction(successCallback)) {
                                successCallback();
                            }
                            onDataDeleteSuccess(data);
                            return result;
                        })
                        .catch(function (err) {
                            onDataDeleteError(data);
                            throw err;
                        });
                };

                // remove from any Sensor Service if there is one
                if (data.targetService) {
                    var $removed = [];
                    data.targetService.forEach(function (service) {
                        if (service.type.toLowerCase() === 'sos' || service.type.toLowerCase() === 'sts') {
                            $removed.push(Examind.sensorServices.removeData(service.id, data.id));
                        }
                    });
                    $q.all($removed).then(function () {
                        return deleteDataHandler(data);
                    });
                } else {
                    return deleteDataHandler(data);
                }


            }
            return $q.reject("cancel");
        });
    };

    self.deleteMultiData = function (data, successCallback) {
        if (!data || data.length === 0) {
            return $q.reject("No data");
        }
        return $modal.open({
            templateUrl: 'views/modal-confirm.html',
            controller: 'ModalConfirmController',
            resolve: {
                'keyMsg': function () {
                    return 'dialog.message.confirm.delete.data';
                }
            }
        }).result.then(function (confirmation) {
            if (confirmation) {

                var deleteDataHandler = function (data) {
                    var ids = data.map(function (item) {
                        return item.id;
                    });

                    return Examind.datas.removeDatas(ids, false)
                        .then(function (result) {
                            if (angular.isFunction(successCallback)) {
                                successCallback();
                            }
                            return result;
                        })
                        .catch(function (err) {
                            throw err;
                        });
                };

                var $removed = [];

                data.forEach(function (item) {
                    // remove from any Sensor Service if there is one
                    if (item.targetService) {
                        item.targetService.forEach(function (service) {
                            if (service.type.toLowerCase() === 'sos' || service.type.toLowerCase() === 'sts') {
                                $removed.push(Examind.sensorServices.removeData(service.id, item.id));
                            }
                        });
                    }
                });

                if ($removed && $removed.length > 0) {
                    $q.all($removed).then(function () {
                        return deleteDataHandler(data);
                    });
                } else {
                    return deleteDataHandler(data);
                }
            }
            return $q.reject("cancel");
        });
    }
}
