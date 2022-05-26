package it.unimi.di.laser.jchainz;

import org.apache.commons.cli.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import soot.*;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import org.apache.commons.io.FileUtils;

public class App {

    private static final String JAVA_HOME = Util.getProp("JAVA_HOME");
    private static final String PROJECT_HOME = Util.getProp("PROJECT_HOME");
    private static ArrayList<SootMethod> chainElements = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(App.class);
    private static final boolean DEBUG = logger.isDebugEnabled();
    private static String chain;
    private static String outputDirectory;

    private static boolean filterIsPassed(SootMethod source, SootMethod target) {
		Type serializableType = Scene.v().getType("java.io.Serializable");
		boolean targetImplementsSerializable = Scene.v().getFastHierarchy().canStoreType(target.getDeclaringClass().getType(), serializableType);
		boolean targetIsSuperclassOfSource = Scene.v().getFastHierarchy().canStoreType(source.getDeclaringClass().getType(), target.getDeclaringClass().getType());
		boolean targetIsExitPoint = chainElements.get(chainElements.size() - 1).getName().equals(target.getName());
		if (!targetIsExitPoint && !targetImplementsSerializable && !targetIsSuperclassOfSource) {
			return false;
		}
		return true;
	}

    private static void configureSootTransform() {
   
		System.out.println("ConfigureSootTransform");	
	    PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {

   		@Override
            protected void internalTransform(String phaseName, Map options) {

            	boolean filterIsPassed = true;
            	Map<String, String> filterSteps = new LinkedHashMap<>();

                CHATransformer.v().transform();

                DataDependencyGraph interMethodDDG = new DataDependencyGraph();

                for (int i = 0; i < chainElements.size(); i++) {
                    SootMethod currentSootMethod = chainElements.get(i);

                    if (i == 0) {
                        interMethodDDG.setEntryMethod(currentSootMethod);
                    } else if (i == chainElements.size() - 1) {
                        interMethodDDG.setExitMethod(currentSootMethod);
                    }

                    SootMethod nextSootMethod = null;
                    Body nextMethodBody = null;
                    if (i + 1 < chainElements.size()) {
                        nextSootMethod = chainElements.get(i + 1);
                        nextMethodBody = nextSootMethod.getActiveBody();
                    }

                    logger.debug("Analyzing chain element " + currentSootMethod);

                    ExceptionalUnitGraph ug = new ExceptionalUnitGraph(currentSootMethod.getActiveBody());

                    DataDependencyAnalysis analysis = new DataDependencyAnalysis(ug, currentSootMethod, nextSootMethod, nextMethodBody, interMethodDDG);
                    analysis.printDDG();

                    if(nextSootMethod != null) {
                    	String filterStep = currentSootMethod.getDeclaringClass().getName() + "." +
							currentSootMethod.getName() + " ==> " +
							nextSootMethod.getDeclaringClass().getName() + "." +
							nextSootMethod.getName();
                    	if(filterIsPassed(currentSootMethod, nextSootMethod)) {
                    		filterSteps.put(filterStep, "PASS");
						} else {
                    		filterIsPassed = false;
                    		filterSteps.put(filterStep, "FAIL");
						}
					}
                }

                interMethodDDG.toDOTFile(outputDirectory + "/full-ddg.dot");

                DataDependencyGraph reducedDDG = interMethodDDG.copy().reduce();
                reducedDDG.typeAnalysisInit();
                reducedDDG.typeAnalysisPropagate();
                reducedDDG.toDOTFile(outputDirectory + "/h1-intermediate.dot");
                reducedDDG.typeAnalysisReduce();
                reducedDDG.reduce();
                reducedDDG.toDOTFile(outputDirectory + "/h1-final.dot");
                interMethodDDG.printNumberOfEdges();
                reducedDDG.printNumberOfEdges();

                List<DataDependencyNode> entryPoints = reducedDDG.getEntryPoints();
                List<DataDependencyNode> exitPoints = reducedDDG.getExitPoints();

                logger.info(entryPoints.size());
                logger.info(exitPoints.size());

                boolean exploitable = false;
                for (DataDependencyNode entryPoint : entryPoints) {
                    for (DataDependencyNode exitPoint : exitPoints) {
                        if (reducedDDG.containsPath(exitPoint, entryPoint)) {
                            exploitable = true;
                        }
                    }
                }

                System.out.println("Chain is " + (exploitable ? "" : "NOT ") + "exploitable");
                try {
                    new File(outputDirectory + "/" + (exploitable ? "" : "not_") + "exploitable.flag").createNewFile();
                    if(filterIsPassed) {
						new File(outputDirectory + "/filter_pass.flag").createNewFile();
					} else {
						PrintWriter printWriter = new PrintWriter(outputDirectory + "/filter_fail.flag");
						for(String key : filterSteps.keySet()) {
							printWriter.println(key + " | " + filterSteps.get(key));
						}
						printWriter.close();
					}
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (DataDependencyNode entryPoint : reducedDDG.getEntryPoints()) {
                    logger.info(entryPoint);
                }
            }
        }));
    }

    public static void main(String[] args) {

        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        Option inputChain = new Option("c", "input-chain", true, "single chain to analyze");
        inputChain.setRequired(true);
        options.addOption(inputChain);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar chains-analyzer.jar", options);
            System.exit(1);
        }

        List<String> sootArgs = new ArrayList<>();
        sootArgs.addAll(
                Arrays.asList(
                        "-w",
                        "-include-all",
                        "-full-resolver",
                        "-allow-phantom-refs"
                )
        );

        String classpath = JAVA_HOME +
			File.separator + "jre" +
			File.separator + "lib" +
			File.separator + "rt.jar" +
			File.pathSeparator + JAVA_HOME +
			File.separator + "jre" +
			File.separator + "lib" +
			File.separator + "jce.jar";

        String targetJarPath = Util.getProp("targetJarPath");

        BasicConfigurator.configure();

        configureSootTransform();

        logger.setLevel(Level.INFO);

        if (!targetJarPath.equals("")) {
            classpath += File.pathSeparator + targetJarPath;
	    for(String s : targetJarPath.split(File.pathSeparator)) {
                sootArgs.add("-process-dir");
                sootArgs.add(s);
	    }
        }

        logger.info("Soot arguments");
        for (String arg : sootArgs) {
            logger.info(arg);
        }

        logger.info("Soot classpath entries: ");
        for (String s : classpath.split(File.pathSeparator)) {
            logger.info("\t- " + s);
        }

        Options.v().set_soot_classpath(classpath);

        logger.debug("Soot classpath: " + Options.v().soot_classpath());

        Options.v().parse(sootArgs.toArray(new String[0]));

        App.loadChain(cmd.getOptionValue("input-chain"));
    }

    private static void loadChain(String chain) {
        App.chain = chain;
        String[] stringChainElements = chain.split("==>");

        for (String stringChainElement : stringChainElements) {
            String className = stringChainElement.split(":")[0].trim();
            String methodName = stringChainElement.split(":")[1].trim();
            SootClass sootClass = Scene.v().forceResolve(className, SootClass.BODIES);
            SootMethod sootMethod = sootClass.getMethod(methodName);
            
            System.out.println(sootMethod.getName());
            
            chainElements.add(sootMethod);
            sootClass.setApplicationClass();
        }

        outputDirectory = PROJECT_HOME + "/ChainzAnalyzer/output/" + chainElements.get(0).getDeclaringClass().getName() + "/" + chainElements.get(0).getName() + "/" + DigestUtils.sha1Hex(chain);
        File outputDirectoryFile = new File(outputDirectory);

        if(outputDirectoryFile.exists()) {
            System.out.println("Chain " + DigestUtils.sha1Hex(chain) + " already analyzed, remove directory to analyze again");
            System.exit(0);
        }

        outputDirectoryFile.mkdirs();

        try {
            PrintWriter printWriter = new PrintWriter(new File(outputDirectory + "/chain.txt"));
            printWriter.println("[");
            for (SootMethod method : chainElements) {
                printWriter.println(" " + method);
            }
            printWriter.println("]");
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

	try{
        	Scene.v().loadNecessaryClasses();
	}catch(IllegalArgumentException e){
		System.err.println("Error:" + chain);
		e.printStackTrace();
		try{
			FileUtils.deleteDirectory(outputDirectoryFile);
		}catch(Exception filecancellastadirexception){
		
		}
		System.exit(42);
	}

        logger.info("Generating call graph with entry points:");
        logger.info("[");
        for (SootMethod method : chainElements) {
            logger.info(" " + method);
        }
        logger.info("]");

        Scene.v().setEntryPoints(chainElements);
	System.out.println("Before runpacks");
        PackManager.v().runPacks();
	System.out.println("After runpacks");
    }
}
