angular.module('cstl-data-dashboard')
    .constant('defaultDatasetQuery', {
        page: 1,
        size: 10,
        sort: { field: 'creation_date', order: 'DESC' },
        filters: []
    });