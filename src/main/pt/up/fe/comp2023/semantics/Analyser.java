package pt.up.fe.comp2023.semantics;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;

public abstract class Analyser {
    protected JmmParserResult parserResult;
    protected MySymbolTable symbolTable;

    protected ArrayList<Report> reports;

    public Analyser(JmmParserResult parserResult, MySymbolTable symbolTable) {
        this.parserResult = parserResult;
        this.symbolTable = symbolTable;
        this.reports = new ArrayList<>();
    }

    public abstract void eval(JmmNode node);
}
