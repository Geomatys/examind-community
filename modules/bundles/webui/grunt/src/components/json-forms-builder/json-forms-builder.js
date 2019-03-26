angular.module('examind.components.json.forms.builder', [])
    .controller('JsonFormsBuilderController', JsonFormsBuilderController)
    .directive('jsonFormsBuilder', jsonFormsBuilderDirective);

/**
 *
 * @returns {{restrict: string, controller: string, controllerAs: string, templateUrl: string, scope: {formSchema: string}}}
 */
function jsonFormsBuilderDirective() {
    return {
        restrict: "E",
        controller: 'JsonFormsBuilderController',
        controllerAs: 'jsonFromsBuildCtrl',
        templateUrl: 'components/json-forms-builder/json-forms-builder.html',
        scope: {
            formSchema: "=",
            hideField: "&?"
        }
    };
}

/**
 *
 * @param $scope
 * @constructor
 */
function JsonFormsBuilderController($scope) {
    var self = this;

    self.formSchema = $scope.formSchema;

    self.hideField = $scope.hideField() || angular.noop;

}

