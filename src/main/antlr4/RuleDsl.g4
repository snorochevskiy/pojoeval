grammar RuleDsl;

@header {
  package snorochevskiy.pojoeval.rules.dsl.v1.parser;
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
  | '(' logicExpr ')'
  ;

eqExpr
  : relExpr
  | eqExpr Eq relExpr
  | eqExpr NEq relExpr
  | eqExpr StrContains relExpr
  | eqExpr StrContainsRegexp relExpr
  | eqExpr StrMatches relExpr
  | relExpr In stringList
  ;

relExpr
  : Identifier
  | StringLiteral
//  | DigitSequence // TODO: add numbers support
  ;

stringList : OpSqBk (StringLiteral Comma )* StringLiteral ClSqBk ;

Eq : '=' ;

NEq : '!=' ;

OR : 'OR' | 'or' | 'Or' ;

AND : 'AND' | 'and' | 'And' ;

NOT : 'NOT' | 'not' | 'Not' ;

In : 'IN' | 'in' | 'In' ;

OpSqBk : '[' ;
ClSqBk : ']' ;

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
    :   ~["\r\n]
//    |   EscapeSequence // Are escape sequences needed?
    |   '\\\n'   // Added line
    |   '\\\r\n' // Added line
    ;


WHITESPACE: [ \t\r\n]-> skip;