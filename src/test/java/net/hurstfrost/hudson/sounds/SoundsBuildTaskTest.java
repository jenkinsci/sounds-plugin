package net.hurstfrost.hudson.sounds;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.User;
import hudson.security.AccessDeniedException2;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.Permission;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.PLAY_METHOD;
import net.hurstfrost.hudson.sounds.SoundsAgentAction.SoundsAgentActionDescriptor;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebApp;

import javax.servlet.http.Cookie;
import java.net.HttpURLConnection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

		webClient.login("configure");

        HtmlPage page;

		page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/");
		assertEquals("Sound played successfully", page.asText());

		page = webClient.goTo("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=NO_SUCH_SOUND");
		assertEquals("Sound failed : net.hurstfrost.hudson.sounds.UnplayableSoundBiteException : No such sound.", page.asText());
	}

    @Test
    public void directHttpDescriptorAccessWithoutPermission() throws Exception {
        JenkinsRule.WebClient webClient = j.createWebClient();

        webClient.assertFails("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testUrl?soundUrl=http://localhost:8080/", HttpURLConnection.HTTP_FORBIDDEN);
        webClient.assertFails("job/freestyle/descriptorByName/net.hurstfrost.hudson.sounds.SoundsBuildTask/testSound?selectedSound=EXPLODE", HttpURLConnection.HTTP_FORBIDDEN);
    }
}
