package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;

public class IdentifierHandler implements Handler {
    private JmmNode node;
    private String identifier;
    private String method;
    private MySymbolTable symbolTable;
    public IdentifierHandler(JmmNode node, String method, MySymbolTable symbolTable) {
        this.node = node;
        this.identifier = this.node.get("value");
        this.method = method;
        this.symbolTable = symbolTable;
    }
    @Override
    public String getType() {
        /* check if the identifier is a field */
        for (Symbol symbol : symbolTable.getFields()) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType().getName();
            }
        }

        /* check if the identifier is a parameter */
        for (Symbol symbol : symbolTable.getParameters(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType().getName();
            }
        }

        /* check if the identifier is a local variable */
        for (Symbol symbol : symbolTable.getLocalVariables(this.method)) {
            if (this.identifier.equals(symbol.getName())) {
                return symbol.getType().getName();
            }
        }

        return null;
    }
}
