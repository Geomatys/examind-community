/*
 * Constellation - An open source and standard compliant SDI
 *
 *     http://www.constellation-sdi.org
 *
 *     Copyright 2014 Geomatys
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @require angular.js
 * @require angular-cookies.js
 */
angular.module('webui-config',[])

    // -------------------------------------------------------------------------
    //  'ngCookies' Substitution (using jQuery Cookie)
    // -------------------------------------------------------------------------

    .factory('$cookieStore', function() {
        return {
            /**
             * @name $cookieStore#get
             *
             * @description
             * Returns the value of given cookie key
             *
             * @param {string} key Id to use for lookup.
             * @returns {string} Cookie value.
             */
            get: function(key) {
                return $.cookie(key);
            },

            /**
             * @name $cookieStore#put
             *
             * @description
             * Sets a value for given cookie key
             *
             * @param {string} key Id for the `value`.
             * @param {string} value Value to be stored.
             * @param {Object} attributes Cookie attributes.
             */
            put: function(key, value, attributes) {
                $.cookie(key, value, attributes);
            },

            /**
             * @name $cookieStore#remove
             *
             * @description
             * Remove given cookie
             *
             * @param {string} key Id of the key-value pair to delete.
             * @param {Object} attributes Cookie attributes.
             */
            remove: function(key, attributes) {
                $.removeCookie(key, attributes);
            }
        };
    })

    // -------------------------------------------------------------------------
    //  Constellation Configuration Properties
    // -------------------------------------------------------------------------

    .constant('CstlConfig', {
        // Injection expressions.
        'inject.expr.ctrl.url':  '@cstl/',

        // Cookies.
        'cookie.cstl.url':   'cstlUrl',
        'cookie.auth.token': 'access_token',
        'cookie.auth.refresh': 'refresh_token',

        //cstl version.
        'cstl.version': (new Date()).getFullYear(),

        // Navigation additional buttons
        // cstl declare its own links directly in the header
        // and some links can be replaced by subproject,
        // this array store commons links that can be replaced by others and it is overrided by subprojects.
        'cstl.navigation' : [
            {
                'id': 'metadataLink',
                'href': '#/metadata',
                'cssClass': 'metadata',
                'iconClass': 'glyphicon glyphicon-file',
                'labelKey': 'global.menu.metadata',
                'defaultLabel': 'Metadata'
            },
            {
                'id': 'thesaurusLink',
                'href': '#/thesaurus',
                'cssClass': 'thesaurus',
                'iconClass': 'fa fa-share-alt',
                'labelKey': 'global.menu.thesaurus',
                'defaultLabel': 'Thesaurus'
            }
        ],

        // Defines if the data overview must use the "conform pyramid" associated
        // to the selected data (if exists). If false, always use the "raw" data.
        'data.overview.use_pyramid': true,

        // Defines if the datasets which contain a single data must be visible.
        // If false, the single data will be displayed instead of the dataset.
        'dataset.listing.show_singleton': false
    })
        
    // -------------------------------------------------------------------------
    //  Build service
    // -------------------------------------------------------------------------
    
    .factory('BuildService', function($resource) {
        return $resource('app/build').get();
    })

    // -------------------------------------------------------------------------
    //  Application configuration service with cache
    // -------------------------------------------------------------------------
    .service('AppConfigService', function($http) {
        var self = this;
        self.config = null;

        self.getConfig = function(callback) {
            if (self.config === null) {
                //synchronous call
                $http.get('app/conf').success(function(data) {
                    $http.get('config/config.json').then(function (config) {
                        self.config = Object.assign({}, data, config.data);
                        callback(self.config);
                    }, function (err) {
                        console.error(err);
                    });
                });
            } else {
                callback(self.config);
            }
        };

        self.getConfigProperty = function(key, callback, fallback) {
            self.getConfig(function (config) {
                var value = config[key] || fallback;
                callback(value);
            });
        };
    });
