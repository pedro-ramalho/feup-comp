package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

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
        typeString.put("boolean",".bool");
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
        addVisit("This",this::dealWithThis);
    }

    private ExprCodeResult dealWithProgram(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren())
            s += visit(child, "").value();
        return new ExprCodeResult("",s);
    }



    private ExprCodeResult dealWithImportDeclaration(JmmNode jmmNode, String s) {
        // fetch the import name
        String importName = jmmNode.get("name");
        // fetch the package that was imported
        if (jmmNode.hasAttribute("pack") && jmmNode.get("pack")!="[]") {
            String importPack = jmmNode.get("pack");
            s += "import " + importName+"."+importPack+ ';' + '\n';
        }else {
            s += "import " + importName + ';' + '\n';
        }
        return new ExprCodeResult("",s);
    }
    private ExprCodeResult dealWithClassDeclaration(JmmNode jmmNode, String s) {
        // fetch the class name
        String className = jmmNode.get("name");
        String classExtension = "";
        // fetch the class extension
        if (jmmNode.hasAttribute("extension"))
            classExtension = " extends " +jmmNode.get("extension");
        s = s + className +  classExtension +" {" + '\n';





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
        String returnStatement = "ret.V;\n";
        s = s + '\t' + ".method public static main(args.array.String).V {\n";

        for (JmmNode child : jmmNode.getChildren()) {
            // fetch the methods local variables
            if (child.getKind().equals("VarDeclaration")) {
                statements+= "";//visit(child,"").value() + '\n';
            }
            if(child.getKind().equals("MethodStatement")){
                statements+=visit(child.getChildren().get(0),"").value();
            }
        }
        body = statements + declarations;
        s+= body;
        s+=returnStatement;
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
        String iStatic = "";
        if (jmmNode.hasAttribute("isStatic")){
            iStatic += " static ";
        }
        boolean parametersFlag = false;
        int currentAuxValue = temporaryVariableNumber;
        for (JmmNode child : jmmNode.getChildren()){
            if(child.getKind().equals("Argument")&&!parametersFlag){
                parameters+=visit(child,"").value();
                parametersFlag = true;
            }
            else if(child.getKind().equals("Argument")){
                parameters+=',';
                parameters+=visit(child,"").value();
            }
            if(child.getKind().equals("ReturnType")){
                JmmNode varType = child.getChildren().get(0);
                if (varType.getKind().equals("Literal")) {
                    returnType = typeString.get(varType.get("keyword"));
                } else {
                    returnType = "." + varType.get("name");
                }
            }
            if(child.getKind().equals("VarDeclaration")){
                declarations+= "";//visit(child,"").value() + '\n';
            }
            if(child.getKind().equals("ReturnStatement")){
                ExprCodeResult childVal = visit(child.getChildren().get(0),"");

                returnStatement+= childVal.prefixCode() +'\n' + "ret"+returnType+ " "+ childVal.value()+";";
            }
            if(child.getKind().equals("MethodStatement")){
                statements+=visit(child.getChildren().get(0),"").value();
            }

        }
        temporaryVariableNumber = currentAuxValue;
        body = declarations + statements;
        s+= ".method " + jmmNode.get("modifier") + iStatic + " " + methodName+"("+parameters+")"+returnType+"{"+'\n'+body+returnStatement+'}';


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
        String methodAbove = getMethodName(jmmNode);
        String type = getType((symbolTable.findVariable(methodAbove,name).getType()).getName());
        JmmNode child = jmmNode.getChildren().get(0);
        ExprCodeResult val = visit(child,"");
        String expression = val.prefixCode();
        String auxVal = val.value();
        String assign = "";
        s += expression + '\n';
        if (symbolTable.isField(methodAbove,name)){
            assign += "putfield(this, " + name + type + ", " +  auxVal + ").V;";
        }else{
            assign += name + type + " :=" + type + " " + auxVal+';';
        }
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


    private ExprCodeResult dealWithThis(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "this");
    }

    private ExprCodeResult dealWithIdentifier(JmmNode jmmNode, String s) {
        String methodAbove = getMethodName(jmmNode);
        String name = jmmNode.get("value");
        String type = this.getType((symbolTable.findVariable(methodAbove,name).getType()).getName());
        int t = symbolTable.isParameter(methodAbove,name);
        if(symbolTable.isField(methodAbove,name)){
            var value = "t" + temporaryVariableNumber+type;
            temporaryVariableNumber++;
            String fieldString = value + " :=" +type+ " " + "getfield(this, " +name +type+")" + type + ";\n";
            return new ExprCodeResult(fieldString,value );
        }else if(t>0){
            return new ExprCodeResult("", "$"+t+"."+name+type);
        }else{
            return new ExprCodeResult("", name+type);
        }
    }

    private ExprCodeResult dealWithFalse(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "false.bool");
    }

    private ExprCodeResult dealWithTrue(JmmNode jmmNode, String s) {
        return new ExprCodeResult("",s + "true.bool");
    }

    private ExprCodeResult dealWithBinaryOp(JmmNode jmmNode, String s) {
        var lhsCode = visit(jmmNode.getJmmChild(0));
        var rhsCode = visit(jmmNode.getJmmChild(1));

        var code = new String();
        code += lhsCode.prefixCode();
        code += rhsCode.prefixCode();
        String type = getType(jmmNode.get("op"));
        var value = "t" + temporaryVariableNumber+type;
        temporaryVariableNumber++;
        code += (value + " :=" +type+ " " + lhsCode.value() + " " + jmmNode.get("op") + type + " " + rhsCode.value() + ";" + '\n');
        return new ExprCodeResult(code,value);
    }

    private ExprCodeResult dealWithInteger(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", jmmNode.get("value") + ".i32");
    }

    private ExprCodeResult dealWithParenthesis(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "(" + visit(jmmNode.getChildren().get(0),"") + ")");
    }

    private ExprCodeResult dealWithCustomInstantiation(JmmNode jmmNode, String s) {
        String type;
        if(jmmNode.getChildren().get(0).hasAttribute("keyword")){
            type = typeString.get(jmmNode.getChildren().get(0).get("keyword"));
        }else{
            type = "." + jmmNode.getChildren().get(0).get("name");
        }
        var value = "t" + temporaryVariableNumber+type;
        temporaryVariableNumber++;
        String newExp = value + " :=" + type + " new ("+ type.substring(1) + ")" + type + ";\n" + "invokespecial("+value+","+'"'+"<init>"+'"'+").V;\n";
        return new ExprCodeResult("", value);
    }

    private ExprCodeResult dealWithArrayInstantiation(JmmNode jmmNode, String s) {
        return new ExprCodeResult("", s + "new int [" +  visit(jmmNode.getChildren().get(0),"") + "]");
    }


    private ExprCodeResult dealWithMethodInvocation(JmmNode jmmNode, String s) {
        String lpsKind = jmmNode.getChildren().get(0).getKind();
        String lps;
        String midType="";
        String expressions = "";
        String methodName = jmmNode.get("method");
        String methodAbove = getMethodName(jmmNode);
        String returnType="";
        if (lpsKind.equals("This")){
            lps = "this";
        }else if(lpsKind.equals("MethodInvocation")){
            ExprCodeResult templps = visit(jmmNode.getChildren().get(0),"");
            expressions += templps.prefixCode();
            lps = templps.value();
        }else{
            lps = jmmNode.getChildren().get(0).get("value");
            String type = "";
            try{
               type += getType((symbolTable.findVariable(methodAbove,lps).getType()).getName());
               lps += type;
            }catch (NullPointerException ignored){}

        }
        if(lps=="this"){
            midType = symbolTable.getClassName();
        }
        else if(symbolTable.isImport(methodAbove,lps)){
            midType = lps;
        }else{
            if(lps.charAt(0)=='$'){
                midType = lps.split("\\.")[1];
            }else{
                midType = lps.split("\\.")[0];
            }

        }
        if (midType.equals(symbolTable.getClassName())){
            Type tempType = symbolTable.getReturnType(methodName);
            if (tempType != null){
                returnType = getType(tempType.getName());
            }
        }
        if (returnType == ""){
            returnType = ".V";
        }
        String para = "";
        for(int idx = 1; idx < jmmNode.getChildren().size(); idx++) {
            para += ", ";
            ExprCodeResult temp = visit(jmmNode.getChildren().get(idx),"");
            expressions += temp.prefixCode();
            para += temp.value();
        }
        String invokeType = getInvoke(lps);
        String ret =expressions;
        if(returnType != ".V"){
            var value = "t" + temporaryVariableNumber+returnType;
            temporaryVariableNumber++;
            ret += value +" :=" + returnType + " "+ invokeType + "(" + lps + ", \"" + methodName + "\"" + para + ")"+ returnType +";\n";
            return new ExprCodeResult(ret, value);
        }else{
            ret += invokeType + "(" + lps + ", \"" + methodName + "\"" + para + ")"+ returnType +";\n";
            return new ExprCodeResult("",ret);
        }

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
    private String getType(String name){
        String type;
        if(typeString.containsKey(name)){
            type = typeString.get(name);
        }else{
            type = "." + name;
        }
        return type;
    }
    private String getInvoke(String lhsName){
        if (lhsName.equals("this")){
            return "invokevirtual";
        }
        List<String> imports = symbolTable.getImports();
        for(String i : imports){
            return "invokestatic";
        }
        return "invokevirtual";
    }
}
