package net.hurstfrost.hudson.sounds;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;

public class SoundsBuildTaskTest {
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

        webClient.login("user");

        HtmlPage page;

        page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/");
        assertEquals("Sound played successfully", page.asText());

        page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=NO_SUCH_SOUND");
        assertEquals("Sound failed : net.hurstfrost.hudson.sounds.UnplayableSoundBiteException : No such sound.", page.asText());

        page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=file://nonexistantfile");
        assertEquals("Resource not found or not readable", page.asText());

        page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=http://localhost:8080/");
        assertEquals("", page.asText());
    }

    @Test
    public void directHttpDescriptorAccessWithoutPermission() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();

        webClient.assertFails("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/", HttpURLConnection.HTTP_FORBIDDEN);
        webClient.assertFails("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=EXPLODE", HttpURLConnection.HTTP_FORBIDDEN);
        webClient.assertFails("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/checkSoundUrl?soundUrl=file://nonexistantfile", HttpURLConnection.HTTP_FORBIDDEN);
    }
}
