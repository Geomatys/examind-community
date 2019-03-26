package org.constellation.engine.template

import groovy.text.GStringTemplateEngine

import java.nio.file.Path

/** GroovyTemplateEngine using groovy.text.GStringTemplateEngine
 * Created by christophe mourette on 02/04/14 for Geomatys.
 */
public class GroovyTemplateEngine implements TemplateEngine {

    /**
     * apply values from TemplateFile
     */
    public String apply(Path templateFile, Properties param){
        def gstring = new GStringTemplateEngine()
        def gbinding = [param: param]
        def goutput = gstring.createTemplate(templateFile.toFile().text).make(gbinding).toString()
		return goutput
    }

    /**
     * apply values from TemplateStream
     */
    public String apply(InputStream templateStream, Properties param){
        def gstring = new GStringTemplateEngine()
        def gbinding = [param: param]
        def goutput = gstring.createTemplate(templateStream.getText()).make(gbinding).toString()
        return goutput
    }
}