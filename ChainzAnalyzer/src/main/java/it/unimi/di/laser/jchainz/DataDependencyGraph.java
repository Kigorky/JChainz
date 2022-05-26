package it.unimi.di.laser.jchainz;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.HashMutableEdgeLabelledDirectedGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DataDependencyGraph extends HashMutableEdgeLabelledDirectedGraph<DataDependencyNode, String> {

    private final Logger logger = Logger.getLogger(DataDependencyGraph.class);
    public boolean DEBUG = logger.isDebugEnabled();
    public static String REACHING_DEFINITIONS_SAME_VAR_EDGE = Util.getProp("REACHING_DEFINITIONS_SAME_VAR_EDGE");
    public static String DEF_DEPENDS_ON_USE_EDGE = Util.getProp("DEF_DEPENDS_ON_USE_EDGE");
    public static String INTER_METHOD_PARAMETER_EDGE = Util.getProp("INTER_METHOD_PARAMETER_EDGE");
    public static String INTER_METHOD_OBJECT_REFERENCE_EDGE = Util.getProp("INTER_METHOD_OBJECT_REFERENCE_EDGE");
    public static String REACHING_DEFINITIONS_OTHER_VAR_EDGE = Util.getProp("REACHING_DEFINITIONS_OTHER_VAR_EDGE");
    private String[] labels = {REACHING_DEFINITIONS_SAME_VAR_EDGE, DEF_DEPENDS_ON_USE_EDGE, INTER_METHOD_PARAMETER_EDGE, INTER_METHOD_OBJECT_REFERENCE_EDGE, REACHING_DEFINITIONS_OTHER_VAR_EDGE};

    private DataDependencyNode found;

    private SootMethod entryMethod;
    private SootMethod exitMethod;

    public DataDependencyGraph() {
        logger.setLevel(Level.INFO);
    }

    public void setEntryMethod(SootMethod entryMethod) {
        this.entryMethod = entryMethod;
    }

    public void setExitMethod(SootMethod exitMethod) {
        this.exitMethod = exitMethod;
    }

    /**
     * Determines if a path exists between two nodes using a depth-first search
     *
     * @param from The starting node
     * @param to   The target node
     * @return Whether the path exists
     */
    public boolean containsPath(DataDependencyNode from, DataDependencyNode to) {
        Set<DataDependencyNode> visited = new HashSet<>();
        this.found = null;

        dfsContainsPath(from, to, visited);

        return (this.found != null);
    }

    private void dfsContainsPath(DataDependencyNode from, DataDependencyNode to, Set<DataDependencyNode> visited) {

        List<DataDependencyNode> successors = this.getSuccsOf(from);

        if (!successors.isEmpty()) {
            for (DataDependencyNode dataDependencyNode : successors) {
                if (!visited.contains(dataDependencyNode)) {
                    visited.add(dataDependencyNode);
                    if (dataDependencyNode.equals(to)) {
                        this.found = to;
                        return;
                    }
                    dfsContainsPath(dataDependencyNode, to, visited);
                }
            }
        }
    }

    public Set<DataDependencyNode> getRecursiveSuccessorsOf(DataDependencyNode dataDependencyNode) {
        Set<DataDependencyNode> visited = new HashSet<DataDependencyNode>();
        dfsRecursiveSuccessorsOf(dataDependencyNode, visited);
        return visited;
    }

    private void dfsRecursiveSuccessorsOf(DataDependencyNode from, Set<DataDependencyNode> visited) {

        List<DataDependencyNode> successors = this.getSuccsOf(from);

        if (!successors.isEmpty()) {
            for (DataDependencyNode dataDependencyNode : successors) {
                if (!visited.contains(dataDependencyNode)) {
                    visited.add(dataDependencyNode);
                    dfsRecursiveSuccessorsOf(dataDependencyNode, visited);
                }
            }
        }
    }

    public Set<DataDependencyNode> getRecursivePredecessorsOf(DataDependencyNode dataDependencyNode) {
        Set<DataDependencyNode> visited = new HashSet<DataDependencyNode>();
        dfsRecursivePredecessorsOf(dataDependencyNode, visited);
        return visited;
    }

    private void dfsRecursivePredecessorsOf(DataDependencyNode from, Set<DataDependencyNode> visited) {

        List<DataDependencyNode> predecessors = this.getPredsOf(from);

        if (!predecessors.isEmpty()) {
            for (DataDependencyNode dataDependencyNode : predecessors) {
                if (!visited.contains(dataDependencyNode)) {
                    visited.add(dataDependencyNode);
                    dfsRecursivePredecessorsOf(dataDependencyNode, visited);
                }
            }
        }
    }

    public void toDOTFile(String filename) {

        int nextID = 0;
        Map<DataDependencyNode, Integer> idMap = new HashMap<>();
        List<String> edges = new ArrayList<>();

        for (String label : labels) {
            Iterator<DataDependencyNode> iterator = this.getEdgesForLabel(label).iterator();
            while (iterator.hasNext()) {
                DataDependencyNode dataDependencyNode = iterator.next();
                if (!idMap.containsKey(dataDependencyNode)) idMap.put(dataDependencyNode, nextID++);
                for (DataDependencyNode succ : this.getEdgesForLabel(label).getSuccsOf(dataDependencyNode)) {
                    if (!idMap.containsKey(succ)) idMap.put(succ, nextID++);
                    edges.add("  " + "n" + idMap.get(dataDependencyNode) + " -> " + "n" + idMap.get(succ) + " " + label + ";");
                }
            }
        }

        try {
            FileWriter fileWriter = new FileWriter(filename);

            fileWriter.write("digraph G {");
            fileWriter.write("\n");
            for (DataDependencyNode node : idMap.keySet()) {
                int numberOfSuccessors = this.getSuccsOf(node).size();
                int numberOfPredecessors = this.getPredsOf(node).size();
                if (numberOfSuccessors == 0) {
                    fileWriter.write("  " + "n" + idMap.get(node) + "[label=" + node.toDotLabel() + ", color=\"" + Util.getProp("successorColor") + "\"];");
                } else if (numberOfPredecessors == 0) {
                    fileWriter.write("  " + "n" + idMap.get(node) + "[label=" + node.toDotLabel() + ", color=\"" + Util.getProp("predecessorColor") + "\"];");
                } else {
                    fileWriter.write("  " + "n" + idMap.get(node) + "[label=" + node.toDotLabel() + "];");
                }
                fileWriter.write("\n");
            }

            fileWriter.write("\n");
            for (String edge : edges) {
                fileWriter.write(edge);
                fileWriter.write("\n");
            }


            fileWriter.write("}");
            fileWriter.write("\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Starts the type analysis. Each node N with value V without dependencies
     * for V is initialized, by allowing type V.getType() for V.
     */
    public void typeAnalysisInit() {
        Iterator<DataDependencyNode> iterator = this.iterator();
        while (iterator.hasNext()) {
            DataDependencyNode node = iterator.next();
            if (this.getSameVarSuccessors(node).size() == 0) {
                node.addAllowedType(node.getValue().toString(), node.getValue().getType());
            }
        }
    }

    public List<DataDependencyNode> getSameVarSuccessors(DataDependencyNode node) {
        List<DataDependencyNode> sameVarSuccessors = new ArrayList<>();
        for(DataDependencyNode successor : this.getSuccsOf(node)) {
            if(!this.containsEdge(node, successor, REACHING_DEFINITIONS_OTHER_VAR_EDGE)) {
                sameVarSuccessors.add(successor);
            }
        }
        return sameVarSuccessors;
    }

    public void typeAnalysisPropagate() {

        Iterator<DataDependencyNode> iterator;

        boolean repeat = true;
        while (repeat) {
            repeat = false;
            List<DataDependencyNode> nodesToProcess = new ArrayList<>();

            iterator = this.iterator();
            while (iterator.hasNext()) {
                DataDependencyNode node = iterator.next();

                if (node.getAllowedTypes() == null) {

                    boolean nodeToProcess = true;
                    for (DataDependencyNode successor : this.getSuccsOf(node)) {
                        if (successor.getAllowedTypes() == null) nodeToProcess = false;
                    }

                    if (nodeToProcess) {
                        nodesToProcess.add(node);
                        repeat = true;
                    }
                }
            }

            for (DataDependencyNode node : nodesToProcess) {
                node.initializeAllowedTypesList();

                List<DataDependencyNode> successors = this.getSuccsOf(node);

                boolean process = true;

                for (DataDependencyNode successor : successors) {
                    if (successor.getAllowedTypes() == null) process = false;
                }

                if (process) {
                    for (DataDependencyNode successor : successors) {
                        boolean processSuccessor = true;
                        for (InterMethodLink interMethodLink : node.getInterMethodLinks()) {
                            if(successor.equals(interMethodLink.getDependentNode())) {
                                processSuccessor = false;
                                break;
                            }
                        }

                        if(processSuccessor) {
                            for (String string : successor.getAllowedTypes().keySet()) {
                                node.getAllowedTypes().init(string);
                                for (Type type : successor.getAllowedTypes().get(string)) {
                                    if (!string.equals(node.getValue().toString())) {
                                        node.addAllowedType(string, type);
                                    } else {
                                        Type currentType = node.getValue().getType();
                                        if (Scene.v().getFastHierarchy().canStoreType(type, currentType)) {
                                            if (Scene.v().getFastHierarchy().canStoreType(type, currentType)) {
                                                node.addAllowedType(string, type);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (InterMethodLink interMethodLink : node.getInterMethodLinks()) {
                            //logger.info("Target: " + interMethodLink.getTargetValue().toString());
                            for (Type type : interMethodLink.getDependentNode().getAllowedTypes().get(interMethodLink.getDependentValue().toString())) {
                                node.addAllowedType(interMethodLink.getSourceValue().toString(), type);
                            }
                        }
                        if (node.getUnit() instanceof DefinitionStmt) {
                            DefinitionStmt definitionStmt = (DefinitionStmt) node.getUnit();
                            logger.debug("DEFINITION: " + definitionStmt);
                            for (Type type : node.getAllowedTypes().get(definitionStmt.getRightOp().toString())) {
                                Type currentType = node.getValue().getType();
                                if (Scene.v().getFastHierarchy().canStoreType(type, currentType)) {
                                    node.addAllowedType(definitionStmt.getLeftOp().toString(), type);
                                }
                            }
                        } else if (node.getUnit() instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) node.getUnit();
                            logger.debug("ASSIGN: " + assignStmt);
                            for (Type type : node.getAllowedTypes().get(assignStmt.getRightOp().toString())) {
                                node.addAllowedType(assignStmt.getLeftOp().toString(), type);
                            }
                        } else if (node.getUnit() instanceof JInvokeStmt) {
                            logger.debug("INVOKE: " + node.getUnit() + " - " + node.getUnit().getClass().getName());
                        }

                    }
                }
            }
        }
    }

    /**
     * Finds and removes nodes without allowed types.
     * For a node N with value V, if the allowed types for V at N are none,
     * each edge pointing to N is removed from the graph.
     */
    public void typeAnalysisReduce() {
        Iterator<DataDependencyNode> iterator = this.iterator();
        List<DataDependencyNode> emptyNodes = new ArrayList<>();
        while (iterator.hasNext()) {
            DataDependencyNode node = iterator.next();
            if (node.getAllowedTypes().get(node.getValue().toString()).size() == 0) {
                emptyNodes.add(node);
            }
            else if (node.getInterMethodLinks().size() > 0) {
                for(InterMethodLink interMethodLink : node.getInterMethodLinks()) {
                    if(interMethodLink instanceof InterMethodLink.Green) {
                        Type invokedObjectType = ((InterMethodLink.Green)interMethodLink).getInvokedObjectType();
                        Type currentType = node.getValue().getType();
                        if (!Scene.v().getFastHierarchy().canStoreType(invokedObjectType, currentType)) {
                            //logger.info(currentType + " cannot hold " + invokedObjectType);
                            emptyNodes.add(interMethodLink.getDependentNode());
                        }
                        else {
                            //logger.info(currentType + " can hold " + invokedObjectType);
                        }
                    }
                    if(interMethodLink instanceof InterMethodLink.Black) {
                        InterMethodLink.Black black = (InterMethodLink.Black) interMethodLink;
                        boolean toRemove = true;
                        for(Type type : black.getDependentNode().getAllowedTypes().get(black.getDependentValueString())) {
                            if(Scene.v().getFastHierarchy().canStoreType(black.getSourceType(), type)) {
                                toRemove = false;
                            }
                        }
                        if(toRemove) emptyNodes.add(black.getDependentNode());
                    }
                }
            }
        }

        for (DataDependencyNode emptyNode : emptyNodes) {
            List<DataDependencyNode> predecessors = new ArrayList<>();
            for (DataDependencyNode predecessor : this.getPredsOf(emptyNode)) {
                predecessors.add(predecessor);
            }

            for (DataDependencyNode predecessor : predecessors) {
                this.removeAllEdges(predecessor, emptyNode);
            }
        }

    }

    public DataDependencyGraph copy() {
        DataDependencyGraph copy = new DataDependencyGraph();

        for (String label : labels) {
            Iterator<DataDependencyNode> iterator = this.getEdgesForLabel(label).iterator();
            while (iterator.hasNext()) {
                DataDependencyNode dataDependencyNode = iterator.next();
                for (DataDependencyNode succ : this.getEdgesForLabel(label).getSuccsOf(dataDependencyNode)) {
                    if (!copy.containsNode(dataDependencyNode)) copy.addNode(dataDependencyNode);
                    if (!copy.containsNode(succ)) copy.addNode(succ);
                    if (!copy.containsEdge(dataDependencyNode, succ, label))
                        copy.addEdge(dataDependencyNode, succ, label);
                }
            }
        }

        copy.setEntryMethod(entryMethod);
        copy.setExitMethod(exitMethod);

        return copy;
    }

    /**
     * Recursively removes all nodes without predecessors along with all edges containing them,
     * except for the exit point
     *
     * @return the modified DataDependencyGraph
     */
    public DataDependencyGraph reduce() {
        boolean repeat = true;
        while (repeat) {
            repeat = false;
            Iterator<DataDependencyNode> iterator = this.iterator();
            List<DataDependencyNode> toRemove = new ArrayList<>();
            while (iterator.hasNext()) {
                DataDependencyNode dataDependencyNode = iterator.next();
                if (!(dataDependencyNode.getUnit().toString().contains("invoke") && dataDependencyNode.getUnit().toString().contains(exitMethod.getSubSignature())) && this.getPredsOf(dataDependencyNode).isEmpty()) {
                    if (!toRemove.contains(dataDependencyNode)) {
                        toRemove.add(dataDependencyNode);
                    }
                }
            }

            for (DataDependencyNode node : toRemove) {
                this.removeNode(node);
                repeat = true;
            }
        }
        return this;
    }

    public void printNumberOfEdges() {
        logger.debug("DDG has " + this.edgeToLabels.keySet().size() + " edges");
    }

    public List<DataDependencyNode> getEntryPoints() {
        List<DataDependencyNode> entryPoints = new ArrayList();

        Iterator<DataDependencyNode> iterator = this.iterator();
        while (iterator.hasNext()) {
            DataDependencyNode node = iterator.next();
            if (node.getMethod().toString().equals(entryMethod.toString()) && this.getSuccsOf(node).size() == 0) {
                entryPoints.add(node);
            }
        }

        return entryPoints;
    }


    public List<DataDependencyNode> getExitPoints() {
        List<DataDependencyNode> exitPoints = new ArrayList();

        Iterator<DataDependencyNode> iterator = this.iterator();
        while (iterator.hasNext()) {
            DataDependencyNode node = iterator.next();
            if (node.getUnit().toString().contains("invoke") && node.getUnit().toString().contains(exitMethod.getSubSignature()) && this.getPredsOf(node).size() == 0) {
                exitPoints.add(node);
            }
        }

        return exitPoints;
    }
}
