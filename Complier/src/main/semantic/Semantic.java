package main.semantic;

import main.parse.TreeNode;

public class Semantic {
    // 符号表
    private SymbolTable table = new SymbolTable();
    // 语法树
    private TreeNode root;
    // 语义分析错误个数
    private int errorNum = 0;
    // 语义分析标识符作用域
    private int level = 0;

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


}
