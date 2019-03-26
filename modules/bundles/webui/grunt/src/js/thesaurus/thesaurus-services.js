angular.module('cstl-thesaurus-services', [])

    // -------------------------------------------------------------------------
    //  Values & constants
    // -------------------------------------------------------------------------

    .constant('thesaurusQuery', {
        page: 1,
        size: 10,
        sort: { field: 'name', order: 'DESC' } })

    .constant('thesaurusLanguages', [
        { label: 'Français', code: 'fr' },
        { label: 'English', code: 'en' }
    ])

    // -------------------------------------------------------------------------
    //  DAOs
    // -------------------------------------------------------------------------

    .factory('Thesaurus', function($resource) {
        return $resource('@cstl/API/THW/:uri', null, {
            search: { method: 'POST', url: '@cstl/API/THW/search' },
            create: { method: 'PUT' }
        });
    })

    .factory('ThesaurusConcepts', function($resource) {
        var resourcePath = '@cstl/API/THW/:thesaurusUri/concept/:conceptUri';
        return $resource(resourcePath, null, {
            getTopMost: { method: 'GET', isArray: true },
            getNarrowers: { method: 'GET', isArray: true, url: '@cstl/API/THW/:thesaurusUri/concept/:conceptUri/narrowers' },
            create: { method: 'POST' },
            save: { method: 'PUT' },
            search: { method: 'GET', isArray: true, url: '@cstl/API/THW/:thesaurusUri/:lang/concept/search/:term' }
        });
    });