package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.optimization.ast.utils.Replacer;

import java.util.HashMap;

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

        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithWhile(JmmNode node, String s) {
        WhileVisitor visitor = new WhileVisitor(this.constants);

        System.out.println("[DEBUG] - Before visiting while");
        visitor.visit(node, "");
        System.out.println("[DEBUG] - After visiting while");

        return null;
    }


    private boolean isLiteral(String kind) {
        return kind.equals("Integer") || kind.equals("True") || kind.equals("False");
    }

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren())
            visit(child, "");

        return "";
    }

    private String dealWithAssignment(JmmNode node, String s) {
        /* lhs of the assignment, identifier */
        String identifier = node.get("var");

        /* rhs of the assignment, expression */
        JmmNode rhs = node.getJmmChild(0);

        if (this.isLiteral(rhs.getKind()))
            this.constants.put(identifier, rhs);
        else
            visit(rhs, "");

        return null;
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        if (this.constants.containsKey(identifier)) {
            JmmNode updated = this.constants.get(identifier);

            this.replacer.exec(updated, node);

            this.transformed = true;
        }

        return null;
    }
}
