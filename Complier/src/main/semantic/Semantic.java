package main.semantic;

import main.lexer.Tag;
import main.parse.TreeNode;

import java.math.BigDecimal;
import java.util.ArrayList;

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

    private void setError(String reason,int line){
        errorNum++;
        SError error = new SError(reason,line,errorNum);
        errors.add(error);
    }

    //识别整数
    private static boolean isInteger(String s){
        if (s.matches("^(\\-|\\+)?[\\s]*[0-9]\\d*$") ||s.matches("^(\\-|\\+?)0$")
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
    public void run(){
        table.removeAll();

    }

    /**
     * 语义分析
     * @param root
     * 语法分析生成的语法树根节点
     */
    private void statement(TreeNode root){
        for (int i=0;i<root.getChildCount();i++){
            TreeNode currentNode = root.getChildAt(i);
            int tag = currentNode.getTag();
            if (tag== Tag.IF){
                //进入if代码段，作用域改变
                level++;

            }else if (tag==Tag.INT || tag==Tag.REAL
                ||tag==Tag.CHAR || tag==Tag.STRING || tag==Tag.BOOL){

            }
        }
    }

    /**
     * if代码段语义分析
     * @param root
     * 不是指根节点而是if树节点
     */
    private void if_analyze(TreeNode root){
        int count = root.getChildCount();
        TreeNode conditionNode = root.getChildAt(0);
        TreeNode statementNode = root.getChildAt(1);
        //todo 条件语句判断
    }

    /**
     * 声明代码段语义分析
     * @param root
     * 不是指根节点而是if树节点
     */
    private void declare_analyze(TreeNode root){
        String content = root.getContent();
        int tag = root.getTag();
        int index = 0;
        while (index<root.getChildCount()){
            TreeNode child = root.getChildAt(index);
            String name = child.getContent();  //变量名
            if (table.getAllLevel(name,level)==null){
                if (child.getChildCount()==0){
                    //新建声明变量
                    Symbol symbol = new Symbol(name,child.getTag(),child.getLineNum(),level);
                    index++;
                    if (index<root.getChildCount()
                            && root.getChildAt(index).getTag()==Tag.ASSIGN){
                        TreeNode valueNode = root.getChildAt(index).getChildAt(0);
                        String value = valueNode.getContent();
                        if (tag==Tag.INT){ //int 变量
                            if(valueNode.getTag()==Tag.INTNUM){
                                symbol.setIntValue(value);
                                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                            }else if (valueNode.getTag()==Tag.REAL){
                                setError("不能将real数值赋值给int型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.CHAR_S)
                                setError("不能将char字符赋值给int型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.STR)
                                setError("不能将string字符串赋值给int型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.TRUE || valueNode.getTag()==Tag.FALSE)
                                setError("不能将布尔值赋值给int型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.ID){
                                if (checkID(valueNode,level)){
                                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(),level);
                                    if (idSymbol.getTag() ==Tag.INT){
                                        symbol.setIntValue(idSymbol.getIntValue());
                                        symbol.setRealValue(String.
                                                valueOf(Double.parseDouble(idSymbol.getIntValue())));
                                    }else if (idSymbol.getTag()==Tag.REAL)
                                        setError("不能将real数值赋值给int型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.CHAR)
                                        setError("不能将char字符赋值给int型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.STRING)
                                        setError("不能将string字符串赋值给int型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.BOOL)
                                        setError("不能将布尔值赋值给int型变量",valueNode.getLineNum());
                                }else
                                    return;
                            }else if (valueNode.getTag()==Tag.ADD || valueNode.getTag()==Tag.SUB
                                    ||valueNode.getTag()==Tag.MUL || valueNode.getTag()==Tag.DIVIDE){
                                ExpressionPart part = expression_analyze(valueNode);
                                String result = part.getResult();
                                if (result!=null){
                                    if (isInteger(result) && part.isInt()){
                                        symbol.setIntValue(result);
                                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                    }else if (isReal(result) && !part.isString()){
                                        setError("不能将real数值赋值给int型变量",valueNode.getLineNum());
                                        return;
                                    }else if (part.isString()){
                                        setError("不能将字符串赋值给int型变量",valueNode.getLineNum());
                                        return;
                                    }
                                    else
                                        return;
                                }
                            }
                        }else if (tag==Tag.REAL){  //real 声明
                            if(valueNode.getTag()==Tag.INTNUM){
                                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                            }else if (valueNode.getTag()==Tag.REAL){
                                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                            }else if (valueNode.getTag()==Tag.CHAR_S)
                                setError("不能将char字符赋值给real型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.STR)
                                setError("不能将string字符串赋值给real型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.TRUE || valueNode.getTag()==Tag.FALSE)
                                setError("不能将布尔值赋值给real型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.ID){
                                if (checkID(valueNode,level)){
                                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(),level);
                                    if (idSymbol.getTag() ==Tag.INT ||idSymbol.getTag()==Tag.REAL){
                                        symbol.setRealValue(String.
                                                valueOf(Double.parseDouble(idSymbol.getRealValue())));
                                    }
                                    else if (idSymbol.getTag()==Tag.CHAR)
                                        setError("不能将char字符赋值给real型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.STRING)
                                        setError("不能将string字符串赋值给real型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.BOOL)
                                        setError("不能将布尔值赋值给real型变量",valueNode.getLineNum());
                                }else
                                    return;
                            }else if (valueNode.getTag()==Tag.ADD || valueNode.getTag()==Tag.SUB
                                    ||valueNode.getTag()==Tag.MUL || valueNode.getTag()==Tag.DIVIDE){
                                ExpressionPart part = expression_analyze(valueNode);
                                String result = part.getResult();
                                if (result!=null){
                                    if (isInteger(result) && part.isInt()){
                                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                    }else if (isReal(result) && !part.isString()){
                                        symbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                    }
                                    else if (part.isString()){
                                        setError("不能将字符串赋值给real型变量",valueNode.getLineNum());
                                        return;
                                    }else
                                        return;
                                }
                            }
                        }else if (tag==Tag.CHAR){  //real 声明
                            if(valueNode.getTag()==Tag.INTNUM){
                                setError("不能将int数值赋值给char型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.REAL){
                                setError("不能将real数值赋值给char型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.CHAR_S)
                                symbol.setCharValue(value);
                            else if (valueNode.getTag()==Tag.STR)
                                setError("不能将string字符串赋值给char型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.TRUE || valueNode.getTag()==Tag.FALSE)
                                setError("不能将布尔值赋值给char型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.ID){
                                if (checkID(valueNode,level)){
                                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(),level);
                                    if (idSymbol.getTag() ==Tag.INT ){
                                        setError("不能将int型变量赋值给char型变量",valueNode.getLineNum());
                                    }else if (idSymbol.getTag()==Tag.REAL){
                                        setError("不能将real型变量字符赋值给char型变量",valueNode.getLineNum());
                                    }
                                    else if (idSymbol.getTag()==Tag.CHAR)
                                        symbol.setCharValue(idSymbol.getCharValue());
                                    else if (idSymbol.getTag()==Tag.STRING)
                                        setError("不能将string变量赋值给char型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.BOOL)
                                        setError("不能将布尔型变量赋值给char型变量",valueNode.getLineNum());
                                }else
                                    return;
                            }else if (valueNode.getTag()==Tag.ADD || valueNode.getTag()==Tag.SUB
                                    ||valueNode.getTag()==Tag.MUL || valueNode.getTag()==Tag.DIVIDE){
                                setError("不能将算术表达式赋值给char型变量",valueNode.getLineNum());
                            }
                        }else if (tag==Tag.STRING){  //real 声明
                            if(valueNode.getTag()==Tag.INTNUM){
                                setError("不能将int数值赋值给string型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.REAL){
                                setError("不能将real数值赋值给string型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.CHAR_S)
                                setError("不能将char字符赋值给string型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.STR)
                                symbol.setStringValue(value);
                            else if (valueNode.getTag()==Tag.TRUE || valueNode.getTag()==Tag.FALSE)
                                setError("不能将布尔值赋值给string型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.ID){
                                if (checkID(valueNode,level)){
                                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(),level);
                                    if (idSymbol.getTag() ==Tag.INT ){
                                        setError("不能将int型变量赋值给string型变量",valueNode.getLineNum());
                                    }else if (idSymbol.getTag()==Tag.REAL){
                                        setError("不能将real型变量赋值给string型变量",valueNode.getLineNum());
                                    }
                                    else if (idSymbol.getTag()==Tag.CHAR)
                                        setError("不能将char变量赋值给string型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.STRING)
                                        symbol.setStringValue(idSymbol.getStringValue());
                                    else if (idSymbol.getTag()==Tag.BOOL)
                                        setError("不能将布尔型变量赋值给string型变量",valueNode.getLineNum());
                                }else
                                    return;
                            }else if (valueNode.getTag()==Tag.ADD ){
                                ExpressionPart part = expression_analyze(valueNode);
                                symbol.setStringValue(part.getResult());
                            }else {
                                setError("除加法之外的算术表达式不能赋值给string型变量",valueNode.getLineNum());
                            }
                        }else if (tag == Tag.BOOL){
                            if(valueNode.getTag()==Tag.INTNUM){
                                setError("不能将int数值赋值给布尔型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.REAL){
                                setError("不能将real数值赋值给布尔型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.CHAR_S)
                                setError("不能将char字符赋值给布尔型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.STR)
                                setError("不能将string字符串赋值给布尔型变量",valueNode.getLineNum());
                            else if (valueNode.getTag()==Tag.TRUE || valueNode.getTag()==Tag.FALSE)
                                symbol.setBoolValue(value);
                            else if (valueNode.getTag()==Tag.ID){
                                if (checkID(valueNode,level)){
                                    Symbol idSymbol = table.getAllLevel(valueNode.getContent(),level);
                                    if (idSymbol.getTag() ==Tag.INT ){
                                        setError("不能将int型变量赋值给布尔型变量",valueNode.getLineNum());
                                    }else if (idSymbol.getTag()==Tag.REAL){
                                        setError("不能将real型变量赋值给布尔型变量",valueNode.getLineNum());
                                    }
                                    else if (idSymbol.getTag()==Tag.CHAR)
                                        setError("不能将char字符赋值给布尔型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.STRING)
                                        setError("不能将string变量赋值给布尔型变量",valueNode.getLineNum());
                                    else if (idSymbol.getTag()==Tag.BOOL)
                                        symbol.setBoolValue(idSymbol.getBoolValue());
                                }else
                                    return;
                            }else if (valueNode.getTag()==Tag.ADD || valueNode.getTag()==Tag.SUB
                                    ||valueNode.getTag()==Tag.MUL || valueNode.getTag()==Tag.DIVIDE){
                                setError("不能将算术表达式赋值给布尔型变量",valueNode.getLineNum());
                            }else if (valueNode.getTag()==Tag.EQ || valueNode.getTag()==Tag.UE
                                    || valueNode.getTag()==Tag.GE || valueNode.getTag()==Tag.LE
                                    || valueNode.getTag()==Tag.LESS || valueNode.getTag()==Tag.GREATER
                                    || valueNode.getTag()==Tag.AND || valueNode.getTag()==Tag.OR){
                                boolean result = condition_analyze(valueNode);
                                if (result)
                                    symbol.setBoolValue("true");
                                else
                                    symbol.setBoolValue("false");
                            }
                        }
                        index++;
                    }
                    table.addSymbol(symbol);
                }else {
                    //声明数组
                    Symbol symbol = new Symbol(name,child.getTag(),child.getLineNum(),level);
                    int arrayTag = child.getChildAt(0).getTag();
                    String sizeValue = child.getChildAt(0).getContent();
                    if (arrayTag==Tag.INTNUM){
                        int arraySize = Integer.parseInt(sizeValue);
                        if (arraySize<1){
                            setError("数组大小必须大于0",root.getLineNum());
                            return;
                        }
                    }else if (arrayTag==Tag.ID){
                        if (checkID(root,level)){
                            Symbol tempSymbol = table.getAllLevel(child.getChildAt(0).getContent(),level);
                            if (tempSymbol.getTag()==Tag.INT){
                                int arraySize = Integer.parseInt(tempSymbol.getIntValue());
                                if (arraySize<1){
                                    setError("数组大小必须大于0",root.getLineNum());
                                    return;
                                }else {
                                    sizeValue=String.valueOf(arraySize);
                                }
                            }else {
                                setError("类型不匹配，数组大小必须为整型",child.getLineNum());
                                return;
                            }
                        }else {
                            return;
                        }
                    }else if (arrayTag==Tag.ADD || arrayTag==Tag.SUB
                            ||arrayTag==Tag.MUL || arrayTag == Tag.DIVIDE){
                        ExpressionPart part = expression_analyze(child.getChildAt(0));
                        if (part!=null){
                            if (part.isInt()){
                                int arraySize = Integer.parseInt(part.getResult());
                                if (arraySize<1){
                                    setError("数组大小必须大于0",root.getLineNum());
                                    return;
                                }else {
                                    sizeValue=String.valueOf(arraySize);
                                }
                            }else {
                                setError("类型不匹配，数组大小必须为整型",child.getLineNum());
                                return;
                            }
                        }else
                            return;
                    }
                    symbol.setArraySize(Integer.parseInt(sizeValue));
                    table.addSymbol(symbol);
                    index++;
                    if (index<root.getChildCount() &&
                            root.getChildAt(index).getTag()==Tag.ASSIGN){
                        TreeNode items = root.getChildAt(index);
                        int count = items.getChildCount();
                        if (count==Integer.parseInt(sizeValue)){
                            for (int j=0;j<count;j++){
                                //大括号声明的数组元素
                                if (items.getChildAt(j).getTag()==tag){
                                    TreeNode item = items.getChildAt(j);
                                    String itemName = child.getContent()+"@"+j;
                                    Symbol itemSymbol = new Symbol(itemName,item.getTag(),
                                            items.getLineNum(),level);
                                    String value = item.getContent();
                                    if (tag==Tag.INT){ //int 变量
                                        if(item.getTag()==Tag.INTNUM){
                                            itemSymbol.setIntValue(value);
                                            itemSymbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                                        }else if (item.getTag()==Tag.REAL){
                                            setError("不能将real数值赋值给int型变量",item.getLineNum());
                                        }else if (item.getTag()==Tag.CHAR_S)
                                            setError("不能将char字符赋值给int型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.STR)
                                            setError("不能将string字符串赋值给int型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.TRUE || item.getTag()==Tag.FALSE)
                                            setError("不能将布尔值赋值给int型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.ID){
                                            if (checkID(item,level)){
                                                Symbol idSymbol = table.getAllLevel(item.getContent(),level);
                                                if (idSymbol.getTag() ==Tag.INT){
                                                    itemSymbol.setIntValue(idSymbol.getIntValue());
                                                    itemSymbol.setRealValue(String.
                                                            valueOf(Double.parseDouble(idSymbol.getIntValue())));
                                                }else if (idSymbol.getTag()==Tag.REAL)
                                                    setError("不能将real数值赋值给int型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.CHAR)
                                                    setError("不能将char字符赋值给int型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.STRING)
                                                    setError("不能将string字符串赋值给int型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.BOOL)
                                                    setError("不能将布尔值赋值给int型变量",item.getLineNum());
                                            }else
                                                return;
                                        }else if (item.getTag()==Tag.ADD || item.getTag()==Tag.SUB
                                                ||item.getTag()==Tag.MUL || item.getTag()==Tag.DIVIDE){
                                            ExpressionPart part = expression_analyze(item);
                                            String result = part.getResult();
                                            if (result!=null){
                                                if (isInteger(result) && part.isInt()){
                                                    itemSymbol.setIntValue(result);
                                                    itemSymbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                                }else if (isReal(result) && !part.isString()){
                                                    setError("不能将real数值赋值给int型变量",item.getLineNum());
                                                    return;
                                                }else if (part.isString()){
                                                    setError("不能将字符串赋值给int型变量",item.getLineNum());
                                                    return;
                                                }
                                                else
                                                    return;
                                            }
                                        }
                                    }else if (tag==Tag.REAL){  //real 声明
                                        if(item.getTag()==Tag.INTNUM){
                                            itemSymbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                                        }else if (item.getTag()==Tag.REAL){
                                            itemSymbol.setRealValue(String.valueOf(Double.parseDouble(value)));
                                        }else if (item.getTag()==Tag.CHAR_S)
                                            setError("不能将char字符赋值给real型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.STR)
                                            setError("不能将string字符串赋值给real型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.TRUE || item.getTag()==Tag.FALSE)
                                            setError("不能将布尔值赋值给real型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.ID){
                                            if (checkID(item,level)){
                                                Symbol idSymbol = table.getAllLevel(item.getContent(),level);
                                                if (idSymbol.getTag() ==Tag.INT ||idSymbol.getTag()==Tag.REAL){
                                                    itemSymbol.setRealValue(String.
                                                            valueOf(Double.parseDouble(idSymbol.getRealValue())));
                                                }
                                                else if (idSymbol.getTag()==Tag.CHAR)
                                                    setError("不能将char字符赋值给real型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.STRING)
                                                    setError("不能将string字符串赋值给real型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.BOOL)
                                                    setError("不能将布尔值赋值给real型变量",item.getLineNum());
                                            }else
                                                return;
                                        }else if (item.getTag()==Tag.ADD || item.getTag()==Tag.SUB
                                                ||item.getTag()==Tag.MUL || item.getTag()==Tag.DIVIDE){
                                            ExpressionPart part = expression_analyze(item);
                                            String result = part.getResult();
                                            if (result!=null){
                                                if (isInteger(result) && part.isInt()){
                                                    itemSymbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                                }else if (isReal(result) && !part.isString()){
                                                    itemSymbol.setRealValue(String.valueOf(Double.parseDouble(result)));
                                                }
                                                else if (part.isString()){
                                                    setError("不能将字符串赋值给real型变量",item.getLineNum());
                                                    return;
                                                }else
                                                    return;
                                            }
                                        }
                                    }else if (tag==Tag.CHAR){  //real 声明
                                        if(item.getTag()==Tag.INTNUM){
                                            setError("不能将int数值赋值给char型变量",item.getLineNum());
                                        }else if (item.getTag()==Tag.REAL){
                                            setError("不能将real数值赋值给char型变量",item.getLineNum());
                                        }else if (item.getTag()==Tag.CHAR_S)
                                            itemSymbol.setCharValue(value);
                                        else if (item.getTag()==Tag.STR)
                                            setError("不能将string字符串赋值给char型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.TRUE || item.getTag()==Tag.FALSE)
                                            setError("不能将布尔值赋值给char型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.ID){
                                            if (checkID(item,level)){
                                                Symbol idSymbol = table.getAllLevel(item.getContent(),level);
                                                if (idSymbol.getTag() ==Tag.INT ){
                                                    setError("不能将int型变量赋值给char型变量",item.getLineNum());
                                                }else if (idSymbol.getTag()==Tag.REAL){
                                                    setError("不能将real型变量字符赋值给char型变量",item.getLineNum());
                                                }
                                                else if (idSymbol.getTag()==Tag.CHAR)
                                                    itemSymbol.setCharValue(idSymbol.getCharValue());
                                                else if (idSymbol.getTag()==Tag.STRING)
                                                    setError("不能将string变量赋值给char型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.BOOL)
                                                    setError("不能将布尔型变量赋值给char型变量",item.getLineNum());
                                            }else
                                                return;
                                        }else if (item.getTag()==Tag.ADD || item.getTag()==Tag.SUB
                                                ||item.getTag()==Tag.MUL || item.getTag()==Tag.DIVIDE){
                                            setError("不能将算术表达式赋值给char型变量",item.getLineNum());
                                        }
                                    }else if (tag==Tag.STRING){  //real 声明
                                        if(item.getTag()==Tag.INTNUM){
                                            setError("不能将int数值赋值给string型变量",item.getLineNum());
                                        }else if (item.getTag()==Tag.REAL){
                                            setError("不能将real数值赋值给string型变量",item.getLineNum());
                                        }else if (item.getTag()==Tag.CHAR_S)
                                            setError("不能将char字符赋值给string型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.STR)
                                            itemSymbol.setStringValue(value);
                                        else if (item.getTag()==Tag.TRUE || item.getTag()==Tag.FALSE)
                                            setError("不能将布尔值赋值给string型变量",item.getLineNum());
                                        else if (item.getTag()==Tag.ID){
                                            if (checkID(item,level)){
                                                Symbol idSymbol = table.getAllLevel(item.getContent(),level);
                                                if (idSymbol.getTag() ==Tag.INT ){
                                                    setError("不能将int型变量赋值给string型变量",item.getLineNum());
                                                }else if (idSymbol.getTag()==Tag.REAL){
                                                    setError("不能将real型变量赋值给string型变量",item.getLineNum());
                                                }
                                                else if (idSymbol.getTag()==Tag.CHAR)
                                                    setError("不能将char变量赋值给string型变量",item.getLineNum());
                                                else if (idSymbol.getTag()==Tag.STRING)
                                                    itemSymbol.setStringValue(idSymbol.getStringValue());
                                                else if (idSymbol.getTag()==Tag.BOOL)
                                                    setError("不能将布尔型变量赋值给string型变量",item.getLineNum());
                                            }else
                                                return;
                                        }else if (item.getTag()==Tag.ADD ){
                                            ExpressionPart part = expression_analyze(item);
                                            itemSymbol.setStringValue(part.getResult());
                                        }else {
                                            setError("除加法之外的算术表达式不能赋值给string型变量",item.getLineNum());
                                        }
                                    }else if (tag == Tag.BOOL) {
                                        if (item.getTag() == Tag.INTNUM) {
                                            setError("不能将int数值赋值给布尔型变量", item.getLineNum());
                                        } else if (item.getTag() == Tag.REAL) {
                                            setError("不能将real数值赋值给布尔型变量", item.getLineNum());
                                        } else if (item.getTag() == Tag.CHAR_S)
                                            setError("不能将char字符赋值给布尔型变量", item.getLineNum());
                                        else if (item.getTag() == Tag.STR)
                                            setError("不能将string字符串赋值给布尔型变量", item.getLineNum());
                                        else if (item.getTag() == Tag.TRUE || item.getTag() == Tag.FALSE)
                                            itemSymbol.setBoolValue(value);
                                        else if (item.getTag() == Tag.ID) {
                                            if (checkID(item, level)) {
                                                Symbol idSymbol = table.getAllLevel(item.getContent(), level);
                                                if (idSymbol.getTag() == Tag.INT) {
                                                    setError("不能将int型变量赋值给布尔型变量", item.getLineNum());
                                                } else if (idSymbol.getTag() == Tag.REAL) {
                                                    setError("不能将real型变量赋值给布尔型变量", item.getLineNum());
                                                } else if (idSymbol.getTag() == Tag.CHAR)
                                                    setError("不能将char字符赋值给布尔型变量", item.getLineNum());
                                                else if (idSymbol.getTag() == Tag.STRING)
                                                    setError("不能将string变量赋值给布尔型变量", item.getLineNum());
                                                else if (idSymbol.getTag() == Tag.BOOL)
                                                    itemSymbol.setBoolValue(idSymbol.getBoolValue());
                                            } else
                                                return;
                                        } else if (item.getTag() == Tag.ADD || item.getTag() == Tag.SUB
                                                || item.getTag() == Tag.MUL || item.getTag() == Tag.DIVIDE) {
                                            setError("不能将算术表达式赋值给布尔型变量", item.getLineNum());
                                        } else if (item.getTag() == Tag.EQ || item.getTag() == Tag.UE
                                                || item.getTag() == Tag.GE || item.getTag() == Tag.LE
                                                || item.getTag() == Tag.LESS || item.getTag() == Tag.GREATER
                                                || item.getTag() == Tag.AND || item.getTag() == Tag.OR) {
                                            boolean result = condition_analyze(item);
                                            if (result)
                                                itemSymbol.setBoolValue("true");
                                            else
                                                itemSymbol.setBoolValue("false");
                                        }
                                    }
                                    table.addSymbol(itemSymbol);
                                }
                                
                            }
                        }else {
                            setError("数组大小与声明大小不匹配",child.getLineNum());
                            return;
                        }
                    }
                }

            }else {
                setError(name+"已经被声明",child.getLineNum());
                return;
            }
        }
    }

    /**
     * 分析条件语句
     * @param root
     * 条件语句根节点
     * @return 条件语句结果
     */
    private boolean condition_analyze(TreeNode root){
        String content = root.getContent();
        int tag = root.getTag();
        if (isInteger(content)){
            if (Integer.parseInt(content)==1)
                return true;
            else if (Integer.parseInt(content)==0)
                return false;
            else
                setError(content+"不能作为判断条件",root.getLineNum());
        }else if (root.getTag()==Tag.TRUE){
            return true;
        }else if (root.getTag()==Tag.FALSE){
            return false;
        } else if (tag==Tag.ID){
            if (checkID(root,level)){
                if (root.getChildCount()!=0){
                    String str = array_analyze(root.getChildAt(0),
                            table.getAllLevel(content,level).getArraySize());
                    if (str!=null)
                        content += "@" + str;
                    else
                        return false;
                }
                Symbol symbol = table.getAllLevel(content,level);
                if (symbol.getTag()==Tag.BOOL){
                    if (symbol.getBoolValue().equals("true"))
                        return true;
                    else
                        return false;
                }
                else if (symbol.getTag()==Tag.INT){
                    int i = Integer.parseInt(symbol.getIntValue());
                    if (i==0)
                        return false;
                    else if (i==1)
                        return true;
                    else
                        setError(content+"不能作为判断条件",root.getLineNum());
                } else{
                    setError("不能将变量"+content+"作为判断条件",root.getLineNum());
                }
            }else return false;
        }else if (tag==Tag.EQ || tag == Tag.LE || tag == Tag.GE || tag == Tag.UE
                || tag == Tag.LESS || tag == Tag.GREATER){
                String[] children = new String[2];
                for (int i=0;i<root.getChildCount();i++){
                    int childTag = root.getChildAt(i).getTag();
                    String childContent = root.getChildAt(i).getContent();
                    if (childTag==Tag.OR || childTag==Tag.AND){
                        if (condition_analyze(root.getChildAt(i)))
                            children[i]="1";
                        else
                            children[i]="0";
                    }else if (childTag==Tag.INTNUM || childTag==Tag.REALNUM
                            || childTag==Tag.CHAR_S){
                        if (childTag==Tag.CHAR_S){
                            char c = childContent.charAt(0);
                            int cToi = (int)c;
                            children[i]=String.valueOf(cToi);
                        }else {
                            children[i]=childContent;
                        }
                    }else if (childTag==Tag.ID){
                        if (checkID(root.getChildAt(i),level)){
                            if (root.getChildAt(i).getChildCount()!=0){
                                String arrStr = array_analyze(root.getChildAt(i).getChildAt(0),
                                        table.getAllLevel(childContent,level).getArraySize());
                                if (arrStr!=null)
                                    childContent+="@"+arrStr;
                                else return false;
                            }
                            Symbol symbol = table.getAllLevel(childContent,level);
                            if (symbol.getTag()==Tag.CHAR){
                                char c = symbol.getCharValue().charAt(0);
                                int cToi = (int)c;
                                children[i]=String.valueOf(cToi);
                            }else if (symbol.getTag()==Tag.INT){
                                children[i]=symbol.getIntValue();
                            }else
                                children[i]=symbol.getRealValue();
                        }else
                            return false;
                    }else if (childTag==Tag.ADD || childTag==Tag.SUB
                            ||childTag==Tag.MUL || childTag==Tag.DIVIDE){
                        String exp = expression_analyze(root.getChildAt(i)).getResult();
                        if (exp!=null)
                            children[i]=exp;
                        else
                            return false;
                    }
                }
        }else if (tag == Tag.AND || tag == Tag.OR){
            String[] children = new String[2];
            for (int i=0;i<root.getChildCount();i++) {
                int childTag = root.getChildAt(i).getTag();
                String childContent = root.getChildAt(i).getContent();
                if (childTag==Tag.OR || childTag==Tag.AND ||
                        childTag==Tag.EQ || childTag == Tag.LE || childTag == Tag.GE || childTag == Tag.UE
                        || childTag == Tag.LESS || childTag == Tag.GREATER){
                    if (condition_analyze(root.getChildAt(i)))
                        children[i]="1";
                    else
                        children[i]="0";
                }
//                else if (){
//
//                }

            }
        }
        return false;
    }

    /**
     * 检查标识符是否声明和初始化
     *
     * @param root
     *            标识符结点
     * @param level
     *            标识符作用域
     * @return 如果声明且初始化则返回true,否则返回false
     */
    private boolean checkID(TreeNode root,int level){
        //获取标识符
        String id = root.getContent();
        if (table.getAllLevel(id,level)==null){
            //标识符未声明情况
            setError(id+"未声明",root.getLineNum());
            return false;
        }else {
            if (root.getChildCount()!=0){
                //数组
                String arrayStr = array_analyze(root.getChildAt(0),
                        table.getAllLevel(id,level).getArraySize());
                if (arrayStr!=null)
                    id+="@"+arrayStr;
                else return false;
            }
            Symbol symbol = table.getAllLevel(id,level);
            if (symbol.getIntValue().equals("") && symbol.getRealValue().equals("")
                && symbol.getCharValue().equals("") && symbol.getStringValue().equals("")){
                setError("变量"+id+"在使用前未初始化",root.getLineNum());
                return false;
            }else
                return true;
        }
    }

    /**
     * array
     *
     * @param root
     * 数组结点
     * @param arraySize
     * 数组大小
     * @return 出错返回null
     */
    private String array_analyze(TreeNode root,int arraySize){
        if (root.getTag()==Tag.INTNUM){
            int arrayIndex=Integer.parseInt(root.getContent());//数组下标
            if (arrayIndex>-1 && arrayIndex<arraySize){
                return root.getContent();
            }else if (arrayIndex<0){
                setError("数组下标不能为负数",root.getLineNum());
                return null;
            }else{
                setError("数组越界",root.getLineNum());
                return null;
            }
        }else if (root.getTag()==Tag.ID){
            //下标为标识符
            if (checkID(root,level)){
                Symbol temp = table.getAllLevel(root.getContent(),level);
                if (temp.getTag()==Tag.INT){
                    int arrayIndex=Integer.parseInt(temp.getIntValue());
                    if (arrayIndex>-1 && arrayIndex<arraySize){
                        return temp.getIntValue();
                    }else if (arrayIndex < 0){
                        setError("数组下标不能为负数",root.getLineNum());
                        return null;
                    }else{
                        setError("数组越界",root.getLineNum());
                        return null;
                    }
                }else{
                    setError("类型不匹配，数组下标必须为整数",root.getLineNum());
                    return null;
                }
            }else {
                return null;
            }
        }else if (root.getTag()==Tag.ADD || root.getTag()==Tag.SUB
                || root.getTag()==Tag.MUL || root.getTag()==Tag.DIVIDE){
            String arrayIndex = expression_analyze(root).getResult();
            if (arrayIndex !=null){
               if (isInteger(arrayIndex)){
                   int index = Integer.parseInt(arrayIndex);
                   if (index>-1 && index<arraySize)
                       return  arrayIndex;
                   else if (index < 0){
                       setError("数组下标不能为负数",root.getLineNum());
                       return null;
                   }else{
                       setError("数组越界",root.getLineNum());
                       return null;
                   }
               }else {
                   setError("类型不匹配，数组下标必须为整数",root.getLineNum());
                   return null;
               }
            }else
                return null;
        }
        return null;
    }

    /**
     * 分析表达式
     *
     * @param root
     * 表达式根节点
     * @return 返回计算结果
     */
    private ExpressionPart expression_analyze(TreeNode root){
        boolean isInt = true;
        boolean hasString = false;
//        String content = root.getContent();
        int rootTag = root.getTag();
        ExpressionPart part=new ExpressionPart();
        for (int i=0;i<root.getChildCount();i++){
            TreeNode temp = root.getChildAt(i);
            int tag = temp.getTag();
            String tempContent = temp.getContent();
            if (tag==Tag.INTNUM){
                part.setChild(tempContent,i);
            }
            else if (tag==Tag.REALNUM){
                part.setChild(tempContent,i);
                part.setIsInt(false);
            }else if (tag==Tag.CHAR_S){
                //将char转成int计算,int要转成string才能存入children数组
                char c = tempContent.charAt(0);
                int cToi = (int)c;
                part.setChild(String.valueOf(cToi),i);
            }else if (tag==Tag.STR){
                part.setIsString(true);
                part.setChild(tempContent,i);
            } else if (tag==Tag.ID){
                if (checkID(temp,level)){
                    if (temp.getChildCount() != 0) {
                        String s = array_analyze(temp.getChildAt(0), table
                                .getAllLevel(tempContent, level)
                                .getArraySize());
                        if (s != null)
                            tempContent += "@" + s;
                        else
                            return null;
                    }
                    Symbol symbol = table.getAllLevel(temp.getContent(),level);
                    if (symbol.getTag()==Tag.INT)
                        part.setChild(symbol.getIntValue(),i);
                    else if (symbol.getTag()==Tag.REAL){
                        part.setChild(symbol.getIntValue(),i);
                        part.setIsInt(false);
                    }else if (symbol.getTag()==Tag.CHAR){
                        String s = symbol.getCharValue();
                        char c = s.charAt(0);
                        int cToi = (int)c;
                        part.setChild(String.valueOf(cToi),i);
                    }else if ((symbol.getTag()==Tag.STRING)){
                        part.setChild(symbol.getIntValue(),i);
                        part.setIsString(true);
                    }
                }else{
                    return null;
                }
            }else if (root.getTag()==Tag.ADD || root.getTag()==Tag.SUB
                    || root.getTag()==Tag.MUL || root.getTag()==Tag.DIVIDE){
                ExpressionPart exp = expression_analyze(root.getChildAt(i));
                if (exp!=null){
                    part.setChild(exp.getResult(),i);
                    if (!exp.isInt() && !exp.isString())
                        part.setIsInt(false);
                }else
                    return null;
            }
        }
        if (part.isInt()){
            int child1 = Integer.parseInt(part.getChild1());
            int child2 = Integer.parseInt(part.getChild2());
            if (rootTag==Tag.ADD){
                part.setResult(String.valueOf(child1+child2));
                return part;
            }
            else if (rootTag==Tag.SUB){
                part.setResult(String.valueOf(child1-child2));
                return part;
            }
            else if (rootTag==Tag.MUL){
                part.setResult(String.valueOf(child1*child2));
                return part;
            }
            else if (rootTag==Tag.DIVIDE){
                if (child2==0){
                    setError("除数不能为0",root.getLineNum());
                    return null;
                }else{
                    part.setResult(String.valueOf(child1/child2));
                    return part;
                }
            }else
                return null;
        }else if (!part.isInt() && !part.isString()){
            BigDecimal bg1 = new BigDecimal(part.getChild1());
            BigDecimal bg2 = new BigDecimal(part.getChild2());
            if (rootTag==Tag.ADD){
                part.setResult(String.valueOf(bg1.add(bg2)));
                return part;
            }
            else if (rootTag==Tag.SUB){
                part.setResult(String.valueOf(bg1.subtract(bg2)));
                return part;
            }
            else if (rootTag==Tag.MUL){
                part.setResult(String.valueOf(bg1.multiply(bg2)));
                return part;
            }
            else if (rootTag==Tag.DIVIDE){
                try {
                    part.setResult(String.valueOf(bg1.divide(bg2)));
                    return part;
                } catch (ArithmeticException e) {
                    setError("除数不能为0",root.getLineNum());
                    return null;
                }
            }else
                return null;
        }else {
            String s1 = part.getChild1();
            String s2 = part.getChild2();
            if (root.getTag()==Tag.ADD){
                part.setResult(s1+s2);
                return part;
            }else {
                setError("字符串不允许除加法以外的算术运算",root.getLineNum());
                return null;
            }
        }
    }
}
