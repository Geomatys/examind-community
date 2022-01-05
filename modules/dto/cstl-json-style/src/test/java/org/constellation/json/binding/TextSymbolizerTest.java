package org.constellation.json.binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TextSymbolizerTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void jsonToDto() throws Exception {
        final TextSymbolizer symbol = MAPPER.readValue(resource("symbols/text/text-symbol.json"), TextSymbolizer.class);
        assertNotNull(symbol);

        final LabelPlacement.Point expectedPoint = new LabelPlacement.Point();
        final XYExpr.AnchorPoint anchor = new XYExpr.AnchorPoint();
        anchor.x = "xxx";
        anchor.y = "yyyy";
        expectedPoint.setAnchor(anchor);

        final TextSymbolizer expectedSymbol = new TextSymbolizer();
        expectedSymbol.setLabelPlacement(expectedPoint);
        expectedSymbol.setLabel("identifier");
        expectedSymbol.setUnit("m");
        expectedSymbol.setGeometry("ST_Centroid(sis:geometry)");
        assertEquals(expectedSymbol, symbol);
    }

    static URL resource(String relativePath) {
        final URL resource = LabelPlacementTest.class.getResource(relativePath);
        assertNotNull("Resource file: "+relativePath, resource);
        return resource;
    }
}
