package main.parse;

import javax.swing.tree.DefaultMutableTreeNode;
/***
 * @author 毕修平
 * 词法分析树
 */
public class TreeNode extends DefaultMutableTreeNode {
    private String kind;
    private String content;
    private int tag;

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    private int lineNum;

    public String  getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String toString(){
        if (lineNum==0){
            return kind+" "+content;
        }
        return kind+" "+content+" 第"+lineNum+"行";
    }

    public TreeNode(){
        super();
        this.kind ="";
        this.content ="";
    }

    public TreeNode(String context){
        super(context);
        this.content =context;
        this.kind ="";
    }

    public TreeNode(String  k, String content) {
        super(content);
        this.content = content;
        this.kind = k;
    }

    public TreeNode(String  k, String content,int lineNum) {
        super(content);
        this.content = content;
        this.lineNum = lineNum;
        this.kind = k;
    }

    public TreeNode(String  k, String content,int t,int lineNum) {
        super(content);
        this.kind = k;
        this.content = content;
        this.tag=t;
        this.lineNum = lineNum;
    }

    //添加孩子节点
    public void add(TreeNode childNode) {
        super.add(childNode);
    }

    public TreeNode getChildAt(int index) {
        return (TreeNode) super.getChildAt(index);
    }




}
