package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static eu.nonstatic.mapper.AssignableHelper.isAssignable;
import static eu.nonstatic.mapper.ReflectionUtils.*;

public class AutoMapper {

    private static final Logger log = LoggerFactory.getLogger(GettersAndSetters.class);

    private final HashMap<Class<?>, GettersAndSetters> registry = new HashMap<>();
    private boolean autoRegister;


    public AutoMapper() {
        setAutoRegister(true);
    }

    public AutoMapper(Class<?>... clazzz) {
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

    public AutoMapper setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
        return this;
    }


    private GettersAndSetters toGettersAndSettersSafe(Class<?> clazz, boolean usingSetters) {
        return toGettersAndSettersSafe(clazz, true, true, usingSetters);
    }


    private GettersAndSetters toGettersAndSettersSafe(Class<?> clazz, boolean getters, boolean setters, boolean usingSetters) {
        if(usingSetters ? isBuildable(clazz) : isMappable(clazz)) {
            return new GettersAndSetters(clazz, getters, setters, usingSetters);
        } else {
            throw new IllegalArgumentException("Won't be able to map type " + clazz.getName());
        }
    }


    public <F, T> T map(F fromInstance, T toInstance) {
        return map(fromInstance, toInstance, (String[])null);
    }


    public <F, T> T map(F fromInstance, T toInstance, String... excludedProps) {
        return mapInternal(fromInstance, toInstance, true, excludedProps);
    }


    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass) {
        return mapToInstance(fromInstance, toClass, (String[])null);
    }


    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass, String... excludedProps) {
        try {
            return map(fromInstance, toClass.newInstance(), excludedProps);
        } catch (InstantiationException | IllegalAccessException e) { // are you POJO enough?
            throw new RuntimeException(e);
        }
    }


    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass) {
        return mapToBuilder(fromInstance, toClass, (String[])null);
    }


    @SuppressWarnings("unchecked")
    public <B> B mapToBuilder(Object fromInstance, Class<?> to, String... excludedProps) {
        try {
            Object toInstance;

            GettersAndSetters gsTo = register(to); // No matter how autoRegister is set
            GettersAndSetters.BuilderContext builderContext = gsTo.builderContext;
            if(builderContext == null) {
                Method builderMethod = findBuilderMethod(to);
                toInstance = builderMethod.invoke(null);// NOT builderMethod.getReturnType(), its result may be abstract whereas calling will give a concrete instance, which is what we're actually mapping to.
                boolean usingSetters = isBuilderUsingSetters(to);

                register(toInstance.getClass(), usingSetters); // No matter how autoRegister is set
                gsTo.builder(builderContext = new GettersAndSetters.BuilderContext(builderMethod, usingSetters));
            } else {
                toInstance = builderContext.method.invoke(null);
            }

            return (B) mapInternal(fromInstance, toInstance, builderContext.usingSetters, excludedProps); // unsafe!
        }
        catch(NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private <F, T> T mapInternal(F from, T to, boolean usingSetters, String[] excludedProps) {
        if(from != null && to != null) {

            Class<?> fromClass = from.getClass(), toClass = to.getClass();
            GettersAndSetters gsFrom = checkRegistered(fromClass, true, "from"),
                                gsTo = checkRegistered(toClass, usingSetters, "to");

            gsFrom.checkGettersContain(excludedProps);

            for (Map.Entry<String, Method> gEntry : gsFrom.getters.entrySet()) {
                String propertyName = gEntry.getKey();
                if(contains(propertyName, excludedProps)) {
                    log.info("Skipping excluded prop {}", propertyName);
                } else {
                    Method setter = gsTo.setters.get(propertyName);
                    String fromName = fromClass.getSimpleName(), toName = toClass.getSimpleName();
                    if (setter != null) {
                        mapProperty(propertyName, from, fromName, gEntry.getValue(), to, toName, setter, usingSetters);
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


    private static <F, T> void mapProperty(String propertyName, F from, String fromName, Method getter, T to, String toName, Method setter, boolean usingSetters) {
        try {
            Object value = getter.invoke(from);
            // taking the most specialized; eg: Number getProp() where prop's value is an actual Integer.
            Class<?> getterReturn = value != null ? value.getClass() : getter.getReturnType();
            Class<?> setterParamType = setter.getParameterTypes()[0];
            if (/* TODO useless? !usingSetters || */ isAssignable(getterReturn, setterParamType)) {
                 // happy that primitives do auto boxing
                log.info("Mapping from {}.{} to {}.{} with {}", fromName, propertyName, toName, propertyName, value);
                try {
                    //TODO coertion
                    setter.invoke(to, value); // also happy auto unboxing takes place when needed
                } catch (IllegalArgumentException e) { // most probably unboxing on null
                    if(value == null && setterParamType.isPrimitive()) {
                        throw new IllegalArgumentException("Can't unbox null value of " + fromName + '.' + propertyName
                                                                                 + " to " + toName + '.' + propertyName, e);
                    } else {
                        throw e;
                    }
                }
            } else {
                log.info("Incompatible mapping from {} {}#{} to {}#{}({})",
                        getterReturn.getSimpleName(), fromName, getter.getName(),
                        toName, setter.getName(), setterParamType.getSimpleName());
            }
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
