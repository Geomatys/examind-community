package org.constellation.json.binding;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import org.constellation.json.binding.XYExpr.AnchorPoint;
import org.constellation.json.binding.XYExpr.Displacement;
import org.constellation.json.util.StyleFactories;
import org.opengis.style.LinePlacement;
import org.opengis.style.PointPlacement;

import static org.constellation.json.util.StyleUtilities.parseExpression;
import static org.constellation.json.util.StyleUtilities.toCQL;
import static org.constellation.json.util.StyleUtilities.type;

@JsonSubTypes({
        @JsonSubTypes.Type(LabelPlacement.Point.class),
        @JsonSubTypes.Type(LabelPlacement.Line.class)
})
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
public interface LabelPlacement extends StyleElement<org.opengis.style.LabelPlacement> {

    static Optional<LabelPlacement> toJsonBinding(org.opengis.style.LabelPlacement placement) {
        if (placement instanceof LinePlacement) {
            LinePlacement lp = (LinePlacement) placement;
            final Line jsonPlacement = new Line();
            jsonPlacement.aligned = lp.IsAligned();
            jsonPlacement.repeated = lp.isRepeated();
            jsonPlacement.generalize = lp.isGeneralizeLine();
            if (lp.getPerpendicularOffset() != null) jsonPlacement.setPerpendicularOffset(toCQL(lp.getPerpendicularOffset()));
            if (lp.getInitialGap() != null) jsonPlacement.setInitialGap(toCQL(lp.getInitialGap()));
            if (lp.getGap() != null) jsonPlacement.setGap(toCQL(lp.getGap()));
            return Optional.of(jsonPlacement);

        } else if (placement instanceof PointPlacement) {
            PointPlacement pp = (PointPlacement) placement;
            final Point jsonPlacement = new Point();
            jsonPlacement.setAnchor(XYExpr.toJsonBinding(pp.getAnchorPoint()));
            jsonPlacement.setDisplacement(XYExpr.toJsonBinding(pp.getDisplacement()));
            if (pp.getRotation() != null) jsonPlacement.setRotation(toCQL(pp.getRotation()));
            return Optional.of(jsonPlacement);
        }

        return Optional.empty();
    }

    class Point implements LabelPlacement {

        private AnchorPoint anchor;
        private Displacement displacement;
        private String rotation = "0";

        public AnchorPoint getAnchor() {
            return anchor;
        }

        public void setAnchor(AnchorPoint anchor) {
            this.anchor = anchor;
        }

        public Displacement getDisplacement() {
            return displacement;
        }

        public void setDisplacement(Displacement displacement) {
            this.displacement = displacement;
        }

        public String getRotation() {
            return rotation;
        }

        public void setRotation(String rotation) {
            this.rotation = rotation;
        }

        @Override
        public org.opengis.style.LabelPlacement toType() {
            return StyleFactories.SF.pointPlacement(type(anchor), type(displacement), parseExpression(rotation));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point point = (Point) o;

            return Objects.equals(anchor, point.anchor)
                    && Objects.equals(displacement, point.displacement)
                    && Objects.equals(rotation, point.rotation);
        }

        @Override
        public int hashCode() {
            int result = anchor != null ? anchor.hashCode() : 0;
            result = 31 * result + (displacement != null ? displacement.hashCode() : 0);
            result = 31 * result + (rotation != null ? rotation.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "LabelPlacement.Point[", "]")
                    .add("anchor=" + anchor)
                    .add("displacement=" + displacement)
                    .add("rotation='" + rotation + "'")
                    .toString();
        }
    }

    class Line implements LabelPlacement {

        private String perpendicularOffset = "0";
        private String initialGap = "0";
        private String gap = "0";

        private boolean repeated;
        private boolean aligned;
        private boolean generalize;

        public String getPerpendicularOffset() {
            return perpendicularOffset;
        }

        public void setPerpendicularOffset(String perpendicularOffset) {
            this.perpendicularOffset = perpendicularOffset;
        }

        public String getInitialGap() {
            return initialGap;
        }

        public void setInitialGap(String initialGap) {
            this.initialGap = initialGap;
        }

        public String getGap() {
            return gap;
        }

        public void setGap(String gap) {
            this.gap = gap;
        }

        public boolean isRepeated() {
            return repeated;
        }

        public void setRepeated(boolean repeated) {
            this.repeated = repeated;
        }

        public boolean isAligned() {
            return aligned;
        }

        public void setAligned(boolean aligned) {
            this.aligned = aligned;
        }

        public boolean isGeneralize() {
            return generalize;
        }

        public void setGeneralize(boolean generalize) {
            this.generalize = generalize;
        }

        @Override
        public org.opengis.style.LabelPlacement toType() {
            return StyleFactories.SF.linePlacement(
                    parseExpression(perpendicularOffset),
                    parseExpression(initialGap),
                    parseExpression(gap),
                    repeated, aligned, generalize
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Line line = (Line) o;

            return repeated == line.repeated
                    && aligned == line.aligned
                    && generalize == line.generalize
                    && Objects.equals(perpendicularOffset, line.perpendicularOffset)
                    && Objects.equals(initialGap, line.initialGap)
                    && Objects.equals(gap, line.gap);
        }

        @Override
        public int hashCode() {
            int result = perpendicularOffset != null ? perpendicularOffset.hashCode() : 0;
            result = 31 * result + (initialGap != null ? initialGap.hashCode() : 0);
            result = 31 * result + (gap != null ? gap.hashCode() : 0);
            result = 31 * result + (repeated ? 1 : 0);
            result = 31 * result + (aligned ? 1 : 0);
            result = 31 * result + (generalize ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", "LabelPlacement.Line[", "]")
                    .add("perpendicularOffset='" + perpendicularOffset + "'")
                    .add("initialGap='" + initialGap + "'")
                    .add("gap='" + gap + "'")
                    .add("repeated=" + repeated)
                    .add("aligned=" + aligned)
                    .add("generalize=" + generalize)
                    .toString();
        }
    }
}
