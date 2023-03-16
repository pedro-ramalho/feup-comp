package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

import java.util.ArrayList;

public class Analysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        Generator gen = new Generator();

        gen.visit(jmmParserResult.getRootNode(), "");

        return new JmmSemanticsResult(jmmParserResult, gen.getSymbolTable(), new ArrayList<>());
    }
}
