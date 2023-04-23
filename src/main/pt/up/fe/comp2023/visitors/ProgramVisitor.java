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
        for (JmmNode child : node.getChildren()) {
            /* visit import nodes */
            if (child.getKind().equals("ImportDeclaration")) {
                ImportVisitor importVisitor = new ImportVisitor(this.symbolTable, this.reports);

                importVisitor.visit(child, "");
            }

            /* visit class declaration nodes */
            if (child.getKind().equals("ClassDeclaration")) {
                ClassVisitor classVisitor = new ClassVisitor(child, this.symbolTable, this.reports);
            }
        }

        return null;
    }
}
