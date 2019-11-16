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
     * 分析条件语句
     * @param root
     * 条件语句根节点
     * @return 条件语句结果
     */
    private boolean condition_analyze(TreeNode root){
        String content = root.getContent();
        if (isInteger(content)){
            return Integer.parseInt(content) != 0;
        }//Todo else if ()
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

            }
        }
        return false;
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
            //todo expression()
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
    private String expression_analyze(TreeNode root){
        boolean isInt = true;
//        String content = root.getContent();
        int rootTag = root.getTag();
        String[] children = new String[2];
        for (int i=0;i<root.getChildCount();i++){
            TreeNode temp = root.getChildAt(i);
            int tag = temp.getTag();
            String tempContent = temp.getContent();
            if (tag==Tag.INTNUM){
                children[i]=tempContent;
            }
            else if (tag==Tag.REALNUM){
                children[i]=tempContent;
                isInt=false;
            }else if (tag==Tag.CHAR_S){
                //将char转成int计算,int要转成string才能存入children数组
                char c = tempContent.charAt(0);
                int cToi = (int)c;
                children[i]=String.valueOf(cToi);
            }else if (tag==Tag.ID){
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
                        children[i]=symbol.getIntValue();
                    else if (symbol.getTag()==Tag.REAL){
                        children[i]=symbol.getRealValue();
                        isInt=false;
                    }else if (symbol.getTag()==Tag.CHAR){
                        char c = symbol.getCharValue();
                        int cToi = (int)c;
                        children[i]=String.valueOf(cToi);
                    }
                }else{
                    return null;
                }
            }else if (root.getTag()==Tag.ADD || root.getTag()==Tag.SUB
                    || root.getTag()==Tag.MUL || root.getTag()==Tag.DIVIDE){
                String exp = expression_analyze(root.getChildAt(i));
                if (exp!=null){
                    children[i]=exp;
                    if (isReal(exp))
                        isInt=false;
                }else
                    return null;
            }
        }
        if (isInt){
            int child1 = Integer.parseInt(children[0]);
            int child2 = Integer.parseInt(children[1]);
            if (rootTag==Tag.ADD)
                return String.valueOf(child1+child2);
            else if (rootTag==Tag.SUB)
                return String.valueOf(child1-child2);
            else if (rootTag==Tag.MUL)
                return String.valueOf(child1*child2);
            else if (rootTag==Tag.DIVIDE){
                if (child2==0){
                    setError("除数不能为0",root.getLineNum());
                    return null;
                }else
                    return String.valueOf(child1/child2);
            }
        }else {
            BigDecimal bg1 = new BigDecimal(children[0]);
            BigDecimal bg2 = new BigDecimal(children[1]);
            if (rootTag==Tag.ADD)
                return String.valueOf(bg1.add(bg2));
            else if (rootTag==Tag.SUB)
                return String.valueOf(bg1.subtract(bg2));
            else if (rootTag==Tag.MUL)
                return String.valueOf(bg1.multiply(bg2));
            else if (rootTag==Tag.DIVIDE){
                try {
                    return String.valueOf(bg1.divide(bg2));
                } catch (ArithmeticException e) {
                    setError("除数不能为0",root.getLineNum());
                    return null;
                }
            }
        }
        return "";
    }
}
