package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class Generator extends AJmmVisitor<String, String> {
    private String className;

    public Generator(String className) {
        this.className = className;
    }

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
        return s + visit(jmmNode.getChildren().get(0),"") + jmmNode.get("op").substring(1,jmmNode.get("op").length()-1) + visit(jmmNode.getChildren().get(1),"");
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
        return s+"String ";
    }

    private String dealWithInt(JmmNode jmmNode, String s) {
        return s+"int ";
    }

    private String dealWithBoolean(JmmNode jmmNode, String s) {
        return s+"boolean ";
    }
}
