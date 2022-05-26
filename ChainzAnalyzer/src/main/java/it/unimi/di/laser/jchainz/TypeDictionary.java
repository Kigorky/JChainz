package it.unimi.di.laser.jchainz;

import soot.Type;

import java.util.*;

public class TypeDictionary {

    private Map<String, List<Type>> dictionary = new HashMap<>();

    public TypeDictionary() {
    }

    public int size() {
        return dictionary.size();
    }

    public void init(String value) {
        if (this.dictionary.get(value) == null) {
            this.dictionary.put(value, new ArrayList<Type>());
        }
    }

    public Set<String> keySet() {
        return dictionary.keySet();
    }

    public void add(String value, Type type) {
        if (dictionary.get(value) == null) dictionary.put(value, new ArrayList<Type>());
        boolean present = false;
        for (Type dictionaryType : dictionary.get(value)) {
            if (dictionaryType.toString().equals(type.toString())) {
                present = true;
                break;
            }
        }
        if (!present)
            dictionary.get(value).add(type);
    }

    public List<Type> get(String value) {
        if (dictionary.get(value) == null) dictionary.put(value, new ArrayList<Type>());
        return dictionary.get(value);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (String value : dictionary.keySet()) {
            stringBuilder.append(value + " ");
            stringBuilder.append("[");
            for (int i = 0; i < dictionary.get(value).size(); i++) {
                stringBuilder.append(dictionary.get(value).get(i));
                if (i < dictionary.get(value).size() - 1) stringBuilder.append(",");
            }
            stringBuilder.append("]");
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }
}
