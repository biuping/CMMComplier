package main.lexer;

public class Token {
    private final int tag;
    private String content;
    private int lineNum;
    private int col;

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public Token(int tag) {
        this.tag = tag;
    }

    public Token(int tag,String content,int lineNum,int col){
        this.tag=tag;
        this.content =content;
        this.lineNum=lineNum;
        this.col=col;
    }

    public String toString(){
        return "<"+Tag.getValue(tag)+" " + content +"···"+"第"+lineNum+"行,第"+col+"列>";
    }

    public int getTag() {
        return tag;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPosition(){
        return ""+lineNum+"行"+col+"列";
    }

}
