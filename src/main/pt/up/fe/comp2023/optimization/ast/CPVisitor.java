package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.optimization.ast.utils.Replacer;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Vector;

public class CPVisitor extends AJmmVisitor<String, String> {
    private boolean transformed;
    private HashMap<String, JmmNode> constants;

    private Replacer replacer;

    public CPVisitor() {
        this.transformed = false;
        this.constants = new HashMap<>();
        this.replacer = new Replacer();
    }

    public boolean transformed() {
        return this.transformed;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("While", this::dealWithWhile);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("True", this::dealWithTrue);
        addVisit("False", this::dealWithFalse);
        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
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

            this.transformed = this.transformed || visitor.folded();
        }

        return null;
    }

    private String dealWithParenthesis(JmmNode jmmNode, String s) {
        return visit(jmmNode.getJmmChild(0), "");
    }

    private String dealWithWhile(JmmNode node, String s) {
        /*
        WhileVisitor visitor = new WhileVisitor(this.constants);

        System.out.println("[DEBUG] - Before visiting while");
        visitor.visit(node, "");
        System.out.println("[DEBUG] - After visiting while");
         */

        for (JmmNode child : node.getChildren())
            visit(child, " ");

        return null;
    }

    private boolean isLiteral(String kind) {
        return kind.equals("Integer") || kind.equals("True") || kind.equals("False");
    }

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            System.out.println("child kind: " + child.getKind());
            visit(child, "");
        }

        return "";
    }

    private String dealWithAssignment(JmmNode node, String s) {
        /* lhs of the assignment, identifier */
        String identifier = node.get("var");
        System.out.println("Identifier: " + identifier);
        /* rhs of the assignment, expression */
        JmmNode rhs = node.getJmmChild(0);

        if (this.isLiteral(rhs.getKind()))
            this.constants.put(identifier, rhs);
        else {
            visit(rhs, "");
        }

        return null;
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        System.out.println("identifier: " + identifier);

        if (this.constants.containsKey(identifier)) {
            JmmNode updated = this.constants.get(identifier);
            System.out.println("updated kind: " + updated.getKind());
            this.replacer.exec(updated, node);

            this.transformed = true;
        }

        return null;
    }
}
