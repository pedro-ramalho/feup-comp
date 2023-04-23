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
import pt.up.fe.comp2023.visitors.utils.MyOperation;
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;

public class ExpressionVisitor extends AJmmVisitor<String, MyType> {
    private String method;

    private String extension;
    private boolean isStatic;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;

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

    private MyType dealWithCondition(JmmNode node, String s) {
        MyType conditionType = visit(node.getJmmChild(0), "");

        if (conditionType == null) {
            this.addReport();

            return null;
        }

        System.out.println("conditionType: " + conditionType.getName());

        /* the condition is not of type 'boolean', must report an error */
        if (!conditionType.isBoolean()) {
            this.addReport();

            return null;
        }

        return conditionType;
    }

    private MyType dealWithObject(JmmNode node, String s) {
        /* the 'this' keyword cannot be used on a static method, report an error */
        if (this.isStatic) {
            this.addReport();

            return null;
        }

        return new MyType("this", false);
    }

    private MyType dealWithIdentifier(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node.get("value"), this.method, this.extension, this.isStatic, this.symbolTable);

        MyType identifierType = handler.getType();

        if (identifierType == null) {
            this.addReport();

            return null;
        }

        return identifierType;
    }

    private MyType dealWithBoolean(JmmNode node, String s) {
        return new MyType("boolean", false);
    }

    private MyType dealWithInteger(JmmNode node, String s) {
        return new MyType("int", false);
    }

    private MyType dealWithBinaryOp(JmmNode node, String s) {
        JmmNode leftOperand = node.getJmmChild(0);
        JmmNode rightOperand = node.getJmmChild(1);

        MyOperation operation = new MyOperation(node.get("op"));

        MyType leftOperandType = visit(leftOperand, "");
        MyType rightOperandType = visit(rightOperand, "");

        if (leftOperandType == null || rightOperandType == null) {
            this.addReport();

            return null;
        }


        if (operation.isArithmetic()) {
            if (leftOperandType.isExtension() || rightOperandType.isExtension()) {
                return new MyType("int", false);
            }

            if (leftOperandType.isImport() || rightOperandType.isImport()) {
                return new MyType("int", false);
            }

            /* if any of the operands is not of type 'int', must report an error */
            if (!(leftOperandType.isInt() && rightOperandType.isInt())) {
                this.addReport();
            }

            return new MyType("int", false);
        }

        if (operation.isLogical()) {
            if (leftOperandType.isExtension() || rightOperandType.isExtension()) {
                return new MyType("boolean", false);
            }

            if (leftOperandType.isImport() || rightOperandType.isImport()) {
                return new MyType("boolean", false);
            }

            /* if any of the operands is not of type 'boolean', must report an error */
            if (!(leftOperandType.isBoolean() && rightOperandType.isBoolean())) {
                this.addReport();
            }

            return new MyType("boolean", false);
        }

        if (operation.isComparison()) {
            if (leftOperandType.isExtension() || rightOperandType.isExtension()) {
                return new MyType("boolean", false);
            }

            if (leftOperandType.isImport() || rightOperandType.isImport()) {
                return new MyType("boolean", false);
            }

            /* if any of the operands is not of type 'int', must report an error */
            if (!(leftOperandType.isInt() && rightOperandType.isInt())) {
                this.addReport();
            }

            return new MyType("boolean", false);
        }

        return null;
    }

    private MyType dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private MyType dealWithCustomInstantiation(JmmNode node, String s) {
        String customType = node.getJmmChild(0).get("name");

        IdentifierHandler handler = new IdentifierHandler(customType, this.method, this.extension, this.isStatic, this.symbolTable);

        return handler.getType();
    }

    private MyType dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayTypeNode = node.getJmmChild(0);
        JmmNode arrayLengthNode = node.getJmmChild(1);

        MyType arrayLengthType = visit(arrayLengthNode, "");

        if (arrayLengthType == null) {
            System.out.println("Report0");
            this.addReport();

            return null;
        }

        /* the expression used for the array length is not of type 'int', must report an error */
        if (!arrayLengthType.isInt()) {
            System.out.println("Report1");
            this.addReport();

            return null;
        }

        if (arrayTypeNode.hasAttribute("keyword")) {
            String keyword = arrayTypeNode.get("keyword");

            return new MyType(keyword, true);
        }
        else {
            return visit(arrayTypeNode, "");
        }
    }

    private MyType dealWithMethodInvocation(JmmNode node, String s) {
        String name = node.get("method");

        JmmNode invoker = node.getJmmChild(0);

        MyType invokerType = visit(invoker, "");

        System.out.println("invokerType: " + invokerType.getName() + ", " + invokerType.isArray());

        if (invokerType == null) {
            System.out.println("Report 0");
            this.addReport();

            return null;
        }

        /* dealing with a method of our own class */
        if (invokerType.isThis()) {

            /* first, check if the method exists */
            if (!this.symbolTable.getMethods().contains(name)) {
                if (this.extension != null) {
                    return null;
                }
                else {
                    System.out.println("Report 1");
                    this.addReport();

                    return null;
                }
            }

            /* then, check its arguments */
            int numArgs = this.symbolTable.getParameters(name).size();
            int numInvokedArgs = node.getChildren().size() - 1;

            /* the number of arguments and invoked arguments are different, must add a report */
            if (numArgs != numInvokedArgs) {
                System.out.println("Report 2");
                this.addReport();

                return null;
            }

            int idx = 1;

            for (Symbol arg : this.symbolTable.getParameters(name)) {
                Type argType = arg.getType();
                MyType invokedArgType = visit(node.getJmmChild(idx), "");

                String argTypeName = argType.getName();
                boolean argTypeIsArray = argType.isArray();

                String invokedArgTypeName = invokedArgType.getName();
                boolean invokedArgTypeIsArray = invokedArgType.isArray();

                /* arguments of different types, must add a report */
                if (!(argTypeName.equals(invokedArgTypeName) && argTypeIsArray == invokedArgTypeIsArray)) {
                    System.out.println("Report 3");
                    this.addReport();
                    return null;
                }

                idx++;
            }

            Type returnType = this.symbolTable.getReturnType(name);

            return new MyType(returnType.getName(), returnType.isArray());
        }

        /* dealing with a method of an imported/extended class */
        else {
            IdentifierHandler handler = new IdentifierHandler(invoker.get("value"), this.method, this.extension, this.isStatic, this.symbolTable);

            if (handler.getType() == null) {
                System.out.println("Report 4");
                this.addReport();

                return null;
            }

            return handler.getType();
        }
    }

    private MyType dealWithArrayLength(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);

        MyType accessedType = visit(accessedExpr, "");

        if (accessedType == null) {
            this.addReport();

            return null;
        }

        if (!accessedType.isArray()) {
            this.addReport();

            return null;
        }

        return new MyType("int", false);
    }

    private MyType dealWithArrayAccess(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);
        JmmNode indexExpr = node.getJmmChild(1);

        MyType accessedType = visit(accessedExpr, "");
        MyType indexType = visit(indexExpr, "");

        if (accessedType == null || indexType == null) {
            this.addReport();

            return null;
        }

        if (!(accessedType.isArray() && indexType.isInt())) {
            this.addReport();

            return null;
        }

        return new MyType("int", false);
    }

    private MyType dealWithNegation(JmmNode node, String s) {
        MyType returnType = visit(node.getJmmChild(0), "");

        if (returnType == null) {
            this.addReport();

            return null;
        }

        if (!returnType.isBoolean()) {
            this.addReport();

            return null;
        }

        return returnType;
    }
}
