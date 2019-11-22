package main.semantic;


public class Element {
    private int tag;
    private String value;

    public Element(int t,String v){
        this.tag=t;
        this.value=v;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
