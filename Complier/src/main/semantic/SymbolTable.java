package main.semantic;

import java.util.Vector;

/***
 * @author 毕修平
 * 语义分析符号表
 */

public class SymbolTable {
    private Vector<Symbol> symbolTable = new Vector<Symbol>();

    //获取符号表中元素
    public Symbol get(int index){
        return symbolTable.get(index);
    }

    //根据symbol的名字对所有作用域进行查找
    public Symbol getAllLevel(String name, int level){
        while (level > -1) {
            for (Symbol symbol : symbolTable) {
                if (symbol.getName().equals(name)
                        && symbol.getLevel() == level) {
                    return symbol;
                }
            }
            level--;
        }
        return null;
    }

    //根据名字对当前作用域进行查找
    public Symbol getCurrentLevel(String name, int level) {
        for (Symbol symbol : symbolTable) {
            if (symbol.getName().equals(name) && symbol.getLevel() == level) {
                return symbol;
            }
        }
        return null;
    }

    //添加symbol
    public boolean addSymbol(Symbol symbol) {
        return symbolTable.add(symbol);
    }

    public void addSymbol(int index, Symbol symbol) {
        symbolTable.add(index, symbol);
    }

    // 根据索引移出symbol
    public void remove(int index) {
        symbolTable.remove(index);
    }
    
    //移出指定名字和作用域的symbol
    public void remove(String name, int level) {
        for (int i = 0; i < size(); i++) {
            if (get(i).getName().equals(name) && get(i).getLevel() == level) {
                remove(i);
                return;
            }
        }
    }
    
    //清空
    public void removeAll() {
        symbolTable.clear();
    }
    
    //当level减小时更新符号表,去除无用的元素
    public void update(int level) {
        for (int i = 0; i < size(); i++) {
            if (get(i).getLevel() > level) {
                remove(i);
                i--;
            }
        }
    }
    
    //判断是否存在指定symbol
    public boolean contains(Symbol symbol) {
        return symbolTable.contains(symbol);
    }

    //判断是否为空
    public boolean isEmpty() {
        return symbolTable.isEmpty();
    }
    
    //返回symbolTable元素个数
    public int size() {
        return symbolTable.size();
    }
}
