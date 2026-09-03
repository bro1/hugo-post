package com.bro1.hugopost;

import java.io.File;
import java.io.FileWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FacebookConfigTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLoadPropertiesFormat() throws Exception {
        File configFile = tempFolder.newFile(".hugopost");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("facebook.page_id=123456789\n");
            writer.write("facebook.app_id=987654321\n");
            writer.write("facebook.app_secret=secret_abc_123\n");
            writer.write("facebook.client_token=token_def_456\n");
            writer.write("facebook.access_token=EAACEdEose0cBA...\n");
        }

        // Temporarily override user.home to tempFolder
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempFolder.getRoot().getAbsolutePath());
            FacebookConfig config = FacebookConfig.loadFromHomeDir();
            Assert.assertEquals("123456789", config.getPageId());
            Assert.assertEquals("987654321", config.getAppId());
            Assert.assertEquals("secret_abc_123", config.getAppSecret());
            Assert.assertEquals("token_def_456", config.getClientToken());
            Assert.assertEquals("EAACEdEose0cBA...", config.getAccessToken());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testLoadYamlFormat() throws Exception {
        File configFile = tempFolder.newFile(".hugopost");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("facebook:\n");
            writer.write("  page_id: \"1122334455\"\n");
            writer.write("  app_id: \"5544332211\"\n");
            writer.write("  access_token: \"token_xyz_123\"\n");
        }

        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempFolder.getRoot().getAbsolutePath());
            FacebookConfig config = FacebookConfig.loadFromHomeDir();
            Assert.assertEquals("1122334455", config.getPageId());
            Assert.assertEquals("5544332211", config.getAppId());
            Assert.assertEquals("token_xyz_123", config.getAccessToken());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingConfigFile() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempFolder.getRoot().getAbsolutePath());
            FacebookConfig.loadFromHomeDir();
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
