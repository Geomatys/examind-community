angular.module('examind.components.wizardDataImport.step1.remote.source', [])
    .controller('RemoteSourceController', RemoteSourceController)
    .directive('remoteSource', remoteSourceDirective);

function remoteSourceDirective() {
    return {
        restrict: 'E',
        require: '^step1Wizard',
        controller: 'RemoteSourceController',
        controllerAs: 'remoteCtrl',
        templateUrl: 'components/wizard-data-import/steps/step1/remote-source/remote-source.html',
        scope: {
            remote: '=',
            processing: '=',
            stores: '=',
            formSchema: '=',
            advConfig: '=',
            fileListRef: '=',
            fileExplorerState: '=',
            createDataSource: '&',
            getFileList: '&',
            getDataSourceId: '&',
            setDataSourceId: '&',
            setDataSource: '&',
            selectStore: '&',
            canShowProviderConfigProperties: '&',
            showAdvancedConfigBlock: '&',
            selectAll: '&',
            hideField: '&',
            upDir: '&',
            openDir: '&',
            disableUpDir: '&',
            isDisabledFile: '&',
            getDataSource: '&',
            deleteDataSource: '&',
            getStores: '&',
            wizardValues: '='
        }
    };
}

function RemoteSourceController($scope, $translate, Examind, Growl, cfpLoadingBar) {
    var self = this;

    self.remote = $scope.remote;

    self.wizardValues = $scope.wizardValues;

    self.processing = $scope.processing;

    self.stores = $scope.stores;

    self.formSchema = $scope.formSchema;

    self.advConfig = $scope.advConfig;

    self.fileListRef = $scope.fileListRef;

    self.fileExplorerState = $scope.fileExplorerState;

    self.createDataSource = $scope.createDataSource();

    self.getFileList = $scope.getFileList();

    self.getDataSourceId = $scope.getDataSourceId();

    self.setDataSourceId = $scope.setDataSourceId();

    self.setDataSource = $scope.setDataSource();

    self.getDataSource = $scope.getDataSource();

    self.selectStore = $scope.selectStore();

    self.canShowProviderConfigProperties = $scope.canShowProviderConfigProperties();

    self.showAdvancedConfigBlock = $scope.showAdvancedConfigBlock();

    self.selectAll = $scope.selectAll();

    self.hideField = $scope.hideField();

    self.upDir = $scope.upDir();

    self.openDir = $scope.openDir();

    self.disableUpDir = $scope.disableUpDir();

    self.isDisabledFile = $scope.isDisabledFile();

    self.getStores = $scope.getStores();

    self.deleteDataSource = $scope.deleteDataSource();

    // The list of formSchema of all the supported db stores
    self.storesSchemas = {};

    self.urlFileExplorerConfig = {
        hideResetBtn: true
    };

    // List of the default supported protocols for the remote source
    self.defaultUrlProtocols = [
        {
            id: 'http',
            name: 'HTTP',
            connection: {
                login: '',
                password: ''
            },
            readFromRemote: false,
            scheme: 'http://',
            isDefaultProtocol: true
        }, {
            id: 'https',
            name: 'HTTPS',
            connection: {
                login: '',
                password: ''
            },
            readFromRemote: false,
            scheme: 'https://',
            isDefaultProtocol: true
        }, {
            id: 'ftp',
            name: 'FTP',
            connection: {
                login: '',
                password: ''
            },
            readFromRemote: false,
            scheme: 'ftp://',
            isDefaultProtocol: true
        }, {
            id: 's3',
            name: 'S3',
            connection: {
                login: '',
                password: '',
                region: ''
            },
            readFromRemote: false,
            scheme: 's3://',
            isDefaultProtocol: true
        }, {
            id: 'smb',
            name: 'SMB',
            connection: {
                login: '',
                password: ''
            },
            readFromRemote: false,
            scheme: 'smb://',
            isDefaultProtocol: true
        }, {
            id: 'file',
            name: 'Server File',
            i18n: 'data.modal.server.title',
            readFromRemote: true,
            scheme: 'file://',
            isDefaultProtocol: true
        }
    ];

    /**
     * The method used to select and to file the url protocol schema
     * @param protocol
     */
    self.selectProtocol = function (protocol) {
        // Delete the old data source and clean all the parameters.
        self.deleteDataSource();
        self.showAllowedFSList = false;

        if (angular.isUndefined(self.remote.url)) {
            self.remote.protocol = protocol;
        } else {
            if (self.remote.protocol && self.remote.url && self.remote.url.startsWith(self.remote.protocol.scheme)) {
                self.remote.url = self.remote.url.replace(self.remote.protocol.scheme, '');
            }
            self.remote.protocol = protocol;
            if (protocol.scheme && !self.remote.url.startsWith(protocol.scheme)) {
                self.remote.url = protocol.scheme;
            }
            if (!self.remote.protocol.isDefaultProtocol) {
                self.formSchema.schema = self.storesSchemas[self.remote.protocol.id];
            }
            if (self.remote.fileSystemSuffix && self.remote.protocol.id !== 'file') {
                delete self.remote.fileSystemSuffix;
            }
        }

        self.fillAllowedFileSystems();
    };

    self.fillAllowedFileSystems = function () {
        if (self.remote.protocol && self.remote.protocol.id === 'file') {
            self.fileSystemSuffix = self.remote.fileSystemSuffix ? self.remote.fileSystemSuffix : '';
            Examind.admin.getAllowedFS()
                .then(function (response) {
                        var allowedFSList = response.data.list;
                        if (allowedFSList) {
                            self.allowedFS = [];
                            allowedFSList.forEach(function (str) {
                                if (str.endsWith('/')) {
                                    str = str.slice(0, -1);
                                }
                                if (self.allowedFS.indexOf(str) === -1) {
                                    self.allowedFS.push(str);
                                }
                            });

                            if (self.allowedFS.length > 0) {
                                self.remote.url = self.allowedFS[0];
                                self.showAllowedFSList = true;
                            }
                        }
                    },
                    function (err) {
                        console.error(err);
                    });

        }
    };

    /**
     * Check if the protocol is selected or not
     * @param protocolId
     * @returns {RTCIceProtocol | *}
     */
    self.isSelectedProtocol = function (protocolId) {
        return self.remote.protocol && angular.equals(self.remote.protocol.id, protocolId);
    };

    /**
     * Check if the selected protocol is one of the default protocol
     * to show the login form and the file explorer
     * @returns {*}
     */
    self.isDefaultProtocol = function () {
        return self.remote.protocol && self.remote.protocol.isDefaultProtocol;
    };

    // connect to the URL source
    self.urlConnection = function () {
        if (!self.remote.protocol) {
            return;
        }
        var dataSource;
        var explore;
        var create;
        switch (self.remote.protocol.id) {
            case 'ftp':
            case 's3' :
            case 'smb':
            case 'file':
            case 'gcs':
                dataSource = {
                    type: self.remote.protocol.id,
                    url: self.remote.protocol.id === 'file' ? self.getUrlWithSuffix() : self.remote.url,
                    readFromRemote: self.remote.protocol.readFromRemote
                };
                if (self.remote.protocol.connection) {
                    dataSource.username = self.remote.protocol.connection.login;
                    dataSource.pwd = self.remote.protocol.connection.password;
                }
                //TODO temporary fix error for ftp url witch should never end with '/' this should be fixed in server side, see issue EXSERV-356.
                if (dataSource.type === 'ftp' && dataSource.url && endsWith(dataSource.url, '/')) {
                    dataSource.url = dataSource.url.substr(0, dataSource.url.length - 1);
                }
                //fix file protocol if missing
                if (dataSource.type === 'file' && dataSource.url.indexOf('file://') !== 0) {
                    dataSource.url = 'file://' + dataSource.url;
                }
                // special case for S3 remote
                if (dataSource.type === 's3') {
                    if (self.remote.protocol.readFromRemote) {
                        dataSource.permanent = true;
                    }
                    if (self.remote.protocol.connection.region) {
                        dataSource.properties = {
                                                 "aws.region": self.remote.protocol.connection.region
                                               };
                    }
                }
                explore = true;
                var reg = new RegExp('file:\/*$');
                if (dataSource.type === 'file' && reg.test(dataSource.url)) { //do not show root machine
                    create = false;
                } else {
                    create = true;
                }
                break;
            case 'http':
            case 'https':
                if (self.getDataSourceId()) {
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                    Examind.dataSources.uploadDistantFileToDataSource(self.getDataSourceId(), self.remote.url)
                        .then(function (response) {
                                self.getFileList('/');
                                self.getStores();
                                cfpLoadingBar.complete();
                            },
                            function (reason) {
                                $translate('wiz.data.import.step1.msg.err.download.remote.file')
                                    .then(function (translatedMsg) {
                                        Growl('error', 'Error', translatedMsg);
                                    });
                                cfpLoadingBar.complete();
                            });

                } else {
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                    Examind.dataSources.uploadDistantFile(self.remote.url, self.remote.protocol.connection.login, self.remote.protocol.connection.password)
                        .then(function (response) {
                                cfpLoadingBar.complete();
                                self.setDataSourceId(response.data);
                                cfpLoadingBar.start();
                                cfpLoadingBar.inc();
                                Examind.dataSources.get(self.getDataSourceId())
                                    .then(function (response) {
                                        self.setDataSource(response.data);
                                        self.getFileList('/');
                                        self.getStores();
                                        cfpLoadingBar.complete();
                                    }, function (reason) {//error
                                        $translate('wiz.data.import.step1.msg.err.get.data.source.info')
                                            .then(function (translatedMsg) {
                                                Growl('error', 'Error', translatedMsg);
                                            });
                                        cfpLoadingBar.complete();
                                    });
                            },
                            function (reason) {
                                $translate('wiz.data.import.step1.msg.err.download.remote.file')
                                    .then(function (translatedMsg) {
                                        Growl('error', 'Error', translatedMsg);
                                    });
                                cfpLoadingBar.complete();
                            });
                }
                create = false;
                break;
            // dynamic url stores
            default:
                dataSource = {
                    type: 'dynamic_url',
                    url: self.remote.url,
                    storeId: self.remote.protocol.id
                };
                explore = false;
                create = true;
        }
        if (create) {
            self.createDataSource(dataSource, 'url', explore);
        }
    };

    self.getUrlWithSuffix = function () {
        self.remote.fileSystemSuffix = self.fileSystemSuffix;
        return self.remote.url + (self.fileSystemSuffix.startsWith('/') ? '' : '/') + self.fileSystemSuffix;
    };

    self.changeReadFromRemoteFlag = function () {
        if (!self.getDataSource()) {
            return;
        }
        var dataSource = self.getDataSource();
        dataSource.readFromRemote = self.remote.protocol.readFromRemote;
        if (dataSource.type === 's3') {
            dataSource.permanent = dataSource.readFromRemote;
        }
        self.setDataSource(dataSource);
    };
    
    self.showReadFromRemoteOption = function () {
        return self.remote.protocol.id !== 'http' && self.remote.protocol.id !== 'https' && self.remote.protocol.id !== 'ftp';
    };
    
    self.showDeepOption = function () {
        return self.remote.protocol.id === 'file' || self.remote.protocol.id === 's3' || self.remote.protocol.id === 'ftp';
    };

    self.getBtnLabelFor = function (protocolId) {
        switch (protocolId) {
            case 'http':
            case 'https':
                return 'wiz.data.import.step1.label.download';
            case 'file':
                return 'wiz.data.import.step1.label.load';
            default:
                return 'wiz.data.import.step1.label.connection';
        }
    };

    self.init = function () {
        self.urlProtocols = self.defaultUrlProtocols;
        cfpLoadingBar.start();
        cfpLoadingBar.inc();
        // retrieve dynamic URL data store
        Examind.datas.getAllDataStoreConfigurations('service')
            .then(
                function (response) {
                    cfpLoadingBar.complete();
                    var stores = response.data.types;
                    stores.forEach(function (element) {
                        // exclude WPS store
                        if (element.id !== 'wps') {
                            var protocol = {
                                id: element.id,
                                name: element.title,
                                tag: element.tag
                            };
                            self.urlProtocols.push(protocol);
                            // Save the schema of each db store
                            self.storesSchemas[element.id] = element;
                        }
                    });
                },
                function (response) {
                    $translate('wiz.data.import.step1.msg.err.get.remote.types')
                        .then(function (translatedMsg) {
                            Growl('error', 'Error', translatedMsg);
                        });
                    cfpLoadingBar.complete();
                }
            );
        self.fillAllowedFileSystems();
    };

    function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }

    self.init();

}
