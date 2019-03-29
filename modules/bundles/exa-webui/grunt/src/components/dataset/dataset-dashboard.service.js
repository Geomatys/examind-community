angular.module("examind.components.dataset")
    .service("DatasetDashboard", DatasetDashboardService);

/**
 * @name DatasetDashboard
 *
 * @constructor
 */
function DatasetDashboardService() {
    var self = this;

    var _dataset = null,
        _data = null,
        _style = null;

    Object.defineProperties(self, {
        dataset: {
            enumerable: true,
            get: function() {
                return _dataset;
            },
            set: function(dataset) {
                if (_dataset && dataset && _dataset.id !== dataset.id) {
                    self.data = null;
                }
                return (_dataset = dataset);
            }
        },
        data: {
            enumerable: true,
            get: function() {
                return _data;
            },
            set: function(data) {
                return (_data = data);
            }
        },
        style: {
            enumerable: true,
            get: function() {
                return _style;
            },
            set: function(style) {
                return (_style = style);
            }
        }
    });
}