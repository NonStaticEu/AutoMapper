package eu.nonstatic.mapper;

public final class Utils {

    private Utils() {}

    public static <T> boolean contains(T needle, T[] haystack) {
        if(haystack != null) {
            for (T candidate : haystack) {
                if (needle.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
