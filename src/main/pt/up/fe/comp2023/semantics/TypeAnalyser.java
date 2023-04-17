package pt.up.fe.comp2023.semantics;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.MySymbolTable;

import java.util.ArrayList;

public class TypeAnalyser extends Analyser {
    public TypeAnalyser(JmmParserResult parserResult, MySymbolTable mySymbolTable) {
        super(parserResult, mySymbolTable);
    }

    @Override
    public void eval(JmmNode node) {
        this.checkIdentifiers(node, new ArrayList<>());
    }

    private void checkIdentifiers(JmmNode node, ArrayList<String> identifiers) {
        String methodName = node.get("name");


    }


}
