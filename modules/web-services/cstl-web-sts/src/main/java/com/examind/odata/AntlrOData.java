/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

package com.examind.odata;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;


/**
 * ANTLR odataFilter parser methods.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class AntlrOData {

    private AntlrOData() {
    }

    public static ParseTree compile(String odataFilter) {
        final Object obj = compileFilterOrExpression(odataFilter);
        ParseTree tree = null;
        if (obj instanceof ParseTree) {
            tree = (ParseTree) obj;
        }
        return tree;
    }

    public static Object compileExpression(String odataFilter) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(odataFilter);
            final TokenStream tokens = new CommonTokenStream(new ODataLexer(input));

            // Parser generates abstract syntax tree.
            final ODataParser parser = new ODataParser(tokens);
            final ODataParser.ExpressionContext ctx = parser.expression();
            return ctx;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static Object compileFilter(String odataFilter) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(odataFilter);
            final TokenStream tokens = new CommonTokenStream(new ODataLexer(input));

            // Parser generates abstract syntax tree.
            final ODataParser parser = new ODataParser(tokens);
            final ODataParser.FilterContext retfilter = parser.filter();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }

    public static Object compileFilterOrExpression(String odataFilter) {
        try {
            // Lexer splits input into tokens.
            final CodePointCharStream input = CharStreams.fromString(odataFilter);
            final TokenStream tokens = new CommonTokenStream(new ODataLexer(input));

            // Parser generates abstract syntax tree.
            final ODataParser parser = new ODataParser(tokens);
            final ODataParser.FilterOrExpressionContext retfilter = parser.filterOrExpression();

            return retfilter;

        } catch (RecognitionException e) {
            throw new IllegalStateException("Recognition exception is never thrown, only declared.");
        }
    }
}
