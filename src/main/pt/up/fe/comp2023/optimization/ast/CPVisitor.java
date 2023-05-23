package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.utils.CPVisitorUtils;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

public class CPVisitor extends AJmmVisitor<String, String> {
    private int transformations;

    private boolean transformed;

    private HashMap<String, String> values;

    private CPVisitorUtils utils;

    public CPVisitor() {
        this.transformations = 0;
        this.transformed = false;
        this.values = new HashMap<>();

        this.utils = new CPVisitorUtils();
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("Integer", this::dealWithInteger);
        addVisit("True", this::dealWithTrue);
        addVisit("False", this::dealWithFalse);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);

        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithFalse(JmmNode jmmNode, String s) {
        return "False";
    }

    private String dealWithTrue(JmmNode jmmNode, String s) {
        return "True";
    }

    private String dealWithAssignment(JmmNode node, String s) {
        String var = node.get("var");

        String value = visit(node.getJmmChild(0), "");

        this.values.put(var, value);

        for (Map.Entry<String, String> entry : this.values.entrySet()) {
            System.out.println("K: " + entry.getKey() + ", V: " + entry.getValue());
        }

        return null;
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
    }

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) visit(child, "");

        return "";
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        if (this.values.containsKey(identifier))
            return this.values.get(identifier);

        return null;
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

            String result = String.valueOf(this.utils.getArithmeticResult(lval, rval, op));

            JmmNode newNode = new JmmNodeImpl("Integer");
            newNode.put("value", result);

            this.replace(newNode, node);

            return result;
        }

        if (optype == OpType.BINARY_OP_LOGICAL) {
            boolean lval = (visit(lexpr, "")).equals("True");
            boolean rval = (visit(rexpr, "")).equals("True");

            String result = this.utils.getLogicalResult(lval, rval, op) ? "True" : "False";

            JmmNode newNode = new JmmNodeImpl(result);

            this.replace(newNode, node);

            return result;
        }

        if (optype == OpType.BINARY_OP_COMPARISON) {
            int lval = Integer.parseInt(visit(lexpr, ""));
            int rval = Integer.parseInt(visit(rexpr, ""));

            String result = this.utils.getComparisonResult(lval, rval, op) ? "True" : "False";
            JmmNode newNode = new JmmNodeImpl(result);

            this.replace(newNode, node);

            return result;
        }

        return null;
    }
}
