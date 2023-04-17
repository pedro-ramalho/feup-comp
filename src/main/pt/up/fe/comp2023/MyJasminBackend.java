package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Map;


public class MyJasminBackend implements JasminBackend {
    private String className;
    private AccessModifiers accessLevel;
    private String superClass;
    private String method;
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
        if (this.superClass == null) this.superClass = "java/lang/object";

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

        code.append("\n.method public <init>()V\n");
        code.append("\taload_0\n")
                .append("\tinvokenonvirtual ").append(this.superClass).append("/<init>()V\n")
                .append("\treturn\n")
                .append(".end method\n\n");


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
            if (method.isConstructMethod()) continue;


            if (method.getMethodName().equals("main")) {
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

        for (Instruction instruction: instructions) {
            for (Map.Entry<String, Instruction> label : method.getLabels().entrySet()) {
                if (label.getValue().equals(instruction)) {
                    code.append(label.getKey()).append(":\n");
                }
            }
        }

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
                break;
            case BINARYOPER:
                code.append(getBinaryOpInstruction((BinaryOpInstruction) instruction, method));
                break;
            case NOPER: {
                code.append(getLoad(((SingleOpInstruction) instruction).getSingleOperand(), method));
                break;
            }
            case ASSIGN: {
                code.append(getAssignInstruction((AssignInstruction) instruction, method));
                break;
            }
        };
        return code.toString();
    }

    private String getCallInstruction(CallInstruction instruction, Method method) {
        return "";
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
        Operation operation = instruction.getOperation();
        OperationType operationType = instruction.getOperation().getOpType();

        return "";


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
//       }

//       code.append(getInstructions(instruction.getRhs(), method));
//       code.append(getStore(dest, method));
//
//       return code.toString();

//    }

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

/*
    ASSIGN,
    CALL,
    GOTO,
    BRANCH,
    RETURN,
    PUTFIELD,
    GETFIELD,
    UNARYOPER,
    BINARYOPER,
    NOPER;

    private Descriptor getDescriptor(Element elem) {
        if(elem.isLiteral()) {
            this.reports.add(new StyleReport(ReportType.ERROR, Stage.GENERATION, "Tried to get a descriptor of a literal"));
            return null;
        }

        if(elem.getType().getTypeOfElement() == ElementType.THIS) {
            return this.currMethod.getVarTable().get("this");
        }
        return this.currMethod.getVarTable().get(((Operand) elem).getName());
    }
*/
