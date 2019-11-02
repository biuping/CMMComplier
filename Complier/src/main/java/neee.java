package main.java;

public class neee {

    private static boolean isESCn(String s){
        if (s.equals("\\\"")||s.equals("\\\'")||s.equals("\\n")||s.equals("\\t")
                ||s.equals("\\r")||s.equals("\\\\"))
            return true;
        else
            return false;
    }

    public static void main(String args[]){

        String s = "\\\'";
        boolean b =isESCn(s);
    }
}
