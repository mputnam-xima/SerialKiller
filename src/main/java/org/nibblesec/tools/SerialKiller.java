/**
 * SerialKiller.java
 * <p>
 * Copyright (c) 2015-2016 Luca Carettoni
 * <p>
 * SerialKiller is an easy-to-use look-ahead Java deserialization library
 * to secure application from untrusted input. When Java serialization is
 * used to exchange information between a client and a server, attackers
 * can replace the legitimate serialized stream with malicious data.
 * SerialKiller inspects Java classes during naming resolution and allows
 * a combination of blacklisting/whitelisting to secure your application.
 * <p>
 * Dual-Licensed Software: Apache v2.0 and GPL v2.0
 */
package org.nibblesec.tools;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class SerialKiller extends ObjectInputStream {
    // TODO: Should SEVERE logs be WARNINGS?

    // DONE:
    // Fix static issues
    // Create tests!
    // Allow exception message to contain class name (proper way) + tell if it's a whitelist/blacklist hit?
    // Fix config reload issues
    //  - Move config out of this class?
    // Fix regexp caching (see own issue in upstream). Need to be done per config or globally.

    // TODO: Does it make sense to use JDK logging, when the project depends on commons-logging?
    private static final Logger LOGGER = Logger.getLogger(SerialKiller.class.getName());

    private static final Map<String, Configuration> configs = new ConcurrentHashMap<>();

    private final Configuration config;
    private final boolean profiling;

    /**
     * SerialKiller constructor, returns instance of ObjectInputStream
     *
     * @param inputStream The original InputStream, used by your service to receive serialized objects
     * @param configFile The location of the config file (absolute path)
     * @throws java.io.IOException File I/O exception
     * @throws org.apache.commons.configuration.ConfigurationException Config exception
     */
    public SerialKiller(final InputStream inputStream, final String configFile) throws IOException, ConfigurationException {
        super(inputStream);

        config = configs.computeIfAbsent(configFile, Configuration::new);

        profiling = config.isProfiling();

        if (config.isLogging()) {
            // TODO: Do we need to do this in code?
            Handler fileHandler = new FileHandler(config.logFile(), true);
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        }
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass serialInput) throws IOException, ClassNotFoundException {
        // Enforce SerialKiller's blacklist
        for (Pattern blackPattern : config.blacklist()) {
            Matcher blackMatcher = blackPattern.matcher(serialInput.getName());

            if (blackMatcher.find()) {
                if (profiling) {
                    // Reporting mode
                    LOGGER.log(Level.FINE, "Blacklist match: ''{0}''", serialInput.getName());
                } else {
                    // Blocking mode
                    LOGGER.log(Level.SEVERE, "Blocked by blacklist ''{0}''. Match found for ''{1}''", new Object[]{blackPattern.pattern(), serialInput.getName()});
                    throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (blacklist)");
                }
            }
        }

        // Enforce SerialKiller's whitelist
        boolean safeClass = false;

        for (Pattern whitePattern : config.whitelist()) {
            Matcher whiteMatcher = whitePattern.matcher(serialInput.getName());

            if (whiteMatcher.find()) {
                safeClass = true;

                if (profiling) {
                    // Reporting mode
                    LOGGER.log(Level.FINE, "Whitelist match: ''{0}''", serialInput.getName());
                }

                // We have found a whitelist match, no need to continue
                break;
            }
        }

        if (!safeClass && !profiling) {
            // Blocking mode
            LOGGER.log(Level.SEVERE, "Blocked by whitelist. No match found for ''{0}''", serialInput.getName());
            throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (non-whitelist)");
        }

        return super.resolveClass(serialInput);
    }

    static final class Configuration {
        private final XMLConfiguration config;

        private PatternList blacklist;
        private PatternList whitelist;

        Configuration(final String configPath) {
            try {
                config = new XMLConfiguration(configPath);

                FileChangedReloadingStrategy reloadStrategy = new FileChangedReloadingStrategy();
                reloadStrategy.setRefreshDelay(config.getLong("refresh", 6000));
                config.setReloadingStrategy(reloadStrategy);
                // TODO: Rethink this, as reload checks happen on propery access only...
                // https://commons.apache.org/proper/commons-configuration/userguide_v1.10/howto_filebased.html#Automatic_Reloading
                config.addConfigurationListener(event -> init(config));

                init(config);
            }
            catch (ConfigurationException | PatternSyntaxException e) {
                throw new IllegalStateException("SerialKiller not properly configured: " + e.getMessage(), e);
            }
        }

        private void init(final XMLConfiguration config) {
            blacklist = new PatternList(config.getStringArray("blacklist.regexp"));
            whitelist = new PatternList(config.getStringArray("whitelist.regexp"));
        }

        boolean isLogging() {
            return config.getBoolean("logging.enabled", true);
        }

        boolean isProfiling() {
            return config.getBoolean("mode.profiling", false);
        }

        Iterable<Pattern> blacklist() {
            return blacklist;
        }

        Iterable<Pattern> whitelist() {
            return whitelist;
        }

        String logFile() {
            return config.getString("logging.logfile", "serialkiller.log");
        }
    }

    static final class PatternList implements Iterable<Pattern> {
        private final Pattern[] patterns;

        PatternList(final String... regExps) {
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
}