package pt.up.fe.comp2023.optimization.ast;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.optimization.ast.utils.CPVisitorUtils;
import pt.up.fe.comp2023.optimization.ast.utils.Result;
import pt.up.fe.comp2023.optimization.ast.utils.Variable;

import java.sql.SQLOutput;
import java.util.HashMap;

public class CPVisitor extends AJmmVisitor<String, String> {
    private int transformations;
    private boolean transformed;

    private HashMap<String, String> values;

    private HashMap<String, JmmNode> constants;

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
        addVisit("Conditional", this::dealWithConditional);
        addVisit("While", this::dealWithWhile);

        /* add a default visitor so that we skip useless nodes */
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithWhile(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithConditional(JmmNode jmmNode, String s) {
        return null;
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

    private boolean isLiteral(String kind) {
        return kind.equals("Integer") || kind.equals("True") || kind.equals("False");
    }

    private String dealWithAssignment(JmmNode node, String s) {
        /* lhs of the assignment, identifier */
        String identifier = node.get("var");

        /* rhs of the assignment, expression */
        JmmNode rhs = node.getJmmChild(0);

        /* if it's a literal, add it to the constants map */
        if (this.isLiteral(rhs.getKind()))
            this.constants.put(identifier, rhs);
        else
            visit(rhs, "");

        return null;
    }

    private String dealWithInteger(JmmNode node, String s) {
        return node.get("value");
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        String identifier = node.get("value");

        if (this.constants.containsKey(identifier)) {
            JmmNode updated = this.constants.get(identifier);

            node.replace(updated);

            /* since we replaced a node in the AST, a transformation has been made */
            this.transformed = true;
        }

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

        /* computate the result of the operation */
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
