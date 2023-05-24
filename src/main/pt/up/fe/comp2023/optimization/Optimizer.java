package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.optimization.ast.CPVisitor;

public class Optimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        System.out.println("[LOG] Optimize method was called!");
        CPVisitor visitor = new CPVisitor();
        visitor.visit(semanticsResult.getRootNode());
        System.out.println("[LOG] Finished visiting!");
        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        return null;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        return JmmOptimization.super.optimize(ollirResult);
    }
}
