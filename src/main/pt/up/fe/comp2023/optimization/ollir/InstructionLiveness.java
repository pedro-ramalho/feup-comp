package pt.up.fe.comp2023.optimization.ollir;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InstructionLiveness {
    private final Instruction instruction;

    private Set<String> def;
    private Set<String> use;
    private Set<String> in;
    private Set<String> out;

    public InstructionLiveness(Instruction instruction) {
        this.instruction = instruction;

        this.def = new HashSet<>();
        this.use = new HashSet<>();
    }

    public void showDef() {
        for (String s : this.def) System.out.println("[-] def element: " + s);

    }
    public void showUse() {
        for (String s : this.use) System.out.println("[+] use element: " + s);
    }

    private void binaryOpHandler(BinaryOpInstruction inst) {
        Element lexpr = inst.getLeftOperand();
        Element rexpr = inst.getRightOperand();

        if (lexpr instanceof Operand lop) this.use.add(lop.getName());
        if (rexpr instanceof Operand rop) this.use.add(rop.getName());
    }

    private void singleOpHandler() {

    }

    private void unaryOpHandler() {

    }

    private void assignHandler(AssignInstruction inst) {
        Element dest = inst.getDest();

        if (dest instanceof Operand dop) this.def.add(dop.getName());

        this.computeUseDef(inst.getRhs());
    }

    public void computeUseDef(Instruction instruction) {
        if (instruction instanceof BinaryOpInstruction inst) {
            this.binaryOpHandler(inst);
        }

        if (instruction instanceof SingleOpInstruction inst) {
            // do something about single op instructions
        }

        if (instruction instanceof UnaryOpInstruction inst) {
            // do something about unary op instructions
        }

        if (instruction instanceof AssignInstruction inst) {
            this.assignHandler(inst);
        }
    }
}
