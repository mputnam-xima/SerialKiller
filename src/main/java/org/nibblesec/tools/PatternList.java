package org.nibblesec.tools;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

class PatternList implements Iterable<Pattern> {
    private final Pattern[] patterns;

    public PatternList(final String... regExps) {

        requireNonNull(regExps, "regExps");

        this.patterns = new Pattern[regExps.length];
        for (int i = 0; i < regExps.length; i++) {
            patterns[i] = Pattern.compile(regExps[i]);
        }
    }

    @Override
    public Iterator<Pattern> iterator() {
        return new Iterator<Pattern>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < patterns.length;
            }

            @Override
            public Pattern next() {
                return patterns[index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    @Override
    public String toString() {
        return Arrays.toString(patterns);
    }

}
