package pt.up.fe.comp2023.optimization.ast.handlers;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.optimization.ast.OpType;
import pt.up.fe.comp2023.optimization.ast.utils.Result;

import java.util.HashMap;

public class WhileVisitor extends AJmmVisitor<String, String> {
    private JmmNode condition;
    private JmmNode codeblock;

    private String operation;
    private String lcondval;
    private String rcondval;

    private boolean stop;

    private HashMap<String, JmmNode> constants;

    public WhileVisitor(JmmNode condition, JmmNode codeblock, HashMap<String, JmmNode> constants) {
        this.condition = condition;
        this.codeblock = codeblock;
        this.constants = constants;

        this.stop = false;
    }

    private OpType getOpType(String op) {
        return switch (op) {
            case "*", "/", "+", "-" -> OpType.BINARY_OP_ARITHMETIC;
            case "&&", "||" -> OpType.BINARY_OP_LOGICAL;
            case "<", ">" -> OpType.BINARY_OP_COMPARISON;
            default -> null;
        };
    }

    public void build() {
        JmmNode condexpr = this.condition.getJmmChild(0);

        JmmNode lhs = condexpr.getJmmChild(0);
        JmmNode rhs = condexpr.getJmmChild(1);

        this.operation = condexpr.get("op");

        this.lcondval = lhs.get("value");
        this.rcondval = rhs.get("value");

        if (lhs.getKind().equals("Identifier"))
            if (this.constants.containsKey(this.lcondval)) {
                JmmNode mapnode = this.constants.get(this.lcondval);

                this.lcondval = mapnode.get("value");
            }

        if (rhs.getKind().equals("Identifier"))
            if (this.constants.containsKey(this.rcondval)) {
                JmmNode mapnode = this.constants.get(this.rcondval);

                this.rcondval = mapnode.get("value");
            }

        Result result = new Result(this.lcondval, this.rcondval, this.getOpType(this.operation), this.operation);
        this.stop = result.get().equals("True");

        System.out.println("lcondval: " + lcondval);
        System.out.println("rcondval: " + rcondval);
        System.out.println("operation: " + operation);
        System.out.println("this.stop = " + this.stop);

        this.executeBody();
    }

    public void executeBody() {
        if (!this.stop) {
            for (JmmNode child : this.codeblock.getChildren())
                visit(child, "");

            this.build();
        }

    }

    @Override
    protected void buildVisitor() {
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Integer", this::dealWithInteger);
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
    }

    private String dealWithIdentifier(JmmNode jmmNode, String s) {
        String identifier = jmmNode.get("value");

        if (this.constants.containsKey(identifier))
            return this.constants.get(identifier).get("value");

        return null;
    }

    private String dealWithBinaryOp(JmmNode node, String s) {
        JmmNode lhs = node.getJmmChild(0);
        JmmNode rhs = node.getJmmChild(1);

        String lval = visit(lhs, "");
        String rval = visit(rhs, "");

        String op = node.get("op");
        OpType optype = this.getOpType(op);

        Result result = new Result(lval, rval, optype, op);

        return result.get();
    }

    private String dealWithAssignment(JmmNode node, String s) {
        JmmNode expr = node.getJmmChild(0);

        String rhs = visit(expr, "");
        String var = node.get("var");

        if (!expr.getKind().equals("BinaryOp"))
            if (this.constants.containsKey(var))
                this.constants.put(var, expr);

        return null;
    }


}
