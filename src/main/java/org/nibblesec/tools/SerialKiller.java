/*
 * SerialKiller.java
 *
 * Copyright (c) 2015-2016 Luca Carettoni
 *
 * SerialKiller is an easy-to-use look-ahead Java deserialization library
 * to secure application from untrusted input. When Java serialization is
 * used to exchange information between a client and a server, attackers
 * can replace the legitimate serialized stream with malicious data.
 * SerialKiller inspects Java classes during naming resolution and allows
 * a combination of blacklisting/whitelisting to secure your application.
 *
 * Dual-Licensed Software: Apache v2.0 and GPL v2.0
 */
package org.nibblesec.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialKiller extends ObjectInputStream {

    private static final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();

    private final Configuration config;
    private final boolean profiling;
    private final LoggingProvider loggingProvider;

    public SerialKiller(final InputStream inputStream, final String configFile, LoggingProvider loggingProvider) throws IOException {
        this(inputStream, configs.computeIfAbsent(configFile, FileConfiguration::new), loggingProvider);
    }

    public SerialKiller(final InputStream inputStream, final Configuration config, LoggingProvider loggingProvider) throws IOException {
    	super(inputStream);
    	this.config = config;
        this.profiling = config.isProfiling();
        this.loggingProvider = loggingProvider;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass serialInput) throws IOException, ClassNotFoundException {
        config.reloadIfNeeded();

        // Enforce SerialKiller's blacklist
        for (Pattern blackPattern : config.blacklist()) {
            Matcher blackMatcher = blackPattern.matcher(serialInput.getName());

            if (blackMatcher.find()) {
                if (profiling) {
                    // Reporting mode
                	loggingProvider.logInfo(String.format("Blacklist match: '%s'", serialInput.getName()));
                } else {
                    // Blocking mode
                	loggingProvider.logError(String.format("Blocked by blacklist '%s'. Match found for '%s'", new Object[] {blackPattern.pattern(), serialInput.getName()}));
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
                	loggingProvider.logInfo(String.format("Whitelist match: '%s'", serialInput.getName()));
                }

                // We have found a whitelist match, no need to continue
                break;
            }
        }

        if (!safeClass) {
        	if (profiling) {
        		loggingProvider.logError(String.format("Class would have been blocked by whitelist. No match found for '%s'", serialInput.getName()));
        	} else {
        		// Blocking mode
        		loggingProvider.logError(String.format("Blocked by whitelist. No match found for '%s'", serialInput.getName()));
        		throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (non-whitelist)");
        	}
        }

        return super.resolveClass(serialInput);
    }
}
