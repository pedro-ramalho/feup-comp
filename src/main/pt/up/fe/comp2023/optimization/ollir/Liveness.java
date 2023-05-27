package pt.up.fe.comp2023.optimization.ollir;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class Liveness {
    private OllirResult ollirResult;

    public Liveness(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
    }

    public void in() {
        ClassUnit classUnit = this.ollirResult.getOllirClass();

        classUnit.buildCFGs();

        ArrayList<Method> methods = classUnit.getMethods();

        System.out.println("number of methods: " + methods.size());

        Method method = classUnit.getMethod(1);

        String name = method.getMethodName();
        Node node = method.getBeginNode();

        ArrayList<Instruction> instructions = method.getInstructions();

        for (Instruction instruction : instructions) {
            System.out.println("Instruction contents:");
            instruction.show();
            InstructionLiveness iliv = new InstructionLiveness(instruction);

            iliv.computeUseDef(instruction);

            iliv.showUse();
            iliv.showDef();
            System.out.println();
        }

        method.show();
    }
}
