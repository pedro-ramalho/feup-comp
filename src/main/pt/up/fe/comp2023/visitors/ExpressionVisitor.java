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

        System.out.println("condType: " + conditionType.getName() + ", " + conditionType.getOrigin());

        if (conditionType == null) {
            System.out.println("conditionType is null!");
            this.addReport();

            return null;
        }

        if (conditionType.isExtension() || conditionType.isImport()) {
            if (conditionType.isMethod()) {

                return conditionType;
            }
        }

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

        // TODO: see how well this behaves
        return new MyType("this", "object", false);
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
        return new MyType("boolean", "primitive", false);
    }

    private MyType dealWithInteger(JmmNode node, String s) {
        return new MyType("int", "primitive",false);
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
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("int", "primitive",false);
                }

                if (rightOperandType.isInt()) {
                    return new MyType("int", "primitive",false);
                }
            }

            if (rightOperandType.isMethod()) {
                if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("int", "primitive",false);
                }

                if (leftOperandType.isInt()) {
                    return new MyType("int", "primitive",false);
                }
            }

            if (leftOperandType.isInt() && rightOperandType.isInt()) {
                return new MyType("int", "primitive",false);
            }

            this.addReport();

            return null;
        }

        if (operation.isLogical()) {
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("boolean", "primitive",false);
                }

                if (rightOperandType.isBoolean()) {
                    return new MyType("boolean", "primitive",false);
                }
            }

            if (rightOperandType.isMethod()) {
                if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("boolean", "primitive",false);
                }

                if (leftOperandType.isBoolean()) {
                    return new MyType("boolean", "primitive",false);
                }
            }

            if (leftOperandType.isBoolean() && rightOperandType.isBoolean()) {
                return new MyType("boolean", "primitive",false);
            }

            this.addReport();

            return null;
        }

        if (operation.isComparison()) {
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("boolean", "primitive",false);
                }

                if (rightOperandType.isInt()) {
                    return new MyType("boolean", "primitive",false);
                }
            }

            if (rightOperandType.isMethod()) {
                if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                    this.addReport();

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport();

                        return null;
                    }

                    return new MyType("boolean", "primitive",false);
                }

                if (leftOperandType.isInt()) {
                    return new MyType("boolean", "primitive",false);
                }
            }

            if (leftOperandType.isInt() && rightOperandType.isInt()) {
                return new MyType("boolean", "primitive",false);
            }

            this.addReport();

            return null;

        }

        return null;
    }

    private MyType dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private MyType dealWithCustomInstantiation(JmmNode node, String s) {
        String customType = node.getJmmChild(0).get("name");

        IdentifierHandler handler = new IdentifierHandler(customType, this.method, this.extension, this.isStatic, this.symbolTable);

        System.out.println("type: " + handler.getType().getName());

        return handler.getType();
    }

    private MyType dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayTypeNode = node.getJmmChild(0);
        JmmNode arrayLengthNode = node.getJmmChild(1);

        MyType arrayLengthType = visit(arrayLengthNode, "");

        if (arrayLengthType == null) {
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

            return new MyType(keyword, "primitive", true);
        }
        else {
            return visit(arrayTypeNode, "");
        }
    }

    private MyType dealWithMethodInvocation(JmmNode node, String s) {
        String name = node.get("method");

        JmmNode invoker = node.getJmmChild(0);

        MyType invokerType = visit(invoker, "");

        if (invokerType == null) {
            this.addReport();

            return null;
        }

        System.out.println("invokerType: " + invokerType.getName() + ", " + invokerType.isArray());

        /* dealing with a method of our own class */
        if (invokerType.isThis()) {

            /* first, check if the method exists */
            if (!this.symbolTable.getMethods().contains(name)) {
                if (this.extension != null) {
                    return null;
                }
                else {
                    this.addReport();

                    return null;
                }
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
                MyType invokedArgType = visit(node.getJmmChild(idx), "");

                String argTypeName = argType.getName();
                boolean argTypeIsArray = argType.isArray();

                String invokedArgTypeName = invokedArgType.getName();
                boolean invokedArgTypeIsArray = invokedArgType.isArray();

                /* arguments of different types, must add a report */
                if (!(argTypeName.equals(invokedArgTypeName) && argTypeIsArray == invokedArgTypeIsArray)) {
                    this.addReport();
                    return null;
                }

                idx++;
            }

            Type returnType = this.symbolTable.getReturnType(name);
            String typename = returnType.getName();

            boolean isPrimitive = typename.equals("int") || typename.equals("boolean") || typename.equals("String");

            return new MyType(returnType.getName(), isPrimitive ? "primitive" : "object", returnType.isArray());
        }

        /* dealing with a method of an imported/extended class */
        else {
            IdentifierHandler handler = new IdentifierHandler(invoker.get("value"), this.method, this.extension, this.isStatic, this.symbolTable);

            if (handler.getType() == null) {
                this.addReport();

                return null;
            }

            System.out.println("handlerType: " + handler.getType().getName() + ", " + handler.getType().getOrigin());

            return new MyType(handler.getType().getName(), "method", handler.getType().isArray());
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

        return new MyType("int", "primitive",false);
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

        return new MyType("int", "primitive",false);
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
