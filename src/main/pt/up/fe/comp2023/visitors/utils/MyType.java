package pt.up.fe.comp2023.visitors.utils;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class MyType {
    private String name;
    private String origin;
    private boolean array;

    public MyType(String name, String origin, boolean array) {
        this.name = name;
        this.origin = origin;
        this.array = array;
    }

    public String getName() {
        return this.name;
    }
    public String getOrigin() { return this.origin; }

    public boolean isMethod() {
        return this.origin.equals("method");
    }

    public boolean isPrimitive() {
        return this.origin.equals("primitive");
    }

    public boolean isObject() {
        return this.origin.equals("object");
    }

    public boolean isInt() {
        return this.name.equals("int") && !this.array;
    }

    public boolean isIntArray() {
        return this.name.equals("int") && this.array;

    }

    public boolean isArray() {
        return this.array;
    }

    public boolean isBoolean() {
        return this.name.equals("boolean") && !this.array;
    }

    public boolean isImport() {
        return this.name.equals("import");
    }

    public boolean isExtension() {
        return this.name.equals("extension");
    }

    public boolean isThis() {
        return this.name.equals("this");
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            MyType other = (MyType)obj;
            if (this.array != other.array) {
                return false;
            } else {
                if (this.name == null) {
                    return other.name == null;
                } else return this.name.equals(other.name);
            }
        }
    }
}
