package eu.nonstatic.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;

public final class ReflectionUtils {

    private static final String DEFAULT_BUILDER_METHOD_NAME = "builder";
    private static final String DEFAULT_BUILDER_S_BUILD_METHOD_NAME = "build";
    private static final String AVRO_SPECIFIC_RECORD_BASE_FQCN = "org.apache.avro.specific.SpecificRecordBase"; // FQCN to avoid dependency

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils() {}

    /**
     * May be abstract. eg: lombok's @SuperBuilder would return an abstract type
     * @param clazz
     * @return
     */
    public static boolean isBuildable(Class<?> clazz) {
        return ! ( clazz.isInterface()
                || clazz.isAnnotation()
                || clazz.isEnum()
                || clazz.isPrimitive()
                || clazz.isArray());
    }

    /**
     * @param clazz
     * @return true is the class given as parameter may be used as a target for mapping
     */
    public static boolean isMappable(Class<?> clazz) {
        return !isAbstract(clazz.getModifiers()) && isBuildable(clazz) && isNoArgsConstructor(clazz);
    }

    public static boolean isNoArgsConstructor(Class<?> clazz){
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
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

        // need to search better now
        for (Method method : clazz.getMethods()) {
            Class<?> returnType = method.getReturnType(); // builder's type ?
            if (isBuilderLikeMethod(method) && hasBuildLikeMethod(returnType, clazz)) {
                return method;
            }
        }

        throw new NoSuchMethodException("Can't find any obvious builder method");
    }


    static boolean hasBuildLikeMethod(Class<?> builderClazz, Class<?> expectedBuiltType) {
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

    static boolean isBuilderLikeMethod(Method method) {
        return isStatic(method.getModifiers()) && method.getParameterTypes().length == 0
                && isBuildable(method.getReturnType());
    }

    static boolean isBuildLikeMethod(Method method, Class<?> expectedBuiltType) {
        return !isStatic(method.getModifiers()) && method.getParameterTypes().length == 0
                && method.getReturnType().equals(expectedBuiltType);
    }

    /**
     * Add more tests if needed
     * @param clazz
     * @return
     */
    static boolean isBuilderUsingSetters(Class<?> clazz) {
        return AVRO_SPECIFIC_RECORD_BASE_FQCN.equals(clazz.getSuperclass().getName());
    }
}
