package pt.up.fe.comp2023.visitors.handlers;

import pt.up.fe.comp.jmm.analysis.table.Type;
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

    private String parseType(String typeStr) {
        if (typeStr.contains("[]")) {
            return typeStr.substring(0, typeStr.length() - 2);
        }

        return typeStr;
    }

    @Override
    public Type getType() {
        JmmNode declaration = node.getJmmChild(0);

        /* built-in type, simply return the keyword */
        if (declaration.getKind().equals("Literal")) {
            String typeName = parseType(declaration.get("keyword"));

            return new Type(typeName, typeName.contains("[]"));
        }

        /* custom type, must be an imported class, an extension, or the class itself */
        if (declaration.getKind().equals("CustomType")) {
            String name = declaration.get("name");
            String parsedName = name.contains("[]") ? name.substring(0, name.length() - 2) : name;

            if (name.equals(this.symbolTable.getClassName())) {
                return new Type(parsedName, name.contains("[]"));
            }

            /* check for imports */
            for (String imp : symbolTable.getImports()) {
                if (name.equals(this.parseImport(imp))) {
                    return new Type(name, name.contains("[]"));
                }
            }

            /* check for class extension */
        }

        return null;
    }
}
