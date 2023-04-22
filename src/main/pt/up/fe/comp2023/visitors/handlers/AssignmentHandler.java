package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2023.MySymbolTable;

public class AssignmentHandler implements Handler {
    private String id;
    private String method;
    private String extension;
    private MySymbolTable symbolTable;
    public AssignmentHandler(String id, String method, String extension, MySymbolTable symbolTable) {
        this.id = id;
        this.method = method;
        this.extension = extension;
        this.symbolTable = symbolTable;
    }
    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        String ret = splitImport[splitImport.length - 1];

        return splitImport[splitImport.length - 1];
    }

    private boolean isImport(String str) {
        for (String imp : this.symbolTable.getImports()) {
            if (str.equals(this.parseImport(imp))) {
                return true;
            }
        }

        return false;
    }

    private boolean isExtension(String str) {
        return str.equals(this.extension);
    }

    @Override
    public Type getType() {
        /* check if the id is a class extension */
        if (this.isExtension(this.id)) {
            return new Type("extension", false);
        }

        /* check if the id is the class itself */
        if (this.id.equals(this.symbolTable.getClassName())) {
            return new Type("this", false);
        }

        /* check if the id is from an import */
        for (String str : symbolTable.getImports()) {
            if (this.id.equals(this.parseImport(str))) {
                return new Type("import", false);
            }
        }

        /* check if the id is a method's local variable */
        for (Symbol symbol : symbolTable.getLocalVariables(this.method)) {
            if (this.id.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (this.isExtension(type.getName())) {
                    return new Type("extension", false);
                }

                if (this.isImport(type.getName())) {
                    return new Type("import", false);
                }

                return type;
            }
        }

        /* check if the id is a method's parameter */
        for (Symbol symbol : symbolTable.getParameters(this.method)) {
            if (this.id.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (this.isExtension(type.getName())) {
                    return new Type("extension", false);
                }

                if (this.isImport(type.getName())) {
                    return new Type("import", false);
                }

                return type;
            }
        }

        /* check if the identifier is a class field */
        for (Symbol symbol : symbolTable.getFields()) {
            if (this.id.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (this.isExtension(type.getName())) {
                    return new Type("extension", false);
                }

                if (this.isImport(type.getName())) {
                    return new Type("import", false);
                }

                return type;
            }
        }

        return null;
    }
}
