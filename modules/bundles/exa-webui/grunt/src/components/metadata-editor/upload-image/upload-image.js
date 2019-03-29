angular.module('examind.components.metadata.editor.upload.image', [])
    .controller('MDUploadImageController', MDUploadImageController)
    .directive('mdUploadImage', mdUploadImageDirective);


function mdUploadImageDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/metadata-editor/upload-image/upload-image.html',
        controller: 'MDUploadImageController',
        controllerAs: 'mdUploadImgCtrl',
        scope: {
            uriRegExp: "=",
            fieldObj: "="
        }
    };
}

function MDUploadImageController($scope, $cookieStore, $translate, Growl, Examind) {
    var self = this;

    // The RegEx object fot the ngPattern directive
    self.uriRegExp = $scope.uriRegExp;

    self.fieldObj = $scope.fieldObj;

    self.isValidUri = function (input) {
        if (input) {
            return !input.$error.pattern;
        }
        return true;
    };

    self.isValidField = function (input) {
        if (input) {
            return (input.$valid || input.$pristine);
        }
        return true;
    };

    self.uploadImage = function (value, field) {
        var cstlUrl = $cookieStore.get('cstlUrl');

        if (value) {
            var $form = $('#metadataform');
            var fileInput = $form.find('.uploadimage');
            if (!fileInput || !fileInput.get(0).files || fileInput.get(0).files.length === 0) {
                return;
            }
            var fileSize = fileInput.get(0).files[0].size / 1000000;
            if (fileSize > 2) {
                $translate('msg.error.exceed.image.size')
                    .then(function (translatedMsg) {
                        Growl('error', 'Error', translatedMsg);
                    });
                return;
            }
            var formData = new FormData($form[0]);

            Examind.metadata.uploadImage(formData)
                .then(function (response) {
                        if(response.data.attachmentId) {
                            field.value = cstlUrl+'API/attachments/view/'+response.data.attachmentId;
                        }
                    },
                    function () {
                        $translate('msg.error.image.upload')
                            .then(function (translatedMsg) {
                                Growl('error', 'Error', translatedMsg);
                            });
                    }
                );

        } else {
            field.value = "";
        }
    };

}