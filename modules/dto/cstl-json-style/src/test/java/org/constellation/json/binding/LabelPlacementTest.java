package org.constellation.json.binding;

import org.junit.Test;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.style.LinePlacement;
import org.opengis.style.PointPlacement;

import static java.lang.Double.parseDouble;
import static org.constellation.json.binding.TextSymbolizerTest.MAPPER;
import static org.constellation.json.binding.TextSymbolizerTest.resource;
import static org.constellation.json.util.StyleFactories.FF;
import static org.constellation.json.util.StyleFactories.SF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LabelPlacementTest {


    @Test
    public void pointOpengisToDTO() {
        final PointPlacement point = SF.pointPlacement(
                SF.anchorPoint(2.1, 3.1), SF.displacement(4.2, 5.2), FF.literal(0.5));
        final LabelPlacement placement = LabelPlacement.toJsonBinding(point)
                .orElseThrow(() -> new AssertionError("Cannot decode OpenGIS Point label placement"));
        assertTrue("label placement should be a point", placement instanceof LabelPlacement.Point);

        LabelPlacement.Point pp = (LabelPlacement.Point) placement;
        assertEquals("roation", 0.5, parseDouble(pp.getRotation()), 1e-1);
        assertEquals("anchor x", 2.1, parseDouble(pp.getAnchor().x), 1e-1);
        assertEquals("anchor y", 3.1, parseDouble(pp.getAnchor().y), 1e-1);
        assertEquals("displacement x", 4.2, parseDouble(pp.getDisplacement().x), 1e-1);
        assertEquals("displacement y", 5.2, parseDouble(pp.getDisplacement().y), 1e-1);
    }

    @Test
    public void pointDtoToOpengis() {
        final LabelPlacement.Point point = new LabelPlacement.Point();
        point.setRotation("2.4");
        final XYExpr.AnchorPoint anchor = new XYExpr.AnchorPoint();
        anchor.x = "1.1";
        anchor.y = "4.4";
        point.setAnchor(anchor);
        final XYExpr.Displacement displacement = new XYExpr.Displacement();
        displacement.x = "2.2";
        displacement.y = "3.3";
        point.setDisplacement(displacement);

        final org.opengis.style.LabelPlacement placement = point.toType();
        assertTrue("Unexpected data type", placement instanceof PointPlacement);
        PointPlacement pp = (PointPlacement) placement;
        assertExprEquals("rotation", 2.4, pp.getRotation(), 1e-1);
        assertExprEquals("anchor X", 1.1, pp.getAnchorPoint().getAnchorPointX(), 1e-1);
        assertExprEquals("anchor Y", 4.4, pp.getAnchorPoint().getAnchorPointY(), 1e-1);
        assertExprEquals("displacement X", 2.2, pp.getDisplacement().getDisplacementX(), 1e-1);
        assertExprEquals("displacement Y", 3.3, pp.getDisplacement().getDisplacementY(), 1e-1);
    }

    @Test
    public void lineOpengisToDTO() {
        final LinePlacement line = SF.linePlacement(
                FF.literal(1.1), FF.literal(2.2), FF.literal(3.3), true, true, false);
        final LabelPlacement placement = LabelPlacement.toJsonBinding(line)
                .orElseThrow(() -> new AssertionError("Cannot decode OpenGIS Point label placement"));
        assertTrue("label placement should be a line", placement instanceof LabelPlacement.Line);
        LabelPlacement.Line lp = (LabelPlacement.Line) placement;

        assertTrue(lp.isRepeated());
        assertTrue(lp.isAligned());
        assertFalse(lp.isGeneralize());

        assertEquals("perpendicular offset", 1.1, parseDouble(lp.getPerpendicularOffset()), 1e-1);
        assertEquals("initial gap", 2.2, parseDouble(lp.getInitialGap()), 1e-1);
        assertEquals("gap", 3.3, parseDouble(lp.getGap()), 1e-1);
    }

    @Test
    public void lineDtoToOpengis() {
        final LabelPlacement.Line line = new LabelPlacement.Line();
        line.setGap("2.2");
        line.setInitialGap("3.3");
        line.setPerpendicularOffset("4.4");
        line.setRepeated(false);
        line.setAligned(true);
        line.setGeneralize(true);

        final org.opengis.style.LabelPlacement placement = line.toType();
        assertTrue("Unexpected data type", placement instanceof LinePlacement);
        LinePlacement lp = (LinePlacement) placement;

        assertExprEquals("gap", 2.2, lp.getGap(), 1e-1);
        assertExprEquals("initial gap", 3.3, lp.getInitialGap(), 1e-1);
        assertExprEquals("perpendicular offset", 4.4, lp.getPerpendicularOffset(), 1e-1);
    }

    /**
     * Ensure that DTO is properly defined and no ambiguity resides, so Jackson can parse it properly.
     * The main point is to ensure that polymorphism is well-managed.
     */
    @Test
    public void jsonToDto() throws Exception {
        LabelPlacement result = MAPPER.readValue(resource("symbols/text/line-placement.json"), LabelPlacement.class);
        assertNotNull("Decoded label placement", result);
        assertTrue("JSON should have been decoded to specialized type Line, but it was: " + result.getClass(), result instanceof LabelPlacement.Line);

        final LabelPlacement.Line expectedLine = new LabelPlacement.Line();
        expectedLine.setPerpendicularOffset("offset");
        expectedLine.setGap("a gap");
        assertEquals(expectedLine, result);

        result = MAPPER.readValue(resource("symbols/text/point-placement.json"), LabelPlacement.class);
        assertNotNull("Decoded label placement", result);
        assertTrue("JSON should have been decoded to specialized type Line, but it was: " + result.getClass(), result instanceof LabelPlacement.Point);

        final LabelPlacement.Point expectedPoint = new LabelPlacement.Point();
        final XYExpr.AnchorPoint anchor = new XYExpr.AnchorPoint();
        anchor.x = "xxx";
        anchor.y = "yyyy";
        expectedPoint.setAnchor(anchor);
        assertEquals(expectedPoint, result);
    }

    /**
     *
     * @param title Title for the assertion that compares values.
     * @param expected Value that should be found in input literal expression
     * @param value The expression to evaluate. We expect it to be a literal, otherwise the assertion fails.
     * @param epsilon Double value comparison tolerance.
     */
    private void assertExprEquals(final String title, final double expected, final Expression value, double epsilon) {
        assertTrue("Input expression should be a literal", value instanceof Literal);
        final org.opengis.filter.Expression<?, Double> valueAsDouble = ((Literal<?, ?>) value).toValueType(Double.class);
        assertEquals(title, expected, valueAsDouble.apply(null), epsilon);
    }
}
