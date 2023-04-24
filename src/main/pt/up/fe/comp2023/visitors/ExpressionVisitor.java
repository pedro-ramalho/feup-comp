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

    private void addReport(String line, String col, String message) {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), message
        ));
    }

    private MyType dealWithCondition(JmmNode node, String s) {
        MyType conditionType = visit(node.getJmmChild(0), "");

        if (conditionType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "conditionType is null!");

            return null;
        }

        if (conditionType.isExtension() || conditionType.isImport()) {
            if (conditionType.isMethod()) {

                return conditionType;
            }
        }

        /* the condition is not of type 'boolean', must report an error */
        if (!conditionType.isBoolean()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "conditionType is not boolean!");
            return null;
        }

        return conditionType;
    }

    private MyType dealWithObject(JmmNode node, String s) {
        /* the 'this' keyword cannot be used on a static method, report an error */
        if (this.isStatic) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "'this' was called in a static method!");

            return null;
        }

        // TODO: see how well this behaves
        return new MyType("this", "object", false);
    }

    private MyType dealWithIdentifier(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node.get("value"), this.method, this.extension, this.isStatic, this.symbolTable);

        MyType identifierType = handler.getType();

        if (identifierType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "identifierType is null!");

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperandType or rightOperandType is null!");

            return null;
        }

        if (operation.isArithmetic()) {
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (arithmetic operation)");

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (arithmetic operation)");

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
                    this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (arithmetic operation)");

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (arithmetic operation)");

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

            this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand or rightOperand are not of type int! (arithmetic operation)");

            return null;
        }

        if (operation.isLogical()) {
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (logical operation)");

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (logical operation)");

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
                    this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (logical operation)");

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (logical operation)");

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

            this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand or rightOperand are not of type boolean! (logical operation)");

            return null;
        }

        if (operation.isComparison()) {
            if (leftOperandType.isMethod()) {
                if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (comparison operation)");

                    return null;
                }

                if (rightOperandType.isMethod()) {
                    if (!(rightOperandType.isExtension() || rightOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (comparison operation)");

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
                    this.addReport(node.get("lineStart"), node.get("colStart"), "rightOperand comes from an import or extension, but isn't a method! (comparison operation)");

                    return null;
                }

                if (leftOperandType.isMethod()) {
                    if (!(leftOperandType.isExtension() || leftOperandType.isImport())) {
                        this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand comes from an import or extension, but isn't a method! (comparison operation)");

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

            this.addReport(node.get("lineStart"), node.get("colStart"), "leftOperand or rightOperand are not of type int! (comparison operation)");

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


        return handler.getType();
    }

    private MyType dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayTypeNode = node.getJmmChild(0);
        JmmNode arrayLengthNode = node.getJmmChild(1);

        MyType arrayLengthType = visit(arrayLengthNode, "");

        if (arrayLengthType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "arrayLengthType is null!");

            return null;
        }

        /* the expression used for the array length is not of type 'int', must report an error */
        if (!arrayLengthType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "arrayLengthType is not int!");;

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "invokerType is null!");

            return null;
        }


        /* dealing with a method of our own class */
        if (invokerType.isThis()) {

            /* first, check if the method exists */
            if (!this.symbolTable.getMethods().contains(name)) {
                if (this.extension != null) {
                    return new MyType("extension", "method", false);
                }
                else {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "the called method doesn't exist in the declared class!");

                    return null;
                }
            }

            /* then, check its arguments */
            int numArgs = this.symbolTable.getParameters(name).size();
            int numInvokedArgs = node.getChildren().size() - 1;

            /* the number of arguments and invoked arguments are different, must add a report */
            if (numArgs != numInvokedArgs) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "different number of arguments!");

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
                Type returnType = this.symbolTable.getReturnType(name);
                String typename = returnType.getName();

                boolean isPrimitive = typename.equals("int") || typename.equals("boolean") || typename.equals("String");

                if (invokedArgTypeName.equals("this")) {
                    if (argTypeName.equals(this.extension) || argTypeName.equals(this.symbolTable.getClassName())) {
                        return new MyType(returnType.getName(), isPrimitive ? "primitive" : "object", returnType.isArray());
                    }
                }

                /* arguments of different types, must add a report */
                if (!(argTypeName.equals(invokedArgTypeName) && argTypeIsArray == invokedArgTypeIsArray)) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "arguments of different types!");

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
                this.addReport(node.get("lineStart"), node.get("colStart"), "handlerType is null! (method invocation)");

                return null;
            }

            return new MyType(handler.getType().getName(), "method", handler.getType().isArray());
        }
    }

    private MyType dealWithArrayLength(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);

        MyType accessedType = visit(accessedExpr, "");

        if (accessedType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessedType is null! (array length)");

            return null;
        }

        if (!accessedType.isArray()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessedType isn't array! (array length)");

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessedType or indexType is null! (array access)");

            return null;
        }

        if (!(accessedType.isArray() && indexType.isInt())) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "accessedType is not array or indexType is not int! (array access)");

            return null;
        }

        return new MyType("int", "primitive",false);
    }

    private MyType dealWithNegation(JmmNode node, String s) {
        MyType returnType = visit(node.getJmmChild(0), "");

        if (returnType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is null! (negation)");

            return null;
        }

        if (!returnType.isBoolean()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "returnType is not boolean! (negation)");

            return null;
        }

        return returnType;
    }
}
