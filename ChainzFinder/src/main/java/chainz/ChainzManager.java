package chainz;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.time.LocalDateTime;
import java.util.*;

public class ChainzManager {
    public static Boolean DEBUG = true;

    public static String PROJECT_HOME = "";
    public static String SYSTEM_USER = System.getenv("USER");

    public static String JAVA_HOME = System.getenv("JAVA_HOME");

    public static String classpath;

    public static String targetJarPath;
    public static String chainsDirectoryPath = "/ChainzFinder/output/chains/";

    public static long CGCreationStart;

    public static int timeout;
    public static LocalDateTime timestart;

    public static void main(String[] argv) {

        if (argv.length < 7){
            System.out.println("[ERROR] Missing arguments:\n\tUsage: java -jar ChainzFinder.jar " +
                    "<TargetJarPath> <EntryPointMethod>:<ExitPointMethod> " +
                    "<MaxChainDepth> " +
                    "<ApplyClassFilter true/false> " +
                    "<projectHome> <MaxChainz> " +
                    "<Timeout Min (0 for infinite)>");
            System.out.flush();
            System.exit(1);
        }

        /* SETTING PROJECT HOME DIRECTORY PATH */
        PROJECT_HOME = argv[4];
        if (DEBUG) {
            System.out.println("\n[i] SYSTEM USER : " + SYSTEM_USER + "\n[i] PROJECT_HOME: " + PROJECT_HOME);
            System.out.flush();
        }
        /*======================================*/


        int maxChainz = Integer.parseInt(argv[5]);
        timeout = Integer.parseInt(argv[6]);
        timestart = getCurrentTimestamp();

        ArrayList<String> tmpSootArgs = new ArrayList<String>(Arrays.asList(new String[]{
                "-w",
                "-include-all",
                "-full-resolver",
                "-allow-phantom-refs",
        }));

        /* SETTING SOOT CLASSPATH */
        classpath = JAVA_HOME + "/jre/lib/rt.jar" + ":" + JAVA_HOME + "/jre/lib/jce.jar";

        targetJarPath = argv[0];
        if(!targetJarPath.equals("")) {
            classpath += ":" + targetJarPath;
            tmpSootArgs.add("-process-dir");
            tmpSootArgs.add(targetJarPath);
        }

        if (DEBUG) {
            System.out.println("\n[i] CLASSPATH: ");
            for (String s : classpath.split(":")) {
                System.out.println("\t- " + s);
            }
            System.out.flush();
        }

        Options.v().set_soot_classpath(classpath);
        /*=========================*/

        /* SETTING SOOT ARGUMENTS LIST */
        List<String> sootArgv = new ArrayList<String>();
        sootArgv.addAll( tmpSootArgs );
        Options.v().parse(sootArgv.toArray(new String[0]));
        Scene.v().loadNecessaryClasses();

        /*=============================*/

        /* SETTING ENTRY AND EXIT POINTS */
        try {

            String entryExitPointsString = argv[1];;
            int entryExitMaxDepth = Integer.valueOf(argv[2]);
            Boolean applyClassFilter = Boolean.valueOf(argv[3]);

            if (DEBUG) {
                System.out.println("\n[i] ADDED TRANSFORMATION\n\t- " + entryExitPointsString);
                System.out.println("\n[i] MAX CHAIN DEPTH: " + entryExitMaxDepth);
                System.out.println("\n[i] CLASS FILTER: " + applyClassFilter);
                System.out.flush();
            }

            ChainzExplorer ce = new ChainzExplorer(targetJarPath, entryExitPointsString, entryExitMaxDepth, applyClassFilter, maxChainz, timeout);
            ce.getChainz();

            CGCreationStart = System.currentTimeMillis();
            PackManager.v().runPacks();

            System.out.flush();

        } catch(Exception e) { e.printStackTrace(); }
        /*========================*/

    }
    private static LocalDateTime getCurrentTimestamp(){
        return(LocalDateTime.now());
    }

}
