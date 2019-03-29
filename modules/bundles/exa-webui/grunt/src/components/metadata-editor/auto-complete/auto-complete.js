angular.module('examind.components.metadata.editor.auto.complete', [])
    .controller('MDAutoCompleteController', MDAutoCompleteController)
    .directive('mdAutoComplete', mdAutoCompleteDirective);


function mdAutoCompleteDirective() {
    return {
        restrict: 'E',
        templateUrl: 'components/metadata-editor/auto-complete/auto-complete.html',
        controller: 'MDAutoCompleteController',
        controllerAs: 'mdAutoCompleteCtrl',
        scope: {
            fieldObj: "="
        }
    };
}

function MDAutoCompleteController($scope, $http, $translate) {
    var self = this;

    self.fieldObj = $scope.fieldObj;

    /**
     * Returns the current lang used.
     * For datepicker as locale.
     */
    self.getCurrentLang = function () {
        var lang = $translate.use();
        if (!lang) {
            lang = 'en';
        }
        return lang;
    };

    /**
     * Return a promise object to load keywords asynchronously for autocompleter.
     * @param val
     */
    self.resolveThesaurusKeywords = function (val) {
        var currentLang = self.getCurrentLang();
        return $http.get('@cstl/API/THW/autocomplete/' + currentLang + '/' + 6 + '/' + val)
            .then(function (response) {
                return response.data;
            });
    };

}