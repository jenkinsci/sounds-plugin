package net.hurstfrost.hudson.sounds;

// import static org.easymock.EasyMock.*;

import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.Cookie;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

import hudson.model.Hudson;
import jenkins.model.Jenkins;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.PLAY_METHOD;
import net.hurstfrost.hudson.sounds.SoundsAgentAction.SoundsAgentActionDescriptor;

import org.junit.Before;
import org.junit.Rule;
// import org.easymock.EasyMock;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class SoundsAgentActionTest {
	private SoundsAgentAction instance;
	private SoundsAgentActionDescriptor descriptor;
	private StaplerRequest request;
	private StaplerResponse response;

	@Rule private JenkinsRule j = new JenkinsRule();
/*
	@Before
	protected void before() throws Exception {
		descriptor = (SoundsAgentActionDescriptor) Jenkins.get().getDescriptor("SoundsAgentAction");
		instance = new SoundsAgentAction();
		
		descriptor.version = 10;
		
		request = createMock(StaplerRequest.class);
		response = createMock(StaplerResponse.class);
		
		HudsonSoundsNotifier.getSoundsDescriptor().setPlayMethod(PLAY_METHOD.BROWSER);
	}

	public void testCancelSounds() {
		// given:
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		expect(request.getCookies()).andReturn(new Cookie[0]).times(2);
		response.addCookie((Cookie) anyObject());
		
		// when:
		replay(request, response);
		instance.doCancelSounds();
		JSONHttpResponse jsonResponse = instance.doGetSounds(request, response, 10);
		
		// then:
		verify(request, response);
		assertFalse(jsonResponse.jsonObject.containsKey("play"));
		assertEquals(13, jsonResponse.jsonObject.optLong("v"));
		assertTrue(jsonResponse.jsonObject.optBoolean("x"));
	}
	
	public void testGetSoundsWithNoVersionParameterOrCookie() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		expect(request.getCookies()).andReturn(new Cookie[0]).times(2);
		response.addCookie((Cookie) anyObject());
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, null);
		
		assertEquals(12, jsonResponse.jsonObject.getInt("v"));
		assertNull(jsonResponse.jsonObject.opt("play"));
		
		verify(request, response);
	}
	
	public void testGetSoundsWithCookieButNoVersionParameter() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		Cookie cookie = new Cookie("SoundsAgentActionDescriptorVersion", "10");
		expect(request.getCookies()).andReturn(new Cookie[] { cookie }).times(3);
		response.addCookie((Cookie) anyObject());
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, null);
		
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		
		verify(request, response);
	}
	
	public void testGetSoundsReturnsNoPlayedSounds() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		response.addCookie((Cookie) anyObject());
		expect(request.getCookies()).andReturn(new Cookie[0]).times(2);
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 12);
		
		assertEquals(12, jsonResponse.jsonObject.getInt("v"));
		assertNull(jsonResponse.jsonObject.opt("play"));
		
		verify(request, response);
	}
	
	public void testGetSoundsReturnsUnplayedSounds() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		response.addCookie((Cookie) anyObject());
		expect(request.getCookies()).andReturn(new Cookie[0]).times(2);
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		
		verify(request, response);
	}
	
	public void testGetSoundsReturnsMutedWhenMutedLocally() {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		response.addCookie((Cookie) anyObject());
		
		expect(request.getCookies()).andReturn(new Cookie[] { new Cookie(SoundsAgentAction.MUTE_COOKIE_NAME, "muted") }).times(2);

		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(-1, jsonResponse.jsonObject.getInt("v"));
		assertFalse(jsonResponse.jsonObject.containsKey("play"));
		assertEquals(60000, jsonResponse.jsonObject.getInt("p"));
		
		verify(request, response);
	}
	
	public void testGetSoundsHasSyncDelay() throws Exception {
		descriptor.addSound("sound1", null);
		
		response.addCookie((Cookie) anyObject());
		expectLastCall().times(3);
		expect(request.getCookies()).andReturn(new Cookie[0]).times(6);
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		int	expectedDelay = 2200;
		
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);
		
		Thread.sleep(500);
		expectedDelay -= 500;
		
		jsonResponse = instance.doGetSounds(request, response, 10);
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);
		
		Thread.sleep(500);
		expectedDelay -= 500;
		
		jsonResponse = instance.doGetSounds(request, response, 10);
		assertEquals(11, jsonResponse.jsonObject.getInt("v"));
		assertEquals("sound1", jsonResponse.jsonObject.getString("play"));
		assertTrue(String.format("d=%d", jsonResponse.jsonObject.getLong("d")), expectedDelay >= jsonResponse.jsonObject.getLong("d") && jsonResponse.jsonObject.getLong("d") > expectedDelay-100);
		
		verify(request, response);
	}
	
	public void testGetSoundsInImmediateAreNotDelayed() throws Exception {
		descriptor.addSound("sound1", 0);
		
		response.addCookie((Cookie) anyObject());
		expect(request.getCookies()).andReturn(new Cookie[0]).times(2);
		
		replay(request, response);
		
		JSONHttpResponse	jsonResponse = instance.doGetSounds(request, response, 10);
		
		assertEquals(0, jsonResponse.jsonObject.optLong("d"));
		
		verify(request, response);
	}
	
	public void testSoundAtOffset() throws Exception {
		descriptor.addSound("sound1", 0);
		descriptor.addSound("sound2", 0);
		
		assertNull(descriptor.soundAtOffset(9));
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertNull(descriptor.soundAtOffset(12));
	}
	
	public void testSoundsAreRemovedAfter5s() throws Exception {
		descriptor.addSound("sound1", 0);
		Thread.sleep(1000);
		descriptor.addSound("sound2", 0);
		
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertEquals(10, descriptor.version);
		assertEquals(2, descriptor.wavsToPlay.size());
		
		Thread.sleep(SoundsAgentAction.SOUND_QUEUE_EXPIRATION_PERIOD_MS - 1000 + 100);
		// After just over 4s sound1 should still be in the list, but expired
		assertEquals("sound1", descriptor.soundAtOffset(10).getUrl(null));
		assertTrue(descriptor.soundAtOffset(10).expired(0));
		// sound2 should be valid
		assertEquals("sound2", descriptor.soundAtOffset(11).getUrl(null));
		assertFalse(descriptor.soundAtOffset(11).expired(0));
		assertEquals(10, descriptor.version);
		assertEquals(2, descriptor.wavsToPlay.size());
		
		Thread.sleep(SoundsAgentAction.EXPIRY_EXTENSION);
		// Now sound1 should have gone
		assertNull(descriptor.soundAtOffset(10));
		assertEquals(11, descriptor.version);
		assertEquals(1, descriptor.wavsToPlay.size());
		
		Thread.sleep(1000);
		assertNull(descriptor.soundAtOffset(12));
		assertEquals(0, descriptor.wavsToPlay.size());
		assertEquals(12, descriptor.version);
	}
	
	/*
	public void testAudioFormats() throws Exception {
		String[]	urls = new String[] {
			"file:///System/Library/Sounds/Tink.aiff",
			"file:///Users/ed/Desktop/atttts.wav",
			"file:///Users/ed/Desktop/bella.wav",
		};
		
		for (String url : urls) {
			AudioInputStream source = null;
			try {
				source = AudioSystem.getAudioInputStream(new URL(url).openStream());
			} catch (UnsupportedAudioFileException e) {
				// Invalid format
			}
			System.out.println(url + " : " + (source!=null?source.getFormat():"null"));
			
			try {
				Info info = new DataLine.Info(SourceDataLine.class, source.getFormat());
				SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

				line.open();
				line.start();
				byte[]	buffer = new byte[8096];
				while (true) {
					int read = source.read(buffer);
					if (read <= 0) break;
					line.write(buffer, 0, read);
				}
				line.drain();
				line.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void testPlaySound() throws Exception {
		InputStream inputStream = new URL("file:///System/Library/Sounds/Tink.aiff").openStream();
		instance.playSound(inputStream, true);
		
		response.addCookie((Cookie) anyObject());
		
		replay(request, response);
		JSONHttpResponse sound = instance.doGetSounds(request, response, 10);
		System.out.println("play="+sound.jsonObject.getString("play"));
		verify(request, response);
	}*/
}
