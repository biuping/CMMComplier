package main.semantic;

import main.lexer.Tag;
import main.parse.TreeNode;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Scanner;

public class Semantic {
    // 符号表
    private SymbolTable table = new SymbolTable();
    // 语法树
    private TreeNode root;
    // 语义分析错误个数
    private int errorNum = 0;

    // 语义分析标识符作用域
    private int level = 0;
    //scan输入
    private String input;

    private ArrayList<SError> errors = new ArrayList<SError>();

    public Semantic(TreeNode root) {
        this.root = root;
    }

    public int getErrorNum() {
        return errorNum;
    }

    public ArrayList<SError> getErrors() {
        return errors;
    }

    private void setError(String reason, int line) {
        errorNum++;
        SError error = new SError(reason, line, errorNum);
        errors.add(error);
    }

    //识别整数
    private static boolean isInteger(String s) {
        if (s.matches("^(\\-|\\+)?[\\s]*[0-9]\\d*$") || s.matches("^(\\-|\\+?)0$")
                && !s.matches("^-?0{1,}\\d+$"))
            return true;
        else
            return false;
    }

    //识别浮点数
    private static boolean isReal(String s) {
        if (s.matches("^((\\-|\\+)?[\\s]*\\d+)(\\.\\d+)+$")
                && !s.matches("^(-?0{2,}+)(\\.\\d+)+$"))
            return true;
        else
            return false;
    }

    private int judgeTag(int t1, int t2) {
        if (t1 == Tag.STRING || t2 == Tag.STRING)
            return Tag.STRING;
        else if (t1 == Tag.REAL || t2 == Tag.REAL)
            return Tag.REAL;
        else
            return Tag.INT;
    }

    private boolean isEsc_char(String s) {
        return s.equals("\\\"") || s.equals("\\\'") || s.equals("\\n") || s.equals("\\t")
                || s.equals("\\r") || s.equals("\\\\");
    }

    //判断数组赋值类型是否匹配
    private boolean judgeTypeEqual(int declareTag,int assignTag){
        if (declareTag==Tag.INT){
            return assignTag==Tag.BOOL || assignTag == Tag.CHAR_S ||
                    assignTag == Tag.INT || assignTag == Tag.CHAR ||
                    assignTag == Tag.TRUE || assignTag == Tag.FALSE ||
                    assignTag == Tag.INTNUM ;
        }else if (declareTag==Tag.REAL){
            return assignTag==Tag.INT || assignTag == Tag.CHAR_S ||
                    assignTag == Tag.INTNUM || assignTag == Tag.CHAR ||
                    assignTag == Tag.TRUE || assignTag == Tag.FALSE ||
                    assignTag==Tag.REALNUM || assignTag == Tag.REAL ||
                    assignTag==Tag.BOOL;
        }else if (declareTag==Tag.BOOL){
            return assignTag==Tag.INT || assignTag == Tag.CHAR_S ||
                    assignTag == Tag.INTNUM || assignTag == Tag.CHAR ||
                    assignTag == Tag.TRUE || assignTag == Tag.FALSE ||
                    assignTag==Tag.REALNUM || assignTag == Tag.REAL ||
                    assignTag==Tag.BOOL;
        }else if (declareTag==Tag.STRING){
            return assignTag==Tag.STRING||assignTag==Tag.STR;
        }else if (declareTag==Tag.CHAR){
            return assignTag == Tag.INTNUM || assignTag == Tag.CHAR_S ||
                    assignTag == Tag.INT || assignTag == Tag.CHAR ||
                    assignTag == Tag.TRUE || assignTag == Tag.FALSE||
                    assignTag==Tag.BOOL;
        }
        return false;
    }

    //print打印转义的真正值
    private static void printEsc(String esc){
        switch (esc){
            case "\\\"":
                System.out.println("\"");
                break;
            case "\\\'":
                System.out.println("'");
                break;
            case "\\n":
                System.out.println();
                break;
            case "\\t":
                System.out.println("\t");
                break;
            case "\\r":
                System.out.println("\r");
                break;
            case "\\\\":
                System.out.println("\\");
                break;
        }
    }

    //根据Tag返回symbol的值
    private String getSymoblValue(Symbol symbol){
        switch (symbol.getTag()){
            case Tag.INT:
                return symbol.getIntValue();
            case Tag.REAL:
                return symbol.getRealValue();
            case Tag.CHAR:
                return symbol.getCharValue();
            case Tag.BOOL:
                return symbol.getBoolValue();
            case Tag.STRING:
                return symbol.getStringValue();
            default:
                return "";
        }
    }

    //负值节点转换处理
    private String negativeHandle(TreeNode negNode){
        String content = negNode.getContent();
        switch (negNode.getTag()){
            case Tag.CHAR_S:
                char c = content.charAt(0);
                if (isEsc_char(content))
                    c=content.charAt(1);
                return String.valueOf(-(int)c);
            case Tag.TRUE:
                return "0";
            case Tag.FALSE:
                return "1";
            default:
                 return "";
        }
    }

    //输入赋值
    public synchronized void setInput(String input) {
        this.input = input;
        notify();
    }

