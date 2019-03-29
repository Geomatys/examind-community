/**
 * @namespace cstl.components.areaGraph
 * @requires ng
 */
angular.module('cstl.components.areaGraph', ['ng'])
    .directive('areaGraph', areaGraphDirective)
    .directive('areaGraphBrush', areaGraphBrushDirective)
    .directive('areaGraphZoom', areaGraphZoomDirective);

/**
 * @ngdoc controller
 * @name AreaGraphController
 *
 * @description
 * The area graph component API.
 *
 * @this AreaGraphController
 */
function AreaGraphController() {

    var self = this;

    var controls = [];


    self.$options = {
        interpolation: 'linear',
        margin: {
            top: 20,
            right: 20,
            bottom: 25,
            left: 40
        },
        x: {
            domain: undefined,
            field: 'date',
            label: undefined,
            scale: 'time',
            tickPadding: 4,
            ticks: undefined
        },
        y: {
            domain: undefined,
            field: 'value',
            label: undefined,
            scale: 'linear',
            tickPadding: 10,
            ticks: undefined
        }
    };

    self.$x = undefined;

    self.$y = undefined;


    self.$$setSize = function(size) {
        angular.forEach(controls, function(control) {
            control.$setSize(size);
        });
    };

    self.$$setData = function(data) {
        angular.forEach(controls, function(control) {
            control.$setData(data);
        });
    };

    self.$addControl = function(control) {
        controls.push(control);
    };

    self.$redraw = angular.noop;
}

/**
 * @ngdoc directive
 *
 * @description
 * The area graph component.
 *
 * @return {Object} The directive configuration object.
 */
function areaGraphDirective() {

    function areaGraphLink(scope, element, attrs, controller) {
        var opts = $.extend(true, controller.$options, scope.$eval(attrs.options)),
            xScale = controller.$x = (opts.x.scale === 'time') ? d3.time.scale() : d3.scale.linear(),
            yScale = controller.$y = (opts.y.scale === 'time') ? d3.time.scale() : d3.scale.linear();

        // Attach redraw function.
        controller.$redraw = redraw;

        // Axis functions.
        var xAxis = d3.svg.axis().scale(xScale)
            .orient('bottom')
            .tickPadding(opts.x.tickPadding).ticks(opts.x.ticks);
        var yAxis = d3.svg.axis().scale(yScale)
            .orient('left')
            .tickPadding(opts.y.tickPadding).ticks(opts.y.ticks);

        // Area function.
        var area = d3.svg.area().interpolate(opts.interpolation)
            .x(function(d) { return xScale(getAxisValue('x', d)); })
            .y1(function(d) { return yScale(getAxisValue('y', d)); });

        // Root SVG.
        var rootSvg = d3.select(element[0])
            .append('svg');
        var clipRectSvg = rootSvg.append('clipPath')
            .attr('id', 'clip')
            .append('rect');

        // Graph SVG.
        var graphSvg = rootSvg.append('g')
            .attr('class', 'graph')
            .attr('transform', 'translate(' + opts.margin.left + ',' + opts.margin.top + ')');
        var xAxisSvg = graphSvg.append('g')
            .attr('class', 'axis x');
        var yAxisSvg = graphSvg.append('g')
            .attr('class', 'axis y');
        var areaSvg = graphSvg.append('path')
            .attr('class', 'area')
            .attr('clip-path', 'url(#clip)');

        // Axis label SVG.
        var xAxisLabelSvg = graphSvg.append('text')
            .attr('class', 'label x')
            .attr('text-anchor', 'middle')
            .text(opts.x.label);
        var yAxisLabelSvg = graphSvg.append('text')
            .attr('dy', '1em')
            .attr('class', 'label y')
            .attr('text-anchor', 'middle')
            .attr('transform', 'rotate(-90)')
            .text(opts.y.label);


        // Observe "data" attribute reference changes.
        scope.$watch(attrs.data, dataChanged);

        // Observe element size changes.
        scope.$watch(getElementSize, sizeChanged, true);


        /**
         * Extracts the value of an axis from a row of the graph data.
         *
         * @param {String} axis The axis name ("x" or "y").
         * @param {Object|Array} dataRow The row value.
         * @return {Date|Number} The axis value.
         */
        function getAxisValue(axis, dataRow) {
            var field = opts[axis].field;
            if (angular.isString(field)) {
                return dataRow[field];
            } else if (angular.isFunction(field)) {
                return field(dataRow);
            } else if (angular.isNumber(field)) {
                return dataRow[field];
            }
            return undefined;
        }

        /**
         * Determines the domain of the specified axis.
         *
         * @param {String} axis The axis name ("x" or "y").
         * @param {Array} data The graph data.
         * @return {Array} The axis domain.
         */
        function getAxisDomain(axis, data) {
            var domain = opts[axis].domain || [];

            // Axis domain is explicitly defined.
            if (angular.isNumber(domain[0]) && angular.isNumber(domain[1])) {
                return domain;
            }

            // Extract axis values.
            var values = data.map(getAxisValue.bind(null, axis));

            // Axis domain must be partially computed.
            if (angular.isNumber(domain[0])) {
                return [domain[0], d3.max(values)];
            } else if (angular.isNumber(domain[1])) {
                return [d3.min(values), domain[1]];
            }

            // Axis domain must be computed.
            return d3.extent(values);
        }

        /**
         * Computes and returns the directive element size.
         *
         * @return {Array} The directive element size.
         */
        function getElementSize() {
            return [element.width(), element.height()];
        }

        /**
         * Redraws the graph with new data.
         *
         * @param {Array} data The area graph data.
         */
        function dataChanged(data) {
            if (angular.isArray(data)) {
                // Update functions.
                xScale.domain(getAxisDomain('x', data));
                yScale.domain(getAxisDomain('y', data));

                // Update SVG.
                areaSvg.datum(data);

                // Update plugins and redraw.
                controller.$$setData(data);
                redraw();
            } else if (typeof data.then === 'function') {
                data.then(dataChanged);
            }
        }

        /**
         * Resize and redraw the graph.
         *
         * @param {Array} size The element size.
         */
        function sizeChanged(size) {
            var innerWidth = Math.max(size[0] - opts.margin.left - opts.margin.right, 0),
                innerHeight = Math.max(size[1] - opts.margin.top - opts.margin.bottom, 0);

            // Update functions.
            xScale.range([0, innerWidth]);
            yScale.range([innerHeight, 0]);
            yAxis.tickSize(-innerWidth);
            area.y0(innerHeight);

            // Update SVG.
            rootSvg.attr('width', size[0]).attr('height', size[1]);
            clipRectSvg.attr('width', innerWidth).attr('height', innerHeight);
            xAxisSvg.attr('transform', 'translate(0,' + innerHeight + ')');
            xAxisLabelSvg.attr('transform', 'translate(' + (innerWidth / 2) + ' ' + (innerHeight + opts.margin.bottom) + ')');
            yAxisLabelSvg.attr('y', 0 - opts.margin.left).attr('x', 0 - (innerHeight / 2));

            // Update plugins and redraw.
            controller.$$setSize(size);
            redraw();
        }

        /**
         * Redraws the graph (data are needed).
         */
        function redraw() {
            if (areaSvg.datum()) {
                xAxisSvg.call(xAxis);
                yAxisSvg.call(yAxis);
                areaSvg.attr('d', area);
            }
        }
    }

    return {
        restrict: 'E',
        priority: 450,
        require: 'areaGraph',
        controller: AreaGraphController,
        link: areaGraphLink
    };
}

