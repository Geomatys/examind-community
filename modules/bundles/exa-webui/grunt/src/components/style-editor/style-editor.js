angular.module('examind.components.style.editor', [
    'examind.components.style.editor.existing',
    'examind.components.style.editor.new',
    'examind.components.style.editor.import'
])
    .controller('StyleEditorController', StyleEditorController)
    .directive('styleEditor', styleEditorDirective);

function styleEditorDirective() {
    return {
        restrict: "E",
        templateUrl: "components/style-editor/style-editor.html",
        controller: 'StyleEditorController',
        controllerAs: "styleEdCtrl",
        scope: {
            selectedDataRef: "=",
            styleEditorOutput: "&",
            associateStyleToDataLayer: "&",
            disableAssociateBtn: "&"
        }
    };
}

function StyleEditorController($scope, Examind) {
    var self = this;

    // The configuration of the style editor
    self.styleEditorConfig = {
        defaultStyleMode: 'existing',
        showExistingModeBtn: true,
        showNewModeBtn: true,
        showImportModeBtn: true
    };

    // Object to fill with the result of the style editor
    self.styleEditorOutput = $scope.styleEditorOutput();

    self.associateStyleToDataLayer = $scope.associateStyleToDataLayer();

    self.disableAssociateBtn = $scope.disableAssociateBtn();

    // The data layer to apply the style
    self.selectedDataRef = $scope.selectedDataRef || {};

    // The style object that used in the duplication or edition of style
    self.styleToEdit = null;

    self.addStyleMode = self.styleEditorConfig.defaultStyleMode;

    self.changeAddStyleMode = function (mode) {
        self.addStyleMode = mode;
    };

    self.returnToExistingStyles = function () {
        self.addStyleMode = 'existing';
    };

    self.isSelectedAddStyleMode = function (mode) {
        return self.addStyleMode === mode;
    };

    self.createNewStyle = function () {
        self.styleToEdit = {
            name: self.selectedDataRef.dataLayer ? self.selectedDataRef.dataLayer.name + "-sld" : "default-sld",
            rules: []
        };
        self.changeAddStyleMode('new');
    };

    /**
     * Configure sld editor with given style object to edit them.
     * @param styleObj
     */
    self.editStyle = function (styleObj) {
        if (!styleObj || !styleObj.id) {
            return;
        }
        Examind.styles.getStyle(styleObj.id)
            .then(function (response) {
                self.styleToEdit = response.data;
                self.changeAddStyleMode('edit');
            });
    };

    /**
     * Configure the sld editor with a copy of the given style object
     * to create a new style based.
     * @param styleObj
     */
    self.duplicateStyle = function (styleObj) {
        if (!styleObj || !styleObj.id) {
            return;
        }
        Examind.styles.getStyle(styleObj.id)
            .then(function (response) {
                self.styleToEdit = response.data;
                var styleName = styleObj.name;
                if (styleName.match(/-\d{9}\d*$/g)) {
                    self.styleToEdit.name = styleName.substring(0, styleName.lastIndexOf("-")) + "-" + new Date().getTime();
                } else {
                    self.styleToEdit.name = styleName + "-" + new Date().getTime();
                }
                self.changeAddStyleMode('duplicate');
            });
    };
}
