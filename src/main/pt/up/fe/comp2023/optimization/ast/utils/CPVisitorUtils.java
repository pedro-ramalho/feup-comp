package pt.up.fe.comp2023.optimization.ast.utils;

public class CPVisitorUtils {
    public CPVisitorUtils() {

    }

    public int getArithmeticResult(int lval, int rval, String op) {
        return switch(op) {
            case "*" -> lval * rval;
            case "/" -> lval / rval;
            case "+" -> lval + rval;
            case "-" -> lval - rval;
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    public boolean getLogicalResult(boolean lval, boolean rval, String op) {
        return switch(op) {
            case "&&" -> lval && rval;
            case "||" -> lval || rval;
            default   -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }

    public boolean getComparisonResult(int lval, int rval, String op) {
        return switch(op) {
            case "<" -> lval < rval;
            case ">" -> lval > rval;
            default  -> throw new RuntimeException("Invalid operation when dealing with BinaryOp: " + op);
        };
    }
}
