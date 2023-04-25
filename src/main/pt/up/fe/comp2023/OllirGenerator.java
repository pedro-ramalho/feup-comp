package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class OllirGenerator extends AJmmVisitor<String, ExprCodeResult> {


    private MySymbolTable symbolTable;
    public OllirGenerator(MySymbolTable symbolTable){
        this.symbolTable = symbolTable;
    }

    private static final HashMap<String, String> typeString = new HashMap<String,String>();

    private ArrayList<String> tempVariables = new ArrayList<String>();
    private ArrayList<String> tempVariablesExpressions = new ArrayList<String>();

    static{
        typeString.put("int",".i32");
        typeString.put("bool",".bool");
        typeString.put("int[]",".array.i32");
        typeString.put("String[]",".array.String");
        typeString.put("String",".String");
        typeString.put("void","V");
        typeString.put("+",".i32");
        typeString.put("-",".i32");
        typeString.put("*",".i32");
        typeString.put("/",".i32");
        typeString.put("<",".bool");
        typeString.put(">",".bool");
        typeString.put("&&",".bool");
        typeString.put("||",".bool");

    }

    private int temporaryVariableNumber = 0;


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

    private ExprCodeResult dealWithProgram(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            s += visit(child, "").value();
        return new ExprCodeResult("",s);
    }



    private ExprCodeResult dealWithImportDeclaration(JmmNode jmmNode, String s) {
        // fetch the import name
        String importName = jmmNode.get("name");

        String importPack = "";
        // fetch the package that was imported
        if (jmmNode.hasAttribute("pack")) {
            importPack = jmmNode.get("pack");
            s = s + "import " + importName+"."+importPack+'\n';
        }else {
            s = s + "import " + importName + '\n';
        }
        return new ExprCodeResult("",s);
    }
    private ExprCodeResult dealWithClassDeclaration(JmmNode jmmNode, String s) {
        // fetch the class name
        String className = jmmNode.get("name");
        s = s + className + " {" + '\n';


        /*String classExtension = "";
        // fetch the class extension
        if (jmmNode.hasAttribute("extension"))
            classExtension = jmmNode.get("extension");
        */

        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("ClassField")) {
                // this node corresponds to the "varDeclaration" node
                JmmNode varNode = child.getChildren().get(0);

                // fetch the variable name and type
                String varName = varNode.get("var");
                JmmNode varType = varNode.getChildren().get(0);
                String type;
                if (varType.getKind().equals("Literal")) {
                    type = typeString.get(varType.get("keyword"));
                } else {
                    type = varType.get("name");
                }

                s = s + '\t' + ".field private " + varName + type + ";" + '\n';
            }
        }
        s = s + "\t.construct " + className+"().V { \n\t\t invokespecial(this, \"<init>\").V;\n\t}\n";

        // fetch the class fields
        for (JmmNode methods : jmmNode.getChildren()) {
            if (methods.getKind().equals("Method") || methods.getKind().equals("Main")) {
                s+= visit(methods,"").value();
            }
        }

        s = s + "}";
        return new ExprCodeResult("",s);
    }

    private ExprCodeResult dealWithMain(JmmNode jmmNode, String s) {
        // fetch the method name
        String body="";
        String declarations="";
        String statements = "";
        s = s + '\t' + ".method public static main(args.array.String).V {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            // fetch the methods local variables
            if (child.getKind().equals("VarDeclaration")) {
                statements+= visit(child,"").value() + '\n';
            }
            if(child.getKind().equals("Assignment")||child.getKind().equals("ArrayAssignment")){
                statements+=visit(child,"").value();
            }
        }
        body = statements + declarations;
        s+= body;
        s += '}';
        return new ExprCodeResult("",s);
    }

    private ExprCodeResult dealWithMethod(JmmNode jmmNode, String s) {
        // fetch the method name
        String methodName = jmmNode.get("name");
        String returnType = "";
        String parameters = "";
        String declarations = "";
        String body = "";
        String statements = "";
        String returnStatement = "";
        boolean parametersFlag = false;
        int currentAuxValue = temporaryVariableNumber;
        for (JmmNode child : jmmNode.getChildren()){
            if(child.getKind().equals("Argument")&&!parametersFlag){
                parameters+=visit(child,"").value();
                parametersFlag = true;
            }
            if(child.getKind().equals("Argument")){
                parameters+=',';
                parameters+=visit(child,"").value();
            }
            if(child.getKind().equals("ReturnType")){
                JmmNode varType = child.getChildren().get(0);
                if (varType.getKind().equals("Literal")) {
                    returnType = typeString.get(varType.get("keyword"));
                } else {
                    returnType = varType.get("name");
                }
            }
            if(child.getKind().equals("VarDeclaration")){
                declarations+= visit(child,"").value() + '\n';
            }
            if(child.getKind().equals("ReturnStatement")){
                ExprCodeResult childVal = visit(child.getChildren().get(0),"");

                returnStatement+= childVal.prefixCode() +'\n' + "ret"+returnType+ " "+ childVal.value();
            }
            if(child.getKind().equals("Assignment")||child.getKind().equals("ArrayAssignment")){
                statements+=visit(child,"").value();
            }

        }
        temporaryVariableNumber = currentAuxValue;
        body = declarations + statements;
        s+= ".method public " + methodName+"("+parameters+")"+returnType+"{"+body+returnStatement+'}';


        /*for (JmmNode child : jmmNode.getChildren()) {
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
        }*/

        return new ExprCodeResult("",s);
    }

    private ExprCodeResult dealWithArgument(JmmNode jmmNode, String s) {
        String name = jmmNode.get("parameter");
        String type = "";
        JmmNode varType = jmmNode.getChildren().get(0);
        if (varType.getKind().equals("Literal")) {
            type = typeString.get(varType.get("keyword"));
        } else {
            type = varType.get("name");
        }
        return new ExprCodeResult("",s+name+type);
    }

    private ExprCodeResult dealWithVarDeclaration(JmmNode jmmNode, String s) {
        String name = jmmNode.get("var");
        String type = "";
        JmmNode varType = jmmNode.getChildren().get(0);
        if (varType.getKind().equals("Literal")) {
            type = typeString.get(varType.get("keyword"));
        } else {
            type = varType.get("name");
        }
        return new ExprCodeResult("",s+name+type+";");
    }

    private ExprCodeResult dealWithAssignment(JmmNode jmmNode, String s) {
        String name = jmmNode.get("var");
        JmmNode child = jmmNode.getChildren().get(0);
        ExprCodeResult val = visit(child,"");
        String expression = val.prefixCode();
        String auxVal = val.value();
        String type = "";
        String assign = "";
        if (child.getKind().equals("Negation")||child.getKind().equals("True")||child.getKind().equals("False")){
            type = typeString.get("bool");
        }else if(child.getKind().equals("BinaryOp")){
            type = typeString.get(child.get("op"));
        }else if(child.getKind().equals("MethodInvocation")){
            type = "";
        }
        s += expression + '\n';
        assign += name + type + " :=" + type + " " + auxVal;
        return new ExprCodeResult("",s + assign + '\n');
    }

    private ExprCodeResult dealWithLiteral(JmmNode jmmNode, String s) {

        return new ExprCodeResult("",s + jmmNode.get("keyword"));
    }

    private ExprCodeResult dealWithCondition(JmmNode jmmNode, String s) {
        return null;
    }

    private ExprCodeResult dealWithElseStatement(JmmNode jmmNode, String s) {
        return null;
    }

    private ExprCodeResult dealWithIfStatement(JmmNode jmmNode, String s) {
        return null;
    }



    private ExprCodeResult dealWithArrayAssignment(JmmNode jmmNode, String s) {
        String ret = s + jmmNode.get("var") + "[";

        String inBrackets = visit(jmmNode.getChildren().get(1), "").value();

        ret += inBrackets + "] = ";

        String expr = visit(jmmNode.getChildren().get(2), "").value();

        ret += expr + ";";

        return new ExprCodeResult("",ret);
    }

    private ExprCodeResult dealWithExprStmt(JmmNode jmmNode, String s) {
        String ret = s;

        for (JmmNode child : jmmNode.getChildren())
            ret += visit(child, "").value();

        return new ExprCodeResult("", ret + ';');
    }

    private ExprCodeResult dealWithWhile(JmmNode jmmNode, String s) {
        String s2 = s + "\t";
        String condition = s + "while (" + visit(jmmNode.getChildren().get(0), "").value() + ") {\n";
        String whileStmt = "";

        for (JmmNode child : jmmNode.getChildren().get(1).getChildren()) {
            whileStmt += visit(child, s2).value();
            whileStmt += '\n';
        }

        whileStmt += s + "}";

        return new ExprCodeResult("" ,condition + whileStmt);
    }

    private ExprCodeResult dealWithConditional(JmmNode jmmNode, String s) {
        String condition = "(" + visit(jmmNode.getChildren().get(0), "").value() + ")";

        String ret = s + "if " + condition;
        String s2 = s + "\t";

        if (jmmNode.getChildren().get(1).getKind().equals("CodeBlock")) {
            ret += visit(jmmNode.getChildren().get(1), s).value();
        }
        else {
            ret += "\n" + visit(jmmNode.getChildren().get(1), s2).value();
            ret += "\n";
        }

        ret += s + "else";

        if (jmmNode.getChildren().get(2).getKind().equals("CodeBlock")) {
            ret += visit(jmmNode.getChildren().get(2), s).value();
        }
        else {
            ret += "\n" + visit(jmmNode.getChildren().get(2), s2).value();
            ret += "\n";
        }

        return new ExprCodeResult("",ret);
    }



    private ExprCodeResult dealWithCodeBlock(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String s2 = s + "\t";
        String ret = " {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            ret += visit(child, s2).value();
            ret += "\n";
        }

        ret += s + "}\n";

        return new ExprCodeResult("",ret);
    }


    private ExprCodeResult dealWithCurrentObject(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "this");
    }

    private ExprCodeResult dealWithIdentifier(JmmNode jmmNode, String s) {
        String methodAbove = getMethodName(jmmNode);
        String name = jmmNode.get("value");
        System.out.println(symbolTable.getFields());
        String type = (symbolTable.findVariable(methodAbove,name).getType()).getName();
        String typeConverted = "";
        if(typeString.containsKey(type)){
            typeConverted+= typeString.get(type);
        }else{
            typeConverted+=type;
        }


        return new ExprCodeResult("", name+typeConverted);
    }

    private ExprCodeResult dealWithFalse(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "false.bool");
    }

    private ExprCodeResult dealWithTrue(JmmNode jmmNode, String s) {
        return new ExprCodeResult("",s + "true.bool");
    }

    private ExprCodeResult dealWithBinaryOp(JmmNode jmmNode, String s) {
        /*String leftExpr = "";
        String rightExpr = "";
        String leftVal = "";
        String rightVal = "";
        String assignAux = "";
        int leftAux = 0;
        int rightAux = 0;
        String op = jmmNode.get("op");
        String opType = typeString.get(op);
        String prev = "";
        int t = temporaryVariableNumber;
        if(jmmNode.getJmmParent().getKind().equals("BinaryOp")){
            assignAux += "Aux" + temporaryVariableNumber + opType + " := ";
        }
        if(jmmNode.getChildren().get(0).getKind().equals("BinaryOp")&&jmmNode.getChildren().get(1).getKind().equals("BinaryOp")){
            leftExpr += visit(jmmNode.getChildren().get(0),"");
            leftAux = temporaryVariableNumber;
            leftVal = "Aux"+ leftAux;
            temporaryVariableNumber++;
            rightExpr += visit(jmmNode.getChildren().get(1),"");
            rightAux = temporaryVariableNumber;
            rightVal = "Aux"+ rightAux;
            temporaryVariableNumber++;
            prev += leftExpr + rightExpr;
            assignAux += leftVal + opType + ' ' + op + opType + ' ' + rightVal + opType + '\n';
        }else if(jmmNode.getChildren().get(0).getKind().equals("BinaryOp")){
            leftExpr += visit(jmmNode.getChildren().get(0),"");
            leftAux = temporaryVariableNumber;
            leftVal = "Aux"+ leftAux;
            temporaryVariableNumber++;
            rightExpr += visit(jmmNode.getChildren().get(1),"");
            prev += leftExpr;
            assignAux += leftVal + opType + ' ' + op + opType + ' ' + rightExpr + '\n';
        }else if(jmmNode.getChildren().get(1).getKind().equals("BinaryOp")){
            leftExpr += visit(jmmNode.getChildren().get(0),"");
            rightExpr += visit(jmmNode.getChildren().get(1),"");
            rightAux = temporaryVariableNumber;
            rightVal = "Aux"+ rightAux;
            temporaryVariableNumber++;
            prev += rightExpr;
            assignAux += leftExpr + ' ' +  op + opType + ' ' + rightVal + opType + '\n';
        }else{
            leftExpr += visit(jmmNode.getChildren().get(0),"");
            rightExpr += visit(jmmNode.getChildren().get(1),"");
            assignAux += leftExpr + ' ' + op + opType + ' ' + rightExpr + '\n';
        }

        return new ExprCodeResult("", prev + s + assignAux);*/
        var lhsCode = visit(jmmNode.getJmmChild(0));
        var rhsCode = visit(jmmNode.getJmmChild(1));

        var code = new String();
        code += lhsCode.prefixCode();
        code += rhsCode.prefixCode();
        var value = "t" + temporaryVariableNumber;
        temporaryVariableNumber++;
        code += (value + " = " + lhsCode.value() + " " + jmmNode.get("op") + " " + rhsCode.value() + '\n');
        return new ExprCodeResult(code,value);
    }

    private ExprCodeResult dealWithInteger(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + jmmNode.get("value") + ".i32");
    }

    private ExprCodeResult dealWithParenthesis(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "(" + visit(jmmNode.getChildren().get(0),"") + ")");
    }

    private ExprCodeResult dealWithCustomInstantiation(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "new " + jmmNode.get("objectType") + "()");
    }

    private ExprCodeResult dealWithArrayInstantiation(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "new int [" +  visit(jmmNode.getChildren().get(0),"") + "]");
    }


    private ExprCodeResult dealWithMethodInvocation(JmmNode jmmNode, String s) {

        String ret = s + visit(jmmNode.getChildren().get(0),"") + "." + jmmNode.get("method") + "(";
        for(int idx = 1; idx < jmmNode.getChildren().size(); idx++) {
            ret += visit(jmmNode.getChildren().get(idx),"");
            if (idx != jmmNode.getChildren().size() - 1)
                ret += ", "; 
        }
        ret += ")";
        return new ExprCodeResult("", ret);
    }

    private ExprCodeResult dealWithArrayLength(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + visit(jmmNode.getChildren().get(0),"") + ".length");
    }

    private ExprCodeResult dealWithArrayAccess(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + visit(jmmNode.getChildren().get(0),"") + "[" + visit(jmmNode.getChildren().get(1),"") + "]");
    }

    private ExprCodeResult dealWithNegation(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "!.bool " + visit(jmmNode.getChildren().get(0),""));
    }

    private ExprCodeResult dealWithCustomType(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s+jmmNode.get("name"));
    }

    public MySymbolTable getSymbolTable() {
        return this.symbolTable;
    }
    private String getMethodName(JmmNode node) {
        Optional<JmmNode> ancestor = node.getAncestor("Method");

        String name = "main";

        if (ancestor.isPresent()) {
            JmmNode method = ancestor.get();

            name = method.get("name");
        }

        return name;
    }
}
