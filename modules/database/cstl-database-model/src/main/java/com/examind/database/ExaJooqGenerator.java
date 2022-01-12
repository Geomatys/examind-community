/*
 *    Examind Community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package com.examind.database;

import org.jooq.codegen.GeneratorStrategy;
import org.jooq.codegen.JavaGenerator;
import org.jooq.codegen.JavaWriter;
import org.jooq.meta.Definition;

/**
 * JOOQ Customized generator. Add licence/class headers to the generated DAO files.
 *
 * @author Guilhem Legal (Geomatys
 */
public class ExaJooqGenerator extends JavaGenerator {

    private static final String EXAMIND_LICENCE =
    "    Examind Community - An open source and standard compliant SDI\n" +
    "    https://community.examind.com/\n" +
    "\n" +
    " Copyright 2022 Geomatys.\n" +
    "\n" +
    " Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
    " you may not use this file except in compliance with the License.\n" +
    " You may obtain a copy of the License at\n" +
    "\n" +
    "      http://www.apache.org/licenses/LICENSE-2.0\n" +
    "\n" +
    " Unless required by applicable law or agreed to in writing, software\n" +
    " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
    " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
    " See the License for the specific language governing permissions and\n" +
    " limitations under the License.\n";

    @Override
    protected void printPackageComment(JavaWriter out, Definition definition, GeneratorStrategy.Mode mode) {
        out.println("/*");
        printJavadocParagraph(out, EXAMIND_LICENCE, "");
        out.println("*/");
    }

    @Override
    protected void printClassJavadoc(JavaWriter out, Definition definition) {
        String comment;
        if (definition.getComment() != null && definition.getComment().isEmpty()) {
            comment = definition.getComment();
        } else {
            comment = "Generated DAO object for table " + definition.getQualifiedInputName();
        }
        printClassJavadoc(out, escapeEntities(comment));
    }

    @Override
    protected void printClassJavadoc(JavaWriter out, String comment) {
        if (generateJavadoc()) {
            out.println("/**");
            printJavadocParagraph(out, comment, "");
            out.println(" */");
        }
    }




}
