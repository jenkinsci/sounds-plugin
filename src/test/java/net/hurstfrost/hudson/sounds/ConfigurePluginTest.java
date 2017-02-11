package net.hurstfrost.hudson.sounds;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;

public class ConfigurePluginTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test() throws IOException, SAXException, InterruptedException {
        /* Still WIP */
        HtmlPage page = j.createWebClient().goTo("sounds");

        page.getElementById("yui-gen3-button").click();

        Page jsonPage = j.createWebClient().goTo("sounds/getSounds", "application/json");
        jsonPage.getWebResponse().getContentAsString();
    }
}
