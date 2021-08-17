package eu.nonstatic.mapper;

import java.lang.reflect.Method;
import java.util.*;

import static eu.nonstatic.mapper.MappingUtils.mapProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

/**
 * Mapper with specific source class.prop => target class.prop descriptions
 */
public class FromToMapper {

    private HashMap<Class, FromMappers> mappings = new HashMap<>(); // Class is fromClass
    private AutoMapper mapper = new AutoMapper();

    private FromMappers getMappers(Class<?> fromClass) {
        return mappings.computeIfAbsent(fromClass, c -> new FromMappers());
    }

    public Collection<MappingDescriptor> getMapping(Class<?> fromClass, Class<?> toClass) {
        return mappings.get(fromClass).get(toClass).values();
    }


    public MappingDescriptor registerMapping(Class<?> fromClass, String fromProp, Class<?> toClass, String toProp) {
        GettersAndSetters gsFrom = mapper.getRegistrationForced(fromClass), gsTo = mapper.getRegistrationForced(toClass);
        Method getter = gsFrom.getter(fromProp), setter = gsTo.setter(toProp);

        return getMappers(fromClass).get(toClass)
                .registerMapping(fromProp, new MappingDescriptor(gsFrom.targetClassName, fromProp, getter, gsTo.targetClassName, toProp, setter));
    }

    public void unregisterMapping(Class<?> fromClass, String fromProp, Class<?> toClass) {
        Iterator<MappingDescriptor> it = mappings.get(fromClass).get(toClass).iterator();
        while(it.hasNext()) {
            MappingDescriptor descriptor = it.next();
            if(fromProp.equals(descriptor.fromPropName)) {
                it.remove();
                // no break, there might be fromProp -> * toProp
            }
        }
    }

    public void unregisterMapping(Class<?> fromClass, String fromProp, Class<?> toClass, String toProp) {
        Iterator<MappingDescriptor> it = mappings.get(fromClass).get(toClass).iterator();
        while(it.hasNext()) {
            MappingDescriptor descriptor = it.next();
            if(fromProp.equals(descriptor.fromPropName) && toProp.equals(descriptor.toPropName)) {
                it.remove();
                break;
            }
        }
    }


    public <F, T> T map(F fromInstance, T toInstance) {
        return map(fromInstance, toInstance, emptySet());
    }

    public <F, T> T map(F fromInstance, T toInstance, String... excludedProps) {
        return map(fromInstance, toInstance, asList(excludedProps));
    }

