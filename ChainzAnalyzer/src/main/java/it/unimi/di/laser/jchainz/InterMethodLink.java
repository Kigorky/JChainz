package it.unimi.di.laser.jchainz;

import soot.Type;
import soot.Value;

public class InterMethodLink {

    private DataDependencyNode dependentNode;
    private Value sourceValue;
    private Value dependentValue;

    public InterMethodLink(DataDependencyNode target, Value sourceValue, Value targetValue) {
        this.dependentNode = target;
        this.sourceValue = sourceValue;
        this.dependentValue = targetValue;
    }

    public DataDependencyNode getDependentNode() {
        return dependentNode;
    }

    public Value getSourceValue() {
        return sourceValue;
    }

    public Value getDependentValue() {
        return dependentValue;
    }

    public static class Green extends InterMethodLink {
        private Type invokedObjectType;

        public Green(DataDependencyNode target, Value sourceValue, Value targetValue, Type invokedObjectType) {
            super(target, sourceValue, targetValue);
            this.invokedObjectType = invokedObjectType;
        }

        public Type getInvokedObjectType() {
            return invokedObjectType;
        }
    }

    public static class Black extends InterMethodLink {
        public Type getSourceType() {
            return sourceType;
        }

        private Type sourceType;

        public String getDependentValueString() {
            return dependentValueString;
        }

        private String dependentValueString;

        public Black(DataDependencyNode target, Value sourceValue, Value targetValue, Type sourceType, String dependentValueString) {
            super(target, sourceValue, targetValue);
            this.sourceType = sourceType;
            this.dependentValueString = dependentValueString;
        }
    }
}
