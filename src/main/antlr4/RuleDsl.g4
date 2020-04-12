grammar RuleDsl;

@header {
  package snorochevskiy.pojoeval.rules.dsl.v2.parser;
}

topExpr : logicExpr EOF ;

logicExpr : orExpr;

orExpr
  : andExpr
  | orExpr OR andExpr
  ;

andExpr
  : notExpr
  | andExpr AND notExpr
  ;

notExpr
  : eqExpr
  | NOT notExpr
  ;

eqExpr
  : additiveExpr
  | eqExpr Eq additiveExpr
  | eqExpr NEq additiveExpr
  | relExpr StrContains relExpr
  | relExpr StrContainsRegexp relExpr
  | relExpr StrMatches relExpr
  | relExpr In stringList
  | additiveExpr Compare additiveExpr
  ;

additiveExpr
  : multiplicativeExpr
  | additiveExpr Plus multiplicativeExpr
  | additiveExpr Minus multiplicativeExpr
  ;

multiplicativeExpr
  : relExpr
  | multiplicativeExpr Multiply relExpr
  | multiplicativeExpr Divide relExpr
  | multiplicativeExpr Mod relExpr
  ;

relExpr
  : Identifier
  | StringLiteral
  | DigitSequence
  | OpBr logicExpr ClBr
  ;


stringList : OpSqBk (StringLiteral Comma )* StringLiteral ClSqBk ;

Eq : '=' ;

NEq : '!=' ;

OR : 'OR' | 'or' | 'Or' ;

AND : 'AND' | 'and' | 'And' ;

NOT : 'NOT' | 'not' | 'Not' ;

In : 'IN' | 'in' | 'In' ;

Plus : '+' ;
Minus : '-' ;
Multiply : '*';
Divide : '/';
Mod : '%';

Compare : '>' | '<' | '>=' | '<=' ;

OpSqBk : '[' ;
ClSqBk : ']' ;

OpBr : '(' ;
ClBr : ')' ;

Comma : ',' ;

StrContains : 'contains' ;
StrContainsRegexp : 'contains_regexp';
StrMatches : 'matches' ;

Identifier
    :   Nondigit
        (   Nondigit
        |   Digit
        )*
    ;

fragment
Nondigit
    :   [a-zA-Z_]
    ;

DigitSequence // TODO: add sign
    : '-'?Digit+
    | '-'?Digit+.Digit+
    ;
fragment
Digit
    :   [0-9]
    ;

StringLiteral
    :   '"' SCharSequence? '"'
    |   '\'' SCharSequence? '\''
    ;

fragment
SCharSequence
    :   SChar+
    ;

fragment
SChar
    :   ~["\r\n']
    | '\\\''
    | '\\"'
//    |   EscapeSequence // Are escape sequences needed?
    |   '\\\n'   // Added line
    |   '\\\r\n' // Added line
    ;


WHITESPACE: [ \t\r\n]-> skip;