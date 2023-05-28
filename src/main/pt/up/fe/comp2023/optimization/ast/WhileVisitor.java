package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.utils.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WhileVisitor extends AJmmVisitor<String, String> {
    private List<String> identifiers;

    public WhileVisitor() {
        this.identifiers = new ArrayList<>();
    }

    public List<String> getIdentifiers() {
        return this.identifiers;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);

        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {
        String identifier = jmmNode.get("var");

        this.identifiers.add(identifier);

        return null;
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, "");

        return null;
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        /* an identifier was caught inside the while statement */
        this.identifiers.add(identifier);

        return null;
    }
}
