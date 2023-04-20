package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;


public class MyJasminBackend implements JasminBackend {
    private String className;
    private AccessModifiers accessLevel;
    private String superClass;
    private int limit_stack = 99;
    private int limit_locals = 99;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(generateClassStructure(ollirClass));
        jasminCode.append(generateMethodsStructure(ollirClass));

        return new JasminResult(jasminCode.toString());
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

        for (Field field: ollirClass.getFields()) {
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
            case INT32: return "I";
            case BOOLEAN: return "Z";
            case ARRAYREF: return "[I";
            case OBJECTREF, CLASS: {
                var objectRef = (ClassType) type;
                return "L" + objectRef.getName() + ";";
            }
            case STRING: return "Ljava/lang/String;";
            case VOID: return "V";
            default:
                throw new NotImplementedException(type.getTypeOfElement());
        }
    }

    private String generateMethodsStructure(ClassUnit ollirClass) {
        StringBuilder code = new StringBuilder();

        for (Method method: ollirClass.getMethods()) {
            if (method.isConstructMethod()) {
                code.append("\n.method public <init>()V\n");
                code.append(generateMethodBody(method));
                code.append("\treturn\n");
                code.append(".end method\n\n");
            }


            else if (method.getMethodName().equals("main")) {
                code.append(".method public static main([Ljava/lang/String;)V\n");
                code.append("\t.limit stack " + limit_stack + "\n");
                code.append("\t.limit locals " + limit_locals + "\n\n");
                code.append(generateMethodBody(method));
                code.append(".end method\n\n");
            }
            else {
               code.append(".method public ");
               code.append(method.getMethodName() + "(");
               for ( Element element: method.getParams()) {
                   code.append(convertType(element.getType()));
               }
               code.append(")" + convertType(method.getReturnType()) + "\n");
               code.append("\t.limit stack " + limit_stack + "\n");
               code.append("\t.limit locals " + limit_locals + "\n\n");
               code.append(generateMethodBody(method));
               code.append(".end method\n\n");

            }
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
        }
*/

        for (Instruction instruction: instructions) {
            code.append(getInstructions(instruction, method));
        }
        return code.toString();
    }

    private String getInstructions(Instruction instruction, Method method) {
        StringBuilder code = new StringBuilder();
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
        };
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

/*
                if(!instruction.getReturnType().getTypeOfElement().equals(ElementType.VOID) && !instruction.getInstType().equals(InstructionType.ASSIGN)) {
                    code.append("\tpop\n");
                }
*/
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
/*
                if(!instruction.getReturnType().getTypeOfElement().equals(ElementType.VOID) && !instruction.getInstType().equals(InstructionType.ASSIGN)) {
                    code.append("\tpop\n");
                }
*/
                break;
            }

/*
                    if (!method.isConstructMethod()) {
                        code.append("\n").append(getStore(firstArg, method));
                    }
*/
            case NEW -> {
                ElementType elementType = firstArg.getType().getTypeOfElement();
                if (elementType == ElementType.OBJECTREF || elementType == ElementType.CLASS) {
                    code.append("\tnew ").append(firstArg.getName()).append("\n");
                    code.append("\tdup\n");
                } else if (elementType == ElementType.ARRAYREF) {
                    // TODO
//                    code.append("");
                }
                break;
            }
            case ldc -> {
                code.append(getLoad(firstArg, method)).append("\n");
                break;
            }
        }
        return code.toString();
    }

    private String getPutFieldInstruction(PutFieldInstruction instruction, Method method) {
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
                code.append("iadd");
                break;
            }
            case SUB: {
                code.append("isub");
                break;
            }
            case MUL: {
                code.append("imul");
                break;
            }
            case DIV: {
                code.append("idiv");
                break;
            }
            default: code.append("");
        }
        code.append("\n");
        return code.toString();
    }

    private String getUnaryOpInstruction(UnaryOpInstruction instruction, Method method) {
        StringBuilder code = new StringBuilder();
        OperationType operationType = instruction.getOperation().getOpType();
        Element element = instruction.getOperand();

        if (operationType == OperationType.NOT || operationType == OperationType.NOTB) {
            code.append(getLoad(element, method)).append("\n\tifeq\n");
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
                    code.append("ireturn\n");
                }
                else {
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

        Instruction rhs = instruction.getRhs();

        return getInstructions(rhs, method) + getStore(dest, method) + "\n";

    }

    private String getLoad(Element element, Method method) {
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
                    return "\tldc " + literalInt + "\n";
                }
                default: return "";
            }
        }
        else {
            Operand operand = (Operand) element;
            Descriptor operandDescriptor = method.getVarTable().get(operand.getName());

            if (operandDescriptor.getVirtualReg() < 0) return "";

            return switch (operandDescriptor.getVarType().getTypeOfElement()) {
                case INT32, BOOLEAN -> "\tiload" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                case CLASS, OBJECTREF, THIS, STRING -> "\taload" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                // TODO arrayref case
                default -> "";
            };
        }
    }

    private String getStore(Element element, Method method) {
        if (element.isLiteral()) return "";
        else {
            Operand operand = (Operand) element;
            Descriptor operandDescriptor = method.getVarTable().get(operand.getName());

            switch (operand.getType().getTypeOfElement()) {
                case INT32, BOOLEAN: {
                    return "\tistore" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                    // TODO arrayref condition
                }
                case CLASS, OBJECTREF, THIS, STRING: {
                    return "\tastore" + (operandDescriptor.getVirtualReg() < 4 ? "_" : " ") + operandDescriptor.getVirtualReg() + "\n";
                }
                // TODO arrayref case
                default: return "";
            }
        }
    }

}

