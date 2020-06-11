/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.json.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.metadata.BlockObj;
import org.constellation.dto.metadata.ComponentObj;
import org.constellation.dto.metadata.Field;
import org.constellation.dto.metadata.FieldObj;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.metadata.SuperBlockObj;
import org.constellation.metadata.utils.MetadataFeeder;
import org.constellation.metadata.utils.Utils;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.metadata.Metadata;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class Template {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.json.metadata");

    private final MetadataStandard standard;

    private final RootObj rootObj;

    private final Map<Class<?>, Class<?>> specialized;

    public Template(final MetadataStandard standard,String resourcePath) {
        this.standard = standard;
        this.specialized = AbstractTemplateHandler.DEFAULT_SPECIALIZED;

        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            Path resPath = IOUtilities.getResourceAsPath(resourcePath);
            try (InputStream stream = Files.newInputStream(resPath)) {
                this.rootObj = objectMapper.readValue(stream, RootObj.class);
            }
        } catch (URISyntaxException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Identifier of Template. Usually bean name.
     */
    public abstract String getIdentifier();

    /**
     * Flag Template as default template.
     * Used if more than one template match.
     * @return
     */
    public abstract boolean isDefault();

    /**
     * Check if template match a metadata.
     * Usually check for metadata type or search for a flag into metadata object.
     *
     * @param metadata object
     * @return true if matching, false otherwise
     */
    public abstract boolean matchMetadata(Object metadata);

    /**
     * Check if template match a data type.
     *
     * @param dataType String like "COVERAGE", "VECTOR", ....
     * @return true if matching, false otherwise
     */
    public abstract boolean matchDataType(String dataType);

    /**
     * Build an empty metadata.
     *
     * @return
     */
    public Object emptyMetadata() {
       return new DefaultMetadata();
    }

    /**
     * Return an existing identifier for the metadata Object or {@code null}.
     *
     * @param metadata A metadata Object.
     *
     * @return An identifier.
     */
    public String getMetadataIdentifier(final Object metadata) {
        if (metadata instanceof Metadata) {
            return ((Metadata)metadata).getFileIdentifier();
        }
        return Utils.findIdentifier(metadata);
    }

    /**
     * Set the identifier for the metadata Object.
     *
     * @param identifier The identifier to set.
     * @param metadata A metadata Object.
     *
     */
    public void setMetadataIdentifier(final String identifier, final Object metadata) {
        if (metadata instanceof DefaultMetadata) {
            ((DefaultMetadata)metadata).setFileIdentifier(identifier);
        } else {
            Utils.setIdentifier(identifier, metadata);
        }
    }

    /**
     * Return an existing title for the metadata Object or {@code null}.
     *
     * @param metadata A metadata Object.
     *
     * @return An identifier.
     */
    public String getMetadataTitle(final Object metadata) {
        if (metadata instanceof DefaultMetadata) {
            MetadataFeeder feeder = new MetadataFeeder((DefaultMetadata) metadata);
            return feeder.getTitle();
        }
        return Utils.findTitle(metadata);
    }

    /**
     * Set the title for the metadata Object.
     *
     * @param title The title to set.
     * @param metadata A metadata Object.
     *
     */
    public void setMetadataTitle(final String title, final Object metadata) {
        if (metadata instanceof DefaultMetadata) {
            MetadataFeeder feeder = new MetadataFeeder((DefaultMetadata) metadata);
            feeder.setTitle(title);
        } else {
            Utils.setTitle(title, metadata);
        }
    }

    /**
     * Writes the values of the given metadata object using the template given at construction time.
     *
     * @param metadata The metadata object to write.
     * @param out      Where to write the JSO file.
     * @param prune    {@code true} for omitting empty nodes.
     * @param overwrite {@code true} for overwriting read-only nodes.
     *
     * @throws IOException if an error occurred while writing to {@code out}.
     */
    public void write(Object metadata, final Writer out, final boolean prune, final boolean overwrite) throws IOException {
        if (metadata == null) {
            metadata = emptyMetadata();
        }
        final TemplateWriter writer     = new TemplateWriter(standard);
        final RootObj rootFilled        = writer.writeTemplate(rootObj, metadata, prune, overwrite);
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(out, rootFilled);
    }

    /**
     * Parses the given JSON object and write the metadata values in the given metadata object.
     *
     * @param  json        Lines of the JSON file to parse.
     * @param  destination Where to store the metadata values.
     * @param  skipNulls   {@code true} for skipping {@code null} values instead than storing null in the metadata object.
     * @throws IOException if an error occurred while parsing.
     */
    public void read(final RootObj json, final Object destination, final boolean skipNulls) throws IOException {
        TemplateReader reader = new TemplateReader(standard, specialized);
        //fix missing node types unsent by the UI
        json.setNodeTypes(rootObj.getNodeTypes());
        reader.readTemplate(json, destination);
    }

    /**
     * Return both the MD completion (percentage of completion) and the completion level (ELEMENTARY, EXTENDED, COMPLETE or NONE)
     * calculated on the specified metadata object.
     *
     * This method is an optimization by combinating getCompletion(...) and calculateMDCompletion (...) to avoid the multiple transformation
     * of Object to RootObj
     *
     * @param metadata a metadata object
     *
     * @return An {@link Entry} with the MD completion as the key and the completion level as value.
     * @throws IOException
     */
    public Entry<Integer, String> getFullCompletion(final Object metadata) throws IOException {
        final TemplateWriter writer = new TemplateWriter(standard);
        final RootObj rootFilled    = writer.writeTemplate(rootObj, metadata, false, false);
        return new AbstractMap.SimpleEntry<>(calculateMDCompletion(rootFilled), getCompletion(rootFilled));
    }

    /**
     * Return the completion level (ELEMENTARY, EXTENDED or COMPLETE) calculated on the specified metadata object.
     *
     * @param metadata a metadata object
     *
     * @return ELEMENTARY, EXTENDED, COMPLETE or NONE
     * @throws IOException
     */
    public String getCompletion(final Object metadata) throws IOException {
        final TemplateWriter writer = new TemplateWriter(standard);
        final RootObj rootFilled    = writer.writeTemplate(rootObj, metadata, false, false);
        return getCompletion(rootFilled);
    }

    /**
     * Return the completion level (ELEMENTARY, EXTENDED or COMPLETE) calculated on the specified {@link RootObj} object.
     *
     * @param metadata a {@link RootObj} object.
     *
     * @return ELEMENTARY, EXTENDED, COMPLETE or NONE
     * @throws IOException
     */
    private String getCompletion(final RootObj metadataValues) {
        Map<String, Boolean> completions = new HashMap<>();
        completions.put("ELEMENTARY", Boolean.TRUE);
        completions.put("EXTENDED",   Boolean.TRUE);
        completions.put("COMPLETE",   Boolean.TRUE);

        final List<SuperBlockObj> superblocks = metadataValues.getRoot().getChildren();
        for(final SuperBlockObj sb:superblocks){
            final List<BlockObj> blocks = sb.getSuperblock().getChildren();
            for(final BlockObj b:blocks){
                final List<ComponentObj> fields = b.getBlock().getChildren();
                for(final ComponentObj f:fields){
                    final Field field = ((FieldObj)f).getField();
                    final String value = field.value;
                    final String completion = field.getCompletion();
                    if (completion != null) {
                        if (value == null || value.isEmpty()) {
                            if (completions.containsKey(completion)) {
                                completions.put(completion, false);
                            } else {
                                LOGGER.log(Level.WARNING, "unrecognized completion:{0}", completion);
                            }
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "No completion for field: {0}", field.getName());
                    }

                }
            }
        }
        if (completions.get("ELEMENTARY")) {
            if (completions.get("EXTENDED")) {
                if (completions.get("COMPLETE")) {
                    return "COMPLETE";
                }
                return "EXTENDED";
            }
            return "ELEMENTARY";
        }
        return "NONE";
    }

    /**
     * Return the MD completion (percentage of completion) calculated on the specified metadata object.
     *
     * @param metadata a metadata object
     *
     * @return The percentage of completion.
     * @throws IOException
     */
    public int calculateMDCompletion(final Object metadata) throws IOException {
        final TemplateWriter writer = new TemplateWriter(standard);
        final RootObj rootFilled    = writer.writeTemplate(rootObj, metadata, false, false);
        return calculateMDCompletion(rootFilled);
    }

    /**
     * Return the MD completion (percentage of completion) calculated on the specified {@link RootObj} object.
     *
     * @param metadata a {@link RootObj} object.
     *
     * @return The percentage of completion.
     * @throws IOException
     */
    private int calculateMDCompletion(final RootObj metadataValues) {
        int result = 0;
        int fieldsCount=0;
        int fieldValueCount=0;
        final List<SuperBlockObj> superblocks = metadataValues.getRoot().getChildren();
        for(final SuperBlockObj sb:superblocks){
            final List<BlockObj> blocks = sb.getSuperblock().getChildren();
            for(final BlockObj b:blocks){
                final List<ComponentObj> fields = b.getBlock().getChildren();
                for(final ComponentObj f:fields){
                    fieldsCount++;
                    final String value = ((FieldObj)f).getField().value;
                    if(value != null && !value.isEmpty()){
                        fieldValueCount++;
                    }

                }
            }
        }
        if (fieldsCount > 0) {
            result = Math.round(fieldValueCount*100/fieldsCount);
        }
        return result;
    }
}
