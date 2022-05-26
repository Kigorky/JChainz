package it.unimi.di.laser.jchainz;

import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class DataDependencyAnalysis extends ForwardFlowAnalysis {

    private ArraySparseSet<DataDependencyNode> emptySet = new ArraySparseSet<DataDependencyNode>();

    private final Logger logger = Logger.getLogger(DataDependencyAnalysis.class);
    private final boolean DEBUG = logger.isDebugEnabled();

    private DataDependencyGraph ddg;

    private SootMethod currentSootMethod;
    private SootMethod nextSootMethod;
    private Body nextMethodBody;

    public DataDependencyAnalysis(DirectedGraph g, SootMethod currentSootMethod, SootMethod nextSootMethod, Body nextMethodBody, DataDependencyGraph interMethodDDG) {
        super(g);

        this.currentSootMethod = currentSootMethod;
        this.nextSootMethod = nextSootMethod;
        this.nextMethodBody = nextMethodBody;
        this.ddg = interMethodDDG;

        logger.setLevel(Level.INFO);

        doAnalysis();
    }

    @Override
    protected void merge(Object in1, Object in2, Object out) {
        FlowSet inSet1 = (FlowSet) in1,
                inSet2 = (FlowSet) in2,
                outSet = (FlowSet) out;
        inSet1.union(inSet2, outSet);
    }

    @Override
    protected void copy(Object source, Object dest) {
        FlowSet srcSet = (FlowSet) source,
                destSet = (FlowSet) dest;
        srcSet.copy(destSet);
    }

    @Override
    protected Object newInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected Object entryInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void flowThrough(Object in, Object node, Object out) {
        FlowSet<DataDependencyNode> inSet = (FlowSet<DataDependencyNode>) in;
        FlowSet<DataDependencyNode> outSet = (FlowSet<DataDependencyNode>) out;

        FlowSet<DataDependencyNode> killSet = emptySet.clone();
        FlowSet<DataDependencyNode> genSet = emptySet.clone();

        boolean isInvoke = false;
        Unit currentUnit = (Unit) node;
        if (currentUnit instanceof Stmt) {
            Stmt currentStatement = (Stmt) currentUnit;

            if (currentStatement.containsInvokeExpr()) {
                InvokeExpr invokeExpression = currentStatement.getInvokeExpr();
                if (isSuperMethod(invokeExpression.getMethodRef().resolve(), nextSootMethod)) {
                    isInvoke = true;
                    int totalArgs = invokeExpression.getArgCount();

                    for (int i = 0; i < totalArgs; i++) {
                        Value v = invokeExpression.getArg(i);
                        UnitPatchingChain unitPatchingChain = nextMethodBody.getUnits();
                        for (Unit u : unitPatchingChain) {
                            if (u.toString().contains("parameter" + i)) {
                                for (ValueBox valueBox : u.getUseBoxes()) {
                                    if (valueBox.getValue().toString().contains("parameter" + i)) {
                                        DataDependencyNode DDNDependent = new DataDependencyNode(currentSootMethod, v, currentUnit);
                                        DataDependencyNode DDNSource = new DataDependencyNode(nextSootMethod, valueBox.getValue(), u);

                                        if (!DDNDependent.equals(DDNSource)) {
                                            if (!ddg.containsNode(DDNDependent)) ddg.addNode(DDNDependent);
                                            if (!ddg.containsNode(DDNSource)) ddg.addNode(DDNSource);
                                            if (!ddg.containsEdge(DDNSource, DDNDependent, DataDependencyGraph.INTER_METHOD_PARAMETER_EDGE))
                                                if (!ddg.getRecursivePredecessorsOf(DDNSource).contains(DDNDependent)) {
                                                    ddg.addEdge(DDNSource, DDNDependent, DataDependencyGraph.INTER_METHOD_PARAMETER_EDGE);
                                                    logger.debug("InterMethodLink formation (black) - Source: " + v.toString() + " | Target: " + valueBox.getValue().toString());

                                                    Type sourceType = Scene.v().getType(DDNSource.getMethod().toString().split("<")[1].split(":")[0]);

                                                    String[] parts = DDNDependent.getUnit().toString().split("[.]")[0].split(" ");
                                                    String dependentValueString = parts[parts.length - 1];

                                                    logger.debug("SOURCEVALUE " + dependentValueString);

                                                    DDNSource.addInterMethodLink(new InterMethodLink.Black(DDNDependent, valueBox.getValue(), v, sourceType, dependentValueString));
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    String valueString = invokeExpression.toString().split("([ ]|[.])")[1];
                    Value targetObjectReference = null;
                    for (ValueBox valueBox : currentUnit.getUseBoxes()) {
                        if (valueBox.getValue().toString().equals(valueString)) {
                            targetObjectReference = valueBox.getValue();
                            break;
                        }
                    }


                    DataDependencyNode DDNSource = new DataDependencyNode(currentSootMethod, targetObjectReference, currentUnit);
                    boolean hasThisUnit = true;
                    try {
                        nextMethodBody.getThisUnit();
                    } catch (RuntimeException e) {
                        hasThisUnit = false;
                    }

                    if (targetObjectReference != null && hasThisUnit) {
                        for (ValueBox valueBox : nextMethodBody.getThisUnit().getUseBoxes()) {
                            if (valueBox.getValue().toString().startsWith("@this")) {
                                DataDependencyNode DDNTarget = new DataDependencyNode(nextSootMethod, valueBox.getValue(), nextMethodBody.getThisUnit());

                                if (!DDNSource.equals(DDNTarget)) {
                                    if (!ddg.containsNode(DDNSource)) ddg.addNode(DDNSource);
                                    if (!ddg.containsNode(DDNTarget)) ddg.addNode(DDNTarget);
                                    if (!ddg.containsEdge(DDNTarget, DDNSource, DataDependencyGraph.INTER_METHOD_OBJECT_REFERENCE_EDGE))
                                        if (!ddg.getRecursivePredecessorsOf(DDNTarget).contains(DDNSource)) {
                                            ddg.addEdge(DDNTarget, DDNSource, DataDependencyGraph.INTER_METHOD_OBJECT_REFERENCE_EDGE);
                                            logger.debug("InterMethodLink formation (green) - Source: " + DDNTarget.getValue().toString() + " | Target: " + DDNSource.getValue().toString());
                                            DDNTarget.addInterMethodLink(new InterMethodLink.Green(DDNSource, DDNTarget.getValue(), DDNSource.getValue(), DDNSource.getValue().getType()));
                                        }
                                }

                            }
                        }
                    }
                }
            }
            if (currentStatement.containsFieldRef()) {
                if (DEBUG) logger.debug("FIELD " + currentStatement.getFieldRef().toString());
            }
        }

        Iterator<DataDependencyNode> inSetIterator;

        for (ValueBox defBox : currentUnit.getDefBoxes()) {
            inSetIterator = inSet.iterator();

            while (inSetIterator.hasNext()) {

                DataDependencyNode inSetDataDependencyNode = inSetIterator.next();

                if (inSetDataDependencyNode.getValue().equivTo(defBox.getValue())) {
                    logger.debug("KILLING " + inSetDataDependencyNode);
                    killSet.add(inSetDataDependencyNode);
                }
            }

            logger.debug("GEN " + defBox);
            genSet.add(new DataDependencyNode(currentSootMethod, defBox.getValue(), currentUnit));
        }

        for (ValueBox useBox : currentUnit.getUseBoxes()) {
            logger.debug("USE " + useBox);
            DataDependencyNode useDataDependencyNode = new DataDependencyNode(currentSootMethod, useBox.getValue(), currentUnit);

            inSetIterator = inSet.iterator();

            while (inSetIterator.hasNext()) {
                DataDependencyNode inSetDataDependencyNode = inSetIterator.next();

                if (inSetDataDependencyNode.getValue().equivTo(useBox.getValue())) {
                    boolean inKillSet = false;
                    Iterator<DataDependencyNode> killSetIterator = killSet.iterator();
                    while (killSetIterator.hasNext()) {
                        DataDependencyNode killSetDataDependencyNode = killSetIterator.next();
                        if (killSetDataDependencyNode.getValue().equivTo(useBox.getValue())) {
                            inKillSet = true;
                        }
                    }

                    if (!inKillSet) {
                        if (!useDataDependencyNode.equals(inSetDataDependencyNode)) {
                            if (!ddg.containsNode(inSetDataDependencyNode)) ddg.addNode(inSetDataDependencyNode);
                            if (!ddg.containsNode(useDataDependencyNode)) ddg.addNode(useDataDependencyNode);
                            if (!ddg.containsEdge(useDataDependencyNode, inSetDataDependencyNode, DataDependencyGraph.REACHING_DEFINITIONS_SAME_VAR_EDGE)) {
                                if (!ddg.getRecursivePredecessorsOf(useDataDependencyNode).contains(inSetDataDependencyNode)) {
                                    ddg.addEdge(useDataDependencyNode, inSetDataDependencyNode, DataDependencyGraph.REACHING_DEFINITIONS_SAME_VAR_EDGE);
                                }
                            }
                        }
                    }
                } else if (currentUnit.toString().contains(inSetDataDependencyNode.getValue().toString())) {
                    boolean inKillSet = false;
                    Iterator<DataDependencyNode> killSetIterator = killSet.iterator();
                    while (killSetIterator.hasNext()) {
                        DataDependencyNode killSetDataDependencyNode = killSetIterator.next();
                        if (killSetDataDependencyNode.getValue().equivTo(useBox.getValue())) {
                            inKillSet = true;
                        }
                    }

                    if (!inKillSet) {
                        if (!useDataDependencyNode.equals(inSetDataDependencyNode)) {
                            if (!ddg.containsNode(inSetDataDependencyNode)) ddg.addNode(inSetDataDependencyNode);
                            if (!ddg.containsNode(useDataDependencyNode)) ddg.addNode(useDataDependencyNode);
                            if (!ddg.containsEdge(useDataDependencyNode, inSetDataDependencyNode, DataDependencyGraph.REACHING_DEFINITIONS_OTHER_VAR_EDGE)) {
                                if (!ddg.getRecursivePredecessorsOf(useDataDependencyNode).contains(inSetDataDependencyNode)) {
                                    ddg.addEdge(useDataDependencyNode, inSetDataDependencyNode, DataDependencyGraph.REACHING_DEFINITIONS_OTHER_VAR_EDGE);
                                }
                            }
                        }
                    }
                }
            }

            for (DataDependencyNode genDataDependencyNode : genSet) {
                if (!genDataDependencyNode.equals(useDataDependencyNode)) {
                    if (!ddg.containsNode(genDataDependencyNode)) ddg.addNode(genDataDependencyNode);
                    if (!ddg.containsNode(useDataDependencyNode)) ddg.addNode(useDataDependencyNode);
                    if (!ddg.containsEdge(genDataDependencyNode, useDataDependencyNode, DataDependencyGraph.DEF_DEPENDS_ON_USE_EDGE)) {
                        if (!ddg.getRecursivePredecessorsOf(genDataDependencyNode).contains(useDataDependencyNode)) {
                            ddg.addEdge(genDataDependencyNode, useDataDependencyNode, DataDependencyGraph.DEF_DEPENDS_ON_USE_EDGE);
                        }
                    }
                }
            }
        }

        inSet.difference(killSet, outSet);
        outSet.union(genSet);
    }

    public void printDDG() {
        Iterator<DataDependencyNode> iterator = ddg.iterator();
        while (iterator.hasNext()) {
            StringBuilder stringBuilder = new StringBuilder();
            DataDependencyNode dataDependencyNode = iterator.next();
            stringBuilder.append(dataDependencyNode);
            if (ddg.getSuccsOf(dataDependencyNode).size() > 0) {
                stringBuilder.append(" ==> ");
                Iterator<DataDependencyNode> pairIterator = ddg.getSuccsOf(dataDependencyNode).iterator();
                while (pairIterator.hasNext()) {
                    stringBuilder.append(pairIterator.next());
                    if (pairIterator.hasNext()) {
                        stringBuilder.append(" -> ");
                    }
                }
            } else {
                stringBuilder.append(" ==> {}");
            }
            logger.debug(stringBuilder.toString());
        }
    }

    public boolean isSuperMethod(SootMethod candidateSuperMethod, SootMethod candidateSubMethod) {
        if (candidateSubMethod == null) return false;

        SootClass candidateSubClass = candidateSubMethod.getDeclaringClass();
        SootClass candidateSuperClass = candidateSuperMethod.getDeclaringClass();

        if (candidateSuperClass.isInterface()) {
            if (Scene.v().getFastHierarchy().canStoreClass(candidateSubClass, candidateSuperClass)) {
                if (DEBUG) logger.debug(candidateSubClass + " implements " + candidateSuperClass);
                return true;
            } else {
                if (DEBUG) logger.debug(candidateSubClass + " does not implement " + candidateSuperClass);
                return false;
            }
        }

        if (Scene.v().getFastHierarchy().isSubclass(candidateSubClass, candidateSuperClass)) {
            if (DEBUG) logger.debug(candidateSuperClass + " is a superclass of " + candidateSubClass);
            return true;
        } else {
            if (DEBUG) logger.debug(candidateSuperClass + " is not a superclass of " + candidateSubClass);
            return false;
        }
    }
}

