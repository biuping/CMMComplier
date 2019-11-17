package main.semantic;

public class Symbol {
    // 名字
    private String name;
    // 类型
    private int tag;
    // 所在行号
    private int lineNum;
    // 作用域
    private int level;
    // 整形数值
    private String intValue;
    // 实数数值
    private String realValue;
    // 字符
    private String charValue;
    // 字符串
    private String stringValue;
    // bool
    private String boolValue;
    // 是否为数组,0表示不是,正整数表示数组的大小
    private int arraySize;

    public Symbol(String name,int tag,int lineNum, int level){
        this.name = name;
        this.tag = tag;
        this.lineNum = lineNum;
        this.level = level;
        this.intValue = "";
        this.realValue = "";
        this.stringValue = "";
        this.arraySize = 0;
        this.charValue = "";
        this.boolValue = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTag() {
        return tag;
    }

    public void setKind(int tag) {
        this.tag = tag;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getIntValue() {
        return intValue;
    }

    public void setIntValue(String intValue) {
        this.intValue = intValue;
    }

    public String getRealValue() {
        return realValue;
    }

    public void setRealValue(String realValue) {
        this.realValue = realValue;
    }

    public String  getCharValue() {
        return charValue;
    }

    public void setCharValue(String charValue) {
        this.charValue = charValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getArraySize() {
        return arraySize;
    }

    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
    }

    public String toString(){
        return name + "_" + tag + "_" + level + "_" + arraySize;
    }

    public boolean equals(Object object) {
        Symbol element = (Symbol)object;
        return this.toString().equals(element.toString());
    }
    public String getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(String boolValue) {
        this.boolValue = boolValue;
    }
}
