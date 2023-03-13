package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2023.MethodInfo;

import javax.swing.plaf.synth.SynthButtonUI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MySymbolTable implements SymbolTable {
    private String name;
    private List<String> imports = new ArrayList<>();
    private List<Symbol> fields = new ArrayList<>();
    private HashMap<String, MethodInfo> methods = new HashMap<>();
    private String extension;


    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.name;
    }

    @Override
    public String getSuper() {
        return this.extension;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return this.methods.keySet().stream().toList();
    }

    @Override
    public Type getReturnType(String methodName) {
        MethodInfo method = this.methods.get(methodName);
        if (method != null) {
            return method.getReturnType();
        } else {
            return null;
        }
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        MethodInfo method = this.methods.get(methodName);
        if (method != null) {
            return method.getParameters();
        } else {
            return null;
        }
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        MethodInfo method = this.methods.get(methodName);
        if (method != null) {
            return method.getLocalVariables();
        } else {
            return null;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean addImport(String importName) {
        return this.imports.add(importName);
    }

    public boolean addField(Symbol field) {
        return this.fields.add(field);
    }

    public boolean addMethod(String methodName, Type returnType) {
        if (this.methods.get(methodName) != null) return false;
        MethodInfo method = new MethodInfo(methodName, returnType);
        this.methods.put(methodName, method);
        return true;
    }

    public boolean addParameter(String methodName, Symbol parameter) {
        if (this.methods.get(methodName) == null) return false;
        MethodInfo method = this.methods.get(methodName);
        method.addParameter(parameter);
        return true;
    }

    public boolean addLocalVariable(String methodName, Symbol localVariable) {
        if (this.methods.get(methodName) == null) return false;
        MethodInfo method = this.methods.get(methodName);
        method.addLocalVariable(localVariable);
        return true;
    }


}
