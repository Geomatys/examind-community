package org.constellation.json.binding;

import java.util.Objects;
import java.util.function.BiFunction;
import org.constellation.json.util.StyleFactories;
import org.opengis.filter.Expression;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleUtilities.parseExpression;
import static org.constellation.json.util.StyleUtilities.toCQL;

public class XYExpr<T> implements StyleElement<T> {

    protected String x = "0";
    protected String y = "0";

    protected final BiFunction<Expression, Expression, T> styleEltBuilder;

    protected XYExpr(BiFunction<Expression, Expression, T> styleEltBuilder) {
        ensureNonNull("Style Element Builder", styleEltBuilder);
        this.styleEltBuilder = styleEltBuilder;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    @Override
    public T toType() {
        return styleEltBuilder.apply(parseExpression(x), parseExpression(y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XYExpr<?> xyExpr = (XYExpr<?>) o;

        return Objects.equals(x, xyExpr.x) && Objects.equals(y, xyExpr.y);
    }

    @Override
    public int hashCode() {
        int result = x != null ? x.hashCode() : 0;
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + getClass().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"("+x+", "+y+")";
    }

    public static class AnchorPoint extends XYExpr<org.opengis.style.AnchorPoint> {

        public AnchorPoint() {
            super(StyleFactories.SF::anchorPoint);
        }
    }

    public static class Displacement extends XYExpr<org.opengis.style.Displacement> {

        public Displacement() {
            super(StyleFactories.SF::displacement);
        }
    }

    public static Displacement toJsonBinding(org.opengis.style.Displacement displacement) {
        if (displacement == null) return null;
        Displacement result = new Displacement();
        if (displacement.getDisplacementX() != null) result.x = toCQL(displacement.getDisplacementX());
        if (displacement.getDisplacementY() != null) result.y = toCQL(displacement.getDisplacementY());
        return result;
    }

    public static AnchorPoint toJsonBinding(org.opengis.style.AnchorPoint anchor) {
        if (anchor == null) return null;
        AnchorPoint result = new AnchorPoint();
        if (anchor.getAnchorPointX() != null) result.x = toCQL(anchor.getAnchorPointX());
        if (anchor.getAnchorPointY() != null) result.y = toCQL(anchor.getAnchorPointY());
        return result;
    }
}
