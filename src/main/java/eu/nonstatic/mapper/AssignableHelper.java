package eu.nonstatic.mapper;

import java.util.HashMap;
import java.util.Map;

public interface AssignableHelper {

    Map<Class<?>, Class<?>> primitiveToAssignable = new HashMap() {{
        put(byte.class, Number.class);
        put(short.class, Number.class);
        put(int.class, Number.class);
        put(long.class, Number.class);
        put(float.class, Number.class);
        put(double.class, Number.class);
        put(boolean.class, Boolean.class);
        put(char.class, Character.class);
    }};

    Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap() {{
        put(byte.class, Byte.class);
        put(short.class, Short.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(float.class, Float.class);
        put(double.class, Double.class);
        put(boolean.class, Boolean.class);
        put(char.class, Character.class);
    }};

    /*
    Map<Class<?>, Class<?>> wrapperToPrimitive = new HashMap() {{
        put(Byte.class, byte.class);
        put(Short.class, short.class);
        put(Integer.class, int.class);
        put(Long.class, long.class);
        put(Float.class, float.class);
        put(Double.class, double.class);
        put(Boolean.class, boolean.class);
        put(Character.class, char.class);
    }};
     */

    /*
     * Another (extreme) approach would be along the lines of
     * ttps://github.com/melezov/runtime-bytegen/blob/master/src/main/java/org/revenj/Primitives.java
     */
    static boolean isAssignable(Class<?> fromClass, Class<?> toClass) {
        if(toClass.isAssignableFrom(fromClass)) { // does not work with primitives <> wrapper
            return true;
        } else if(fromClass.isPrimitive()) {
            return primitiveToAssignable.get(fromClass).isAssignableFrom(toClass);
        } else if(toClass.isPrimitive()) {
            return primitiveToWrapper.get(toClass).equals(fromClass);
        } else {
            return false;
        }
    }
}
