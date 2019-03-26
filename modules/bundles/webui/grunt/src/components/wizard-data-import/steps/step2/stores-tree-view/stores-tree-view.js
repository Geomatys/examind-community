angular.module('examind.components.stores.tree.view', [])
    .controller('StoresTreeViewController', StoresTreeViewController)
    .directive('storesTreeView', storesTreeViewDirective);

function storesTreeViewDirective() {
    return {
        restrict: 'E',
        controller: 'StoresTreeViewController',
        controllerAs: 'treeCtrl',
        templateUrl: 'components/wizard-data-import/steps/step2/stores-tree-view/stores-tree-view.html',
        scope: {
            storesRef: "=",
            displayDataLayer: "&"
        }
    };
}

function StoresTreeViewController($scope) {
    var self = this;

    self.storesRef = $scope.storesRef;

    self.displayDataLayer = $scope.displayDataLayer();

    self.allStoresSelection = false;

    // Get the index of the item in the given list
    function getIndexForItem(list, item) {
        for (var i = 0; i < list.length; i++) {
            if (list[i].id === item.id) {
                return i;
            }
        }
        return -1;
    }

    // Check if element exist in the list
    self.isExist = function (list, item) {
        return list && list.length && getIndexForItem(list, item) !== -1;
    };

    /**
     * Toggle store selection in the case of indivisible data store
     * @param item
     */
    self.toggleStoreSelection = function (item) {
        var i = getIndexForItem(self.storesRef.selectedItemsList, item);
        if (i !== -1) {
            item.collapsed = true;
            self.storesRef.selectedItemsList.splice(i, 1);
        } else {
            item.collapsed = false;
            self.storesRef.selectedItemsList.push(item);
        }
    };

    self.toggleAllStoreSelection = function() {
        if(self.allStoresSelection) {
            //add all to selection
            self.storesRef.selectedItemsList = [];
            angular.forEach(self.storesRef.storeList, function (store) {
                store.collapsed = false;
                self.storesRef.selectedItemsList.push(store);
            });
        } else {
            //remove all from selection
            self.storesRef.selectedItemsList = [];
            angular.forEach(self.storesRef.storeList, function (store) {
                store.collapsed = true;
            });
        }
    };

    /**
     * Toggle the selection of resource in the case of divisible data store
     */
    self.toggleResourceSelection = function (item) {
        var i = getIndexForItem(self.storesRef.selectedItemsList, item);
        if (i !== -1) {
            self.storesRef.selectedItemsList.splice(i, 1);
            self.displayDataLayer(null);
        } else {
            self.storesRef.selectedItemsList.push(item);
            self.displayDataLayer(item);
        }
    };

    self.isSelected = function (item) {
        return self.isExist(self.storesRef.selectedItemsList, item);
    };

    self.getFileName = function (path) {
        if (path) {
            path = path.replace(/^.*[\\\/]/, '');
            return decodeURIComponent(path);
        }
    };

    self.isDisplayedLayer = function (resource) {
        return self.storesRef.displayedResource && resource && self.storesRef.displayedResource.id === resource.id;
    };

    /**
     * Toggle display the data layer in the preview map for indivisible data store
     * @param resource
     */
    self.toggleDisplayLayer = function (resource) {
        if (self.isDisplayedLayer(resource)) {
            self.storesRef.displayedResource = null;
            self.displayDataLayer(null);
        } else {
            self.storesRef.displayedResource = resource;
            self.displayDataLayer(resource);
        }
    };

    self.initStore = function (store) {
        if (!angular.isDefined(store.collapsed)) {
            store.collapsed = true;
        }
    };

}

