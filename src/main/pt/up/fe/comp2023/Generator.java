package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.Arrays;

public class Generator extends AJmmVisitor<String, String> {

    private MySymbolTable symbolTable = new MySymbolTable();

    private boolean isArray(String literal) {
        return literal.contains("[]");
    }

    private String parseType(String type) {
        return isArray(type) ? type.substring(0, type.length() - 2) : type;
    }

    @Override
    protected void buildVisitor() {
        /*
         * NOTE: the method calls with a 'checkpoint 01' comment next to them correspond to the node visits which are necessary to
         * populate the Symbol Table for this checkpoint. Remaining visitors will have to implemented for future deliveries, but for this
         * checkpoint, these suffice.
        */

        /* program rule */
        addVisit("Program", this::dealWithProgram); // checkpoint 01

        /* import rule */
        addVisit("ImportDeclaration", this::dealWithImportDeclaration); // checkpoint 01

        /* class rule */
        addVisit("ClassDeclaration", this::dealWithClassDeclaration); // checkpoint 01

        /* var rule */
        addVisit("VarDeclaration", this::dealWithVarDeclaration); // checkpoint 01

        /* type rule */
        addVisit("Literal", this::dealWithLiteral);
        addVisit("CustomType",this::dealWithCustomType); // checkpoint 01

        /* argument rule */
        addVisit("Argument", this::dealWithArgument); // checkpoint 01

        /* methodDeclaration rule */
        addVisit("Method", this::dealWithMethod); // checkpoint 01
        addVisit("Main", this::dealWithMain); // checkpoint 01

        /* ifStatement rule */
        addVisit("IfStatement", this::dealWithIfStatement);

        /* elseStatement rule */
        addVisit("ElseStatement", this::dealWithElseStatement);

        /* condition rule */
        addVisit("Condition", this::dealWithCondition);

        /* statement rule */
        addVisit("CodeBlock", this::dealWithCodeBlock);
        addVisit("Conditional", this::dealWithConditional);
        addVisit("While", this::dealWithWhile);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);

        /* expression rule */
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

    private String dealWithLiteral(JmmNode jmmNode, String s) {
        return s + jmmNode.get("keyword");
    }

