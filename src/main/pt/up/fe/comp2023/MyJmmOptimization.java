package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class MyJmmOptimization implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        System.out.println("Generating OLLIR code... ");

        OllirGenerator ollirGenerator = new OllirGenerator((MySymbolTable) jmmSemanticsResult.getSymbolTable());
        ExprCodeResult ollirText = ollirGenerator.visit(jmmSemanticsResult.getRootNode(),"");
        System.out.println(ollirText.value());
        return new OllirResult(ollirText.value(), jmmSemanticsResult.getConfig());
    }
}
