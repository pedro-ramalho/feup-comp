package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.List;


public class MyJasminBackend implements JasminBackend {
    private String className;
    private AccessModifiers accessLevel;
    private String superClass;
    private int currStackSize = 0;

    private int maxStackSize = 0;

    private int labelCounter = 0;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(generateClassStructure(ollirClass));
        jasminCode.append(generateMethodsStructure(ollirClass));

        return new JasminResult(jasminCode.toString());
    }

    private String generateStacklimits() {
        return "\t .limit stack " + (this.maxStackSize + 4) + "\n";
    }

    private String generateLocalLimits(Method method) {
        if (method.isConstructMethod()) return "";

        int locals = (int) method.getVarTable()
                .values()
                .stream()
                .map(Descriptor::getVirtualReg)
                .distinct()
                .count();

        if (!method.isStaticMethod()) {
            locals++;
        }
        return "\t .limit locals " + locals + "\n";
    }


    private String generateClassStructure(ClassUnit ollirClass) {
        StringBuilder code = new StringBuilder();

        this.className = ollirClass.getClassName();
        this.accessLevel = ollirClass.getClassAccessModifier();
        this.superClass = ollirClass.getSuperClass();
        if (this.superClass == null) this.superClass = "java/lang/Object";

        code.append(".class public ");
        if (ollirClass.isStaticClass())
            code.append("static ");
        code.append(this.className).append("\n");

        code.append(".super ").append(this.superClass).append("\n");

        for (Field field : ollirClass.getFields()) {
            code.append(".field public ");
            if (field.isStaticField())
                code.append("static ");
            code.append(field.getFieldName()).append(" ");
            code.append(convertType(field.getFieldType()));
            code.append("\n");
        }

        return code.toString();
    }

    private static String convertType(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case ARRAYREF:
                return "[I";
            case OBJECTREF, CLASS: {
                var objectRef = (ClassType) type;
                return "L" + objectRef.getName() + ";";
            }
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            default:
                throw new NotImplementedException(type.getTypeOfElement());
        }
    }

    private String generateMethodsStructure(ClassUnit ollirClass) {
        StringBuilder code = new StringBuilder();

        for (Method method : ollirClass.getMethods()) {

            if (method.isConstructMethod()) {
                code.append("\n.method public <init>()V\n");
                code.append(generateMethodBody(method));
                code.append("\treturn\n");
                code.append(".end method\n\n");
            } else if (method.getMethodName().equals("main")) {
                code.append(".method public static main([Ljava/lang/String;)V\n");
                code.append(generateStacklimits());
                code.append(generateLocalLimits(method));
                code.append(generateMethodBody(method));
                code.append(".end method\n\n");
            } else {
                code.append(".method public ");
                code.append(method.getMethodName() + "(");
                for (Element element : method.getParams()) {
                    code.append(convertType(element.getType()));
                }
                code.append(")" + convertType(method.getReturnType()) + "\n");
                code.append(generateStacklimits());
                code.append(generateLocalLimits(method));
                code.append(generateMethodBody(method));
                code.append(".end method\n\n");

            }
            this.currStackSize = 0;
            this.maxStackSize = 0;

        }
        return code.toString();
    }

    private String generateMethodBody(Method method) {
        StringBuilder code = new StringBuilder();

        List<Instruction> instructions = method.getInstructions();

/*
        for (Instruction instruction: instructions) {
            for (Map.Entry<String, Instruction> label : method.getLabels().entrySet()) {
                if (label.getValue().equals(instruction)) {
                    code.append(label.getKey()).append(":\n");
                }
            }
//        }
*/

        for (Instruction instruction : instructions) {
            code.append(getInstructions(instruction, method));
            if (instruction.getInstType() == InstructionType.CALL && ((CallInstruction) (instruction)).getReturnType().getTypeOfElement() != ElementType.VOID) {
                updateStackSize(-1);
                code.append("\t").append("pop").append("\n");
            }
        }
        return code.toString();
    }

    private String getInstructions(Instruction instruction, Method method) {
        StringBuilder code = new StringBuilder();

        List<String> labels = method.getLabels(instruction);
        for (String label : labels) {
            code.append(label).append(":\n\t");
        }

        switch (instruction.getInstType()) {
            case RETURN:
                code.append(getReturnInstruction((ReturnInstruction) instruction, method));
                break;
            case PUTFIELD:
                code.append(getPutFieldInstruction((PutFieldInstruction) instruction, method));
                break;
            case CALL:
                code.append(getCallInstruction((CallInstruction) instruction, method));
                break;
            case GETFIELD:
                code.append(getGetFieldInstruction((GetFieldInstruction) instruction, method));
                break;
            case UNARYOPER:
                code.append(getUnaryOpInstruction((UnaryOpInstruction) instruction, method));
                break;
            case BINARYOPER:
                code.append(getBinaryOpInstruction((BinaryOpInstruction) instruction, method));
                break;
            case NOPER:
                code.append(getLoad(((SingleOpInstruction) instruction).getSingleOperand(), method));
                break;
            case ASSIGN:
                code.append(getAssignInstruction((AssignInstruction) instruction, method));
                break;
            case BRANCH:
                code.append(getBranchInstruction((CondBranchInstruction) instruction, method));
                break;
            case GOTO:
                code.append(getGoToInstruction((GotoInstruction) instruction));
                break;
        }
        ;

        this.maxStackSize = Integer.max(this.maxStackSize, this.currStackSize);
        return code.toString();
    }

    private String getGoToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String getBranchInstruction(CondBranchInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();

        if (instruction instanceof SingleOpCondInstruction) {
            Element operand = ((SingleOpCondInstruction) instruction).getCondition().getSingleOperand();
            code.append(getLoad(operand, method));

            // Use ifeq or ifne based on the condition type
            if (operand.isLiteral()) {
                LiteralElement literal = (LiteralElement) operand;
                if (Integer.parseInt(literal.getLiteral()) == 0) {
                    updateStackSize(-1);
                    code.append("\tifeq " + instruction.getLabel() + "\n");
                } else {
                    updateStackSize(-1);
                    code.append("\tifne " + instruction.getLabel() + "\n");
                }
            } else {
                // Default to using ifne
                updateStackSize(-1);
                code.append("\tifne " + instruction.getLabel() + "\n");
            }
        } else if (instruction instanceof OpCondInstruction) {
            OpInstruction opInstruction = ((OpCondInstruction) instruction).getCondition();
            code.append(handleOpCondition(opInstruction, instruction, method));
        }

        return code.toString();
    }

    private String handleOpCondition(OpInstruction opInstruction, CondBranchInstruction condBranchInstruction, Method method) {
        OperationType type;
        StringBuilder code = new StringBuilder();

        if (opInstruction instanceof BinaryOpInstruction binaryOpInstruction) {
            type = binaryOpInstruction.getOperation().getOpType();
            code.append(getLoad(binaryOpInstruction.getLeftOperand(), method)).append("\n");
            code.append(getLoad(binaryOpInstruction.getRightOperand(), method)).append("\n");
        }
        else if (opInstruction instanceof UnaryOpInstruction unaryOpInstruction) {
            type = unaryOpInstruction.getOperation().getOpType();
            code.append(getLoad(unaryOpInstruction.getOperand(), method)).append("\n");
        }
        else {
            throw new RuntimeException("Invalid operation type");
        }

        switch (type) {
            case EQ -> {
                updateStackSize(-2);
                code.append("\tif_icmpeq " + condBranchInstruction.getLabel() + "\n");
            }
            case NEQ -> {
                updateStackSize(-2);
                code.append("\tif_icmpne " + condBranchInstruction.getLabel() + "\n");
            }
            case LTH -> {
                updateStackSize(-2);
                code.append("\tif_icmplt " + condBranchInstruction.getLabel() + "\n");
            }
            case GTH -> {
                updateStackSize(-2);
                code.append("\tif_icmpgt " + condBranchInstruction.getLabel() + "\n");
            }
            case LTE -> {
                updateStackSize(-2);
                code.append("\tif_icmple " + condBranchInstruction.getLabel() + "\n");
            }
            case GTE -> {
                updateStackSize(-2);
                code.append("\tif_icmpge " + condBranchInstruction.getLabel() + "\n");
            }
            case AND, ANDB -> {
                updateStackSize(-1);
                updateStackSize(-1);
                code.append("\tiand\n").append("\tifne " + condBranchInstruction.getLabel() + "\n");
            }
            case OR, ORB -> {
                updateStackSize(-1);
                updateStackSize(-1);
                code.append("\tior\n").append("\tifne " + condBranchInstruction.getLabel() + "\n");
            }
            case NOT, NOTB -> {
                updateStackSize(-1);
                code.append("\tifeq " + condBranchInstruction.getLabel() + "\n");
            }
        }
        return code.toString();
    }


    private String getCallInstruction(CallInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();
        Operand firstArg = (Operand) instruction.getFirstArg();
        LiteralElement secondArg = (LiteralElement) instruction.getSecondArg();

        CallType callType = instruction.getInvocationType();
        switch (callType) {
            case invokestatic, invokevirtual -> {
                if (callType == CallType.invokevirtual) {
                    code.append(getLoad(firstArg, method)).append("\n");
                }
                for (Element element : instruction.getListOfOperands()) {
                    code.append(getLoad(element, method)).append("\n");
                }
                if (callType == CallType.invokestatic) {
                    code.append("\tinvokestatic ");
                    code.append(firstArg.getName()).append("/").append(secondArg.getLiteral().replace("\"", "")).append("(");
                } else {
                    code.append("\tinvokevirtual ");
                    ClassType classType = (ClassType) firstArg.getType();
                    code.append(classType.getName()).append("/").append(secondArg.getLiteral().replace("\"", "")).append("(");
                }
                for (Element element : instruction.getListOfOperands()) {
                    code.append(convertType(element.getType()));
                }
                code.append(")").append(convertType(instruction.getReturnType())).append("\n");
                break;
            }
            case invokespecial -> {
                if (firstArg.getName().equals("this")) {
                    code.append(getLoad(firstArg, method)).append("\n");
                }
                for (Element element : instruction.getListOfOperands()) {
                    code.append(getLoad(element, method)).append("\n");
                }
                code.append("\tinvokespecial ");
                if (method.isConstructMethod() && firstArg.getName().equals("this")) {
                    if (method.getOllirClass().getSuperClass() == null) {
                        code.append("java/lang/Object");
                    }else {
                        code.append(this.superClass);
                    }
                } else {
                    ClassType classType1 = (ClassType) firstArg.getType();
                    code.append(classType1.getName());
                }
                code.append("/").append(secondArg.getLiteral().replace("\"", "")).append("(");
                for (Element element : instruction.getListOfOperands()) {
                    code.append(convertType(element.getType()));
                }
                code.append(")").append(convertType(instruction.getReturnType())).append("\n");
                break;
            }

            case NEW -> {
                updateStackSize(1);
                ElementType elementType = firstArg.getType().getTypeOfElement();
                if (elementType == ElementType.OBJECTREF || elementType == ElementType.CLASS) {
                    code.append("\tnew ").append(firstArg.getName()).append("\n");
                    code.append("\tdup\n");
                } else if (elementType == ElementType.ARRAYREF) {
                    // TODO
                    ArrayList<Element> operands = instruction.getListOfOperands();
                    if (operands.size() < 1) {
                        return "";
                    }
                    code.append(getLoad(operands.get(0), method)).append("\n");
                    code.append("\tnewarray int \n");
                }
                break;
            }
            case arraylength -> {
                code.append(getLoad(instruction.getFirstArg(), method));
                code.append("\tarraylength\n");
            }
            case ldc -> {
                code.append(getLoad(firstArg, method)).append("\n");
                break;
            }
        }
        return code.toString();
    }

    private String getPutFieldInstruction(PutFieldInstruction instruction, Method method) {
        updateStackSize(-2);
        Element firstOp = instruction.getFirstOperand();
        Element secondOp = instruction.getSecondOperand();

        if (firstOp.isLiteral() || secondOp.isLiteral()) return "";

        StringBuilder code = new StringBuilder();
        Element newOp = instruction.getThirdOperand();

        code.append(getLoad(firstOp, method)).append("\n");
        code.append(getLoad(newOp, method)).append("\n");
        code.append("\tputfield ");

        ClassType classType = (ClassType) firstOp.getType();

        code.append(classType.getName()).append("/").append(((Operand) secondOp).getName());
        code.append(" ").append(convertType(secondOp.getType())).append("\n");
        return code.toString();
    }

    private String getGetFieldInstruction(GetFieldInstruction instruction, Method method) {
        Element firstOp = instruction.getFirstOperand();
        Element secondOp = instruction.getSecondOperand();

        if (firstOp.isLiteral() || secondOp.isLiteral()) return "";

        StringBuilder code = new StringBuilder();

        code.append(getLoad(firstOp, method)).append("\n");
        code.append("\tgetfield ");

        ClassType classType = (ClassType) firstOp.getType();

        code.append(classType.getName()).append("/").append(((Operand) secondOp).getName());
        code.append(" ").append(convertType(secondOp.getType())).append("\n");
        return code.toString();
    }

    private String getBinaryOpInstruction(BinaryOpInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();
        Element leftElement = instruction.getLeftOperand();
        Element rightElement = instruction.getRightOperand();
        OperationType operationType = instruction.getOperation().getOpType();

        code.append(getLoad(leftElement, method)).append(getLoad(rightElement, method)).append("\t");
        switch (operationType) {
            case ADD: {
                updateStackSize(-1);
                code.append("iadd");
                break;
            }
            case SUB: {
                updateStackSize(-1);
                code.append("isub");
                break;
            }
            case MUL: {
                updateStackSize(-1);
                code.append("imul");
                break;
            }
            case DIV: {
                updateStackSize(-1);
                code.append("idiv");
                break;
            }
            case LTH: {
                updateStackSize(-2);
                String operationStr = "LESS_THAN";
                String operation = "if_icmplt " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
            case GTH: {
                updateStackSize(-2);
                String operationStr = "GREATER_THAN";
                String operation = "if_icmpgt " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
            case LTE: {
                updateStackSize(-2);
                String operationStr = "LESS_THAN_OR_EQUAL";
                String operation = "if_icmple " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
            case GTE: {
                updateStackSize(-2);
                String operationStr = "GREATER_THAN_OR_EQUAL";
                String operation = "if_icmpge " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
            case EQ: {
                updateStackSize(-2);
                String operationStr = "EQUAL";
                String operation = "if_icmpeq " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
            case NEQ: {
                updateStackSize(-2);
                String operationStr = "NOT_EQUAL";
                String operation = "if_icmpne " + operationStr + "_" + this.labelCounter;
                code.append(getIf(operationStr, operation));
                break;
            }
/*
                try {
                    if (rightElement.isLiteral()) {
                        LiteralElement literalElement = (LiteralElement) rightElement;
                        int literal = Integer.parseInt(literalElement.getLiteral());
                        if (literal == 0) {
                            return getLoad(leftElement, method) + "\n\t" + this.getIf("iflt");
                        } else {
                            throw new Exception("Binary Op Instruction: LTH case, literal is not 0");
                        }
                    } else {
                        throw new Exception("Binary Op Instruction: LTH case, right element is not literal");
                    }
                } catch (Exception e) {
                    code.append(getIf("if_icmplt"));
                }
                break;
            }
*/
            case AND, ANDB: {
                updateStackSize(-1);
                code.append("iand");
                break;
            }
            case OR, ORB: {
                updateStackSize(-1);
                code.append("ior");
                break;
            }
            default: code.append(""); // code.append(getIf("if_icpmpeq"));
        }
        code.append("\n");
        return code.toString();
    }

    private String getUnaryOpInstruction(UnaryOpInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();
        OperationType operationType = instruction.getOperation().getOpType();
        Element element = instruction.getOperand();

        if (operationType == OperationType.NOT || operationType == OperationType.NOTB) {
//            code.append(getLoad(element, method)).append("\n\tifeq\n");
            code.append(getLoad(element, method));
            code.append("\ticonst_1\n");
        }
        return code.toString();
    }



    private String getReturnInstruction(ReturnInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();

        if (instruction.hasReturnValue()) code.append(getLoad(instruction.getOperand(), method));

        ElementType returnType = instruction.getReturnType().getTypeOfElement();

        code.append("\t");
        switch (returnType) {
            case BOOLEAN, INT32, OBJECTREF, CLASS, STRING, ARRAYREF:

                if (returnType == ElementType.BOOLEAN || returnType == ElementType.INT32) {
                    updateStackSize(-1);
                    code.append("ireturn\n");
                }
                else {
                    updateStackSize(-1);
                    code.append("areturn\n");
                }
                break;
            case VOID: code.append("return\n");
        }

        return code.toString();
    }


    private String getAssignInstruction(AssignInstruction instruction, Method method) {
        Element dest = instruction.getDest();

        if (dest.isLiteral()) {
            return "";
        }

        if (dest instanceof ArrayOperand arrayOperand) {
            StringBuilder code = new StringBuilder();
            Descriptor arrayDescriptor = method.getVarTable().get(arrayOperand.getName());
            updateStackSize(1);
            code.append("\taload" + (arrayDescriptor.getVirtualReg() < 4 ? "_" : " ") + arrayDescriptor.getVirtualReg() + "\n");

            Element index = arrayOperand.getIndexOperands().get(0);
            code.append(getLoad(index, method));

            code.append((getInstructions(instruction.getRhs(), method)));

            ElementType elementType = instruction.getTypeOfAssign().getTypeOfElement();

            switch (elementType) {
                case THIS, OBJECTREF, CLASS, STRING, ARRAYREF -> {
                    updateStackSize(-3);
                    code.append("\taastore\n");
                }
                case INT32, BOOLEAN -> {
                    updateStackSize(-3);
                    code.append("\tiastore\n");
                }
            }
            return code.toString();
        }

        Instruction rhs = instruction.getRhs();

        return getInstructions(rhs, method) + getStore(dest, method) + "\n";

    }

    private String getLoad(Element element, Method method) {
        updateStackSize(1);
        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;
            String literal = literalElement.getLiteral();
            int literalInt = Integer.parseInt(literal);

            switch (literalElement.getType().getTypeOfElement()) {
                case INT32, BOOLEAN: {
                    if (literalInt == -1) return "\ticonst_m1\n";
                    if (literalInt >= 0 && literalInt <= 5) return "\ticonst_" + literalInt + "\n";
                    if (literalInt >= -128 && literalInt <= 127) return "\tbipush " + literalInt + "\n";
                    if (literalInt >= -32768 && literalInt <= 32767) return "\tsipush " + literalInt + "\n";
                    updateStackSize(1);
                    return "\tldc " + literalInt + "\n";
                }
                default: return "";
            }
        }
        else {
            Operand operand = (Operand) element;
            Descriptor operandDescriptor = method.getVarTable().get(operand.getName());

            if (operandDescriptor.getVirtualReg() < 0) return "";

            switch (operandDescriptor.getVarType().getTypeOfElement()) {
                case INT32, BOOLEAN: {
                    updateStackSize(1);
                    return "\tiload" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                }
                case CLASS, OBJECTREF, THIS, STRING: {
                    updateStackSize(1);
                    return "\taload" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                }
                case ARRAYREF: {/*
                    StringBuilder code = new StringBuilder();
                    Descriptor arrayDescriptor = method.getVarTable().get(operand.getName());

                    ArrayOperand arrayOperand = (ArrayOperand) operand;

                    updateStackSize(1);
                    code.append("\taload" + (arrayDescriptor.getVirtualReg() < 4 ? "_" : "") + "\n");

                    Element index = arrayOperand.getIndexOperands().get(0);
                    code.append(getLoad(index, method));

                    switch (element.getType().getTypeOfElement()) {
                        case THIS, OBJECTREF, CLASS, STRING, ARRAYREF -> {
                            updateStackSize(-1);
                            code.append("\taaload");
                        }
                        case INT32, BOOLEAN -> {
                            updateStackSize(-1);
                            code.append("\tiaload");
                        }
                    }
*/

                    StringBuilder code = new StringBuilder();

                    updateStackSize(1);
                code.append("\t").append("aload").append((operandDescriptor.getVirtualReg() < 4 ? "_" : " ")).append(operandDescriptor.getVirtualReg()).append("\n");

                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;
                        code.append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        code.append(getLoad(index, method)).append("\n");

                        updateStackSize(-1);
                        code.append("\tiaload\n");

                    }
                    return code.toString();
                }
               default: return "";
            }
        }
    }

    private String getStore(Element element, Method method) {


        if (element.isLiteral()) return "";
        else {
            Operand operand = (Operand) element;
            Descriptor operandDescriptor = method.getVarTable().get(operand.getName());

            switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN: {
                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;
                        StringBuilder code = new StringBuilder();
                        updateStackSize(1);
                        code.append("aload").append((operandDescriptor.getVirtualReg() < 4 ? "_" : " ")).append(operandDescriptor.getVirtualReg()).append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        code.append(getLoad(index, method));
                        return code.toString();
                    }

                    updateStackSize(-1);
                    return "\tistore" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                }
                case CLASS, OBJECTREF, THIS, STRING: {
                    updateStackSize(-1);
                    return "\tastore" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                }

                case ARRAYREF: {
                    StringBuilder code = new StringBuilder();

                    if (element instanceof ArrayOperand) {
                        ArrayOperand arrayOperand = (ArrayOperand) operand;
                        updateStackSize(1);
                        code.append("\taload").append((operandDescriptor.getVirtualReg() < 4 ? "_" : " ")).append(operandDescriptor.getVirtualReg()).append("\n");

                        ArrayList<Element> indexes = arrayOperand.getIndexOperands();
                        Element index = indexes.get(0);

                        code.append(getLoad(index, method)).append("\n");
                    } else {
                        code.append("\tastore_").append(operandDescriptor.getVirtualReg());
                    }

                    return code.toString();
                }
                default: return "";
            }
        }
    }

/*    private String checkIncrement(AssignInstruction instruction, Method method, Descriptor lhsDescriptor) {
        StringBuilder code = new StringBuilder();
        final Instruction rhsInstruction = instruction.getRhs();
        if (!(rhsInstruction instanceof BinaryOpInstruction binaryOpInstruction)) return "";

        OperationType operationType = binaryOpInstruction.getOperation().getOpType();
        if (operationType != OperationType.ADD) return "";

        final Element leftOperand = binaryOpInstruction.getLeftOperand();
        final Element rightOperand = binaryOpInstruction.getRightOperand();

        final Descriptor leftDescriptor = method.getVarTable().get(getElementName(leftOperand));
        final Descriptor rightDescriptor = method.getVarTable().get(getElementName(rightOperand));

        Descriptor varDecriptor;
        Element complement;

        if (leftDescriptor == null || leftDescriptor.getVirtualReg() != lhsDescriptor.getVirtualReg()) {
            if (rightDescriptor == null || rightDescriptor.getVirtualReg() != lhsDescriptor.getVirtualReg())
                return "";
            varDecriptor = rightDescriptor;
            complement = leftOperand;
        } else {
            varDecriptor = leftDescriptor;
            complement = rightOperand;
        }

        if (!complement.isLiteral() || complement.getType().getTypeOfElement() != ElementType.INT32) return "";

        final int increment = Integer.parseInt(getElementName(complement));
        if (increment < -128 || increment > 127) return ""; // Max 1 byte

        code.append("iinc " + varDecriptor.getVirtualReg() + " " +  increment);

        return code.toString();
    }*/

    public static String getElementName(Element element) {
        if (element.isLiteral())
            return ((LiteralElement) element).getLiteral();
        return ((Operand) element).getName();
    }

    private String getIf(String operationStr, String operation) {
        StringBuilder code = new StringBuilder();

        code.append(operation).append("\n");
        code.append("\tldc 0\n");
        code.append("\tgoto NOT_" + operationStr + "_" + this.labelCounter + "\n");
        code.append("\t" + operationStr + "_" + this.labelCounter + ":\n");
        code.append("\tldc 1\n");
        code.append("\tNOT_" + operationStr + "_" + this.labelCounter + ":\n");
        this.labelCounter++;
        return code.toString();
    }

    private void updateStackSize(int size) {
            this.currStackSize += size;
            this.maxStackSize = Math.max(this.maxStackSize, this.currStackSize);
    }
}

