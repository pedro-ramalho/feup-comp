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
import pt.up.fe.comp2023.visitors.utils.MyType;

import java.util.ArrayList;

public class StatementVisitor extends AJmmVisitor<String, String> {
    private String method;
    private String extension;
    private boolean isStatic;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;
    public StatementVisitor(String method, String extension, boolean isStatic, MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.method = method;
        this.extension = extension;
        this.isStatic = isStatic;
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    private void addReport() {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, -1, -1, ""
        ));
    }

    @Override
    protected void buildVisitor() {
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("Conditional", this::dealWithConditional);
        addVisit("MethodStatement", this::dealWithMethodStatement);
        addVisit("ReturnStatement", this::dealWithReturnStatement);
        addVisit("IfStatement", this::dealWithConditionalStatement);
        addVisit("ElseStatement", this::dealWithConditionalStatement);
        addVisit("While", this::dealWithWhile);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    private String dealWithReturnStatement(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType returnType = visitor.visit(node.getJmmChild(0), "");
        Type stReturnType = this.symbolTable.getReturnType(this.method);

        if (returnType == null) {
            this.addReport();

            return null;
        }

        if (returnType.getName().equals("import") || returnType.getName().equals("extension")) {

            return null;
        }

        if (!returnType.equals(new MyType(stReturnType.getName(), stReturnType.isArray()))) {
            this.addReport();

        }

        return null;
    }

    private String dealWithMethodStatement(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithConditionalStatement(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }

    private String dealWithArrayAssignment(JmmNode node, String s) {
        String var = node.get("var");

        MyType assigneeType = null;

        JmmNode accessExpr = node.getJmmChild(0);
        JmmNode expression = node.getJmmChild(1);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType accessType = visitor.visit(accessExpr);
        MyType exprType = visitor.visit(expression);

        /* check if the assignment is being done over a class field */
        for (Symbol symbol : this.symbolTable.getFields()) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = new MyType(symbol.getType().getName(), symbol.getType().isArray());
                }
            }
        }

        /* check if the assignment is being done over a parameter */
        for (Symbol symbol : this.symbolTable.getParameters(this.method)) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = new MyType(symbol.getType().getName(), symbol.getType().isArray());
                }
            }
        }

        /* check if the assignment is being done over a local variable */
        for (Symbol symbol : this.symbolTable.getLocalVariables(this.method)) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = new MyType(symbol.getType().getName(), symbol.getType().isArray());
                }
            }
        }

        if (!accessType.isInt()) {
            this.addReport();

            return null;
        }

        if (assigneeType == null) {
            this.addReport();

            return null;
        }

        if (!assigneeType.isIntArray()) {
            this.addReport();

            return null;
        }

        if (!exprType.isInt()) {
            this.addReport();

            return null;
        }

        return null;
    }

    private String dealWithAssignment(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node.get("var"), this.method, this.extension, this.symbolTable);

        Type handlerType = handler.getType();
        String assigneeTypeName = handlerType.getName().equals(this.symbolTable.getClassName()) ? "this" : handlerType.getName();

        MyType assigneeType = new MyType(assigneeTypeName, handler.getType().isArray());

        JmmNode expression = node.getJmmChild(0);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType assignedType = visitor.visit(expression, "");

        if (assignedType == null) {
            this.addReport();

            return null;
        }

        /* the assignee is either an extension or an import, assume it's correct */
        if (assigneeType.getName().equals("extension") || assigneeType.getName().equals("import")) {

            return null;
        }

        /* the assignee is an object of the declared class */
        if (assigneeType.getName().equals("this")) {
            if (!assignedType.getName().equals("this") && !assignedType.getName().equals("extension")) {
                this.addReport();
            }

            return null;
        }

        /* the assigned is either an extension or an import, assume it's correct */
        if (assignedType.getName().equals("extension") || assignedType.getName().equals("import")) {
            return null;
        }

        /* the remaining case implies that both the assignee and the assigned are of the same type */
        if (!assigneeType.equals(assignedType)) {

            this.addReport();

            return null;
        }

        return null;
    }

    private String dealWithExprStmt(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        for (JmmNode child : node.getChildren()) {
            visitor.visit(child, "");
        }

        return null;
    }

    private String dealWithWhile(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        Type cond = visitor.visit(node.getJmmChild(0));

        if (cond == null) {
            this.addReport();

            return null;
        }

        if (!cond.equals(new Type("boolean", false))) {
            this.addReport();
        }

        return null;
    }

    private String dealWithConditional(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            /* condition is of type 'expression' */
            if (child.getKind().equals("Condition")) {
                ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

                visitor.visit(child);
            }
            else {
               visit(child, "");
            }
        }

        return null;
    }

    private String dealWithCodeBlock(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }
}
