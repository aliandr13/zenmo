package com.github.aliandr13.zenmo.utils;

import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Utility class to work with Collections.
 */
@UtilityClass
public class CollectionUtils {

    /**
     * Utility method to get first element of a list.
     *
     * @param list source list, can be null
     * @param <E>  list to get first element.
     * @return Optional.empty if the list is null or empty, otherwise Optional of the first element of the list.
     */
    public static <E> Optional<E> optionalFirst(List<E> list) {
        if (list == null) {
            return Optional.empty();
        }
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.getFirst());
    }

}
