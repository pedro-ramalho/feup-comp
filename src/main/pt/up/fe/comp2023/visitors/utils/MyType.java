package pt.up.fe.comp2023.visitors.utils;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class MyType extends Type {
    public final static Type INT = new Type("int", false);
    public final static Type INT_ARRAY = new Type("int", true);
    public final static Type STRING = new Type("String", false);
    public final static Type STRING_ARRAY = new Type("String", true);
    public final static Type BOOLEAN = new Type("boolean", false);
    public final static Type THIS = new Type("this", false);
    public final static Type IMPORT = new Type("import", false);
    public final static Type EXTENSION = new Type("extension", false);
    public MyType(String name, boolean isArray) {
        super(name, isArray);
    }

    public boolean isInt() {
        return super.equals(INT);
    }

    public boolean isIntArray() {
        return super.equals(INT_ARRAY);
    }

    public boolean isString() {
        return super.equals(STRING);
    }

    public boolean isStringArray() {
        return super.equals(STRING_ARRAY);
    }

    public boolean isBoolean() {
        return super.equals(BOOLEAN);
    }

    public boolean isThis() {
        return super.equals(THIS);
    }

    public boolean isImport() {
        return super.equals(IMPORT);
    }

    public boolean isExtension() {
        return super.equals(EXTENSION);
    }
}
