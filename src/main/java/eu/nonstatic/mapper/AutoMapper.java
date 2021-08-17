package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static eu.nonstatic.mapper.GettersAndSetters.USING_SETTERS_DEFAULT;
import static eu.nonstatic.mapper.MappingUtils.mapProperty;
import static eu.nonstatic.mapper.ReflectionUtils.findBuilderMethod;
import static eu.nonstatic.mapper.ReflectionUtils.isBuilderUsingSetters;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

public class AutoMapper {

    private static final Logger log = LoggerFactory.getLogger(GettersAndSetters.class);

    private final HashMap<Class<?>, GettersAndSetters> registry = new HashMap<>();
    private boolean autoRegister;


    public AutoMapper() {
        setAutoRegister(true);
    }

    public AutoMapper(Class<?>... clazzz) {
        for (Class<?> clazz : clazzz) {
            registerClass(clazz, USING_SETTERS_DEFAULT);
        }
    }


    public boolean isAutoRegister() {
        return autoRegister;
    }

    public AutoMapper setAutoRegister(boolean autoRegister) {
        this.autoRegister = autoRegister;
        return this;
    }


    @Deprecated
    private GettersAndSetters registerClass(Class<?> clazz) {
        return registerClass(clazz, USING_SETTERS_DEFAULT);
    }

    /**
     * Calling twice willre-register (in case you changed your mind about setters or the class got altered!)
     * @param clazz
     * @param usingSetters
     * @return
     */
    private GettersAndSetters registerClass(Class<?> clazz, boolean usingSetters) {
        GettersAndSetters gs = GettersAndSetters.of(clazz, usingSetters);
        registry.put(clazz, gs);
        return gs;
    }

    public GettersAndSetters unregisterClass(Class<?> clazz) {
        return registry.remove(requireNonNull(clazz));
    }


    public GettersAndSetters getRegistration(Class<?> clazz) {
        return getRegistration(clazz, USING_SETTERS_DEFAULT);
    }

    private GettersAndSetters getRegistration(Class<?> clazz, boolean usingSetters) {
        return registerOnDemand(clazz, usingSetters, this.autoRegister);
    }

    public GettersAndSetters getRegistrationForced(Class<?> clazz) {
        return getRegistrationForced(clazz, USING_SETTERS_DEFAULT);
    }

    private GettersAndSetters getRegistrationForced(Class<?> clazz, boolean usingSetters) {
        return registerOnDemand(clazz, usingSetters, true);
    }

    private GettersAndSetters registerOnDemand(Class<?> clazz, boolean usingSetters, boolean autoRegister) {
        if(autoRegister) {
            return registry.computeIfAbsent(clazz, c -> registerClass(clazz, usingSetters));
        } else {
            GettersAndSetters gs = registry.get(clazz);
            if(gs != null) {
                return gs;
            } else {
                throw new IllegalArgumentException("Don't know how to map " + clazz.getName());
            }
        }
    }


    public <F, T> T map(F fromInstance, T toInstance) {
        return map(fromInstance, toInstance, emptySet(), identity());
    }


    public <F, T> T map(F fromInstance, T toInstance, String... excludedProps) {
        return map(fromInstance, toInstance, asList(excludedProps), identity());
    }

    public <F, T> T map(F fromInstance, T toInstance, Collection<String> excludedProps) {
        return map(fromInstance, toInstance, excludedProps, identity());
    }

