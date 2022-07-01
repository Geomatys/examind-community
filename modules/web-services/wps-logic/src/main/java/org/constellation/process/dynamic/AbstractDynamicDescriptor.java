/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.process.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.lineage.Algorithm;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractDynamicDescriptor implements ProcessDescriptor {

    private static final String DEFAULT_VERSION = "1.0";

    protected static final ParameterBuilder BUILDER = new ParameterBuilder();

    protected final List<ParameterDescriptor> dynamicInput  = new ArrayList<>();
    protected final List<ParameterDescriptor> dynamicOutput = new ArrayList<>();
    private final Identifier id;
    private final InternationalString displayName;
    private final InternationalString _abstract;
    private final String version;

    public AbstractDynamicDescriptor(String name, InternationalString _abstract) {
        this.id = new DerivateIdentifier(name, ExamindDynamicProcessFactory.IDENTIFICATION);
        this.displayName = null;
        this._abstract = _abstract;
        this.version = DEFAULT_VERSION;
    }

    @Override
    public Identifier getIdentifier() {
        return id;
    }

    @Override
    public InternationalString getDisplayName() {
        return displayName;
    }

    @Override
    public InternationalString getProcedureDescription() {
        return _abstract;
    }

    /**
     * Get the process version. By default, return {@code 1.0}.
     *
     * @return The process version. By default {@code 1.0}.
     */
    public String getVersion() {
        return version;
    }

    @Override
    public Collection<? extends Citation> getSoftwareReferences() {
        return Collections.emptySet();
    }

    @Override
    public Collection<? extends Citation> getDocumentations() {
        return Collections.emptySet();
    }

    @Override
    public InternationalString getRunTimeParameters() {
        return null;
    }

    @Override
    public Collection<? extends Algorithm> getAlgorithms() {
        return Collections.emptySet();
    }

    public void addNewInput(ParameterDescriptor input) {
        dynamicInput.add(input);
    }

    public void addNewOutput(ParameterDescriptor input) {
        dynamicOutput.add(input);
    }

    protected static class DerivateIdentifier implements Identifier {

        private final String code;
        private final Identification factoryId;

        public DerivateIdentifier(final String code, final Identification factoryId) {
            ArgumentChecks.ensureNonNull("factoryId", factoryId);
            ArgumentChecks.ensureNonNull("code", code);
            this.code = code;
            this.factoryId = factoryId;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public Citation getAuthority() {
            return factoryId.getCitation();
        }

        @Override
        public String getCodeSpace() {
            return Citations.getIdentifier(getAuthority());
        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public InternationalString getDescription() {
            return null;
        }
    }

    /**
     * @return process authority and name. Also table of process inputs and outputs.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Authority         : ");
        sb.append(id.getAuthority().getTitle().toString()).append("\n");
        sb.append("Code              : ");
        sb.append(id.getCode()).append("\n");
        sb.append("Display name      : ");
        sb.append(displayName).append("\n");
        sb.append("Abstract          : ");
        sb.append(_abstract.toString()).append("\n");
        sb.append(getInputDescriptor().toString());
        sb.append(getOutputDescriptor().toString());
        return sb.toString();
    }
}
