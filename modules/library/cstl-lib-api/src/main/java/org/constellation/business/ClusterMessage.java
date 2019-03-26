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
package org.constellation.business;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.sis.util.ArgumentChecks;

/**
 * Message object broadcasted between constellation hazelcast instances.<br>
 * <br>
 * The message map should be composed of simple value types including :<br>
 * <ul>
 * <li>Boolean</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>String</li>
 * <li>MessageMap</li>
 * </ul>
 *
 * Other object types are also supported but not recommended, if complex objects
 * are really needed it is recommended to convert the object to a MessageMap, a String
 * or make it implement DataSerializable.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ClusterMessage extends HashMap<String, Object> {

    public static enum Type {
        /** request message without response */
        REQUEST_NO_RESPONSE,
        /** request message with expected response */
        REQUEST_WITH_RESPONSE,
        /** response */
        RESPONSE,
        /** message part, contained in a parent message */
        PART
    }

    /**
     * Key containing the exception message if this message is a response from a node
     * which produced an exception when processing the request.
     */
    public static final String KEY_EXCEPTION_MESSAGE = "exceptionMessage";
    /**
     * Key containing the exception stack if this message is a response from a node
     * which produced an exception when processing the request.
     */
    public static final String KEY_EXCEPTION_STACK = "exceptionStack";

    protected String typeId;
    /**
     * Message type
     */
    protected Type messageType;
    /**
     * Unique identifier of the message, this is used to track possible responses.
     */
    protected long messageUID;
    /**
     * Unique identifier of the instance who created the message.
     */
    protected String memberUID;

    /**
     * Create a message map which will be contained in another message map.
     *
     * @return
     */
    public ClusterMessage createPart(){
        final ClusterMessage message = new ClusterMessage(memberUID);
        message.typeId = "";
        message.messageType = Type.PART;
        message.messageUID = 0;
        return message;
    }

    /**
     * Create a new response message.
     *
     * @return MessageMap never null
     */
    public ClusterMessage createResponse(IClusterBusiness clusterBusiness){
        if(messageType!=Type.REQUEST_WITH_RESPONSE){
            throw new IllegalArgumentException("This message does not expect any response.");
        }
        final ClusterMessage message = new ClusterMessage(clusterBusiness.getMemberUID());
        message.typeId = typeId;
        message.messageType = Type.RESPONSE;
        message.messageUID = messageUID;
        return message;
    }

    /**
     * Create a new exception response message.
     *
     * @param ex not null
     * @return MessageMap never null
     */
    public ClusterMessage createExceptionResponse(IClusterBusiness clusterBusiness,Exception ex){
        ArgumentChecks.ensureNonNull("exception", ex);
        final ClusterMessage response = createResponse(clusterBusiness);
        response.put(KEY_EXCEPTION_MESSAGE, ex.getMessage());
        final StringWriter writer = new StringWriter();
        final PrintWriter pw = new PrintWriter(writer, true);
        ex.printStackTrace(pw);
        pw.flush();
        response.put(KEY_EXCEPTION_STACK, writer.toString());
        return response;
    }

    protected ClusterMessage(String memberUID) {
        this.memberUID = memberUID;
        this.typeId = "";
        this.messageType = Type.REQUEST_NO_RESPONSE;
    }

    protected ClusterMessage(String memberUID, String typeId, Type messageType) {
        this.memberUID = memberUID;
        this.typeId = typeId;
        this.messageType = messageType;
    }

    /**
     * Message type identifier.<br>
     * This identifier should be used to filter messages.
     *
     * @return message type id, never null
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * @return unique request and responses id.
     */
    public long getMessageUID() {
        return messageUID;
    }

    /**
     *
     * @return unique member identifier
     */
    public String getMemberUID() {
        return memberUID;
    }

    /**
     * @return true if message is a request.
     */
    public boolean isRequest() {
        return messageType == Type.REQUEST_NO_RESPONSE || messageType == Type.REQUEST_WITH_RESPONSE;
    }

    /**
     * @return true if message is a request and expects a response.
     */
    public boolean expectResponse(){
        return messageType == Type.REQUEST_WITH_RESPONSE;
    }

    /**
     * @return true if message is a response and contains an exception.
     */
    public boolean isResponseException(){
        return messageType == Type.RESPONSE && (containsKey(KEY_EXCEPTION_MESSAGE) || containsKey(KEY_EXCEPTION_STACK));
    }

    public Boolean getBoolean(String key, boolean nullable) throws MessageException {
        return getCast(Boolean.class, key, nullable);
    }

    public Integer getInteger(String key, boolean nullable) throws MessageException {
        return getCast(Integer.class, key, nullable);
    }

    public Long getLong(String key, boolean nullable) throws MessageException {
        return getCast(Long.class, key, nullable);
    }

    public Float getFloat(String key, boolean nullable) throws MessageException {
        return getCast(Float.class, key, nullable);
    }

    public Double getDouble(String key, boolean nullable) throws MessageException {
        return getCast(Double.class, key, nullable);
    }

    public String getString(String key, boolean nullable) throws MessageException {
        return getCast(String.class, key, nullable);
    }

    public ClusterMessage getMap(String key, boolean nullable) throws MessageException {
        return getCast(ClusterMessage.class, key, nullable);
    }

    private <T> T getCast(Class<T> clazz, String key, boolean nullable) throws MessageException {
        final Object val = get(key);
        if(val==null && nullable){
            return null;
        }else if(clazz.isInstance(val)){
            return (T) val;
        }else{
            throw new MessageException("Missing message value "+key);
        }
    }

    public String printMessage() {
        final StringBuilder sb = new StringBuilder(" [");
        sb.append(typeId).append(':').append(messageUID).append("] (");
        if(isRequest()){
            sb.append(expectResponse() ? " REQUEST WITH REPONSE" : "REQUEST");
        }else{
            sb.append(isResponseException() ? "EXCEPTION RESPONSE" : "RESPONSE");
        }
        sb.append(")");

        try {
            ObjectMapper mapper = new ObjectMapper();
            sb.append(mapper.writeValueAsString(entrySet()));
            return sb.toString();
        } catch (JsonProcessingException e) {
            return asList(sb.toString(), entrySet());
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message [");
        sb.append(typeId).append(':').append(messageUID).append("] (");
        if(isRequest()){
            sb.append(expectResponse() ? " REQUEST WITH REPONSE" : "REQUEST");
        }else{
            sb.append(isResponseException() ? "EXCEPTION RESPONSE" : "RESPONSE");
        }
        sb.append(")");

        return asList(sb.toString(), entrySet());
    }

    /**
     * Display the given collection as a line separated list of strings. List
     * content is simply converted using {@link Objects#toString()} method, and
     * no recursion is performed on eventual sublists.
     *
     * Note : generated list ends with a new line.
     *
     * @param header A header to put on the first line of the generated String.
     * @param list The collection to print.
     * @return A String representing given arguments as a list.
     */
    private static String asList(final String header, final Collection<?> list) {
        return list.stream()
                .map(Objects::toString)
                .collect(Collectors.joining(System.lineSeparator(), String.format("%s%n", header), System.lineSeparator()));
    }
}