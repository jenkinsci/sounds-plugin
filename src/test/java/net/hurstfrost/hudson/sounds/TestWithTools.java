package net.hurstfrost.hudson.sounds;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URL;

public class TestWithTools {
    protected HtmlPage whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(JenkinsRule.WebClient webClient, String relative) throws IOException {
        WebRequest webRequest = new WebRequest(new URL(webClient.getContextPath() + relative), HttpMethod.POST);
        webClient.addCrumb(webRequest);
        Page page = webClient.loadWebResponseInto(webClient.loadWebResponse(webRequest), webClient.getCurrentWindow());
        return (HtmlPage) page;
    }
}
