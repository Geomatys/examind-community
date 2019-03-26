angular.module('examind.paged.search.service', [
    'examind-factory'
])
    .constant('defaultQuery', {
    page: 1,
    size: 10,
    sort: {field: 'creation_date', order: 'DESC'},
    filters: []
})
    .factory('PagedSearchService', PagedSearchServiceFactory);


function PagedSearchServiceFactory(defaultQuery) {

    function PagedSearchService() {
        this.query = angular.copy(defaultQuery);
    }

    PagedSearchService.prototype.search = angular.noop;

    PagedSearchService.prototype.filterBy = function (field, value) {
        this.removeFilter(field);
        this.query.filters.push({"field": field, "value": value});
        this.search();
    };

    PagedSearchService.prototype.removeFilter = function (field) {
        if (this.query.filters) {
            for (var i = 0; i < this.query.filters.length; i++) {
                if (angular.equals(this.query.filters[i].field, field)) {
                    this.query.filters.splice(i, 1);
                    break;
                }
            }
        }
    };

    PagedSearchService.prototype.getFilter = function (field) {
        for (var i = 0; i < this.query.filters.length; i++) {
            if (angular.equals(this.query.filters[i].field, field)) {
                return this.query.filters[i].value;
            }
        }
        return '';
    };

    PagedSearchService.prototype.isSortedBy = function (field) {
        return this.query.sort && angular.equals(this.query.sort.field, field);
    };

    PagedSearchService.prototype.sortBy = function (field) {
        if (this.isSortedBy(field)) {
            switch (this.query.sort.order) {
                case 'ASC':
                    this.query.sort.order = 'DESC';
                    break;
                default:
                    this.query.sort.order = 'ASC';
            }
        } else {
            this.query.sort = {field: field, order: 'ASC'};
        }
        this.search();
    };

    PagedSearchService.prototype.getSortOrderIcon = function (field) {
        if (this.isSortedBy(field)) {
            switch (this.query.sort.order) {
                case 'ASC':
                    return 'fa-caret-up';
                case 'DESC':
                    return 'fa-caret-down';
            }
        }
        return null;
    };

    PagedSearchService.prototype.setPage = function (page) {
        this.query.page = page;
        this.search();
    };

    PagedSearchService.prototype.reset = function () {
        this.query = angular.copy(defaultQuery);
        this.search();
    };

    return PagedSearchService;
}