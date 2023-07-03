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
package org.constellation.metadata.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.xml.bind.JAXBElement;

import org.opengis.metadata.Metadata;
import org.opengis.temporal.Instant;
import org.opengis.util.InternationalString;
import org.opengis.util.LocalName;

import org.apache.sis.internal.metadata.Merger;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.Classes;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.IdentifierSpace;

import org.constellation.util.NodeUtilities;
import org.constellation.util.ReflectionUtilities;
import org.constellation.util.Util;
import org.w3c.dom.Node;

/**
 * Utility methods used in CSW object.
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class Utils {

    /**
     * A debugging logger.
     */
    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.Utils");

    /**
     * A string constant used when we don't find a title on an object.
     */
    public static final String UNKNOW_TITLE = "unknow title";

    /**
     * A string constant used when we don't find an identifier on an object.
     */
    public static final String UNKNOW_IDENTIFIER = "unknow_identifier";

    private static final String NULL_VALUE = "null";

    private Utils() {}

    /**
      * This method try to find a title for this object.
      * if the object is a ISO19115:Metadata or CSW:Record we know were to search the title,
      * else we try to find a getName(), getTitle(), or getId() method.
      *
      * This method use path with an old structure (MDweb) and should be changed to use proper XPath
      *
      * @param obj the object for which we want a title.
      *
      * @return the founded title or UNKNOW_TITLE
      */

    public static String findTitle(final Object obj) {

        //here we try to get the title
        String title = UNKNOW_TITLE;

        final List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:identificationInfo:citation:title");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo:citation:title");
        paths.add("ISO 19115:MD_Metadata:fileIdentifier");
        paths.add("ISO 19115-2:MI_Metadata:fileIdentifier");
        paths.add("ISO 19115:CI_ResponsibleParty:individualName");
        paths.add("ISO 19115:CI_ResponsibleParty:organisationName");
        paths.add("ISO 19110:FC_FeatureCatalogue:name");
        paths.add("ISO 19110:FC_FeatureCatalogue:featureType:typeName");
        paths.add("Catalog Web Service:Record:title:content");
        paths.add("Catalog Web Service:Record:identifier:content");
        paths.add("Ebrim v3.0:*:name:localizedString:value");
        paths.add("Ebrim v3.0:*:id");
        paths.add("Ebrim v2.5:*:name:localizedString:value");
        paths.add("Ebrim v2.5:*:id");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("NASA Directory Interchange Format:DIF:entryTitle");
        for (String path : paths) {
            Object value = ReflectionUtilities.getValuesFromPath(path, obj);
            if (value instanceof String && !((String)value).isEmpty()) {
                title = (String) value;
                // we stop when we have found a response
                break;
            } else if (value instanceof InternationalString && !((InternationalString) value).toString().isEmpty()) {
                title = value.toString();
                // we stop when we have found a response
                break;
            } else if (value instanceof Collection) {
                Collection c = (Collection) value;
                Iterator it = c.iterator();
                if (it.hasNext()) {
                    Object cValue = it.next();
                    if (cValue instanceof String) {
                        title = (String) cValue;
                        break;
                    } else if (cValue != null) {
                        title = cValue.toString();
                        break;
                    }
                }
            } else if (value != null) {
                LOGGER.finer("FIND TITLE => unexpected String type: " + value.getClass().getName() + "\ncurrentPath:" + path);
            }
        }
        return title;
    }

    /**
      * This method try to find a standard name for this object.
      * if the object is a ISO19115:Metadata we know where to search,
      *
      * This method use path with an old structure (MDweb) and should be changed to use proper XPath
      *
      * @param obj the object for which we want a title.
      *
      * @return the founded standard name or {@code null}
      */

    public static String findStandardName(final Object obj) {

        //here we try to get the title
        String standardName = null;

        final List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:metadataStandardName");
        paths.add("ISO 19115-2:MI_Metadata:metadataStandardName");
        paths.add("ISO 19115:CI_ResponsibleParty:xLink:href");

        for (String path : paths) {
            Object value = ReflectionUtilities.getValuesFromPath(path, obj);
            if (value instanceof String) {
                standardName = (String) value;
                // we stop when we have found a response
                break;
            } else if (value != null) {
                LOGGER.finer("FIND Standard name => unexpected String type: " + value.getClass().getName() + "\ncurrentPath:" + path);
            }
        }
        return standardName;
    }

    /**
     * This method try to find an identifier for this object.
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param obj the object for which we want a identifier.
     *
     * @return the founded identifier or UNKNOW_IDENTIFIER
     */
    public static String findIdentifier(final Object obj) {

        if (obj instanceof Node) {
            return findIdentifierNode((Node)obj);
        }

        final List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:fileIdentifier");
        paths.add("ISO 19115-2:MI_Metadata:fileIdentifier");
        paths.add("ISO 19115:CI_ResponsibleParty:uuid");
        paths.add("Catalog Web Service:Record:identifier:content");
        paths.add("Ebrim v3.0:*:id");
        paths.add("Ebrim v2.5:*:id");
        paths.add("ISO 19110:FC_FeatureCatalogue:id");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("NASA Directory Interchange Format:DIF:entryID:shortName");

        return findIdentifier(obj, paths);
    }

    /**
     * This method try to find an identifier for this object.
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param obj the object for which we want a identifier.
     * @param paths
     *
     * @return the founded identifier or UNKNOW_IDENTIFIER
     */
    public static String findIdentifier(final Object obj, final List<String> paths) {

        if (obj instanceof Node) {
            return findIdentifierNode((Node)obj, paths);
        }
        String identifier = UNKNOW_IDENTIFIER;

        for (String path : paths) {
            Object value = ReflectionUtilities.getValuesFromPath(path, obj);
            // we stop when we have found a response
            if (value instanceof String && !((String)value).isEmpty()) {
                identifier = (String) value;
                break;
            }
            if (value instanceof InternationalString) {
                identifier = value.toString();
                break;
            }
            if (value instanceof UUID) {
                identifier = value.toString();
                break;
            } else if (value instanceof Collection) {
                Collection c = (Collection) value;
                Iterator it = c.iterator();
                if (it.hasNext()) {
                    Object cValue = it.next();
                    if (cValue instanceof String) {
                        identifier = (String) cValue;
                        break;
                    } else if (cValue != null) {
                        identifier = cValue.toString();
                        break;
                    }
                }
            } else if (value != null) {
                LOGGER.finer("FIND IDENTIFIER => unexpected String type: " + value.getClass().getName() + "\ncurrentPath:" + path);
            }
        }
        return identifier;
    }

    /**
     * This method try to set an identifier for this object.
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param identifier the new identifier to set
     * @param object the object for which we want to set identifier.
     *
     */
    public static void setIdentifier(final String identifier, final Object object) {

        final List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:fileIdentifier");
        paths.add("ISO 19115-2:MI_Metadata:fileIdentifier");
        paths.add("ISO 19115:CI_ResponsibleParty:uuid");
        paths.add("Catalog Web Service:Record:identifier:content");
        paths.add("Ebrim v3.0:*:id");
        paths.add("Ebrim v2.5:*:id");
        paths.add("ISO 19110:FC_FeatureCatalogue:id");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("NASA Directory Interchange Format:DIF:entryID:shortName");

        for (String pathID : paths) {

            if (ReflectionUtilities.pathMatchObjectType(object, pathID)) {
                Object currentObject = object;
                /*
                 * we remove the prefix path part the path always start with STANDARD:TYPE:
                 */
                pathID = pathID.substring(pathID.indexOf(':') + 1);
                pathID = pathID.substring(pathID.indexOf(':') + 1);
                while (!pathID.isEmpty()) {

                    //we extract the current attributeName
                    String attributeName;
                    if (pathID.indexOf(':') != -1) {
                        attributeName = pathID.substring(0, pathID.indexOf(':'));
                        pathID = pathID.substring(pathID.indexOf(':') + 1);
                    } else {
                        attributeName = pathID;
                        pathID = "";
                    }

                    //we extract the specified type if there is one
                    String paramClassName = null;
                    int brackIndex = attributeName.indexOf('[');
                    if (brackIndex != -1) {
                        paramClassName = attributeName.substring(brackIndex + 1, attributeName.length() -1);
                        attributeName = attributeName.substring(0, brackIndex);
                    }

                    if (!pathID.isEmpty()) {
                        Method getter = null;
                        Object temp = null;
                        if (currentObject != null) {
                            // we get the temporary object when navigating through the object.
                            temp = currentObject;
                            getter = ReflectionUtilities.getGetterFromName(attributeName, currentObject.getClass());
                        }

                        if (getter != null) {
                            currentObject = ReflectionUtilities.invokeMethod(currentObject, getter);
                            // if the object is not yet instantiate, we build it
                            if (currentObject == null) {
                                currentObject = ReflectionUtilities.newInstance(getter.getReturnType());
                                // if the build succeed we set it to the global previous object
                                if (currentObject != null) {
                                    Method setter = ReflectionUtilities.getSetterFromName(attributeName, currentObject.getClass(), temp.getClass());
                                    if (setter != null) {
                                        ReflectionUtilities.invokeMethod(setter, temp, currentObject);
                                    }
                                }
                            } else if (currentObject instanceof Collection) {
                                if (((Collection) currentObject).size() > 0) {
                                    currentObject = ((Collection) currentObject).iterator().next();
                                    if (currentObject instanceof JAXBElement) {
                                        currentObject = ((JAXBElement)currentObject).getValue();
                                    }
                                } else {
                                    if (paramClassName == null) {
                                        Class returnType = Classes.boundOfParameterizedProperty(getter);
                                        currentObject = ReflectionUtilities.newInstance(returnType);
                                    } else {
                                        try {
                                            Class paramClass = Class.forName(paramClassName);
                                            currentObject = ReflectionUtilities.newInstance(paramClass);
                                        } catch (ClassNotFoundException ex) {
                                            LOGGER.log(Level.WARNING, "unable to find the class:" + paramClassName, ex);
                                        }
                                    }
                                    // if the build succeed we set it to the global previous object
                                    if (currentObject != null) {
                                        Method setter = ReflectionUtilities.getSetterFromName(attributeName, getter.getReturnType(), temp.getClass());
                                        if (setter != null) {
                                            ReflectionUtilities.invokeMethod(setter, temp, Arrays.asList(currentObject));
                                        }
                                    }
                                }
                            } else if (currentObject instanceof JAXBElement) {
                                currentObject = ((JAXBElement)currentObject).getValue();
                            }
                        }

                    } else {
                        if (currentObject != null) {
                            /*
                             * we use the getter to determinate the parameter class.
                             */
                            Class objClass = currentObject.getClass();
                            Method getter = ReflectionUtilities.getGetterFromName(attributeName,objClass);

                            Class parameterClass;
                            if (getter != null) {
                                parameterClass = getter.getReturnType();
                            } else {
                                parameterClass = String.class;
                            }

                            Method setter = ReflectionUtilities.getSetterFromName(attributeName, parameterClass, objClass);
                            if (setter != null) {
                                // if the parameter is a string collection
                                if (parameterClass.equals(List.class)) {
                                    ReflectionUtilities.invokeMethod(setter, currentObject, Arrays.asList(identifier));
                                } else if (parameterClass.equals(InternationalString.class)) {
                                    ReflectionUtilities.invokeMethod(setter, currentObject, new SimpleInternationalString(identifier));
                                } else {
                                    ReflectionUtilities.invokeMethod(setter, currentObject, identifier);
                                }
                                return;
                            } else if (object instanceof ISOMetadata && "uuid".equals(attributeName)) {
                                ((ISOMetadata)object).getIdentifierMap().putSpecialized(IdentifierSpace.UUID, UUID.fromString(identifier));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method try to set an title for this object.
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param title the new title to set.
     * @param object the object for which we want to set title.
     *
     */
    public static void setTitle(final String title, final Object object) {

        final List<String> paths = new ArrayList<>();
        paths.add("ISO 19115:MD_Metadata:identificationInfo[org.apache.sis.metadata.iso.identification.DefaultDataIdentification]:citation[org.apache.sis.metadata.iso.citation.DefaultCitation]:title");
        paths.add("ISO 19115-2:MI_Metadata:identificationInfo[org.apache.sis.metadata.iso.identification.DefaultDataIdentification]:citation[org.apache.sis.metadata.iso.citation.DefaultCitation]:title");
        //paths.add("ISO 19115:CI_ResponsibleParty:individualName");
        paths.add("ISO 19115:CI_ResponsibleParty:organisationName");
        paths.add("ISO 19110:FC_FeatureCatalogue:name");
        paths.add("Catalog Web Service:Record:title:content");
        paths.add("Ebrim v3.0:*:name:localizedString:value");
        paths.add("Ebrim v2.5:*:name:localizedString:value");
        paths.add("SensorML:SensorML:member:process:id");
        paths.add("NASA Directory Interchange Format:DIF:entryTitle");

        for (String pathID : paths) {

            if (ReflectionUtilities.pathMatchObjectType(object, pathID)) {
                Object currentObject = object;
                /*
                 * we remove the prefix path part the path always start with STANDARD:TYPE:
                 */
                pathID = pathID.substring(pathID.indexOf(':') + 1);
                pathID = pathID.substring(pathID.indexOf(':') + 1);
                while (!pathID.isEmpty()) {

                    //we extract the current attributeName
                    String attributeName;
                    if (pathID.indexOf(':') != -1) {
                        attributeName = pathID.substring(0, pathID.indexOf(':'));
                        pathID = pathID.substring(pathID.indexOf(':') + 1);
                    } else {
                        attributeName = pathID;
                        pathID = "";
                    }

                    //we extract the specified type if there is one
                    String paramClassName = null;
                    int brackIndex = attributeName.indexOf('[');
                    if (brackIndex != -1) {
                        paramClassName = attributeName.substring(brackIndex + 1, attributeName.length() -1);
                        attributeName = attributeName.substring(0, brackIndex);
                    }

                    if (!pathID.isEmpty()) {
                        Object temp = null;
                        Method getter = null;
                        if (currentObject != null) {
                            // we get the temporary object when navigating through the object.
                            temp = currentObject;
                            getter = ReflectionUtilities.getGetterFromName(attributeName, currentObject.getClass());
                        }

                        if (getter != null) {
                            currentObject = ReflectionUtilities.invokeMethod(currentObject, getter);
                            // if the object is not yet instantiate, we build it
                            if (currentObject == null) {
                                if (paramClassName == null) {
                                    currentObject = ReflectionUtilities.newInstance(getter.getReturnType());
                                } else {
                                    try {
                                        Class paramClass = Class.forName(paramClassName);
                                        currentObject = ReflectionUtilities.newInstance(paramClass);
                                    } catch (ClassNotFoundException ex) {
                                        LOGGER.log(Level.WARNING, "unable to find the class:" + paramClassName, ex);
                                    }
                                }
                                // if the build succeed we set it to the global previous object
                                if (currentObject != null) {
                                    Method setter = ReflectionUtilities.getSetterFromName(attributeName, currentObject.getClass(), temp.getClass());
                                    if (setter != null) {
                                        ReflectionUtilities.invokeMethod(setter, temp, currentObject);
                                    }
                                }
                            } else if (currentObject instanceof Collection) {
                                if (((Collection) currentObject).size() > 0) {
                                    currentObject = ((Collection) currentObject).iterator().next();
                                    if (currentObject instanceof JAXBElement) {
                                        currentObject = ((JAXBElement)currentObject).getValue();
                                    }
                                } else {
                                    if (paramClassName == null) {
                                        Class returnType = Classes.boundOfParameterizedProperty(getter);
                                        currentObject = ReflectionUtilities.newInstance(returnType);
                                    } else {
                                        try {
                                            Class paramClass = Class.forName(paramClassName);
                                            currentObject = ReflectionUtilities.newInstance(paramClass);
                                        } catch (ClassNotFoundException ex) {
                                            LOGGER.log(Level.WARNING, "unable to find the class:" + paramClassName, ex);
                                        }
                                    }
                                    // if the build succeed we set it to the global previous object
                                    if (currentObject != null) {
                                        Method setter = ReflectionUtilities.getSetterFromName(attributeName, getter.getReturnType(), temp.getClass());
                                        if (setter != null) {
                                            ReflectionUtilities.invokeMethod(setter, temp, Arrays.asList(currentObject));
                                        }
                                    }
                                }
                            } else if (currentObject instanceof JAXBElement) {
                                currentObject = ((JAXBElement)currentObject).getValue();
                            }
                        }

                    } else {
                        if (currentObject != null) {
                            /*
                             * we use the getter to determinate the parameter class.
                             */
                            Method getter = ReflectionUtilities.getGetterFromName(attributeName, currentObject.getClass());
                            Class parameterClass;
                            if (getter != null) {
                                parameterClass = getter.getReturnType();
                            } else {
                                parameterClass = String.class;
                            }
                            Method setter = ReflectionUtilities.getSetterFromName(attributeName, parameterClass, currentObject.getClass());
                            if (setter != null) {
                                // if the parameter is a string collection
                                if (parameterClass.equals(List.class)) {
                                    ReflectionUtilities.invokeMethod(setter, currentObject, Arrays.asList(title));
                                } else if (parameterClass.equals(InternationalString.class)){
                                    ReflectionUtilities.invokeMethod(setter, currentObject, new SimpleInternationalString(title));
                                } else {
                                    ReflectionUtilities.invokeMethod(setter, currentObject, title);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }

    }

    public static String findIdentifierNode(final Node node) {
        final List<String> paths = new ArrayList<>();
        paths.add("MD_Metadata/fileIdentifier/CharacterString");
        paths.add("MI_Metadata/fileIdentifier/CharacterString");
        paths.add("CI_ResponsibleParty/@uuid");
        paths.add("Record/identifier");
        paths.add("SensorML/member/System/@gml:id");
        paths.add("SensorML/member/Component/@gml:id");
        paths.add("*/@id");
        paths.add("DIF/Entry_ID/Short_Name");
        return findIdentifierNode(node, paths);
    }

    /**
     * This method try to find an identifier for this Document Node.
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param node the node object for which we want a identifier.
     * @param paths
     *
     * @return the founded identifier or UNKNOW_IDENTIFIER
     */
    public static String findIdentifierNode(final Node node, final List<String> paths) {

        for (String path : paths) {
            final String[] parts = path.split("/");
            if (parts[0].equals(node.getLocalName()) || parts[0].equals("*")) {
                List<Node> nodes = Arrays.asList(node);
                for (int i = 1; i < parts.length; i++) {
                    nodes = NodeUtilities.getNodes(parts[i], nodes, -1, false);
                    if (nodes.isEmpty()) {
                        break;
                    }
                }
                if (!nodes.isEmpty()) {
                    return nodes.get(0).getTextContent();
                }
            }
        }
        return UNKNOW_IDENTIFIER;
    }

    /**
     * Extract the String values denoted by the specified paths
     * and return the values as a String values1,values2,....
     * if there is no values corresponding to the paths the method return "null" (the string)
     *
     * This method use path with an old structure (MDweb) and should be changed to use proper XPath
     *
     * @param metadata
     * @param paths
     * @return
     */
    public static List<Object> extractValues(final Object metadata, final List<String> paths) {
        final List<Object> response  = new ArrayList<>();

        if (paths != null) {
            for (String fullPathID : paths) {
               if (!ReflectionUtilities.pathMatchObjectType(metadata, fullPathID)) {
                   continue;
               }
                String pathID;
                String conditionalAttribute = null;
                String conditionalValue     = null;

                // if the path ID contains a # we have a conditional value (codeList element) next to the searched value.
                final int separator = fullPathID.indexOf('#');
                if (separator != -1) {
                    pathID               = fullPathID.substring(0, separator);
                    conditionalAttribute = fullPathID.substring(separator + 1, fullPathID.indexOf('='));
                    conditionalValue     = fullPathID.substring(fullPathID.indexOf('=') + 1);
                    int nextSeparator    = conditionalValue.indexOf(':');
                    if (nextSeparator == -1) {
                        throw new IllegalArgumentException("A conditionnal path must be in the form ....:attribute#attibuteconditional=value:otherattribute");
                    } else {
                        pathID = pathID + conditionalValue.substring(nextSeparator);
                        conditionalValue = conditionalValue.substring(0, nextSeparator);
                    }
                    LOGGER.finer("pathID              : " + pathID               + '\n' +
                                 "conditionalAttribute: " + conditionalAttribute + '\n' +
                                 "conditionalValue    : " + conditionalValue);

                } else {
                    pathID = fullPathID;
                }

                if (conditionalAttribute == null) {
                    final Object brutValue   = ReflectionUtilities.getValuesFromPath(pathID, metadata);
                    final List<Object> value = getStringValue(brutValue);
                    if (value != null && !value.isEmpty() && !value.equals(Arrays.asList(NULL_VALUE))) {
                        response.addAll(value);
                    }
                } else {
                    final Object brutValue   = ReflectionUtilities.getConditionalValuesFromPath(pathID, conditionalAttribute, conditionalValue, metadata);
                    final List<Object> value = getStringValue(brutValue);
                    response.addAll(value);
                }
            }
        }
        if (response.isEmpty()) {
            //response.add(NULL_VALUE);
        }
        return response;
    }

    /**
     * Return a String value from the specified Object.
     * Let the number object as Number
     *
     * @param obj
     * @return
     */
    private static List<Object> getStringValue(final Object obj) {
        final List<Object> result = new ArrayList<>();
        if (obj == null) {
            result.add(NULL_VALUE);
        } else if (obj instanceof String) {
            result.add(obj);
        } else if (obj instanceof Number) {
            result.add(obj);
        } else if (obj instanceof InternationalString) {
            final InternationalString is = (InternationalString) obj;
            result.add(is.toString());
        } else if (obj instanceof LocalName) {
            final LocalName ln = (LocalName) obj;
            result.add(ln.toString());
        } else if (obj instanceof Double || obj instanceof Long) {
            result.add(obj.toString());
        } else if (obj instanceof java.util.Locale) {
            try {
                result.add(((java.util.Locale)obj).getISO3Language());
            } catch (MissingResourceException ex) {
                result.add(((java.util.Locale)obj).getLanguage());
            }
        } else if (obj instanceof Collection) {
            for (Object o : (Collection) obj) {
                result.addAll(getStringValue(o));
            }
            if (result.isEmpty()) {
                result.add(NULL_VALUE);
            }
        } else if (obj instanceof org.opengis.util.CodeList) {
            result.add(((org.opengis.util.CodeList)obj).name());

        } else if (ReflectionUtilities.instanceOf("org.geotoolkit.gml.xml.AbstractTimePosition", obj.getClass())) {
            final Method getDate = ReflectionUtilities.getMethod("getDate", obj.getClass());
            final Date d = (Date) ReflectionUtilities.invokeMethod(obj, getDate);
            if (d != null) {
                synchronized (Util.LUCENE_DATE_FORMAT) {
                    result.add(Util.LUCENE_DATE_FORMAT.format(d));
                }
            } else {
                result.add(NULL_VALUE);
            }

        } else if (obj instanceof Instant) {
            final Instant inst = (Instant)obj;
            if (inst.getDate() != null) {
                synchronized (Util.LUCENE_DATE_FORMAT) {
                    result.add(Util.LUCENE_DATE_FORMAT.format(inst.getDate()));
                }
            } else {
                result.add(NULL_VALUE);
            }
        } else if (obj instanceof Date) {
            synchronized (Util.LUCENE_DATE_FORMAT){
                result.add(Util.LUCENE_DATE_FORMAT.format((Date)obj));
            }
        } else if (obj instanceof Enum) {
            result.add(((Enum)obj).name());

        } else {
            throw new IllegalArgumentException("this type is unexpected: " + obj.getClass().getSimpleName());
        }
        return result;
    }

    /**
     * @param fileMetadata
     * @param metadataToMerge
     *
     */
    public static DefaultMetadata mergeMetadata(final Metadata fileMetadata, final Metadata metadataToMerge) {
        final DefaultMetadata merged = new DefaultMetadata(fileMetadata);
        final Merger merger = new Merger(null) {
            @Override
            protected void merge(ModifiableMetadata target, String propertyName, Object sourceValue, Object targetValue) {
                // Ignore (TODO: setup merge policy).
                LOGGER.log(Level.FINE, "Ignoring merge for property {0}", propertyName);
            }
        };
        merger.copy(metadataToMerge, merged);
        return merged;
    }

    public static String encodeXMLMark(String str) {
        if (str != null && !str.isEmpty()) {
            str = str.trim();
            // XML mark does not support no space or %20
            str = str.replace("%20", "_");
            str = str.replace(" ", "_");
            return encodeXML(str);
        }
        return str;
    }
    
    /**
     * Escapes the characters in a String.
     *
     * @param str
     * @return String
     */
    public static String encodeXML(String str) {
        if (str != null && !str.isEmpty()) {
            str = str.trim();
            final StringBuilder buf = new StringBuilder(str.length() * 2);
            int i;
            for (i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                int intValue = (int)ch;
                String entityName = null;

                switch (intValue) {
                    case 34 : entityName = "quot"; break;
                    case 39 : entityName = "apos"; break;
                    case 38 : entityName = "amp"; break;
                    case 60 : entityName = "lt"; break;
                    case 62 : entityName = "gt"; break;
                    case 65533 : ch = '_'; break; // fallback value when the character has not been correctly interpretted.
                }

                if (entityName == null) {
                    if (ch > 0x7F) {
                        buf.append("&#");
                        buf.append(intValue);
                        buf.append(';');
                    } else {
                        buf.append(ch);
                    }
                } else {
                    buf.append('&');
                    buf.append(entityName);
                    buf.append(';');
                }
            }
            return buf.toString();
        }
        return str;
    }
}