    protected <F, T, R> R map(F fromInstance, T toInstance, Collection<String> excludedProps, Function<T, R> postProcessing) {
        GettersAndSetters gsFrom = getRegistration(fromInstance.getClass()), gsTo = getRegistration(toInstance.getClass());
        return mapInternal(fromInstance, gsFrom, toInstance, gsTo, excludedProps, postProcessing);
    }



    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass) {
        return mapToInstance(fromInstance, toClass, emptySet(), identity());
    }

    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass, String... excludedProps) {
        return mapToInstance(fromInstance, toClass, asList(excludedProps), identity());
    }

    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass, Collection<String> excludedProps) {
        return mapToInstance(fromInstance, toClass, excludedProps, identity());
    }

    protected <F, T, R> R mapToInstance(F fromInstance, Class<T> toClass, Collection<String> excludedProps, Function<T, R> postProcessing) {
        try {
            return map(fromInstance, toClass.getDeclaredConstructor().newInstance(), excludedProps, postProcessing);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) { // are you POJO enough?
            throw new RuntimeException(e);
        }
    }




    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass) {
        return mapToBuilder(fromInstance, toClass, emptySet());
    }

    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass, String... excludedProps) {
        return mapToBuilder(fromInstance, toClass, asList(excludedProps));
    }

    @SuppressWarnings("unchecked")
    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass, Collection<String> excludedProps) {
        return mapToBuilder(fromInstance, toClass, excludedProps, identity());
    }

    @SuppressWarnings("unchecked")
    protected <B> B mapToBuilder(Object fromInstance, Class<?> toClass, Collection<String> excludedProps, Function<B, B> postProcessing) {
        GettersAndSetters gsFrom = getRegistration(fromInstance.getClass());

        BuilderWrapper<B> builderWrapper = getContextualizedBuilder(toClass);
        GettersAndSetters gsTo = builderWrapper.gettersAndSetters;

        return mapInternal(fromInstance, gsFrom, builderWrapper.builder, gsTo, excludedProps, postProcessing); // unsafe!
    }


    <B> BuilderWrapper<B> getContextualizedBuilder(Class<?> toClass) {
        try {
            B builderInstance;
            GettersAndSetters gsTo = getRegistrationForced(toClass); // No matter how autoRegister is set
            GettersAndSetters.BuilderContext builderContext = gsTo.builderContext;

            if(builderContext == null) {
                Method builderMethod = findBuilderMethod(toClass);
                builderInstance = (B)builderMethod.invoke(null);// NOT builderMethod.getReturnType(), it may be abstract, whereas calling the builder method will obviously give a concrete instance, which is what we're actually mapping to.
                // We needed to build to know what to register
                Class<?> builderClass = builderInstance.getClass();
                boolean usingSetters = isBuilderUsingSetters(toClass);
                GettersAndSetters builderClassGS = getRegistrationForced(builderClass, usingSetters);// No matter how autoRegister is set
                gsTo.setBuilderContext(builderContext = new GettersAndSetters.BuilderContext(builderClass, builderMethod, builderClassGS));
            } else {
                builderInstance = (B)builderContext.method.invoke(null);
            }

            return new BuilderWrapper<>(builderInstance, builderContext);
        }
        catch(NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    class BuilderWrapper<B> {
        B builder; // builder instance
        GettersAndSetters gettersAndSetters;

        BuilderWrapper(B builder, GettersAndSetters.BuilderContext context) {
            this.builder = builder;
            this.gettersAndSetters = context.gettersAndSetters;
        }
    }

    <F, T, R> R mapInternal(F fromInstance, GettersAndSetters gsFrom, T toInstanceOrBuilder, GettersAndSetters gsTo, Collection<String> excludedProps, Function<T, R> postProcessing) {
        if(fromInstance != null && toInstanceOrBuilder != null) {
            gsFrom.checkGettersContain(excludedProps);

            for (Map.Entry<String, Method> gEntry : gsFrom.getters.entrySet()) {
                String propertyName = gEntry.getKey();
                if(excludedProps.contains(propertyName)) {
                    log.debug("Skipping excluded prop {}", propertyName);
                } else {
                    Method setter = gsTo.setters.get(propertyName);
                    if (setter != null) {
                        Method getter = gEntry.getValue();
                        mapProperty(fromInstance, gsFrom.getTargetClassName(), getter, propertyName, toInstanceOrBuilder, gsTo.getTargetClassName(), setter, propertyName);
                    } else {
                        log.debug("No match for getter {}.{} into {}", gsFrom.getTargetClassName(), propertyName, gsTo.getTargetClassName());
                    }
                }
            }
        }

        return postProcessing.apply(toInstanceOrBuilder);
    }
}
