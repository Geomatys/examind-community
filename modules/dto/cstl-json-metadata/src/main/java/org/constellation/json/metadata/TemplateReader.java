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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.measure.Unit;
import org.apache.sis.xml.bind.gco.Multiplicity;
import org.apache.sis.xml.bind.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.metadata.AbstractMetadata;
import org.apache.sis.metadata.KeyNamePolicy;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.TypeValuePolicy;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Locales;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.Types;
import org.apache.sis.xml.NilReason;
import org.constellation.dto.metadata.RootObj;
import org.constellation.util.ReflectionUtilities;
import org.geotoolkit.gml.xml.AbstractTimePosition;
import org.geotoolkit.gml.xml.v311.TimeInstantType;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.gml.xml.v311.TimePositionType;
import org.geotoolkit.gts.xml.PeriodDurationType;
import org.geotoolkit.metadata.MetadataFactory;
import org.geotoolkit.sml.xml.v101.SensorMLStandard;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.util.LocalName;
import org.opengis.util.TypeName;
import org.opengis.util.ScopedName;

/**
 * RootObj to Metadata Object
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TemplateReader extends AbstractTemplateHandler {

    private static final MetadataFactory
            DEFAULT   = new MetadataFactory(),
            SYSTEM    = new MetadataFactory(SensorMLStandard.SYSTEM),
            COMPONENT = new MetadataFactory(SensorMLStandard.COMPONENT);

    /**
     * The metadata factory to use for creating new instances.
     */
    private final MetadataFactory factory;


    public TemplateReader(final MetadataStandard standard) {
        this(standard, DEFAULT_SPECIALIZED);
    }

    public TemplateReader(final MetadataStandard standard, Map<Class<?>, Class<?>> specialized) {
        super(standard, specialized);
        if (standard == SensorMLStandard.SYSTEM) {
            factory = SYSTEM;
        } else if (standard == SensorMLStandard.COMPONENT) {
            factory = COMPONENT;
        } else if (standard == MetadataStandard.ISO_19115){
            factory = DEFAULT;
        } else {
            factory = new MetadataFactory(standard, MetadataStandard.ISO_19115);
        }
    }

    public Object readTemplate(final RootObj template, final Object metadata) throws ParseException {

        final TemplateTree tree  = TemplateTree.getTreeFromRootObj(template);

        updateObjectFromTree(tree,  tree.getRoot(), metadata, new ReservedObjects());

        if (metadata instanceof AbstractMetadata am) {
            am.prune();
        }
        return metadata;
    }

    private void updateObjectFromTree(TemplateTree tree, final ValueNode root, final Object metadata, ReservedObjects reserved) throws ParseException {
        reOrder(root, metadata);
        final List<ValueNode> children = new ArrayList<>(root.children);
        for (ValueNode node : children) {

            final Object obj = getValue(node, metadata, reserved);

            if (obj instanceof Collection list) {

                // remove disapeared collection instance
                strip(children, list, node);

                // treat child object
                Object child = get(list, node.ordinal);
                if (child != null) {
                    if (node.isField()) {
                        replace(list, node.ordinal, convert(node.name, child.getClass(), node.value));
                    } else {
                        updateObjectFromTree(tree, node, child, reserved);
                    }
                } else {
                    if (node.isField()) {
                        if (!hadAnotherSimilarField(tree, node)) {
                            putValue(node, metadata);
                        }
                    } else {
                        Object newValue = buildNewInstance(metadata, node);
                        putValue(node, metadata, newValue);
                        updateObjectFromTree(tree, node, newValue, reserved);
                    }
                }

            } else if (obj != null){
                if (node.isField()) {
                    putValue(node, metadata); // replace
                } else {
                    updateObjectFromTree(tree, node, obj, reserved);
                }
            } else {
                if (node.isField()) {
                    putValue(node, metadata);
                } else {
                    Object newValue = buildNewInstance(metadata, node);
                    // if new Value is null, an unexpected type may has been found
                    if (newValue != null) {
                        putValue(node, metadata, newValue);
                        updateObjectFromTree(tree, node, newValue, reserved);
                    }
                }
            }
        }
    }


    private Object getValue(final ValueNode node, Object metadata, ReservedObjects reserved) throws ParseException {
        if (metadata == null) {
            return null;
        }
        // special types
        if (metadata instanceof ReferenceSystemMetadata || metadata instanceof Period || metadata instanceof AbstractTimePosition
                || metadata instanceof Instant || metadata instanceof org.opengis.temporal.Instant || !(metadata instanceof AbstractMetadata))
        {
            final Method getter = ReflectionUtilities.getGetterFromName(node.name, metadata.getClass());
            return ReflectionUtilities.invokeMethod(metadata, getter);

        } else {
            Object result = fixImmutable(asMap(metadata).get(node.name));

            /*
             * if the node is strict we verify that the values correspound to the sub nodes.
             * For a collection, we return a sub-collection with only the matching instance
             */
            if (node.strict){

                if (result instanceof Collection c) {
                    final NumeratedCollection results = new NumeratedCollection(c);
                    final Iterator it        = c.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        final Object o = it.next();
                        if (!reserved.isReserved(node, o) && objectMatchStrictNode(o, node, reserved)) {
                            reserved.reserve(node, o);
                            results.put(i, o);
                        }
                        i++;
                    }
                    return results;

                } else if (!reserved.isReserved(node, result) && objectMatchStrictNode(result, node, reserved)) {
                    reserved.reserve(node, result);
                    return result;
                }
                return null;

            /*
             * if the node has a type we verify that the values correspound to the declared type.
             * For a collection, we return a sub-collection with only the matching instance
             */
            } else if (node.type != null) {
                final Class type = readType(node);
                if (result instanceof Collection c) {
                    final NumeratedCollection results = new NumeratedCollection(c);
                    final Iterator it        = c.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        final Object o = it.next();
                        if (!reserved.isReserved(node, o) && type.isInstance(o)) {
                            reserved.reserve(node, o);
                            results.put(i, o);
                        }
                        i++;
                    }
                    return results;
                } else if (!reserved.isReserved(node, result) && type.isInstance(result)) {
                    reserved.reserve(node, result);
                    return result;
                }
                return null;

            } else {
                if (result instanceof Collection c) {
                    final NumeratedCollection results = new NumeratedCollection(c);
                    final Iterator it        = c.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        final Object o = it.next();
                        if (!reserved.isReserved(node, o)) {
                            reserved.reserve(node, o);
                            results.put(i, o);
                        }
                        i++;
                    }
                    return results;
                } else if (!reserved.isReserved(node, result)) {
                    reserved.reserve(node, result);
                    return result;
                }
                return null;
            }
        }
    }

    private boolean hadAnotherSimilarField(TemplateTree tree, ValueNode n) throws ParseException {
        if (n.value != null) return false;

        final List<ValueNode> nodes = tree.getNodesByPath(n.path);
        for (ValueNode node : nodes) {
            if (node.blockName != null && !node.blockName.equals(n.blockName)) {
                return true;
            }
        }
        return false;
    }

    private boolean objectMatchStrictNode(final Object obj, ValueNode n, ReservedObjects reserved) throws ParseException {
        if (n.type != null) {
            Class type = readType(n);
            if (!type.isInstance(obj)) return false;
        }

        if (n.isField() && n.render != null && n.render.contains("readonly") && n.defaultValue != null) {
            if (obj == null) return false;
            Class type      = obj.getClass();
            Object defValue = convert(n.name, type, n.defaultValue);
            if (!Objects.equals(obj, defValue)) {
                return false;
            }
        } else if (n.isField() && !n.getPredefinedValues().isEmpty()) {
            if (obj == null) return false;
            Class type = obj.getClass();
            final List<Object> predefinedValues = new ArrayList<>();
            for (String predefinedValue : n.getPredefinedValues()) {
                predefinedValues.add(convert(n.name, type, predefinedValue));
            }
            if (!predefinedValues.contains(obj)) {
                return false;
            }
        }

        for (ValueNode child : n.children) {
            final Object childO = getValue(child, obj, new ReservedObjects());
            final Collection c;
            if (childO instanceof Collection cc) {
                c = cc;
            } else {
                c = Arrays.asList(childO);
            }
            for (Object o : c) {
                if (!objectMatchStrictNode(o, child, reserved)) {
                    return false;
                }
            }

        }
        return true;
    }

    private void putValue(final ValueNode node, final Object metadata) throws ParseException {
        putValue(node, metadata, node.value);
    }

    private void putValue(final ValueNode node, final Object metadata, Object value) throws ParseException {
        Class type = getType(metadata, node);
        if (type != null) {
            value      = convert(node.name, type, value);
            //special case for Multiplicity clase
            if (metadata instanceof Multiplicity && node.name.equals("range")) {
                try {
                    ((Multiplicity)metadata).range().add((NumberRange<Integer>) value);
                } catch (ClassCastException ex) {
                    LOGGER.warning("bad type for multiplicty range attribute");
                }
            } else  if (metadata instanceof ImmutableIdentifier || metadata instanceof NumberRange){
                Field f = ReflectionUtilities.getFieldFromName(node.name, metadata.getClass());
                if (f != null) {
                    try {
                        f.setAccessible(true);
                        f.set(metadata, value);
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOGGER.log(Level.WARNING, "Erro while setting value to " + node.name + " field in class " + metadata.getClass().getName(), ex);
                    }
                } else {
                    LOGGER.warning("Unable to find a field for:" + node.name + " in " + metadata.getClass().getName());
                }
            } else if (type == ReferenceSystem.class ||
               (metadata instanceof ReferenceSystem) ||
               (metadata instanceof Period) ||
               (metadata instanceof AbstractTimePosition) ||
               (metadata instanceof Instant) ||
               (!(metadata instanceof AbstractMetadata))) {

                if (value != null) {
                    String attributeName;
                    // special case
                    if (node.name.equals("referenceSystemIdentifier") && metadata instanceof ReferenceSystem) {
                        attributeName = "name";
                    // TODO remove when the setter "setPosition" will be added to geotk AbstractTimePosition
                    } else if (node.name.equals("position") && metadata instanceof AbstractTimePosition) {
                        attributeName = "value";
                        value = value.toString();

                    // temporal object from sis are immutable
                    } else if (node.name.equals("position") && !(metadata instanceof AbstractTimePosition)) {
                        Field f = ReflectionUtilities.getFieldFromName(node.name, metadata.getClass());
                        try {
                            f.setAccessible(true);
                            f.set(metadata,  value);
                        } catch (Exception ex) {
                            LOGGER.warning("Unable to set position field");
                        }
                        return;
                    } else {
                        attributeName = node.name;
                    }
                    final Method setter = ReflectionUtilities.getSetterFromName(attributeName, value.getClass(), metadata.getClass());
                    if (setter != null) {
                        if (setter.getParameterTypes()[0] == Collection.class) {
                            value = Arrays.asList(value);
                        }
                        ReflectionUtilities.invokeMethod(setter, metadata, value);
                    } else {
                        LOGGER.warning("Unable to find a setter for:" + attributeName + "with type " +  value.getClass().getName() + " in " + metadata.getClass().getName());
                    }
                } else {
                    LOGGER.warning("TODO find a setter for null values");
                }
            } else {
                final Map<String,Object> values = asMap(metadata);
                try {
                    values.put(node.name, value);

                // better msg send to the user in case of a bad unit code
                } catch (ClassCastException ex) {
                    if (type.equals(Unit.class)) {
                        throw new ParseException("Impossible de trouvé une unité de mesure correspondant au code:" + value, ex);
                    }
                    throw ex;
                }
            }
        }
    }

    /**
     * Returns the type of values for the given property in the given metadata.
     */
    private Class<?> getType(final Object metadata, final ValueNode node) throws ParseException {
        if (node.type != null) {
            return readType(node);
        }
        Class type;
        if (metadata instanceof ReferenceSystemMetadata || metadata instanceof Period || metadata instanceof AbstractTimePosition || metadata instanceof Instant || !(metadata instanceof AbstractMetadata)) {
            final Method getter = ReflectionUtilities.getGetterFromName(node.name, metadata.getClass());
            if (getter != null) {
                type = getter.getReturnType();

                // if the method return a collection, return the parameterized class
                if (Collection.class.isAssignableFrom(type)) {
                    type = Classes.boundOfParameterizedProperty(getter);
                }
            } else {
                return null;
            }
        } else {
            type = standard.asTypeMap(metadata.getClass(), KeyNamePolicy.UML_IDENTIFIER, TypeValuePolicy.ELEMENT_TYPE).get(node.name);
        }
        final Class<?> special = specialized.get(type);
        if (special != null) {
            return special;
        }
        return type;
    }

    private Object buildNewInstance(final Object metadata, final ValueNode node) throws ParseException {
        try {
            Class type = getType(metadata, node);
            // special case
            if (type == ReferenceSystem.class) {
                return new ReferenceSystemMetadata();
            } else if (type == TemporalPrimitive.class || type == TimePeriodType.class) {
                return new TimePeriodType();
            } else if (type == TimePositionType.class) {
                return new TimePositionType();
            } else if (type == TimeInstantType.class || type == Instant.class) {
                return new TimeInstantType();
            } else if (type == Multiplicity.class) {
                return new Multiplicity();
            }else if (type == NumberRange.class) {
                return NumberRange.create(-1, true, 1, true);
            } else if (type != null) {
                return factory.create(type, Collections.<String,Object>emptyMap());
            } else {
                LOGGER.log(Level.INFO, "no type find for attribute:{0} in object:{1}", new Object[]{node.name, metadata.getClass().getName()});
                return null;
            }
        } catch (FactoryException ex) {
            throw new ParseException("Error while building empty instance from factory:" + ex.getMessage());
        }
    }

    /**
     * Converts the given value to an instance of the given class before to store in the metadata object.
     */
    private Object convert(final String identifier, final Class<?> type, Object value) throws ParseException {
        if (type == Date.class) {
            return toDate(value, identifier);
        }
        if (type == Instant.class || type == Temporal.class) {
            return toInstant(value, identifier);
        }
        if ((type == double.class || type == Double.class) && value instanceof String) {
            return Double.valueOf((String) value);
        }
        if (type == LocalName.class && value instanceof String) {
            return Names.createLocalName(null, null, (String) value);
        }
        if (type == TypeName.class && value instanceof String) {
            return Names.createTypeName(null, null, (String) value);
        }
        if (type == ScopedName.class && value instanceof String) {
            return Names.createScopedName(null, null, (String) value);
        }
        if ((type == int.class ||type == Integer.class) && value instanceof String) {
            return Integer.valueOf((String) value);
        }
        if ((type == long.class || type == Long.class) && value instanceof String) {
            return Long.valueOf((String) value);
        }
        if (type == java.time.Period.class && value instanceof String) {
            try {
                return new PeriodDurationType((String) value);
            } catch (IllegalArgumentException ex) {
                // TODO: "log and continue" is not appropriate here, since the user can not know that his data is lost.
                LOGGER.log(Level.WARNING, "Bad period duration value: {0} (property: {1})",
                        new Object[] {value, identifier});
                return null;
            }
        }
        if (InternationalString.class.isAssignableFrom(type) && value instanceof String) {
            return new SimpleInternationalString(value.toString());
        }
        if (Charset.class.isAssignableFrom(type) && value instanceof String) {
            return Charset.forName(value.toString());
        }
        if (Locale.class.isAssignableFrom(type) && value instanceof String) {
            String text = value.toString();
            text = text.substring(text.indexOf('.') + 1).trim();
            return Locales.parse(text);
        }
        if (!CharSequence.class.isAssignableFrom(type) && (value instanceof CharSequence)) {
            String text = value.toString();
            if (type == URI.class) {
                text = text.replace("\\", "%5C");
                text = text.replace(" ", "%20");
                return URI.create(text);
            } else if (text.startsWith("nilReason:")) {
                NilReason nr;
                try {
                    nr = NilReason.valueOf(text.substring("nilReason:".length()));
                } catch (URISyntaxException | IllegalArgumentException e) {
                    throw new ParseException("Illegal value: \"" + text + "\".(property:" + identifier + ")", e);
                }
                try {
                    value = nr.createNilObject(type);
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Illegal type for nill object: \"" + type.getName() + "\".(property:" + identifier + ")");
                    value = null;
                }
            } else {
                final boolean isCodeList = CodeList.class.isAssignableFrom(type);
                if (isCodeList || type == Locale.class || type == Charset.class || type.isEnum()) {
                    text = text.substring(text.indexOf('.') + 1).trim();
                    if (isCodeList) {
                        value = Types.forCodeName(type.asSubclass(CodeList.class), text, false);
                    } else if (type.isEnum()){
                        value = Types.forEnumName(type.asSubclass(Enum.class), text);
                    } else {
                        value = text;
                    }
                }
            }
        }
        return value;
    }

    private static Object fixImmutable(final Object obj) {
        if (obj instanceof Collection c) {
            final Iterator it = c.iterator();
            while(it.hasNext()) {
                Object o = it.next();
                if (o instanceof ReferenceSystemMetadata rsm) {
                    fixImmutableRs(rsm);
                }
            }
        }
        if (obj instanceof ReferenceSystemMetadata rsm) {
            fixImmutableRs(rsm);
        }
        return obj;
    }

    private static void fixImmutableRs(final ReferenceSystemMetadata rs) {
        if (rs.getName() instanceof ImmutableIdentifier) {
            final DefaultIdentifier newID = new DefaultIdentifier(rs.getName());
            rs.setName(newID);
        }
    }

    /**
     * Returns the given value as a date.
     */
    private Date toDate(final Object value, final String identifier) throws ParseException {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return new Date(n.longValue());
        }
        final String t = (String) value;
        if (t.indexOf('-') < 0) try {
            return new Date(Long.parseLong(t));
        } catch (NumberFormatException e) {
            throw new ParseException("Illegal date: " + value + " (property:" + identifier +")", e);
        }
        try {
            if (t.indexOf(':') < 0) {
                synchronized (dateFormat) {
                    return dateFormat.parse((String) value);
                }
            } else {
                synchronized (dateHourFormat) {
                    return dateHourFormat.parse((String) value);
                }
            }
        } catch (java.text.ParseException e) {
            throw new ParseException("Illegal date: " + value + " (property:" + identifier +")", e);
        }
    }
    
    private Temporal toInstant(final Object value, final String identifier) throws ParseException {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue());
        }
        final String t = (String) value;
        if (t.indexOf('-') < 0) try {
            return Instant.ofEpochMilli(Long.parseLong(t));
        } catch (NumberFormatException e) {
            throw new ParseException("Illegal date: " + value + " (property:" + identifier +")", e);
        }
        if (t.indexOf(':') < 0) {
            return LocalDate.parse((String) value).atStartOfDay().atOffset(ZoneOffset.UTC).toInstant();
        } else {
            try {
                return instantHourFormat.parse((String) value, Instant::from);
            } catch (DateTimeParseException e) {
                try {
                    return instantHourFormat2.parse((String) value, Instant::from);
                }  catch (DateTimeParseException e2) {
                    throw new ParseException(e2.getMessage(), e2);
                }
            }
        }
    }

    private Object get(Collection c , int ordinal) {
        if (c instanceof NumeratedCollection nc) {
            return nc.get(ordinal);
        }
        if (c.size() > ordinal) {
            final Iterator it = c.iterator();
            for (int i = 0; i < c.size(); i++) {
                Object o = it.next();
                if (i == ordinal) {
                    return o;
                }
            }
        }
        return null;
    }

    private void replace(Collection c , int ordinal, Object newValue) {
        if (c instanceof List list) {
            list.set(ordinal, newValue);
        } else if (c instanceof NumeratedCollection nc){
            NumeratedCollection list = nc;
            list.replace(ordinal, newValue);
        } else {
            final Iterator it = c.iterator();
            Object old = it.next();
            for (int i = 0; i < ordinal; i++) {
                old = it.next();
            }
            c.remove(old);
            c.add(newValue);
        }
    }

    private void reOrder(ValueNode root, Object metadata) throws ParseException {
        Map<String, List<ValueNode>> map = new HashMap<>();
        ReservedObjects reserved = new ReservedObjects();

        // split the nodes which may cause trouble by their path
        for (ValueNode child : root.children) {
            if (!child.isField() && (child.strict || child.type != null) && child.blockName != null) {
                if (map.containsKey(child.path)) {
                    boolean add = true;
                    List<ValueNode> nodes = map.get(child.path);
                    for (ValueNode node : nodes) {
                        if (node.blockName.equals(child.blockName)) {
                            add = false;
                        }
                    }
                    if (add) {
                        map.get(child.path).add(child);
                    }
                } else {
                    List<ValueNode> l = new ArrayList<>();
                    l.add(child);
                    map.put(child.path, l);
                }
            }
        }

        for (String path : map.keySet()) {
            List<ValueNode> nodes = map.get(path);
            if (nodes.size() > 1) {
                Collections.sort(nodes, new NodeOrdinalComparator());
                final Collection c = new ArrayList<>();
                for (ValueNode node : nodes) {
                    NumeratedCollection coll = (NumeratedCollection) getValue(node, metadata, reserved);
                    if (coll != null) {
                        Iterator it = coll.iterator();
                        while (it.hasNext()) {
                            c.add(it.next());
                        }
                    }

                }
                asMap(metadata).put(nodes.get(0).name, c);
            }

        }
    }

    private static void strip(final List<ValueNode> nodes, Collection c, ValueNode current) {
        // remove only node that are multiple in the forms (ie only the one wich can be remove by the user)
        if (current.blockName != null || current.isField()) {

            int cpt = 0;
            for (ValueNode node : nodes) {
                if (Objects.equals(node.path,      current.path) &&
                    Objects.equals(node.blockName, current.blockName)) {
                    cpt++;
                }
            }
            if (c.size() > cpt) {
                final List toRemove = new ArrayList<>();
                final Iterator it = c.iterator();
                int i = 0;
                while (it.hasNext()) {
                    Object o = it.next();
                    if (i >= cpt) {
                        toRemove.add(o);
                    }
                    i++;
                }
                c.removeAll(toRemove);
            }
        }
    }

    private static class ReservedObjects {

        private final Map<String, Map<String, List<Object>>> objects = new HashMap<>();

        public boolean isReserved(final ValueNode node, final Object obj) {
            if (node.blockName != null) {
                final Map<String, List<Object>> map = objects.get(node.path);
                if (map != null) {
                    for (String blockName : map.keySet()) {
                        if (!blockName.equals(node.blockName) && map.get(blockName).contains(obj)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public void reserve(final ValueNode node, final Object obj) {
            if (node.blockName != null) {
                if (objects.containsKey(node.path) && objects.get(node.path).containsKey(node.blockName)) {
                    objects.get(node.path).get(node.blockName).add(obj);

                } else if (objects.containsKey(node.path)) {
                    final List<Object> set = new ArrayList<>();
                    set.add(obj);
                    objects.get(node.path).put(node.blockName, set);
                } else {
                    final List<Object> set = new ArrayList<>();
                    set.add(obj);
                    Map<String, List<Object>> map = new HashMap<>();
                    map.put(node.blockName, set);
                    objects.put(node.path, map);
                }
            }
        }
    }

    private static class NodeOrdinalComparator implements Comparator<ValueNode> {

        @Override
        public int compare(ValueNode o1, ValueNode o2) {
            return Integer.compare(o1.ordinal, o2.ordinal);
        }



    }
}
