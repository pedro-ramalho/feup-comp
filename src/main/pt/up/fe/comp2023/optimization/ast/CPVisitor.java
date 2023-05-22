package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.sql.SQLOutput;
import java.util.HashMap;

public class CPVisitor extends AJmmVisitor<String, String> {
    private int transformations;
    private boolean transformed;

    public CPVisitor() {
        this.transformations = 0;
        this.transformed = false;
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("Integer", this::dealWithInteger);

        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
    }

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) visit(child, "");

        return "";
    }

    private String dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private OpType getOpType(String op) {
        return switch (op) {
            case "*", "/", "+", "-" -> OpType.BINARY_OP_ARITHMETIC;
            case "&&", "||" -> OpType.BINARY_OP_LOGICAL;
            case "<", ">" -> OpType.BINARY_OP_COMPARISON;
            default -> null;
        };
    }

    private int getArithmeticResult(int lval, int rval, String op) {
        return switch(op) {
            case "*" -> lval * rval;
            case "/" -> lval / rval;
            case "+" -> lval + rval;
            case "-" -> lval - rval;
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    private boolean getLogicalResult(boolean lval, boolean rval, String op) {
        return switch(op) {
            case "&&" -> lval && rval;
            case "||" -> lval || rval;
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    private boolean getComparisonResult(int lval, int rval, String op) {
        return switch(op) {
            case "<" -> lval < rval;
            case ">" -> lval > rval;
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    private void replace(JmmNode newNode, JmmNode oldNode) {
        JmmNode parent = oldNode.getJmmParent();

        int position = parent.getChildren().indexOf(oldNode);

        parent.removeJmmChild(position);
        parent.add(newNode, position);

        newNode.setParent(parent);
    }

    private String dealWithBinaryOp(JmmNode node, String s) {
        JmmNode lexpr = node.getJmmChild(0);
        JmmNode rexpr = node.getJmmChild(1);

        String op = node.get("op");
        OpType optype = getOpType(op);

        if (optype == OpType.BINARY_OP_ARITHMETIC) {
            int lval = Integer.parseInt(visit(lexpr, ""));
            int rval = Integer.parseInt(visit(rexpr, ""));

            String result = String.valueOf(getArithmeticResult(lval, rval, op));

            JmmNode newNode = new JmmNodeImpl("Integer");
            newNode.put("value", result);

            this.replace(newNode, node);

            return result;
        }

        if (optype == OpType.BINARY_OP_LOGICAL) {
            boolean lval = (visit(lexpr, "")).equals("True");
            boolean rval = (visit(lexpr, "")).equals("True");

            String result = getLogicalResult(lval, rval, op) ? "True" : "False";

            JmmNode newNode = new JmmNodeImpl(result);

            this.replace(newNode, node);

            return result;
        }

        if (optype == OpType.BINARY_OP_COMPARISON) {
            int lval = Integer.parseInt(visit(lexpr, ""));
            int rval = Integer.parseInt(visit(rexpr, ""));

            String result = getComparisonResult(lval, rval, op) ? "True" : "False";
            JmmNode newNode = new JmmNodeImpl(result);

            this.replace(newNode, node);

            return result;
        }

        return null;
    }
}
