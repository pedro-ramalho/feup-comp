package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Generator extends AJmmVisitor<String, String> {

    @Override
    protected void buildVisitor() {
        addVisit("Boolean",this::dealWithBoolean);
        addVisit("Int",this::dealWithInt);
        addVisit("String",this::dealWithString);
        addVisit("CustomType",this::dealWithCustomType);
        addVisit("Negation",this::dealWithNegation);
        addVisit("ArrayAccess",this::dealWithArrayAccess);
        addVisit("ArrayLength",this::dealWithArrayLength);
        addVisit("MethodInvocation",this::dealWithMethodInvocation);
        addVisit("ArrayInstantiation",this::dealWithArrayInstantiation);
        addVisit("CustomInstantiation",this::dealWithCustomInstantiation);
        addVisit("Parenthesis",this::dealWithParenthesis);
        addVisit("BinaryOp",this::dealWithBinaryOp);
        addVisit("Integer",this::dealWithInteger);
        addVisit("True",this::dealWithTrue);
        addVisit("False",this::dealWithFalse);
        addVisit("Identifier",this::dealWithIdentifier);
        addVisit("CurrentObject",this::dealWithCurrentObject);

        addVisit("Program", this::dealWithProgram);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ImportDeclaration", this::dealWithImportDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("IntArray", this::dealWithIntArray);
        addVisit("Method", this::dealWithMethod);
        addVisit("Main", this::dealWithMain);
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("Conditional", this::dealWithConditional);
        addVisit("While", this::dealWithWhile);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    private Boolean isType(String kind) {
        return kind.equals("String") || kind.equals("Boolean") || kind.equals("IntArray") || kind.equals("Int") || kind.equals("CustomType");
    }

    private String dealWithArrayAssignment(JmmNode jmmNode, String s) {
        String ret = s + jmmNode.get("var") + "[";

        String inBrackets = visit(jmmNode.getChildren().get(1), "");

        ret += inBrackets + "] = ";

        String expr = visit(jmmNode.getChildren().get(2), "");

        ret += expr + ";";

        return ret;
    }

    private String dealWithExprStmt(JmmNode jmmNode, String s) {
        String ret = s;

        for (JmmNode child : jmmNode.getChildren())
            ret += visit(child, "");

        return ret + ';';
    }

    private String dealWithWhile(JmmNode jmmNode, String s) {
        String s2 = s + "\t";
        String condition = s + "while (" + visit(jmmNode.getChildren().get(0), "") + ") {\n";
        String whileStmt = "";

        for (JmmNode child : jmmNode.getChildren().get(1).getChildren()) {
            whileStmt += visit(child, s2);
            whileStmt += '\n';
        }

        whileStmt += s + "}";

        return condition + whileStmt; // TODO
    }

    private String dealWithConditional(JmmNode jmmNode, String s) {
        String condition = "(" + visit(jmmNode.getChildren().get(0), "") + ")";

        String ret = s + "if " + condition;
        String s2 = s + "\t";

        if (jmmNode.getChildren().get(1).getKind().equals("CodeBlock")) {
            ret += visit(jmmNode.getChildren().get(1), s);
        }
        else {
            ret += "\n" + visit(jmmNode.getChildren().get(1), s2);
            ret += "\n";
        }

        ret += s + "else";

        if (jmmNode.getChildren().get(2).getKind().equals("CodeBlock")) {
            ret += visit(jmmNode.getChildren().get(2), s);
        }
        else {
            ret += "\n" + visit(jmmNode.getChildren().get(2), s2);
            ret += "\n";
        }

        return ret;
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {
        String ret = s + jmmNode.get("var") + " = ";

        for (JmmNode child : jmmNode.getChildren())
            ret += visit(child, "");

        return ret + ';';
    }

    private String dealWithCodeBlock(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String s2 = s + "\t";
        String ret = " {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            ret += visit(child, s2);
            ret += "\n";
        }

        ret += s + "}\n";

        return ret;
    }

    private String dealWithMain(JmmNode jmmNode, String s) {
        String s2 = s + "\t";
        String ret = s + "public static void main(String[] args) {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            ret += visit(child, s2);
            ret += '\n';
        }

        ret += s + "}\n";

        return ret;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        String s2 = s + '\t';

        // fetch the return type of the method, it's always the first child
        String returnType = visit(jmmNode.getChildren().get(0), "");

        String ret = s + "public " + returnType + " " + jmmNode.get("name") + "(";

        // fetch the list of the methods parameters and convert them into clean strings
        String paramList = jmmNode.get("parameter").substring(1, jmmNode.get("parameter").length() - 1);
        List<String> argNameList = Arrays.asList(paramList.split(", "));
        List<String> argTypeList = new ArrayList<>();

        int lastArgIdx = 1;

        // if the method has no parameters we can skip the following for-loops
        if (paramList.isEmpty()) {
            ret += ") {\n";
        }
        else {
            // fetch the types of the argument
            for (int idx = 1; idx < jmmNode.getChildren().size(); idx++) {
                JmmNode child = jmmNode.getChildren().get(idx);

                // if it's a node of kind 'type' we are dealing with an argument
                if (isType(child.getKind())) {
                    argTypeList.add(visit(child, ""));
                } else {
                    lastArgIdx = idx;
                    break;
                }
            }

            // build the parameter string
            for (int idx = 0; idx < argTypeList.size(); idx++) {
                String currType = argTypeList.get(idx);
                String currName = argNameList.get(idx);
                String param = "";

                if (idx != argTypeList.size() - 1) {
                    param = currType + " " + currName + ", ";
                } else {
                    param = currType + " " + currName + ")";
                }

                ret += param;
            }

            ret += " {\n";
        }

        // now we must iterate through the other child nodes and deal with them individually, starting at the first node that isn't of kind 'type'
        for (int i = lastArgIdx; i < jmmNode.getChildren().size(); i++) {
            if (i == jmmNode.getChildren().size()-1) {
                ret += s2 + "\t" + "return " + visit(jmmNode.getChildren().get(i), "") + ";\n";
            }
            else {
                ret += visit(jmmNode.getChildren().get(i), s2);
                ret += '\n';
            }
        }

        ret += s + "}\n";

        return ret;
    }

    private String dealWithIntArray(JmmNode jmmNode, String s) {
        return s + "int[]";
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        String ret = s;

        for (JmmNode child : jmmNode.getChildren())
            ret += visit(child, "");

        ret += " " + jmmNode.get("var") + ";";

        return ret;
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        System.out.println("im in class declaration");
        String ret = s + "public class " + jmmNode.get("name") + " {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            ret += visit(child, "\t");
            ret += '\n';
        }

        ret += s + "}\n";

        return ret;
    }

    private String dealWithImportDeclaration(JmmNode jmmNode, String s) {
        String grammarPack = jmmNode.get("pack");
        String pack = grammarPack.substring(1, grammarPack.length() - 1);

        return s + "import " + pack + ";\n\n";
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            s += visit(child, "");

        return s;
    }

    private String dealWithCurrentObject(JmmNode jmmNode, String s) {
        return s + "this";
    }

    private String dealWithIdentifier(JmmNode jmmNode, String s) {
        return s + jmmNode.get("value");
    }

    private String dealWithFalse(JmmNode jmmNode, String s) {
        return s + "false";
    }

    private String dealWithTrue(JmmNode jmmNode, String s) {
        return s + "true";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        String leftExpr = s + visit(jmmNode.getChildren().get(0), "");

        String op = jmmNode.get("op");

        String rightExpr = visit(jmmNode.getChildren().get(1), "");

        return leftExpr + ' ' + op + ' ' + rightExpr;
    }

    private String dealWithInteger(JmmNode jmmNode, String s) {
        return s + jmmNode.get("value");
    }

    private String dealWithParenthesis(JmmNode jmmNode, String s) {
        return s + "(" + visit(jmmNode.getChildren().get(0),"") + ")";
    }

    private String dealWithCustomInstantiation(JmmNode jmmNode, String s) {
        return s + "new" + jmmNode.get("objectType") + "()";
    }

    private String dealWithArrayInstantiation(JmmNode jmmNode, String s) {
        return s + "new int [" +  visit(jmmNode.getChildren().get(0),"") + "]";
    }


    private String dealWithMethodInvocation(JmmNode jmmNode, String s) {
        String ret = s + visit(jmmNode.getChildren().get(0),"") + "." + jmmNode.get("method") + "(";
        for(JmmNode child: jmmNode.getChildren()){
            ret += visit(child,""); //TODO arranjar forma de pôr virgula e começar loop no index 1
        }
        ret += ")";
        return ret;
    }

    private String dealWithArrayLength(JmmNode jmmNode, String s) {
        return s + visit(jmmNode.getChildren().get(0),"") + ".length";
    }

    private String dealWithArrayAccess(JmmNode jmmNode, String s) {
        return s + visit(jmmNode.getChildren().get(0),"") + "[" + visit(jmmNode.getChildren().get(1),"") + "]";
    }

    private String dealWithNegation(JmmNode jmmNode, String s) {
        return s + "!" + visit(jmmNode.getChildren().get(0),"");
    }

    private String dealWithCustomType(JmmNode jmmNode, String s) {
        return s+jmmNode.get("name");
    }

    private String dealWithString(JmmNode jmmNode, String s) {
        return s+"String";
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        return s+"int";
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        return s+"boolean";
    }
}
