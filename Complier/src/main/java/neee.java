package main.java;

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

        String s = "\t";
        String  a ="c";

        char n = 'c';
        int aaa=(int)n;
        int i =2222;
        double f =0.0001;
        a=a+f;
        int dd[]={2,3};
        n=a.charAt(0);
        Object[] o = new Object[2];
        o[0]=i;
        o[1]=n;
        char d=s.charAt(0);
        double e = f*n;
        boolean b =isInteger(s);
        change(a);
        System.out.println(isESC(s));
    }
}
