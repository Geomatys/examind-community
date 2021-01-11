angular.module('examind.components.style.editor.existing', [
    'examind.shared.data.viewer.service',
    'examind.paged.search.service'
])
    .controller('ExistingStyleController', ExistingStyleController)
    .directive('existingStyle', existingStyleDirective);

function existingStyleDirective() {
    return {
        restrict: 'E',
        require: '^styleEditor',
        templateUrl: 'components/style-editor/existing-style/existing-style.html',
        controller: 'ExistingStyleController',
        controllerAs: 'existingCtrl',
        scope: {
            styleValues: '=',
            selectedStyleType: '=',
            selectedDataRef: '=',
            editStyle: '&',
            duplicateStyle: '&',
            styleEditorOutput: "&",
            associateStyleToDataLayer: "&",
            disableAssociateBtn: "&"
        }
    };
}

function ExistingStyleController($scope, $translate, Growl, Examind, DataViewerService, PagedSearchService) {

    var self = this;

    self.styleValues = $scope.styleValues;

    self.selectedDataRef = $scope.selectedDataRef;

    self.helper = {
        selectedStyleType: null,
        selectedStyle: null,
        dataBbox: []
    };

    self.helper.selectedStyleType = $scope.selectedStyleType;

    // remove the default data for the prod
    if (!self.selectedDataRef.dataLayer) {
        self.selectedDataRef.dataLayer = {
            id: 1,
            name: 'CNTR_RG_60M_2006',
            provider: "generic_shp"
        };
    }

    self.styleEditorOutput = $scope.styleEditorOutput();

    self.associateStyleToDataLayer = $scope.associateStyleToDataLayer();

    self.disableAssociateBtn = $scope.disableAssociateBtn();

    self.searchVisible = false;

    // Create an instance of PagedSearchService to handle the search and filter
    self.pagedSearchService = new PagedSearchService();

    self.pagedSearchService.search = function () {
        Examind.styles.searchStyles(self.pagedSearchService.query)
            .then(function (response) {
                self.page = response.data;
            }, function () {
                $translate('style.editor.msg.error.get.statistics')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
            });
    };

    self.editStyle = $scope.editStyle();

    self.duplicateStyle = $scope.duplicateStyle();

    self.isSelected = function (style) {
        return self.helper.selectedStyle && self.helper.selectedStyle.id === style.id;
    };

    self.selectStyle = function (style) {
        if (self.isSelected(style)) {
            self.helper.selectedStyle = null;
            self.styleEditorOutput();
        } else {
            self.helper.selectedStyle = style;
            self.styleEditorOutput(style);
        }
        self.displayCurrentExistingStyle('styledMapWithSelectedStyle', null);
    };

    /**
     * Performs a preview of current style in map
     */
    self.displayCurrentExistingStyle = function (mapId) {
        //skip if layerName is undefined
        if (!self.selectedDataRef.dataLayer && !self.selectedDataRef.dataLayer.name) {
            return;
        }

        DataViewerService.initConfig();

        var styleName = null;

        if (self.helper.selectedStyle) {
            styleName = self.helper.selectedStyle.name;
        }

        var layerData;

        if (styleName) {
            layerData = DataViewerService.createLayerWithStyle(window.localStorage.getItem('cstlUrl'),
                self.selectedDataRef.dataLayer.id,
                self.selectedDataRef.dataLayer.name,
                styleName, null, null, false);
        } else {
            layerData = DataViewerService.createLayer(window.localStorage.getItem('cstlUrl'),
                self.selectedDataRef.dataLayer.id, self.selectedDataRef.dataLayer.name, null, false);
        }

        // To force the browser cache reloading styled layer.
        layerData.get('params').ts = new Date().getTime();

        DataViewerService.layers = [layerData];

        if (self.helper.dataBbox) {
            var extent = [self.helper.dataBbox[0],
                self.helper.dataBbox[1],
                self.helper.dataBbox[2],
                self.helper.dataBbox[3]];
            DataViewerService.extent = extent;
        }

        DataViewerService.initMap(mapId);
    };

    self.initDataLayerProperties = function (dataId) {
        if (!dataId) {
            return;
        }

        Examind.datas.getGeographicExtent(dataId)
            .then(function (response) {
                    self.helper.dataBbox = response.data.boundingBox;
                    self.displayCurrentExistingStyle('styledMapWithSelectedStyle', null);
                },
                function () {
                    $translate('style.editor.msg.error.data.descriptions')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                });
    };

    self.init = function () {
        self.pagedSearchService.query.size = 5;
        self.pagedSearchService.query.filters.push({field: "type", value: self.selectedDataRef.dataLayer.type});
        self.pagedSearchService.search();
        self.initDataLayerProperties(self.selectedDataRef.dataLayer.id);
    };

    self.init();

    $scope.$watch(function () {
        return self.selectedDataRef.dataLayer
    }, function () {
        self.init();
    }, true);

}