package com.bro1.hugopost;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.yaml.snakeyaml.Yaml;

public class FacebookConfig {

    private final String pageId;
    private final String appId;
    private final String appSecret;
    private final String clientToken;
    private final String accessToken;

    public FacebookConfig(String pageId, String appId, String appSecret, String clientToken, String accessToken) {
        this.pageId = pageId;
        this.appId = appId;
        this.appSecret = appSecret;
        this.clientToken = clientToken;
        this.accessToken = accessToken;
    }

    public FacebookConfig(String pageId, String appId, String accessToken) {
        this(pageId, appId, "", "", accessToken);
    }

    public String getPageId() {
        return pageId;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public static FacebookConfig loadFromHomeDir() throws Exception {
        File file = new File(System.getProperty("user.home"), ".hugopost");
        if (!file.exists()) {
            throw new IllegalStateException(
                "Config file ~/.hugopost does not exist.\n" +
                "Please follow specs/f-02 facebook oauth onboarding.md to create it."
            );
        }

        String pId = null;
        String aId = null;
        String aSecret = null;
        String cToken = null;
        String token = null;

        // 1. Try standard Java Properties format
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(file)) {
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            pId = props.getProperty("facebook.page_id");
            if (pId == null) pId = props.getProperty("page_id");

            aId = props.getProperty("facebook.app_id");
            if (aId == null) aId = props.getProperty("app_id");

            aSecret = props.getProperty("facebook.app_secret");
            if (aSecret == null) aSecret = props.getProperty("app_secret");

            cToken = props.getProperty("facebook.client_token");
            if (cToken == null) cToken = props.getProperty("client_token");

            token = props.getProperty("facebook.access_token");
            if (token == null) token = props.getProperty("access_token");
        } catch (Exception ignored) {}

        // 2. Fallback to YAML parsing if properties returned null
        if (pId == null || token == null) {
            try (InputStream is = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(is);
                if (map != null) {
                    if (map.containsKey("facebook") && map.get("facebook") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fbMap = (Map<String, Object>) map.get("facebook");
                        if (pId == null && fbMap.get("page_id") != null) {
                            pId = String.valueOf(fbMap.get("page_id"));
                        }
                        if (aId == null && fbMap.get("app_id") != null) {
                            aId = String.valueOf(fbMap.get("app_id"));
                        }
                        if (aSecret == null && fbMap.get("app_secret") != null) {
                            aSecret = String.valueOf(fbMap.get("app_secret"));
                        }
                        if (cToken == null && fbMap.get("client_token") != null) {
                            cToken = String.valueOf(fbMap.get("client_token"));
                        }
                        if (token == null && fbMap.get("access_token") != null) {
                            token = String.valueOf(fbMap.get("access_token"));
                        }
                    } else {
                        if (pId == null && map.get("page_id") != null) {
                            pId = String.valueOf(map.get("page_id"));
                        }
                        if (aId == null && map.get("app_id") != null) {
                            aId = String.valueOf(map.get("app_id"));
                        }
                        if (aSecret == null && map.get("app_secret") != null) {
                            aSecret = String.valueOf(map.get("app_secret"));
                        }
                        if (cToken == null && map.get("client_token") != null) {
                            cToken = String.valueOf(map.get("client_token"));
                        }
                        if (token == null && map.get("access_token") != null) {
                            token = String.valueOf(map.get("access_token"));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        pId = clean(pId);
        aId = clean(aId);
        aSecret = clean(aSecret);
        cToken = clean(cToken);
        token = clean(token);

        if (pId == null || pId.isEmpty()) {
            throw new IllegalStateException("Missing 'facebook.page_id' (or 'page_id') in ~/.hugopost");
        }
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Missing 'facebook.access_token' (or 'access_token') in ~/.hugopost");
        }

        return new FacebookConfig(pId, aId != null ? aId : "", aSecret != null ? aSecret : "", cToken != null ? cToken : "", token);
    }

    private static String clean(String s) {
        if (s == null) return null;
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }
}
