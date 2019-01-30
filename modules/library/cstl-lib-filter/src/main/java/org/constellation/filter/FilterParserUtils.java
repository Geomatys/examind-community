/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.filter;

import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import static org.constellation.api.CommonConstants.QUERY_CONSTRAINT;
import static org.constellation.filter.AbstractFilterParser.LOGGER;
import org.geotoolkit.cql.CQL;
import org.geotoolkit.cql.CQLException;
import org.geotoolkit.csw.xml.QueryConstraint;
import org.geotoolkit.filter.FilterFactoryImpl;
import org.geotoolkit.ogc.xml.XMLFilter;
import org.geotoolkit.ogc.xml.v110.FilterType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.PropertyName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FilterParserUtils {

    /**
     * Build a Filter with the specified CQL query
     *
     * @param cqlQuery A well-formed CQL query .
     * @return An OGC Filter Object.
     *
     * @throws org.geotoolkit.cql.CQLException
     * @throws javax.xml.bind.JAXBException
     */
    public static FilterType cqlToFilter(final String cqlQuery) throws CQLException, JAXBException {
        final FilterType result;
        final Object newFilter = CQL.parseFilter(cqlQuery, new FilterFactoryImpl());

        if (!(newFilter instanceof FilterType)) {
            result = new FilterType(newFilter);
        } else {
            result = (FilterType) newFilter;
        }
        return result;
    }

    /**
     * Build a CQL query with the specified Filter.
     *
     * @param filter A well-formed Filter .
     * @return A CSQL query string
     *
     * @throws org.geotoolkit.cql.CQLException
     * @throws javax.xml.bind.JAXBException
     */
    public static String filterToCql(final FilterType filter) throws CQLException, JAXBException {
        return CQL.write(filter);
    }

    /**
     * Extract a OCG filter from the query constraint of the received request.
     *
     * @param constraint
     * @return
     * @throws FilterParserException
     */
    public static XMLFilter getFilterFromConstraint(final QueryConstraint constraint) throws FilterParserException {

        //The null case must be trreated before calling this method
        if (constraint == null)  {
            throw new IllegalArgumentException("The null case must be already treated!");

        // both constraint type are filled we throw an exception
        } else if (constraint.getCqlText() != null && constraint.getFilter() != null) {
            throw new FilterParserException("The query constraint must be in Filter or CQL but not both.",
                    INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);

        // none constraint type are filled we throw an exception
        } else if ((constraint.getCqlText() == null || constraint.getCqlText().isEmpty()) && constraint.getFilter() == null) {
            throw new FilterParserException("The query constraint must contain a Filter or a CQL query (not empty).",
                    INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);

        // for a CQL request we transform it in Filter
        } else if (constraint.getCqlText() != null) {
            try {
                return FilterParserUtils.cqlToFilter(constraint.getCqlText());

            } catch (JAXBException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                throw new FilterParserException("JAXBException while parsing CQL query: " + ex.getMessage(), NO_APPLICABLE_CODE, QUERY_CONSTRAINT);
            } catch (CQLException ex) {
                throw new FilterParserException("The CQL query is malformed: " + ex.getMessage(), INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            } catch (UnsupportedOperationException ex) {
                throw new FilterParserException("The CQL query is not supported: " + ex.getMessage(), INVALID_PARAMETER_VALUE, QUERY_CONSTRAINT);
            }

        // for a filter we return directly it
        } else {
            return constraint.getFilter();
        }
    }

    /**
     * Replace The special character in a literal value for a propertyIsLike filter,
     * with the implementation specific value.
     *
     * @param pil A propertyIsLike filter.
     * @param wildchar The character replacing the filter wildChar.
     * @param singleChar The character replacing the filter singleChar.
     * @param escapeChar The character replacing the filter escapeChar.
     *
     * @return A formatted value.
     */
    public static String translateSpecialChar(final PropertyIsLike pil, final String wildchar, final String singleChar, final String escapeChar) {
        String brutValue = pil.getLiteral();
        brutValue = brutValue.replace(pil.getWildCard(), wildchar);
        brutValue = brutValue.replace(pil.getSingleChar(), singleChar);
        brutValue = brutValue.replace(pil.getEscape(), escapeChar);

        //for a date we remove the '-'
        if (isDateField((PropertyName) pil.getExpression())) {
            brutValue = brutValue.replaceAll("-", "");
            brutValue = brutValue.replace("Z", "");
        }
        return brutValue;
    }

    /**
     * Return true is the specified property has to be treated as a date Field.
     *
     * @param pName A property name extract from a filter.
     * @return true is the specified property has to be treated as a date Field.
     */
    public static boolean isDateField(final PropertyName pName) {
        if (pName != null && pName.getPropertyName() != null) {
            String propertyName = pName.getPropertyName();
            final int semicolonPos = propertyName.lastIndexOf(':');
            if (semicolonPos != -1) {
                propertyName = propertyName.substring(semicolonPos + 1);
            }
            return propertyName.contains("Date") || propertyName.contains("Modified")  || propertyName.contains("date")
                || propertyName.equalsIgnoreCase("TempExtent_begin") || propertyName.equalsIgnoreCase("TempExtent_end");
        }
        return false;
    }

    public static boolean isDateField(String propertyName) {
        if (propertyName != null) {
            final int semicolonPos = propertyName.lastIndexOf(':');
            if (semicolonPos != -1) {
                propertyName = propertyName.substring(semicolonPos + 1);
            }
            return propertyName.contains("Date") || propertyName.contains("Modified")  || propertyName.contains("date")
                || propertyName.equalsIgnoreCase("TempExtent_begin") || propertyName.equalsIgnoreCase("TempExtent_end");
        }
        return false;
    }

}
