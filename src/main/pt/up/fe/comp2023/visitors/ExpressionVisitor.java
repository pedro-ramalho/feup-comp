package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.visitors.handlers.IdentifierHandler;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ExpressionVisitor extends AJmmVisitor<String, Type> {
    private String method;

    private String extension;
    private boolean isStatic;

    private MySymbolTable symbolTable;

    private ArrayList<Report> reports;

    private final Type intType = new Type("int", false);
    private final Type boolType = new Type("boolean", false);
    private final Type importType = new Type("import", false);
    private final Type thisType = new Type("this", false);

    public ExpressionVisitor(String method, String extension, boolean isStatic, MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.method = method;
        this.extension = extension;
        this.isStatic = isStatic;
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

    private Type dealWithCondition(JmmNode node, String s) {
        Type conditionType = visit(node.getJmmChild(0), "");

        if (!conditionType.equals(this.boolType)) {
            this.addReport();

            return null;
        }

        return conditionType;
    }
    private Type dealWithObject(JmmNode node, String s) {
        /* the 'this' keyword cannot be used on a static method, must add a report */
        if (this.isStatic) {
            this.addReport();

            return null;
        }

        return new Type("this", false);
    }

    private Type dealWithIdentifier(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node, this.method, this.extension, this.symbolTable);

        Type identifierType = handler.getType();

        if (identifierType == null) {
            this.addReport();

            return null;
        }

        return identifierType;
    }

    private Type dealWithBoolean(JmmNode node, String s) {
        return this.boolType;
    }

    private Type dealWithInteger(JmmNode node, String s) {
        return this.intType;
    }

    private Type dealWithBinaryOp(JmmNode node, String s) {
        JmmNode leftOperand = node.getJmmChild(0);
        JmmNode rightOperand = node.getJmmChild(1);

        String operation = node.get("op");

        Type leftOperandType = visit(leftOperand, "");
        Type rightOperandType = visit(rightOperand, "");

        if (isArithmetic(operation)) {
            if (!(leftOperandType.equals(this.intType) && rightOperandType.equals(this.intType))) {
                this.addReport();
            }

            return this.intType;
        }

        if (isLogical(operation)) {
            if (!(leftOperandType.equals(this.boolType) && rightOperandType.equals(this.boolType))) {
                this.addReport();
            }

            return this.boolType;
        }

        if (isComparison(operation)) {
            if (!(leftOperandType.equals(this.intType) && rightOperandType.equals(this.intType))) {
                this.addReport();
            }

            return this.boolType;
        }

        return null;
    }

    private Type dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private Type dealWithCustomInstantiation(JmmNode node, String s) {
        String customType = node.getJmmChild(0).get("name");
        boolean isArray = customType.contains("[]");

        String parsedType = isArray ? customType.substring(0, customType.length() - 2) : customType;

        return new Type(parsedType, isArray);
    }

    private Type dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayTypeNode = node.getJmmChild(0);
        JmmNode arrayLengthNode = node.getJmmChild(1);

        Type arrayLengthType = visit(arrayLengthNode, "");

        if (!arrayLengthType.equals(this.intType)) {
            this.addReport();

            return null;
        }

        if (arrayTypeNode.hasAttribute("keyword")) {
            return new Type(arrayTypeNode.get("keyword"), arrayTypeNode.get("keyword").contains("[]"));
        }
        else {
            return new Type(arrayTypeNode.get("name"), arrayTypeNode.get("name").contains("[]"));
        }
    }

    private Type dealWithMethodInvocation(JmmNode node, String s) {
        String name = node.get("method");

        JmmNode invoker = node.getJmmChild(0);

        Type invokerType = visit(invoker, "");

        /* dealing with a method of our own class */
        if (invokerType.getName().equals(this.symbolTable.getClassName()) || invokerType.equals(this.thisType)) {

            /* first, check if the method exists */
            if (!this.symbolTable.getMethods().contains(name)) {
                this.addReport();

                return null;
            }

            /* then, check its arguments */
            int numArgs = this.symbolTable.getParameters(name).size();
            int numInvokedArgs = node.getChildren().size() - 1;

            /* the number of arguments and invoked arguments are different, must add a report */
            if (numArgs != numInvokedArgs) {
                this.addReport();

                return null;
            }

            int idx = 1;

            for (Symbol arg : this.symbolTable.getParameters(name)) {
                Type argType = arg.getType();
                Type invokedArgType = visit(node.getJmmChild(idx), "");

                /* arguments of different types, must add a report */
                if (!argType.equals(invokedArgType)) {
                    this.addReport();

                    return null;
                }

                idx++;
            }

            return this.symbolTable.getReturnType(name);
        }

        /* dealing with a method of an imported/extended class */
        else {
            return this.importType;
        }
    }

    private Type dealWithArrayLength(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);

        Type accessedType = visit(accessedExpr, "");

        if (!accessedType.isArray()) {
            this.addReport();

            return null;
        }

        return this.intType;
    }

    private Type dealWithArrayAccess(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);
        JmmNode indexExpr = node.getJmmChild(1);

        Type accessedType = visit(accessedExpr, "");
        Type indexType = visit(indexExpr, "");

        if (!(accessedType.isArray() && indexType.equals(this.intType))) {
            this.addReport();

            return null;
        }

        return this.intType;
    }

    private Type dealWithNegation(JmmNode node, String s) {
        Type returnType = visit(node.getJmmChild(0), "");

        if (!returnType.equals(this.boolType)) {
            this.addReport();

            return null;
        }

        return returnType;
    }
}
