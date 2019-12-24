package main.semantic;

import main.lexer.Tag;

/***
 * @author 毕修平
 * 语义分析用于计算对象
 */

public class ExpressionPart {
    private String[] children=new String[2];
    private String result;
    private boolean isInt;
    private boolean isString;
    private int childCount;

    public ExpressionPart(){
        children=new String[2];
        result="";
        isInt=true;
        isString=false;
    }

    public int getTag(){
        if (isString)
            return Tag.STRING;
        else if (isInt)
            return Tag.INT;
        else
            return Tag.REAL;
    }

    public ExpressionPart(String child1, String child2){
        children[0]=child1;
        children[1]=child2;
        isInt=true;
        result="";
    }

    public void setChild(String s,int i){
        children[i]=s;
    }

    public int getChildCount(){
        int count=0;
        for (int i=0;i<children.length;i++){
            if (children[i]!=null)
                count++;
        }
        return count;
    }

    public String getChild(int i){
        return children[i];
    }

    public String[] getChildren() {
        return children;
    }

    public String getChild1(){
        return children[0];
    }

    public void setChild1(String s){
        this.children[0]=s;
    }

    public String getChild2(){
        return children[1];
    }

    public void setChild2(String s){
        this.children[1]=s;
    }

    public void setChildren(String[] children) {
        this.children = children;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isInt() {
        return isInt;
    }

    public void setIsInt(boolean anInt) {
        isInt = anInt;
    }

    public boolean isString() {
        return isString;
    }

    public void setIsString(boolean string) {
        isString = string;
    }
}