    public <F, T> T map(F fromInstance, T toInstance, Collection<String> excludedProps) {
        ToMappers toMappers = getMappers(fromInstance.getClass()).get(toInstance.getClass());
        Collection<String> excludedPropsMerged = mergeExcludes(toMappers, excludedProps);
        return mapper.map(fromInstance, toInstance, excludedPropsMerged, ti -> mapInternal(fromInstance, ti, toMappers));
    }



    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass) {
        return mapToInstance(fromInstance, toClass, emptySet());
    }

    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass, String... excludedProps) {
        return mapToInstance(fromInstance, toClass, asList(excludedProps));
    }

    public <F, T> T mapToInstance(F fromInstance, Class<T> toClass, Collection<String> excludedProps) {
        ToMappers toMappers = getMappers(fromInstance.getClass()).get(toClass);
        Collection<String> excludedPropsMerged = mergeExcludes(toMappers, excludedProps);
        return mapper.mapToInstance(fromInstance, toClass, excludedPropsMerged, toInstance -> mapInternal(fromInstance, toInstance, toMappers));
    }



    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass) {
        return mapToBuilder(fromInstance, toClass, emptySet());
    }

    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass, String... excludedProps) {
        return mapToBuilder(fromInstance, toClass, asList(excludedProps));
    }

    public <B> B mapToBuilder(Object fromInstance, Class<?> toClass, Collection<String> excludedProps) {
        GettersAndSetters gsFrom = mapper.getRegistrationForced(fromInstance.getClass());

        AutoMapper.BuilderWrapper<B> builderWrapper = mapper.getContextualizedBuilder(toClass);
        GettersAndSetters gsTo = builderWrapper.gettersAndSetters;

        ToMappers toMappers = getMappers(fromInstance.getClass()).get(toClass) // the issue here is those mappers apply to toClass, not its builder
                .migrate(gsTo); // so we're recomputing dynamically on the builder class to prevent running after mapping sync between a class and its builder
        Collection<String> excludedPropsMerged = mergeExcludes(toMappers, excludedProps);

        return mapper.mapInternal(fromInstance, gsFrom, builderWrapper.builder, gsTo, excludedPropsMerged,
                toBuilder -> mapInternal(fromInstance, toBuilder, toMappers));
    }


    // =================================================================================================================

    private static Collection<String> mergeExcludes(ToMappers toMappers, Collection<String> excludedProps) {
        Set<String> excludedPropsSpecific = toMappers.keySet();
        if(excludedProps != null && !excludedProps.isEmpty()) {
            // I prefer a List to a HashSet, less lookup-consuming and it doesnt matter if there are dupes.
            List<String> excludedPropsMerged = new ArrayList<>(excludedPropsSpecific.size() + excludedProps.size());
            excludedPropsMerged.addAll(excludedPropsSpecific);
            excludedPropsMerged.addAll(excludedProps);
            return excludedPropsMerged;
        } else {
            return excludedPropsSpecific;
        }
    }

    private static <F, T> T mapInternal(F fromInstance, T toInstanceOrBuilder, ToMappers toMappers) {
        for (MappingDescriptor descriptor : toMappers) {
            mapProperty(fromInstance, descriptor.fromClassName, descriptor.fromGetter, descriptor.fromPropName,
                    toInstanceOrBuilder, descriptor.toClassName, descriptor.toSetter, descriptor.toPropName);
        }
        return toInstanceOrBuilder;
    }


    // =================================================================================================================

    /**
     * This class contains each defined mapping (target classes => target props) associated with a source class
     */
    public static final class FromMappers implements Iterable<ToMappers> {
        private HashMap<Class<?>, ToMappers> map = new HashMap<>(); // Class is toClass

        public ToMappers get(Class<?> toClass) {
            return map.computeIfAbsent(toClass, c -> new ToMappers());
        }

        @Override
        public Iterator<ToMappers> iterator() {
            return map.values().iterator();
        }
    }

    /**
     * This class contains each defined mapping (target prop => mapping description) associated to a {source class, target class} couple
     */
    public static final class ToMappers implements Iterable<MappingDescriptor> {
        private HashMap<String, MappingDescriptor> map = new HashMap<>(); // String is fromPropName

        public MappingDescriptor registerMapping(String fromPropName, MappingDescriptor mappingFunction) {
            return map.put(fromPropName, mappingFunction);
        }

        public Set<String> keySet() {
            return map.keySet();
        }

        public Collection<MappingDescriptor> values() {
            return map.values();
        }

        @Override
        public Iterator<MappingDescriptor> iterator() {
            return map.values().iterator();
        }

        public ToMappers migrate(GettersAndSetters gs) {
            ToMappers result = new ToMappers();
            map.forEach((fromPropName, mappingDescriptor) -> result.registerMapping(fromPropName, mappingDescriptor.migrate(gs)));
            return result;
        }
    }


    /**
     * This class expresses how to map a property from one instance to another
     */
    public static final class MappingDescriptor {
        private final String fromClassName;
        private final String fromPropName;
        private final Method fromGetter;

        private final String toClassName;
        private final String toPropName;
        private final Method toSetter;

        public MappingDescriptor(String fromClassName, String fromPropName, Method fromGetter,
                                 String toClassName, String toPropName, Method toSetter) {
            this.fromClassName = fromClassName;
            this.fromPropName = fromPropName;
            this.fromGetter = fromGetter;

            this.toClassName = toClassName;
            this.toPropName = toPropName;
            this.toSetter = toSetter;
        }

        public MappingDescriptor migrate(GettersAndSetters gsTo) {
            Method migratedSetter = gsTo.setter(toPropName);
            return new MappingDescriptor(fromClassName, fromPropName, fromGetter,
                                         gsTo.targetClassName, toPropName, migratedSetter);
        }
    }
}
