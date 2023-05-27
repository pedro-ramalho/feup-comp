package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.OpType;
import pt.up.fe.comp2023.optimization.ast.utils.Replacer;
import pt.up.fe.comp2023.optimization.ast.utils.Result;

public class CFVisitor extends AJmmVisitor<String, String> {
    private boolean folded;
    private Replacer replacer;

    public CFVisitor() {
        this.folded = false;
        this.replacer = new Replacer();
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Integer", this::dealWithInteger);
        addVisit("True", this::dealWithTrue);
        addVisit("False", this::dealWithFalse);

        setDefaultVisit(this::defaultVisit);
    }

    public boolean folded() {
        return this.folded;
    }

    private boolean isLiteral(String kind) {
        return kind.equals("Integer") || kind.equals("True") || kind.equals("False");
    }

    private OpType getOpType(String op) {
        return switch (op) {
            case "*", "/", "+", "-" -> OpType.BINARY_OP_ARITHMETIC;
            case "&&", "||" -> OpType.BINARY_OP_LOGICAL;
            case "<", ">" -> OpType.BINARY_OP_COMPARISON;
            default -> null;
        };
    }

    private String defaultVisit(JmmNode node, String s) {
        for (JmmNode child : node.getChildren())
            visit(child, "");

        return null;
    }

    private String dealWithFalse(JmmNode jmmNode, String s) {
        return "False";
    }

    private String dealWithTrue(JmmNode jmmNode, String s) {
        return "True";
    }

    private String dealWithInteger(JmmNode jmmNode, String s) {
        return jmmNode.get("value");
    }

    private String dealWithBinaryOp(JmmNode node, String s) {
        String op = node.get("op");
        JmmNode lexpr = node.getJmmChild(0);
        JmmNode rexpr = node.getJmmChild(1);

        String lkind = lexpr.getKind();
        String rkind = rexpr.getKind();

        /* both expressions must be literals */
        if (!(this.isLiteral(lkind) && this.isLiteral(rkind)))
            return null;

        String lval = visit(lexpr, "");
        String rval = visit(rexpr, "");
        OpType type = this.getOpType(op);

        Result result = new Result(lval, rval, type, op);
        String resval = result.get();

        if (type == OpType.BINARY_OP_ARITHMETIC) {
            JmmNode updated = new JmmNodeImpl("Integer");
            updated.put("value", resval);

            this.replacer.exec(updated, node);

            this.folded = true;
        }

        if (type == OpType.BINARY_OP_LOGICAL) {
            JmmNode updated = new JmmNodeImpl(resval);

            this.replacer.exec(updated, node);

            this.folded = true;
        }

        if (type == OpType.BINARY_OP_COMPARISON) {
            JmmNode updated = new JmmNodeImpl(resval);

            this.replacer.exec(updated, node);

            this.folded = true;
        }

        return null;
    }
}
