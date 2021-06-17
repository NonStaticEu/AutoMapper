package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static eu.nonstatic.mapper.MappingUtils.mapProperty;
import static eu.nonstatic.mapper.ReflectionUtils.*;
import static eu.nonstatic.mapper.Utils.contains;
import static java.util.Objects.requireNonNull;

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
        return registry.computeIfAbsent(requireNonNull(clazz), c -> toGettersAndSettersSafe(c, usingSetters));
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
            return map(fromInstance, toClass.getDeclaredConstructor().newInstance(), excludedProps);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) { // are you POJO enough?
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


    private <F, T> T mapInternal(F fromInstance, T toInstance, boolean usingSetters, String[] excludedProps) {
        if(fromInstance != null && toInstance != null) {

            Class<?> fromClass = fromInstance.getClass(), toClass = toInstance.getClass();
            GettersAndSetters gsFrom = checkRegistered(fromClass, true, "from"),
                                gsTo = checkRegistered(toClass, usingSetters, "to");

            gsFrom.checkGettersContain(excludedProps);

            for (Map.Entry<String, Method> gEntry : gsFrom.getters.entrySet()) {
                String propertyName = gEntry.getKey();
                if(contains(propertyName, excludedProps)) {
                    log.debug("Skipping excluded prop {}", propertyName);
                } else {
                    Method setter = gsTo.setters.get(propertyName);
                    if (setter != null) {
                        Method getter = gEntry.getValue();
                        mapProperty(fromInstance, gsFrom.getTargetName(), getter, propertyName, toInstance, gsTo.getTargetName(), setter, propertyName, usingSetters);
                    } else {
                        log.debug("No match for getter {}.{} into {}", gsFrom.getTargetName(), propertyName, gsTo.getTargetName());
                    }
                }
            }
        }
        return toInstance;
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
}
