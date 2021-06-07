package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

import static java.lang.Character.toLowerCase;
import static java.lang.Math.min;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.sort;


class GettersAndSetters {

    private static final Logger log = LoggerFactory.getLogger(GettersAndSetters.class);


    private static final String PREFIX_IS = "is";
    private static final int PREFIX_IS_LENGTH = PREFIX_IS.length();
    private static final String PREFIX_GET = "get";
    private static final int PREFIX_GET_LENGTH = PREFIX_GET.length();
    private static final String PREFIX_SET = "set";
    private static final int PREFIX_SET_LENGTH = PREFIX_SET.length();

    final HashMap<String, Method> getters = new HashMap<>();
    final HashMap<String, Method>  setters = new HashMap<>();
    BuilderContext builderContext;


    public GettersAndSetters(Class<?> clazz) {
        this(clazz, true, true, true);
    }

    public GettersAndSetters(Class<?> clazz, boolean extractGetters, boolean extractSetters, boolean usingSetters) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (isPublic(modifiers) && !isStatic(modifiers)) {
                boolean getterFound = false;
                if (extractGetters) {
                    String getterProp = isGetter(clazz, method);
                    if (getterFound = (getterProp != null)) {
                        log.info("{} getter: {} => {} {}()", clazz.getSimpleName(), getterProp, method.getReturnType().getSimpleName(), method.getName());
                        this.getters.put(getterProp, method);
                    }
                }

                if (!getterFound && extractSetters) { // not a getter, may be a setter then
                    String setterProp = isSetter(clazz, method, usingSetters);
                    if (setterProp != null) {
                        log.info("{} setter: {} => {}({})", clazz.getSimpleName(), setterProp, method.getName(), method.getParameterTypes()[0]);
                        this.setters.put(setterProp, method);
                    }
                }
            }
        }
    }


    public HashMap<String, Method> getters() {
        return getters;
    }

    public HashMap<String, Method> setters() {
        return setters;
    }

    public BuilderContext getBuilder() {
        return builderContext;
    }

    public void builder(BuilderContext builderContext) {
        this.builderContext = builderContext;
    }

    public Set<String> getterProps() {
        return getters.keySet();
    }

    public Set<String> setterProps() {
        return setters.keySet();
    }

    public Method getter(String prop) {
        return getters.get(prop);
    }

    public Method setter(String prop) {
        return setters.get(prop);
    }



    private static String isGetter(Class<?> clazz, Method method){
        if(method.getParameterTypes().length == 0) {
            String name = method.getName();
            Class<?> returnType = method.getReturnType();
            if(name.startsWith(PREFIX_GET) && !"getClass".equals(name) && !void.class.equals(returnType)) {
                return formatProp(name.substring(PREFIX_GET_LENGTH));
            } else if(name.startsWith(PREFIX_IS) && boolean.class.equals(returnType)) {
                return formatProp(name.substring(PREFIX_IS_LENGTH));
            }
        }
        return null;
    }


    private static String isSetter(Class<?> clazz, Method method, boolean usingSetPrefix) {
        String name = method.getName();
        if(!usingSetPrefix) {
            if(method.getReturnType().isAssignableFrom(clazz)) { // is it really a builder method? Still, taking Lombok's @SuperBuilder into account
                return formatProp(name);
            }
        } else if(name.startsWith(PREFIX_SET) && method.getParameterTypes().length == 1) {
            return formatProp(name.substring(PREFIX_SET_LENGTH));
        }
        return null;
    }

    private static String formatProp(String s) {
        if(s != null) {
            if(s.isEmpty()) {
                s = null;
            } else {
                s = toLowerCase(s.charAt(0)) + s.substring(1); //TODO ideally we should check the actual (private) prop name to check if it starts with a lowercase or not
            }
        }
        return s;
    }


    public void checkGettersContain(String... props) throws IllegalArgumentException {
        checkContains(getterProps(), props);
    }

    public void checkSettersContain(String... props) throws IllegalArgumentException {
        checkContains(setterProps(), props);
    }

    private void checkContains(Collection<String> gsProps, String... checkedProps) throws IllegalArgumentException {
        if(checkedProps != null && checkedProps.length > 0) {
            List<String> unknown = new ArrayList<>(checkedProps.length);
            for (String prop : checkedProps) {
                if (!gsProps.contains(prop)) {
                    unknown.add(prop);
                }
            }
            if(!unknown.isEmpty()) {
                throw new IllegalArgumentException("Unknown props: " + unknown);
            }
        }
    }

    public List<String> getMappableProps(GettersAndSetters gs) {
        List<String> result = new ArrayList<>(min(getters.size(), gs.setters.size()));
        for (String propertyName : getterProps()) {
            if(gs.setters.get(propertyName) != null) {
                result.add(propertyName);
            }
        }
        sort(result);
        return result;
    }


    public static List<String> getMappableProps(Class<?> fromClass, Class<?> toClass) {
        // unsafe on purpose
        GettersAndSetters fromGetters = new GettersAndSetters(fromClass, true, false, true),
                            toSetters = new GettersAndSetters(toClass, false, true, true);
        return fromGetters.getMappableProps(toSetters);
    }


    public final static class BuilderContext {
        Method method;
        boolean usingSetters;

        public BuilderContext(Method method, boolean usingSetters) {
            this.method = method;
            this.usingSetters = usingSetters;
        }
    }
}