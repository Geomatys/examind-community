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

package org.constellation.sos.io.lucene;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.index.SearchingException;
import org.geotoolkit.lucene.filter.SpatialQuery;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

import javax.xml.namespace.QName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.sos.io.lucene.LuceneObervationUtils.getLuceneTimeValue;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE_NAME;
import static org.geotoolkit.observation.OMUtils.getVersionFromHints;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.ogc.xml.v200.TimeAfterType;
import org.geotoolkit.ogc.xml.v200.TimeBeforeType;
import org.geotoolkit.ogc.xml.v200.TimeDuringType;
import org.geotoolkit.ogc.xml.v200.TimeEqualsType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;

/**
 * TODO
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class LuceneObservationFilter implements ObservationFilterReader {

    private StringBuilder luceneRequest;

    private LuceneObservationSearcher searcher;

    protected final List<Filter> eventTimes = new ArrayList<>();

    protected QName resultModel;

    protected ResponseModeType responseMode;

    protected final String phenomenonIdBase;

    protected String version = null;

    protected OMEntity objectType = null;
    
    private static final String OR_OPERATOR = " OR ";

    public LuceneObservationFilter(final LuceneObservationFilter omFilter) throws DataStoreException {
        this.searcher = omFilter.searcher;
        this.phenomenonIdBase  = omFilter.phenomenonIdBase;
    }

    public LuceneObservationFilter(final Path confDirectory, final Map<String, Object> properties) throws DataStoreException {
        this.phenomenonIdBase  = (String) properties.get(PHENOMENON_ID_BASE_NAME);
        try {
            this.searcher = new LuceneObservationSearcher(confDirectory, "");
        } catch (IndexingException ex) {
            throw new DataStoreException("IndexingException in LuceneObservationFilter constructor", ex);
        }
    }

    @Override
    public void init(OMEntity objectType, Map<String, Object> hints) throws DataStoreException {
        this.objectType = objectType;
        this.version                 = getVersionFromHints(hints);
        this.eventTimes.clear();
         
        switch (objectType) {
            case FEATURE_OF_INTEREST: luceneRequest = new StringBuilder("type:foi"); break;
            case OBSERVED_PROPERTY:   luceneRequest = new StringBuilder("type:phenomenon"); break;
            case PROCEDURE:           luceneRequest = new StringBuilder("type:procedure"); break;
            case OFFERING:            luceneRequest = new StringBuilder("type:offering"); break;
            case OBSERVATION:         initFilterObservation(hints);break;
            case RESULT:              initFilterGetResult(hints);break;
            case LOCATION:
            case HISTORICAL_LOCATION: throw new UnsupportedOperationException("Not supported yet.");
            default: throw new DataStoreException("unexpected object type:" + objectType);
        }
    }
    
    private void initFilterObservation(final Map<String, Object> hints) {
        this.responseMode = (ResponseModeType) hints.get("responseMode");
        this.resultModel = (QName) hints.get("resultModel");
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            luceneRequest = new StringBuilder("type:measurement ");
        } else {
            luceneRequest = new StringBuilder("type:observation ");
        }

        if (ResponseModeType.RESULT_TEMPLATE.equals(responseMode)) {
            luceneRequest.append("template:TRUE ");
        } else {
            luceneRequest.append("template:FALSE ");
        }
        this.objectType = OMEntity.OBSERVATION;
    }

    private void initFilterGetResult(final Map<String, Object> hints) {
        String procedure = (String) hints.get("procedure");
        this.resultModel = (QName) hints.get("resultModel");
        this.responseMode = (ResponseModeType) hints.get("responseMode");
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            luceneRequest = new StringBuilder("type:measurement AND template:FALSE AND procedure:\"" + procedure + "\" ");
        } else {
            luceneRequest = new StringBuilder("type:observation AND template:FALSE AND procedure:\"" + procedure + "\" ");
        }
        this.resultModel = resultModel;
        this.objectType = OMEntity.RESULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcedure(final List<String> procedures) {
        if (procedures != null && !procedures.isEmpty()) {
            luceneRequest.append(" ( ");
            for (String s : procedures) {
                luceneRequest.append(" procedure:\"").append(s).append("\" OR ");
            }
            luceneRequest.delete(luceneRequest.length() - 3, luceneRequest.length());
            luceneRequest.append(") ");
        }
    }

    private boolean allPhenonenon(final List<String> phenomenons) {
        return phenomenons.size() == 1 && phenomenons.get(0).equals(phenomenonIdBase + "ALL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObservedProperties(final List<String> phenomenon) {
        if (phenomenon != null && !allPhenonenon(phenomenon) && !phenomenon.isEmpty()) {
            boolean getPhen = OMEntity.OBSERVED_PROPERTY.equals(objectType);
            luceneRequest.append(" AND( ");
            for (String p : phenomenon) {
                if (getPhen) {
                    if (p.startsWith(phenomenonIdBase)) {
                        p = p.substring(phenomenonIdBase.length());
                    }
                    luceneRequest.append(" id:\"").append(p).append('"').append(OR_OPERATOR);
                } else {
                    luceneRequest.append(" observed_property:\"").append(p).append('"').append(OR_OPERATOR);
                }

            }
            luceneRequest.delete(luceneRequest.length() - 3, luceneRequest.length());
            luceneRequest.append(") ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFeatureOfInterest(final List<String> fois) {
        if (fois != null && !fois.isEmpty()) {
            boolean getFOI = OMEntity.FEATURE_OF_INTEREST.equals(objectType);
            luceneRequest.append(" AND (");
            for (String foi : fois) {
                if (getFOI) {
                    luceneRequest.append("id:").append(foi).append(" OR ");
                } else {
                    luceneRequest.append("feature_of_interest:").append(foi).append(" OR ");
                }
            }
            luceneRequest.delete(luceneRequest.length() - 3, luceneRequest.length());
            luceneRequest.append(") ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObservationIds(List<String> ids) {
        if (!ids.isEmpty()) {
            luceneRequest.append(" AND (");
            for (String oid : ids) {
                luceneRequest.append("id:\"").append(oid).append("\" OR ");
            }
            luceneRequest.delete(luceneRequest.length() - 3, luceneRequest.length());
            luceneRequest.append(") ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeFilter(final TemporalOperator tFilter) throws DataStoreException {
        // we get the property name (not used for now)
        // String propertyName = tFilter.getExpression1()
        Object time = tFilter.getExpressions().get(1);
        TemporalOperatorName type = tFilter.getOperatorType();
        if (type == TemporalOperatorName.EQUALS) {
            eventTimes.add(new TimeEqualsType("result_time", time));
            if (time instanceof Period tp) {
                final String begin      = getLuceneTimeValue(tp.getBeginning().getDate());
                final String end        = getLuceneTimeValue(tp.getEnding().getDate());

                // we request directly a multiple observation or a period observation (one measure during a period)
                luceneRequest.append("AND (");
                luceneRequest.append(" sampling_time_begin:").append(begin).append(" AND ");
                luceneRequest.append(" sampling_time_end:").append(end).append(") ");

            // if the temporal object is a timeInstant
            } else if (time instanceof Instant ti) {
                final String position    = getLuceneTimeValue(ti.getDate());
                luceneRequest.append("AND (");

                // case 1 a single observation
                luceneRequest.append("(sampling_time_begin:'").append(position).append("' AND sampling_time_end:NULL)");
                luceneRequest.append(OR_OPERATOR);

                //case 2 multiple observations containing a matching value
                luceneRequest.append("(sampling_time_begin: [19700000 TO ").append(position).append("] ").append(" AND sampling_time_end: [").append(position).append(" TO 30000000]))");

            } else {
                throw new ObservationStoreException("TM_Equals operation require timeInstant or TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (type == TemporalOperatorName.BEFORE) {
            eventTimes.add(new TimeBeforeType("result_time", time));
            // for the operation before the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final String position    = getLuceneTimeValue(ti.getDate());
                luceneRequest.append("AND (");

                // the single and multpile observations which begin after the bound
                luceneRequest.append("(sampling_time_begin: [19700000000000 TO ").append(position).append("]))");

            } else {
                throw new ObservationStoreException("TM_Before operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (type == TemporalOperatorName.AFTER) {
            eventTimes.add(new TimeAfterType("result_time", time));
            // for the operation after the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final String position    = getLuceneTimeValue(ti.getDate());
                luceneRequest.append("AND (");

                // the single and multpile observations which begin after the bound
                luceneRequest.append("(sampling_time_begin:[").append(position).append(" TO 30000000])");
                luceneRequest.append(OR_OPERATOR);
                // the multiple observations overlapping the bound
                luceneRequest.append("(sampling_time_begin: [19700000 TO ").append(position).append("] AND sampling_time_end:[").append(position).append(" TO 30000000]))");


            } else {
                throw new ObservationStoreException("TM_After operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (type == TemporalOperatorName.DURING) {
            eventTimes.add(new TimeDuringType("result_time", time));
            if (time instanceof Period tp) {
                final String begin      = getLuceneTimeValue(tp.getBeginning().getDate());
                final String end        = getLuceneTimeValue(tp.getEnding().getDate());
                luceneRequest.append("AND (");

                // the multiple observations included in the period
                luceneRequest.append(" (sampling_time_begin:[").append(begin).append(" TO 30000000] AND sampling_time_end:[19700000 TO ").append(end).append("])");
                luceneRequest.append(OR_OPERATOR);
                // the single observations included in the period
                luceneRequest.append(" (sampling_time_begin:[").append(begin).append(" TO 30000000] AND sampling_time_begin:[19700000 TO ").append(end).append("] AND sampling_time_end IS NULL)");
                luceneRequest.append(OR_OPERATOR);
                // the multiple observations which overlaps the first bound
                luceneRequest.append(" (sampling_time_begin:[19700000 TO ").append(begin).append("] AND sampling_time_end:[19700000 TO ").append(end).append("] AND sampling_time_end:[").append(begin).append(" TO 30000000])");
                luceneRequest.append(OR_OPERATOR);
                // the multiple observations which overlaps the second bound
                luceneRequest.append(" (sampling_time_begin:[").append(begin).append(" TO 30000000] AND sampling_time_end:[").append(end).append(" TO 30000000] AND sampling_time_begin:[19700000 TO ").append(end).append("])");
                luceneRequest.append(OR_OPERATOR);
                // the multiple observations which overlaps the whole period
                luceneRequest.append(" (sampling_time_begin:[19700000 TO ").append(begin).append("] AND sampling_time_end:[").append(end).append(" TO 30000000]))");


            } else {
                throw new ObservationStoreException("TM_During operation require TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else {
            throw new ObservationStoreException("This operation is not take in charge by the Web Service, supported one are: TM_Equals, TM_After, TM_Before, TM_During");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOfferings(final List<String> offerings) throws DataStoreException {
        if (offerings != null && !offerings.isEmpty()) {
            luceneRequest.append(" ( ");
            for (String s : offerings) {
                luceneRequest.append(" offering:\"").append(s).append("\" OR ");
            }
            luceneRequest.delete(luceneRequest.length() - 3, luceneRequest.length());
            luceneRequest.append(") ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationResult> filterResult() throws DataStoreException {
        try {
            final SpatialQuery query = new SpatialQuery(luceneRequest.toString());
            final SortField sf       = new SortField("sampling_time_begin_sort", SortField.Type.STRING, false);
            query.setSort(new Sort(sf));
            return searcher.doResultSearch(query);
        } catch(SearchingException ex) {
            throw new DataStoreException("Search exception while filtering the observation", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getIdentifiers() throws DataStoreException {
        try {
            Set<String> results = searcher.doSearch(new SpatialQuery(luceneRequest.toString()));
            // order results
            List<String> tmp = new ArrayList<>(results);
            Collections.sort(tmp);
            return new LinkedHashSet<>(tmp);
        } catch(SearchingException ex) {
            throw new DataStoreException("Search exception while filtering the observation", ex);
        }
    }

    @Override
    public long getCount() throws DataStoreException {
        if (objectType == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        // TODO optimize
        switch (objectType) {
            case FEATURE_OF_INTEREST:
            case OBSERVED_PROPERTY:
            case PROCEDURE:
            case OFFERING:            
            case OBSERVATION:         return getIdentifiers().size();
            case RESULT:              return filterResult().size();
            case HISTORICAL_LOCATION:
            case LOCATION:            throw new DataStoreException("not implemented yet.");
        }
        throw new DataStoreException("initialisation of the filter missing.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBoundingBox(Envelope e) throws DataStoreException {
        throw new DataStoreException("SetBoundingBox is not supported by this ObservationFilter implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResultFilter(final BinaryComparisonOperator filter) throws DataStoreException {
        throw new DataStoreException("setResultFilter is not supported by this ObservationFilter implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() throws DataStoreException {
        try {
            searcher.refresh();
        } catch (IndexingException ex) {
            throw new DataStoreException("Indexing Exception while refreshing the lucene index", ex);
        }
    }

    @Override
    public void destroy() {
        if (searcher != null) {
            searcher.destroy();
        }
    }

    @Override
    public void setProcedureType(String type) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
