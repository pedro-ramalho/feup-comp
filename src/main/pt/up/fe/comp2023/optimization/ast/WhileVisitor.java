package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.utils.Result;

import java.util.HashMap;

public class WhileVisitor extends AJmmVisitor<String, String> {
    private HashMap<String, JmmNode> constants;

    private boolean condval;
    /**
     * enquanto (condition == true)
     * visitar (code block)
     */
    private boolean transformed = false;

    public WhileVisitor(HashMap<String, JmmNode> constants) {
        this.constants = constants;
        this.condval = true;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("While", this::dealWithWhile);
        addVisit("Condition", this::dealWithCondition);
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Integer", this::dealWithInteger);
        addVisit("True", this::dealWithTrue);
        addVisit("False", this::dealWithFalse);

        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, "");

        return null;
    }

    private String dealWithFalse(JmmNode node, String s) {
        return "False";
    }

    private String dealWithTrue(JmmNode node, String s) {
        return "True";
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        if (this.constants.containsKey(identifier)) {
            JmmNode updated = this.constants.get(identifier);

            return updated.getKind().equals("Integer") ? updated.get("value") : updated.getKind();
        }

        return null;
    }

    private OpType getOpType(String op) {
        return switch (op) {
            case "*", "/", "+", "-" -> OpType.BINARY_OP_ARITHMETIC;
            case "&&", "||" -> OpType.BINARY_OP_LOGICAL;
            case "<", ">" -> OpType.BINARY_OP_COMPARISON;
            default -> null;
        };
    }

    private String dealWithBinaryOp(JmmNode node, String s) {
        String op = node.get("op");
        JmmNode lexpr = node.getJmmChild(0);
        JmmNode rexpr = node.getJmmChild(1);

        String lval = visit(lexpr, "");
        String rval = visit(rexpr, "");
        OpType type = this.getOpType(op);

        Result result = new Result(lval, rval, type, op);

        if (node.getJmmParent().getKind().equals("Condition")) {
            System.out.println("lval: " + lval);
            System.out.println("rval: " + rval);
            System.out.println("op: " + op);
            System.out.println("res: " + result.get());
        }

        return result.get();
    }

    private String dealWithCodeBlock(JmmNode node, String s) {
        for (JmmNode child : node.getChildren())
            visit(child, "");

        return null;
    }

    private String dealWithCondition(JmmNode node, String s) {
        JmmNode expr = node.getJmmChild(0);

        String expresult = visit(expr, "");

        System.out.println("expresult: " + expresult);

        return visit(expr, "");
    }

    private String dealWithWhile(JmmNode node, String s) {
        while (this.condval) {
            System.out.println("Im in the while loop");
            JmmNode condition = node.getJmmChild(0);
            JmmNode statement = node.getJmmChild(1);

            visit(statement, "");

            this.condval = visit(condition, "").equals("True");
            System.out.println("this.condval = " + this.condval);
        }

        return null;
    }

    private String dealWithAssignment(JmmNode node, String s) {
        String identifier = node.get("var");
        String rval = visit(node.getJmmChild(0), "");

        if (this.constants.containsKey(identifier)) {
            JmmNode current = this.constants.get(identifier);

            String updatedKind = current.getKind();
            JmmNode updated = new JmmNodeImpl(current.getKind());

            if (updatedKind.equals("Integer"))
                updated.put("value", rval);

            this.constants.put(identifier, updated);
        }

        return null;
    }


}
