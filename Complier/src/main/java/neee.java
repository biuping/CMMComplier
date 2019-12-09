package main.java;

import java.util.Scanner;

public class neee {

    private static boolean isInteger(String s){
        if (s.matches("^(\\-|\\+)?[\\s]*[0-9]\\d*$") ||s.matches("^(\\-|\\+?)0$")
                && !s.matches("^-?0{1,}\\d+$"))
            return true;
        else
            return false;
    }

    private static boolean isESCn(String s){
        if (s.equals("\\\"")||s.equals("\\\'")||s.equals("\\n")||s.equals("\\t")
                ||s.equals("\\r")||s.equals("\\\\"))
            return true;
        else
            return false;
    }

    public static void change(String  a){
        a="2323x";
    }

    private static boolean isEsc_char(String str){
        if (str.contains("\\"))
            return true;
        else
            return false;
    }

    private static boolean isESC(String s){
        if (s.equals("\\\"")||s.equals("\\\'")||s.equals("\\n")||s.equals("\\t")
                ||s.equals("\\r")||s.equals("\\\\"))
            return true;
        else
            return false;
    }

    private static boolean isReal(String s) {
        if (s.matches("^((\\-|\\+)?[\\s]*\\d+)(\\.\\d+)+$"))
            return true;
        else
            return false;
    }

    public static void main(String args[]){
        int m,n;
        n=6;m=00000;
        double d = 000.000;
        String s = "ffww";
        System.out.println(s.length());

    }
}
