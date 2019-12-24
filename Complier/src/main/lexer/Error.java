package main.lexer;

/***
 * @author 毕修平
 * 词法分析错误处理
 */
public class Error {
    private String reason;
    private String value;
    private int lineNum;
    private int col;

    public Error(String v,String r,int lineNum,int col){
        this.value=v;
        this.reason=r;
        this.lineNum=lineNum;
        this.col=col;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String toString(){
        return "ERROR:"+value+reason +"---第"+lineNum+"行,第"+col+"列";
    }


}
