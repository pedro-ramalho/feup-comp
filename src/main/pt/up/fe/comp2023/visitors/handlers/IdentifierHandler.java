package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;


public class IdentifierHandler implements Handler {
    private JmmNode node;
    private String identifier;
    private String method;
    private String extension;
    private MySymbolTable symbolTable;
    public IdentifierHandler(JmmNode node, String method, String extension, MySymbolTable symbolTable) {
        this.node = node;
        this.identifier = this.node.get("value");
        this.method = method;
        this.extension = extension;
        this.symbolTable = symbolTable;
    }

    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        String ret = splitImport[splitImport.length - 1];

        return splitImport[splitImport.length - 1];
    }

    @Override
    public String getType() {
        /* check if the identifier is a class extension */
        if (this.identifier.equals(this.extension)) {
            return "extension";
        }

        /* check if the identifier is from an import */
        for (String str : symbolTable.getImports()) {
            if (this.identifier.equals(this.parseImport(str))) {
                return "imported";
            }
        }

        /* check if the identifier is a class field */
        for (Symbol symbol : symbolTable.getFields()) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                return type.isArray() ? "array" : type.getName();
            }
        }

        /* check if the identifier is a method parameter */
        for (Symbol symbol : symbolTable.getParameters(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                return type.isArray() ? "array" : type.getName();
            }
        }

        /* check if the identifier is a method's local variable */
        for (Symbol symbol : symbolTable.getLocalVariables(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                Type type = symbol.getType();

                return type.isArray() ? "array" : type.getName();
            }
        }

        return null;
    }
}
