angular.module('cstl-thesaurus-controllers', [
    'cstl-thesaurus-services',
    'examind-instance'
])

    .controller('ThesaurusDashboardController', function($modal, $translate, Growl, DashboardHelper, Thesaurus, thesaurusQuery) {

        var self = this;

        DashboardHelper.call(self, Thesaurus.search, angular.copy(thesaurusQuery));

        self.search();

        self.selection = null;

        self.resetCriteria = function() {
            self.query = angular.copy(thesaurusQuery);
            self.search();
        };

        self.select = function(th) {
            self.selection = { uri: th.uri }; // highlight the clicked item without waiting the HTTP response
            Thesaurus.get(self.selection, onSelectSuccess, onSelectError);
        };

        self.create = function() {
            $modal.open({
                templateUrl: 'views/thesaurus/modal-create.html',
                controller: 'ThesaurusCreationController as mc'
            }).result.then(self.search);
        };

        self.import = function() {
            $modal.open({
                templateUrl: 'views/thesaurus/modal-import.html',
                controller: 'ThesaurusImportController as mc',
                resolve: {
                    'thesaurus': function() {
                        return self.selection;
                    },
                    'topConcepts': function(ThesaurusConcepts) {
                        return ThesaurusConcepts.getTopMost({ thesaurusUri: self.selection.uri }).$promise;
                    }
                }
            });
        };

        self.editSelected = function() {
            $modal.open({
                templateUrl: 'views/thesaurus/modal-concepts.html',
                controller: 'ThesaurusConceptsController as mc',
                resolve: {
                    'thesaurus': function() {
                        return self.selection;
                    },
                    'topConcepts': function(ThesaurusConcepts) {
                        return ThesaurusConcepts.getTopMost({ thesaurusUri: self.selection.uri }).$promise;
                    }
                }
            });
        };

        self.deleteSelected = function() {
            $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function() {
                        return 'thesaurus.msg.confirm_removal';
                    }
                }
            }).result.then(function(confirmed){
                if (confirmed) {
                    Thesaurus.delete({ uri: self.selection.uri }, onDeleteSuccess, onDeleteError);
                }
            });
        };


        function onSelectSuccess(data) {
            self.selection = data;
        }

        function onSelectError() {
            self.selection = null;
        }

        function onDeleteSuccess() {
            self.search();
            $translate(['label.success', 'thesaurus.msg.removal_success']).then(function (translations) {
                Growl('success', translations['label.success'],  translations['thesaurus.msg.removal_success']);
            });
        }

        function onDeleteError() {
            $translate(['label.error', 'thesaurus.msg.removal_error']).then(function (translations) {
                Growl('error', translations['label.error'],  translations['thesaurus.msg.removal_error']);
            });
        }
    })

    .controller('ThesaurusCreationController', function($modalInstance, $translate, Growl, Thesaurus, thesaurusLanguages) {

        var self = this;

        self.thesaurusLanguages = thesaurusLanguages;

        self.thesaurus = {
            defaultLang: thesaurusLanguages[0].code,
            langs: thesaurusLanguages.map(function(lang) { return lang.code; })
        };

        self.submit = function() {
            Thesaurus.create(self.thesaurus, onCreationSuccess, onCreationError);
        };


        function onCreationSuccess() {
            $modalInstance.close();
            $translate(['label.success', 'thesaurus.msg.creation_success']).then(function (translations) {
                Growl('success', translations['label.success'],  translations['thesaurus.msg.creation_success']);
            });
        }

        function onCreationError() {
            $translate(['label.error', 'thesaurus.msg.creation_error']).then(function (translations) {
                Growl('error', translations['label.error'],  translations['thesaurus.msg.creation_error']);
            });
        }
    })

    .controller('ThesaurusImportController', function($scope, $rootScope, $modalInstance, $translate, cfpLoadingBar, Growl, thesaurus, Examind) {

        var self = this;

        self.isValid = false;

        self.touched = false;

        self.uploading = false;

        self.checkFile = function(fileName) {
            var pointIndex = fileName.lastIndexOf(".");
            var extension = fileName.substring(pointIndex + 1, fileName.length);
            $scope.$apply(function() {
                self.touched = true;
                self.isValid = (extension === 'xml');
            });
        };

        self.submit = function() {
            var formData = new FormData(document.getElementById('thForm'));

            self.uploading = true;
            $.ajax({
                url: window.localStorage.getItem('cstlUrl') + "API/THW/"+ thesaurus.uri + "/import",
                type: 'POST',
                data: formData,
                cache: false,
                contentType: false,
                processData: false,
                beforeSend: function(){
                    cfpLoadingBar.start();
                    cfpLoadingBar.inc();
                },
                success: onImportSuccess,
                error: onImportError
            });
        };


        function onImportSuccess() {
            $scope.$apply(function() {
                self.uploading = false;
                cfpLoadingBar.complete();
                $translate(['label.success', 'thesaurus.msg.import_success']).then(function (translations) {
                    Growl('success', translations['label.success'],  translations['thesaurus.msg.import_success']);
                });
                $modalInstance.close();
            });
        }

        function onImportError() {
            $scope.$apply(function() {
                self.uploading = false;
                cfpLoadingBar.complete();
                $translate(['label.error', 'thesaurus.msg.import_error']).then(function (translations) {
                    Growl('error', translations['label.error'],  translations['thesaurus.msg.import_error']);
                });
            });
        }
    })

    .controller('ThesaurusConceptsController', function($timeout, $modal, $translate, Growl, ThesaurusConcepts, thesaurus, topConcepts) {

        // Global
        // ----------

        var timeout = null; // debounce search timeout

        var self = this;

        self.viewName = 'navigation';

        self.langs = thesaurus.langs;

        self.lang = thesaurus.defaultLang;

        self.getNarrowers = function(node) {
            return ThesaurusConcepts.getNarrowers({ thesaurusUri: thesaurus.uri, conceptUri: node.uri });
        };

        function search(treeContext) {
            if (!treeContext.search) {
                clearSearch(treeContext);
            } else {
                ThesaurusConcepts.search({ thesaurusUri: thesaurus.uri, term: treeContext.search, lang: self.lang }, function(data) {
                    treeContext.tops = data;
                });
            }
        }

        function searchDebounce(treeContext) {
            $timeout.cancel(timeout);
            timeout = $timeout(function() { search(treeContext); }, 300);
        }

        function clearSearch(treeContext) {
            treeContext.search = null;
            ThesaurusConcepts.getTopMost({ thesaurusUri: thesaurus.uri }, function(data) {
                topConcepts = treeContext.tops = data;
            });
        }

        // Navigation
        // ----------

        var nav = self.nav = {
            tree: { search: null, tops: topConcepts },
            concept: null
        };

        nav.select = function(node) {
            ThesaurusConcepts.get({ thesaurusUri: thesaurus.uri, conceptUri: node.uri }, function(data) {
                nav.concept = data;
            });
        };

        nav['delete'] = function() {
            $modal.open({
                templateUrl: 'views/modal-confirm.html',
                controller: 'ModalConfirmController',
                resolve: {
                    'keyMsg': function() {
                        return 'thesaurus.msg.confirm_concept_removal';
                    }
                }
            }).result.then(function(confirmed){
                if (confirmed) {
                    ThesaurusConcepts.delete({ thesaurusUri: thesaurus.uri, conceptUri: nav.concept.uri }, onConceptDeleteSuccess, onConceptDeleteError);
                }
            });
        };

        nav.search = function() {
            nav.concept = null;
            searchDebounce(nav.tree);
        };

        nav.clearSearch = function() {
            nav.concept = null;
            clearSearch(nav.tree);
        };

        function onConceptDeleteSuccess() {
            $translate(['label.success', 'thesaurus.msg.concept_delete_success']).then(function (translations) {
                Growl('success', translations['label.success'],  translations['thesaurus.msg.concept_delete_success']);
            });
            nav.concept = null;
            search(nav.tree);
        }

        function onConceptDeleteError() {
            $translate(['label.error', 'thesaurus.msg.concept_delete_error']).then(function (translations) {
                Growl('error', translations['label.error'],  translations['thesaurus.msg.concept_delete_error']);
            });
        }

        // Edition
        // ----------

        var edit = self.edit = {};

        edit.setup = function(concept, defaultBroader) {
            self.viewName = 'edition';

            if (concept) {
                concept = edit.concept = angular.copy(concept);
            } else {
                concept = edit.concept = {
                    topConcept: !defaultBroader,
                    prefLabel: {},
                    definition: {},
                    altLabels: {},
                    narrowers: [],
                    broaders: defaultBroader ? [defaultBroader] : [],
                    related: []
                };
            }

            // Create placeholders for all languages supported by the thesaurus.
            angular.forEach(thesaurus.langs, function(lang) {
                if (angular.isUndefined(concept.prefLabel[lang])) {
                    concept.prefLabel[lang] = null;
                }
                if (angular.isUndefined(concept.definition[lang])) {
                    concept.definition[lang] = null;
                }
                if (angular.isUndefined(concept.altLabels[lang])) {
                    concept.altLabels[lang] = [];
                }
            });
        };

        edit.cancel = function() {
            self.viewName = 'navigation';
            edit.isNew = false;
            edit.concept = null;
        };

        edit.validate = function() {
            if (angular.isDefined(edit.concept.uri)) {
                ThesaurusConcepts.save({ thesaurusUri: thesaurus.uri }, edit.concept, onConceptEditSuccess, onConceptEditError);
            } else {
                ThesaurusConcepts.create({ thesaurusUri: thesaurus.uri }, edit.concept, onConceptEditSuccess, onConceptEditError);
            }
        };

        function onConceptEditSuccess() {
            $translate(['label.success', 'thesaurus.msg.concept_save_success']).then(function (translations) {
                Growl('success', translations['label.success'],  translations['thesaurus.msg.concept_save_success']);
            });
            clearSearch(self.nav.tree);
            edit.cancel();
        }

        function onConceptEditError() {
            $translate(['label.error', 'thesaurus.msg.concept_save_error']).then(function (translations) {
                Growl('error', translations['label.error'],  translations['thesaurus.msg.concept_save_error']);
            });
        }

        // Selection
        // ----------

        var select = self.select = {};

        select.setup = function(target) {
            self.viewName = 'selection';
            select.tree = { search: null, tops: topConcepts };
            select.node = null;

            // Implement "contextual" methods.
            select.validate = function() {
                target.push(select.node);
                select.cancel();
            };
            select.isPresent = function() {
                var i = target.length;
                if (select.node) {
                    while (i--) {
                        if (select.node.uri === target[i].uri) {
                            return true;
                        }
                    }
                }
                return false;
            };
        };

        select.cancel = function() {
            self.viewName = 'edition';
            select.tree = { search: null, tops: topConcepts };
            select.node = null;
            select.validate = angular.noop;
            select.isPresent = angular.noop;
        };

        select.search = function() {
            select.node = null;
            searchDebounce(select.tree);
        };

        select.clearSearch = function() {
            select.node = null;
            clearSearch(select.tree);
        };

        select.validate = angular.noop;

        select.isPresent = angular.noop;
    });