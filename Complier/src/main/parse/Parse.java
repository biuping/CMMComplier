package main.parse;

import main.lexer.Tag;
import main.lexer.Token;

import java.util.ArrayList;

public class Parse {
    private ArrayList<Token> tokens;
    private int index = 0;
    private Token currentToken = null;
    private int errorCount=0;
    //语法树根节点
    private static TreeNode root;

    public static TreeNode getRoot() {
        return root;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }


    public Parse(ArrayList<Token> tokens){
        this.tokens=tokens;
        if (tokens.size()!=0){
            currentToken=tokens.get(0);
        }
    }


    public TreeNode runParse(){
        root = new TreeNode("root","PROGRAM");
        for (; index < tokens.size();) {
            root.add(statement());
        }
        return root;
    }

    private void lastToken(){
        --index;
        if (index < 0) {
            currentToken = null;
        }
        currentToken = tokens.get(index);
    }

    private void nextToken(){
        ++index;
        if (index > tokens.size() - 1) {
            currentToken = null;
            if (index > tokens.size())
                --index;
            return;
        }
        currentToken = tokens.get(index);
    }

    //处理出错信息
    private PError setError(String reason){
        Token previous;
        if (index>0)
            previous = tokens.get(index-1);
        else
            previous=currentToken;
        errorCount++;
        if (currentToken != null && previous !=null
                && currentToken.getLineNum() != previous.getLineNum()) {
            return new PError(reason,previous.getLineNum(),previous.getCol());
        }else {
            try {
                if (currentToken!=null)
                    return new PError(reason,currentToken.getLineNum(),currentToken.getCol());
                else {
                    int i = index -1;
                    Token t = tokens.get(i);
                    while (t==null){
                        i--;
                        t = tokens.get(i);
                    }
                    return new PError(reason,t.getLineNum(),t.getCol());
                }
            } catch (NullPointerException e) {

                return new PError(reason,0,0);
            }
        }
    }

