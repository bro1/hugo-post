package com.bro1.hugopost;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FacebookOnboarderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSaveConfig() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempFolder.getRoot().getAbsolutePath());
            FacebookOnboarder.saveConfig("APP123", "PAGE456", "TOKEN789");

            FacebookConfig config = FacebookConfig.loadFromHomeDir();
            Assert.assertEquals("APP123", config.getAppId());
            Assert.assertEquals("PAGE456", config.getPageId());
            Assert.assertEquals("TOKEN789", config.getAccessToken());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testLoadSpecDefaults() {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempFolder.getRoot().getAbsolutePath());
            FacebookOnboarder.SpecDefaults defaults = FacebookOnboarder.loadSpecDefaults();
            Assert.assertEquals("1933223393902734", defaults.appId);
            Assert.assertEquals("92c2477991e9453d048cfd940f6abd6d", defaults.appSecret);
            Assert.assertEquals("01101125c38e9c07abda01bda76b4798", defaults.clientToken);
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testFetchManagedPagesParsing() throws Exception {
        String mockResponseJson = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"access_token\": \"PAGE_TOKEN_ABC\",\n" +
            "      \"category\": \"Community\",\n" +
            "      \"name\": \"Test Page Name\",\n" +
            "      \"id\": \"9876543210\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        FacebookOnboarder.HttpSender mockSender = req -> new MockHttpResponse<>(200, mockResponseJson);
        FacebookOnboarder onboarder = new FacebookOnboarder(mockSender);

        List<FacebookOnboarder.PageInfo> pages = onboarder.fetchManagedPages("LONG_LIVED_TOKEN");
        Assert.assertEquals(1, pages.size());
        Assert.assertEquals("9876543210", pages.get(0).getId());
        Assert.assertEquals("Test Page Name", pages.get(0).getName());
        Assert.assertEquals("PAGE_TOKEN_ABC", pages.get(0).getAccessToken());
    }

    @Test
    public void testExchangeForLongLivedTokenParsing() throws Exception {
        String mockResponseJson = "{\n" +
            "  \"access_token\": \"EXCHANGED_LONG_LIVED_TOKEN\",\n" +
            "  \"token_type\": \"bearer\"\n" +
            "}";

        FacebookOnboarder.HttpSender mockSender = req -> new MockHttpResponse<>(200, mockResponseJson);
        FacebookOnboarder onboarder = new FacebookOnboarder(mockSender);

        String longLivedToken = onboarder.exchangeForLongLivedToken("APP_123", "SECRET_456", "SHORT_TOKEN");
        Assert.assertEquals("EXCHANGED_LONG_LIVED_TOKEN", longLivedToken);
    }

    private static class MockHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final T bodyContent;

        public MockHttpResponse(int statusCode, T bodyContent) {
            this.statusCode = statusCode;
            this.bodyContent = bodyContent;
        }

        @Override
        public int statusCode() { return statusCode; }

        @Override
        public HttpRequest request() { return null; }

        @Override
        public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);
        }

        @Override
        public T body() { return bodyContent; }

        @Override
        public Optional<SSLSession> sslSession() { return Optional.empty(); }

        @Override
        public URI uri() { return URI.create("https://graph.facebook.com"); }

        @Override
        public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_2; }
    }
}
