package pt.up.fe.comp2023;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;


public class MethodInfo {

    private String name;
    private Type returnType;
    private List<Symbol> parameters = new ArrayList<>();
    private List<Symbol> localVariables = new ArrayList<>();

    public MethodInfo(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public String getName() {return this.name;}

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public void addParameter(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public void addLocalVariable(Symbol localVariable) {
        this.localVariables.add(localVariable);
    }
}

