package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.optimization.ast.utils.Replacer;

import java.sql.SQLOutput;
import java.util.*;

public class CPVisitor extends AJmmVisitor<String, String> {
    private boolean transformed;
    private HashMap<String, JmmNode> constants;
    private HashMap<String, Boolean> propagate;
    private Replacer replacer;

    public CPVisitor() {
        this.transformed = false;
        this.constants = new HashMap<>();
        this.propagate = new HashMap<>();
        this.replacer = new Replacer();
    }

    public boolean transformed() {
        return this.transformed;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("While", this::dealWithWhile);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Conditional", this::dealWithConditional);
        addVisit("True", this::dealWithTrue);
        addVisit("False", this::dealWithFalse);

        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithCodeBlock(JmmNode jmmNode, String s) {
        if (jmmNode.getJmmParent().getKind().equals("While"))
            return null;

        return null;
    }

    private String dealWithConditional(JmmNode jmmNode, String s) {
        this.constants.clear();

        for (JmmNode child : jmmNode.getChildren())
            visit(child, "");

        return null;
    }

    private String dealWithFalse(JmmNode jmmNode, String s) {
        return "False";
    }

    private String dealWithTrue(JmmNode jmmNode, String s) {
        return "True";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        JmmNode lexpr = jmmNode.getJmmChild(0);
        JmmNode rexpr = jmmNode.getJmmChild(1);

        visit(lexpr, "");
        visit(rexpr, "");

        if (this.isLiteral(lexpr.getKind()) && this.isLiteral(rexpr.getKind())) {
            CFVisitor visitor = new CFVisitor();
            visitor.visit(jmmNode, "");

            this.transformed = true;
        }

        return null;
    }

    private String dealWithParenthesis(JmmNode jmmNode, String s) {
        return visit(jmmNode.getJmmChild(0), "");
    }

    private void updateScope(List<String> identifiers) {
        System.out.println("Identifiers: ");

        for (String identifier : identifiers) {
            System.out.println(">> " + identifier);
            this.propagate.put(identifier, false);
        }

        for (Map.Entry<String, Boolean> entry : this.propagate.entrySet()) {
            System.out.println("K: " + entry.getKey() + ", V: " + entry.getValue());
        }
    }

    private String dealWithWhile(JmmNode node, String s) {
        WhileVisitor visitor = new WhileVisitor();
        visitor.visit(node);

        List<String> identifiers = visitor.getIdentifiers();
        this.updateScope(identifiers);

        return null;
    }

    private boolean isLiteral(String kind) {
        return kind.equals("Integer") || kind.equals("True") || kind.equals("False");
    }

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return "";
    }

    private String dealWithAssignment(JmmNode node, String s) {
        /* lhs of the assignment, identifier */
        String identifier = node.get("var");

        /* rhs of the assignment, expression */
        JmmNode rhs = node.getJmmChild(0);

        if (this.isLiteral(rhs.getKind())) {
            this.constants.put(identifier, rhs);
            this.propagate.put(identifier, true);
        }
        else {
            visit(rhs, "");
        }

        return null;
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        if (this.constants.containsKey(identifier)) {
            /* we can propagate the identifier */
            if (this.propagate.get(identifier)) {
                JmmNode updated = this.constants.get(identifier);

                this.replacer.exec(updated, node);

                this.transformed = true;
            }
        }

        return null;
    }
}
