package chainz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import soot.*;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import static chainz.ChainzManager.*;

public class ChainzExplorer {

    String entryExitCouple;
    String targetJarName;
    int maxChainDepth;
    Boolean applyClassFilter;

    String entryPoint;
    String exitPoint;

    String entryPointPackageName;
    String entryPointClassName;
    String entryPointMethodName;

    String exitPointPackageName;
    String exitPointClassName;
    String exitPointMethodName;

    File file;
    BufferedWriter bw;

    LocalDateTime timestart = null;
    int timeout;

    int maxChainz;

    ArrayList<String> chainzStrings = new ArrayList<String>();
    CallGraph methodsCallGraph = null;

    public ChainzExplorer(String targetJarPath, String entryexitcouple, int maxchaindepth, Boolean classFilterFlag, int maxChainz, int timeout) {
        int l = targetJarPath.split("/").length;
        targetJarName = targetJarPath.equals("") ? "openJDK/" : targetJarPath.split("/")[l - 1] + "/";
        entryExitCouple = entryexitcouple;
        maxChainDepth = maxchaindepth;
        applyClassFilter = classFilterFlag;

        /* SETTING ENTRY AND EXIT POINTS */
        entryPoint = entryExitCouple.split(":")[0];
        exitPoint = entryExitCouple.split(":")[1];

        /* Setting time-out */
        this.timeout = timeout;

        String[] tmp;
        tmp = entryPoint.split("\\.");
        entryPointPackageName = "";
        for (int i = 0; i < tmp.length - 2; i++) {
            entryPointPackageName += i == 0 ? tmp[i] : "." + tmp[i];
        }
        entryPointClassName = tmp[tmp.length - 2];
        entryPointMethodName = tmp[tmp.length - 1];

        tmp = exitPoint.split("\\.");
        exitPointPackageName = "";
        for (int i = 0; i < tmp.length - 2; i++) {
            exitPointPackageName += i == 0 ? tmp[i] : "." + tmp[i];
        }
        exitPointClassName = tmp[tmp.length - 2];
        ;
        exitPointMethodName = tmp[tmp.length - 1];
        /*=============================*/

        /* SETTING CHAINZ OUTPUT FILE */
        try {
            file = new File(PROJECT_HOME + chainsDirectoryPath + targetJarName + entryPointClassName + "." + entryPointMethodName + "." + maxChainDepth + "." + "chains");
            file.getParentFile().mkdirs();
            bw = new BufferedWriter(new FileWriter(file, true));
            bw.append(this.targetJarName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*=============================*/


        this.maxChainz = maxChainz;
    }

    public void printConfig() {
        String s = "===== ChainzExplorer =====\n";
        s += "\t- PROJECT_HOME: [" + PROJECT_HOME + "]\n";
        s += "\t- classpath: [" + classpath + "]\n";
        s += "\t- chainsDirectoryPath: [" + chainsDirectoryPath + "]\n";
        s += "\t- DEBUG: [" + DEBUG + "]\n";
        s += "..........................\n";
        s += "\t- entryExitCouple: [" + entryExitCouple + "]\n";
        s += "\t- maxChainDepth: [" + maxChainDepth + "]\n";
        s += "\t- entryPointPackageName: [" + entryPointPackageName + "]\n";
        s += "\t- entryPointClassName: [" + entryPointClassName + "]\n";
        s += "\t- entryPointMethodName: [" + entryPointMethodName + "]\n";
        s += "\t- exitPointPackageName: [" + exitPointPackageName + "]\n";
        s += "\t- exitPointClassName: [" + exitPointClassName + "]\n";
        s += "\t- exitPointMethodName: [" + exitPointMethodName + "]\n";
        s += "==========================";
        System.out.println("\n" + s);
        System.out.flush();
    }

    private String getDeltaTimeString(long start, long end) {
        long deltaTime = end - start;
        long deltaMillis, deltaSeconds, deltaMinutes, deltaHours, deltaDays;

        deltaMillis = TimeUnit.MILLISECONDS.toMillis(deltaTime) % 1000;
        deltaSeconds = TimeUnit.MILLISECONDS.toSeconds(deltaTime) % 60;
        deltaMinutes = TimeUnit.MILLISECONDS.toMinutes(deltaTime) % 60;
        deltaHours = TimeUnit.MILLISECONDS.toHours(deltaTime) % 24;
        deltaDays = TimeUnit.MILLISECONDS.toDays(deltaTime);

        return deltaDays + ":" + deltaHours + ":" + deltaMinutes + ":" + deltaSeconds + ":" + deltaMillis;
    }

    private static String methodToString(SootMethod method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    private static String methodToStringOut(SootMethod method) {
        return method.getDeclaringClass() + ": " + method.getSubSignature();
    }

    private void visit(int currentChainDepth, String currentMethodPath, String currentMethodPathOut, SootMethod edgeSourceMethod) {

        if (currentMethodPath.endsWith(exitPointClassName + "." + exitPointMethodName)) {
            return;
        }

        Iterator<Edge> edgeIterator = methodsCallGraph.edgesOutOf(edgeSourceMethod);
        SootMethod edgeTargetMethod;
        SootClass edgeTargetMethodClass;
        String currentNewChain, currentNewChainOut;

        while (edgeIterator.hasNext()) {

            if (!canContinueTimeout())
                System.exit(123);

            edgeTargetMethod = edgeIterator.next().tgt().method();
            edgeTargetMethodClass = edgeTargetMethod.method().getDeclaringClass();

            if (applyClassFilter) {
                Type serializableType = Scene.v().getType("java.io.Serializable");
                boolean targetImplementsSerializable = Scene.v().getFastHierarchy().canStoreType(edgeTargetMethod.getDeclaringClass().getType(), serializableType);
                boolean targetIsSuperclassOfSource = Scene.v().getFastHierarchy().canStoreType(edgeSourceMethod.getDeclaringClass().getType(), edgeTargetMethod.getDeclaringClass().getType());
                boolean targetIsExitPoint = exitPointMethodName.equals(edgeTargetMethod.getName());
                if (!targetIsExitPoint && !targetImplementsSerializable && !targetIsSuperclassOfSource) {
                    //System.out.println("NO ARC " + edgeSourceMethod.getDeclaringClass().getType() +" - "+ edgeTargetMethod.getDeclaringClass().getType());
                    continue;
                }
            }

            if (
                    currentChainDepth < maxChainDepth &&
                            !methodToString(edgeTargetMethod).contains("<clinit>") &&
                            !currentMethodPath.contains(methodToString(edgeTargetMethod)) //if not a loop
            ) {

                //currentNewChain = currentMethodPath + " ==> " + methodToString(edgeTargetMethod) + edgeTargetMethod.getParameterTypes();
                currentNewChain = currentMethodPath + " ==> " + methodToString(edgeTargetMethod);
                currentNewChainOut = currentMethodPathOut + " ==> " + methodToStringOut(edgeTargetMethod);


                if (currentNewChain.contains(exitPointClassName + "." + exitPointMethodName) && !chainzStrings.contains(currentNewChain)) {


                    if (maxChainz <= 0) {
                        System.exit(123);
                    } else {

                        //System.out.println(maxChainz + " Stop");
                        maxChainz--;
                    }
                    if (DEBUG) {
                        System.out.println("\t- " + currentNewChainOut);
                        System.out.flush();
                    }

                    try {

                        //System.out.println("\nMAX CHAINZ - > "+maxChainz);
                        bw.append(currentNewChainOut + "\n");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    chainzStrings.add(currentNewChain);

                }

                visit(currentChainDepth + 1, currentNewChain, currentNewChainOut, edgeTargetMethod);
            }

        }

    }

    public void getChainz() {

        /* ADDING A CUSTOM TRANSFORMATION TO THE SOOT ANALYSIS PIPELINE */
        PackManager.v().getPack("wjtp").add(
                new Transform(
                        "wjtp." + entryPointClassName + "." + maxChainDepth,
                        new SceneTransformer() {
                            @Override
                            protected void internalTransform(String phaseName, Map options) {
                                System.out.println(entryPointClassName + "." + maxChainDepth);

                                long start;
                                long end = System.currentTimeMillis();
                                String deltaTimeCGCreation = getDeltaTimeString(CGCreationStart, end);

                                CHATransformer.v().transform();

                                if (DEBUG) {
                                    System.out.println("\n************************ STARTED ************************");
                                    System.out.flush();

                                    //printConfig();

                                    System.out.println("\n[i] ENTRY AND EXIT POINTS: ");
                                    System.out.println("\t- ENTRY POINT: " + entryPoint + "\n\t- EXIT  POINT: " + exitPoint);
                                    System.out.flush();
                                }

                                /* CREATING METHODS CALL GRAPH */
                                methodsCallGraph = Scene.v().getCallGraph();

                                if (DEBUG) {
                                    System.out.println("\n[+] CREATED METHODS CALL GRAPH \n\t- IN [DD:HH:MM:SS:MM]: " + deltaTimeCGCreation + "\n\t- Graph Size: " + methodsCallGraph.size() + " Edges\n\t- Graph Generation Entry Point: " + Scene.v().getEntryPoints().get(0));
                                    System.out.flush();
                                }
                                /*=============================*/


                                /* VISITING THE METHODS CALL GRAPH */
                                if (DEBUG) {
                                    System.out.println("\n[i] STARTING CALL GRAPH ANALYSIS " + "@ MAX DEPTH [" + maxChainDepth + "]");
                                    System.out.flush();
                                }

                                SootClass entryPointClass = Scene.v().getSootClass(entryPointPackageName + "." + entryPointClassName);
                                SootMethod entryPointMethod = entryPointClass.getMethodByName(entryPointMethodName);

                                Set<String> chains = new HashSet<String>();

                                if (DEBUG) {
                                    System.out.println("\n[+] CHAINS FOUND");
                                    System.out.flush();
                                }

                                try {
                                    bw.write("CLASSPATH:" + classpath + "\n");
                                    bw.flush();

                                    start = System.currentTimeMillis();
                                    if (timestart == null)
                                        timestart = getCurrentTimestamp();
                                    visit(0, methodToString(entryPointMethod), methodToStringOut(entryPointMethod), entryPointMethod);
                                    end = System.currentTimeMillis();

                                    bw.append("FINISHED\n");
                                    bw.flush();
                                    bw.close();

                                    if (DEBUG) {
                                        System.out.println("\n[+] EXPLORED METHODS CALL GRAPH IN [DD:HH:MM:SS:MM]: " + getDeltaTimeString(start, end));
                                        System.out.println("\n************************* ENDED *************************");
                                        System.out.flush();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                /*=============================*/

                            }
                        }
                )
        );

        SootClass c = Scene.v().forceResolve(entryPointPackageName + "." + entryPointClassName, SootClass.BODIES);
        c.setApplicationClass();
        Scene.v().loadNecessaryClasses();
        SootMethod method = c.getMethodByName(entryPointMethodName);
        List entryPoints = new ArrayList();
        entryPoints.add(method);
        Scene.v().setEntryPoints(entryPoints);

    }

    private LocalDateTime getCurrentTimestamp() {
        return (LocalDateTime.now());
    }

    private long getDifferenceSecond() {
        return (timestart.until(getCurrentTimestamp(), ChronoUnit.SECONDS));
    }

    private long getDifferenceMinute() {
        return (timestart.until(getCurrentTimestamp(), ChronoUnit.MINUTES));
    }

    private boolean canContinueTimeout() {

        if (timeout <= 0)
            return true;

        //Return true if current timestamp is less then timeout time
        return (getDifferenceSecond() <= timeout);
    }

}
