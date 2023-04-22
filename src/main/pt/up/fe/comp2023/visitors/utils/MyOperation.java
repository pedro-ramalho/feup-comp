package pt.up.fe.comp2023.visitors.utils;

public class MyOperation {
    private String op;
    public MyOperation(String op) {
        this.op = op;
    }

    public boolean isArithmetic() {
        return this.op.equals("+") || this.op.equals("-") || this.op.equals("*") || this.op.equals("/");
    }

    public boolean isLogical() {
        return this.op.equals("&&") || this.op.equals("||");
    }

    public boolean isComparison() {
        return this.op.equals("<") || this.op.equals(">");
    }
}
