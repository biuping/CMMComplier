# CMMComplier
a little interpreter wrote by java for CMM language
## 点击.exe运行解释器
### 文法
* program -> statement// 外部（全局）声明

* statement -> declare | block_sta | if_sta | while_sta | for_sta | print_sta | scan_sta | assign_sta | break_sta | continue_sta | conditionTop | statement

* //声明语句
declare -> (INT|REAL|BOOL|CHAR|STRING) declare_sub (COMI declare_sub)* SEMI
* declare_sub -> (ID (ASSIGN condition)?) | (ID array (ASSIGN LBRACE arrayDeclare RBRACE)?)
* array -> LBRACKET expression RBRACKET
* arrayDeclare -> (INTNUM|REALNUM|CHAR_S|STR|TRUE|FALSE|ID) (COMI (INTNUM|REALNUM|CHAR_S|STR|TRUE|FALSE|ID))*
* //{}代码段语句
block_sta -> LBRACE (statement)* RBRACE
* //if语句
* if_sta -> IF LPAREN condition RPAREN (LBRACE (statement)* RBRACE | (statement)?) (ELSE IF elseif_sta)* ELSE(LBRACE (statement)* RBRACE | (statement)?)
* elseif_sta -> LPAREN condition RPAREN (LBRACE (statement)* RBRACE | (statement)?)
* //while语句
while_sta -> WHILE LPAREN condition RPAREN LBRACE (statement)* RBRACE
* //for语句
for_sta -> FOR LPAREN (declare | assign_sta) condition assign_sta RPAREN (LBRACE (statement)* RBRACE | (statement)?)
* //赋值语句
assign -> ID (array)? ASSIGN condition SEMI
* //print语句
print_sta -> PRINT LPAREN (condition | ID) RPAREN SEMI
* //scan语句
scan_sta -> SCAN LPAREN (STR | ID)? RPAREN (SEMI)?
* //break
break_sta -> BREAK SEMI
* //continue
continue_sta -> CONTINUE SEMI
* //声明，赋值之外直接出现算术式情况 eg：1+2；
conditionTop -> condition (SEMI)+ 
* //条件语句 解决符号优先级问题
* condition -> firstCondition (OR firstCondition)*
* firstCondition -> secondCondition (AND secondCondition)*
* secondCondition -> thirdCondition (BAND thirdCondition)*
* thirdCondition -> fourCondition (BOR fourCondition)*
* fourCondition -> expression (comparsion expression)*
* expression -> mdexpression ((ADD|SUB) mdexpression)*
* mdexpression -> factor ((MUL|DEVIDE) factor)*
* //单个因子式
factor->INTNUM|REALNUM|CHAR_S|STR|TRUE|FALSE|(ID(array)?)|((NEG|POS) factor)|( LPAREN condition RPAREN ) | scan_sta
comparsion -> EQ | GREATER | UE | GE | LE | LESS
* //注解   
< LPARENT: ( >  < RPARENT: ) > < LBRACE: { > < RBRACE: } > < LBRACKET: [ > < RBRACKET: ] > < SEMI: ; > < COMI: , >
