package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;


public class IdentifierHandler implements Handler {
    private final String EXTENSION = "extension";
    private final String METHOD = "method";
    private final String IMPORT = "import";
    private final String OBJECT = "object";
    private final String THIS = "this";
    private final String PRIMITIVE = "primitive";
    private String identifier;
    private String method;
    private String extension;
    private boolean isStatic;
    private MySymbolTable symbolTable;
    public IdentifierHandler(String identifier, String method, String extension, boolean isStatic, MySymbolTable symbolTable) {
        this.identifier = identifier;
        this.method = method;
        this.extension = extension;
        this.isStatic = isStatic;
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
    public MyType getType() {
        /* check if the identifier is a class extension */
        if (this.isClassExtension(this.identifier)) {
            return new MyType(this.identifier, this.METHOD, false);
        }

        /* check if the identifier is the class itself */
        if (this.isClassItself(this.identifier)) {
            return new MyType(this.identifier, this.METHOD, false);
        }

        /* check if the identifier is an import */
        if (this.isImport(this.identifier)) {
            return new MyType(this.identifier, this.METHOD, false);
        }

        /* check if the identifier is a method's local variable */
        for (Symbol symbol : this.symbolTable.getLocalVariables(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                String typename = type.getName();

                if (this.isClassExtension(typename))
                    return new MyType(typename, this.OBJECT, false);

                if (this.isClassItself(typename))
                    return new MyType(typename, this.THIS, false);

                if (this.isImport(typename))
                    return new MyType(typename, this.OBJECT, false);

                return new MyType(typename, this.PRIMITIVE, type.isArray());
            }
        }

        /* check if the identifier is a method parameter */
        for (Symbol symbol : this.symbolTable.getParameters(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                String typename = type.getName();

                if (this.isClassExtension(typename))
                    return new MyType(typename, this.OBJECT, false);

                if (this.isClassItself(typename))
                    return new MyType(typename, this.THIS, false);

                if (this.isImport(typename))
                    return new MyType(typename, this.OBJECT, false);

                return new MyType(typename, this.PRIMITIVE, type.isArray());
            }
        }

        for (Symbol symbol : this.symbolTable.getFields()) {
            if (this.identifier.equals(symbol.getName())) {
                if (this.isStatic)
                    return null;

                Type type = symbol.getType();

                String typename = type.getName();

                if (this.isClassExtension(typename))
                    return new MyType(typename, this.OBJECT, false);

                if (this.isClassItself(typename))
                    return new MyType(typename, this.THIS, false);

                if (this.isImport(typename))
                    return new MyType(typename, this.OBJECT, false);

                return new MyType(typename, this.PRIMITIVE, type.isArray());
            }
        }

        return null;
    }
}
