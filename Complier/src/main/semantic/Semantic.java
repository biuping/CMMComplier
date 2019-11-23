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
        if (s.equals("\\\"") || s.equals("\\\'") || s.equals("\\n") || s.equals("\\t")
                || s.equals("\\r") || s.equals("\\\\"))
            return true;
        else
            return false;
    }

    //判断数组赋值类型是否匹配
    private boolean judgeArrTypeEqual(int declareTag,int assignTag){
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
        statement(root);

        if (errorNum!=0){
            System.out.println("该程序中共有" + errorNum + "个语义错误！");
            for (int i = 0;i<errors.size();i++){
                System.out.println(errors.get(i).toString());
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
            if (tag == Tag.IF) {
                //进入if代码段，作用域改变
                level++;

            } else if (tag == Tag.INT || tag == Tag.REAL
                    || tag == Tag.CHAR || tag == Tag.STRING || tag == Tag.BOOL) {
                declare_analyze(currentNode);
            } else if (tag == Tag.PRINT) {
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
        //todo 条件语句判断
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
                    int arrayTag = child.getChildAt(0).getTag();
                    String sizeValue = child.getChildAt(0).getContent();
                    if (arrayTag == Tag.INTNUM) {
                        int arraySize = Integer.parseInt(sizeValue);
                        if (arraySize < 1) {
                            setError("数组大小必须大于0", root.getLineNum());
                            return;
                        }
                    } else if (arrayTag == Tag.ID) {
                        if (checkID(root, level)) {
                            Symbol tempSymbol = table.getAllLevel(child.getChildAt(0).getContent(), level);
                            if (tempSymbol.getTag() == Tag.INT) {
                                int arraySize = Integer.parseInt(tempSymbol.getIntValue());
                                if (arraySize < 1) {
                                    setError("数组大小必须大于0", root.getLineNum());
                                    return;
                                } else {
                                    sizeValue = String.valueOf(arraySize);
                                }
                            } else {
                                setError("类型不匹配，数组大小必须为整型", child.getLineNum());
                                return;
                            }
                        } else {
                            return;
                        }
                    } else if (arrayTag == Tag.ADD || arrayTag == Tag.SUB
                            || arrayTag == Tag.MUL || arrayTag == Tag.DIVIDE) {
                        ExpressionPart part = expression_analyze(child.getChildAt(0));
                        if (part != null) {
                            if (part.isInt()) {
                                int arraySize = Integer.parseInt(part.getResult());
                                if (arraySize < 1) {
                                    setError("数组大小必须大于0", root.getLineNum());
                                    return;
                                } else {
                                    sizeValue = String.valueOf(arraySize);
                                }
                            } else {
                                setError("类型不匹配，数组大小必须为整型", child.getLineNum());
                                return;
                            }
                        } else
                            return;
                    }
                    symbol.setArraySize(Integer.parseInt(sizeValue));
                    table.addSymbol(symbol);
                    index++;
                    if (index < root.getChildCount() &&
                            root.getChildAt(index).getTag() == Tag.ASSIGN) {
                        TreeNode items = root.getChildAt(index).getChildAt(0);
                        int count = items.getChildCount();
                        if (count == Integer.parseInt(sizeValue)) {
                            for (int j = 0; j < count; j++) {
                                //大括号声明的数组元素
                                TreeNode item = items.getChildAt(j);
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
                        } else {
                            ++index;
                            setError("数组大小与声明大小不匹配", child.getLineNum());
                            return;
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
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE) {
                ExpressionPart part = expression_analyze(valueNode);
                String result = part.getResult();
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
            } else if (valueNode.getTag() == Tag.REAL) {
                symbol.setRealValue(String.valueOf(Double.parseDouble(value)));
            } else if (valueNode.getTag() == Tag.CHAR_S) {
                char c ;
                if (isEsc_char(valueNode.getContent()))
                    c=valueNode.getContent().charAt(1);
                else
                    c=valueNode.getContent().charAt(0);
                symbol.setRealValue(String.valueOf((double)c));
            }
            else if (valueNode.getTag() == Tag.STR)
                setError("不能将string字符串赋值给real型变量", valueNode.getLineNum());
            else if (valueNode.getTag() == Tag.TRUE )
                symbol.setRealValue("1");
            else if ( valueNode.getTag() == Tag.FALSE)
                symbol.setRealValue("0");
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
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE) {
                ExpressionPart part = expression_analyze(valueNode);
                String result = part.getResult();
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
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE) {
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
                symbol.setStringValue(part.getResult());
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
                    || valueNode.getTag() == Tag.MUL || valueNode.getTag() == Tag.DIVIDE) {
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
     * print语句分析
     *
     * @param root print节点
     */
    private void print_analyze(TreeNode root) {
        int tag = root.getTag();
        String content = root.getContent();
        if (tag == Tag.INTNUM || tag == Tag.REALNUM ||
                tag == Tag.CHAR_S || tag == Tag.STR ||
                tag == Tag.TRUE || tag == Tag.FALSE) {
            System.out.println(root.getContent());
        } else if (tag == Tag.ID) {
            if (checkID(root, level)) {
                if (root.getChildCount() > 0) {
                    String array = array_analyze(root.getChildAt(0),
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
                    System.out.println(symbol.getStringValue());
                }else if (symbol.getTag()==Tag.CHAR){
                    System.out.println(symbol.getCharValue());
                }else if (symbol.getTag()==Tag.BOOL){
                    if (symbol.getBoolValue().equals("0"))
                        System.out.println("false");
                    else
                        System.out.println("true");
                }
            }else
                setError("标识符"+root.getContent()+"未声明",root.getLineNum());
        }else if (tag==Tag.ADD || tag==Tag.SUB ||
                tag==Tag.MUL ||tag==Tag.DIVIDE){
            ExpressionPart exp = expression_analyze(root);
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
        } else if (root.getTag() == Tag.TRUE) {
            return true;
        } else if (root.getTag() == Tag.FALSE) {
            return false;
        } else if (tag == Tag.ID) {
            if (checkID(root, level)) {
                if (root.getChildCount() != 0) {
                    String str = array_analyze(root.getChildAt(0),
                            table.getAllLevel(content, level).getArraySize());
                    if (str != null)
                        content += "@" + str;
                    else
                        return false;
                }
                Symbol symbol = table.getAllLevel(content, level);
                if (symbol.getTag() == Tag.BOOL) {
                    return symbol.getBoolValue().equals("true");
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
                            String arrStr = array_analyze(root.getChildAt(i).getChildAt(0),
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
                        || childTag == Tag.MUL || childTag == Tag.DIVIDE) {
                    ExpressionPart exp = expression_analyze(root.getChildAt(i));
                    if (exp != null)
                        children[i] = new Element(exp.getTag(), exp.getResult());
                    else
                        return false;
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
        if (table.getAllLevel(id, level) == null) {
            //标识符未声明情况
            setError(id + "未声明", root.getLineNum());
            return false;
        } else {
            if (root.getChildCount() != 0) {
                //数组
                String arrayStr = array_analyze(root.getChildAt(0),
                        table.getAllLevel(id, level).getArraySize());
                if (arrayStr != null)
                    id += "@" + arrayStr;
                else return false;
            }
            Symbol symbol = table.getAllLevel(id, level);
            if (symbol.getIntValue().equals("") && symbol.getRealValue().equals("")
                    && symbol.getCharValue().equals("") && symbol.getStringValue().equals("") &&
                symbol.getBoolValue().equals("")) {
                setError("变量" + id + "在使用前未初始化", root.getLineNum());
                return false;
            } else
                return true;
        }
    }

    /**
     * array
     *
     * @param root      数组结点
     * @param arraySize 数组大小
     * @return 出错返回null
     */
    private String array_analyze(TreeNode root, int arraySize) {
        if (root.getTag() == Tag.INTNUM) {
            int arrayIndex = Integer.parseInt(root.getContent());//数组下标
            if (arrayIndex > -1 && arrayIndex < arraySize) {
                return root.getContent();
            } else if (arrayIndex < 0) {
                setError("数组下标不能为负数", root.getLineNum());
                return null;
            } else {
                setError("数组越界", root.getLineNum());
                return null;
            }
        } else if (root.getTag() == Tag.ID) {
            //下标为标识符
            if (checkID(root, level)) {
                Symbol temp = table.getAllLevel(root.getContent(), level);
                if (temp.getTag() == Tag.INT) {
                    int arrayIndex = Integer.parseInt(temp.getIntValue());
                    if (arrayIndex > -1 && arrayIndex < arraySize) {
                        return temp.getIntValue();
                    } else if (arrayIndex < 0) {
                        setError("数组下标不能为负数", root.getLineNum());
                        return null;
                    } else {
                        setError("数组越界", root.getLineNum());
                        return null;
                    }
                } else {
                    setError("类型不匹配，数组下标必须为整数", root.getLineNum());
                    return null;
                }
            } else {
                return null;
            }
        } else if (root.getTag() == Tag.ADD || root.getTag() == Tag.SUB
                || root.getTag() == Tag.MUL || root.getTag() == Tag.DIVIDE) {
            String arrayIndex = expression_analyze(root).getResult();
            if (arrayIndex != null) {
                if (isInteger(arrayIndex)) {
                    int index = Integer.parseInt(arrayIndex);
                    if (index > -1 && index < arraySize)
                        return arrayIndex;
                    else if (index < 0) {
                        setError("数组下标不能为负数", root.getLineNum());
                        return null;
                    } else {
                        setError("数组越界", root.getLineNum());
                        return null;
                    }
                } else {
                    setError("类型不匹配，数组下标必须为整数", root.getLineNum());
                    return null;
                }
            } else
                return null;
        }
        return null;
    }

    /**
     * 分析表达式
     *
     * @param root 表达式根节点
     * @return 返回计算结果
     */
    private ExpressionPart expression_analyze(TreeNode root) {
        int rootTag = root.getTag();
        ExpressionPart part = new ExpressionPart();
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode temp = root.getChildAt(i);
            int tag = temp.getTag();
            String tempContent = temp.getContent();
            if (tag == Tag.INTNUM) {
                part.setChild(tempContent, i);
            } else if (tag == Tag.REALNUM) {
                part.setChild(tempContent, i);
                part.setIsInt(false);
            } else if (tag == Tag.CHAR_S) {
                //将char转成int计算,int要转成string才能存入children数组
                char c = tempContent.charAt(0);
                int cToi = (int) c;
                part.setChild(String.valueOf(cToi), i);
            } else if (tag == Tag.STR) {
                part.setIsString(true);
                part.setChild(tempContent, i);
            } else if (tag == Tag.ID) {
                if (checkID(temp, level)) {
                    if (temp.getChildCount() != 0) {
                        String s = array_analyze(temp.getChildAt(0), table
                                .getAllLevel(tempContent, level)
                                .getArraySize());
                        if (s != null)
                            tempContent += "@" + s;
                        else
                            return null;
                    }
                    Symbol symbol = table.getAllLevel(tempContent, level);
                    if (symbol.getTag() == Tag.INT)
                        part.setChild(symbol.getIntValue(), i);
                    else if (symbol.getTag() == Tag.REAL) {
                        part.setChild(symbol.getIntValue(), i);
                        part.setIsInt(false);
                    } else if (symbol.getTag() == Tag.CHAR) {
                        String s = symbol.getCharValue();
                        char c = s.charAt(0);
                        int cToi = (int) c;
                        part.setChild(String.valueOf(cToi), i);
                    } else if (symbol.getTag() == Tag.BOOL) {
                        if (symbol.getBoolValue().equals("false") ||
                                symbol.getBoolValue().equals("0"))
                            part.setChild("0", i);
                        else
                            part.setChild("1", i);
                    } else if (symbol.getTag() == Tag.STRING) {
                        part.setChild(symbol.getIntValue(), i);
                        part.setIsString(true);
                    }
                } else {
                    return null;
                }
            } else if (tag == Tag.ADD || tag == Tag.SUB
                    || tag == Tag.MUL || tag == Tag.DIVIDE) {
                ExpressionPart exp = expression_analyze(root.getChildAt(i));
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
