package org.nibblesec.tools;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

public class FileConfiguration implements Configuration {
    private final XMLConfiguration config;

    private PatternList blacklist;
    private PatternList whitelist;

    public FileConfiguration(final String configPath) {
        try {
            config = new XMLConfiguration(configPath);

            FileChangedReloadingStrategy reloadStrategy = new FileChangedReloadingStrategy();
            reloadStrategy.setRefreshDelay(config.getLong("refresh", 6000));
            config.setReloadingStrategy(reloadStrategy);
            config.addConfigurationListener(event -> init(config));

            init(config);
        } catch (ConfigurationException | PatternSyntaxException e) {
            throw new IllegalStateException("SerialKiller not properly configured: " + e.getMessage(), e);
        }
    }

    private void init(final XMLConfiguration config) {
        blacklist = new PatternList(config.getStringArray("blacklist.regexps.regexp"));
        whitelist = new PatternList(config.getStringArray("whitelist.regexps.regexp"));
    }

    @Override
    public void reloadIfNeeded() {
        // NOTE: Unfortunately, this will invoke synchronized blocks in Commons Configuration
        config.reload();
    }

    @Override
    public Iterable<Pattern> blacklist() {
        return blacklist;
    }

    @Override
    public Iterable<Pattern> whitelist() {
        return whitelist;
    }

    @Override
    public boolean isProfiling() {
        return config.getBoolean("mode.profiling", false);
    }
}
