package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class MappingUtils {

    private static final Logger log = LoggerFactory.getLogger(MappingUtils.class);

    private MappingUtils() {
        // nada
    }

    private static final Map<Class<?>, Class<?>> primitiveToAssignable = new HashMap() {{
        put(byte.class, Number.class);
        put(short.class, Number.class);
        put(int.class, Number.class);
        put(long.class, Number.class);
        put(float.class, Number.class);
        put(double.class, Number.class);
        put(boolean.class, Boolean.class);
        put(char.class, Character.class);
    }};

    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap() {{
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


    static <F, T> void mapProperty(F fromInstance, String fromClassName, Method getter, String getterPropName,
                                   T toInstance, String toClassName, Method setter, String setterPropName,
                                   boolean usingSetters) {
        try {
            Object value = getter.invoke(fromInstance);
            // taking the most specialized; eg: Number getProp() where prop's value is an actual Integer.
            Class<?> getterReturn = value != null ? value.getClass() : getter.getReturnType();
            Class<?> setterParamType = setter.getParameterTypes()[0];
            if (/* TODO useless? !usingSetters || */ isAssignable(getterReturn, setterParamType)) {
                // happy that primitives do auto boxing
                log.info("Mapping from {}.{} to {}.{} with {}", fromClassName, getterPropName, toClassName, setterPropName, value);
                try {
                    //TODO coertion
                    setter.invoke(toInstance, value); // also happy auto unboxing takes place when needed
                } catch (IllegalArgumentException e) { // most probably unboxing on null
                    if(value == null && setterParamType.isPrimitive()) {
                        throw new IllegalArgumentException("Can't unbox null value of " + fromClassName + '.' + getterPropName
                                + " to " + toClassName + '.' + setterPropName, e);
                    } else {
                        throw e;
                    }
                }
            } else {
                log.info("Incompatible mapping from {} {}#{} to {}#{}({})",
                        getterReturn.getSimpleName(), fromClassName, getter.getName(),
                        toClassName, setter.getName(), setterParamType.getSimpleName());
            }
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
