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

    private String parseImport(String imp) {
        String[] splitImport = imp.split("\\.");

        return splitImport[splitImport.length - 1];
    }

    private boolean isImport(String id) {
        for (String imp : this.symbolTable.getImports()) {
            if (id.equals(this.parseImport(imp))) {
                return true;
            }
        }

        return false;
    }
    private boolean isClassExtension(String id) {
        return id.equals(this.extension);
    }


    private MyType dealWithCondition(JmmNode node, String s) {
        MyType conditionType = visit(node.getJmmChild(0), "");

        if (conditionType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the condition is NULL! (method: dealWithCondition)");

            return null;
        }

        String typename = conditionType.getName();

        if (conditionType.isMethod())
            if (this.isImport(typename) || this.isClassExtension(typename))
                return conditionType;

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the identifier is NULL! (method: dealWithIdentifier)");

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the left operand or right operand is NULL! (method: dealWithBinaryOp)");

            return null;
        }

        if (leftOperandType.isObject() || rightOperandType.isObject()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The origin of the left operand or right operand is OBJECT! (method: dealWithBinaryOp)");

            return null;
        }

        if (operation.isArithmetic()) {
            if (!(leftOperandType.isInt() && rightOperandType.isInt())) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the left operand or right operand is not INT, in ARITHMETIC OPERATION! (method: dealWithBinaryOp)");
            }

            return new MyType("int", "primitive", false);
        }

        if (operation.isLogical()) {
            if (!(leftOperandType.isBoolean() && rightOperandType.isBoolean())) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the left operand or right operand is not BOOLEAN, in LOGICAL OPERATION! (method: dealWithBinaryOp)");

                return null;
            }

            return new MyType("boolean", "primitive", false);
        }

        if (operation.isComparison()) {
            if (!(leftOperandType.isInt() && rightOperandType.isInt())) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the left operand or right operand is not INT, in COMPARISON OPERATION! (method: dealWithBinaryOp)");

                return null;
            }

            return new MyType("boolean", "primitive", false);
        }

        return null;
    }

    private MyType dealWithParenthesis(JmmNode node, String s) {
        return visit(node.getJmmChild(0), "");
    }

    private MyType dealWithCustomInstantiation(JmmNode node, String s) {
        String typename = node.getJmmChild(0).get("name");

        if (typename.equals(this.symbolTable.getClassName())) {
            return new MyType(typename, "object", typename.contains("[]"));
        }

        if (!(this.isImport(typename) || this.isClassExtension(typename))) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "Trying to instantiate an object with a type that wasn't imported! (method: dealWithCustomInstantiation)");

            return null;
        }

        return new MyType(typename, "object", typename.contains("[]"));
    }

    private MyType dealWithArrayInstantiation(JmmNode node, String s) {
        JmmNode arrayTypeNode = node.getJmmChild(0);
        JmmNode arrayLengthNode = node.getJmmChild(1);

        MyType arrayLengthType = visit(arrayLengthNode, "");

        if (arrayLengthType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the array length is NULL! (method: dealWithArrayInstantiation)");

            return null;
        }

        /* the expression used for the array length is not of type 'int', must report an error */
        if (!arrayLengthType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the array length is not INT! (method: dealWithArrayInstantiation)");;

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the invoker is NULL! (method: dealWithMethodInvocation)");

            return null;
        }

        /* dealing with a method of our own class */
        if (invokerType.isThis() || invokerType.getName().equals(this.symbolTable.getClassName())) {

            /* first, check if the method exists */
            if (!this.symbolTable.getMethods().contains(name)) {
                if (this.extension != null) {
                    return new MyType(this.extension, "method", false);
                }
                else {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "The invoked method doesn't exist in the declared class! (method: dealWithMethodInvocation)");

                    return null;
                }
            }

            /* then, check its arguments */
            int numArgs = this.symbolTable.getParameters(name).size();
            int numInvokedArgs = node.getChildren().size() - 1;

            /* the number of arguments and invoked arguments are different, must add a report */
            if (numArgs != numInvokedArgs) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "The number of invoked arguments and the method arguments is different! (method: dealWithMethodInvocation)");

                return null;
            }

            int idx = 1;

            /* check if the parameters are of the same type */
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

                if (invokedArgTypeName.equals("this") || invokedArgTypeName.equals(this.symbolTable.getClassName())) {
                    if (argTypeName.equals(this.extension) || argTypeName.equals(this.symbolTable.getClassName())) {
                        return new MyType(returnType.getName(), isPrimitive ? "primitive" : "object", returnType.isArray());
                    }
                }

                /* arguments of different types, must add a report */
                if (!(argTypeName.equals(invokedArgTypeName) && argTypeIsArray == invokedArgTypeIsArray)) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "The invoked arguments have different types than the actual arguments! (method: dealWithMethodInvocation)");

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
                this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the handler is NULL! (method: dealWithMethodInvocation)");

                return null;
            }

            return new MyType(handler.getType().getName(), "method", handler.getType().isArray());
        }
    }

    private MyType dealWithArrayLength(JmmNode node, String s) {
        JmmNode accessedExpr = node.getJmmChild(0);

        MyType accessedType = visit(accessedExpr, "");

        if (accessedType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the accessed expression is NULL! (method: dealWithArrayLength)");

            return null;
        }

        if (!accessedType.isArray()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the accessed expression is not ARRAY! (method: dealWithArrayLength)");

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
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the accessed or index expression is NULL! (dealWithArrayAccess)");

            return null;
        }

        if (!(accessedType.isArray() && indexType.isInt())) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the accessed expression isn't ARRAY or the the type of index expression is not INT! (dealWithArrayAccess)");

            return null;
        }

        return new MyType("int", "primitive",false);
    }

    private MyType dealWithNegation(JmmNode node, String s) {
        MyType returnType = visit(node.getJmmChild(0), "");

        if (returnType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the return expression is NULL! (method: dealWithNegation)");

            return null;
        }

        if (!returnType.isBoolean()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The type of the return expression is not BOOLEAN! (method: dealWithNegation)");

            return null;
        }

        return returnType;
    }
}
