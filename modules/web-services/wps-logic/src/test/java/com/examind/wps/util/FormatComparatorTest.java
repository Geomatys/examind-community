/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.examind.wps.util;

import java.util.Arrays;
import org.geotoolkit.wps.xml.v200.Format;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FormatComparatorTest {

    @Test
    public void testWithNullValues() {
        final Format[] values = {
            new Format(null, "image/png", null, null),
            new Format("toto/titi", false),
            new Format("UTF-8", null, "http://my.schemas.com/toto.xsd", null),
            new Format("US-ASCII", null, "http://my.schemas.com/toto.xsd", null),
            new Format(null, null, null, null),
            new Format(null, true)
        };

        checkSort(values, new int[]{5, 0, 1, 3, 2, 4, 0});

    }

    @Test
    public void testDifferentMimes() {
        final Format[] values = {
            new Format("image/png", false),
            new Format("toto/titi", false),
            new Format("application/json", false),
            new Format("text/xml", true)
        };

        checkSort(values, new int[]{3, 2, 0, 1});
    }

    @Test
    public void testSameMimeType() {
        final Format[] values = {
            new Format("UTF-8", "text/xml", "http://my.schemas.com/toto.xsd", null),
            new Format(null, "text/xml", null, null),
            new Format("US-ASCII", "text/xml", "http://my.schemas.com/toto.xsd", null),
            new Format("US-ASCII", "text/xml", "http://my.schemas.com/tata.xsd", null)
        };

        checkSort(values, new int[]{3, 2, 0, 1});

    }

    @Test
    public void testSameEncoding() {
        final Format[] values = {
            new Format("US-ASCII", "text/xml", null, null),
            new Format("US-ASCII", "text/xml", "http://my.schemas.com/tata.xsd", null),
            new Format("US-ASCII", "text/xml", "http://my.schemas.com/toto.xsd", null)
        };

        checkSort(values, new int[]{1, 2, 0});
    }

    private void checkSort(final Format[] values, final int[] expectedOrder) {
        final Format[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted, new WPSUtils.FormatComparator());

        for (int i = 0 ; i < sorted.length ; i++) {
            if (sorted[i] != values[expectedOrder[i]]) {
                Assert.fail(String.format(
                        "Bad sort order at index %d%nExpected: %s%nFound: %s",
                        i, sorted[i], values[expectedOrder[i]]
                ));
            }
        }
    }
}
