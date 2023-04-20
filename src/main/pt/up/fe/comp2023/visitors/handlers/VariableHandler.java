package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;
import java.util.List;

public class VariableHandler implements Handler {
    private JmmNode node;
    private String extension;

    private MySymbolTable symbolTable;

    public VariableHandler(JmmNode node, MySymbolTable symbolTable) {
        this.node = node;
        this.symbolTable = symbolTable;
    }

    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        return splitImport[splitImport.length - 1];
    }

    @Override
    public String getType() {
        JmmNode declaration = node.getJmmChild(0);

        /* built-in type, simply return the keyword */
        if (declaration.getKind().equals("Literal")) {
            return declaration.get("keyword");
        }

        /* custom type, must be an imported class or an extension */
        if (declaration.getKind().equals("CustomType")) {
            String name = declaration.get("name");

            /* check for imports */
            for (String imp : symbolTable.getImports()) {
                if (name.equals(this.parseImport(imp))) {
                    return name;
                }
            }

            /* check for class extension */

        }

        return null;
    }
}
