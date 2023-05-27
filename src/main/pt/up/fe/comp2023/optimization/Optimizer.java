package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ExprCodeResult;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.OllirGenerator;
import pt.up.fe.comp2023.optimization.ast.CPVisitor;

import java.util.HashMap;
import java.util.Map;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        Map<String, String> config = semanticsResult.getConfig();

        String toOptimize = config.getOrDefault("optimize", "false");

        /* skip the optimization process */
        //if (toOptimize.equals("false")) return semanticsResult;

        /* apply propagation */
        CPVisitor visitor = new CPVisitor();
        visitor.visit(semanticsResult.getRootNode());

        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        System.out.println("Generating OLLIR code... ");

        OllirGenerator ollirGenerator = new OllirGenerator((MySymbolTable) jmmSemanticsResult.getSymbolTable());
        ExprCodeResult ollirText = ollirGenerator.visit(jmmSemanticsResult.getRootNode(),"");
        System.out.println(ollirText.value());
        return new OllirResult(ollirText.value(), jmmSemanticsResult.getConfig());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        return JmmOptimization.super.optimize(ollirResult);
    }
}
