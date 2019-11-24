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

    public static void main(String args[]){

        String s="";
        String  a ="c";
        Scanner scanner = new Scanner(System.in);
        s=scanner.nextLine();
        char n = 'c';
        int aaa=(int)n;
        int i =1;
        double f =1.0;
        a=a+f;
        int dd[]={2,3};
        n=a.charAt(0);

        char d=(char)0;
        double e = f*n;
        boolean b =isInteger(s);
        if (isESC(s)){
            d=s.charAt(1);
        }else
            d=s.charAt(0);

        System.out.println(d);
        System.out.print(s);
        System.out.println(a);
    }
}
