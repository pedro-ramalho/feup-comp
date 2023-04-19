package pt.up.fe.comp2023.visitors;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;
import java.util.Arrays;

public class ImportVisitor extends AJmmVisitor<String, String> {
    private MySymbolTable symbolTable;

    private ArrayList<Report> reports;

    public ImportVisitor(MySymbolTable symbolTable, ArrayList<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ImportDeclaration", this::dealWithImportDeclaration);
    }

    private String dealWithImportDeclaration(JmmNode node, String s) {
        String importName = node.get("name");

        String importPack = "";
        // fetch the package that was imported
        if (node.hasAttribute("pack"))
            importPack = node.get("pack");



        // add the information to the symbol table
        this.symbolTable.addImport(importName + '.' + importPack);

        return null;
    }
}
