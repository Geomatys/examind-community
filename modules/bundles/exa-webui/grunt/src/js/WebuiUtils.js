
/**
 * @require angular.js
 * @require angular-cookies.js
 */
angular.module('webui-utils',[])

    // -------------------------------------------------------------------------
    //  Growl Service
    // -------------------------------------------------------------------------

    .factory('Growl', function() {
        /**
         * Displays a notification with the specified title and text.
         *
         * @param type  - {string} the notification type (info|error|success|warning)
         * @param title - {string} the notification title
         * @param msg   - {string} the notification message
         */
        return function(type, title, msg) {
            if (type === 'info') {
                $.growl({title: title, message: msg});
            } else if (type === 'error') {
                $.growl.error({title: title, message: msg});
            } else if (type === 'success') {
                $.growl.notice({title: title, message: msg});
            } else if (type === 'warning') {
                $.growl.warning({title: title, message: msg});
            }
        };
    })

    // -------------------------------------------------------------------------
    //  Paging service
    // -------------------------------------------------------------------------

    .factory('Paging', function($timeout) {
        /**
         * An helper service to manage paging features.
         *
         * Provides methods and variables to manage :
         *  - text filter
         *  - column sorting
         *  - request status
         *  - paging
         *
         * Query structure : { page: 1, size: 20, text: 'mytext', sort: { order: 'ASC', field: 'myfield' } }
         *
         * @constructor
         * @param {Function} searchMethod The search method to call.
         * @param {Object} [initialQuery] The initial query for search (default is { page: 1, size: 20 }).
         * @param {Object} [initialPage] The initial data page (default is null).
         */
        function Paging(searchMethod, initialQuery, initialPage) {

            var self = this;

            var timeout = null;


            // States on the search request status (-1 pending, 0 not sent, 1 success, 2 error).
            self.searchStatus = 0;

            // Search request criteria.
            self.query = initialQuery || { page: 1, size: 20 };

            // Search result page.
            self.page = initialPage;


            // Sends the search query and gets the results.
            self.search = function() {
                    self.searchStatus = -1;
                    return searchMethod(self.query).then(
                        function (response) { //success callback
                            self.searchStatus = 1;
                            self.page = response.data;
                        },
                        function () { //error callback
                            self.searchStatus = 2;
                            self.page = null;
                        });
                };

            // Avoids too much HTTP requests on 'keyup' event for text filter.
            self.searchDebounce = function(ms) {
                self.searchStatus = -1;
                self.query.page = 1;
                $timeout.cancel(timeout);
                timeout = $timeout(self.search, ms || 300);
            };

            // Modify the sort order for result items. Like SQL order we use the ascending order
            // by default.
            self.sortBy = function(field) {
                if (self.isSortedBy(field)) {
                    switch (self.query.sort.order) {
                        case 'ASC':
                            self.query.sort.order = 'DESC';
                            break;
                        default:
                            self.query.sort.order = 'ASC';
                    }
                } else {
                    self.query.sort = { field: field, order: 'ASC' };
                }
                self.search();
            };

            // Checks if the result items are sorted on the specified field.
            self.isSortedBy = function(field) {
                return self.query.sort && (self.query.sort.field === field);
            };

            // Returns the icon class to apply according the current sort order.
            self.getOrderIcon = function(field) {
                if (self.isSortedBy(field)) {
                    switch (self.query.sort.order) {
                        case 'ASC':
                            return 'fa-caret-up';
                        case 'DESC':
                            return 'fa-caret-down';
                    }
                }
                return null;
            };

            // Changes the page index for results.
            self.setPage = function(page) {
                self.query.page = page;
                self.search();
            };
        }

        return Paging;
    })


    // -------------------------------------------------------------------------
    //  SelectionApi service to have multi selection in paging dashboard (metadata)
    // -------------------------------------------------------------------------

    .factory('SelectionApi', function() {
        var SelectionApi = {};

        var _list = SelectionApi._list = [];

        SelectionApi.add = function(item) {
            if (!SelectionApi.isExist(item)) {
                _list.push(item);
            }
        };

        SelectionApi.remove = function(item) {
            var i = getIndexForItem(item);
            if (i !== -1) {
                _list.splice(i, 1);
            }
        };

        SelectionApi.update = function(item) {
            var i = getIndexForItem(item);
            if (i !== -1) {
                _list[i] = item;
            }
        };

        SelectionApi.toggle = function(item) {
            var i = getIndexForItem(item);
            if (i !== -1) {
                _list.splice(i, 1);
            }
            else {
                _list.push(item);
            }
        };

        SelectionApi.isExist = function(item) {
            return _list.length && getIndexForItem(item) !== -1;
        };

        SelectionApi.getLength = function() {
            return _list.length;
        };

        SelectionApi.getList = function() {
            return _list;
        };

        SelectionApi.clear = function() {
            _list = [];
        };

        function getIndexForItem(item) {
            for(var i=0;i<_list.length;i++) {
                if(_list[i].id === item.id){
                    return i;
                }
            }
            return -1;
        }

        return SelectionApi;
    })

    // -------------------------------------------------------------------------
    //  Old Dashboard service rewrited to supports non capitalized properties
    // ie : style.Name is now style.name
    // @TODO to be removed when paging will be done for all dashboard, you must use Paging service
    // -------------------------------------------------------------------------

    .factory('OldDashboard', function($filter) {
        return function(scope, fullList, filterOnType) {
            scope.wrap = scope.wrap || {};
            scope.service = scope.service || null;
            scope.wrap.fullList = fullList || [];
            scope.wrap.dataList = scope.wrap.dataList || [];
            scope.wrap.matchExactly = scope.wrap.matchExactly || false;
            scope.wrap.filtertext = scope.wrap.filtertext || "";
            scope.wrap.filtertype = scope.wrap.filtertype || undefined;
            scope.wrap.ordertype = scope.wrap.ordertype || ((scope.service && scope.service.type && (scope.service.type.toLowerCase()==='sos' || scope.service.type.toLowerCase()==='sts')) ? "id" : (scope.service && scope.service.type && scope.service.type.toLowerCase==='csw') ? "title" : "name");
            scope.wrap.orderreverse = scope.wrap.orderreverse || false;
            scope.wrap.countdata = scope.wrap.countdata || 0;
            scope.wrap.nbbypage = scope.wrap.nbbypage || 10;
            scope.wrap.currentpage = scope.wrap.currentpage || 1;
            scope.selected = scope.selected || null;
            scope.selectedDS = scope.selectedDS || null;
            scope.exclude = scope.exclude || [];

            // Dashboard methods
            scope.displayPage = function(page) {
                var array;
                if (filterOnType) {
                    var match = false;
                    if(scope.wrap.filtertext){
                        match=scope.wrap.matchExactly;
                    }
                    array = $filter('filter')(scope.wrap.fullList, {'type':scope.wrap.filtertype, '$': scope.wrap.filtertext},match);
                } else {
                    array = $filter('filter')(scope.wrap.fullList, {'$': scope.wrap.filtertext});
                }
                array = $filter('orderBy')(array, scope.wrap.ordertype, scope.wrap.orderreverse);

                var list = [];
                for (var i = 0; i < array.length; i++) {
                    var found = false;
                    for (var j = 0; j < scope.exclude.length; j++) {
                        var arrName = angular.isDefined(array[i].name) ? array[i].name : array[i].Name; // @TODO CSTL-1926
                        var exclName = angular.isDefined(scope.exclude[j].name) ? scope.exclude[j].name : scope.exclude[j].Name; // @TODO CSTL-1926
                        var exclId = angular.isDefined(scope.exclude[j].id) ? scope.exclude[j].id : scope.exclude[j].Id; // @TODO CSTL-1926
                        if (scope.service && (scope.service.type.toLowerCase() === 'sos' || scope.service.type.toLowerCase() === 'sts')) {
                            if (exclId === arrName) {
                                found = true;
                                break;
                            }
                        } else {
                            if (exclName === arrName) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        list.push(array[i]);
                    }
                }

                var start = (page - 1) * scope.wrap.nbbypage;

                scope.wrap.currentpage = page;
                scope.wrap.countdata = list.length;
                scope.wrap.dataList = list.splice(start, scope.wrap.nbbypage);
            };

            scope.select = scope.select || function(item) {
                    if (scope.selected === item) {
                        scope.selected = null;
                    } else {
                        scope.selected = item;
                    }
                };

            scope.selectDS = function(item) {
                if (item && scope.selectedDS && scope.selectedDS.id === item.id) {
                    scope.selectedDS = null;
                } else {
                    scope.selectedDS = item;
                }
            };

            scope.$watch('wrap.nbbypage+wrap.filtertext+wrap.filtertype+wrap.fullList', function() {
                scope.displayPage(1);
            },true);

            scope.$watch('wrap.ordertype+wrap.orderreverse', function() {
                scope.displayPage(scope.wrap.currentpage);
            },true);
        };
    });