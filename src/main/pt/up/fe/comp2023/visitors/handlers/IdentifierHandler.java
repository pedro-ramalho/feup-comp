package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;


public class IdentifierHandler implements Handler {
    private String identifier;
    private String method;
    private String extension;
    private MySymbolTable symbolTable;
    public IdentifierHandler(String identifier, String method, String extension, MySymbolTable symbolTable) {
        this.identifier = identifier;
        this.method = method;
        this.extension = extension;
        this.symbolTable = symbolTable;
    }

    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        return splitImport[splitImport.length - 1];
    }

    private boolean isClassExtension(String id) {
        return id.equals(this.extension);
    }

    private boolean isClassItself(String id) {
        return id.equals(this.symbolTable.getClassName());
    }

    private boolean isImport(String id) {
        for (String imp : this.symbolTable.getImports()) {
            if (id.equals(this.parseImport(imp))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Type getType() {
        /* check if the identifier is a class extension */
        if (this.isClassExtension(this.identifier)) {
            return MyType.EXTENSION;
        }

        /* check if the identifier is the class itself */
        if (this.isClassItself(this.identifier)) {
            return MyType.THIS;
        }

        /* check if the identifier is an import */
        if (this.isImport(this.identifier)) {
            return MyType.IMPORT;
        }

        /* check if the identifier is a method's local variable */
        for (Symbol symbol : symbolTable.getLocalVariables(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                String typeName = type.getName();

                /* check if it's an extension */
                if (this.isClassExtension(typeName)) {
                    return MyType.EXTENSION;
                }

                /* check if it's an import */
                if (this.isImport(typeName)) {
                    return MyType.IMPORT;
                }

                return symbol.getType();
            }

        }

        /* check if the identifier is a method parameter */
        for (Symbol symbol : symbolTable.getParameters(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                String typeName = type.getName();

                /* check if it's an extension */
                if (this.isClassExtension(typeName)) {
                    return MyType.EXTENSION;
                }

                /* check if it's an import */
                if (this.isImport(typeName)) {
                    return MyType.IMPORT;
                }

                return symbol.getType();
            }
        }

        /* check if the identifier is a class field */
        for (Symbol symbol : symbolTable.getFields()) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                String typeName = type.getName();

                /* check if it's an extension */
                if (this.isClassExtension(typeName)) {
                    return MyType.EXTENSION;
                }

                /* check if it's an import */
                if (this.isImport(typeName)) {
                    return MyType.IMPORT;
                }

                return symbol.getType();
            }
        }

        return null;
    }
}
