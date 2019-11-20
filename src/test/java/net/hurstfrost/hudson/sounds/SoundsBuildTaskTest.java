package net.hurstfrost.hudson.sounds;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;

public class SoundsBuildTaskTest extends TestWithTools {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void before() throws Exception {
        j.createFreeStyleProject("freestyle");

        JenkinsRule.DummySecurityRealm securityRealm = j.createDummySecurityRealm();
        j.jenkins.setSecurityRealm(securityRealm);
        j.jenkins.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
    }

    @Test
    public void directHttpDescriptorAccessWithPermission() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page;

        webClient.login("user");

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/");
        assertEquals("Sound played successfully", page.asText());

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=NO_SUCH_SOUND");
        assertEquals("Sound failed : net.hurstfrost.hudson.sounds.UnplayableSoundBiteException : No such sound.", page.asText());

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=file://nonexistantfile");
        assertEquals("Resource not found or not readable", page.asText());
    }

    @Test
    public void directHttpDescriptorAccessWithoutPermission() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();
        HtmlPage page;

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/");
        assertEquals(page.getWebResponse().getStatusCode(), HttpURLConnection.HTTP_FORBIDDEN);

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=EXPLODE");
        assertEquals(page.getWebResponse().getStatusCode(), HttpURLConnection.HTTP_FORBIDDEN);

        page = whyDoesntJenkinsRuleWebClientLetMeDoPostForPage(webClient, "job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=file://nonexistantfile");
        assertEquals(page.getWebResponse().getStatusCode(), HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    public void directHttpDescriptorAccessWithGetDisallowed() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();

        webClient.login("user");

        webClient.assertFails("descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/", HttpURLConnection.HTTP_BAD_METHOD);
        webClient.assertFails("descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=EXPLODE", HttpURLConnection.HTTP_BAD_METHOD);

        webClient.assertFails("descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=file://nonexistantfile", HttpURLConnection.HTTP_BAD_METHOD);
    }
}
