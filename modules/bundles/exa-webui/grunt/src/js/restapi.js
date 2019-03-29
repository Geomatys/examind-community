/*
 * Constellation - An open source and standard compliant SDI
 *
 *     http://www.constellation-sdi.org
 *
 *     Copyright 2014 Geomatys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @require angular.js
 * @require angular-resource.js
 * @require app-services.js
 */
angular.module('cstl-restapi', ['ngResource', 'webui-config', 'cstl-services'])
    
    .factory('Metrics', function($resource) {
        return $resource('@cstl/metrics/metrics', {}, {
            'get': { method: 'GET'}
        });
    })

    .factory('textService', function($http, Growl) {
        return {
            capa : function(type, id, version){
                return $http.get('@cstl/WS/'+type+'/'+id+'?REQUEST=GetCapabilities&SERVICE='+type.toUpperCase()+'&VERSION='+version);
            }
        };
    });