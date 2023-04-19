package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class VariableHandler implements Handler {
    private JmmNode node;

    private MySymbolTable symbolTable;

    public VariableHandler(JmmNode node, MySymbolTable symbolTable) {
        this.node = node;
        this.symbolTable = symbolTable;
    }

    @Override
    public String getType() {
        JmmNode declaration = node.getJmmChild(0);

        // simply return the keyword
        if (declaration.getKind().equals("Literal")) {
            return declaration.get("keyword");
        }

        // custom type, must have been imported
        if (declaration.getKind().equals("CustomType")) {
            System.out.println("I'm dealing with a CustomType node.");

            String name = declaration.get("name");
            List<String> imports = symbolTable.getImports();

            for (String imp : imports) {
                if (imp.contains(name)) {
                    System.out.println("The list of imports contains that name!");
                    return name;
                }
            }

        }

        return null;
    }
}
