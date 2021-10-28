angular.module('examind.components.metadata.editor', [
    'examind.components.metadata.editor.auto.complete',
    'examind.components.metadata.editor.upload.image',
    'examind.components.metadata.editor.bbox.modal'
])
    .controller('MetadataEditorController', MetadataEditorController)
    .directive('metadataEditor', metadataEditorDirective);

function metadataEditorDirective() {
    return {
        restrict: "E",
        templateUrl: "components/metadata-editor/metadata-editor.html",
        controller: 'MetadataEditorController',
        controllerAs: "mdCtrl",
        scope: {
            theme: "@?",
            saveFunction: "&?",
            showMap: "=?",
            metadataValues: "=",
            load: "=?"
        }
    };
}


function MetadataEditorController($scope, $timeout, $translate, $modal, Examind) {
    var self = this;

    self.values = {
        metadataValues: []
    };

    self.load = $scope.load || {
        processing: true
    };

    self.load.processing = true;

    self.values.theme = $scope.theme || 'primary';

    self.save = $scope.saveFunction();

    self.values.metadataValues = $scope.metadataValues;

    self.showMap = $scope.showMap || false;

    // The RegEx object fot the ngPattern directive
    self.uriRegExp = /^([a-z][a-z0-9+.-]*):(?:\/\/((?:(?=((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*))(\3)@)?(?=(\[[0-9A-F:.]{2,}\]|(?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*))\5(?::(?=(\d*))\6)?)(\/(?=((?:[a-z0-9-._~!$&'()*+,;=:@\/]|%[0-9A-F]{2})*))\8)?|(\/?(?!\/)(?=((?:[a-z0-9-._~!$&'()*+,;=:@\/]|%[0-9A-F]{2})*))\10)?)(?:\?(?=((?:[a-z0-9-._~!$&'()*+,;=:@\/?]|%[0-9A-F]{2})*))\11)?(?:#(?=((?:[a-z0-9-._~!$&'()*+,;=:@\/?]|%[0-9A-F]{2})*))\12)?$/i;

    self.emailRegExp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

    self.checkFillMetadataTitleError = function () {
        if (self.values.metadataValues && self.values.metadataValues.length > 0) {
            for (var i = 0; i < self.values.metadataValues[0].root.children.length; i++) {
                var sb = self.values.metadataValues[0].root.children[i];
                for (var j = 0; j < sb.superblock.children.length; j++) {
                    var b = sb.superblock.children[j];
                    for (var k = 0; k < b.block.children.length; k++) {
                        var f = b.block.children[k];
                        if (f.field.tag === 'title') {
                            if (f.field.value !== null && f.field.value !== "" && angular.isDefined(f.field.value)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    };

    /**
     * Get all codelists for metadata editor
     */
    self.codeLists = {};

    Examind.metadata.getMetadataCodeLists()
        .then(function (response) {
            self.codeLists = response.data;
        });

    /**
     * Returns the title value of metadata.
     * a field should be marked with tag=title in json template.
     * @returns {string} the title value located in json model.
     */
    self.getMetadataTitle = function () {
        if (self.values.metadataValues && self.values.metadataValues.length > 0) {
            for (var i = 0; i < self.values.metadataValues[0].root.children.length; i++) {
                var sb = self.values.metadataValues[0].root.children[i];
                for (var j = 0; j < sb.superblock.children.length; j++) {
                    var b = sb.superblock.children[j];
                    for (var k = 0; k < b.block.children.length; k++) {
                        var f = b.block.children[k];
                        if (f.field.tag === 'title') {
                            return f.field.value;
                        }
                    }
                }
            }
        }
        return null;
    };

    /**
     * Returns true if given block is an occurrence of another block.
     * @param superBlockObj the given block's parent.
     * @param blockObj given block json to check.
     * @returns {boolean} return true if block's path is not null and the occurrence number is >1.
     */
    self.isBlockOccurrence = function (superBlockObj, blockObj) {
        var strPath = blockObj.block.path;
        if (!strPath) {
            return false;
        }
        if (endsWith(strPath, '+')) {
            return true;
        }
        var number = getNumeroForPath(strPath);
        var index = getFirstNonStrictIndex(superBlockObj, blockObj);
        return number > index;
    };

    /**
     * Add new occurrence of given block.
     * @param superBlockObj the parent of given block.
     * @param blockObj the given block to create a new occurrence based on it.
     */
    self.addBlockOccurrence = function (superBlockObj, blockObj) {
        var newBlock = {"type": "block", "block": {}};
        // Deep copy
        newBlock.block = jQuery.extend(true, {}, blockObj.block);
        var blockPath = newBlock.block.path;
        var commonStr = blockPath.substring(0, blockPath.lastIndexOf('['));
        var max = getMaxNumeroForBlock(superBlockObj, commonStr);
        for (var i = 0; i < newBlock.block.children.length; i++) {
            var fieldObj = newBlock.block.children[i];
            fieldObj.field.value = fieldObj.field.defaultValue;
            var fieldPath = fieldObj.field.path;
            fieldPath = fieldPath.replace(commonStr, '');
            if (fieldPath.indexOf('[') === 0) {
                fieldPath = '[' + max + fieldPath.substring(fieldPath.indexOf(']'));
                fieldPath = commonStr + fieldPath;
                fieldObj.field.path = fieldPath;
            }
        }
        newBlock.block.path = commonStr + '[' + max + ']';
        var indexOfBlock = superBlockObj.superblock.children.indexOf(blockObj);
        superBlockObj.superblock.children.splice(indexOfBlock + 1, 0, newBlock);
    };

    /**
     * Returns the maximum number used in each block which is child of given superBlock
     * and when block's path starts with given prefix.
     * @param superBlockObj the given superblock json object.
     * @param prefix the given string that each path must matches with.
     * @returns {number} the result incremented number.
     */
    function getMaxNumeroForBlock(superBlockObj, prefix) {
        var max = 1;
        for (var i = 0; i < superBlockObj.superblock.children.length; i++) {
            var childObj = superBlockObj.superblock.children[i];
            var childPath = childObj.block.path;
            if (childPath && childPath.indexOf(prefix) !== -1) {
                childPath = childPath.replace(prefix, '');
                if (childPath.indexOf('[') === 0) {
                    var numero = childPath.substring(1, childPath.indexOf(']'));
                    max = Math.max(max, parseInt(numero));
                }
            }
        }
        max++;
        return max;
    }

    /**
     * Proceed to remove the given block occurrence from its parent superblock.
     * @param superBlockObj the given block's parent.
     * @param blockObj the given block to remove.
     */
    self.removeBlockOccurrence = function (superBlockObj, blockObj) {
        var indexToRemove = superBlockObj.superblock.children.indexOf(blockObj);
        superBlockObj.superblock.children.splice(indexToRemove, 1);
    };

    function getFirstNonStrictIndex(superBlockObj, blockObj) {
        var blockPath = blockObj.block.path;
        var commonStr = blockPath.substring(0, blockPath.lastIndexOf('['));
        var result = 0;
        for (var i = 0; i < superBlockObj.superblock.children.length; i++) {
            var childObj = superBlockObj.superblock.children[i];
            var childPath = childObj.block.path;
            if (childObj.block.name !== blockObj.block.name) {
                continue;
            }
            if (childPath && childPath.indexOf(commonStr) !== -1) {
                childPath = childPath.replace(commonStr, '');
                if (childPath.indexOf('[') === 0) {
                    var numero = childPath.substring(1, childPath.indexOf(']'));
                    result = parseInt(numero);
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the current lang used.
     * For datepicker as locale.
     */
    self.getCurrentLang = function () {
        var lang = $translate.use();
        if (!lang) {
            lang = 'en';
        }
        return lang;
    };

    /**
     * This is the predefined values for some fields.
     */
    self.predefinedValues = {};
    self.predefinedValues.inspireThemes = [
        "Addresses", "Hydrography", "Administrative units", "Land cover",
        "Agricultural and aquaculture facilities", "Land use",
        "Area management/restriction/regulation zones and reporting units",
        "Meteorological geographical features", "Atmospheric conditions",
        "Mineral resources", "Bio-geographical regions", "Natural risk zones",
        "Buildings", "Oceanographic geographical features", "Cadastral parcels",
        "Orthoimagery", "Coordinate reference systems", "Population distribution — demography",
        "Elevation", "Production and industrial facilities", "Energy resources",
        "Protected sites", "Environmental monitoring facilities", "Sea regions",
        "Geographical grid systems", "Soil", "Geographical names", "Species distribution",
        "Geology", "Statistical units", "Habitats and biotopes",
        "Transport networks", "Human health and safety", "Utility and governmental services"];
    self.predefinedValues.referenceSystemIdentifier = [
        'WGS84 - EPSG:4326',
        'Fort Desaix / UTM 20 N - EPSG:2973',
        'UTM 20 N - EPSG:32620',
        'CGS 1967 / UTM 21 N - EPSG:3312',
        'UTM 22 N - EPSG:2971',
        'RGFG95 / UTM 22 N - EPSG:2972',
        'UTM 30 N - EPSG:32630',
        'UTM 31 N - EPSG:32631',
        'UTM 32 N - EPSG:32632',
        'RGF93 / Lambert-93 - EPSG:2154',
        'Lambert I - EPSG:27571',
        'Lambert II - EPSG:27572',
        'Lambert III - EPSG:27573',
        'Lambert IV - EPSG:27574',
        'CC42 - EPSG:3942',
        'WGS84 / UTM 20 N - EPSG:4559'];
    self.predefinedValues.distributionFormat = ['SHP', 'TAB', 'MIF/MID', 'KML', 'GML', 'GeoTIFF', 'ECW', 'Autre'];
    self.predefinedValues.specifications = [
        'No INSPIRE Data Specification',
        'INSPIRE Data Specification on Administrative Units',
        'INSPIRE Data Specification on Cadastral Parcels',
        'INSPIRE Data Specification on Geographical Names',
        'INSPIRE Data Specification on Hydrography',
        'INSPIRE Data Specification on Protected Sites',
        'INSPIRE Data Specification on Transport Networks',
        'INSPIRE Data Specifications on Addresses',
        'INSPIRE Specification on Coordinate Reference Systems',
        'INSPIRE Specification on Geographical Grid Systems',
        'Data Specification on Agricultural and Aquaculture Facilities',
        'Data Specification on Area management / restriction / regulation zones and reporting units',
        'Data Specification on Atmospheric Conditions- Meteorological geographical features',
        'Data Specification on Bio-geographical regions',
        'Data Specification on Buildings',
        'Data Specification on Elevation',
        'Data Specification on Energy Resources',
        'Data Specification on Environmental monitoring Facilities',
        'Data Specification on Geology',
        'Data Specification on Habitats and biotopes',
        'Data Specification on Human health and safety',
        'Data Specification on Land cover',
        'Data Specification on Land use',
        'Data Specification on Mineral Resources',
        'Data Specification on Natural risk zones',
        'Data Specification on Oceanographic geographical features',
        'Data Specification on Orthoimagery',
        'Data Specification on Population distribution - demography',
        'Data Specification on Production and Industrial Facilities',
        'Data Specification on Sea regions',
        'Data Specification on Soil',
        'Data Specification on Species distribution',
        'Data Specification on Statistical units',
        'Data Specification on Utility and Government Services',
        'RÈGLEMENT (UE) N°1089/2010'];
    self.predefinedValues.resultPass = ['nilReason:unknown', 'false', 'true'];

    /**
     * Function to set values for ISO INSPIRE selectOneMenu
     * the INSPIRE predefinedValues are defined in sql.
     * the corresponding code is the topicCategory codes
     * @param value
     * @param parentBlock
     */
    self.updateIsoInspireSelectOneMenu = function (value, parentBlock, index) {
        if (value) {
            var fieldIndexToChange = -1;
            var arr = [];
            for (var i = 0; i < parentBlock.children.length; i++) {
                var fobj = parentBlock.children[i];
                if (fobj.field && fobj.field.render === 'ISO_INSPIRE.codelist') {
                    arr.push(i);
                }
            }
            if (index < arr.length) {
                fieldIndexToChange = arr[index];
            }

            if (fieldIndexToChange !== -1) {
                var INSPIRE_ISO_MAP = {};
                /* jshint ignore:start */
                INSPIRE_ISO_MAP['Elevation'] = 'MD_TopicCategoryCode.elevation';
                INSPIRE_ISO_MAP['Geology'] = 'MD_TopicCategoryCode.geoscientificInformation';
                INSPIRE_ISO_MAP['Habitats and biotopes'] = 'MD_TopicCategoryCode.biota';
                INSPIRE_ISO_MAP['Environmental monitoring facilities'] = 'MD_TopicCategoryCode.structure';
                INSPIRE_ISO_MAP['Land cover'] = 'MD_TopicCategoryCode.imageryBaseMapsEarthCover';
                INSPIRE_ISO_MAP['Species distribution'] = 'MD_TopicCategoryCode.biota';
                INSPIRE_ISO_MAP['Land use'] = 'MD_TopicCategoryCode.planningCadastre';
                INSPIRE_ISO_MAP['Area management/restriction/regulation zones and reporting units'] = 'MD_TopicCategoryCode.planningCadastre';
                INSPIRE_ISO_MAP['Natural risk zones'] = 'MD_TopicCategoryCode.planningCadastre';
                INSPIRE_ISO_MAP['Buildings'] = 'MD_TopicCategoryCode.structure';
                INSPIRE_ISO_MAP['Oceanographic geographical features'] = 'MD_TopicCategoryCode.oceans';
                INSPIRE_ISO_MAP['Bio-geographical regions'] = 'MD_TopicCategoryCode.biota';
                INSPIRE_ISO_MAP['Sea regions'] = 'MD_TopicCategoryCode.oceans';
                INSPIRE_ISO_MAP['Statistical units'] = 'MD_TopicCategoryCode.boundaries';
                INSPIRE_ISO_MAP['Addresses'] = 'MD_TopicCategoryCode.location';
                INSPIRE_ISO_MAP['Geographical names'] = 'MD_TopicCategoryCode.location';
                INSPIRE_ISO_MAP['Hydrography'] = 'MD_TopicCategoryCode.inlandWaters';
                INSPIRE_ISO_MAP['Cadastral parcels'] = 'MD_TopicCategoryCode.planningCadastre';
                INSPIRE_ISO_MAP['Transport networks'] = 'MD_TopicCategoryCode.transportation';
                INSPIRE_ISO_MAP['Protected sites'] = 'MD_TopicCategoryCode.environment';
                INSPIRE_ISO_MAP['Administrative units'] = 'MD_TopicCategoryCode.boundaries';
                INSPIRE_ISO_MAP['Orthoimagery'] = 'MD_TopicCategoryCode.imageryBaseMapsEarthCover';
                INSPIRE_ISO_MAP['Meteorological geographical features'] = 'MD_TopicCategoryCode.climatologyMeteorologyAtmosphere';
                INSPIRE_ISO_MAP['Atmospheric conditions'] = 'MD_TopicCategoryCode.climatologyMeteorologyAtmosphere';
                INSPIRE_ISO_MAP['Agricultural and aquaculture facilities'] = 'MD_TopicCategoryCode.farming';
                INSPIRE_ISO_MAP['Production and industrial facilities'] = 'MD_TopicCategoryCode.structure';
                INSPIRE_ISO_MAP['Population distribution — demography'] = 'MD_TopicCategoryCode.society';
                INSPIRE_ISO_MAP['Mineral resources'] = 'MD_TopicCategoryCode.economy';
                INSPIRE_ISO_MAP['Human health and safety'] = 'MD_TopicCategoryCode.health';
                INSPIRE_ISO_MAP['Utility and governmental services'] = 'MD_TopicCategoryCode.utilitiesCommunication';
                INSPIRE_ISO_MAP['Soil'] = 'MD_TopicCategoryCode.geoscientificInformation';
                INSPIRE_ISO_MAP['Energy resources'] = 'MD_TopicCategoryCode.economy';
                /* jshint ignore:end */
                var valueToSet = INSPIRE_ISO_MAP[value];
                parentBlock.children[fieldIndexToChange].field.value = valueToSet;
            }
        }
    };


    /**
     * Add new occurrence of field. the field must have multiplicity gt 1
     * @param blockObj
     * @param fieldObj
     */
    self.addFieldOccurrence = function (blockObj, fieldObj) {
        var newField = {"type": "field", "field": {}};
        // Shallow copy
        newField.field = jQuery.extend({}, fieldObj.field);
        newField.field.value = fieldObj.field.defaultValue;
        if (newField.field.path.indexOf('+') === -1) {
            newField.field.path = newField.field.path + '+';
        }
        var indexOfField = blockObj.block.children.indexOf(fieldObj);
        blockObj.block.children.splice(indexOfField + 1, 0, newField);
    };


    /**
     * Returns true if the given field is an occurrence that can be removed from the form.
     * @param fieldObj
     * @returns {boolean}
     */
    self.isFieldOccurrence = function (fieldObj) {
        var strPath = fieldObj.field.path;
        if (endsWith(strPath, '+')) {
            return true;
        }
        var number = getNumeroForPath(strPath);
        if (number > 0) {
            return true;
        }
        return false;
    };

    /**
     * Utility function that returns if the string ends with given suffix.
     * @param str given string to check.
     * @param suffix given suffix.
     * @returns {boolean} returns true if str ends with suffix.
     */
    function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }

    /**
     * Return the occurrence number of given path.
     * @param path given path to read.
     * @returns {null} if path does not contains occurrence number.
     */
    function getNumeroForPath(path) {
        if (path && path.indexOf('[') !== -1) {
            var number = path.substring(path.lastIndexOf('[') + 1, path.length - 1);
            return number;
        }
        return null;
    }

    /**
     * Remove occurrence of given field for given block.
     * @param blockObj
     * @param fieldObj
     */
    self.removeFieldOccurrence = function (blockObj, fieldObj) {
        var indexToRemove = blockObj.block.children.indexOf(fieldObj);
        blockObj.block.children.splice(indexToRemove, 1);
    };

    self.isValidField = function (input) {
        if (input) {
            return (input.$valid || input.$pristine);
        }
        return true;
    };

    self.isValidRequired = function (input) {
        if (input) {
            return !input.$error.required;
        }
        return true;
    };

    self.isValidEmail = function (input) {
        if (input) {
            return !input.$error.pattern;
        }
        return true;
    };

    self.isValidUrl = function (input) {
        if (input) {
            return !input.$error.url;
        }
        return true;
    };

    self.isValidUri = function (input) {
        if (input) {
            return !input.$error.pattern;
        }
        return true;
    };

    self.isValidNumber = function (input) {
        if (input) {
            return !input.$error.number;
        }
        return true;
    };

    /**
     * Scrolling to top with animation effect.
     */
    self.scrollToTop = function () {
        jQuery('html, body').animate({scrollTop: '0px'}, 1000);
    };

    /**
     * Init editor events
     */
    self.initMetadataEditorEvents = function () {
        initCollapseEvents();
    };

    /**
     * attach events click for editor blocks elements
     * to allow collapsible/expandable panels.
     */
    function initCollapseEvents() {
        if (window.collapseEditionEventsRegistered) {
            return;
        } //to fix a bug with angular
        $(document).on('click', '#editorMetadata .small-block .heading-block', function () {
            var blockRow = $(this).parents('.block-row');
            var parent = $(this).parent('.small-block');
            parent.toggleClass('closed');
            blockRow.find('.collapse-block').toggleClass('hide');
            var icon = parent.find('.data-icon');
            if (icon.hasClass('fa-angle-down')) {
                icon.removeClass('fa-angle-down');
                icon.addClass('fa-angle-right');
            } else {
                icon.removeClass('fa-angle-right');
                icon.addClass('fa-angle-down');
            }
        });
        $(document).on('click', '#editorMetadata .collapse-row-heading', function () {
            $(this).parent().toggleClass('open');
            $(this).next().toggleClass('hide');
            var icon = $(this).find('.data-icon');
            if (icon.hasClass('fa-angle-down')) {
                icon.removeClass('fa-angle-down');
                icon.addClass('fa-angle-right');
            } else {
                icon.removeClass('fa-angle-right');
                icon.addClass('fa-angle-down');
            }
        });
        window.collapseEditionEventsRegistered = true;
    }

    /**
     * Proceed to check validation form
     * @param form
     */
    self.checkValidation = function (form) {
        if (form.$error.required) {
            for (var i = 0; i < form.$error.required.length; i++) {
                form.$error.required[i].$pristine = false;
            }
        }
        self.showValidationPopup(form);
    };

    /**
     * Display validation modal popup that show the form state
     * and when popup is closed then animate scroll to next invalid input.
     * @param form
     */
    self.showValidationPopup = function (form) {
        var validationPopup = $modal.open({
            templateUrl: 'validation_popup.html',
            controller: ['$scope', 'metadataform', function ($scope, metadataform) {
                $scope.metadataform = metadataform;
            }],
            resolve: {
                'metadataform': function () {
                    return form;
                }
            }
        });

        validationPopup.result.then(function (value) {
            //nothing to do here
        }, function () {
            if ($('#metadataform').hasClass('ng-invalid')) {
                var selClass = '.highlight-invalid';
                var firstInvalid = $(selClass).get(0);
                if (firstInvalid) {
                    var modalBody = $(selClass).parents('.modal-body');
                    if (modalBody && modalBody.get(0)) {
                        modalBody.animate(
                            {scrollTop: $(firstInvalid).offset().top - 200}, 1000);
                    } else {
                        $('html, body').animate(
                            {scrollTop: $(firstInvalid).offset().top - 200}, 1000);
                    }
                    $(firstInvalid).focus();
                }
            }
        });
    };

    /**
     * Display bbox modal to enter spatial extent for metadata block with render=BOUNDINGBOX
     * @param blockObj the current block that contains field children with current values.
     */
    self.openBboxModal = function (blockObj) {
        var bboxPopup = $modal.open({
            templateUrl: 'components/metadata-editor/bbox-select-modal/bbox-select-modal.html',
            controller: 'BboxMetadataModalController as bboxCtrl',
            resolve: {
                'block': function () {
                    return blockObj;
                }
            }
        });
    };


    // Hack for the heavy generation time of the metadata-editor form
    $timeout(function () {
        self.load.processing = false;
    }, 2000);

}
