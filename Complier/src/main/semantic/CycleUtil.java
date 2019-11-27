package main.semantic;

import java.util.ArrayList;

public class CycleUtil {
    private ArrayList<Integer> levelList;   //存储循环作用域
    private ArrayList<Integer> cLevelList;  //continue使用的作用域集合
    private boolean isCycle;   //判断是否处于循环中，当levelList为空时退出循环为false
    private int breakCount;    //记录break执行次数，在所属的循环将其减一
    private int continueCount;  //记录continue执行次数，进行下次循环时减一

    public  CycleUtil(){
        levelList= new ArrayList<>();
        cLevelList = new ArrayList<>();
        isCycle=false;
        breakCount=0;
        continueCount=0;
    }

    //添加level
    public void addLevel(int level){
        levelList.add(level);
    }

    //break执行
    public void breakCountAdd(){
        this.breakCount++;
    }

    //break所属循环被break中断
    public void breakCountSub(){
        this.breakCount--;
    }

    public int getBreakCount(){
        return breakCount;
    }

    //得到continue所在循环的level值
    public int getContinueLevel(){
        return cLevelList.get(cLevelList.size()-1);
    }

    //continue执行
    public void continueCountAdd(){
        this.continueCount++;
    }

    public int getContinueCount(){
        return continueCount;
    }

    //continue所属循环进行下一次循环
    public void continueCountSub(){
        this.continueCount--;
    }

    //判断list是否为空
    public boolean isEmpty(){
        return levelList.isEmpty();
    }

    //获取最近的作用域值
    public int getLastLevel(){
        return levelList.get(levelList.size()-1);
    }

    //移出最后一个level
    public void removeLastLevel(){
        levelList.remove(levelList.size()-1);
    }

    //进入循环
    public void intoCycle(int level){
        isCycle=true;
        levelList.add(level);
        cLevelList.add(level+1);
    }

    //退出循环
    public void outCycle(){
        this.removeLastLevel();
        cLevelList.remove(cLevelList.size()-1);
        if (breakCount!=0)
            this.breakCountSub();
        if (this.isEmpty())
            isCycle=false;
    }

    public ArrayList<Integer> getLevelList() {
        return levelList;
    }

    public void setLevelList(ArrayList<Integer> levelList) {
        this.levelList = levelList;
    }

    public boolean isCycle() {
        return isCycle;
    }

    public void setCycle(boolean cycle) {
        isCycle = cycle;
    }

}
