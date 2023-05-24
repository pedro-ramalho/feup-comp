package pt.up.fe.comp2023.optimization.ast.utils;

import pt.up.fe.comp2023.optimization.ast.OpType;

public class Result {
    private String lval;
    private String rval;

    private final OpType type;
    private final String op;

    private String result;


    public Result(String lval, String rval, OpType type, String op) {
        this.lval = lval;
        this.rval = rval;
        this.type = type;
        this.op = op;
    }

    public String get() {
        return switch (this.type) {
            case BINARY_OP_ARITHMETIC -> this.getArithmeticResult();
            case BINARY_OP_LOGICAL    -> this.getLogicalResult();
            case BINARY_OP_COMPARISON -> this.getComparisonResult();
            case SIMPLE_INTEGER       -> "";
            case NOT_EXPR             -> "";
            default -> throw new RuntimeException();
        };
    }

    public String getArithmeticResult() {
        int lop = Integer.parseInt(this.lval);
        int rop = Integer.parseInt(this.rval);

        return switch(this.op) {
            case "*" -> String.valueOf(lop * rop);
            case "/" -> String.valueOf(lop / rop);
            case "+" -> String.valueOf(lop + rop);
            case "-" -> String.valueOf(lop - rop);
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    public String getLogicalResult() {
        boolean lop = this.lval.equals("True");
        boolean rop = this.rval.equals("True");

        return switch(this.op) {
            case "&&" -> (lop && rop) ? "True" : "False";
            case "||" -> (lop || rop) ? "True" : "False";
            default   -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    public String getComparisonResult() {
        int lop = Integer.parseInt(this.lval);
        int rop = Integer.parseInt(this.rval);

        return switch(this.op) {
            case "<" -> (lop < rop) ? "True" : "False";
            case ">" -> (lop > rop) ? "True" : "False";
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }
}
