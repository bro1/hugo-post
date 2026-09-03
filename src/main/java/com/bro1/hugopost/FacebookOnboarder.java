package com.bro1.hugopost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class FacebookOnboarder {

    public static class SpecDefaults {
        public String appId = "";
        public String appSecret = "";
        public String clientToken = "";
    }

    public static SpecDefaults loadSpecDefaults() {
        SpecDefaults defaults = new SpecDefaults();

        // 1. Prioritize reading config from ~/.hugopost
        try {
            FacebookConfig config = FacebookConfig.loadFromHomeDir();
            if (config.getAppId() != null && !config.getAppId().isEmpty()) {
                defaults.appId = config.getAppId();
            }
            if (config.getAppSecret() != null && !config.getAppSecret().isEmpty()) {
                defaults.appSecret = config.getAppSecret();
            }
            if (config.getClientToken() != null && !config.getClientToken().isEmpty()) {
                defaults.clientToken = config.getClientToken();
            }
        } catch (Exception ignored) {}

        // 2. Fallback to parsing specs/f-02 facebook oauth onboarding.md if values are empty
        if (defaults.appId.isEmpty() || defaults.appSecret.isEmpty() || defaults.clientToken.isEmpty()) {
            File file = new File("specs/f-02 facebook oauth onboarding.md");
            if (!file.exists()) {
                file = new File(System.getProperty("user.dir"), "specs/f-02 facebook oauth onboarding.md");
            }
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    boolean inSpecifics = false;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.equalsIgnoreCase("## Specifics")) {
                            inSpecifics = true;
                            continue;
                        }
                        if (inSpecifics) {
                            String lower = line.toLowerCase();
                            if (defaults.appId.isEmpty() && lower.startsWith("app id")) {
                                defaults.appId = line.substring(6).trim();
                            } else if (defaults.appSecret.isEmpty() && lower.startsWith("app secret")) {
                                defaults.appSecret = line.substring(10).trim();
                            } else if (defaults.clientToken.isEmpty() && lower.startsWith("client token")) {
                                defaults.clientToken = line.substring(12).trim();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return defaults;
    }

    @FunctionalInterface
    public interface HttpSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    public static class PageInfo {
        private final String id;
        private final String name;
        private final String accessToken;

        public PageInfo(String id, String name, String accessToken) {
            this.id = id;
            this.name = name;
            this.accessToken = accessToken;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }

    private final HttpSender httpSender;

    public FacebookOnboarder() {
        this(req -> HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()));
    }

    public FacebookOnboarder(HttpSender httpSender) {
        this.httpSender = httpSender;
    }

    /**
     * Step 1: Exchange short-lived user access token for a long-lived user access token.
     */
    public String exchangeForLongLivedToken(String appId, String appSecret, String shortLivedToken) throws Exception {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("App ID cannot be empty.");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("App Secret cannot be empty.");
        }
        if (shortLivedToken == null || shortLivedToken.isBlank()) {
            throw new IllegalArgumentException("Short-lived User Access Token cannot be empty.");
        }

        String url = String.format(
            "https://graph.facebook.com/v26.0/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
            URLEncoder.encode(appId.trim(), StandardCharsets.UTF_8),
            URLEncoder.encode(appSecret.trim(), StandardCharsets.UTF_8),
            URLEncoder.encode(shortLivedToken.trim(), StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        HttpResponse<String> response = httpSender.send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Failed to exchange long-lived token (HTTP " + response.statusCode() + "): " + response.body());
        }

        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(response.body());
        if (map == null || !map.containsKey("access_token")) {
            throw new IOException("Response did not contain access_token: " + response.body());
        }

        return String.valueOf(map.get("access_token"));
    }

    /**
     * Step 2: Fetch managed Facebook pages for the long-lived user token.
     */
    public List<PageInfo> fetchManagedPages(String longLivedToken) throws Exception {
        return fetchManagedPages(longLivedToken, null);
    }

    public List<PageInfo> fetchManagedPages(String longLivedToken, String targetPageId) throws Exception {
        if (longLivedToken == null || longLivedToken.isBlank()) {
            throw new IllegalArgumentException("Long-lived User Access Token cannot be empty.");
        }

        String url = "https://graph.facebook.com/v19.0/me/accounts?access_token=" +
            URLEncoder.encode(longLivedToken.trim(), StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        HttpResponse<String> response = httpSender.send(request);
        List<PageInfo> pages = new ArrayList<>();

        if (response.statusCode() == 200) {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(response.body());

            if (map != null && map.get("data") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");
                for (Map<String, Object> item : data) {
                    String id = String.valueOf(item.get("id"));
                    String name = item.get("name") != null ? String.valueOf(item.get("name")) : id;
                    String token = String.valueOf(item.get("access_token"));
                    pages.add(new PageInfo(id, name, token));
                }
            }
        }

        // If /me/accounts returned empty pages and a target Page ID is provided, attempt direct fetch
        if (pages.isEmpty() && targetPageId != null && !targetPageId.isBlank()) {
            PageInfo directPage = fetchPageById(targetPageId, longLivedToken);
            if (directPage != null) {
                pages.add(directPage);
            }
        }

        if (pages.isEmpty()) {
            throw new IOException(
                "No managed Facebook pages found for this user token.\n\n" +
                "Troubleshooting Steps:\n" +
                "1. Ensure 'pages_show_list', 'pages_read_engagement', and 'pages_manage_posts' permissions were selected when generating the User Access Token in Meta Graph API Explorer.\n" +
                "2. Confirm that your Facebook account has Admin or Editor access to the Facebook Page.\n" +
                "3. If you have the Page ID, add 'facebook.page_id=YOUR_PAGE_ID' to ~/.hugopost or enter it in the Page ID field."
            );
        }

        return pages;
    }

    public PageInfo fetchPageById(String pageId, String userToken) {
        try {
            String url = String.format(
                "https://graph.facebook.com/v19.0/%s?fields=access_token,name&access_token=%s",
                URLEncoder.encode(pageId.trim(), StandardCharsets.UTF_8),
                URLEncoder.encode(userToken.trim(), StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = httpSender.send(request);
            if (response.statusCode() == 200) {
                Yaml yaml = new Yaml();
                Map<String, Object> map = yaml.load(response.body());
                if (map != null && map.containsKey("id")) {
                    String id = String.valueOf(map.get("id"));
                    String name = map.get("name") != null ? String.valueOf(map.get("name")) : id;
                    String token = map.get("access_token") != null ? String.valueOf(map.get("access_token")) : userToken;
                    return new PageInfo(id, name, token);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Step 3: Save configuration to ~/.hugopost.
     */
    public static void saveConfig(String appId, String appSecret, String clientToken, String pageId, String accessToken) throws IOException {
        File configFile = new File(System.getProperty("user.home"), ".hugopost");
        StringBuilder sb = new StringBuilder();
        sb.append("# Hugo Post - Facebook Configuration\n");
        sb.append("facebook.app_id=").append(appId != null ? appId.trim() : "").append("\n");
        if (appSecret != null && !appSecret.isBlank()) {
            sb.append("facebook.app_secret=").append(appSecret.trim()).append("\n");
        }
        if (clientToken != null && !clientToken.isBlank()) {
            sb.append("facebook.client_token=").append(clientToken.trim()).append("\n");
        }
        sb.append("facebook.page_id=").append(pageId.trim()).append("\n");
        sb.append("facebook.access_token=").append(accessToken.trim()).append("\n");

        try (FileWriter writer = new FileWriter(configFile, false)) {
            writer.write(sb.toString());
        }
    }

    public static void saveConfig(String appId, String pageId, String accessToken) throws IOException {
        saveConfig(appId, "", "", pageId, accessToken);
    }

    /**
     * Run full automated onboarding flow for given credentials.
     */
    public PageInfo automateOnboarding(String appId, String appSecret, String shortLivedToken, String targetPageId) throws Exception {
        String longLivedToken = exchangeForLongLivedToken(appId, appSecret, shortLivedToken);
        List<PageInfo> pages = fetchManagedPages(longLivedToken);

        PageInfo selectedPage = null;
        if (targetPageId != null && !targetPageId.isBlank()) {
            for (PageInfo p : pages) {
                if (p.getId().equalsIgnoreCase(targetPageId.trim())) {
                    selectedPage = p;
                    break;
                }
            }
        }

        if (selectedPage == null) {
            selectedPage = pages.get(0);
        }

        saveConfig(appId, selectedPage.getId(), selectedPage.getAccessToken());
        return selectedPage;
    }
}
