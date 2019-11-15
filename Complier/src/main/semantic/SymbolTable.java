package main.semantic;

import java.util.Vector;

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
        for (Symbol element : symbolTable) {
            if (element.getName().equals(name) && element.getLevel() == level) {
                return element;
            }
        }
        return null;
    }

    //添加symbol
    public boolean addSymbol(Symbol element) {
        return symbolTable.add(element);
    }

    public void addSymbol(int index, Symbol element) {
        symbolTable.add(index, element);
    }


}
