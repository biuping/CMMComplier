package main.lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
/***
 * @author 毕修平
 * 词法分析
 */
public class Lexer {
    // 注释的标志
    private boolean isNotation = false;
    //正负数标志
    private static boolean isPositive = false;
    private static boolean isNegative = false;
    private boolean isChar = false;
    // 分析后得到的tokens集合，用于其后的语法及语义分析
    private ArrayList<Token> tokens = new ArrayList<Token>();
    // 读取CMM文件文本
    private BufferedReader reader;
    //错误集合
    private ArrayList<Error> errors = new ArrayList<Error>();

    public ArrayList<Error> getErrors() {
        return errors;
    }

    public void setErrors(ArrayList<Error> errors) {
        this.errors = errors;
    }

    public boolean isNotation() {
        return isNotation;
    }

    public void setNotation(boolean notation) {
        isNotation = notation;
    }


    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void setTokens(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }


    public BufferedReader getReader() {
        return reader;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    //判断是不是字母
    private static boolean isLetter(char c) {
        if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')||c=='_')
            return true;
        return false;
    }

    //判断是不是数字
    private static boolean isNumber(char c){
        if (c >= '0' && c <= '9')
            return true;
        return false;
    }

    //识别整个整数
    private static boolean isInteger(String s){
        if (s.matches("^(\\-|\\+)?[\\s]*[0-9]\\d*$") ||s.matches("^(\\-|\\+?)0$"))
            return true;
        else
            return false;
    }

    //识别浮点数
    private static boolean isReal(String s) {
        if (s.matches("^((\\-|\\+)?[\\s]*\\d+)(\\.\\d+)+$"))
            return true;
        else
            return false;
    }

    //    识别关键字
    private static boolean isKey(String s){
        if (s.equals("if")||s.equals("else") ||s.equals("int")||s.equals("scan")||
                s.equals("real")||s.equals("break")||s.equals("while")||s.equals("for")
                ||s.equals("print")||s.equals("char")||s.equals("continue")
                ||s.equals("bool")||s.equals("string")||s.equals("true")||s.equals("false")){
            return true;
        }else {
            return false;
        }
    }

    //    识别标识符
    private static boolean isID(String input) {
        if (input.matches("^\\w+$") && !input.endsWith("_")
                && input.substring(0, 1).matches("[A-Za-z]"))
            return true;
        else
            return false;
    }

    //判断是不是转义字符
    private boolean isESC(String s){
        if (s.equals("\\\"")||s.equals("\\\'")||s.equals("\\n")||s.equals("\\t")
                ||s.equals("\\r")||s.equals("\\\\"))
            return true;
        else
            return false;
    }

    private static int jump(int begin, String str) {
        if (begin >= str.length())
            return str.length();
        if (isNegative || isPositive){
            for (int i = begin+1; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\n' || c == ',' || c == ' ' || c == '\t'
                        || c == '{'|| c == '}' || c == '['|| c == ']'|| c == '(' || c == ')'
                        || c == ';' || c == '='|| c == '<' || c == '>' ||c=='\\'
                        || c == '+' || c == '-' || c == '*' || c == '/'||c=='\''){
                    isPositive=false;
                    isNegative=false;
                    return i-1 ;
                }
            }
        }else{
            for (int i = begin; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\n' || c == ',' || c == ' ' || c == '\t'
                        || c == '{'|| c == '}' || c == '['|| c == ']'|| c == '(' || c == ')'
                        || c == ';' || c == '='|| c == '<' || c == '>'
                        || c == '+' || c == '-' || c == '*' || c == '/'||c=='\'')
                    return i-1 ;
            }
        }

