package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.visitors.ProgramVisitor;

import java.util.ArrayList;

public class Analysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        Generator gen = new Generator();

        /* build the AST */
        gen.visit(jmmParserResult.getRootNode(), "");

        /* fetch the Symbol Table */
        MySymbolTable symbolTable = gen.getSymbolTable();

        ArrayList<Report> reports = new ArrayList<>();

        ProgramVisitor visitor = new ProgramVisitor(symbolTable, reports);
        visitor.visit(jmmParserResult.getRootNode(), "");

        return new JmmSemanticsResult(jmmParserResult, gen.getSymbolTable(), reports);
    }

}
