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
package org.constellation.api.rest.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.sld.MutableLayer;
import org.geotoolkit.sld.MutableStyledLayerDescriptor;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableStyle;
import org.opengis.sld.LayerStyle;
import org.opengis.sld.NamedLayer;
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.sld.UserLayer;
import org.opengis.style.Style;
import org.opengis.util.FactoryException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * XML Style message converter.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class StyleMessageConverter implements HttpMessageConverter<Object> {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.rest.api");
    
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return Style.class.isAssignableFrom(clazz) 
            || StyledLayerDescriptor.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return Style.class.isAssignableFrom(clazz) 
            || StyledLayerDescriptor.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        
        //copy the file content in memory
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtilities.copy(inputMessage.getBody(), bos);
        final byte[] buffer = bos.toByteArray();
            
        //try to parse SLD from various form and version      
        final List<MutableStyle> styles = new ArrayList<>();
        final StyleXmlIO io = new StyleXmlIO();  
        MutableStyle style = null;
        
        //try to parse an SLD input
        MutableStyledLayerDescriptor sld = null;
        try {
            sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_1_0);
        } catch (JAXBException | FactoryException ex) {
            LOGGER.log(Level.FINEST, ex.getMessage(),ex);
        }
        if(sld==null){
            try {
                sld = io.readSLD(new ByteArrayInputStream(buffer), Specification.StyledLayerDescriptor.V_1_0_0);
            } catch (JAXBException | FactoryException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage(),ex);
            }
        }
        
        if(sld != null){
            for(MutableLayer sldLayer : sld.layers()){
                if(sldLayer instanceof NamedLayer){
                    final NamedLayer nl = (NamedLayer) sldLayer;
                    for(LayerStyle ls : nl.styles()){
                        if(ls instanceof MutableStyle){
                            styles.add((MutableStyle)ls);
                        }
                    }
                }else if(sldLayer instanceof UserLayer){
                    final UserLayer ul = (UserLayer) sldLayer;
                    for(org.opengis.style.Style ls : ul.styles()){
                        if(ls instanceof MutableStyle){
                            styles.add((MutableStyle)ls);
                        }
                    }
                }
            }
            if(!styles.isEmpty()){
                style = styles.remove(0);
            }
        }else{
            //try to parse a UserStyle input
            try {
                style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.V_1_1_0);
            } catch (JAXBException | FactoryException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage(),ex);
            }
            if(style==null){
                try {
                    style = io.readStyle(new ByteArrayInputStream(buffer), Specification.SymbologyEncoding.SLD_1_0_0);
                } catch (JAXBException | FactoryException ex) {
                    LOGGER.log(Level.FINEST, ex.getMessage(),ex);
                }
            }
        }
        
        if(style==null){
            throw new HttpMessageNotReadableException("No UserStyle definition found.");
        }
        
        //log styles which have been ignored
        if(!styles.isEmpty()){
            final StringBuilder sb = new StringBuilder("Ignored styles at import :");
            for(MutableStyle ms : styles){
                sb.append(' ').append(ms.getName());
            }
            LOGGER.log(Level.FINEST, sb.toString());
        }
                
        return style;
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final StyleXmlIO utils = new StyleXmlIO();
        try {
            if (t instanceof Style) {
                utils.writeStyle(outputMessage.getBody(), (Style) t, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            } else if (t instanceof StyledLayerDescriptor) {
                utils.writeSLD(outputMessage.getBody(), (StyledLayerDescriptor) t, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            } else {
                throw new HttpMessageNotWritableException("Unhandle object : " + t);
            }
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
    }
    
}
