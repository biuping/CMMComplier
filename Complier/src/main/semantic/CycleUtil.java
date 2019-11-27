package main.semantic;

import java.util.ArrayList;

public class CycleUtil {
    private ArrayList<Integer> levelList;   //存储循环作用域
    private boolean isCycle;   //判断是否处于循环中，当levelList为空时退出循环为false
    private int breakCount;    //记录break执行次数，在所属的循环将其减一

    public  CycleUtil(){
        levelList= new ArrayList<>();
        isCycle=false;
        breakCount=0;
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
        this.addLevel(level);
    }

    //退出循环
    public void outCycle(){
        this.removeLastLevel();
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
