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

    public static void main(String args[]){

        String s = "2";
        String  a ="c";

        char n = 'f';
        int i =2222;
        double f =3.2;
        n=a.charAt(0);
        Object[] o = new Object[2];
        o[0]=i;
        o[1]=n;
        char d=s.charAt(0);
        double e = f*n;
        boolean b =isInteger(s);
        System.out.println((int)d);
    }
}
