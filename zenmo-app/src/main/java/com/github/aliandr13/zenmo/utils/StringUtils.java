package com.github.aliandr13.zenmo.utils;

import lombok.experimental.UtilityClass;

/**
 * Utility class to work with Strings.
 */
@UtilityClass
public class StringUtils {

    /**
     * Null-safe method to convert string to lower case.
     *
     * @param source string to convert to lower case, can be null.
     * @return empty string if source is null, otherwise - the String, converted to lowercase.
     */
    public static String toLowerCase(String source) {
        return source == null ? "" : source.toLowerCase();
    }

    /**
     * Null-safe trim.
     *
     * @param source string to trim, can be null.
     * @return empty string if source is null, trimmed string otherwise.
     */
    public static String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }

    /**
     * Null-safe method to removed leading and trailing of the source string and convert to lower case.
     *
     * @param source string to normalize, can be null.
     * @return trimmed converted to lower case string and if source is not null, empty string otherwise.
     */
    public static String normalize(String source) {
        return toLowerCase(trimToEmpty(source));
    }

}