/**
 * @ngdoc controller
 * @name AreaGraphPluginController
 *
 * @description
 * The graph plugin API.
 *
 * @this AreaGraphPluginController
 */
function AreaGraphPluginController() {

    this.$setSize = angular.noop;

    this.$setData = angular.noop;
}

/**
 * @ngdoc directive
 *
 * @description
 * The brush plugin.
 *
 * @param $parse {$parse}
 *
 * @return {Object} The directive configuration object.
 */
function areaGraphBrushDirective($parse) {

    function areaGraphBrushLink(scope, element, attrs, controllers) {
        var brushCtrl = controllers[0],
            graphCtrl = controllers[1];

        var modelGetter = $parse(attrs.brushModel),
            modelSetter = modelGetter.assign || angular.noop,
            brushChanged = $parse(attrs.brushChanged);

        var brush = d3.svg.brush()
            .x(graphCtrl.$x)
            .on('brushend', brushed);

        var brushSvg = d3.select(element[0])
            .select('svg g.graph')
            .append('g')
            .attr('class', 'x brush')
            .call(brush);


        scope.$watch(attrs.brushModel, redraw, true);

        brushCtrl.$setSize = function(size) {
            var margin = graphCtrl.$options.margin;
            brushSvg.select('rect.background').attr('width', Math.max(size[0] - margin.left - margin.right, 0));
            brushSvg.selectAll('rect').attr('height', Math.max(size[1] - margin.top - margin.bottom, 0));
            redraw(modelGetter(scope));
        };

        brushCtrl.$setData = function() {
            redraw(modelGetter(scope));
        };

        graphCtrl.$addControl(brushCtrl);


        function brushed(event) {
            scope.$apply(function() {
                modelSetter(scope, brush.extent());
                brushChanged(scope, { $event: event });
            });
        }

        function redraw(extent) {
            var maxExtent = graphCtrl.$x.domain();
            if (angular.isArray(extent) && !angular.equals(extent, maxExtent)) {
                brush.extent(extent);
            } else {
                brush.clear();
            }
            brushSvg.call(brush);
        }
    }

    return {
        restrict: 'A',
        require: ['areaGraphBrush', 'areaGraph'],
        priority: 500,
        controller: AreaGraphPluginController,
        link: areaGraphBrushLink
    }
}

/**
 * @ngdoc directive
 *
 * @description
 * The zoom plugin.
 *
 * @return {Object} The directive configuration object.
 */
function areaGraphZoomDirective() {

    function areaGraphZoomLink(scope, element, attrs, ctrls) {
        var zoomCtrl = ctrls[0],
            graphCtrl = ctrls[1];

        var zoom = d3.behavior.zoom()
            .on('zoom', graphCtrl.$redraw);

        var zoomSvg = d3.select(element[0])
            .select('svg g.graph')
            .append('rect')
            .attr('class', 'zoom')
            .attr('fill', 'none')
            .call(zoom);


        zoomCtrl.$setSize = function(size) {
            zoom.x(graphCtrl.$x);
            var margin = graphCtrl.$options.margin;
            zoomSvg.attr('width', Math.max(size[0] - margin.left - margin.right, 0));
            zoomSvg.attr('height', Math.max(size[1] - margin.top - margin.bottom, 0));
        };

        zoomCtrl.$setData = function() {
            zoom.x(graphCtrl.$x);
        };

        graphCtrl.$addControl(zoomCtrl);
    }

    return {
        restrict: 'A',
        require: ['areaGraphZoom', 'areaGraph'],
        priority: 500,
        controller: AreaGraphPluginController,
        link: areaGraphZoomLink
    }
}