angular.module('examind.components.admin.manager.map.background', [
    'examind-instance',
    'cstl-restapi'
]).controller('MapBackgroundController', MapBackgroundController)
    .directive('mapBackground', MapBackgroundDirective);

function MapBackgroundDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/admin-manager/map-background/map-background.html',
        controller: 'MapBackgroundController',
        controllerAs: 'ctrl',
        scope: {}
    };
}

function MapBackgroundController($http, Growl) {
    var self = this;

    self.offlineMode = JSON.parse(window.localStorage.getItem('map-background-offline-mode')) || false;

    self.backgroundUrl = window.localStorage.getItem('map-background-url') || '';

    self.layer = window.localStorage.getItem('map-background-layer') || '';

    var parser = new ol.format.WMSCapabilities();

    self.layersArray = [];

    self.getCapabilities = function () {
        if (!self.backgroundUrl) {
            Growl('error', 'Error', 'You must fill a WMS service URL');
        }

        $http.get(self.backgroundUrl + '?REQUEST=GetCapabilities&SERVICE=WMS&VERSION=1.3.0')
            .then(function (response) {
                var result = parser.read(response.data);
                self.layersArray = result.Capability.Layer.Layer;
            }, function () {
                Growl('error', 'Error', 'Cannot get WMS GetCapabilities data');
            });
    };

    self.saveMode = function () {
        window.localStorage.setItem('map-background-offline-mode', self.offlineMode);
        if (!self.offlineMode) {
            window.localStorage.removeItem('map-background-url');
            window.localStorage.removeItem('map-background-layer');
            self.backgroundUrl = '';
            self.layer = '';
            self.layersArray = [];
            Growl('success', 'Success', 'Background offline mode successfully disabled');
        }
    };

    self.saveUrl = function () {
        window.localStorage.setItem('map-background-url', self.backgroundUrl);
        window.localStorage.setItem('map-background-layer', self.layer);
        Growl('success', 'Success', 'Background offline mode successfully activated');
    };

    if (self.offlineMode && self.backgroundUrl && self.layer) {
        self.getCapabilities();
    }

}
