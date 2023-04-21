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
    public Type getType() {
        /* check if the identifier is a class extension */
        if (this.identifier.equals(this.extension)) {
            return new Type("extension", false);
        }

        /* check if the identifier is the class itself */
        if (this.identifier.equals(this.symbolTable.getClassName())) {
            return new Type("class", identifier.contains("[]"));
        }

        /* check if the identifier is from an import */
        for (String str : symbolTable.getImports()) {
            if (this.identifier.equals(this.parseImport(str))) {
                return new Type("import", false);
            }
        }

        /* check if the identifier is a method's local variable */
        for (Symbol symbol : symbolTable.getLocalVariables(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType();
            }
        }

        /* check if the identifier is a method parameter */
        for (Symbol symbol : symbolTable.getParameters(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType();
            }
        }

        /* check if the identifier is a class field */
        for (Symbol symbol : symbolTable.getFields()) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType();
            }
        }

        return null;
    }
}
