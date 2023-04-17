package pt.up.fe.comp2023;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.source.util.SourcePositions;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;



public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
//        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
//        SimpleParser parser = new SimpleParser();

        // Parse stage
//        JmmParserResult parserResult = parser.parse(code, parser.getDefaultRule(), config);

        // Check if there are parsing errors
//        TestUtils.noErrors(parserResult.getReports());

        // Print the resulting AST
//        System.out.println(parserResult.getRootNode().toTree());

        // Testing the generated code
        // Generator gen = new Generator();
        // String generatedCode = gen.visit(parserResult.getRootNode(), "");
        // System.out.println(generatedCode);

/*
         Generator gen = new Generator();
        gen.visit(parserResult.getRootNode(), "");

        MySymbolTable symbolTable = gen.getSymbolTable();

        System.out.println("Printing Symbol Table...");
        symbolTable.printSymbolTable();
*/

        System.out.println("OLLIR -> JASMIN");
        String ollirCode = SpecsIo.read(inputFile);
        OllirResult ollirResult = new OllirResult(ollirCode, Collections.emptyMap());
//        JasminResult jasminResult = TestUtils.backend(ollirResult);
        MyJasminBackend jasminBackend = new MyJasminBackend();
        JasminResult myJasminResult = jasminBackend.toJasmin(ollirResult);

        System.out.println(myJasminResult.getJasminCode());

        // ... add remaining stages
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }


}
