package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.utils.CPVisitorUtils;
import pt.up.fe.comp2023.optimization.ast.utils.Result;
import pt.up.fe.comp2023.optimization.ast.utils.Variable;

import java.util.HashMap;

public class CPVisitor extends AJmmVisitor<String, String> {
    private int transformations;
    private boolean transformed;

    private HashMap<String, String> values;
    private HashMap<Variable, Boolean> constants;
    private final Folder folder;

    public CPVisitor() {
        this.transformations = 0;
        this.transformed = false;

        this.values = new HashMap<>();
        this.constants = new HashMap<>();

        this.folder = new Folder();
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

    private String dealWithDefault(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) visit(child, "");

        return "";
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

        return null;
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
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
        /* lhs and rhs of the operation */
        JmmNode lexpr = node.getJmmChild(0);
        JmmNode rexpr = node.getJmmChild(1);

        /* fetch the operation type */
        String op = node.get("op");
        OpType optype = this.getOpType(op);

        /* fetch the operands */
        String lval = visit(lexpr, "");
        String rval = visit(rexpr, "");

        System.out.println("lval: " + lval);
        System.out.println("rval: " + rval);

        /* computate the result of the operation*/
        Result result = new Result(lval, rval, optype, op);
        String resval = result.get();

        if (optype == OpType.BINARY_OP_ARITHMETIC) {
            JmmNode updated = new JmmNodeImpl("Integer");
            updated.put("value", resval);

            this.folder.fold(updated, node);
        }

        if (optype == OpType.BINARY_OP_LOGICAL) {
            JmmNode updated = new JmmNodeImpl(resval);

            this.folder.fold(updated, node);
        }

        if (optype == OpType.BINARY_OP_COMPARISON) {
            JmmNode updated = new JmmNodeImpl(resval);

            this.folder.fold(updated, node);
        }

        return resval;
    }
}
