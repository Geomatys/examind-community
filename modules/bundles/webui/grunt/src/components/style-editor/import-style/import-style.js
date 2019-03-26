angular.module('examind.components.style.editor.import', [
    'examind-instance'
])
    .controller('ImportStyleController', ImportStyleController)
    .directive('importStyle', importStyleDirective);

function importStyleDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/style-editor/import-style/import-style.html',
        controller: 'ImportStyleController',
        controllerAs: 'importStyleCtrl',
        scope: {
            afterImport: '&?'
        }
    };
}

function ImportStyleController($scope, $translate, Examind, cfpLoadingBar, Growl) {

    self = this;

    self.helper = {
        styleName: '',
        allowSubmit: false
    };

    self.errors = {
        nameErr: false,
        badExtension: false
    };

    self.afterImport = $scope.afterImport() || angular.noop;

    self.isValidField = function (input) {
        if (input) {
            return (input.$valid || input.$pristine);
        }
        return true;
    };

    self.isValidRequired = function (input) {
        if (input) {
            return !input.$error.required;
        }
        return true;
    };

    self.checkStyleName = function () {
        if (!self.helper.styleName || self.helper.styleName === '') {
            self.errors.nameErr = true;
            return false;
        }

        Examind.styles.existStyleName(self.helper.styleName)
            .then(function (response) {
                self.errors.nameErr = response.data === "true";
                return response.data === "false";
            }, function (reason) {
                $translate('style.editor.import.msg.error.check.style.name')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
                self.errors.nameErr = true;
                return false;
            });
    };

    $scope.verifyExtension = function (path) {
        $scope.$apply(function () {
            var lastPointIndex = path.lastIndexOf(".");
            var extension = path.substring(lastPointIndex + 1, path.length);
            if (extension && (extension.toLowerCase() === 'xml' || extension.toLowerCase() === 'sld')) {
                //ok to sumbit the form
                self.helper.allowSubmit = true;
                self.errors.badExtension = false;
                if (!self.helper.styleName || self.helper.styleName === '') {
                    self.helper.styleName = path.substring(path.lastIndexOf("\\") + 1, lastPointIndex);
                    self.checkStyleName();
                }
            } else {
                //bad extension then disable submitting the form
                self.helper.allowSubmit = false;
                self.errors.badExtension = true;
            }
        });
    };

    self.generateStyleName = function () {
        self.helper.styleName = 'SLD_import_' + new Date().getTime();
    };

    self.uploadStyleFile = function () {
        var $form = $('#uploadSLDform');
        var formData = new FormData($form[0]);

        cfpLoadingBar.start();
        cfpLoadingBar.inc();

        Examind.styles.importStyleFile(formData)
            .then(function (response) {
                $translate('style.editor.import.msg.success.style.import')
                    .then(function (translatedMsg) {
                        Growl('success', 'Success', translatedMsg);
                    });
                cfpLoadingBar.complete();
                self.afterImport();
            }, function (reason) {
                if (reason.status !== 422) {
                    $translate('style.editor.import.msg.error.style.import')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                } else {
                    self.errors.nameErr = true;
                }
                cfpLoadingBar.complete();
            });
    };

}