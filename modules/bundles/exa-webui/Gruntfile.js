module.exports = function(grunt) {
    'use strict';

    grunt.initConfig({

        lib_dir: './grunt/lib',
        src_dir: './grunt/src',
        temp_dir: './grunt/temp',
        target_dir: grunt.option('target_dir') || './target/exa-webui',

        // Clean output and temporary directories.
        clean: {
            options: {
                force: true
            },
            target: '<%= target_dir %>',
            temp: '<%= temp_dir %>'
        },

        // Validate JavaScript code style.
        jshint: {
            app: {
                options: {
                    'reporterOutput':"",
                    'curly': true,
                    'eqnull': true,
                    'eqeqeq': true,
                    'nonew':true,
                    'noarg':true,
                    'forin':true,
                    'noempty':true,
                    'undef':true,
                    'bitwise':true,
                    'latedef':false,
                    'immed':true,
                    'freeze':true,
                    'devel':true,
                    'browser':true,
                    'jquery':true,
                    'globals':{
                        "angular":false,
                        "Growl":false,
                        "$translate":false,
                        "OpenLayers":false,
                        "ol":false,
                        "olext":false,
                        "d3":false,
                        "Stomp":false,
                        "SockJS":false,
                        "Dygraph":false,
                        "c3":false,
                        "hljs":false,
                        "dataNotReady":true,
                        "cstlAdminApp":true,
                        "DataViewer":true,
                        "WmtsViewer":true,
                        "DataDashboardViewer":true,
                        "LayerDashboardViewer":true,
                        "WmtsLayerDashboardViewer":true,
                        "MapContextDashboardViewer":true,
                        "StyleDashboardViewer":true,
                        "MetadataEditorViewer":true,
                        "Netcdf":true
                    }
                },
                src: ['<%= src_dir %>/js/**/*.js']
            }
        },

        // Copy assets files.
        copy: {
            app: {
                files: [
                    {
                        src: ['*.html'],
                        cwd: '<%= src_dir %>/',
                        dest: '<%= target_dir %>/',
                        expand: true
                    },
                    {
                        src: ['img/**'],
                        cwd: '<%= src_dir %>/',
                        dest: '<%= target_dir %>/',
                        expand: true
                    },
                    {
                        src: ['views/**'],
                        cwd: '<%= src_dir %>/',
                        dest: '<%= target_dir %>/',
                        expand: true
                    },
                    {
                        src: ['i18n/**'],
                        cwd: '<%= src_dir %>/',
                        dest: '<%= target_dir %>/',
                        expand: true
                    },
                    {
                        src: ['config/**'],
                        cwd: '<%= src_dir %>/',
                        dest: '<%= target_dir %>/',
                        expand: true
                    }
                ]
            },
            lib: {
                files: [
                    {
                        src: [
                            '<%= lib_dir %>/**/css/*',
                            'node_modules/angularjs-slider/dist/rzslider.min.css'
                        ],
                        dest: '<%= target_dir %>/css',
                        expand: true,
                        flatten: true
                    },
                    {
                        src: ['<%= lib_dir %>/**/fonts/*'],
                        dest: '<%= target_dir %>/fonts',
                        expand: true,
                        flatten: true
                    },
                    {
                        src: ['<%= lib_dir %>/**/img/*'],
                        dest: '<%= target_dir %>/img',
                        expand: true,
                        flatten: true
                    },
                    {
                        src: ['<%= lib_dir %>/**/images/*'],
                        dest: '<%= target_dir %>/images',
                        expand: true,
                        flatten: true
                    },
                    {
                        src: ['<%= lib_dir %>/**/views/*'],
                        dest: '<%= target_dir %>/views',
                        expand: true,
                        flatten: true
                    }
                ]
            }
        },

        // Compile less files.
        less: {
            app: {
                options: {
                    compress: true,
                    cleancss: true
                },
                files: {
                    '<%= target_dir %>/css/cstl.css': '<%= src_dir %>/less/app.less'
                }
            },
            lib: {
                options: {
                    compress: true,
                    cleancss: true
                },
                files: {
                    '<%= target_dir %>/css/angular.min.css': '<%= lib_dir %>/angular/less/angular.less',
                    '<%= target_dir %>/css/bootstrap.min.css': '<%= lib_dir %>/bootstrap/less/bootstrap.less',
                    '<%= target_dir %>/css/c3.min.css': '<%= lib_dir %>/c3/less/c3.less',
                    '<%= target_dir %>/css/famfamfam-flags.min.css': '<%= lib_dir %>/famfamfam-flags/less/famfamfam-flags.less',
                    '<%= target_dir %>/css/font-awesome.min.css': '<%= lib_dir %>/font-awesome/less/font-awesome.less',
                    '<%= target_dir %>/css/highlight.min.css': '<%= lib_dir %>/highlight/less/highlight.less',
                    '<%= target_dir %>/css/jquery.min.css': '<%= lib_dir %>/jquery/less/jquery.less',
                    '<%= target_dir %>/css/openlayers.min.css': '<%= lib_dir %>/ol3/ol-3.13.0/ol.less'
                }
            }
        },

        // Transform HTML templates into an Angular module.
        html2js: {
            app: {
                options: {
                    base: '<%= src_dir %>',
                    htmlmin: {
                        removeComments: true,
                        collapseWhitespace: true
                    },
                    module: 'examind.templates',
                    singleModule: true
                },
                files: {
                    '<%= temp_dir %>/app-templates.js': [
                        '<%= src_dir %>/js/**/*.html',
                        '<%= src_dir %>/components/**/*.html',
                        '<%= src_dir %>/shared/**/*.html'
                    ]
                }
            }
        },

        // Minify application templates files.
        htmlmin: {
            app: {
                options: {
                    removeComments: true,
                    collapseWhitespace: true
                },
                files: [{
                    cwd: '<%= src_dir %>/',
                    src: '**/*.html',
                    dest: '<%= target_dir %>/',
                    expand: true
                }]
            }
        },

        // Merge script files.
        concat: {
            app: {
                options: {
                    banner: '(function(window, angular, undefined) {\'use strict\';',
                    footer: '})(window, window.angular);'
                },
                files: {
                    '<%= target_dir %>/js/cstl.js': [
                        '<%= src_dir %>/js/app-dependencies.js',
                        '<%= src_dir %>/js/app.js',
                        '<%= src_dir %>/components/**/*.module.js',
                        '<%= src_dir %>/components/**/*!(.module).js',
                        '<%= src_dir %>/shared/**/*.js',
                        '<%= src_dir %>/**/*.module.js',
                        '<%= src_dir %>/**/*!(.module).js',
                        '<%= temp_dir %>/app-templates.js'
                    ]
                }
            },
            app_index: {
                options: {
                    banner: '(function(window, angular, undefined) {\'use strict\';',
                    footer: '})(window, window.angular);'
                },
                files: {
                    '<%= target_dir %>/js/cstl-index.js': [
                        '<%= src_dir %>/js/app-index.js',
                        '<%= src_dir %>/js/directives.js',
                        '<%= src_dir %>/js/WebuiConfig.js',
                        '<%= src_dir %>/js/WebuiUtils.js',
                        '<%= src_dir %>/js/ExamindFactory.js'
                    ]
                }
            },
            login: {
                files : {
                    '<%= target_dir %>/js/login.js': [
                        '<%= src_dir %>/js/login.js',
                        '<%= src_dir %>/js/directives.js',
                        '<%= src_dir %>/js/WebuiConfig.js',
                        '<%= src_dir %>/js/WebuiUtils.js',
                        '<%= src_dir %>/js/ExamindFactory.js'
                    ]
                }
            },
            reset_password: {
                files : {
                    '<%= target_dir %>/js/reset-password.js': [
                        '<%= src_dir %>/js/reset-password.js',
                        '<%= src_dir %>/js/directives.js',
                        '<%= src_dir %>/js/WebuiConfig.js',
                        '<%= src_dir %>/js/WebuiUtils.js',
                        '<%= src_dir %>/js/ExamindFactory.js']
                }
            },
            lib: {
                files: {
                    '<%= target_dir %>/js/ace.min.js': '<%= lib_dir %>/ace/js/*.js',
                    '<%= target_dir %>/js/angular.min.js': '<%= lib_dir %>/angular/js/*.js',
                    '<%= target_dir %>/js/bootstrap.min.js': '<%= lib_dir %>/bootstrap/js/*.js',
                    '<%= target_dir %>/js/c3.min.js': '<%= lib_dir %>/c3/js/*.js',
                    '<%= target_dir %>/js/d3.min.js': '<%= lib_dir %>/d3/js/*.js',
                    '<%= target_dir %>/js/highlight.min.js': '<%= lib_dir %>/highlight/js/*.js',
                    '<%= target_dir %>/js/jquery.min.js': '<%= lib_dir %>/jquery/js/*.js',
                    '<%= target_dir %>/js/openlayers.min.js': [
                        '<%= lib_dir %>/ol3/ol-3.13.0/ol.js',
                        '<%= lib_dir %>/ol3/js/olext/CQL.js'
                    ],
                    '<%= target_dir %>/js/sockjs.min.js': '<%= lib_dir %>/sockjs/js/*.js',
                    '<%= target_dir %>/js/stomp.min.js': '<%= lib_dir %>/stomp/js/*.js',
                    '<%= target_dir %>/js/dygraph.min.js': '<%= lib_dir %>/dygraph/js/*.js',
                    '<%= target_dir %>/js/anime.min.js': 'node_modules/animejs/anime.min.js',
                    '<%= target_dir %>/js/moment.min.js': 'node_modules/moment/min/moment.min.js',
                    '<%= target_dir %>/js/rzslider.min.js': 'node_modules/angularjs-slider/dist/rzslider.min.js'
                }
            }
        },

        // Annotate AngularJS application script files for obfuscation.
        ngAnnotate: {
            app: {
                src: ['<%= target_dir %>/js/cstl.js'],
                dest: '<%= target_dir %>/js/cstl.js'
            },
            app_index: {
                src: ['<%= target_dir %>/js/cstl-index.js'],
                dest: '<%= target_dir %>/js/cstl-index.js'
            }
        },

        // Obfuscate application script files.
        uglify: {
            app: {
                src: ['<%= target_dir %>/js/cstl.js'],
                dest: '<%= target_dir %>/js/cstl.js'
            },
            app_index: {
                src: ['<%= target_dir %>/js/cstl-index.js'],
                dest: '<%= target_dir %>/js/cstl-index.js'
            }
        },

        // Watch for source changes.
        watch: {
            app: {
                files: ['<%= src_dir %>/**'],
                tasks: ['update'],
                options: {
                    spawn: false
                }
            }
        }
    });

    // Load NPM tasks.
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-less');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-ng-annotate');
    grunt.loadNpmTasks('grunt-html2js');
    grunt.loadNpmTasks('grunt-contrib-htmlmin');

    // Register tasks.
    grunt.registerTask('dev', ['jshint', 'clean', 'copy', 'less', 'html2js', 'concat', 'clean:temp']);
    grunt.registerTask('prod', ['clean:target', 'jshint', 'copy', 'less', 'html2js', 'concat', 'ngAnnotate', 'uglify', 'clean:temp']);
    grunt.registerTask('update', ['jshint:app', 'copy:app', 'less:app', 'html2js:app', 'concat', 'clean:temp']);
    grunt.registerTask('live', ['update', 'watch']);
};

