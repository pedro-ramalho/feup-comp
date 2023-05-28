package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ExprCodeResult;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.OllirGenerator;
import pt.up.fe.comp2023.optimization.ast.CFVisitor;
import pt.up.fe.comp2023.optimization.ast.CPVisitor;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        Map<String, String> config = semanticsResult.getConfig();

        String toOptimize = config.getOrDefault("optimize", "false");

        /* skip the optimization process */
        //if (toOptimize.equals("false")) return semanticsResult;

        CPVisitor cpv;
        CFVisitor cfv;

        int iter = 1;

        do {
            /* propagate constants */
            cpv = new CPVisitor();
            cpv.visit(semanticsResult.getRootNode());

            /* fold constants */
            cfv = new CFVisitor();
            cfv.visit(semanticsResult.getRootNode());

            System.out.println("- Optimization no. " + iter);
            System.out.println(semanticsResult.getRootNode().toTree());

            iter++;

        } while (cpv.transformed());

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
