package it.unimi.di.laser.jchainz;

import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataDependencyNode {

    private static Map<DataDependencyNode, TypeDictionary> allowedTypes = new HashMap<>();
    private static Map<DataDependencyNode, List<InterMethodLink>> interMethodLinks = new HashMap<>();

    private SootMethod method;
    private Value value;
    private Unit unit;

    public void addInterMethodLink(InterMethodLink interMethodLink) {
        if (interMethodLinks.get(this) == null) interMethodLinks.put(this, new ArrayList<InterMethodLink>());
        interMethodLinks.get(this).add(interMethodLink);
    }

    public List<InterMethodLink> getInterMethodLinks() {
        if (interMethodLinks.get(this) == null) interMethodLinks.put(this, new ArrayList<InterMethodLink>());
        return interMethodLinks.get(this);
    }

    public void initializeAllowedTypesList() {
        allowedTypes.put(this, new TypeDictionary());
        allowedTypes.get(this).init(this.value.toString());
    }

    public void addAllowedType(String value, Type type) {
        if (allowedTypes.get(this) == null) allowedTypes.put(this, new TypeDictionary());
        allowedTypes.get(this).add(value, type);
    }

    public TypeDictionary getAllowedTypes() {
        return allowedTypes.get(this);
    }

    public DataDependencyNode(SootMethod method, Value value, Unit unit) {
        this.method = method;
        this.value = value;
        this.unit = unit;
    }

    public SootMethod getMethod() {
        return method;
    }

    public Value getValue() {
        return value;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "<METHOD " + method.toString() + ",UNIT " + unit.toString() + ",VALUE " + value.toString() + ">";
    }

    private String allowedTypesToString() {
        if (this.getAllowedTypes() == null) return "null";
        return allowedTypes.get(this).toString();
    }

    public String toDotLabel() {
        return "\"" +
                "METHOD " + method.toString().replace("\"", "'") + "\\n" +
                "UNIT " + unit.toString().replace("\"", "'") + "\\n" +
                "VALUE " + value.toString().replace("\"", "'") + "\\n" +
                "TYPE " + value.getType().toString().replace("\"", "'") + "\\n" +
                "ALLOWED TYPES\n" + this.allowedTypesToString().replace("\"", "'") +
                "\"";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DataDependencyNode) {
            return (((DataDependencyNode) other).method.equivHashCode() == (method.equivHashCode())) &&
                    ((DataDependencyNode) other).value.equivTo(value) &&
                    ((DataDependencyNode) other).unit.toString().equals(unit.toString());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + method.equivHashCode();
        result = 31 * result + value.equivHashCode();
        result = 31 * result + unit.toString().hashCode();
        return result;
    }
}
