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
package org.constellation.api.rest.converter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.style.Style;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StyleResponseConverter implements HttpMessageConverter<Object> {

    @Override
    public boolean canRead(Class<?> type, MediaType mt) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return Style.class.isAssignableFrom(type) ||
               StyledLayerDescriptor.class.isAssignableFrom(type);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
         return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class<? extends Object> type, HttpInputMessage him) throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("Style message converter do not support reading.", him);
    }

    @Override
    public void write(Object r, MediaType mt, HttpOutputMessage hom) throws IOException, HttpMessageNotWritableException {
        final StyleXmlIO utils = new StyleXmlIO();
        
        try{
            if(r instanceof Style){
                utils.writeStyle(hom.getBody(), (Style)r, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            }else if(r instanceof StyledLayerDescriptor){
                utils.writeSLD(hom.getBody(), (StyledLayerDescriptor)r, org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor.V_1_1_0);
            }else{
                throw new IOException("Unhandle object class : " + r.getClass());
            }
        }catch(JAXBException ex){
            throw new IOException(ex);
        }
    }
    
}
