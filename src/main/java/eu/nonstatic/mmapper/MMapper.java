package eu.nonstatic.mmapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

public class MMapper {

    private static final Logger log = LoggerFactory.getLogger(GettersAndSetters.class);

    private static final String DEFAULT_BUILDER_METHOD_NAME = "builder";
    private static final String DEFAULT_BUILDER_S_BUILD_METHOD_NAME = "build";
    public static final String AVRO_SPECIFIC_RECORD_BASE_FQCN = "org.apache.avro.specific.SpecificRecordBase"; // to avoid dependency


    private final HashMap<Class<?>, GettersAndSetters> registry = new HashMap<>();
    private boolean autoRegister;


    public MMapper() {
        setAutoRegister(true);
    }

    public MMapper(Class<?>... clazzz) {
        for (Class<?> clazz : clazzz) {
            register(clazz, true);
        }
    }


    public GettersAndSetters register(Class<?> clazz) {
        return register(clazz, true);
    }

    public GettersAndSetters register(Class<?> clazz, boolean usingSetters) {
        return registry.computeIfAbsent(clazz, c -> toGettersAndSettersSafe(c, usingSetters));
    }


    public boolean isAutoRegister() {
        return autoRegister;
    }

    public MMapper setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
        return this;
    }


    private GettersAndSetters toGettersAndSettersSafe(Class<?> clazz, boolean usingSetters) {
        return toGettersAndSettersSafe(clazz, true, true, usingSetters);
    }


    private GettersAndSetters toGettersAndSettersSafe(Class<?> clazz, boolean getters, boolean setters, boolean usingSetters) {
        if(isMappable(clazz)) {
            return new GettersAndSetters(clazz, getters, setters, usingSetters);
        } else {
            throw new IllegalArgumentException("Won't be able to map type " + clazz.getName());
        }
    }


    public <F, T> T map(F from, T to) {
        return map(from, to, (String[])null);
    }


    public <F, T> T map(F from, T to, String... excludedProps) {
        return mapInternal(from, to, true, excludedProps);
    }


    public <F, T> T mapToInstance(F from, Class<T> to) {
        return mapToInstance(from, to, (String[])null);
    }


    public <F, T> T mapToInstance(F from, Class<T> to, String... excludedProps) {
        try {
            return map(from, to.newInstance(), excludedProps);
        } catch (InstantiationException | IllegalAccessException e) { // are you POJO enough?
            throw new RuntimeException(e);
        }
    }


    public <F, T, B> B mapToBuilder(F from, Class<T> to) {
        return mapToBuilder(from, to, (String[])null);
    }


    @SuppressWarnings("unchecked")
    public <F, T, B> B mapToBuilder(F from, Class<T> to, String... excludedProps) {
        try {
            GettersAndSetters gsTo = register(to); // No matter how autoRegister is set
            GettersAndSetters.BuilderContext builderContext = gsTo.builderContext;
            if(builderContext == null) {
                Method builderMethod = findBuilderMethod(to);
                Class<?> builderClass = builderMethod.getReturnType();
                boolean usingSetters = isBuilderUsingSetters(to);

                register(builderClass, usingSetters); // No matter how autoRegister is set
                gsTo.builder(builderContext = new GettersAndSetters.BuilderContext(builderMethod, usingSetters));
            }
            return (B) mapInternal(from, builderContext.method.invoke(from), builderContext.usingSetters, excludedProps); // unsafe!
        }
        catch(NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean isMappable(Class<?> clazz) {
        return ! ( clazz.isInterface()
                || isAbstract(clazz.getModifiers())
                || clazz.isAnnotation()
                || clazz.isEnum()
                || clazz.isPrimitive()
                || clazz.isArray());
    }



    public static Method findBuilderMethod(Class<?> clazz) throws NoSuchMethodException {
        try {
            Method builderMethod = clazz.getMethod(DEFAULT_BUILDER_METHOD_NAME);
            if(isBuilderLikeMethod(builderMethod)) {
                return builderMethod;
            } else {
                log.debug("{}.{} method doesn't look like a builder", clazz.getSimpleName(), DEFAULT_BUILDER_METHOD_NAME);
            }
        } catch (NoSuchMethodException e) {
            // nothing for now, we have a second chance
        }

        // need to search better
        for (Method method : clazz.getMethods()) {
            Class<?> returnType = method.getReturnType(); // builder's type ?
            if (isBuilderLikeMethod(method) && hasBuildLikeMethod(returnType, clazz)) {
                return method;
            }
        }

        throw new NoSuchMethodException("Can't find any obvious builder method");
    }


    private static boolean hasBuildLikeMethod(Class<?> builderClazz, Class<?> expectedBuiltType) {
        try {
            Method buildMethod = builderClazz.getMethod(DEFAULT_BUILDER_S_BUILD_METHOD_NAME);
            if(isBuildLikeMethod(buildMethod, expectedBuiltType)) {
                return true;
            } else {
                log.debug("{}.{}} method doesn't look like a build method", builderClazz.getSimpleName(), DEFAULT_BUILDER_S_BUILD_METHOD_NAME);
            }
        } catch (NoSuchMethodException e) {
            // nothing for now, we have a second chance
        }

        // need to search better
        for (Method method : builderClazz.getMethods()) {
            if (isBuildLikeMethod(method, expectedBuiltType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBuilderLikeMethod(Method method) {
        return isStatic(method.getModifiers()) && method.getParameterTypes().length == 0
                && isMappable(method.getReturnType());
    }

    private static boolean isBuildLikeMethod(Method method, Class<?> expectedBuiltType) {
        return !isStatic(method.getModifiers()) && method.getParameterTypes().length == 0
                && method.getReturnType().equals(expectedBuiltType);
    }

    /**
     * Add more tests if needed
     * @param clazz
     * @return
     */
    private static boolean isBuilderUsingSetters(Class<?> clazz) {
        return AVRO_SPECIFIC_RECORD_BASE_FQCN.equals(clazz.getSuperclass().getName());
    }


    private <F, T> T mapInternal(F from, T to, boolean usingSetters, String[] excludedProps) {
        if(from != null && to != null) {

            Class<?> fromClass = from.getClass(), toClass = to.getClass();
            GettersAndSetters gsFrom = checkRegistered(fromClass, true, "from"),
                                gsTo = checkRegistered(toClass, usingSetters, "to");

            for (Map.Entry<String, Method> gEntry : gsFrom.getters.entrySet()) {
                String propertyName = gEntry.getKey();
                if(contains(propertyName, excludedProps)) {
                    log.info("Skipping excluded prop {}", propertyName);
                } else {
                    Method setter = gsTo.setters.get(propertyName);
                    String fromName = fromClass.getSimpleName(), toName = toClass.getSimpleName();
                    if (setter != null) {
                        mapProperty(propertyName, from, fromName, gEntry.getValue(), to, toName, setter, !usingSetters);
                    } else {
                        log.info("No match for getter {}.{} into {}", fromName, propertyName, toName);
                    }
                }
            }
        }
        return to;
    }


    private GettersAndSetters checkRegistered(Class<?> clazz, boolean usingSetters, String direction) {
        GettersAndSetters gs = registry.get(clazz);
        if(gs == null) {
            if(autoRegister) {
                gs = register(clazz, usingSetters);
            } else {
                throw new IllegalArgumentException("Don't know how to map " + direction + ' ' + clazz.getName());
            }
        }
        return gs;
    }


    private static <T> boolean contains(T needle, T[] haystack) {
        if(haystack != null) {
            for (T candidate : haystack) {
                if (needle.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static <F, T> void mapProperty(String propertyName, F from, String fromName, Method getter, T to, String toName, Method setter, boolean builder) {
        try {
            Class<?> getterReturn = getter.getReturnType(), setterParamType = setter.getParameterTypes()[0];
            if (builder || setterParamType.isAssignableFrom(getterReturn)) {
                Object value = getter.invoke(from);
                log.info("Mapping from {}.{} to {}.{} with {}", fromName, propertyName, toName, propertyName, value);
                setter.invoke(to, value);
            } else {
                log.info("Incompatible mapping from {}.{} as {} to {}.{} on {}",
                        fromName, propertyName, getterReturn.getSimpleName(),
                        toName, propertyName, setterParamType.getSimpleName());
            }
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static List<String> getMappableProps(Class<?> fromClass, Class<?> toClass) {
        // unsafe on purpose
        GettersAndSetters fromGetters = new GettersAndSetters(fromClass, true, false, true),
                            toSetters = new GettersAndSetters(toClass, false, true, true);
        return fromGetters.getMappableProps(toSetters);
    }
}
