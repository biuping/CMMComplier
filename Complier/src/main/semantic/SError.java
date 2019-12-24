package main.semantic;

/***
 * @author 毕修平
 * 语义分析错误处理
 */

public class SError {
    private String content;
    private int lineNum;
    private int errorNum;

    public String getContent() {
        return content;
    }

    public int getLineNum() {
        return lineNum;
    }

    public SError(String con, int l, int n){
        this.content=con;
        this.lineNum=l;
        this.errorNum=n;
    }

    public String toString(){
        String str = "ERROR"+errorNum+":第"+lineNum+"行,"+content;
        return str;
    }
}
