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

    private void addReport(String line, String col, String message) {
        this.reports.add(new Report(
                ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(line), Integer.parseInt(col), message
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

    private String dealWithReturnStatement(JmmNode node, String s) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType returnType = visitor.visit(node.getJmmChild(0), "");
        Type stReturnType = this.symbolTable.getReturnType(this.method);

        if (returnType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The return type is NULL! (method: dealWithReturnStatement)");

            return null;
        }

        if (returnType.isMethod()) {
            if (this.isImport(returnType.getName()) || this.isClassExtension(returnType.getName())) {
                return null;
            }
        }

        if (returnType.isThis() || returnType.getName().equals(this.symbolTable.getClassName())) {
            if (stReturnType.getName().equals(this.symbolTable.getClassName())) {
                return null;
            }

            if (stReturnType.getName().equals(this.extension)) {
                return null;
            }

            this.addReport(node.get("lineStart"), node.get("colStart"), "The return type is 'this', but it shouldn't! (method: dealWithReturnStatement)");
        }

        if (stReturnType.getName().equals(this.symbolTable.getClassName())) {
            if (!returnType.isThis() || !returnType.getName().equals(this.extension)) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "The return type is not THIS! (method: dealWithReturnStatement)");;
            }

            return null;
        }

        if (!(returnType.equals(new MyType(stReturnType.getName(), "", stReturnType.isArray())))) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The return type and the symbol table return type are different! (method: dealWithReturnStatement)");
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

        JmmNode accessExpr = node.getJmmChild(0);
        JmmNode expression = node.getJmmChild(1);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType accessType = visitor.visit(accessExpr);
        MyType exprType = visitor.visit(expression);

        if (accessType == null || exprType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The access expression or type expression is NULL! (method: dealWithArrayAssignment)");

            return null;
        }

        if (!accessType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The access expression is not of type INT! (method: dealWithArrayAssignment)");

            return null;
        }

        if (!exprType.isInt()) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The expression is not of type INT! (method: dealWithArrayAssignment)");

            return null;
        }

        return null;
    }

    private String dealWithAssignment(JmmNode node, String s) {
        IdentifierHandler handler = new IdentifierHandler(node.get("var"), this.method, this.extension, this.isStatic, this.symbolTable);

        MyType assigneeType = handler.getType();

        if (assigneeType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The assignee is NULL! (method: dealWithAssignment)");

            return null;
        }

        JmmNode expression = node.getJmmChild(0);

        ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

        MyType assignedType = visitor.visit(expression, "");

        if (assignedType == null) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The assigned is NULL! (assignment)");

            return null;
        }

        /* the assignee is of type 'extension', assume it's correct */
        if (this.isClassExtension(assigneeType.getName()) || this.isClassExtension(assignedType.getName())) {
            return null;
        }

        if (this.isImport(assigneeType.getName())) {
            if (this.isImport(assignedType.getName())) {
                return null;
            }

            if (assignedType.getName().equals("this") || assignedType.getName().equals(this.symbolTable.getClassName())) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "IMPORT was assigned to THIS! (method: dealWithAssignment)");;
            }

            return null;
        }

        if (assigneeType.getName().equals("this") || assigneeType.getName().equals(this.symbolTable.getClassName())) {
            if (!assignedType.getName().equals("this") && !assignedType.getName().equals(this.symbolTable.getClassName()) && !assignedType.getName().equals(this.extension)) {
                this.addReport(node.get("lineStart"), node.get("colStart"), "THIS was assigned to something other than THIS or EXTENSION! (method: dealWithAssignment)");
            }

            return null;
        }

        if (assigneeType.isPrimitive()) {
            if (this.isClassExtension(assignedType.getName()) && assignedType.isMethod()) {
                return null;
            }

            if (this.isImport(assignedType.getName()) && assignedType.isMethod()) {
                return null;
            }
        }

        if (!assigneeType.equals(assignedType)) {
            this.addReport(node.get("lineStart"), node.get("colStart"), "The types of the assignee and assigned are different! (method: dealWithAssignment)");

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
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Condition")) {
                ExpressionVisitor visitor = new ExpressionVisitor(this.method, this.extension, this.isStatic, this.symbolTable, this.reports);

                MyType cond = visitor.visit(node.getJmmChild(0));

                if (cond == null) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "Condition is NULL! (method: dealWithWhile)");

                    return null;
                }

                if (cond.isMethod()) {
                    if (this.isClassExtension(cond.getName()) || this.isImport(cond.getName())) {
                        return null;
                    }
                }

                if (!cond.isBoolean()) {
                    this.addReport(node.get("lineStart"), node.get("colStart"), "Condition is not of type BOOLEAN! (method: dealWithWhile)");
                }
            }
            else {
                visit(child, "");
            }
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
