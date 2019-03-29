angular.module("examind.components.dataset.explorer")
    .factory("DatasetExplorerStyle", DatasetExplorerStyleFactory);

/**
 * @name DatasetExplorerStyleFactory
 *
 * @returns Object
 */
function DatasetExplorerStyleFactory() {
    /**
     * @name DatasetExplorerStyle
     * @type Object
     */
    var self = {};

    /**
     * @name DatasetExplorerStyle#fill
     * @type {{default: ol.style.Fill, selected: ol.style.Fill, hover: ol.style.Fill}}
     */
    self.fill = {
        default : new ol.style.Fill({
            color: 'rgba(255,255,255,0.4)'
        }),
        selected : new ol.style.Fill({
            color: 'rgba(255,255,255,0.4)'
        }),
        hover : new ol.style.Fill({
            color: 'rgba(255,255,255,0.4)'
        })
    };

    /**
     * @name DatasetExplorerStyle#stroke
     * @type {{default: ol.style.Stroke, selected: ol.style.Stroke, hover: ol.style.Stroke}}
     */
    self.stroke = {
        default : new ol.style.Stroke({
            color: '#cc7d15',
            width: 1.25
        }),
        selected : new ol.style.Stroke({
            color: '#2dcc42',
            width: 1.25
        }),
        hover : new ol.style.Stroke({
            color: '#3399CC',
            width: 1.25
        })
    };

    /**
     * @name DatasetExplorerStyle#style
     * @type {{default: [ol.style.Style], selected: [ol.style.Style], hover: [ol.style.Style]}}
     */
    self.style = {
        default : [new ol.style.Style({
            fill: self.fill.default,
            stroke: self.stroke.default,
            zIndex: 1
        })],
        selected : [new ol.style.Style({
            fill: self.fill.selected,
            stroke: self.stroke.selected,
            zIndex: 2
        })],
        hover : [new ol.style.Style({
            fill: self.fill.hover,
            stroke: self.stroke.hover,
            zIndex: 3
        })]
    };

    /**
     * @name DatasetExplorerStyle#styleFunction
     * @type {{default: Function, selected: Function, hover: Function}}
     */
    self.styleFunction = {
        default : function _datasetExplorerStyleDefault(feature, resolution) {
            return self.style.default;
        },
        selected : function _datasetExplorerStyleSelected(feature, resolution) {
            return self.style.selected;
        },
        hover : function _datasetExplorerStyleHover(feature, resolution) {
            return self.style.hover;
        }
    };

    return self;
}