    //读取输入
    public synchronized String readInput() {
        String result;
        try {
            while (input == null) {
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        result = input;
        input = null;
        return result;
    }

    //运行
    public void run() {
        table.removeAll();
        System.out.println("=========语义分析=========");
        try {
            statement(root);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (errorNum!=0){
                System.out.println("该程序中共有" + errorNum + "个语义错误！");
                for (int i = 0;i<errors.size();i++){
                    System.out.println(errors.get(i).toString());
                }
            }
        }
    }

    /**
     * 语义分析
     *
     * @param root 语法分析生成的语法树根节点
     */
    private void statement(TreeNode root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode currentNode = root.getChildAt(i);
            int tag = currentNode.getTag();
            if (tag == Tag.INT || tag == Tag.REAL
                    || tag == Tag.CHAR || tag == Tag.STRING || tag == Tag.BOOL) {
                //声明语句
                declare_analyze(currentNode);
            }else if(tag==Tag.ASSIGN){
                //赋值语句
                assign_analyze(currentNode);
            }else if (tag==Tag.IF){
                //进入if，作用域改变
                level++;
                if_analyze(currentNode);
                //出if，作用域改变
                level--;
                table.update(level);
            }else if (tag==Tag.FOR){
                //进入for，作用域改变
                level++;
                for_analyze(currentNode);
                //出for，作用域改变
                level--;
                table.update(level);
            }else if (tag==Tag.WHILE){
                //进入while，作用域改变
                level++;
                while_analyze(currentNode);
                //出while，作用域改变
                level--;
                table.update(level);
            }else if (tag==Tag.BLOCK){
                //进入block，作用域改变
                level++;
                statement(currentNode);
                //出block，作用域改变
                level--;
                table.update(level);
            }
            else if (tag == Tag.PRINT) {
                print_analyze(currentNode.getChildAt(0));
            }
        }
    }

    /**
     * if代码段语义分析
     *
     * @param root 不是指根节点而是if树节点
     */
    private void if_analyze(TreeNode root) {
        int count = root.getChildCount();
        TreeNode conditionNode = root.getChildAt(0);
        TreeNode statementNode = root.getChildAt(1);
        if (condition_analyze(conditionNode.getChildAt(0))){
            statement(statementNode);
        }else if (count>=3){
            for (int i=2;i<count;i++){
                if (root.getChildAt(i).getTag()==Tag.IF){//else if结点
                    level++;
                    if (elseif_analyze(root.getChildAt(i))){
                        level--;
                        table.update(level);
                        break;
                    }
                    level--;
                    table.update(level);
                }else if (root.getChildAt(i).getTag()==Tag.ELSE){
                    TreeNode elseNode = root.getChildAt(i);
                    level++;
                    statement(elseNode);
                    level--;
                    table.update(level);
                }
            }
        }else {
            //条件为假，且没有else if，else
            return;
        }
    }

    /**
     * if代码段语义分析
     *
     * @param root
     * else if树节点
     * @return 是否进入这个elseif 如果是 接下来的else if，else跳过
     */
    private boolean elseif_analyze(TreeNode root){
        TreeNode conditionNode = root.getChildAt(0);
        TreeNode statementNode = root.getChildAt(1);
        if (condition_analyze(conditionNode.getChildAt(0))){
            statement(statementNode);
            return true;
        }else {
            return false;
        }
    }

    //得到数组下标数组
    private int[] getArraySize(TreeNode root){
        int count = root.getChildCount();  //数组维度
        int[] size = new int[count];
        for (int i=0;i<count;i++){
            int tag = root.getChildAt(i).getTag();
            if (tag==Tag.INTNUM){
                int v = Integer.parseInt(root.getChildAt(i).getContent());
                if (v<1){
                    setError("数组大小必须大于0",root.getLineNum());
                    size[0]=Integer.MIN_VALUE;
                    break;
                }else
                    size[i]=v;
            }
            else if (tag==Tag.ID){
                if (checkID(root,level)){
                    Symbol tempSymbol = table.getAllLevel(root.getChildAt(i).getContent(), level);
                    if (tempSymbol.getTag()==Tag.INT){
                        int v = Integer.parseInt(tempSymbol.getIntValue());
                        if (v<=0){
                            setError("数组大小必须大于0",root.getLineNum());
                            size[0]=Integer.MIN_VALUE;
                            break;
                        }else
                            size[i]=v;
                    }
                    else if (tempSymbol.getTag()== Tag.BOOL){
                        if (tempSymbol.getBoolValue().equals("false") || tempSymbol.getBoolValue().equals("0")){
                            setError("数组大小必须大于0",root.getLineNum());
                            size[0]=Integer.MIN_VALUE;
                            break;
                        }
                    }else if (tempSymbol.getTag()==Tag.CHAR){
                        int c = (int)tempSymbol.getCharValue().charAt(0);
                        if (isEsc_char(tempSymbol.getCharValue()))
                            c=(int)tempSymbol.getCharValue().charAt(1);
                        if (c<=0){
                            setError("数组大小必须大于0",root.getLineNum());
                            size[0]=Integer.MIN_VALUE;
                            break;
                        }
                        size[i]=c;
                    }else {
                        setError("数组下标类型不正确",root.getLineNum());
                        size[0]=Integer.MIN_VALUE;
                        break;
                    }
                }
            }else if (tag == Tag.ADD || tag == Tag.SUB
                    || tag == Tag.MUL || tag == Tag.DIVIDE
                    || tag==Tag.NEG || tag==Tag.POS) {
                ExpressionPart part = expression_analyze(root.getChildAt(i));
                if (part != null) {
                    if (part.isInt()) {
                        int result = Integer.parseInt(part.getResult());
                        if (result < 1) {
                            setError("数组大小必须大于0", root.getLineNum());
                            size[0]=Integer.MIN_VALUE;
                            break;
                        } else {
                            size[i]=result;
                        }
                    } else {
                        setError("数组下标类型不正确", root.getLineNum());
                        size[0]=Integer.MIN_VALUE;
                        break;
                    }
                } else {
                    size[0] = Integer.MIN_VALUE;
                    break;
                }
            }
        }
        return size;
    }

    //计算下标数组乘积
    private int getArrIndexMul(int[] arr){
        int sum=1;
        for (int i = 0;i<arr.length;i++){
            sum*=arr[i];
        }
        return sum;
    }

    /**
     * 声明代码段语义分析
     *
     * @param root 不是指根节点而是if树节点
     */
    private void declare_analyze(TreeNode root) {
        int tag = root.getTag();
        int index = 0;
        while (index < root.getChildCount()) {
            TreeNode child = root.getChildAt(index);
            String name = child.getContent();  //变量名
            if (table.getAllLevel(name, level) == null) {
                if (child.getChildCount() == 0) {
                    //新建声明变量
                    Symbol symbol = new Symbol(name, root.getTag(), child.getLineNum(), level);
                    index++;
                    if (index < root.getChildCount()
                            && root.getChildAt(index).getTag() == Tag.ASSIGN) {
                        TreeNode valueNode = root.getChildAt(index).getChildAt(0);
                        String value = valueNode.getContent();
                        String judge = declare_sub(tag, valueNode, symbol, value);
                        if (judge == null)
                            return;
                        else
                            index++;
                    }
                    table.addSymbol(symbol);
                } else {
                    //声明数组
                    Symbol symbol = new Symbol(name, root.getTag(), child.getLineNum(), level);
                    int[] indexArray = getArraySize(child);
                    if (indexArray[0]==Integer.MIN_VALUE)
                        return;
                    int size = getArrIndexMul(indexArray);
                    symbol.setArraySize(indexArray);
                    table.addSymbol(symbol);
                    index++;
                    if (index < root.getChildCount() &&
                            root.getChildAt(index).getTag() == Tag.ASSIGN) {
                        TreeNode items = root.getChildAt(index).getChildAt(0);
                        int count = items.getChildCount();
                        if (count <= size) {
                            for (int j = 0; j < count; j++) {
                                //大括号声明的数组元素
                                TreeNode item = items.getChildAt(j);  //赋值的值结点
                                String itemName = child.getContent() + "@" + j;
                                Symbol itemSymbol = new Symbol(itemName, root.getTag(),
                                        items.getLineNum(), level);
                                String value = item.getContent();
                                String judge = declare_sub(tag, item, itemSymbol, value);
                                if (judge == null)
                                    return;
                                else
                                    table.addSymbol(itemSymbol);
                            }
                            if (count<size){
                                for (int j = count;j<size;j++){
                                    TreeNode item = new TreeNode();
                                    switch (tag){
                                        case Tag.INT:
                                            item.setTag(Tag.INTNUM);
                                            item.setContent(String.valueOf(Integer.MIN_VALUE));
                                            break;
                                        case Tag.REAL:
                                            item.setTag(Tag.REALNUM);
                                            item.setContent(String.valueOf(Double.MIN_VALUE));
                                            break;
                                        case Tag.CHAR:
                                            item.setTag(Tag.CHAR_S);
                                            item.setContent(String.valueOf(Character.MIN_VALUE));
                                            break;
                                        case Tag.BOOL:
                                            item.setTag(Tag.FALSE);
                                            item.setContent("0");
                                            break;
                                        case Tag.STRING:
                                            item.setTag(Tag.STR);
                                            item.setContent("");
                                            break;
                                    }
                                    item.setLineNum(root.getLineNum());
                                    String itemName = child.getContent() + "@" + j;
                                    Symbol itemSymbol = new Symbol(itemName,root.getTag(),items.getLineNum(), level);
                                    String value = item.getContent();
                                    String judge = declare_sub(tag, item, itemSymbol, value);
                                    if (judge == null)
                                        return;
                                    else
                                        table.addSymbol(itemSymbol);
                                }
                            }
                        } else {
                            ++index;
                            setError("数组大小超过声明大小",root.getLineNum());
                            return;
                        }
                    }else {
                        //默认值
                        for (int i=0;i<size;i++){
                            String itemName = child.getContent() + "@" + i;
                            Symbol itemSymbol = new Symbol(itemName, root.getTag(),
                                    child.getLineNum(), level);
                            switch (tag){
                                case Tag.INT:
                                    itemSymbol.setIntValue(String.valueOf(Integer.MIN_VALUE));
                                    itemSymbol.setRealValue(String.valueOf(Integer.MIN_VALUE));
                                    break;
                                case Tag.REAL:
                                    itemSymbol.setRealValue(String.valueOf(Double.MIN_VALUE));
                                    break;
                                case Tag.CHAR:
                                    itemSymbol.setCharValue(String.valueOf(Character.MIN_VALUE));
                                    break;
                                case Tag.BOOL:
                                    itemSymbol.setBoolValue("0");
                                    break;
                                case Tag.STRING:
                                    itemSymbol.setStringValue(String.valueOf(Character.MIN_VALUE));
                                    break;
                            }
                            table.addSymbol(itemSymbol);
                        }
                    }
                    index++;
                }

            } else {
                setError(name + "已经被声明", child.getLineNum());
                return;
            }
        }
    }

    private String declare_sub(int tag, TreeNode valueNode, Symbol symbol, String value) {
        if (tag == Tag.INT) { //int 变量
            if (valueNode.getTag() == Tag.INTNUM) {
                symbol.setIntValue(value);
                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
            } else if (valueNode.getTag() == Tag.REALNUM) {
                setError("不能将real数值赋值给int型变量", valueNode.getLineNum());
            } else if (valueNode.getTag() == Tag.CHAR_S){
                char c ;
                if (isEsc_char(valueNode.getContent()))
                    c=valueNode.getContent().charAt(1);
                else
                    c=valueNode.getContent().charAt(0);
                symbol.setIntValue(String.valueOf((int)c));
                symbol.setRealValue(String.valueOf((double)c));
            }
            else if (valueNode.getTag() == Tag.STR)
                setError("不能将string字符串赋值给int型变量", valueNode.getLineNum());
            else if (valueNode.getTag() == Tag.TRUE ) {
                symbol.setIntValue(String.valueOf(1));
                symbol.setRealValue(String.valueOf((double)1));
            }else if (valueNode.getTag() == Tag.FALSE){
                symbol.setIntValue(String.valueOf(0));
                symbol.setRealValue(String.valueOf((double)0));
            }else if (valueNode.getTag()==Tag.SCAN){
                String input = scan_analyze(valueNode,symbol.getName());
                if (input!=null){
                    if (isInteger(input)){
                        symbol.setIntValue(String.valueOf(Integer.parseInt(input)));
                        symbol.setRealValue(String.valueOf(Double.parseDouble(input)));
                    }else if (input.length()==1){
                        int i = (int)input.charAt(0);
                        symbol.setIntValue(String.valueOf(i));
                        symbol.setRealValue(String.valueOf(i));
                    }else if (input.equals("true")){
                        symbol.setIntValue(String.valueOf(1));
                        symbol.setRealValue(String.valueOf((double)1));
                    }else if (input.equals("false")){
                        symbol.setIntValue(String.valueOf(0));
                        symbol.setRealValue(String.valueOf((double)0));
                    }else {
                        setError("类型不匹配，不能赋值给int类型的变量",valueNode.getLineNum());
                    }
                }
            }
            else if (valueNode.getTag() == Tag.ID) {
                if (checkID(valueNode, level)) {
                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(), level);
                    if (idSymbol.getTag() == Tag.INT) {
                        symbol.setIntValue(idSymbol.getIntValue());
                        symbol.setRealValue(String.
                                valueOf(Double.parseDouble(idSymbol.getIntValue())));
                    } else if (idSymbol.getTag() == Tag.REAL)
                        setError("不能将real数值赋值给int型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.CHAR) {
                        char c ;
                        if (isEsc_char(idSymbol.getCharValue()))
                            c=idSymbol.getCharValue().charAt(1);
                        else
                            c=idSymbol.getCharValue().charAt(0);
                        symbol.setIntValue(String.valueOf((int)c));
                        symbol.setRealValue(String.valueOf((double)c));
                    }
                    else if (idSymbol.getTag() == Tag.STRING)
                        setError("不能将string字符串赋值给int型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.BOOL) {
                        if (idSymbol.getBoolValue().equals("0")){
                            symbol.setIntValue(String.valueOf(0));
                            symbol.setRealValue(String.valueOf((double)0));
                        }else {
                            symbol.setIntValue(String.valueOf(1));
                            symbol.setRealValue(String.valueOf((double)1));
                        }
                    }
                } else
                    return null;
            } else if (valueNode.getTag() == Tag.ADD || valueNode.getTag() == Tag.SUB
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE
                    || valueNode.getTag()==Tag.NEG ||valueNode.getTag()==Tag.POS) {
                ExpressionPart part = expression_analyze(valueNode);
                String result;
                if (part==null){
                    return null;
                }else {
                    result = part.getResult();
                }
                if (result != null) {
                    if (isInteger(result) && part.isInt()) {
                        symbol.setIntValue(result);
                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                    } else if (isReal(result) && !part.isString()) {
                        setError("不能将real数值赋值给int型变量", valueNode.getLineNum());
                        return null;
                    } else if (part.isString()) {
                        setError("不能将字符串赋值给int型变量", valueNode.getLineNum());
                        return null;
                    } else
                        return null;
                }
            } else if (valueNode.getTag() == Tag.EQ || valueNode.getTag() == Tag.LE || valueNode.getTag() == Tag.GE || valueNode.getTag() == Tag.UE
                    || valueNode.getTag() == Tag.LESS || valueNode.getTag() == Tag.GREATER ||
                    valueNode.getTag() == Tag.AND || valueNode.getTag() == Tag.OR) {
                if (condition_analyze(valueNode)) {
                    symbol.setIntValue("1");
                    symbol.setRealValue(String.valueOf(Double.parseDouble("1")));
                } else {
                    symbol.setIntValue("0");
                    symbol.setRealValue(String.valueOf(Double.parseDouble("0")));
                }
            }
        } else if (tag == Tag.REAL) {  //real 声明
            if (valueNode.getTag() == Tag.INTNUM) {
                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
            } else if (valueNode.getTag() == Tag.REALNUM) {
                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
            } else if (valueNode.getTag() == Tag.CHAR_S) {
                char c ;
                if (isEsc_char(valueNode.getContent()))
                    c=valueNode.getContent().charAt(1);
                else
                    c=valueNode.getContent().charAt(0);
                symbol.setRealValue(String.valueOf((double)c));
            }else if (valueNode.getTag() == Tag.STR)
                setError("不能将string字符串赋值给real型变量", valueNode.getLineNum());
            else if (valueNode.getTag() == Tag.TRUE )
                symbol.setRealValue("1");
            else if ( valueNode.getTag() == Tag.FALSE)
                symbol.setRealValue("0");
            else if (valueNode.getTag()==Tag.SCAN){
                String input = scan_analyze(valueNode,symbol.getName());
                if (input!=null){
                    if (isInteger(input)){
                        symbol.setRealValue(String.valueOf(Double.parseDouble(input)));
                    }else if (isReal(input)){
                        symbol.setRealValue(input);
                    }else if (input.length()==1){
                        int i = (int) input.charAt(0);
                        symbol.setRealValue(String.valueOf((double)i));
                    }else if (input.equals("true")){
                        symbol.setRealValue(String.valueOf((double)1));
                    }else if (input.equals("false")){
                        symbol.setRealValue(String.valueOf((double)0));
                    }else {
                        setError("类型不匹配，不能赋值给real类型的变量",valueNode.getLineNum());
                    }
                }
            }
            else if (valueNode.getTag() == Tag.ID) {
                if (checkID(valueNode, level)) {
                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(), level);
                    if (idSymbol.getTag() == Tag.INT || idSymbol.getTag() == Tag.REAL) {
                        symbol.setRealValue(String.
                                valueOf(Double.parseDouble(idSymbol.getRealValue())));
                    } else if (idSymbol.getTag() == Tag.CHAR) {
                        char c ;
                        if (isEsc_char(idSymbol.getCharValue()))
                            c=idSymbol.getCharValue().charAt(1);
                        else
                            c=idSymbol.getCharValue().charAt(0);
                        symbol.setRealValue(String.valueOf((double)c));
                    }
                    else if (idSymbol.getTag() == Tag.STRING)
                        setError("不能将string字符串赋值给real型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.BOOL) {
                        symbol.setRealValue(idSymbol.getBoolValue());
                    }
                } else
                    return null;
            } else if (valueNode.getTag() == Tag.ADD || valueNode.getTag() == Tag.SUB
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE
                    || valueNode.getTag()==Tag.NEG ||valueNode.getTag()==Tag.POS) {
                ExpressionPart part = expression_analyze(valueNode);
                String result;
                if (part==null){
                    return null;
                }else {
                    result = part.getResult();
                }
                if (result != null) {
                    if (isInteger(result) && part.isInt()) {
                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                    } else if (isReal(result) && !part.isString()) {
                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                    } else if (part.isString()) {
                        setError("不能将字符串赋值给real型变量", valueNode.getLineNum());
                        return null;
                    } else
                        return null;
                }
            } else if (valueNode.getTag() == Tag.EQ || valueNode.getTag() == Tag.LE || valueNode.getTag() == Tag.GE || valueNode.getTag() == Tag.UE
                    || valueNode.getTag() == Tag.LESS || valueNode.getTag() == Tag.GREATER ||
                    valueNode.getTag() == Tag.AND || valueNode.getTag() == Tag.OR) {
                if (condition_analyze(valueNode)) {
                    symbol.setRealValue(String.valueOf(Double.parseDouble("1")));
                } else {
                    symbol.setRealValue(String.valueOf(Double.parseDouble("0")));
                }
            }
        } else if (tag == Tag.CHAR) {  //real 声明
            if (valueNode.getTag() == Tag.INTNUM) {
                char c = (char) Integer.parseInt(valueNode.getContent());
                symbol.setCharValue(String.valueOf(c));
            } else if (valueNode.getTag() == Tag.REAL) {
                setError("不能将real数值赋值给char型变量", valueNode.getLineNum());
            } else if (valueNode.getTag() == Tag.CHAR_S)
                symbol.setCharValue(value);
            else if (valueNode.getTag() == Tag.STR)
                setError("不能将string字符串赋值给char型变量", valueNode.getLineNum());
            else if (valueNode.getTag()==Tag.SCAN){
                String input = scan_analyze(valueNode,symbol.getName());
                if (input!=null){
                    if (isInteger(input)){
                        int i = Integer.parseInt(input);
                        symbol.setCharValue(String.valueOf((char)i));
                    }else if (input.length()==1){
                        symbol.setCharValue(input);
                    }else if (input.equals("true")){
                        symbol.setCharValue(String.valueOf((char) 1));
                    }else if (input.equals("false")){
                        symbol.setCharValue(String.valueOf((char) 0));
                    }else {
                        setError("类型不匹配，不能赋值给char类型的变量",valueNode.getLineNum());
                    }
                }
            }
            else if (valueNode.getTag() == Tag.TRUE || valueNode.getTag() == Tag.FALSE) {
                if (valueNode.getTag() == Tag.TRUE)
                    symbol.setCharValue(String.valueOf((char) 1));
                else
                    symbol.setCharValue(String.valueOf((char) 0));
            } else if (valueNode.getTag() == Tag.ID) {
                if (checkID(valueNode, level)) {
                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(), level);
                    if (idSymbol.getTag() == Tag.INT) {
                        char c = (char) Integer.parseInt(idSymbol.getIntValue());
                        symbol.setCharValue(String.valueOf(c));
                    } else if (idSymbol.getTag() == Tag.REAL) {
                        setError("不能将real型变量字符赋值给char型变量", valueNode.getLineNum());
                    } else if (idSymbol.getTag() == Tag.CHAR)
                        symbol.setCharValue(idSymbol.getCharValue());
                    else if (idSymbol.getTag() == Tag.STRING)
                        setError("不能将string变量赋值给char型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.BOOL) {
                        if (idSymbol.getBoolValue().equals("1"))
                            symbol.setCharValue(String.valueOf((char) 1));
                        else
                            symbol.setCharValue(String.valueOf((char) 0));
                    }
                } else
                    return null;
            } else if (valueNode.getTag() == Tag.EQ || valueNode.getTag() == Tag.LE || valueNode.getTag() == Tag.GE || valueNode.getTag() == Tag.UE
                    || valueNode.getTag() == Tag.LESS || valueNode.getTag() == Tag.GREATER ||
                    valueNode.getTag() == Tag.AND || valueNode.getTag() == Tag.OR) {
                if (condition_analyze(valueNode)) {
                    symbol.setCharValue(String.valueOf(1));
                } else {
                    symbol.setCharValue(String.valueOf(0));
                }
            } else if (valueNode.getTag() == Tag.ADD || valueNode.getTag() == Tag.SUB
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE
                    || valueNode.getTag()==Tag.NEG ||valueNode.getTag()==Tag.POS) {
                ExpressionPart exp = expression_analyze(valueNode);
                assert exp != null;
                if (exp.getTag() == Tag.INT) {
                    symbol.setCharValue(String.valueOf((char) Integer.parseInt(exp.getResult())));
                } else {
                    setError("类型不匹配，不能赋值给char型变量", valueNode.getLineNum());
                }
            }
        } else if (tag == Tag.STRING) {  //real 声明
            if (valueNode.getTag() == Tag.INTNUM) {
                setError("不能将int数值赋值给string型变量", valueNode.getLineNum());
            } else if (valueNode.getTag() == Tag.REAL) {
                setError("不能将real数值赋值给string型变量", valueNode.getLineNum());
            } else if (valueNode.getTag() == Tag.CHAR_S)
                setError("不能将char字符赋值给string型变量", valueNode.getLineNum());
            else if (valueNode.getTag() == Tag.STR)
                symbol.setStringValue(value);
            else if (valueNode.getTag() == Tag.TRUE || valueNode.getTag() == Tag.FALSE)
                setError("不能将布尔值赋值给string型变量", valueNode.getLineNum());
            else if (valueNode.getTag()==Tag.SCAN){
                String input = scan_analyze(valueNode,symbol.getName());
                if (input!=null){
                    symbol.setStringValue(input);
                }
            }
            else if (valueNode.getTag() == Tag.ID) {
                if (checkID(valueNode, level)) {
                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(), level);
                    if (idSymbol.getTag() == Tag.INT) {
                        setError("不能将int型变量赋值给string型变量", valueNode.getLineNum());
                    } else if (idSymbol.getTag() == Tag.REAL) {
                        setError("不能将real型变量赋值给string型变量", valueNode.getLineNum());
                    } else if (idSymbol.getTag() == Tag.CHAR)
                        setError("不能将char变量赋值给string型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.STRING)
                        symbol.setStringValue(idSymbol.getStringValue());
                    else if (idSymbol.getTag() == Tag.BOOL)
                        setError("不能将布尔型变量赋值给string型变量", valueNode.getLineNum());
                } else
                    return null;
            } else if (valueNode.getTag() == Tag.ADD) {
                ExpressionPart part = expression_analyze(valueNode);
                if (part!=null)
                    symbol.setStringValue(part.getResult());
                else
                    return null;
            } else {
                setError("除加法之外的运算表达式不能赋值给string型变量", valueNode.getLineNum());
            }
        } else if (tag == Tag.BOOL) {
            if (valueNode.getTag() == Tag.INTNUM) {
                if (Integer.parseInt(valueNode.getContent()) == 0)
                    symbol.setBoolValue("0");
                else
                    symbol.setBoolValue("1");
            } else if (valueNode.getTag() == Tag.REAL) {
                if (Double.parseDouble(valueNode.getContent()) == 0)
                    symbol.setBoolValue("0");
                else
                    symbol.setBoolValue("1");
            } else if (valueNode.getTag() == Tag.CHAR_S) {
                int i;
                if (isEsc_char(valueNode.getContent()))
                    i = (int) valueNode.getContent().charAt(1);
                else
                    i = (int) valueNode.getContent().charAt(0);
                if (i == 0)
                    symbol.setBoolValue("0");
                else
                    symbol.setBoolValue("1");
            } else if (valueNode.getTag() == Tag.STR)
                setError("不能将string字符串赋值给布尔型变量", valueNode.getLineNum());
            else if (valueNode.getTag() == Tag.TRUE )
                symbol.setBoolValue("1");
            else if (valueNode.getTag() == Tag.FALSE)
                symbol.setBoolValue("0");
            else if (valueNode.getTag()==Tag.SCAN){
                String input = scan_analyze(valueNode,symbol.getName());
                if (input!=null){
                    if (isInteger(input)){
                        int i = Integer.parseInt(input);
                        if (i==0){
                            symbol.setBoolValue("0");
                        }else
                            symbol.setBoolValue("1");
                    }else if (input.length()==1){
                        int i = (int)input.charAt(0);
                        if (i==0){
                            symbol.setBoolValue("0");
                        }else
                            symbol.setBoolValue("1");
                    }else if (input.equals("true")){
                        symbol.setBoolValue("1");
                    }else if (input.equals("false")){
                        symbol.setBoolValue("0");
                    }else if (isReal(input)){
                        double d = Double.parseDouble(input);
                        if (d==0)
                            symbol.setBoolValue("0");
                        else
                            symbol.setBoolValue("1");
                    }
                    else {
                        setError("类型不匹配，不能赋值给布尔类型的变量",valueNode.getLineNum());
                    }
                }
            }
            else if (valueNode.getTag() == Tag.ID) {
                if (checkID(valueNode, level)) {
                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(), level);
                    if (idSymbol.getTag() == Tag.INT) {
                        if (Integer.parseInt(idSymbol.getIntValue()) == 0)
                            symbol.setBoolValue("0");
                        else
                            symbol.setBoolValue("1");
                    } else if (idSymbol.getTag() == Tag.REAL) {
                        if (Double.parseDouble(idSymbol.getRealValue()) == 0)
                            symbol.setBoolValue("0");
                        else
                            symbol.setBoolValue("1");
                    } else if (idSymbol.getTag() == Tag.CHAR) {
                        int i;
                        if (isEsc_char(idSymbol.getCharValue()))
                            i = (int) idSymbol.getCharValue().charAt(1);
                        else
                            i = (int) idSymbol.getCharValue().charAt(0);
                        if (i == 0)
                            symbol.setBoolValue("0");
                        else
                            symbol.setBoolValue("1");
                    } else if (idSymbol.getTag() == Tag.STRING)
                        setError("不能将string变量赋值给布尔型变量", valueNode.getLineNum());
                    else if (idSymbol.getTag() == Tag.BOOL)
                        symbol.setBoolValue(idSymbol.getBoolValue());
                } else
                    return null;
            } else if (valueNode.getTag() == Tag.ADD || valueNode.getTag() == Tag.SUB
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE
                    || valueNode.getTag()==Tag.NEG ||valueNode.getTag()==Tag.POS) {
                ExpressionPart exp = expression_analyze(valueNode);
                assert exp != null;
                if (exp.getTag() == Tag.STRING) {
                    setError("不能将string变量赋值给布尔型变量", valueNode.getLineNum());
                } else if (exp.getTag() == Tag.INT) {
                    if (Integer.parseInt(exp.getResult()) == 0)
                        symbol.setBoolValue("0");
                    else
                        symbol.setBoolValue("1");
                } else {
                    if (Double.parseDouble(exp.getResult()) == 0)
                        symbol.setBoolValue("0");
                    else
                        symbol.setBoolValue("1");
                }
            } else if (valueNode.getTag() == Tag.EQ || valueNode.getTag() == Tag.UE
                    || valueNode.getTag() == Tag.GE || valueNode.getTag() == Tag.LE
                    || valueNode.getTag() == Tag.LESS || valueNode.getTag() == Tag.GREATER
                    || valueNode.getTag() == Tag.AND || valueNode.getTag() == Tag.OR) {
                boolean result = condition_analyze(valueNode);
                if (result)
                    symbol.setBoolValue("true");
                else
                    symbol.setBoolValue("false");
            }
        }
        return "";
    }

    /**
     * assign语句分析
     *
     * @param root
     * 语法树中赋值(=)语句结点
     */
    private void assign_analyze(TreeNode root){
        //被赋值的节点
        TreeNode leftNode = root.getChildAt(0);
        String content = leftNode.getContent();
        if (table.getAllLevel(content,level) != null){
            if (leftNode.getChildCount()!=0){
                String array = array_analyze(leftNode,
                        table.getAllLevel(content,level).getArraySize());
                if (array!=null)
                    content+="@"+array;
                else
                    return;
            }
        }else {
            setError("变量"+content+"未声明",leftNode.getLineNum());
            return;
        }
        //已经声明，找到symbol
        Symbol symbol = table.getAllLevel(content,level);
        int leftTag = symbol.getTag();
        //赋予的值
        TreeNode valueNode = root.getChildAt(1);
        String valueContent = valueNode.getContent();
        String s = declare_sub(leftTag,valueNode,symbol,valueContent);
    }

    /**
     * print语句分析
     *
     * @param root print节点
     */
    private void print_analyze(TreeNode root) {
        int tag = root.getTag();
        String content = root.getContent();
        if (tag == Tag.INTNUM || tag == Tag.REALNUM ||
                tag == Tag.TRUE || tag == Tag.FALSE) {
            System.out.println(root.getContent());
        }else if (tag == Tag.CHAR_S || tag == Tag.STR ){
            if (isEsc_char(root.getContent()))
                printEsc(root.getContent());
            else
                System.out.println(root.getContent());
        }
        else if (tag == Tag.ID) {
            if (checkID(root, level)) {
                if (root.getChildCount() > 0) {
                    String array = array_analyze(root,
                            table.getAllLevel(content, level).getArraySize());
                    if (array != null)
                        content += "@" + array;
                    else
                        return;
                }
                Symbol symbol = table.getAllLevel(content, level);
                if (symbol.getTag() == Tag.INT )
                    System.out.println(symbol.getIntValue());
                else if (symbol.getTag() == Tag.REAL ){
                    System.out.println(symbol.getRealValue());
                }else if (symbol.getTag()==Tag.STRING){
                    if (isEsc_char(symbol.getStringValue()))
                        printEsc(symbol.getStringValue());
                    else
                        System.out.println(symbol.getStringValue());
                }else if (symbol.getTag()==Tag.CHAR){
                    if (isEsc_char(symbol.getCharValue()))
                        printEsc(symbol.getCharValue());
                    else
                        System.out.println(symbol.getCharValue());
                }else if (symbol.getTag()==Tag.BOOL){
                    if (symbol.getBoolValue().equals("0") || symbol.getBoolValue().equals("false"))
                        System.out.println("false");
                    else
                        System.out.println("true");
                }
            }else{
                if (root.getChildCount()>0)
                    setError("数组"+root.getContent()+
                            "["+root.getChildAt(0).getContent()+"]未声明",root.getLineNum());
                else
                    setError("标识符"+root.getContent()+"未声明",root.getLineNum());
            }
        }else if (tag==Tag.ADD || tag==Tag.SUB ||
                tag==Tag.MUL ||tag==Tag.DIVIDE){
            ExpressionPart exp = expression_analyze(root);
            if (exp!=null)
                System.out.println(exp.getResult());
        }else if (tag == Tag.EQ || tag == Tag.LE || tag == Tag.GE || tag == Tag.UE
                || tag == Tag.LESS || tag == Tag.GREATER ||
                tag == Tag.AND || tag == Tag.OR){
            if (condition_analyze(root))
                System.out.println("true");
            else
                System.out.println("false");
        }
    }

    /**
     * 分析for语句
     * @param root
     * for语句结点
     */
    private void for_analyze(TreeNode root){
        //以声明或赋值方法确定for循环变量的结点
        TreeNode initNode = root.getChildAt(0);
        //条件语句结点
        TreeNode conditionNode = root.getChildAt(1);
        //变量变化语句结点
        TreeNode changeNode = root.getChildAt(2);
        //for代码块语句结点
        TreeNode statementNode = root.getChildAt(3);
        if (initNode.getTag()==Tag.DECLARE){
            declare_analyze(initNode.getChildAt(0));
        }else if (initNode.getTag()==Tag.ASSIGN_STA){
            assign_analyze(initNode.getChildAt(0));
        }
        while (condition_analyze(conditionNode.getChildAt(0))){
            level++;
            statement(statementNode);
            level--;
            table.update(level);
            assign_analyze(changeNode.getChildAt(0));
        }
    }

    /**
     * while语句分析
     *
     * @param root
     * while节点
     */
    private void while_analyze(TreeNode root){
        TreeNode conditionNode = root.getChildAt(0);
        TreeNode statementNode = root.getChildAt(1);
        while (condition_analyze(conditionNode.getChildAt(0))){
            level++;
            statement(statementNode);
            level--;
            table.update(level);
        }
    }

    /**
     * scan语句分析
     *
     * @param root
     * while节点
     */
    private String scan_analyze(TreeNode root,String name){
        String input = "";
        if (root.getChildCount()==0){
            System.out.print("输入"+name+":");
            Scanner scanner = new Scanner(System.in);
            input = scanner.nextLine();
            return input;
        }else {
            //打开文件
            TreeNode child = root.getChildAt(0);
            String content = child.getContent();
            String filePath="D:\\test.txt";
            if (child.getTag()==Tag.ID){
                if (checkID(child,level)){
                    if (root.getChildCount() != 0) {
                        String str = array_analyze(root,
                                table.getAllLevel(content, level).getArraySize());
                        if (str != null)
                            content += "@" + str;
                        else
                            return null;
                    }
                    Symbol symbol = table.getAllLevel(content, level);
                    if (symbol.getTag()==Tag.STRING){
                        filePath=symbol.getStringValue();
                    }else {
                        setError("scan无法读取此类型",child.getLineNum());
                        return null;
                    }
                }
            }else if (child.getTag()==Tag.STR){
                filePath=child.getContent();
            }
            File file = new File(filePath);
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(new FileInputStream(file),"gbk");
                BufferedReader br = new BufferedReader(reader);
                String line = "";
                while ((line=br.readLine())!=null){
                    input+=line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return input;
        }
    }

    /**
     * 分析条件语句
     *
     * @param root 条件语句根节点
     * @return 条件语句结果
     */
    private boolean condition_analyze(TreeNode root) {
        String content = root.getContent();
        int tag = root.getTag();
        if (tag == Tag.INTNUM) {
            return Integer.parseInt(content) != 0;
        } else if (tag == Tag.REALNUM) {
            return !(Double.parseDouble(content) == 0);
        } else if (tag == Tag.CHAR_S) {
            char c;
            if (isEsc_char(content))
                c = content.charAt(1);
            else
                c = content.charAt(0);
            int i = (int) c;
            return i != 0;
        } else if (tag == Tag.TRUE) {
            return true;
        } else if (tag == Tag.FALSE) {
            return false;
        } else if (tag == Tag.ID) {
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {
                    String str = array_analyze(root,
                            table.getAllLevel(content, level).getArraySize());
                    if (str != null)
                        content += "@" + str;
                    else
                        return false;
                }
                Symbol symbol = table.getAllLevel(content, level);
                if (symbol.getTag() == Tag.BOOL) {
                    return symbol.getBoolValue().equals("1");
                } else if (symbol.getTag() == Tag.INT) {
                    int i = Integer.parseInt(symbol.getIntValue());
                    return i != 0;
                } else if (symbol.getTag() == Tag.REAL) {
                    double b = Double.parseDouble(symbol.getRealValue());
                    return !(b == 0);
                } else if (symbol.getTag() == Tag.CHAR) {
                    char c;
                    if (isEsc_char(content))
                        c = content.charAt(1);
                    else
                        c = content.charAt(0);
                    int i = (int) c;
                    return i != 0;
                } else {
                    setError("不能将变量" + content + "作为判断条件", root.getLineNum());
                }
            } else return false;
        } else if (tag == Tag.EQ || tag == Tag.LE || tag == Tag.GE || tag == Tag.UE
                || tag == Tag.LESS || tag == Tag.GREATER ||
                tag == Tag.AND || tag == Tag.OR) {
            Element[] children = new Element[2];
            for (int i = 0; i < root.getChildCount(); i++) {
                int childTag = root.getChildAt(i).getTag();
                String childContent = root.getChildAt(i).getContent();
                if (childTag == Tag.OR || childTag == Tag.AND || childTag == Tag.EQ
                        || childTag == Tag.LE || childTag == Tag.GE || childTag == Tag.UE
                        || childTag == Tag.LESS || childTag == Tag.GREATER) {
                    if (condition_analyze(root.getChildAt(i)))
                        children[i] = new Element(Tag.INT, "1");
                    else
                        children[i] = new Element(Tag.INT, "0");
                } else if (childTag == Tag.TRUE) {
                    children[i] = new Element(Tag.INT, "1");
                } else if (childTag == Tag.FALSE) {
                    children[i] = new Element(Tag.INT, "0");
                } else if (childTag == Tag.INTNUM || childTag == Tag.REALNUM
                        || childTag == Tag.CHAR_S || childTag == Tag.STR) {
                    if (childTag == Tag.CHAR_S) {
                        char c;
                        if (isEsc_char(childContent))
                            c = childContent.charAt(1);
                        else
                            c = childContent.charAt(0);
                        int cToi = (int) c;
                        children[i] = new Element(Tag.INT, String.valueOf(cToi));
                    } else if (childTag == Tag.REALNUM) {
                        children[i] = new Element(Tag.REAL, childContent);
                    } else if (childTag == Tag.INTNUM)
                        children[i] = new Element(Tag.INT, childContent);
                    else
                        children[i] = new Element(Tag.STRING, childContent);
                } else if (childTag == Tag.ID) {
                    if (checkID(root.getChildAt(i), level)) {
                        if (root.getChildAt(i).getChildCount() != 0) {
                            String arrStr = array_analyze(root.getChildAt(i),
                                    table.getAllLevel(childContent, level).getArraySize());
                            if (arrStr != null)
                                childContent += "@" + arrStr;
                            else return false;
                        }
                        Symbol symbol = table.getAllLevel(childContent, level);
                        if (symbol.getTag() == Tag.CHAR) {
                            char c = symbol.getCharValue().charAt(0);
                            int cToi = (int) c;
                            children[i] = new Element(Tag.INT, String.valueOf(cToi));
                        } else if (symbol.getTag() == Tag.INT) {
                            children[i] = new Element(Tag.INT, symbol.getIntValue());
                        } else if (symbol.getTag() == Tag.BOOL) {
                            if (symbol.getBoolValue().equals("false") ||
                                    symbol.getBoolValue().equals("0"))
                                children[i] = new Element(Tag.INT, "0");
                            else
                                children[i] = new Element(Tag.INT, "1");
                        } else if (symbol.getTag() == Tag.STRING)
                            children[i] = new Element(Tag.STRING, symbol.getStringValue());
                        else
                            children[i] = new Element(Tag.REAL, symbol.getRealValue());
                    } else
                        return false;
                } else if (childTag == Tag.ADD || childTag == Tag.SUB
                        || childTag == Tag.MUL || childTag == Tag.DIVIDE
                        || childTag==Tag.NEG || childTag==Tag.POS ) {
                    ExpressionPart exp = expression_analyze(root.getChildAt(i));
                    if (exp != null)
                        children[i] = new Element(exp.getTag(), exp.getResult());
                    else
                        return false;
                } else if (childTag==Tag.SCAN) {
                    String input = scan_analyze(root.getChildAt(i),"");
                    if (input != null) {
                        if (isInteger(input)) {
                            children[i]=new Element(Tag.INT,input);
                        } else if (isReal(input)) {
                            children[i]=new Element(Tag.REAL,input);
                        } else if (input.length() == 1) {
                            int c = (int) input.charAt(0);
                            children[i]=new Element(Tag.INT,String.valueOf(c));
                        } else if (input.equals("true")) {
                            children[i]=new Element(Tag.INT,"1");
                        } else if (input.equals("false")) {
                            children[i]=new Element(Tag.INT,"0");
                        }else {
                            children[i]=new Element(Tag.STRING,input);
                        }
                    }
                }
            }
            if (children[0] != null && children[1] != null) {
                int tag1 = children[0].getTag();
                int tag2 = children[1].getTag();
                String value1 = children[0].getValue();
                String value2 = children[1].getValue();
                boolean b1 = true;
                boolean b2 = true;
                int result = 0;
                switch (tag) {
                    case Tag.OR:
                        if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) == 0)
                                b1 = false;
                            if (Integer.parseInt(value2) == 0)
                                b2 = false;
                            if (b1 || b2)
                                result = 1;
                        } else if (judgeTag(tag1, tag2) == Tag.REALNUM) {
                            if (Double.parseDouble(value1) == 0)
                                b1 = false;
                            if (Double.parseDouble(value2) == 0)
                                b2 = false;
                            if (b1 || b2)
                                result = 1;
                        } else {
                            setError("字符串不能进行或运算", root.getLineNum());
                        }
                        break;
                    case Tag.AND:
                        if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) == 0)
                                b1 = false;
                            if (Integer.parseInt(value2) == 0)
                                b2 = false;
                            if (b1 && b2)
                                result = 1;
                        } else if (judgeTag(tag1, tag2) == Tag.REALNUM) {
                            if (Double.parseDouble(value1) == 0)
                                b1 = false;
                            if (Double.parseDouble(value2) == 0)
                                b2 = false;
                            if (b1 && b2)
                                result = 1;
                        } else {
                            setError("字符串不能进行与运算", root.getLineNum());
                        }
                        break;
                    case Tag.EQ:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            if (tag1 != tag2)
                                setError(value1 + "," + value2 + "类型不允许做==运算",
                                        root.getLineNum());
                            else {
                                if (value1.equals(value2))
                                    result = 1;
                            }
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) == Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) == Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) == Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) == Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                    case Tag.UE:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            if (tag1 != tag2)
                                setError(value1 + "," + value2 + "类型不允许做!=运算",
                                        root.getLineNum());
                            else {
                                if (!value1.equals(value2))
                                    result = 1;
                            }
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) != Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) != Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) != Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) != Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                    case Tag.GE:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            setError(value1 + "," + value2 + "的类型不允许做>=运算",
                                    root.getLineNum());
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) >= Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) >= Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) >= Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) >= Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                    case Tag.LE:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            setError(value1 + "," + value2 + "的类型不允许做<=运算",
                                    root.getLineNum());
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) <= Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) <= Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) <= Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) <= Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                    case Tag.GREATER:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            setError(value1 + "," + value2 + "的类型不允许做>运算",
                                    root.getLineNum());
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) > Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) > Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) > Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) > Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                    case Tag.LESS:
                        if (judgeTag(tag1, tag2) == Tag.STRING) {
                            setError(value1 + "," + value2 + "的类型不允许做<运算",
                                    root.getLineNum());
                        } else if (judgeTag(tag1, tag2) == Tag.INT) {
                            if (Integer.parseInt(value1) < Integer.parseInt(value2))
                                result = 1;
                        } else {
                            if (tag1 == Tag.REAL) {
                                if (tag2 == Tag.REAL) {
                                    if (Double.parseDouble(value1) < Double.parseDouble(value2))
                                        result = 1;
                                } else {
                                    if (Double.parseDouble(value1) < Integer.parseInt(value2))
                                        result = 1;
                                }
                            } else {
                                if (Double.parseDouble(value2) < Integer.parseInt(value1))
                                    result = 1;
                            }
                        }
                        break;
                }
                return result == 1;
            }
        }
        return false;
    }

    /**
     * 检查标识符是否声明和初始化
     *
     * @param root  标识符结点
     * @param level 标识符作用域
     * @return 如果声明且初始化则返回true, 否则返回false
     */
    private boolean checkID(TreeNode root, int level) {
        //获取标识符
        String id = root.getContent();
        boolean isArray=false;
        if (table.getAllLevel(id, level) == null) {
            //标识符未声明情况
            setError(id + "未声明", root.getLineNum());
            return false;
        } else {
            if (root.getChildCount() != 0) {
                //数组
                isArray=true;
            }
            Symbol symbol = table.getAllLevel(id, level);
            if (symbol!=null){
                if (symbol.getIntValue().equals("") && symbol.getRealValue().equals("")
                        && symbol.getCharValue().equals("") && symbol.getStringValue().equals("") &&
                        symbol.getBoolValue().equals("") && !isArray) {
                    setError("变量" + id + "在使用前未初始化", root.getLineNum());
                    return false;
                } else {
                    return true;
                }
            }else
                return false;
        }
    }

    /**
     * array
     *
     * @param root      数组结点
     * @param arraySize 数组大小
     * @return 出错返回null
     */
    private String array_analyze(TreeNode root, int[] arraySize) {
        int count = root.getChildCount();
        if (count<arraySize.length){
            setError("数组维度小于声明维度", root.getLineNum());
            return null;
        }else if (count>arraySize.length){
            setError("数组维度大于声明维度，越界", root.getLineNum());
            return null;
        }else {
            int size = 1;
            for (int i = 0;i<count;i++){
                TreeNode child = root.getChildAt(i);
                if (child.getTag() == Tag.INTNUM) {
                    int arrayIndex = Integer.parseInt(child.getContent());//数组下标
                    if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                        size*=(arrayIndex+1);
                    } else if (arrayIndex < 0) {
                        setError("数组下标不能为负数", child.getLineNum());
                        return null;
                    } else {
                        setError("数组越界", child.getLineNum());
                        return null;
                    }
                }else if (child.getTag()==Tag.TRUE){
                    if (1<arraySize[i]){
                        size*=2;
                    }else {
                        setError("数组越界", child.getLineNum());
                        return null;
                    }
                }else if (child.getTag()==Tag.FALSE){
                    size*=1;
                }else if (child.getTag()==Tag.CHAR_S){
                    int arrayIndex = (int)child.getContent().charAt(0);
                    if (isEsc_char(child.getContent()))
                        arrayIndex = (int)child.getContent().charAt(1);
                    if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                        size*=(arrayIndex+1);
                    } else if (arrayIndex < 0) {
                        setError("数组下标不能为负数", child.getLineNum());
                        return null;
                    } else {
                        setError("数组越界", child.getLineNum());
                        return null;
                    }
                }
                else if (child.getTag() == Tag.SCAN){
                    TreeNode parent = (TreeNode) child.getParent();
                    String input = scan_analyze(child,parent.getContent());
                    if (input!=null){
                        if (isInteger(input)){
                            int arrayIndex = Integer.parseInt(input);
                            if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                                size*=(Integer.parseInt(input)+1);
                            } else if (arrayIndex < 0) {
                                setError("数组下标不能为负数", child.getLineNum());
                                return null;
                            } else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        }else if (input.equals("true")){
                            if (1<arraySize[i]){
                                size*=2;
                            }else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        }else if (input.equals("false")){
                            size*=1;
                        }else if (input.length()==1){
                            int arrayIndex = (int)child.getContent().charAt(0);
                            if (isEsc_char(child.getContent()))
                                arrayIndex = (int)child.getContent().charAt(1);
                            if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                                size*=(arrayIndex+1);
                            } else if (arrayIndex < 0) {
                                setError("数组下标不能为负数", child.getLineNum());
                                return null;
                            } else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        }else {
                            setError("类型不匹配，数组下标必须为整数型", child.getLineNum());
                            return null;
                        }
                    }
                }
                else if (child.getTag() == Tag.ID) {
                    //下标为标识符
                    if (checkID(child, level)) {
                        Symbol temp = table.getAllLevel(child.getContent(), level);
                        if (temp.getTag() == Tag.INT) {
                            int arrayIndex = Integer.parseInt(temp.getIntValue());
                            if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                                size*=(Integer.parseInt(temp.getIntValue()));
                            } else if (arrayIndex < 0) {
                                setError("数组下标不能为负数", child.getLineNum());
                                return null;
                            } else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        }else if (temp.getTag()==Tag.BOOL){
                           if (temp.getBoolValue().equals("true")||temp.getBoolValue().equals("1")){
                               if (1 < arraySize[i]) {
                                   size*=2;
                               }else {
                                   setError("数组越界", child.getLineNum());
                                   return null;
                               }
                           }else
                               size*=1;
                        }else if (temp.getTag()==Tag.CHAR){
                            int arrayIndex = (int)temp.getCharValue().charAt(0);
                            if (isEsc_char(temp.getCharValue()))
                                arrayIndex = (int)temp.getCharValue().charAt(1);
                            if (arrayIndex > -1 && arrayIndex < arraySize[i]) {
                                size*=(arrayIndex+1);
                            } else if (arrayIndex < 0) {
                                setError("数组下标不能为负数", child.getLineNum());
                                return null;
                            } else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        }
                        else{
                            setError("类型不匹配，数组下标必须为整型类型", child.getLineNum());
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else if (child.getTag() == Tag.ADD || child.getTag() == Tag.SUB
                        || child.getTag() == Tag.MUL || child.getTag() == Tag.DIVIDE) {
                    ExpressionPart exp = expression_analyze(child);
                    if (exp != null) {
                        String arrayIndex = exp.getResult();
                        if (isInteger(arrayIndex)) {
                            int index = Integer.parseInt(arrayIndex);
                            if (index > -1 && index < arraySize[i])
                                size*=(Integer.parseInt(arrayIndex)+1);
                            else if (index < 0) {
                                setError("数组下标不能为负数", child.getLineNum());
                                return null;
                            } else {
                                setError("数组越界", child.getLineNum());
                                return null;
                            }
                        } else {
                            setError("类型不匹配，数组下标必须为整数", child.getLineNum());
                            return null;
                        }
                    } else
                        return null;
                }
            }
            return String.valueOf(size-1);
        }
    }

    /**
     * 分析表达式
     *
     * @param root 表达式根节点
     * @return 返回计算结果
     */
    private ExpressionPart expression_analyze(TreeNode root) {
        int rootTag = root.getTag();
        boolean isNegative = false;
        boolean isNP=false;   //正负号
        ExpressionPart part = new ExpressionPart();
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode temp = root.getChildAt(i);
            //声明语句或赋值语句处理正负号时，传入的root为正负结点，更改temp为root
            if (rootTag==Tag.NEG||rootTag==Tag.POS){
                temp = root;
            }
            int tag = temp.getTag();
            String tempContent = temp.getContent();
            //处理正负号
            if (tag==Tag.POS){
                isNP=true;
                temp=temp.getChildAt(0);
                tag=temp.getTag();
                tempContent=temp.getContent();
            }else if (tag==Tag.NEG){
                isNP=true;
                temp=temp.getChildAt(0);
                tag=temp.getTag();
                isNegative=true;
                tempContent=temp.getContent();
            }
            if (tag == Tag.INTNUM) {
                part.setChild(tempContent, i);
            } else if (tag == Tag.REALNUM) {
                part.setChild(tempContent, i);
                part.setIsInt(false);
            }else if (tag==Tag.TRUE){
                if (isNegative)
                    part.setChild(negativeHandle(temp),i);
                else
                    part.setChild("1", i);
            }else if (tag==Tag.FALSE){
                if (isNegative)
                    part.setChild(negativeHandle(temp),i);
                else
                    part.setChild("0", i);
            } else if (tag == Tag.CHAR_S) {
                //将char转成int计算,int要转成string才能存入children数组
                if (isNegative)
                    part.setChild(negativeHandle(temp), i);
                else {
                    char c = tempContent.charAt(0);
                    if (isEsc_char(tempContent)){
                        c=tempContent.charAt(1);
                    }
                    int cToi = (int) c;
                    part.setChild(String.valueOf(cToi), i);
                }
            } else if (tag == Tag.STR) {
                if (isNegative){
                    setError("字符串不允许正负号运算",temp.getLineNum());
                    return null;
                }
                part.setIsString(true);
                part.setChild(tempContent, i);
            }else if (tag==Tag.SCAN){
                String input = scan_analyze(temp,"");
                if (input!=null){
                    if (isInteger(input)){
                        if (isNegative)
                            part.setChild(String.valueOf(-Integer.parseInt(input)),i);
                        else
                            part.setChild(input,i);
                    }else if (isReal(input)){
                        if (isNegative)
                            part.setChild(String.valueOf(-Double.parseDouble(input)),i);
                        else
                            part.setChild(input,i);
                        part.setIsInt(false);
                    }else if (input.length()==1){
                        int c = (int)input.charAt(0);
                        if (isNegative)
                            part.setChild(String.valueOf(-c),i);
                        else
                            part.setChild(String.valueOf(c),i);
                    }else if (input.equals("true")){
                        if (isNegative)
                            part.setChild("0",i);
                        else
                            part.setChild("1",i);
                    }else if (input.equals("false")){
                        if (isNegative)
                            part.setChild("1",i);
                        else
                            part.setChild("0",i);
                    }else {
                        if (isNegative){
                            setError("scan输入为字符串，不允许正负号操作",temp.getLineNum());
                            return null;
                        }
                        part.setChild(input,i);
                        part.setIsString(true);
                    }
                }
            }
            else if (tag == Tag.ID) {
                if (checkID(temp, level)) {
                    if (temp.getChildCount() != 0) {
                        String s = array_analyze(temp, table
                                .getAllLevel(tempContent, level)
                                .getArraySize());
                        if (s != null)
                            tempContent += "@" + s;
                        else
                            return null;
                    }
                    Symbol symbol = table.getAllLevel(tempContent, level);
                    if (symbol.getTag() == Tag.INT) {
                        if (isNegative)
                            part.setChild(String.valueOf(-Integer.parseInt(symbol.getIntValue())), i);
                        else
                            part.setChild(symbol.getIntValue(), i);
                    }
                    else if (symbol.getTag() == Tag.REAL) {
                        if (isNegative)
                            part.setChild(String.valueOf(-Double.parseDouble(symbol.getIntValue())), i);
                        else
                            part.setChild(symbol.getRealValue(), i);
                        part.setIsInt(false);
                    } else if (symbol.getTag() == Tag.CHAR) {
                        String s = symbol.getCharValue();
                        char c = s.charAt(0);
                        int cToi = (int) c;
                        if (isNegative)
                            part.setChild(String.valueOf(-cToi), i);
                        else
                            part.setChild(String.valueOf(cToi), i);
                    } else if (symbol.getTag() == Tag.BOOL) {
                        if (isNegative){
                            if (symbol.getBoolValue().equals("false") ||
                                    symbol.getBoolValue().equals("0"))
                                part.setChild("1", i);
                            else
                                part.setChild("0", i);
                        }else {
                            if (symbol.getBoolValue().equals("false") ||
                                    symbol.getBoolValue().equals("0"))
                                part.setChild("0", i);
                            else
                                part.setChild("1", i);
                        }
                    } else if (symbol.getTag() == Tag.STRING) {
                        if (isNegative){
                            setError("字符串不存在负操作",temp.getLineNum());
                            return null;
                        }
                        part.setChild(symbol.getStringValue(), i);
                        part.setIsString(true);
                    }
                } else {
                    return null;
                }
            } else if (tag == Tag.ADD || tag == Tag.SUB
                    || tag == Tag.MUL || tag == Tag.DIVIDE) {
                ExpressionPart exp;
                if (isNP)
                    exp = expression_analyze(root.getChildAt(i).getChildAt(0));
                else
                    exp = expression_analyze(root.getChildAt(i));
                if (exp != null) {
                    part.setChild(exp.getResult(), i);
                    if (exp.getTag() == Tag.REAL)
                        part.setIsInt(false);
                } else
                    return null;
            } else if (tag == Tag.EQ || tag == Tag.LE || tag == Tag.GE || tag == Tag.UE
                    || tag == Tag.LESS || tag == Tag.GREATER ||
                    tag == Tag.AND || tag == Tag.OR) {
                if (condition_analyze(root)) {
                    part.setChild("1", i);
                } else
                    part.setChild("0", i);
            }
        }
        if (root.getChildCount()==1 &&
                (rootTag==Tag.NEG || rootTag==Tag.POS)){
            part.setResult(part.getChild1());
            return part;
        }
        if (part.getTag() == Tag.INT) {
            int child1 = Integer.parseInt(part.getChild1());
            int child2 = Integer.parseInt(part.getChild2());
            if (rootTag == Tag.ADD) {
                part.setResult(String.valueOf(child1 + child2));
                return part;
            } else if (rootTag == Tag.SUB) {
                part.setResult(String.valueOf(child1 - child2));
                return part;
            } else if (rootTag == Tag.MUL) {
                part.setResult(String.valueOf(child1 * child2));
                return part;
            } else if (rootTag == Tag.DIVIDE) {
                if (child2 == 0) {
                    setError("除数不能为0", root.getLineNum());
                    return null;
                } else {
                    part.setResult(String.valueOf(child1 / child2));
                    return part;
                }
            } else
                return null;
        } else if (part.getTag() == Tag.REAL) {
            BigDecimal bg1 = new BigDecimal(part.getChild1());
            BigDecimal bg2 = new BigDecimal(part.getChild2());
            if (rootTag == Tag.ADD) {
                part.setResult(String.valueOf(bg1.add(bg2)));
                return part;
            } else if (rootTag == Tag.SUB) {
                part.setResult(String.valueOf(bg1.subtract(bg2)));
                return part;
            } else if (rootTag == Tag.MUL) {
                part.setResult(String.valueOf(bg1.multiply(bg2)));
                return part;
            } else if (rootTag == Tag.DIVIDE) {
                try {
                    part.setResult(String.valueOf(bg1.divide(bg2)));
                    return part;
                } catch (ArithmeticException e) {
                    setError("除数不能为0", root.getLineNum());
                    return null;
                }
            } else
                return null;
        } else {
            String s1 = part.getChild1();
            String s2 = part.getChild2();
            if (root.getTag() == Tag.ADD) {
                part.setResult(s1 + s2);
                return part;
            } else {
                setError("字符串不允许除加法以外的算术运算", root.getLineNum());
                return null;
            }
        }
    }
}
