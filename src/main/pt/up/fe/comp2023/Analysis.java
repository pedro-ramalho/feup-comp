package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.ArrayList;

public class Analysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        Visitor gen = new Visitor();

        /* build the AST */
        gen.visit(jmmParserResult.getRootNode(), "");

        /* fetch the Symbol Table */
        MySymbolTable symbolTable = gen.getSymbolTable();

        /* fetch the list of Reports */
        // ArrayList<Report> reports = gen.

        return new JmmSemanticsResult(jmmParserResult, gen.getSymbolTable(), new ArrayList<>());
    }

}
