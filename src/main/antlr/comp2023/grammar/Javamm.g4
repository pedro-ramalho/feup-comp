grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' pack+=ID ('.' pack+=ID)* ';'
    ;

classDeclaration
    : 'class' name=ID ('extends' pack=ID)? '{' ( varDeclaration )* ( methodDeclaration )* '}'
    ;

varDeclaration
    : type var=ID ';'
    ;

type
    : 'int' '[' ']' #IntArray
    | 'boolean' #Boolean
    | 'int' #Int
    | 'String' #String
    | name=ID #CustomType
    ;

methodDeclaration
    : ('public')? type name=ID '(' ( type parameter+=ID (',' type parameter+=ID)*)? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #Method
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}' #Main
    ;
statement
    : '{' (statement)* '}' #CodeBlock
    | 'if' '(' expression ')' statement 'else' statement #Conditional
    | 'while' '(' expression ')' statement #While
    | expression ';' #ExprStmt
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '!' expression #Negation
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #ArrayLength
    | expression '.' method=ID '(' (expression(',' expression)*)?')' #MethodInvocation
    | 'new' 'int' '[' expression ']' #ArrayInstantiation
    | 'new' objectType=ID '(' ')' #CustomInstantiation
    | '(' expression ')' #Parenthesis
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('<' | '>') expression #BinaryOp
    | expression op=('&&' | '||') expression #BinaryOp
    | value=INTEGER #Integer
    | 'true' #True
    | 'false' #False
    | value=ID #Identifier
    | 'this' #CurrentObject
    ;
