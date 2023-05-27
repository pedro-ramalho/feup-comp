package pt.up.fe.comp2023.optimization.ollir;


import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class MethodLiveness {
    private final OllirResult ollirResult;

    private ArrayList<Instruction> instructions;

    public MethodLiveness(OllirResult ollirResult, Method method) {
        this.ollirResult = ollirResult;
        this.instructions = method.getInstructions();
    }
}