    /**
     *递归下降子程序法来实现语法分析
     * 故文法是LL(1)型
     * program->statement
     * statement->各种关键字阶段，标识符语句段等
     * 各段再相同道理判断语法是否正确
     */
    private TreeNode statement(){
        TreeNode temp = null;
        //int real char声明语句
        if (currentToken!=null && (currentToken.getTag()==Tag.INT
                || currentToken.getTag()==Tag.REAL ||currentToken.getTag()==Tag.BOOL
                || currentToken.getTag()==Tag.CHAR || currentToken.getTag()==Tag.STRING)){
            temp=declare(false);
        }
        //大括号代码块
        else if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("{")){
            temp=block_sta();
        }
        //if语句
        else if (currentToken!=null && currentToken.getTag()==Tag.IF){
            temp=if_sta();
        }
        //while语句
        else if (currentToken!=null && currentToken.getTag()==Tag.WHILE){
            temp = while_sta();
        }
        //for语句
        else if (currentToken!=null && currentToken.getTag()==Tag.FOR){
            temp = for_sta();
        }
        //print语句
        else if (currentToken!=null && currentToken.getTag()==Tag.PRINT){
            TreeNode printNode = new TreeNode("关键字","print",currentToken.getTag(),currentToken.getLineNum());
            printNode.add(print_sta());
            temp=printNode;
        }
        //scan语句
        else if (currentToken!=null && currentToken.getTag()==Tag.SCAN){
            temp=scan_sta(false);
        }
        //赋值语句
        else if (currentToken!=null && currentToken.getTag()==Tag.ID){
            temp=assign_sta(false);
        }else if (currentToken!=null && (currentToken.getTag()==Tag.INTNUM||currentToken.getTag()==Tag.CHAR_S||
                currentToken.getTag()==Tag.REALNUM || currentToken.getTag()==Tag.STR)){
            temp=conditionTop();
        }
        //break
        else if (currentToken!=null && currentToken.getTag()==Tag.BREAK){
            temp=break_sta();
        }
        //continue
        else if (currentToken!=null && currentToken.getTag()==Tag.CONTINUE){
            temp=continue_sta();
        }else if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
            temp=statement();
        }
        //出错
        else {
            PError error = setError("程序以错误的token"+currentToken.getContent()+"开始");
            temp=new TreeNode("Error"+errorCount,error.toString());
            nextToken();
        }

        return temp;
    }

    private TreeNode block_sta(){
        nextToken();
        TreeNode temp = new TreeNode("代码块","block",Tag.BLOCK,currentToken.getLineNum());
        temp.add(statement());
        while (true){
            if (index<tokens.size()){
                if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("}")){
                    nextToken();
                    break;
                }else {
                    temp.add((statement()));
                }
            }else {
                PError error = setError("缺少右括号");
                temp.add(new TreeNode("Error"+errorCount,error.toString()));
                break;
            }

        }
        return temp;
    }

    /**
     * if语句
     * 格式 if(condition){statement}else{statement}
     **/
    private TreeNode if_sta(){
        // if语句是否有大括号,默认为true
        boolean hasIfBrace = true;
        // else语句是否有大括号,默认为true
        boolean hasElseBrace = true;
        //确保一个if后只能有一个else
        int elseNum=0;
        //建立if函数根节点
        TreeNode ifTreeNode = new TreeNode("关键字","if",currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        //匹配if之后左括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("if之后缺少左括号\"(\"");
            ifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //括号中的条件语句加入TreeNode
        TreeNode conditionNode = new TreeNode("条件语句","condition",1,currentToken.getLineNum());
        ifTreeNode.add(conditionNode);
        conditionNode.add(condition());
        //匹配右括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
            currentToken.getContent().equals(")")){
            nextToken();
        }else {
            PError error = setError("if之后缺少右括号\")\"");
            ifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }

        //匹配左大括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals("{")){
            nextToken();
        }else {
            hasIfBrace=false;
        }


        //检测statement
        TreeNode statementNode = new TreeNode("代码段","Statements",0,currentToken.getLineNum());
        ifTreeNode.add(statementNode);
        if (hasIfBrace){
            while (currentToken != null) {
                if (!currentToken.getContent().equals("}"))
                    statementNode.add(statement());
                else if (statementNode.getChildCount() == 0) {
                    ifTreeNode.remove(ifTreeNode.getChildCount() - 1);
                    statementNode.setContent("EmptyStatement");
                    ifTreeNode.add(statementNode);
                    break;
                } else {
                    break;
                }
            }
            //匹配右大括号
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("}")){
                nextToken();
            }else {
                PError error = setError("if语句缺少右大括号\"}\"");
                ifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
            }
        }else {
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(";")){
                statementNode.setContent("EmptyStatement");
                ifTreeNode.add(statementNode);
                nextToken();
            }else if (currentToken != null && currentToken.getTag()!=Tag.ELSE)
                statementNode.add(statement());
        }
        //处理else
        while (currentToken != null && currentToken.getTag()==Tag.ELSE) {
            nextToken();
            if (currentToken != null && currentToken.getTag()==Tag.IF){
                ifTreeNode.add(elseif_sta());
            }else {
                if (elseNum<1){
                    TreeNode elseNode = new TreeNode("关键字", "else",Tag.ELSE, currentToken.getLineNum());
                    ifTreeNode.add(elseNode);
                    // 匹配左大括号{
                    if (currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("{")) {
                        nextToken();
                    } else {
                        hasElseBrace = false;
                    }
                    if (hasElseBrace) {
                        // statement
                        while (currentToken != null && !currentToken.getContent().equals("}")) {
                            elseNode.add(statement());
                        }
                        // 匹配右大括号}
                        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR
                                && currentToken.getContent().equals("}")) {
                            nextToken();
                        } else {
                            PError error = setError("else语句缺少右大括号\"}\"");
                            elseNode.add(new TreeNode("Error"+errorCount, error.toString()));
                        }
                    } else {
                        if (currentToken != null)
                            elseNode.add(statement());
                    }
                }else {
                    PError error =setError("if之后只能有一个else语句块");
                    ifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
                }
                elseNum++;
            }

        }
        return ifTreeNode;
    }

    private TreeNode elseif_sta(){
        // if语句是否有大括号,默认为true
        boolean hasIfBrace = true;
        //建立if函数根节点
        TreeNode elseifTreeNode = new TreeNode("关键字","else if",currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        //匹配if之后左括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("elseif之后缺少左括号\"(\"");
            elseifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //括号中的条件语句加入TreeNode
        TreeNode conditionNode = new TreeNode("条件语句","condition",1,currentToken.getLineNum());
        elseifTreeNode.add(conditionNode);
        conditionNode.add(condition());
        //匹配右括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")")){
            nextToken();
        }else {
            PError error = setError("elseif之后缺少右括号\")\"");
            elseifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }

        //匹配左大括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals("{")){
            nextToken();
        }else {
            hasIfBrace=false;
        }


        //检测statement
        TreeNode statementNode = new TreeNode("代码段","Statements",0,currentToken.getLineNum());
        elseifTreeNode.add(statementNode);
        if (hasIfBrace){
            while (currentToken != null) {
                if (!currentToken.getContent().equals("}"))
                    statementNode.add(statement());
                else if (statementNode.getChildCount() == 0) {
                    elseifTreeNode.remove(elseifTreeNode.getChildCount() - 1);
                    statementNode.setContent("EmptyStatement");
                    elseifTreeNode.add(statementNode);
                    break;
                } else {
                    break;
                }
            }
            //匹配右大括号
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("}")){
                nextToken();
            }else {
                PError error = setError("if语句缺少右大括号\"}\"");
                elseifTreeNode.add(new TreeNode("Error"+errorCount,error.toString()));
            }
        }else {
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";")){
                statementNode.setContent("EmptyStatement");
                elseifTreeNode.add(statementNode);
                nextToken();
            }else if (currentToken != null && currentToken.getTag()!=Tag.ELSE)
                statementNode.add(statement());
        }
        return elseifTreeNode;
    }

    /**
     * while语句
     * 格式：while(condition){statement}
     * */
    private TreeNode while_sta(){
        // 是否有大括号,默认为true
        boolean hasBrace = true;

        TreeNode whileNode = new TreeNode("关键字", "while", currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        //匹配while后的左括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("while之后缺少左括号\"(\"");
            whileNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //括号中的条件语句加入TreeNode
        TreeNode conditionNode = new TreeNode("条件语句","condition",1,currentToken.getLineNum());
        whileNode.add(conditionNode);
        conditionNode.add(condition());
        //匹配右括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")")){
            nextToken();
        }else {
            PError error = setError("while之后缺少右括号\")\"");
            whileNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //匹配左大括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals("{")){
            nextToken();
        }else {
            hasBrace=false;
        }

        //检测statement
        TreeNode statementNode = new TreeNode("代码段","Statements",0,currentToken.getLineNum());
        whileNode.add(statementNode);
        if (hasBrace){
            while (currentToken != null && !currentToken.getContent().equals("}")) {
                if (!currentToken.getContent().equals("}")){
                    statementNode.add(statement());
                } else if (statementNode.getChildCount() == 0) {
                    whileNode.remove(whileNode.getChildCount() - 1);
                    statementNode.setContent("EmptyStatement");
                    whileNode.add(statementNode);
                    break;
                } else {
                    break;
                }
            }
            //匹配右大括号
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("}")){
                nextToken();
            }else {
                PError error = setError("while循环语句缺少右大括号\"}\"");
                whileNode.add(new TreeNode("Error"+errorCount,error.toString()));
            }
        }else {
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";")){
                statementNode.setContent("EmptyStatement");
                whileNode.add(statementNode);
                nextToken();
            }else if (currentToken != null)
                statementNode.add(statement());
        }
        return whileNode;
    }



    /**
     * for语句
     * 格式：for(declare|assign ; condition ; assign ){statement}
     */
    private TreeNode for_sta(){
        boolean hasBrace = true;
        //for根节点
        TreeNode forNode = new TreeNode("关键字","for",currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        //匹配for后的左括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("for之后缺少左括号\"(\"");
            forNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //括号后的声明语句或赋值语句
        TreeNode AD_Node;
        if (currentToken!=null && (currentToken.getTag()==Tag.INT ||
                currentToken.getTag()==Tag.REAL || currentToken.getTag()==Tag.CHAR)){
            AD_Node = new TreeNode("declare","Declare",Tag.DECLARE,currentToken.getLineNum());
            AD_Node.add(declare(true));
            forNode.add(AD_Node);
        }else if (currentToken!=null && currentToken.getTag()==Tag.ID){
            AD_Node = new TreeNode("assign","Assign",Tag.ASSIGN_STA,currentToken.getLineNum());
            AD_Node.add(assign_sta(true));
            forNode.add(AD_Node);
        }else{
            PError error = setError("for声明或赋值语句出现不正确Toke "+currentToken.getContent());
            forNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //匹配第一个分号
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
            currentToken.getContent().equals(";")){
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
        }else {
            PError error = setError("for缺少分号\";\"");
            forNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //条件语句
        TreeNode conNode = new TreeNode("条件语句","Condition",1,currentToken.getLineNum());
        conNode.add(condition());
        forNode.add(conNode);

        //匹配第二个分号
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(";")){
            nextToken();
        }else {
            PError error = setError("for缺少分号\";\"");
            forNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }

        //赋值语句
        TreeNode assignNode = new TreeNode("赋值语句","Assign",3,currentToken.getLineNum());
        assignNode.add(assign_sta(true));
        forNode.add(assignNode);

        //匹配for后的右括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")")){
            nextToken();
        }else {
            //错误
            PError error = setError("for之后缺少右括号\")\"");
            forNode.add(new TreeNode("Error"+errorCount,error.toString()));
        }

        //匹配左大括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals("{")){
            nextToken();
        }else {
            hasBrace=false;
        }

        TreeNode staNode = new TreeNode("代码段","Statement",0,currentToken.getLineNum());
        forNode.add(staNode);
        if (hasBrace){
            while(currentToken!=null){
                if (!(currentToken.getTag()==Tag.SEPARATOR &&
                        currentToken.getContent().equals("}")))
                    staNode.add(statement());
                else if (staNode.getChildCount()==0){
                    forNode.remove(forNode.getChildCount()-1);
                    staNode.setContent("Empty Statement");
                    forNode.add(staNode);
                    break;
                }else {
                    break;
                }
            }
            //匹配右大括号
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("}")){
                nextToken();
            }else {
                PError error = setError("for循环语句缺少右大括号\"}\"");
                forNode.add(new TreeNode(error.toString()));
            }

        }else {
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";")){
                staNode.setContent("EmptyStatement");
                forNode.add(staNode);
                nextToken();
            }else if (currentToken != null)
                staNode.add(statement());
        }

        return forNode;

    }

    /**
     * print语句
     * 格式：print(expression);
     * 所以匹配完左括号后，要检验expression
     * */
    private TreeNode print_sta(){
        TreeNode temp = null;
        nextToken();
        //匹配print后的左括号
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR
                && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("print之后缺少左括号\"(\"");
            temp=new TreeNode("Error"+errorCount,error.toString());
        }
        TreeNode eNode = condition();
        if (temp == null){
            temp = eNode;
        }else {
            temp.add(eNode);
        }
        //匹配右括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")")){
            nextToken();
        }else {
            PError error = setError("print之后缺少右括号\")\"");
            if (temp==null){
                temp=new TreeNode("Error"+errorCount,error.toString());
            }else
                temp.add(new TreeNode("Error"+errorCount,error.toString()));
        }
        //结尾分号
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
            currentToken.getContent().equals(";")){
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
        }else {
            PError error = setError("print之后缺少分号\";\"");
            temp.add(new TreeNode("Error"+errorCount,error.toString()));
            return temp;
        }
        return temp;
    }

    /**
     * scan语句
     * 格式：scan(ID);
     * */
    private TreeNode scan_sta(boolean isDeclare){
        TreeNode temp =new TreeNode("关键字","scan",currentToken.getTag(),currentToken.getLineNum());;
        nextToken();
        //匹配scan后的左括号
        if (currentToken!=null&&currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            nextToken();
        }else {
            //错误
            PError error = setError("scan之后缺少左括号\"(\"");
            temp.add(new TreeNode("Error"+errorCount,error.toString()));
            return temp;
        }
        Token tempToken = tokens.get(index+1);
        if (tempToken!=null && !(currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")"))){
            temp.add(condition());
        }
        //匹配右括号
        if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals(")")){
            nextToken();
        }else {
            PError error = setError("scan之后缺少右括号\")\"");
            temp.add(new TreeNode("Error"+errorCount,error.toString()));
            return temp;
        }
        if (!isDeclare){
            //结尾分号
            if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";")){
                nextToken();
            }else {
                PError error = setError("scan之后缺少分号\";\"");
                temp.add(new TreeNode("Error"+errorCount,error.toString()));
                return temp;
            }
        }
        return temp;
    }

    /**
     * 赋值语句
     * 格式：ID or array = condition;
     *               =
     *          /        \
     *    ID or array    condition
     * */
    private TreeNode assign_sta(boolean isFor){
        //记录进入时的index
        int i = index;
        //创建=根结点
        TreeNode assignNode = new TreeNode("运算符","=",Tag.ASSIGN,currentToken.getLineNum());
        TreeNode idNode = new TreeNode("标识符",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
        assignNode.add(idNode);
        nextToken();

        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                currentToken.getContent().equals("[")){
            array(idNode);
        }

        if (currentToken!=null && (currentToken.getTag()==Tag.SEPARATOR
            && (!currentToken.getContent().equals("[") && !currentToken.getContent().equals(";") ))){
            PError error = setError("标识符之后出现错误分割符"+currentToken.getContent());
            idNode.add(new TreeNode("Error"+errorCount,error.toString()));
            nextToken();
        }

        //匹配赋值符号
        if (currentToken!=null && currentToken.getTag() == Tag.ASSIGN){
            //因为根结点已经默认设置了，所以直接扫描下一个token
            nextToken();
        }else if (currentToken!=null &&
                ((currentToken.getTag()>=267 && currentToken.getTag()<=282)||
                        (currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")))){
            //非赋值情况，没有=的运算
            while (i<index){
                //回退到进入token位置
                lastToken();
            }
            return conditionTop();
        }
        else {
            PError error = setError("赋值语句缺少赋值符号\"=\"");
            assignNode.add(new TreeNode("Error"+errorCount,error.toString()));
            return assignNode;
        }
        // 赋值符号后的表达式
        assignNode.add(condition());

        if (!isFor){
            //结尾分号
            if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";")){
                while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                    nextToken();
                }
            }else {
                PError error = setError("缺少分号\";\"");
                assignNode.add(new TreeNode("Error"+errorCount,error.toString()));
                return assignNode;
            }
        }
        return assignNode;
    }

    /**
     * 变量声明语句
     * 格式：(int|real|char) (声明分段declare_sub);
     * (declare_sub)可多次出现 目的在于实现同类型多个标识符声明
     */
    private TreeNode declare(boolean isFor){
        TreeNode declareNode = new TreeNode("关键字",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
        boolean isChar = false;
        if (currentToken!=null && currentToken.getTag()==Tag.CHAR)
            isChar=true;
        nextToken();
        declareNode = declare_sub(declareNode,isChar);
        //处理多个声明
        while (currentToken!=null){
            if (currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(",")){
                nextToken();
                declareNode=declare_sub(declareNode,isChar);
            }else {
                break;
            }
        }
        //分号 for分号在for_sta处理
        if (!isFor){
            if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals(";") ){
                while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                    nextToken();
                }
            }else {
                PError error = setError("声明语句缺少分号");
                declareNode.add(new TreeNode("Error"+errorCount,error.toString()));

            }
        }
        return declareNode;
    }

    /**
     * 声明分段
     * 格式：ID|array (赋值语句)
     * 赋值语句可能多次出现
     */
    private TreeNode declare_sub(TreeNode rootNode,boolean isChar){
        boolean isArray=false;
        if (currentToken!=null && currentToken.getTag()==Tag.ID){
            TreeNode idNode = new TreeNode("标识符",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            rootNode.add(idNode);
            nextToken();

            if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("[")){
                array(idNode);
                isArray=true;
            }else if (currentToken!=null
                    && !currentToken.getContent().equals(";") && !currentToken.getContent().equals(",")
                    && currentToken.getTag()!=Tag.ASSIGN){
                PError error = setError("声明语句出错，标识符之后出现不正确token "+currentToken.getContent());
                rootNode.add(new TreeNode("Error"+errorCount,error.toString()));
                nextToken();
            }
        }else {
            PError error = setError("声明语句标识符出错");
            rootNode.add(new TreeNode("Error"+errorCount,error.toString()));
            nextToken();
        }

        if (currentToken!=null && currentToken.getTag()==Tag.ASSIGN){

            TreeNode assignNode = new TreeNode("运算符","=",currentToken.getTag(),currentToken.getLineNum());
            rootNode.add(assignNode);
            nextToken();
            if (!isArray)
                assignNode.add(condition());
            else {
                if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                    currentToken.getContent().equals("{")){
                    nextToken();
                    assignNode.add(arrayDeclare());
                }else if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR
                        && currentToken.getContent().equals("\"") && isChar){
                    //char数组 字符串声明方式
                    nextToken();
                    assignNode.add(new TreeNode("字符串",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum()));
                    nextToken();
                }
                else {
                    PError error = setError("数组声明缺少左大括号\"{\"");
                    assignNode.add(new TreeNode(error.toString()));
                }

                if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR &&
                        !currentToken.getContent().equals("}") && !isChar){
                    PError error = setError("数组声明缺少右大括号\"}\"");
                    assignNode.add(new TreeNode(error.toString()));
                }else
                    nextToken();
            }
        }
        return rootNode;
    }

    /**
     * break语句
     * 格式：break;
     */
    private TreeNode break_sta(){
        TreeNode breakNode = new TreeNode("关键字","break",currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        if (currentToken!=null && !(currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";"))){
            PError error = setError("break之后缺少分号\";\"");
            breakNode.add(new TreeNode(error.toString()));
        }else{
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
        }
        return breakNode;
    }

    /**
     * continue语句
     * 格式：break;
     */
    private TreeNode continue_sta(){
        TreeNode continueNode = new TreeNode("关键字","continue",currentToken.getTag(),currentToken.getLineNum());
        nextToken();
        if (currentToken!=null && !(currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";"))){
            PError error = setError("continue之后缺少分号\";\"");
            continueNode.add(new TreeNode(error.toString()));
        }else{
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
        }

        return continueNode;
    }

    private TreeNode arrayDeclare(){
        TreeNode temp = new TreeNode("array_declare","Declare",2,currentToken.getLineNum());
        TreeNode conNodeFirst = condition();
        temp.add(conNodeFirst);
        while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR
                && currentToken.getContent().equals(",")){
            nextToken();
            if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR
                    && currentToken.getContent().equals(",")){
                PError error = setError("非法表达式的开始");
                temp.add(new TreeNode(error.toString()));
            }else if (currentToken !=null && currentToken.getTag()==Tag.SEPARATOR
                    && currentToken.getContent().equals("}"))
                break;
            else {
                if (currentToken.getTag()==Tag.SCAN){
                    TreeNode conNodeOther = scan_sta(true);
                    temp.add(conNodeOther);
                }else {
                    TreeNode conNodeOther = condition();
                    temp.add(conNodeOther);
                }

            }
        }
        return temp;
    }

    private TreeNode conditionTop(){
        TreeNode temp = condition();
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
            while(currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
        }else{
            PError error = setError("缺少分号\";\"");
            temp.add(new TreeNode(error.toString()));
        }
        return temp;
    }

    /**
    * 条件语句
    * 格式  表达式or标识符(比较操作符 表达式or标识符)？/
    * (比较操作符 表达式or标识符)可能出现多次
    * */
    private TreeNode condition(){
        TreeNode temp = firstCondition();
        while (currentToken!=null && currentToken.getTag()==Tag.OR ){
            TreeNode comparisonNode = comparison_op();
            comparisonNode.add(temp);
            temp=comparisonNode;
            temp.add(firstCondition());
        }
        return temp;
    }

    private TreeNode firstCondition(){
        TreeNode temp = secondCondition();
        while (currentToken!=null && currentToken.getTag()==Tag.AND ){
            TreeNode comparisonNode = comparison_op();
            comparisonNode.add(temp);
            temp=comparisonNode;
            temp.add(secondCondition());
        }

        return temp;
    }


    //按位或
    private TreeNode secondCondition(){
        TreeNode temp = thirdCondition();
        while (currentToken!=null && currentToken.getTag()==Tag.BOR ){
            TreeNode comparisonNode = comparison_op();
            comparisonNode.add(temp);
            temp=comparisonNode;
            temp.add(thirdCondition());
        }

        return temp;
    }

    //按位与
    private TreeNode thirdCondition(){
        TreeNode temp = fourthCondition();
        while (currentToken!=null && currentToken.getTag()==Tag.BAND ){
            TreeNode comparisonNode = comparison_op();
            comparisonNode.add(temp);
            temp=comparisonNode;
            temp.add(fourthCondition());
        }

        return temp;
    }

    private TreeNode fourthCondition(){
        TreeNode temp = expression();
        while (currentToken!=null && (currentToken.getTag()==Tag.EQ || currentToken.getTag()==Tag.GREATER
                || currentToken.getTag()==Tag.UE || currentToken.getTag()==Tag.LE
                || currentToken.getTag()==Tag.GE || currentToken.getTag()==Tag.LESS) ){
            TreeNode comparisonNode = comparison_op();
            comparisonNode.add(temp);
            temp=comparisonNode;
            temp.add(expression());
        }

        return temp;
    }

    /**
     * 表达式语句 分为加减和乘除，因为乘除优先级高于加减，故不能同时处理
     * 格式：乘除表达式(加减运算符 乘除表达式)？/
     * (加减运算符 乘除表达式)可能出现多次
     * */
    private TreeNode expression(){
        TreeNode temp =mdexperssion();

        /**
         *  树：
         *           +or-
         *      /         \
         *   乘除表达式   乘除表达式
         * */
        //判断接下来是不是加减号
        while (currentToken != null && (currentToken.getTag()==Tag.ADD
                || currentToken.getTag()==Tag.SUB)){
            TreeNode asNode = as_op();
            asNode.add(temp);
            temp=asNode;
            temp.add(mdexperssion());
        }

        return temp;
    }

    /**
     * 乘除表达式
     * 格式 因子(乘除运算符 因子)?/
     * (乘除运算符 因子)可能出现多次
     * */
    private TreeNode mdexperssion(){

        TreeNode temp = factor();
        /**
         *  树：
         *        *or/
         *      /   \
         *   因子   因子
         */

        while (currentToken!=null && (currentToken.getTag()==Tag.MUL ||
                currentToken.getTag()==Tag.DIVIDE)){
            TreeNode mdNode = md_op();
            mdNode.add(temp);
            temp=mdNode;
            temp.add(factor());
        }
        return temp;
    }

    /**
     * 因子
     * 格式：标识符|整数|实数|字符|数组|(表达式)
     * (表达式) 表示 左括号 表达式 右括号
     * */

    private TreeNode factor(){
        TreeNode temp;
        if (currentToken != null && currentToken.getTag()==Tag.INTNUM){
            temp = new TreeNode("整数",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.REALNUM){
            temp = new TreeNode("实数",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.ID){
            temp = new TreeNode("标识符",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            nextToken();
            //数组情况
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR
                    && currentToken.getContent().equals("[")){
                array(temp);
            }
        }else if (currentToken !=null && currentToken.getTag()==Tag.TRUE){
            temp = new TreeNode("布尔值", "true",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken !=null && currentToken.getTag()==Tag.FALSE){
            temp = new TreeNode("布尔值", "false",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR
                && currentToken.getContent().equals("\"")){
            nextToken();
            temp = new TreeNode("字符串",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            //下一个双引号
            nextToken();
            nextToken();
        }else if (currentToken!=null &&
                (currentToken.getTag()==Tag.NEG || currentToken.getTag()==Tag.POS)){
            temp=new TreeNode("正负号",currentToken.getContent(),currentToken.getTag(),
                    currentToken.getLineNum());
            nextToken();
            temp.add(factor());
        }
        else if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("(")){
            //匹配(表达式)情况
            nextToken();
            //表达式
            temp=condition();
            //）
            if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(")")){
                nextToken();
            }else {
                PError error = setError("表达式缺少右括号\")\"");
                return new TreeNode("Error"+errorCount,error.toString());
            }
        }
        else if (currentToken!=null && currentToken.getTag()==Tag.SCAN){
            temp=scan_sta(true);
        }
        //匹配字符
        else if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR
                && currentToken.getContent().equals("'")){
            nextToken();
            temp=new TreeNode("字符",currentToken.getContent(),currentToken.getTag(),currentToken.getLineNum());
            nextToken();
            nextToken();
        }else {
            PError error = setError("表达式因子出现错误或为空");
            if (currentToken != null &&
                    currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals(";")){
                nextToken();
            }
            temp = new TreeNode("Error"+errorCount,error.toString());
        }
        return temp;
    }


    /**
     * 数组
     * 格式：[表达式]
     * */
    private void array(TreeNode root){
        if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("[")){
            while (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("[")){
                nextToken();
                if (currentToken!=null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("]")){
                    root.add(new TreeNode("undefined","Undefined",4,currentToken.getLineNum()));
                    nextToken();
                } else {
                    //分析中括号中的表达式
                    root.add(expression());
                    //检测右中括号
                    if (currentToken != null && currentToken.getTag()==Tag.SEPARATOR && currentToken.getContent().equals("]")){
                        nextToken();
                    }else {
                        PError error = setError("缺少右中括号\"]\"");
                        root.add(new TreeNode("Error"+errorCount,error.toString()));
                    }
                }
            }
        }else{
            PError error = setError("缺少左中括号\"[\"");
            root.add(new TreeNode("Error"+errorCount,error.toString()));
        }
    }

    /**
     * 加减
     * 格式：+ or -
     * */
    private TreeNode as_op(){
        TreeNode temp;
        if (currentToken != null && currentToken.getTag()==Tag.ADD) {
            temp = new TreeNode("运算符", "+", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.SUB){
            temp=new TreeNode("运算符", "-", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else {
            PError error = setError("加减符号出错");
            return new TreeNode("Error"+errorCount,error.toString());
        }
        return temp;
    }

    /**
     * 乘除
     * 格式：* or /
     * */
    private TreeNode md_op(){
        TreeNode temp;
        if (currentToken != null && currentToken.getTag()==Tag.MUL){
            temp = new TreeNode("运算符","*",currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.DIVIDE){
            temp = new TreeNode("运算符","/",currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else{
            PError error = setError("乘除符号出错");
            return new TreeNode("Error"+errorCount,error.toString());
        }
        return temp;
    }

    /**
     * 比较运算符
     * 格式：< | > | >= | <= | == | <>
     * */
    private TreeNode comparison_op(){
        TreeNode temp;
        if (currentToken != null && currentToken.getTag()==Tag.GREATER) {
            temp = new TreeNode("运算符", ">",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.LESS){
            temp = new TreeNode("运算符", "<", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.GE){
            temp = new TreeNode("运算符", ">=",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.LE){
            temp = new TreeNode("运算符", "<=",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.EQ){
            temp = new TreeNode("运算符", "==",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.UE){
            temp = new TreeNode("运算符", "<>",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.OR){
            temp = new TreeNode("运算符", "||", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }else if (currentToken != null && currentToken.getTag()==Tag.AND){
            temp = new TreeNode("运算符", "&&", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }
        else if (currentToken != null && currentToken.getTag()==Tag.BOR){
            temp = new TreeNode("运算符", "|", currentToken.getTag(),currentToken.getLineNum());
            nextToken();
        }
        else if (currentToken != null && currentToken.getTag()==Tag.BAND){
            temp = new TreeNode("运算符", "&",currentToken.getTag(), currentToken.getLineNum());
            nextToken();
        }
        else {
            PError error = setError("无法识别符号");
            return new TreeNode("Error"+errorCount,error.toString());
        }
        return temp;
    }



}
