package pt.up.fe.comp2023;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.optimization.Optimizer;
import pt.up.fe.comp2023.optimization.ollir.Liveness;
import pt.up.fe.comp2023.visitors.ProgramVisitor;
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
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, parser.getDefaultRule(), config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Testing the generated code
        Generator gen = new Generator();
        String generatedCode = gen.visit(parserResult.getRootNode(), "");
        System.out.println(generatedCode);

        MySymbolTable symbolTable = gen.getSymbolTable();


        // System.out.println("Printing Symbol Table...");
        // symbolTable.printSymbolTable();

        ArrayList<Report> reports = new ArrayList<>();

        ProgramVisitor visitor = new ProgramVisitor(symbolTable, reports);

        visitor.visit(parserResult.getRootNode(), "");

        Analysis analysis = new Analysis();

        JmmSemanticsResult semanticsResult = analysis.semanticAnalysis(parserResult);

        System.out.println(semanticsResult.getRootNode().toTree());

        TestUtils.noErrors(reports);

        int counter = 1;

        if (reports.isEmpty()) {
            System.out.println("All good! No reports were found.");
        }

        for (Report report : reports) {
            System.out.println("- Report no. " + counter + ": " + report.toString());
            counter++;
        }

        System.out.println("Starting the otpimization process...");
        Optimizer optimizer = new Optimizer();
        JmmSemanticsResult optimized = optimizer.optimize(semanticsResult);

        //System.out.println(optimized.getRootNode().toTree());

        //MyJmmOptimization optimization = new MyJmmOptimization();
        //OllirResult ollirResult = optimization.toOllir(semanticsResult);

        //Liveness liveness = new Liveness(ollirResult);

        //liveness.in();

        //TestUtils.noErrors(ollirResult);

        // TESTING OLLIR TO JASIMIN



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
