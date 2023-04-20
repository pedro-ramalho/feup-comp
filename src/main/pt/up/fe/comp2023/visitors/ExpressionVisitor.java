package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.handlers.IdentifierHandler;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ExpressionVisitor extends AJmmVisitor<String, String> {
    private String method;

    private String extension;

    private MySymbolTable symbolTable;

    private ArrayList<Report> reports;

    public ExpressionVisitor(String method, String extension, MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.method = method;
        this.extension = extension;
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Negation", this::dealWithNegation);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("ArrayLength", this::dealWithArrayLength);
        addVisit("MethodInvocation", this::dealWithMethodInvocation);
        addVisit("ArrayInstantiation", this::dealWithArrayInstantiation);
        addVisit("CustomInstantiation", this::dealWithCustomInstantiation);
        addVisit("Parenthesis", this::dealWithParenthesis);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Integer", this::dealWithInteger);
        addVisit("True", this::dealWithBoolean);
        addVisit("False", this::dealWithBoolean);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("This", this::dealWithObject);
        addVisit("Condition", this::dealWithCondition);
    }

    private void addReport() {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, -1, -1, ""
        ));
    }

    private boolean isArithmetic(String operation) {
        return operation.equals("+") || operation.equals("-") || operation.equals("*") || operation.equals("/");
    }

    private boolean isLogical(String operation) {
        return operation.equals("&&") || operation.equals("||");
    }

    private boolean isComparison(String operation) {
        return operation.equals("<") || operation.equals(">");
    }

    private String dealWithCondition(JmmNode node, String s) {
        String conditionType = visit(node.getJmmChild(0), "");

        if (!conditionType.equals("boolean")) {
            this.addReport();

            return null;
        }

        return "boolean";
    }
    private String dealWithObject(JmmNode node, String s) {
        return "object";
    }

    private String dealWithIdentifier(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node, this.method, this.extension, this.symbolTable);

        String identifierType = handler.getType();

        if (identifierType == null) {
            this.addReport();

            return null;
        }

        return identifierType;
    }

    private String dealWithBoolean(JmmNode node, String s) {
        return "boolean";
    }

    private String dealWithInteger(JmmNode node, String s) {
        return "int";
    }

    private String dealWithBinaryOp(JmmNode node, String s) {
        JmmNode leftOperand = node.getJmmChild(0);
        JmmNode rightOperand = node.getJmmChild(1);

        String operation = node.get("op");
        String leftOperandType = visit(leftOperand, "");
        String rightOperandType = visit(rightOperand, "");

        if (isArithmetic(operation)) {
            if (!(leftOperandType.equals("int") && rightOperandType.equals("int"))) {
                this.addReport();
            }

            return "int";
        }

        if (isLogical(operation)) {
            if (!(leftOperandType.equals("boolean") && rightOperandType.equals("boolean"))) {
                this.addReport();
            }

            return "boolean";
        }

        if (isComparison(operation)) {
            if (!(leftOperandType.equals("int") && rightOperandType.equals("int"))) {
                this.addReport();
            }

            return "boolean";
        }

        return null;
    }

    private String dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private String dealWithCustomInstantiation(JmmNode node, String s) {
        return node.getJmmChild(0).get("name");
    }

    private String dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayLength = node.getJmmChild(0);

        String arrayLengthType = visit(arrayLength, "");

        if (!arrayLengthType.equals("int")) {
            this.addReport();

            return null;
        }

        return "array";
    }

    // TODO: change return type
    private String dealWithMethodInvocation(JmmNode node, String s) {
        // the node which will invoke the method
        JmmNode invoker = node.getJmmChild(0);

        String invokerType = visit(invoker, "");

        System.out.println("invokerType: " + invokerType);

        return invokerType;

        /*
        it is only possible to call a method if the invoker is of type:
        * 1. this
        * 2. imported
        * 3. extension

        the return of this function will be:
        * 1. the return type of the method, if the invoker is of type "this"
        * 2. "imported", if the invoker is of type "imported"
        * 3. "extension", if the invoker is of type "extension"
        * */
    }

    private String dealWithArrayLength(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);

        String accessedType = visit(accessedExpr, "");

        if (!accessedType.equals("array")) {
            this.addReport();

            return null;
        }

        return "int";
    }

    private String dealWithArrayAccess(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);
        JmmNode indexExpr = node.getJmmChild(1);

        String accessedType = visit(accessedExpr, "");
        String indexType = visit(indexExpr, "");

        if (!(accessedType.equals("array") && indexType.equals("int"))) {
            this.addReport();

            return null;
        }

        return "int";
    }

    private String dealWithNegation(JmmNode node, String s) {
        String returnType = visit(node.getJmmChild(0), "");

        if (!returnType.equals("boolean")) {
            this.addReport();

            return null;
        }

        return returnType;
    }
}
