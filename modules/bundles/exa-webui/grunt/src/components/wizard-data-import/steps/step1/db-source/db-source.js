angular.module('examind.components.wizardDataImport.step1.db.source', [
    'examind.components.json.forms.builder'
])
    .controller('DbSourceController', DbSourceController)
    .directive('dbSource', dbSourceDirective);

function dbSourceDirective() {
    return {
        restrict: "E",
        require: '^step1Wizard',
        controller: 'DbSourceController',
        controllerAs: 'dbCtrl',
        templateUrl: 'components/wizard-data-import/steps/step1/db-source/db-source.html',
        scope: {
            db: "=",
            formSchema: "=",
            advConfig: "=",
            createDataSource: "&",
            autoFillFields: "&",
            canShowProviderConfigProperties: "&",
            showAdvancedConfigBlock: "&",
            hideField: "&"
        }
    };
}

function DbSourceController($scope, $translate, Examind, cfpLoadingBar, Growl) {
    var self = this;

    self.db = $scope.db;

    self.formSchema = $scope.formSchema;

    self.advConfig = $scope.advConfig;

    self.createDataSource = $scope.createDataSource();

    self.autoFillFields = $scope.autoFillFields();

    self.canShowProviderConfigProperties = $scope.canShowProviderConfigProperties();

    self.showAdvancedConfigBlock = $scope.showAdvancedConfigBlock();

    self.hideField = $scope.hideField();

    // The list of all the support database types
    self.databaseTypes = [];

    // The list of formSchema of all the supported db stores
    self.storesSchemas = {};

    // Disable the database connection fields until the user select a database type
    self.disableDBFields = function () {
        return !self.db || !self.db.type || angular.equals(self.db.type, '');
    };

    // The condition to disable the connection btn until fill all the necessary fields
    self.notFilledFields = function () {
        if (self.db.type && self.db.host !== '' && self.db.port !== '' && self.db.name !== '' &&
            self.db.user !== '' && self.db.password !== '') {
            return false;
        }
        return true;
    };

    // Connect to the database source
    self.dbConnection = function () {
        // TODO ... dynamic jdbc driver
        var dburl = "postgres://" + self.db.host + ":" + self.db.port + "/" + self.db.name;

        var dataSource = {
            type: "database",
            url: dburl,
            username: self.db.user,
            pwd: self.db.password,
            storeId: self.db.type.id
        };

        self.createDataSource(dataSource, 'database', false);
    };

    self.selectDbStore = function () {
        if (!self.db.type || !self.db.type.id) {
            return;
        }
        // Get the saved formSchema of the selected db store
        self.formSchema.schema = self.storesSchemas[self.db.type.id];
        self.advConfig.showAdvConfig = false;
        if (!self.notFilledFields()) {
            self.autoFillFields();
        }
    };

    self.init = function () {
        cfpLoadingBar.start();
        cfpLoadingBar.inc();
        // retrieve dynamic URL data store
        Examind.datas.getAllDataStoreConfigurations('jdbc')
            .then(function (response) {
                    cfpLoadingBar.complete();
                    var stores = response.data.types;
                    stores.forEach(function (element) {
                        var dbType = {
                            id: element.id,
                            name: element.id.toUpperCase(),
                            value: {
                                id: element.id,
                                tag: element.tag
                            }
                        };
                        self.databaseTypes.push(dbType);
                        // Save the schema of each db store
                        self.storesSchemas[element.id] = element;
                    });
                },
                function (response) {
                    cfpLoadingBar.complete();
                    $translate('wiz.data.import.step1.msg.err.get.db.types')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                }
            );
    };

    self.init();

}