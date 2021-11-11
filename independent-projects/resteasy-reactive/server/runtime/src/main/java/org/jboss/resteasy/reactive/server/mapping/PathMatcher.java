package org.jboss.resteasy.reactive.server.mapping;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Handler that dispatches to a given handler based of a prefix match of the path.
 * <p>
 * This only matches a single level of a request, e.g if you have a request that takes the form:
 * <p>
 * /foo/bar
 * <p>
 *
 * @author Stuart Douglas
 */
public class PathMatcher<T> implements Dumpable {

    private static final String STRING_PATH_SEPARATOR = "/";

    private volatile T defaultHandler;
    private final SubstringMap<T> paths = new SubstringMap<T>();

    /**
     * lengths of all registered paths
     */
    private volatile int[] lengths = {};

    public PathMatcher() {
    }

    /**
     * Matches a path against the registered handlers.
     * 
     * @param path The relative path to match
     * @return The match match. This will never be null, however if none matched its value field will be
     */
    public PathMatch<T> match(String path) {
        int length = path.length();
        final int[] lengths = this.lengths;
        for (int i = 0; i < lengths.length; ++i) {
            int pathLength = lengths[i];
            if (pathLength == length) {
                SubstringMap.SubstringMatch<T> next = paths.get(path, length);
                if (next != null) {
                    return new PathMatch<>(path, "", next.getValue());
                }
            } else if (pathLength < length) {
                SubstringMap.SubstringMatch<T> next = paths.get(path, pathLength);
                if (next != null) {
                    return new PathMatch<>(next.getKey(), path.substring(pathLength), next.getValue());
                }
            }
        }
        return new PathMatch<>("/", path, defaultHandler);
    }

    /**
     * Adds a path prefix and a handler for that path. If the path does not start
     * with a / then one will be prepended.
     * <p>
     * The match is done on a prefix bases, so registering /foo will also match /bar. Exact
     * path matches are taken into account first.
     * <p>
     * If / is specified as the path then it will replace the default handler.
     *
     * @param path The path
     * @param handler The handler
     */
    public synchronized PathMatcher addPrefixPath(final String path, final T handler) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path not specified");
        }

        if (PathMatcher.STRING_PATH_SEPARATOR.equals(path)) {
            this.defaultHandler = handler;
            return this;
        } else if (path.endsWith(STRING_PATH_SEPARATOR)) {
            throw new RuntimeException("Prefix path cannot end with /");
        }

        paths.put(path, handler);

        buildLengths();
        return this;
    }

    private void buildLengths() {
        final Set<Integer> lengths = new TreeSet<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return -o1.compareTo(o2);
            }
        });
        for (String p : paths.keys()) {
            lengths.add(p.length());
        }

        int[] lengthArray = new int[lengths.size()];
        int pos = 0;
        for (int i : lengths) {
            lengthArray[pos++] = i;
        }
        this.lengths = lengthArray;
    }

    public static final class PathMatch<T> {
        private final String matched;
        private final String remaining;
        private final T value;

        public PathMatch(String matched, String remaining, T value) {
            this.matched = matched;
            this.remaining = remaining;
            this.value = value;
        }

        public String getRemaining() {
            return remaining;
        }

        public String getMatched() {
            return matched;
        }

        public T getValue() {
            return value;
        }
    }

    @Override
    public void dump(int level) {
        System.err.println("Paths: " + paths.size());
        for (String key : paths.keys()) {
            System.err.println(" " + key + ": ");
            SubstringMap.SubstringMatch<T> match = paths.get(key);
            System.err.println("  matchKey: " + match.getKey());
            System.err.println("  matchValue: ");
            dumpValue(match.getValue(), 3);
        }
        System.err.println("Default handler: " + defaultHandler);
    }

    private void dumpValue(T value, int level) {
        if (value instanceof List) {
            for (Object x : (List) value) {
                if (x instanceof Dumpable)
                    ((Dumpable) x).dump(level);
            }
        }
    }

}
