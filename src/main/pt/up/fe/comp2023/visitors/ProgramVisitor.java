package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;

public class ProgramVisitor extends AJmmVisitor<String, String> {

    private MySymbolTable symbolTable;
    private ArrayList<Report> reports;

    public ProgramVisitor(MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
    }

    private String dealWithProgram(JmmNode node, String s) {
        JmmNode importNode = node.getJmmChild(0);
        JmmNode classNode = node.getJmmChild(1);

        ImportVisitor importVisitor = new ImportVisitor(this.symbolTable, this.reports);
        ClassVisitor classVisitor = new ClassVisitor(this.symbolTable, this.reports);

        importVisitor.visit(importNode, "");
        classVisitor.visit(classNode, "");

        return null;
    }
}