    private String dealWithCondition(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithElseStatement(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithIfStatement(JmmNode jmmNode, String s) {
        return null;
    }

    private String dealWithArgument(JmmNode jmmNode, String s) {
        return s + visit(jmmNode.getChildren().get(0), "");
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

        return condition + whileStmt;
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
        // fetch the method name
        String methodName = "main";

        for (JmmNode child : jmmNode.getChildren()) {
            // fetch the methods arguments
            if (child.getKind().equals("Argument")) {
                // fetch the parameter name and type
                String argumentName = child.get("parameter");
                Type type = new Type("String", true);

                // update the symbol table
                symbolTable.addParameter(methodName, new Symbol(type, argumentName));
            }

            // fetch the methods local variables
            if (child.getKind().equals("VarDeclaration")) {
                // fetch the variable name and type
                String varName = child.get("var");
                String varType = visit(child.getChildren().get(0), "");

                Type type = new Type(parseType(varType), isArray(varType));

                // update the symbol table
                symbolTable.addLocalVariable(methodName, new Symbol(type, varName));
            }
        }

        return "";
    }

    private void getMethodIdentifiers(JmmNode jmmNode, ArrayList<String> identifiers) {
        for (JmmNode child : jmmNode.getChildren()) {

            if (child.getKind().equals("Assignment")) {
                identifiers.add(child.get("var"));
            }

            else {
                getMethodIdentifiers(child.getChildren().get(0), identifiers);
            }
        }

    }
    private String dealWithMethod(JmmNode jmmNode, String s) {
        ArrayList<String> identifiers = new ArrayList<>();

        // this.getMethodIdentifiers(jmmNode, identifiers);

        // fetch the method name
        String methodName = jmmNode.get("name");

        for (JmmNode child : jmmNode.getChildren()) {
            // fetch the methods arguments
            if (child.getKind().equals("Argument")) {
                // fetch the parameter name and type
                String argumentName = child.get("parameter");
                String argumentType = visit(child, "");

                Type type = new Type(parseType(argumentType), isArray(argumentType));

                // update the symbol table
                symbolTable.addParameter(methodName, new Symbol(type, argumentName));
            }

            // fetch the methods local variables
            if (child.getKind().equals("VarDeclaration")) {
                // fetch the variable name and type
                String varName = child.get("var");
                String varType = visit(child.getChildren().get(0), "");

                Type type = new Type(parseType(varType), isArray(varType));

                // update the symbol table
                symbolTable.addLocalVariable(methodName, new Symbol(type, varName));
            }

            if (child.getKind().equals("Identifier")) {
                identifiers.add(child.get("value"));
            }

            if (child.getKind().equals("Assignment")) {
                identifiers.add(child.get("var"));
            }
        }

        return s;
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, String s) {
        String ret = s;

        for (JmmNode child : jmmNode.getChildren())
            ret += visit(child, "");

        ret += " " + jmmNode.get("var") + ";";

        return ret;
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {
        // fetch the class name
        String className = jmmNode.get("name");

        String classExtension = "";
        // fetch the class extension
        if (jmmNode.hasAttribute("extension"))
            classExtension = jmmNode.get("extension");

        // add the information to the symbol table
        symbolTable.setName(className);
        symbolTable.setExtension(classExtension);

        // fetch the class fields
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("ClassField")) {
                // this node corresponds to the "varDeclaration" node
                JmmNode varNode = child.getChildren().get(0);

                // fetch the variable name and type
                String varName = varNode.get("var");
                String varType = visit(varNode.getChildren().get(0), "");

                Type type = new Type(parseType(varType), isArray(varType));

                // update the symbol table
                symbolTable.addField(new Symbol(type, varName));
            }
            else if (child.getKind().equals("Method")) {
                // fetch the method's name
                String methodName = child.get("name");

                // fetch the return node
                JmmNode returnNode = child.getChildren().get(0);
                String returnType = visit(returnNode.getChildren().get(0), "");

                Type type = new Type(parseType(returnType), isArray(returnType));

                // update the symbol table with the new method
                symbolTable.addMethod(methodName, type);

                // after adding the new method to the table, we must fetch the method's parameters and local variables
                visit(child, "");
            }
            else if (child.getKind().equals("Main")) {
                symbolTable.addMethod("main", new Type("void", false));

                visit(child, "");
            }
        }

        return s;
    }

    private String dealWithImportDeclaration(JmmNode jmmNode, String s) {
        // fetch the import name
        String importName = jmmNode.get("name");

        String importPack = "";

        // fetch the package that was imported
        if (jmmNode.hasAttribute("pack"))
            importPack = jmmNode.get("pack");

        String r1 = importPack.replace("[", "");
        String r2 = r1.replace("]", "");
        String r3 = r2.replace(" ", "");

        ArrayList<String> list = new ArrayList<String>(Arrays.asList(r3.split(",")));

        String result = importName + '.' + String.join(".", list);

        // add the information to the symbol table
        this.symbolTable.addImport(result);

        return s;
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
        System.out.println("Parent node = " + jmmNode.getJmmParent());
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
        return s + "new " + jmmNode.get("objectType") + "()";
    }

    private String dealWithArrayInstantiation(JmmNode jmmNode, String s) {
        return s + "new int [" +  visit(jmmNode.getChildren().get(0),"") + "]";
    }


    private String dealWithMethodInvocation(JmmNode jmmNode, String s) {
        String ret = s + visit(jmmNode.getChildren().get(0),"") + "." + jmmNode.get("method") + "(";
        for(int idx = 1; idx < jmmNode.getChildren().size(); idx++) {
            ret += visit(jmmNode.getChildren().get(idx),"");
            if (idx != jmmNode.getChildren().size() - 1)
                ret += ", "; 
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

    public MySymbolTable getSymbolTable() {
        return this.symbolTable;
    }
}
