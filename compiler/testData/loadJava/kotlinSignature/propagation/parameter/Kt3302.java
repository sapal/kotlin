package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

// See SubclassFromGenericAndNot, as well
public interface Kt3302 {
    public interface BSONObject {
        Object put(@NotNull String s, @NotNull Object o);
    }

    public interface LinkedHashMap<K, V> {
        @KotlinSignature("fun put(key : K, value : V) : V?")
        public V put(K key, V value);
    }

    public interface BasicBSONObject extends LinkedHashMap<String, Object>, BSONObject {
        @Override
        public Object put(String key, Object value);
    }
}
