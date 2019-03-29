angular.module('examind.components.admin.manager.system.providers.modal.config', [])
    .controller('ModalProviderConfigController', ModalProviderConfigController)
    .directive('modalConfig', modalConfigDirective);

function modalConfigDirective() {
    return {
        restrict: "E",
        templateUrl: "components/admin-manager/system-providers/modal-config/modal-config.html",
        controller: 'ModalProviderConfigController',
        controllerAs: "ctrl",
        scope: {}
    };
}

function ModalProviderConfigController($scope, $modalInstance, details) {
    var self = this;

    //function to indent xml string
    function formatXml(xml) {
        var formatted = '';
        var reg = /(>)\s*(<)(\/*)/g;
        xml = xml.replace(reg, '$1\r\n$2$3');
        var pad = 0;
        angular.forEach(xml.split('\r\n'), function (node) {
            var indent = 0;
            if (node.match(/.+<\/\w[^>]*>$/)) {
                indent = 0;
            } else if (node.match(/^<\/\w/)) {
                if (pad !== 0) {
                    pad -= 1;
                }
            } else if (node.match(/^<\w[^>]*[^\/]>.*$/)) {
                indent = 1;
            } else {
                indent = 0;
            }
            var padding = '';
            for (var i = 0; i < pad; i++) {
                padding += '  ';
            }
            formatted += padding + node + '\r\n';
            pad += indent;
        });
        return formatted;
    }

    $scope.details = formatXml(details);

    $scope.close = function () {
        $modalInstance.dismiss('close');
    };
}
