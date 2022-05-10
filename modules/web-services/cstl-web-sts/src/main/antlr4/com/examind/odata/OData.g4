/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
grammar OData;

options {
    language = Java;
}

//-----------------------------------------------------------------//
// LEXER
//-----------------------------------------------------------------//


// GLOBAL STUFF ---------------------------------------

COMMA 	: ',' ;
WS  :   ( '\t' | '\r'| '\n' ) -> skip;
UNARY : '+' | '-' ;
MULT : '*' ;
fragment DIGIT : '0'..'9' ;

// caseinsensitive , possible alternative solution ?
fragment A: ('a'|'A');
fragment B: ('b'|'B');
fragment C: ('c'|'C');
fragment D: ('d'|'D');
fragment E: ('e'|'E');
fragment F: ('f'|'F');
fragment G: ('g'|'G');
fragment H: ('h'|'H');
fragment I: ('i'|'I');
fragment J: ('j'|'J');
fragment K: ('k'|'K');
fragment L: ('l'|'L');
fragment M: ('m'|'M');
fragment N: ('n'|'N');
fragment O: ('o'|'O');
fragment P: ('p'|'P');
fragment Q: ('q'|'Q');
fragment R: ('r'|'R');
fragment S: ('s'|'S');
fragment T: ('t'|'T');
fragment U: ('u'|'U');
fragment V: ('v'|'V');
fragment W: ('w'|'W');
fragment X: ('x'|'X');
fragment Y: ('y'|'Y');
fragment Z: ('z'|'Z');
fragment LETTER : ~('0'..'9' | ' ' | '\t' | '\r'| '\n' | ',' | '-' | '+' | '*' | '(' | ')' | '=' | '>' | '<');

LPAREN : '(';
RPAREN : ')';


//LITERALS  ----------------------------------------------

TEXT :   '\'' ( ESC_SEQ | ~('\'') )* '\'' ;
INT : DIGIT+ ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;


// FILTERING OPERAND -----------------------------------
COMPARE
	: EQUALABOVE
	| EQUALUNDER
	| NOTEQUAL
	| EQUAL
	| ABOVE
	| UNDER
	;
fragment EQUALABOVE : ' ge ' ;
fragment EQUALUNDER : ' le ' ;
fragment NOTEQUAL   : ' ne ' ;
fragment EQUAL      : ' eq ' ;
fragment ABOVE      : ' gt ' ;
fragment UNDER      : ' lt ' ;



// LOGIC ----------------------------------------------
AND : ' and ';
OR  : ' or ';
NOT : 'not ' ;

// SPATIAL  ----------------------------------------------
CONTAINS    : 'st_contains' ;
GEOGRAPHY    : ' geography' ;

// TEMPORAL TYPES AND FILTERS

DATE : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT 'T' DIGIT DIGIT ':' DIGIT DIGIT ':' DIGIT DIGIT ('.' DIGIT+)? 'Z';
DURATION_P : P (INT 'Y')? (INT 'M')? (INT 'D')? (INT 'H')? (INT 'M')? (INT 'S')?;
DURATION_T : T (INT 'H')? (INT 'M')? (INT 'S')?;


// PROPERTY NAME -------------------------------------
PROPERTY_NAME    	:  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'    ;
NAME   	: LETTER (DIGIT|LETTER)* ;


// FRAGMENT -------------------------------------------

fragment EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;
fragment HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;




//-----------------------------------------------------------------//
// PARSER
//-----------------------------------------------------------------//

expressionNum : INT | FLOAT ;
expressionUnary : UNARY? expressionNum ;

expressionFctParam
        : expression (COMMA expression)*
        ;

expressionTerm
	: TEXT
	| expressionUnary
	| PROPERTY_NAME
	| DATE
	| DURATION_P
	| DURATION_T
	| NAME (LPAREN expressionFctParam? RPAREN)?
	| LPAREN expression RPAREN
	;

expression : expression MULT expression
           | expression UNARY expression
           | expressionTerm
           ;

filterTerm 	: expression
                    (
                              COMPARE  expression
                            | NOT? IN LPAREN (expressionFctParam )?  RPAREN
                    )
                ;

filterGeometry
        : CONTAINS LPAREN expression COMMA GEOGRAPHY TEXT RPAREN;

filter : filter (AND filter)+
       | filter (OR filter )+
       | LPAREN filter RPAREN
       | NOT (filterTerm | (LPAREN filter RPAREN) )
       | filterTerm
       | filterGeometry
       ;

filterOrExpression : filter | expression ;


