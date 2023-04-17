package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;

public class ClassVisitor extends AJmmVisitor<String, String> {
    private JmmNode node;
    private String name;
    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;

    public ClassVisitor(MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("ClassField", this::dealWithClassField);
        addVisit("Method", this::dealWithMethod);
    }

    private String dealWithMethod(JmmNode node, String s) {
        MethodVisitor visitor = new MethodVisitor(node, this.symbolTable, this.reports);

        return visitor.visit(node, "");
    }

    private String dealWithClassField(JmmNode node, String s) {
        return null;
    }

    private String dealWithClassDeclaration(JmmNode node, String s) {
        for (JmmNode child : node.getChildren()) {
            visit(child, "");
        }

        return null;
    }
}
