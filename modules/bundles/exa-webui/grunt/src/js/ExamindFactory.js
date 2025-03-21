
/**
 * @author Johann Sorel (Geomatys)
 */
var module = angular.module('examind-factory', []);

/**
 * Instance of Examind client.
 *
 * @param {type} $http
 * @param {type} url
 * @returns {undefined}
 */
function Examind($http, url) {
    var self = this;
    if(! url) {
        return;
    }
    if (url.lastIndexOf('/') !== url.length-1) {url = url + '/';}
    if (url.lastIndexOf('API/') !== url.length-4) {url = url + 'API/';}
    self.baseUrl = url;

    /**
     * Prepare a request, updating url and adding token.
     *
     * @param {{method: string, url: string}} query $http request content
     * @returns {Promise}
     */
    self.request = function(query) {
        query.url = query.url.replace('@url', self.baseUrl);
        if (query.url.indexOf('http') !== 0) {
            query.url = self.baseUrl + query.url;
        }
        if (angular.isUndefined(query.headers)) {
            query.headers = {};
        }
        return $http(query);
    };

    /**
     * Authentication API
     */
    self.authentication = {

        /**
         * Log in examind.
         *
         * @param {String} user
         * @param {String} password
         * @returns {Promise}
         */
        login : function(user, password) {
            return self.request({
                method: 'POST',
                url: 'auth/login',
                headers: {'Accept': 'application/json'},
                data: {
                    username:user,
                    password:password
                    }
                }).then(function(response){
                    return response;
                });
        },

        /**
         * Logout examind.
         */
        logout : function() {
            return self.request({
                method: 'DELETE',
                url: 'auth/logout'
                }).then(function(response){
                    return response;
                });
        },

        /**
         * Request a forgot password email.
         *
         * @param {String} email user email
         * @returns {Promise}
         */
        forgotPassword : function(email) {
            return self.request({
                method: 'POST',
                url: 'auth/forgotPassword',
                data: {
                    email:email
                    }
                });
        },

        /**
         * Request a reset password email.
         *
         * @param {String} password new user password
         * @param {String} uuid security uuid
         * @returns {Promise}
         */
        resetPassword : function(password, uuid) {
            return self.request({
                method: 'POST',
                url: 'auth/resetPassword',
                data: {
                    password: password,
                    uuid: uuid
                    }
                });
        },

 /**
         * Set refresh token url.
         *
         * @param {type} url
         * @returns {undefined}
         */
        setTokenRefreshURL : function(url) {
             window.localStorage.setItem('cstlRefreshURL', url);
        },

        /**
         * Get refresh token url.
         *
         * @returns {String}
         */
        getTokenRefreshURL : function() {
            var url = window.localStorage.getItem('cstlRefreshURL');
            if (!url) {
                url = 'auth/extendToken';
            }
            return url;
        },

        /**
         * Renew authentication token.
         */
        renewToken: function() {
            self.request({
                    method: 'GET',
                    url: self.authentication.getTokenRefreshURL()
                    })
                .then(function(response){
                    console.log("Token extended.");
            });
        },

        /**
         * Get account.
         *
         * @returns {Promise}
         */
        account : function() {
            return self.request({
                method: 'GET',
                url: 'auth/account',
                headers: {'Accept': 'application/json'}
                });
        }
    };

    /**
     * Administration API
     */
    self.admin = {

        /**
         * Run garbage collector.
         *
         * @returns {Promise}
         */
        runGC : function() {
            return self.request({
                method: 'GET',
                url: 'admin/rungc'
                });
        },

        /**
         * Get loggers.
         *
         * @returns {Promise}
         */
        getLoggers : function() {
            return self.request({
                method: 'GET',
                url: 'admin/loggers'
                });
        },

        /**
         * Update a logger.
         *
         * @param {Logger} logger
         * @returns {Promise}
         */
        updateLogger : function(logger) {
            return self.request({
                method: 'PUT',
                url: 'admin/loggers',
                data:logger
                });
        },

        /**
         * Get global contact informations.
         *
         * @returns {Promise}
         */
        getContact : function() {
            return self.request({
                method: 'GET',
                url: 'admin/contact'
                });
        },

        /**
         * Update global contact informations.
         *
         * @param {Contact} contact
         * @returns {Promise}
         */
        updateContact : function(contact) {
            return self.request({
                method: 'POST',
                url: 'admin/contact',
                data: contact
                });
        },

        /**
         * Get configuration properties.
         *
         * @returns {Promise}
         */
        getProperties : function() {
            return self.request({
                method: 'GET',
                url: 'admin/properties'
                });
        },

        /**
         * Get configuration property.
         *
         * @returns {Promise}
         */
        getProperty : function(key) {
            return self.request({
                method: 'GET',
                url: 'admin/property/' + key
                });
        },

        /**
         * update configuration property.
         *
         * @returns {Promise}
         */
        setProperty : function(key, value) {
            return self.request({
                method: 'POST',
                url: 'admin/property/' + key,
                headers: {'Accept': 'application/json'},
                data: {
                    value : value
                }
            });
        },

        /**
         * Get Allowed File System Paths
         *
         * @returns {Promise}
         */
        getAllowedFS: function () {
            return self.request({
                method: 'GET',
                url: 'admin/property/exa.allowed.fs.path'
            });
        },

        /**
         * Get WMS background url
         *
         * @returns {Promise}
         */
        getWMSBackground: function () {
            return self.request({
                method: 'GET',
                url: 'admin/property/examind.wms.background'
            });
        },

        /**
         * Set WMS background url
         *
         * @returns {Promise}
         */
        setWMSBackground: function (wmsUrl, wmsLayer) {
            var val;
            if (wmsUrl && wmsLayer) {
                val = wmsUrl + '|' + wmsLayer;
            } else {
                val = null;
            }
            return self.request({
                method: 'POST',
                url: 'admin/property/examind.wms.background',
                data: {
                    value: val
                }
            });
        }
    };

    /**
     * Style API
     */
    self.styles = {

        /**
         * Create style object.
         *
         * @param {Style} style
         * @param {String} type 'sld' or 'sld_temp', default is 'sld'
         * @returns {Promise}
         */
        createStyle : function(style, type) {
            if (angular.isUndefined(type)) {type = 'sld';}
            return self.request({
                method: 'POST',
                url: 'internal/styles?type='+type,
                headers: {'Accept': 'application/json'},
                data: style
                });
        },

        /**
         * Returns style object.
         *
         * @param {int} id
         * @returns {Promise}
         */
        getStyle : function(id) {
            return self.request({
                method: 'GET',
                url: 'styles/'+id,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Delete style object.
         *
         * @param {int} id
         * @returns {Promise}
         */
        deleteStyle : function(id) {
            return self.request({
                method: 'DELETE',
                url: 'styles/'+id,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Returns styles object.
         *
         * @param {Object} pagedSearch
         * @returns {Promise}
         */
        searchStyles : function(pagedSearch) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/search',
                headers: {'Accept': 'application/json'},
                data : pagedSearch
                });
        },

        /**
         * Returns styles object.
         *
         * @returns {Promise}
         */
        getStyles : function() {
            return self.request({
                method: 'GET',
                url: 'styles?mode=brief',
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Update style object.
         *
         * @param {int} id
         * @param {Style} style
         * @returns {Promise}
         */
        updateStyle : function(id, style) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/'+id,
                headers: {'Accept': 'application/json'},
                data: style
                });
        },

        /**
         * Link a style to a data.
         *
         * @param {int} styleId
         * @param {int} dataId
         * @returns {Promise}
         */
        link : function(styleId, dataId) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/'+styleId+'/data/'+dataId,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Unlink a style to a data.
         *
         * @param {int} styleId
         * @param {int} dataId
         * @returns {Promise}
         */
        unlink : function(styleId, dataId) {
            return self.request({
                method: 'DELETE',
                url: 'internal/styles/'+styleId+'/data/'+dataId,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Import a XML style file.
         * @param formData
         * @returns {Promise}
         */
        importStyleFile : function(formData) {
            return self.request({
                headers: {'Content-Type': undefined},
                method: 'POST',
                url: 'styles',
                transformRequest: angular.identity,
                data: formData,
                cache: false
            });
        },

        /**
         * Create style download URL.
         *
         * @param {integer} id stle ID
         * @returns {String} url
         */
        getExportStyleURL : function(id) {
            return self.baseUrl+'styles/'+id+'?f=file';
        },

        // INTERNAL ///////////

        /**
         * Create automatic intervals style.
         *
         * @param {doc} parameters
         * @param {int} id mandatory
         * @returns {undefined}
         */
        generateAutoInterval : function(parameters, id) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/' + id + '/generateAutoInterval',
                headers: {'Accept': 'application/json'},
                data: parameters
                });
        },

        /**
         * Create singular values style.
         *
         * @param {doc} parameters
         * @param {int} id mandatory
         * @returns {undefined}
         */
        generateAutoUniqueStyle : function(parameters, id) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/' + id + '/generateAutoUnique',
                headers: {'Accept': 'application/json'},
                data: parameters
                });
        },

        /**
         * Get chart informations.
         *
         * @param {doc} parameters
         * @returns {undefined}
         */
        getChartData : function(parameters) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/getChartDataJson',
                headers: {'Accept': 'application/json'},
                data: parameters
                });
        },

        /**
         * Get Palette description.
         *
         * @param {Integer} styleId
         * @param {String} ruleName
         * @param {Integer} interval
         * @returns {undefined}
         */
        getPalette : function(styleId, ruleName, interval) {
            return self.request({
                method: 'GET',
                url: 'internal/styles/'+styleId+'/'+ruleName+'/'+interval,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Get histogram.
         *
         * @param {Integer} dataId
         * @returns {undefined}
         */
        getHistogram : function(dataId) {
            return self.request({
                method: 'GET',
                url: 'internal/styles/histogram/'+dataId,
                headers: {'Accept': 'application/json'}
                });
        },
        changeSharedPropertyMulti : function (shared, idList) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/shared/'+shared,
                headers: {'Accept': 'application/json'},
                data: idList
            });
        },
        changeSharedProperty : function (shared,id) {
            return self.request({
                method: 'POST',
                url: 'internal/styles/' + id + '/shared/' + shared,
                headers: {'Accept': 'application/json'}
            });
        },

        existStyleName : function(name) {
            return self.request({
                method: 'GET',
                url: 'internal/styles/name/' + name + '/exist',
                headers: {'Accept': 'application/json'}
            });
        },

    };

    /**
     * User API
     */
    self.users = {

        /**
         * Count number of users.
         *
         * @returns {Promise}
         */
        getCount : function() {
            return self.request({
                method: 'GET',
                url: 'users/count'
                });
        },

        /**
         * Return the user with the specified id.
         *
         * @returns {Promise}
         */
        getUser : function(id) {
            return self.request({
                method: 'GET',
                url: 'users/' + id
                });
        },
        /**
         * Return the current logged user.
         *
         * @returns {Promise}
         */
        getCurrentUser : function() {
            return self.request({
                method: 'GET',
                url: 'internal/users/current'
                });
        },
        /**
         * Returns users object.
         *
         * @param {Object} pagedSearch
         * @returns {Promise}
         */
        searchUsers : function(pagedSearch) {
            return self.request({
                method: 'POST',
                url: 'users/search',
                headers: {'Accept': 'application/json'},
                data : pagedSearch
                });
        },
        /**
         * Activate / deactivate user account.
         *
         * @returns {Promise}
         */
        activate : function(id) {
            return self.request({
                method: 'PUT',
                url: 'users/' + id + '/activate'
                });
        }
    };

    /**
     * Sensor API
     */
    self.sensors = {

        /**
         * List all sensors.
         *
         * @returns {Promise}
         */
        list : function() {
            return self.request({
                method: 'GET',
                url: 'sensors'
                });
        },

        /**
         * add a new sensor.
         *
         * @returns {Promise}
         */
        add : function(sensor) {
            return self.request({
                method: 'PUT',
                url: 'sensors/add',
                data:sensor
                });
        },

        /**
         * Delete a sensor.
         *
         * @returns {Promise}
         */
        delete : function(sensorId, removeData) {
            return self.request({
                method: 'DELETE',
                url: 'sensors/' + sensorId + '?removeData=' + removeData
                });
        },

        /**
         * Get the sensor metadata.
         *
         * @returns {Promise}
         */
        getMetadata : function(sensorId, type, prune) {
            return self.request({
                method: 'GET',
                url: 'sensors/' + sensorId + '/metadata/' +  type + '/' + prune
                });
        },

        /**
         * Save the sensor metadata.
         *
         * @returns {Promise}
         */
        saveMetadata : function(sensorId, type, sensor) {
            return self.request({
                method: 'POST',
                url: 'sensors/' + sensorId + '/metadata/' +  type,
                data:sensor
                });
        },

        /**
         * Generate the sensor metadata.
         *
         * @returns {Promise}
         */
        generateSML : function(sensorId) {
            return self.request({
                method: 'PUT',
                url: 'sensors/generate/' + sensorId
                });
        }
    };

    /**
     * Datasource API
     */
    self.dataSources = {

        /**
         * add a new datasource.
         *
         * @returns {Promise}
         */
        create : function(ds) {
            return self.request({
                method: 'POST',
                url: 'datasources',
                data:ds
                });
        },

        /**
         * update a datasource.
         *
         * @returns {Promise}
         */
        update : function(dataSource) {
            return self.request({
                method: 'PUT',
                url: 'datasources',
                data: dataSource
            });
        },

        /**
         * Delete a datasource.
         *
         * @returns {Promise}
         */
        delete : function(id) {
            return self.request({
                method: 'DELETE',
                url: 'datasources/' + id
                });
        },

        /**
         * Get the datasource.
         *
         * @returns {Promise}
         */
        get : function(id) {
            return self.request({
                method: 'GET',
                url: 'datasources/' + id
                });
        },

        /**
         * explore the datasource.
         *
         * @returns {Promise}
         */
        explore : function(id, path) {
            return self.request({
                method: 'GET',
                url: 'datasources/' + id + '/explore?path=' + encodeURIComponent(path)
                });
        },

        /**
         * Test a datasource.
         *
         * @returns {Promise}
         */
        test : function(ds) {
            return self.request({
                method: 'POST',
                url: 'datasources/test',
                data:ds
                });
        },

        /**
         * Upload multiple file and create a datasource.
         * Return the datasource created id
         *
         * @returns {Promise}
         */
        uploadFiles : function(formData) {
            return self.request({
                method: 'POST',
                headers: {'Content-Type': undefined},
                url: 'datasources/uploads',
                transformRequest: angular.identity,
                cache: false,
                data: formData
                });
        },

        /**
         * Upload single file and create a datasource.
         *
         * Return the datasource created id
         *
         * @returns {Promise}
         */
        uploadFile : function(formData) {
            return self.request({
                method: 'POST',
                headers: {'Content-Type': undefined},
                url: 'datasources/upload',
                transformRequest: angular.identity,
                cache: false,
                data: formData
                });
        },

        /**
         * Upload single file into a datasource.
         *
         *
         * @returns {Promise}
         */
        uploadFileToDataSource : function(id, formData, timeoutPromise) {
            return self.request({
                method: 'POST',
                headers: {'Content-Type': undefined},
                url: 'datasources/' + id + '/upload',
                transformRequest: angular.identity,
                cache: false,
                data: formData,
                timeout:timeoutPromise
                });
        },

        /**
         * Upload multiple files into a datasource.
         *
         *
         * @returns {Promise}
         */
        uploadFilesToDataSource : function(id, formData) {
            return self.request({
                method: 'POST',
                headers: {'Content-Type': undefined},
                url: 'datasources/' + id + '/uploads',
                transformRequest: angular.identity,
                cache: false,
                data: formData
                });
        },

        /**
         * Upload distant file and create a datasource.
         *
         * Return the datasource created id
         *
         * @returns {Promise}
         */
        uploadDistantFile : function(distUrl, user, pwd) {
            var url = 'datasources/upload/distant?url=' + distUrl;
            if (user) {
                url = url + '&user=' + user;
            }
            if (pwd) {
                url = url + '&pwd=' + pwd;
            }
            return self.request({
                method: 'POST',
                url: url
            });
        },

        /**
         * Upload distant file into a datasource.
         *
         *
         * @returns {Promise}
         */
        uploadDistantFileToDataSource : function(id, url) {
            return self.request({
                method: 'POST',
                url: 'datasources/' + id + '/upload/distant?url=' + url
            });
        },

        downloadStoreFilesV3 : function(store) {
            return self.request({
                method: 'POST',
                url: 'datasources/store/downloadV3',
                data: store
            });
        },

        addSelectedPaths : function(id, selectedFiles) {
            return self.request({
                method: 'POST',
                url: 'datasources/' + id + '/selectedPath',
                data: selectedFiles
            });
        },

        /**
         * Get the datasource analysis.
         *
         * @returns {Promise}
         */
        getAnalysisV3 : function(dataSourceId, storeParams) {
            return self.request({
                method: 'POST',
                url: 'datasources/' + dataSourceId +'/analysisV3',
                headers: {'Accept': 'application/json'},
                data: storeParams
            });
        },

        /**
         * Get the datasource analysis sample.
         *
         * @returns {Promise}
         */
        getAnalysisSample : function(dataSourceId, storeParams) {
            return self.request({
                method: 'POST',
                url: 'datasources/' + dataSourceId +'/analysis/sample',
                headers: {'Accept': 'application/json'},
                data: storeParams
            });
        },

        /**
         * Get the datasource analysis sample.
         *
         * @returns {Promise}
         */
        getAnalysisBatch : function(dataSourceId, params) {
            return self.request({
                method: 'POST',
                url: 'datasources/' + dataSourceId +'/analysis/batch',
                headers: {'Accept': 'application/json'},
                data: params
            });
        },

        /**
         * Get the datasource analysis state.
         *
         * @returns {Promise}
         */
        getAnalysisState : function(dataSourceId) {
            return self.request({
                method: 'GET',
                url: 'datasources/' + dataSourceId +'/analysis/state'
            });
        },

        /**
         * Get the list of stores from analysing the datasource
         * @param dataSourceId
         * @param async
         * @param deep
         * @returns {Promise}
         */
        computeDatasourceStores : function(dataSourceId, async, deep) {
            return self.request({
                method: 'GET',
                url: 'datasources/' + dataSourceId + '/stores?async=' + async + '&deep=' + deep
                });
        }

    };

    /**
     * Role API
     */
    self.roles = {

        /**
         * Count number of users.
         *
         * @returns {Promise}
         */
        getRoles : function() {
            return self.request({
                method: 'GET',
                url: 'roles'
                });
        }
    };

    /**
     * Sensor Service API
     */
    self.sensorServices = {

        /**
         * Remove a sensor.
         *
         * @returns {Promise}
         */
        removeSensor : function(id, sensorID) {
            return self.request({
                method: 'DELETE',
                url: 'SensorService/' + id + '/sensor/' + encodeURIComponent(sensorID)
                });
        },

        /**
         * Return Sensor tree.
         *
         * @returns {Promise}
         */
        getSensorsTree : function(id) {
            return self.request({
                method: 'GET',
                 url: 'SensorService/' + id + '/sensors'
                });
        },

        /**
         * Return Sensor features.
         *
         * @returns {Promise}
         */
        getFeatures : function(id, sensorID) {
            return self.request({
                method: 'GET',
                 url: 'SensorService/' + id + '/sensor/location/' + encodeURIComponent(sensorID)
                });
        },

        /**
         * Return Sensor features.
         *
         * @returns {Promise}
         */
        measuresForSensor : function(id, sensorID) {
            return self.request({
                method: 'GET',
                 url: 'SensorService/' + id + '/observedProperty/identifiers/' + encodeURIComponent(sensorID)
                });
        },

        /**
         * Link a Sensor Service service and a provider.
         *
         * @returns {Promise}
         */
        linkSensorProvider : function(id, providerID, fullLink) {
            return self.request({
                method: 'GET',
                 url: 'SensorService/' + id + '/link/' + providerID + '?fullLink=' + fullLink
                });
        },
        
        /**
         * Generate existing sensor for a sensor service.
         *
         * @returns {Promise}
         */
        generateSensorFromOMProvider : function(id) {
            return self.request({
                method: 'PUT',
                 url: 'SensorService/' + id + '/sensors/generate'
                });
        },

        /**
         * import a sensor into the Sensor Service.
         *
         * @returns {Promise}
         */
        importSensor : function(id, sensorID) {
            return self.request({
                method: 'PUT',
                url: 'SensorService/' + id + '/sensor/import/' + encodeURIComponent(sensorID)
                });
        },

        /**
         * import a data into the Sensor Service.
         *
         * @returns {Promise}
         */
        importData : function(id, dataID) {
            return self.request({
                method: 'PUT',
                url: 'SensorService/' + id + '/data/' + dataID
                });
        },

        /**
         * remove a data from the SensorService.
         *
         * @returns {Promise}
         */
        removeData : function(id, dataID) {
            return self.request({
                method: 'DELETE',
                url: 'SensorService/' + id + '/data/' + dataID
                });
        }
    };

    /**
     * OGC services API
     */
    self.ogcServices = {

        /**
         * Return an ogc service instance.
         *
         * @returns {Promise}
         */
        get : function(type, id, lang) {
            return self.request({
                method: 'GET',
                url: 'OGC/' + type + '/' + id + '/' + lang
            });
        },

        /**
         * Remove an ogc service instance.
         *
         * @returns {Promise}
         */
        delete : function(type, id) {
            return self.request({
                method: 'DELETE',
                url: 'OGC/' + type + '/' + id
            });
        },

        /**
         * Create an ogc service instance.
         *
         * @returns {Promise}
         */
        create : function(type, metadata) {
            return self.request({
                method: 'PUT',
                url: 'OGC/' + type,
                headers: {'Accept': 'application/json'},
                data: metadata
            });
        },

        /**
         * Restart an ogc service instance.
         *
         * @returns {Promise}
         */
        restart : function(type, id, stopFirst) {
            return self.request({
                method: 'POST',
                url: 'OGC/' + type + '/' + id + '/restart?stopFirst=' + stopFirst
            });
        },

        /**
         * Start an ogc service instance.
         *
         * @returns {Promise}
         */
        start : function(type, id) {
            return self.request({
                method: 'POST',
                url: 'OGC/' + type + '/' + id + '/start'
            });
        },

        /**
         * Start an ogc service instance.
         *
         * @returns {Promise}
         */
        stop : function(type, id) {
            return self.request({
                method: 'POST',
                url: 'OGC/' + type + '/' + id + '/stop'
            });
        },

        /**
         * Return an ogc service instance metadata.
         *
         * @returns {Promise}
         */
        getMetadata : function(type, id, lang) {
            return self.request({
                method: 'GET',
                url: 'OGC/' + type + '/' + id + '/metadata/' + lang
            });
        },

        /**
         * Update an ogc service instance metadata.
         *
         * @returns {Promise}
         */
        setMetadata : function(type, id, metadata) {
            return self.request({
                method: 'POST',
                url: 'OGC/' + type + '/' + id + '/metadata',
                data: metadata
            });
        },

        /**
         * Return an ogc service instance configuration.
         *
         * @returns {Promise}
         */
        getConfig : function(type, id) {
            return self.request({
                method: 'GET',
                url: 'OGC/' + type + '/' + id + '/config'
            });
        },

        /**
         * Update an ogc service instance configuration.
         *
         * @returns {Promise}
         */
        setConfig : function(type, id, conf) {
            return self.request({
                method: 'POST',
                url: 'OGC/' + type + '/' + id + '/config',
                data: conf
            });
        }
    };

    /**
     * CRS internal API
     */
    self.crs = {

        /**
         * return a list of crs names.
         *
         * @returns {Promise}
         */
        listAll : function() {
            return self.request({
                method: 'GET',
                url: 'internal/crs'
                });
        }
    };

    /**
     * Data API
     */
    self.datas = {
        getData : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/'+dataId
            });
        },
        getDataDescription : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/description',
                headers: {'Accept': 'application/json'}
            });
        },
        getGeographicExtent : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/geographicExtent',
                headers: {'Accept': 'application/json'}
            });
        },
        mergedDataExtent : function (dataIds) {
            return self.request({
                method: 'POST',
                url: 'datas/geographicExtent/merge',
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        getDataFolder : function (path, filtered) {
            return self.request({
                method: 'POST',
                url: 'internal/datas/datapath/'+filtered,
                headers: {'Accept': 'application/json'},
                data: path
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        getMetaDataFolder : function (path, filtered) {
            return self.request({
                method: 'POST',
                url: 'internal/datas/metadatapath/'+filtered,
                headers: {'Accept': 'application/json'},
                data: path
            });

        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        createDataset : function (datasetIdentifier, metadataFilePath) {
            return self.request({
                method: 'POST',
                url: 'internal/datasets',
                headers: {'Accept': 'application/json'},
                data: {
                    metadataFilePath : metadataFilePath,
                    datasetIdentifier : datasetIdentifier
                }
            });
        },
        createDatasetNew : function (datasetIdentifier, metadataValues, hidden) {
            return self.request({
                method: 'POST',
                url: 'datasets?identifier=' + datasetIdentifier + '&hidden=' + hidden,
                headers: {'Accept': 'application/json'},
                data: metadataValues
            });
        },
        deleteDataset : function (datasetId) {
            return self.request({
                method: 'DELETE',
                url: 'datasets/' + datasetId
            });
        },
        getDatasetData : function (datasetId) {
            return self.request({
                method: 'GET',
                url: 'datasets/' + datasetId + '/datas',
                headers: {'Accept': 'application/json'}
            });
        },
        getDatasetDataSummary : function (datasetId) {
            return self.request({
                method: 'GET',
                url: 'datasets/' + datasetId + '/datas/summary',
                headers: {'Accept': 'application/json'}
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        initMetadata : function (providerId, dataType, mergeWithUploadedMD) {
            return self.request({
                method: 'POST',
                url: 'internal/datas/init/metadata?providerId=' + providerId + '&dataType=' + dataType + '&mergeWithUploadedMD=' + mergeWithUploadedMD,
                headers: {'Accept': 'application/json'},
                data: {}
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        uploadData : function (formData) {
            return self.request({
                headers: {'Content-Type': undefined},
                method: 'POST',
                url: 'internal/datas/upload/data',
                transformRequest: angular.identity,
                data: formData,
                cache: false
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        uploadMetadata : function (formData) {
            return self.request({
                headers: {'Content-Type': undefined},
                method: 'POST',
                url: 'internal/datas/upload/metadata',
                transformRequest: angular.identity,
                data: formData,
                cache: false
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        proceedToImport : function (dataPath, metadataFilePath, dataType, dataName, extension, fsServer) {
            return self.request({
                method: 'POST',
                url: 'internal/datas/import/full',
                headers: {'Accept': 'application/json'},
                data: {
                    values : {
                        dataPath : dataPath,
                        metadataFilePath : metadataFilePath,
                        dataType : dataType,
                        dataName : dataName,
                        extension : extension,
                        fsServer : fsServer
                    }
                }
            });
        },
        getDataStoreConfiguration : function (storeId) {
            return self.request({
                method: 'GET',
                url: 'internal/datas/store/' + storeId,
                headers: {'Accept': 'application/json'}
            });
        },
        getAllDataStoreConfigurations : function (dataType) {
            return self.request({
                method: 'GET',
                url: 'internal/datas/store/list?type=' + dataType,
                headers: {'Accept': 'application/json'}
            });
        },
        putDataStoreConfiguration : function (selected, hidden, providerId) {
            var url = 'internal/datas/store';
            var separator = '?';

            if (hidden) {
                url = url + separator + 'hidden=' + hidden;
                separator = '&';
            }
            if (providerId) {
                url = url + separator + 'providerId=' + providerId;
            }
            return self.request({
                method: 'POST',
                url: url,
                headers: {'Accept': 'application/json'},
                data: selected
            });
        },
        getDatasetMetadata : function (datasetId, prune) {
            return self.request({
                method: 'GET',
                url: 'datasets/' + datasetId + '/metadata?prune=' + prune,
                headers: {'Accept': 'application/json'}
            });
        },
        getDatasetMetadataOld : function (identifier) {
            return self.request({
                method: 'GET',
                url: 'internal/datasets/metadata/' + identifier,
                headers: {'Accept': 'application/json'},
            });
        },
        getDataMetadata : function (dataId, prune) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/metadata?prune=' + prune,
                headers: {'Accept': 'application/json'}
            });
        },
        mergeMetadata : function (dataId, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'datas/' + dataId + '/metadata',
                headers: {'Accept': 'application/json'},
                data : metadataValues

            });
        },
        mergeMetadataDS : function (datasetId, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'datasets/' + datasetId + '/metadata',
                headers: {'Accept': 'application/json'},
                data : metadataValues
            });
        },
        mergeMetadataDSOld : function (identifier, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'internal/datasets/metadata/' + identifier,
                headers: {'Accept': 'application/json'},
                data : metadataValues
            });
        },
        findMetadata : function (term) {
            return self.request({
                method: 'GET',
                url: 'datas/search?term=' + term,
                headers: {'Accept': 'application/json'}
            });
        },
        createTiledProviderConform : function (dataId) {
            return self.request({
                method: 'POST',
                url: 'datas/' + dataId + '/pyramid',
                headers: {'Accept': 'application/json'}
            });
        },
        pyramidData : function (crs, layerName, dataIds) {
            return self.request({
                method: 'POST',
                url: 'datas/pyramid?crs='+crs+'&layerName='+layerName,
                headers: {'Accept': 'application/json'},
                data : dataIds
            });
        },
        describePyramid : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/describePyramid',
                headers: {'Accept': 'application/json'}
            });
        },
        getDataSummary : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/'+ dataId,
                headers: {'Accept': 'application/json'}
            });
        },
        getDataList : function () {
            return self.request({
                method: 'GET',
                url: 'datas'
            });
        },
        getDataListLight : function (fetchDataDescription) {
            return self.request({
                method: 'GET',
                url: 'datas?fetchDataDescription='+fetchDataDescription
            });
        },
        getDataListForType : function (type) {
            return self.request({
                method: 'GET',
                url: 'datas?type='+type
            });
        },
        getDataListForProvider : function (providerIdentifier) {
            return self.request({
                method: 'GET',
                url: 'internal/datas/provider/'+providerIdentifier
            });
        },
        /**
         * Count number of datas.
         *
         * @returns {Promise}
         */
        getCount : function() {
            return self.request({
                method: 'GET',
                url: 'datas/count'
            });
        },
        getDatasetList : function () {
            return self.request({
                method: 'GET',
                url: 'datasets'
            });
        },
        getPublishedDataList : function (published) {
            return self.request({
                method: 'GET',
                url: 'datas?published=' + published
            });
        },
        getPublishedDatasetList : function (published) {
            return self.request({
                method: 'GET',
                url: 'datasets?published=' + published
            });
        },
        getSensorableDataList : function (sensorable) {
            return self.request({
                method: 'GET',
                url: 'datas?sensorable='+sensorable
            });
        },
        getSensorableDatasetList : function (sensorable) {
            return self.request({
                method: 'GET',
                url: 'datasets/sensorable=' + sensorable
            });
        },
        searchDataset : function(pagedSearch) {
            return self.request({
                method: 'POST',
                url: 'datasets/search',
                headers: {'Accept': 'application/json'},
                data : pagedSearch
                });
        },
        includeData : function (dataId) {
            return self.request({
                method: 'POST',
                url: 'datas/' + dataId + '/include',
                headers: {'Accept': 'application/json'}
            });
        },
        acceptDatas : function (dataIds, hidden) {
            return self.request({
                method: 'POST',
                url: 'datas/accept?hidden=' + hidden,
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        acceptData : function (dataId, hidden) {
            return self.request({
                method: 'POST',
                url: 'datas/' + dataId + '/accept?hidden=' + hidden,
                headers: {'Accept': 'application/json'}
            });
        },
        changeHiddenFlag : function (dataIds, flag) {
            return self.request({
                method: 'POST',
                url: 'datas/hide/' + flag,
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        removeData : function (dataId, removeFiles) {
            return self.request({
                method: 'DELETE',
                url: 'datas/' + dataId + '?removeFiles=' + removeFiles
            });
        },
        removeDatas : function (dataIds, removeFiles) {
            return self.request({
                method: 'POST',
                url: 'datas/remove?removeFiles=' + removeFiles,
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        searchDatas : function(pagedSearch) {
            return self.request({
                method: 'POST',
                url: 'datas/search',
                headers: {'Accept': 'application/json'},
                data : pagedSearch
                });
        },
        downloadMetadataForData : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/metadata'
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        testExtension : function (extension) {
            return self.request({
                method: 'GET',
                url: 'internal/datas/testextension/' + extension,
                headers: {'Accept': 'application/json'}
            });
        },
        exportData : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/export'
            });
        },
        exportDatas : function (dataIds) {
            return self.request({
                method: 'POST',
                url: 'datas/export',
                data : dataIds,
                responseType: "arraybuffer"
            });
        },
        linkDataToSensor : function (dataId, sensorId) {
            return self.request({
                method: 'POST',
                url: 'datas/' + dataId + '/sensors/' + sensorId
            });
        },
        unlinkDataToSensor : function (dataId, sensorId) {
            return self.request({
                method: 'DELETE',
                url: 'datas/' + dataId + '/sensors/' + sensorId
            });
        },
        getAssociations : function (dataId) {
            return self.request({
                method: 'GET',
                url: 'datas/' + dataId + '/associations'
            });
        },
        deleteStyleAssociation : function (dataId, styleId) {
            return self.request({
                method: 'DELETE',
                url: 'datas/' + dataId + '/styles/' + styleId
            });
        },
        deleteStyleAssociations : function (dataId) {
            return self.request({
                method: 'DELETE',
                url: 'datas/' + dataId + '/styles'
            });
        },
        deleteStyleAssociationsMulti : function (dataIds) {
            return self.request({
                method: 'DELETE',
                url: 'datas/styles/unlink',
                headers: {'Accept': 'application/json',
                          'Content-Type':'application/json'},
                data: dataIds
            });
        },
        /**
         * This method has been removed from the server and should be removed (with all its use) from the front code.
         */
        saveUploadedMetadata : function(params) {
            return self.request({
                method: 'POST',
                url: 'internal/datas/saveUploadedMetadata',
                headers: {'Accept': 'application/json'},
                data : params
                });
        },
        existDatasetName : function (name) {
            return self.request({
                method: 'GET',
                url: 'datasets/name/' + name + '/exist',
                headers: {'Accept': 'application/json'}
            });
        },
        updateDataDataset : function (dataIds, datasetId) {
            return self.request({
                method: 'POST',
                url: 'datasets/' + datasetId + '/datas',
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        mergeModelDataMetadata : function (modelId, dataIds) {
            return self.request({
                method: 'POST',
                url: 'datas/metadata/model/' + modelId,
                headers: {'Accept': 'application/json'},
                data: dataIds
            });
        },
        computeStatistics : function (dataId) {
            return self.request({
                method: 'POST',
                url: 'datas/'+ dataId + '/stats',
                headers: {'Accept': 'application/json'}
            });
        },
        isCoverageAggregationDatasetCandidate : function (datasetId) {
            return self.request({
                method: 'GET',
                url:'datasets/coverage-aggregation/' + datasetId
            });
        },
        createCoverageAggregation: function (datasetId, dataName) {
            return self.request({
                method: 'PUT',
                url: 'datasets/coverage-aggregation/' + datasetId,
                params: {
                    dataName: dataName
                }
            });
        }
    };


    /**
     * Provider API
     */
    self.providers = {

        /**
         * Get all providers.
         *
         * @returns {Promise}
         */
        getProviders : function() {
            return self.request({
                method: 'GET',
                url: 'providers',
                headers: {'Accept': 'application/json'}
                });
        },
        /**
         * Get all data names in a provider.
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        getDataNamesList : function(providerId) {
            return self.request({
                method: 'GET',
                url: 'providers/' + providerId + '/datas/name',
                headers: {'Accept': 'application/json'}
                });
        },
        /**
         * Get all data in a provider.
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        getDataList : function(providerId) {
            return self.request({
                method: 'GET',
                url: 'providers/' + providerId + '/datas',
                headers: {'Accept': 'application/json'}
                });
        },
        /**
         * Reload a provider
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        reload : function(providerId) {
            return self.request({
                method: 'GET',
                url: 'providers/'+providerId+'/reload',
                headers: {'Accept': 'application/json'}
                });
        },
        /**
         * Remove a provider
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        delete : function(providerId) {
            return self.request({
                method: 'DELETE',
                url: 'providers/'+providerId,
                headers: {'Accept': 'application/json'}
                });
        },

        /**
         * Test a provider
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        test : function(providerId, configuration) {
            return self.request({
                method: 'POST',
                url: 'providers/'+providerId+'/test',
                headers: {'Accept': 'application/json'},
                data : configuration
                });
        },

        /**
         * Create a new provider
         *
         * @param {type} providerId
         * @returns {Promise}
         */
        create : function(providerId, createData, configuration) {
            return self.request({
                method: 'POST',
                url: 'providers/'+providerId+'?createdata=' + createData,
                headers: {'Accept': 'application/json'},
                data : configuration
                });
        },

        createPRJ : function(providerId, epsgCode) {
            return self.request({
                method: 'POST',
                url: 'providers/'+providerId+'/createprj',
                headers: {'Accept': 'application/json'},
                data : epsgCode
                });
        }
    };

    /**
     * Services API
     */
    self.services = {

        /**
         * Get a list of all services.
         *
         * @param {String} type : optional
         * @param {String} lang : optional
         * @returns {Promise}
         */
        getInstances : function(type,lang) {
            return self.request({
                method: 'GET',
                url: 'services/instances',
                params:{
                    type:type,
                    lang:lang
                    }
                });
        },

        /**
         * Get a service logs.
         *
         * @param {Logger} logger
         * @returns {Promise}
         */
        getLogs : function(type, id, offset, limit) {
            return self.request({
                method: 'GET',
                url: 'services/logs/'+type+'/'+id,
                params:{
                    o:offset,
                    l:limit
                    }
                });
        },

        getTypes: function () {
            return self.request({
                method: 'GET',
                url: 'services/types'
            });
        }
    };

    /**
     * Task/Process API
     */
    self.tasks = {

        /**
         * Returns a list of all process factories available in the current factories.
         *
         * @returns {Promise}
         */
        listProcessFactories : function() {
            return self.request({
                method: 'GET',
                url: 'task/listProcessFactories'
                });
        },

        /**
         * Returns a list of all process available in the current factories.
         *
         * @returns {Promise}
         */
        listProcesses : function() {
            return self.request({
                method: 'GET',
                url: 'task/listProcesses'
                });
        },

        /**
         * Returns a list all the saved tasks.
         *
         * @returns {Promise}
         */
        listTasks : function() {
            return self.request({
                method: 'GET',
                url: 'task/listTasks'
                });
        },

        /**
         * Cancel a task.
         *
         * @returns {Promise}
         */
        cancelTaskParam : function(id) {
            return self.request({
                method: 'DELETE',
                url: 'task/params/delete/' + id
                });
        },

        /**
         * Returns a list all the saved tasks.
         *
         * @returns {Promise}
         */
        listTaskParams : function() {
            return self.request({
                method: 'GET',
                url: 'task/params/list'
                });
        },

        /**
         * Create a new task with parameter.
         *
         * @returns {Promise}
         */
        createParamsTask : function(params) {
            return self.request({
                method: 'POST',
                url: 'task/params/create',
                headers: {'Accept': 'application/json'},
                data : params
            });
        },

        /**
         * Update a task with parameter.
         *
         * @returns {Promise}
         */
        updateParamsTask : function(params) {
            return self.request({
                method: 'POST',
                url: 'task/params/update',
                headers: {'Accept': 'application/json'},
                data : params
            });
        },

        /**
         * Return the input parameters for the task.
         *
         * @returns {Promise}
         */
        getParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/get/' + id
                });
        },

        /**
         * Remove the input parameters for the task.
         *
         * @returns {Promise}
         */
        deleteParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/delete/' + id
                });
        },

        /**
         * Duplicate a task and create a new one.
         *
         * @returns {Promise}
         */
        duplicateParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/duplicate/' + id
                });
        },

        /**
         * Execute a task.
         *
         * @returns {Promise}
         */
        executeParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/execute/' + id
                });
        },

        /**
         * Schedule a task.
         *
         * @returns {Promise}
         */
        startScheduleParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/schedule/start/' + id
                });
        },

        /**
         * Un-schedule a task.
         *
         * @returns {Promise}
         */
        stopScheduleParamsTask : function(id) {
            return self.request({
                method: 'GET',
                url: 'task/params/schedule/stop/' + id
                });
        },

        /**
         * Return the execution history for the task.
         *
         * @returns {Promise}
         */
        getTaskHistory : function(id, limit) {
            return self.request({
                method: 'GET',
                url: 'task/taskHistory/' + id + '/' + limit
                });
        },

        /**
         * Return descriptor for the specified process.
         *
         * @returns {Promise}
         */
        getProcessDescriptor : function(params) {
            return self.request({
                method: 'POST',
                url: 'task/process/descriptor',
                headers: {'Accept': 'application/json'},
                data : params
            });
        },

        /**
         * Count number of processes.
         *
         * @returns {Promise}
         */
        getCount : function() {
            return self.request({
                method: 'GET',
                url: 'task/countProcesses'
                });
        },

        /**
         * Get datasets references.
         *
         * @returns {Promise}
         */
        getDatasets : function() {
            return self.request({
                method: 'GET',
                url: 'task/list/datasets'
                });
        },

        /**
         * Get datas references.
         *
         * @returns {Promise}
         */
        getDatas : function(type) {
            var url = 'task/list/datas';
            if (type) {
                url = url + '?type=' + type;
            }
            return self.request({
                method: 'GET',
                url: url
                });
        },

        /**
         * Get mapContexts references.
         *
         * @returns {Promise}
         */
        getMapContexts : function() {
            return self.request({
                method: 'GET',
                url: 'task/list/mapcontexts'
                });
        },

        /**
         * Get services references.
         *
         * @returns {Promise}
         */
        getServices : function() {
            return self.request({
                method: 'GET',
                url: 'task/list/services'
                });
        },

        /**
         * Get styles references.
         *
         * @returns {Promise}
         */
        getStyles : function() {
            return self.request({
                method: 'GET',
                url: 'task/list/styles'
                });
        },

        /**
         * Get users references.
         *
         * @returns {Promise}
         */
        getUsers : function() {
            return self.request({
                method: 'GET',
                url: 'task/list/users'
                });
        }
    };

    /**
     * Portrayal API
     */
    self.portrayal = {

        /**
         * Get portrayal URL
         *
         * @returns {Promise}
         */
        getPortrayalURL : function() {
            return self.baseUrl+'portray';
        },

        /**
         * Get portrayal with style URL
         *
         * @returns {Promise}
         */
        getPortrayalStyleURL : function() {
            return self.baseUrl+'portray/style';
        }
    };

    /**
     * MAP API
     */
    self.map = {

        /**
         * Get single layer
         *
         * @returns {Promise}
         */
        getLayer : function(layerId) {
            return self.request({
                method: 'GET',
                url: 'MAP/layer/' + layerId,
                headers: {'Accept': 'application/json'}
            });
        },


        /**
         * Get all layers for a service
         *
         * @returns {Promise}
         */
        getLayers : function(type, id) {
            return self.request({
                method: 'GET',
                url: 'MAP/' + type + '/' + id + '/layersummary/all',
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Associate a layer with the specified map service.
         *
         * @returns {Promise}
         */
        addLayerNew : function(layer) {
            return self.request({
                method: 'PUT',
                url: 'MAP/layer/add',
                headers: {'Accept': 'application/json'},
                data: layer
            });
        },

        /**
         * Update a layer.
         *
         * @returns {Promise}
         */
        updateLayer : function(layer) {
            return self.request({
                method: 'POST',
                url: 'MAP/layer/' + layer.id,
                headers: {'Accept': 'application/json'},
                data: layer
            });
        },

        /**
         * Delete a layer.
         *
         * @returns {Promise}
         */
        deleteLayer : function(layerId) {
            return self.request({
                method: 'DELETE',
                url: 'MAP/layer/delete/' + layerId,
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Update layer style.
         *
         * @returns {Promise}
         */
        updateLayerStyle : function(type, id, params) {
            return self.request({
                method: 'POST',
                url: 'MAP/' + type + '/' + id + '/updatestyle',
                headers: {'Accept': 'application/json'},
                data: params
            });
        },

        /**
         * Update activateStats for layer style.
         *
         * @returns {Promise}
         */
        updateActivateStatsLayerStyle : function(params) {
            return self.request({
                method: 'POST',
                url: 'MAP/layer/style/activatestats',
                headers: {'Accept': 'application/json'},
                data: params
            });
        },

        /**
         * Remove layer style.
         *
         * @returns {Promise}
         */
        removeLayerStyle : function(type, id, params) {
            return self.request({
                method: 'POST',
                url: 'MAP/' + type + '/' + id + '/removestyle',
                headers: {'Accept': 'application/json'},
                data: params
            });
        },

        isAvailableAlias : function (serviceId, alias) {
            return self.request({
                method: 'GET',
                url: 'MAP/' + serviceId + '/alias?value=' + alias,
                headers: {'Accept': 'application/json'}
            });
        },
        
        isAvailableQName : function (serviceId, name, namespace) {
            return self.request({
                method: 'POST',
                url: 'MAP/' + serviceId + '/name',
                headers: {'Accept': 'application/json'},
                data: {
                    name:name,
                    namespace:namespace
                }
            });
        },

        /**
         * Extract WMTS layer info.
         *
         * @returns {Promise}
         */
        extractWMTSLayerInfo : function(type, id, layerName, crs, capabilities) {
            return self.request({
                method: 'POST',
                url: 'MAP/' + type + '/' + id + '/extractLayerInfo/' + layerName + '/' + crs,
                headers: {'Accept': 'application/json', 'Content-Type':'application/xml'},
                data: capabilities
            });
        }
    };

    /**
     * WPS API
     */
    self.wps = {

        /**
         * Get all processes
         *
         * @returns {Promise}
         */
        getAllProcess : function() {
            return self.request({
                method: 'GET',
                url: 'processes',
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Get processes associated with the specified wps service.
         *
         * @returns {Promise}
         */
        getProcess : function(serviceId) {
            return self.request({
                method: 'GET',
                url: 'processes/' + serviceId,
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Associate a process with the specified wps service.
         *
         * @returns {Promise}
         */
        addProcess : function(serviceId, registries) {
            return self.request({
                method: 'PUT',
                url: 'processes/' + serviceId,
                headers: {'Accept': 'application/json'},
                data: registries
            });
        },

        /**
         * Remove authority from the specified wps service.
         *
         * @returns {Promise}
         */
        removeAuthority : function(serviceId, code) {
            return self.request({
                method: 'DELETE',
                url: 'processes/' + serviceId + '/authority/' + code
            });
        },

        /**
         * Remove process from the specified wps service.
         *
         * @returns {Promise}
         */
        removeProcess : function(serviceId, code, pid) {
            return self.request({
                method: 'DELETE',
                url: 'processes/' + serviceId + '/process/' + code + '/' + pid
            });
        },

        /**
         * Remove process from the specified wps service.
         *
         * @returns {Promise}
         */
        getExternalProcessList : function(wpsUrl) {
            return self.request({
                method: 'GET',
                url: 'processes/external?wpsUrl=' + wpsUrl,
                headers: {'Accept': 'application/json'}
            });
        }
    };

    /**
     * CSW API
     */
    self.csw = {

        /**
         * Get records for specified CSW service
         *
         * @returns {Promise}
         */
        getRecords : function(serviceId, count, start) {
            return self.request({
                method: 'GET',
                url: 'CSW/' + serviceId + '/records/' + count + '/' + start,
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Get records count for specified CSW service
         *
         * @returns {Promise}
         */
        getRecordsCount : function(serviceId) {
            return self.request({
                method: 'GET',
                url: 'CSW/' + serviceId + '/records/count',
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Refresh the csw index
         *
         * @returns {Promise}
         */
        refresh : function(serviceId, asynchrone, forced) {
            return self.request({
                method: 'GET',
                url: 'CSW/' + serviceId + '/index/refresh?asynchrone=' + asynchrone + '&forced=' + forced,
                headers: {'Accept': 'application/json'}
            });
        },

        /**
         * Add the specified records in a csw service.
         *
         * @returns {Promise}
         */
        addRecords : function (serviceId, metadataIds) {
            return self.request({
                method: 'POST',
                url: 'CSW/' + serviceId + '/records',
                headers: {'Accept': 'application/json'},
                data: metadataIds
            });
        },

        /**
         * Remove the specified record from csw service.
         *
         * @returns {Promise}
         */
        deleteRecord : function(serviceId, recordId) {
            return self.request({
                method: 'DELETE',
                url: 'CSW/' + serviceId + '/record/' + recordId,
                headers: {'Accept': 'application/json'}
            });
        },

         /**
         * Remove the specified records from csw service.
         *
         * @returns {Promise}
         */
        deleteRecords : function(serviceId, recordIds) {
            return self.request({
                method: 'DELETE',
                url: 'CSW/' + serviceId + '/records',
                headers: {'Accept': 'application/json'},
                data: recordIds
            });
        },

        /**
         * Return the json metadata representation for editor.
         *
         * @returns {Promise}
         */
        getJsonMetadata : function(serviceId, recordId, type, prune) {
            return self.request({
                method: 'GET',
                url: 'CSW/' + serviceId + '/metadata/' + recordId + '/json?type=' + type + '&prune=' + prune,
                headers: {'Accept': 'application/json'}
            });
        },

        saveMetadata : function (serviceId, recordId, type, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'CSW/' + serviceId + '/metadata/save/' + recordId + '?type=' + type,
                headers: {'Accept': 'application/json'},
                data: metadataValues
            });
        }
    };

    /**
     * Metadata API
     */
    self.metadata = {
        convertMetadataJson : function (metadataId,prune,profile) {
            return self.request({
                method: 'GET',
                url: 'metadatas/' + metadataId + '/json/convert?prune='+prune+'&profile='+profile,
                headers: {'Accept': 'application/json'}
            });
        },
        download : function (directory,file) {
            return self.request({
                method: 'GET',
                url: 'metadatas/download/'+directory+'/'+file,
                headers: {'Accept': 'application/json'}
            });
        },
        askForValidation : function (metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/askForValidation',
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        changePublication : function (ispublished, metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/publication/'+ispublished,
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        getUsersList : function () {
            return self.request({
                method: 'GET',
                url: 'metadatas/users',
                headers: {'Accept': 'application/json'}
            });
        },
        delete : function (metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/delete',
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        deleteById : function (id) {
            return self.request({
                method: 'DELETE',
                url: 'metadatas/' + id
            });
        },
        changeOwner : function (ownerId,metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/owner/'+ownerId,
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        changeSharedPropertyMulti : function (shared,metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/shared/'+shared,
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        changeSharedProperty : function (shared,id) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + id + '/shared/' + shared,
                headers: {'Accept': 'application/json'}
            });
        },
        changeHiddenPropertyMulti : function (hidden,metadataList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/hidden/'+hidden,
                headers: {'Accept': 'application/json'},
                data: metadataList
            });
        },
        changeHiddenProperty : function (hidden,id) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + id + '/hidden/' + hidden,
                headers: {'Accept': 'application/json'}
            });
        },
        changeValidation : function (isvalid,validationList) {
            return self.request({
                method: 'POST',
                url: 'metadatas/validation/'+isvalid,
                headers: {'Accept': 'application/json'},
                data: validationList
            });
        },
        saveMetadata : function (metadataId, profile, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + metadataId + '/save?profile=' + profile,
                headers: {'Accept': 'application/json'},
                data: metadataValues
            });
        },
        duplicateMetadata : function (id,title,type) {
            var url = 'metadatas/' + id + '/duplicate';
            var separator = '?';

            if (title) {
                url = url + separator + 'title=' + title;
                separator = '&';
            }
            if (type) {
                url = url + separator + 'type=' + type;
            }
            return self.request({
                method: 'POST',
                url: url,
                headers: {'Accept': 'application/json'}
            });
        },
        uploadMetadata : function (formData, type, profile) {
            var url = 'metadatas/upload?type=' + type;
            if (profile) {
                url = url + '&profile=' + profile;
            }
            return self.request({
                method: 'POST',
                headers: {'Content-Type': undefined},
                url: url,
                transformRequest: angular.identity,
                cache: false,
                data: formData
            });
        },
        exportMetadata :  function(metadataIdArray) {
            return self.request({
                method: 'POST',
                url: 'metadatas/export',
                cache: false,
                data: metadataIdArray
            });
        },
        getIsoMetadataJson :function (metadataId, prune) {
            return self.request({
                method: 'GET',
                headers: {'Accept': 'application/json'},
                url: 'metadatas/'+metadataId+'/json?prune='+prune
            });
        },
        getNewMetadataJson : function (profile) {
            return self.request({
                method: 'GET',
                url: 'metadatas/json/new?profile=' + profile,
                headers: {'Accept': 'application/json'}
            });
        },
        create: function (profile, type, metadataValues) {
            return self.request({
                method: 'POST',
                url: 'metadatas?profile=' + profile + '&type=' + type,
                headers: {'Accept': 'application/json'},
                data: metadataValues
            });
        },
        linkAttachment: function (metadataId, attId) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + metadataId + '/attachment/' + attId
            });
        },
        unlinkAttachment: function (metadataId, attId) {
            return self.request({
                method: 'DELETE',
                url: 'metadatas/' + metadataId + '/attachment/' + attId
            });
        },
        linkData: function (metadataId, dataId) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + metadataId + '/data/' + dataId
            });
        },
        unlinkData: function (metadataId, dataId) {
            return self.request({
                method: 'DELETE',
                url: 'metadatas/' + metadataId + '/data/' + dataId
            });
        },
        linkDataset: function (metadataId, datasetId) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + metadataId + '/dataset/' + datasetId
            });
        },
        unlinkDataset: function (metadataId, datasetId) {
            return self.request({
                method: 'DELETE',
                url: 'metadatas/' + metadataId + '/dataset/' + datasetId
            });
        },
        linkMapcontext: function (metadataId, contextId) {
            return self.request({
                method: 'POST',
                url: 'metadatas/' + metadataId + '/mapcontext/' + contextId
            });
        },
        unlinkMapcontext: function (metadataId, contextId) {
            return self.request({
                method: 'DELETE',
                url: 'metadatas/' + metadataId + '/mapcontext/' + contextId
            });
        },
        uploadAttachment: function (formData) {
            return self.request({
                headers: {'Content-Type': undefined},
                method: 'POST',
                url: 'attachments/upload',
                transformRequest: angular.identity,
                cache: false,
                data: formData
            });
        },
        uploadImage: function (formData) {
            return self.request({
                headers: {'Content-Type': undefined},
                method: 'POST',
                url: 'internal/metadata/image/upload',
                transformRequest: angular.identity,
                cache: false,
                data: formData
            });
        },
        getMetadataCodeLists : function () {
            return self.request({
                method: 'GET',
                url: 'internal/metadata/codeLists'
            });
        },
        getProfiles : function (all, dataType, type) {
            var url = 'profiles?all=' + all;
            if (dataType) {
                url = url + '&dataType=' + dataType;
            }
            if (type) {
                url = url + '&type=' + type;
            }
            return self.request({
                method: 'GET',
                url: url
            });
        },
        get : function (id) {
            return self.request({
                method: 'GET',
                url: 'metadatas/' + id
            });
        },
        getStats : function (type) {
            return self.request({
                method: 'GET',
                url: 'metadatas/stats?type=' + type
            });
        },
        getFilteredStats : function (search,opts) {
            return self.request({
                method: 'POST',
                url: 'metadatas/stats?type=' + opts.type,
                data : search
            });
        },
        search : function (search,opts) {
            return self.request({
                method: 'POST',
                url: 'metadatas/search?type=' + opts.type,
                data : search
            });
        },
        searchIds : function (search,opts) {
            return self.request({
                method: 'POST',
                url: 'metadatas/search/id?type=' + opts.type,
                data : search
            });
        },
        getAssociatedData : function (params) {
            return self.request({
                method: 'POST',
                url: 'metadatas/data',
                headers: {'Accept': 'application/json'},
                data: params
            });
        }
    };

    /**
     * MapContext API
     */
    self.mapcontexts = {

        /**
         * Get all map contexts
         *
         * @returns {Promise}
         */
        getMapContexts : function() {
            return self.request({
                method: 'GET',
                url: 'mapcontexts/'
                });
        },

        /**
         * Get map context
         *
         * @param {Integer} id
         *
         * @returns {Promise}
         */
        getMapContext : function(id) {
            return self.request({
                method: 'GET',
                url: 'mapcontexts/' + id
                });
        },

        /**
         * Returns map contexts object.
         *
         * @param {Object} pagedSearch
         * @returns {Promise}
         */
        searchMapContexts : function(pagedSearch) {
            return self.request({
                method: 'POST',
                url: 'mapcontexts/search',
                headers: {'Accept': 'application/json'},
                data : pagedSearch
                });
        },

        /**
         * Create a mapcontext
         *
         * @param {Context} context
         */
        createMapContext : function(context) {
            return self.request({
                method: 'POST',
                url: 'mapcontexts',
                headers: {'Accept': 'application/json'},
                data : context
            });
        },

        /**
         * Update a mapcontext
         *
         * @param {Integer} mapcontextId
         * @param {Context} context
         */
        updateMapContext : function(context) {
            return self.request({
                method: 'PUT',
                url: 'mapcontexts/'+context.id,
                headers: {'Accept': 'application/json'},
                data : context
            });
        },

        /**
         * Delete a mapcontext
         *
         * @param {Integer} mapcontextId
         */
        deleteMapContext : function(mapcontextId) {
            return self.request({
                method: 'DELETE',
                url: 'mapcontexts/'+mapcontextId
            });
        },

        /**
         * Get a map layer
         *
         * @returns {Promise}
         */
        getMapLayer : function(mapcontextId) {
            return self.request({
                method: 'GET',
                url: 'mapcontexts/' + mapcontextId + '/layers'
                });
        },

        /**
         * Get all map layers
         *
         * @returns {Promise}
         */
        getMapLayers : function() {
            return self.request({
                method: 'GET',
                url: 'mapcontexts/layers'
                });
        },

        /**
         * Get all map layers available on given external service url.
         *
         * @param {String} url
         * @param {String} version
         * @returns {Promise}
         */
        getExternalMapLayers : function(url,version) {
            return self.request({
                method: 'POST',
                url: 'internal/mapcontexts/external/capabilities/layers/'+version,
                headers: {'Accept': 'application/json',
                    'Content-Type':'text/plain;charset=UTF-8'},
                data: url
                });
        },

        /**
         * Get map layer extent.
         *
         * @param {Objects} layers
         * @returns {Promise}
         */
        getMapLayersExtent : function(layers) {
            return self.request({
                method: 'POST',
                url: 'internal/mapcontexts/extent/layers',
                headers: {'Accept': 'application/json'},
                data: layers
                });
        },

        pyramidMapContext : function (contextId, crs, layerName) {
            return self.request({
            method: 'GET',
                    url: 'mapcontexts/' + contextId + '/pyramid?crs=' + crs + '&layerName=' + layerName
            });
        },

        getMapContextData : function (contextId) {
            return self.request({
            method: 'GET',
                    url: 'mapcontexts/' + contextId + '/data'
            });
        }
    };
}

/**
 * Factory to create new instances of examind clients.
 *
 * @param {type} $http
 * @returns {factory}
 */
var factory = function($http) {
    var self = this;

    self.create = function(url) {
        return new Examind($http,url);
    };
};

module.service('ExamindFactory', factory);






