package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;

public class StatementVisitor extends AJmmVisitor<String, String> {
    private String method;
    private String extension;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;
    public StatementVisitor(String method, String extension, MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.method = method;
        this.extension = extension;
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
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

        Type returnType = visitor.visit(node.getJmmChild(0), "");

        if (!returnType.equals(this.symbolTable.getReturnType(this.method))) {
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

        Type assigneeType = null;

        JmmNode accessExpr = node.getJmmChild(0);
        JmmNode expression = node.getJmmChild(1);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

        Type accessType = visitor.visit(accessExpr);
        Type exprType = visitor.visit(expression);

        /* check if the assignment is being done over a class field */
        for (Symbol symbol : this.symbolTable.getFields()) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = symbol.getType();
                }
            }
        }

        /* check if the assignment is being done over a parameter */
        for (Symbol symbol : this.symbolTable.getParameters(this.method)) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = symbol.getType();
                }
            }
        }

        /* check if the assignment is being done over a local variable */
        for (Symbol symbol : this.symbolTable.getLocalVariables(this.method)) {
            if (var.equals(symbol.getName())) {
                Type type = symbol.getType();

                if (type.isArray()) {
                    assigneeType = symbol.getType();
                }
            }
        }

        if (!accessType.equals(new Type("int", false))) {
            this.addReport();

            return null;
        }

        if (assigneeType == null) {
            this.addReport();

            return null;
        }

        if (!assigneeType.equals(new Type("int", true))) {
            this.addReport();

            return null;
        }

        if (!exprType.equals(new Type("int", false))) {
            this.addReport();

            return null;
        }

        return null;
    }

    private String dealWithAssignment(JmmNode node, String s) {
        String var = node.get("var");
        Type assigneeType = null;

        JmmNode expression = node.getJmmChild(0);

        /* check if the assignment is being done over a class field */
        for (Symbol symbol : this.symbolTable.getFields()) {
            if (var.equals(symbol.getName())) {
                assigneeType = symbol.getType();
            }
        }

        /* check if the assignment is being done over a parameter */
        for (Symbol symbol : this.symbolTable.getParameters(this.method)) {
            if (var.equals(symbol.getName())) {
                assigneeType = symbol.getType();
            }
        }

        /* check if the assignment is being done over a local variable */
        for (Symbol symbol : this.symbolTable.getLocalVariables(this.method)) {
            if (var.equals(symbol.getName())) {
                assigneeType = symbol.getType();
            }
        }

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

        Type assignedType = visitor.visit(expression, "");

        System.out.println("assigneeType: " + assigneeType.toString());
        System.out.println("assignedType: " + assignedType.toString());

        if (assigneeType == null) {
            this.addReport();

            return null;
        }

        if (assigneeType.equals(new Type("import", false)) || assigneeType.equals(new Type("extension", false))) {
            return null;
        }

        if (assignedType.equals(new Type("import", false)) || assignedType.equals(new Type("extension", false))) {
            return null;
        }

        if (!assigneeType.equals(assignedType)) {
            this.addReport();

            return null;
        }

        return null;
    }

    private String dealWithExprStmt(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

        for (JmmNode child : node.getChildren()) {
            visitor.visit(child, "");
        }

        return null;
    }

    private String dealWithWhile(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

        if (!visitor.visit(node.getJmmChild(0), "").equals("boolean")) {
            this.addReport();
        }

        return null;
    }

    private String dealWithConditional(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            /* condition is of type 'expression' */
            if (child.getKind().equals("Condition")) {
                ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.symbolTable, this.reports);

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
