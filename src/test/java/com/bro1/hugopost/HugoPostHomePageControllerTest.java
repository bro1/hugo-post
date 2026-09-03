package com.bro1.hugopost;

import org.junit.Assert;
import org.junit.Test;

public class HugoPostHomePageControllerTest {

    @Test
    public void testExtractFirstUrlMarkdownLink() {
        String text = "Check out this article [Hugo Post Guide](https://iamnotadoctorbut.wordpress.com/2026/08/post-1) for details.";
        String url = HugoPostHomePageController.extractFirstUrl(text);
        Assert.assertEquals("https://iamnotadoctorbut.wordpress.com/2026/08/post-1", url);
    }

    @Test
    public void testExtractFirstUrlPlainHttp() {
        String text = "Here is an interesting link https://example.com/blog/article.html and more text.";
        String url = HugoPostHomePageController.extractFirstUrl(text);
        Assert.assertEquals("https://example.com/blog/article.html", url);
    }

    @Test
    public void testExtractFirstUrlNoLink() {
        String text = "Just plain text without any URLs or links.";
        String url = HugoPostHomePageController.extractFirstUrl(text);
        Assert.assertNull(url);
    }

    @Test
    public void testExtractFirstUrlNullOrEmpty() {
        Assert.assertNull(HugoPostHomePageController.extractFirstUrl(null));
        Assert.assertNull(HugoPostHomePageController.extractFirstUrl("   "));
    }
}
