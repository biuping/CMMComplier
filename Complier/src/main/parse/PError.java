package main.parse;

public class PError {
    private String content;
    private int lineNum;
    private int col;

    public String getContent() {
        return content;
    }

    public int getLineNum() {
        return lineNum;
    }

    public int getCol() {
        return col;
    }

    public PError(String con, int l, int c){
        this.content=con;
        this.lineNum=l;
        this.col=c;
    }

    public String toString(){
        String str = "ERROR:第"+lineNum+"行，第"+col+"列:"+content;
        return str;
    }

}
