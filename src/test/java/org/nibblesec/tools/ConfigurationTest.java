package org.nibblesec.tools;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.Test;

/**
 * ConfigurationTest
 */
public class ConfigurationTest {
    @Test(expected = IllegalStateException.class)
    public void testCreateNull() {
        new FileConfiguration(null);
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateNonExistant() {
        new FileConfiguration("/i/am/pretty-sure/this-file/does-not-exist");
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateNonConfig() throws IOException {
        Path tempFile = Files.createTempFile("sk-", ".tmp");
        new FileConfiguration(tempFile.toAbsolutePath().toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateBadPattern() {
        new FileConfiguration("src/test/resources/broken-pattern.conf");
    }

    @Test
    public void testCreateGood() {
        FileConfiguration configuration = new FileConfiguration("src/test/resources/blacklist-all.conf");

        assertFalse(configuration.isProfiling());
        //@TODO after fixing loggging
        //assertEquals("/tmp/serialkiller.log", configuration.logFile());
        assertEquals(".*", configuration.blacklist().iterator().next().pattern());
        assertEquals("java\\.lang\\..*", configuration.whitelist().iterator().next().pattern());
    }

    @Test
    public void testReload() throws Exception {
        Path tempFile = Files.createTempFile("sk-", ".conf");
        Files.copy(new File("src/test/resources/blacklist-all-refresh-10-ms.conf").toPath(), tempFile, REPLACE_EXISTING);

        FileConfiguration configuration = new FileConfiguration(tempFile.toAbsolutePath().toString());

        assertFalse(configuration.isProfiling());
        assertEquals(".*", configuration.blacklist().iterator().next().pattern());
        assertEquals("java\\.lang\\..*", configuration.whitelist().iterator().next().pattern());

        Files.copy(new File("src/test/resources/whitelist-all.conf").toPath(), tempFile, REPLACE_EXISTING);
        Thread.sleep(1000L); // Wait to ensure the file is fully copied
        Files.setLastModifiedTime(tempFile, FileTime.fromMillis(System.currentTimeMillis())); // Commons configuration watches file modified time
        Thread.sleep(1000L); // Wait to ensure a reload happens

        configuration.reloadIfNeeded(); // Trigger reload

        assertFalse(configuration.blacklist().iterator().hasNext());
        assertEquals(".*", configuration.whitelist().iterator().next().pattern());
    }
}