        return str.length();
    }

    private int jump_char(int begin, String str){
        if (begin >= str.length())
            return str.length();
        for (int i = begin;i<str.length();i++){
            char c = str.charAt(i);
            if (c=='\'')
                return i;
            if (c=='\\')
                i++;
        }
        return str.length()+1;
    }

    private int jump_string(int begin, String str){
        if (begin >= str.length())
            return str.length();
        for (int i = begin;i<str.length();i++){
            char c = str.charAt(i);
            if (c=='\"')
                return i;
            if (c=='\\')
                i++;
        }
        return str.length()+1;
    }

    public void analyze(String cmmProgram,int lineNum){
        int flag=0;
        int begin=0;
        int end = 0;
        for (int index=0;index<=cmmProgram.length();index++){
            char c;
            if (index==cmmProgram.length()){
                c='\n';  //相当于程序的终止符
            }else {
                c=cmmProgram.charAt(index);
            }
            if (!isNotation){
                if(isNumber(c)||isLetter(c)||c=='('||c==')'||c=='<'||c=='>'||c=='='||c=='\''
                        ||c=='+'||c=='-' ||c=='*'||c=='/'||c==';'|| c=='['||c==']'||c=='{'||c=='}'
                        ||c=='&'||c=='|'||c=='\"'||c==','
                        ||String.valueOf(c).equals("\r")||String.valueOf(c).equals(" ")
                        ||String.valueOf(c).equals("\t")||String.valueOf(c).equals("\n")||isChar){
                    switch (flag){
                        //扫描到回车，空格，制表符，换行跳过
                        case 0:
                            if (c == '(' || c == ')' || c == ';' || c == '{'
                                    || c == '}' || c == '[' || c == ']'
                                    || c == ','){
                                //分隔符直接加入tokens
                                Token token = new Token(Tag.SEPARATOR,String.valueOf(c),lineNum,index+1);
                                tokens.add(token);
                                flag=0;
                            }else if (isLetter(c)){
                                //为字母跳转到1进行处理
                                flag=1;
                                begin=index;
                            }else if (isNumber(c)){
                                //为数字跳转到2进行处理
                                flag=2;
                                begin=index;
                            }
                            /*扫描为部分符号进行相应的处理，一元运算符则跳转到0，开始下一次的扫描
                            为二元运算符的前部分则跳转判断是否为二元运算符
                             */
                            else if (c=='+'){
                                flag=10;
                            }else if (c=='-'){
                                //负号或减号
                                flag=3;
                            }else if (c=='*'){
                                //可能为乘号，也可能为多行注释的结束标志
                                flag=8;
                            }else if(c=='/'){
                                flag=4;
                            }else if(c=='='){
                                flag=5;
                            }else if(c=='>'){
                                flag=6;
                            }else if(c=='<'){
                                flag=7;
                            }else if (c=='\''){
                                isChar=true;
                                Token token = new Token(Tag.SEPARATOR,"'",lineNum,index+1);
                                tokens.add(token);
                                begin=index+1;
                                flag=9;
                            }else if (c=='"'){
                                isChar=true;
                                Token token = new Token(Tag.SEPARATOR,"\"",lineNum,index+1);
                                tokens.add(token);
                                begin=index+1;
                                flag=11;
                            }else if (c=='&'){
                                flag=12;
                            }else if (c=='|'){
                                flag=13;
                            }
                            else {
                                flag=0;
                            }
                            break;
                        case 1:
                            if (isLetter(c) || isNumber(c)){
                                flag=1;
                            }else{
                                end=index;
                                String str = cmmProgram.substring(begin,end);
                                if (isKey(str)){
                                    Token token;
                                    switch (str){
                                        case "if":
                                            token = new Token(Tag.IF,str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "else":
                                            token = new Token(Tag.ELSE, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "while":
                                            token = new Token(Tag.WHILE, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "int":
                                            token = new Token(Tag.INT, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "real":
                                            token = new Token(Tag.REAL, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "break":
                                            token = new Token(Tag.BREAK, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "scan":
                                            token = new Token(Tag.SCAN, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "print":
                                            token = new Token(Tag.PRINT, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "char":
                                            token = new Token(Tag.CHAR, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "continue":
                                            token = new Token(Tag.CONTINUE, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "for":
                                            token = new Token(Tag.FOR, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "bool":
                                            token = new Token(Tag.BOOL, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "string":
                                            token = new Token(Tag.STRING, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "true":
                                            token = new Token(Tag.TRUE, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                        case "false":
                                            token = new Token(Tag.FALSE, str,lineNum,index-str.length()+1);
                                            tokens.add(token);
                                            break;
                                    }

                                }else if(isID(str)){
                                    Token token=new Token(Tag.ID,str,lineNum,index-str.length()+1);
                                    tokens.add(token);
                                }else{
                                    Error error = new Error(str,"非法标识符",lineNum,index-str.length()+1);
                                    errors.add(error);
                                }
                                index--;
                                flag=0;
                            }
                            break;
                        case 2:
                            if (isNumber(c)||String.valueOf(c).equals(".")){
                                flag=2;
                            }else{
                                if(isLetter(c)){
                                /*
                                数字或小数点之后出现字母，则直接跳到之后的分隔符或运算符，
                                相当于忽略这之后的数字和字母
                                比如int i = 3444tttt; 扫描到第一个t就直接跳到分号，其他的t不用再扫描
                                然后从该位置继续扫描
                                 */
                                    flag=0;
                                    index = jump(begin,cmmProgram);   //等同于end
                                    String str = cmmProgram.substring(begin,index+1);
                                    Error error = new Error(str,"数字格式错误或者非法标识符",lineNum,index);
                                    errors.add(error);

                                }else {
                                    end = index;
                                    String str = cmmProgram.substring(begin, end);
                                    if (str.contains(".")) {
                                        if (isReal(str)) {
                                            Token token = new Token(Tag.REALNUM, str, lineNum, begin+1);

                                            tokens.add(token);
                                        } else {
                                            Error error = new Error(str, "实数格式错误", lineNum, begin+1);
                                            errors.add(error);
                                        }
                                    } else {
                                        if (isInteger(str)) {
                                            Token token = new Token(Tag.INTNUM, str, lineNum, begin+1);
                                            tokens.add(token);
                                        } else {
                                            Error error = new Error(str, "整数数格式错误", lineNum, begin+1);
                                            errors.add(error);
                                        }
                                    }
                                    index = begin+str.length()-1;
                                }
                                flag=0;
                            }
                            break;
                        case 3:
                            int kind = 0;
                            String context = null;
                            if (tokens.isEmpty()){
                                //当-位于程序开头当做减号处理
                                Token token = new Token(Tag.SUB,"-",lineNum,index+1);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }else {
                                kind = tokens.get(tokens.size()-1).getTag();
                                context = tokens.get(tokens.size()-1).getContent();
                                if (kind==Tag.REALNUM||kind==Tag.INTNUM||context.equals(")")||context.equals("]")
                                        ||kind==Tag.ID||kind==Tag.CHAR ||kind==Tag.STR || kind==Tag.CHAR_S||
                                        kind==Tag.STRING||kind==Tag.INT || kind==Tag.REAL ||
                                        kind == Tag.BOOL || kind==Tag.TRUE || kind==Tag.FALSE ||
                                        context.equals("\"")||context.equals("'")){
                                    Token token = new Token(Tag.SUB,"-",lineNum,index+1);
                                    tokens.add(token);
                                    index--;
                                    flag=0;
                                }else if (c<'0' || c>'9'){
                                    Token token = new Token(Tag.NEG,"-",lineNum,index+1);
                                    tokens.add(token);
                                    index--;
                                    flag=0;
                                }
                                else {
                                    isNegative=true;
                                    begin=index-1;
                                    flag=2;
                                }
                            }
                            break;
                        case 4:
                            if (c=='/'){
                                //单行注释直接跳到行最后一个字符扫描
                                flag=0;
                                index=cmmProgram.length()-1;
                            }else if (c=='*'){
                                /*
                                多行注释,isNotation保证接下来在多行注释中的内容直接跳过，
                                直到多行注释结束标志出现
                                 */
                                isNotation=true;
                                begin=index+1;
                            }else {
                                Token token = new Token(Tag.DIVIDE,"/",lineNum,index+1);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }
                            break;
                        case 5:
                            if (c=='='){
                                //判断为等于符号，加入tokens
                                Token token = new Token(Tag.EQ,"==",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else{
                                //为赋值符号
                                Token token = new Token(Tag.ASSIGN,"=",lineNum,index);
                                tokens.add(token);
                                flag=0;
                                index--;
                            }
                            break;
                        case 6:
                            if (c=='='){
                                //为大于等于符号
                                Token token = new Token(Tag.GE,">=",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else{
                                //大于符号
                                Token token = new Token(Tag.GREATER,">",lineNum,index);
                                tokens.add(token);
                                flag=0;
                                index--;
                            }
                            break;
                        case 7:
                            if (c=='>'){
                                //不等于<>
                                Token token = new Token(Tag.UE,"<>",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else if (c=='='){
                                //小于等于
                                Token token = new Token(Tag.LE,"运算符<=",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else {
                                //小于
                                Token token = new Token(Tag.LESS,"<",lineNum,index);
                                tokens.add(token);
                                flag=0;
                                index--;
                            }
                            break;
                        case 8:
                            if (c=='/'){
                                Error error = new Error("*/","多行注释格式错误",lineNum,index);
                                errors.add(error);
                            }else {
                                //乘号
                                Token token = new Token(Tag.MUL,"*",lineNum,index);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }
                            break;
                        case 9:
                            int i = jump_char(begin,cmmProgram);
                            //正确字符格式
                            if (begin+1==i){
                                String str = cmmProgram.substring(begin,i);
                                Token token_char=new Token(Tag.CHAR_S,str,lineNum,i+1-str.length());
                                tokens.add(token_char);
                                Token token=new Token(Tag.SEPARATOR,"'",lineNum,i+1);
                                tokens.add(token);
                                flag=0;
                                index=i;
                            }
                            else if (i==cmmProgram.length()+1){
                                //缺少引号
                                String string = cmmProgram.substring(begin);
                                Error error = new Error(string,"缺少引号",lineNum,begin+1);
                                errors.add(error);
                                index=i-1;
                                flag=0;
                            }else if (begin+1<i){
                                String s_judge=cmmProgram.substring(begin,i);
                                if (isESC(s_judge) || s_judge.equals("\\'")){
                                    Token token_char=new Token(Tag.CHAR_S,s_judge,lineNum,i+1-s_judge.length());
                                    tokens.add(token_char);
                                    Token token=new Token(Tag.SEPARATOR,"'",lineNum,i+1);
                                    tokens.add(token);
                                }else {
                                    //字符格式不对
                                    Error error = new Error(s_judge,"字符格式错误",lineNum,begin+1);
                                    errors.add(error);
                                    Token token=new Token(Tag.SEPARATOR,"'",lineNum,i+1);
                                    tokens.add(token);
                                }
                                index=i;
                                flag=0;
                            }else{
                                //缺少前引号
                                String string = cmmProgram.substring(begin,i);
                                Error error = new Error(string,"缺少前引号",lineNum,begin+1);
                                errors.add(error);
                                index=i-1;
                                flag=0;
                            }
                            break;
                        case 10:
                            //处理加号和正号问题
                            if (tokens.isEmpty()){
                                //开头第一位当加号处理
                                Token token = new Token(Tag.ADD,"+",lineNum,index+1);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }else{
                                kind = tokens.get(tokens.size()-1).getTag();
                                context = tokens.get(tokens.size()-1).getContent();
                                if (kind==Tag.REALNUM||kind==Tag.INTNUM||context.equals(")")||context.equals("]")
                                        ||kind==Tag.ID||kind==Tag.CHAR ||kind==Tag.STR || kind==Tag.CHAR_S||
                                        kind==Tag.STRING||kind==Tag.INT || kind==Tag.REAL ||
                                        kind == Tag.BOOL || kind==Tag.TRUE || kind==Tag.FALSE||
                                        context.equals("\"")||context.equals("'")){
                                    Token token = new Token(Tag.ADD,"+",lineNum,index+1);
                                    tokens.add(token);
                                    index--;
                                    flag=0;
                                }else if (String.valueOf(c).equals("\n")){
                                    Error error = new Error("+","错误",lineNum,index+1);
                                    errors.add(error);
                                }else if (c<'0'||c>'9'){
                                    Token token = new Token(Tag.POS,"+",lineNum,index+1);
                                    tokens.add(token);
                                    index--;
                                    flag=0;
                                }
                                else {
                                    isPositive=true;
                                    begin=index-1;
                                    flag=2;
                                }
                            }
                            break;
                        case 11:
                            int si = jump_string(begin,cmmProgram);
                            if (si<cmmProgram.length()){
                                index=si;
                                String string = cmmProgram.substring(begin,index);
                                Token token = new Token(Tag.STR,string,lineNum,begin);
                                tokens.add(token);
                                Token token1 = new Token(Tag.SEPARATOR,"\"",lineNum,index+1);
                                tokens.add(token1);
                            }else if (si==cmmProgram.length()+1){
                                index = cmmProgram.length();
                                String  string = cmmProgram.substring(begin,index);
                                Error error = new Error(string,"缺少引号",lineNum,begin+1);
                                errors.add(error);
                            }
                            flag=0;
                            break;
                        case 12:
                            if (c=='&'){
                                //与运算
                                Token token = new Token(Tag.AND,"&&",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else {
                                //按位与
                                Token token=new Token(Tag.BAND,"&",lineNum,index);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }
                            break;
                        case 13:
                            if (c=='|'){
                                //或运算
                                Token token = new Token(Tag.OR,"||",lineNum,index);
                                tokens.add(token);
                                flag=0;
                            }else {
                                //按位或
                                Token token=new Token(Tag.BOR,"|",lineNum,index);
                                tokens.add(token);
                                index--;
                                flag=0;
                            }
                            break;
                    }

                }else {
                    if (c > 19967 && c < 40870 || c == '\\'
                            || c == '、' || c == '^'|| c == '《' || c == '》'
                            || c == '?' || c == '。' || c == '“'|| c == '~'
                            || c == '$' || c == '；' || c == '【'|| c == '#'
                            || c == '】' || c == '，'|| c == '@' || c == '!'
                            || c == '”' || c == '‘'|| c == '%'
                            || c == '’' || c == '？' || c == '（' || c == '）'
                            || c == '·'|| c == '`'){
                        Error error = new Error(String.valueOf(c),"是不可识别符号",lineNum,index+1);
                        errors.add(error);
                    }
                }
            }else{
                if (c=='*'){
                    flag=8;
                }else if (c=='/' && flag==8){
                    //多行注释结束
                    isNotation=false;
                    flag=0;
                }else {
                    flag=0;
                }
            }

        }

    }




}
