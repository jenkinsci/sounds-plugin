package net.hurstfrost.hudson.sounds;

import hudson.model.Hudson;
import hudson.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.sound.sampled.UnsupportedAudioFileException;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.SoundEvent;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.UnplayableSoundBiteException;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor.SoundBite;

import org.jvnet.hudson.test.HudsonTestCase;

public class HudsonSoundsNotifierTest extends HudsonTestCase {
	private HudsonSoundsDescriptor descriptor;
	
	private String TEST_ARCHIVE_URL;

	private HudsonSoundsNotifier instance;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TEST_ARCHIVE_URL = HudsonSoundsNotifier.class.getResource("/test-sound-archive.zip").toString();
		descriptor = (HudsonSoundsDescriptor) Hudson.getInstance().getDescriptor("HudsonSoundsNotifier");
		instance = new HudsonSoundsNotifier();
	}
	
	public void testRebuildIndex() throws Exception {
		TreeMap<String, SoundBite> index = HudsonSoundsDescriptor.rebuildSoundsIndex(TEST_ARCHIVE_URL);
		
		assertNotNull(index);
		assertEquals(5, index.size());
		
		assertNull(index.get(""));
		assertNotNull(index.get("YAWN"));
		assertEquals("YAWN", index.get("YAWN").id);
		assertEquals("sound-bite-library/YAWN.wav", index.get("YAWN").entryName);
		assertEquals(TEST_ARCHIVE_URL, index.get("YAWN").url);
	}
	
	public void testGetSounds() throws Exception {
		descriptor.setSoundArchive(TEST_ARCHIVE_URL);
		
		assertNotNull(descriptor.getSounds());
		assertEquals(5, descriptor.getSounds().size());
	}
	
	public void testPlaySound() throws Exception {
		descriptor.setSoundArchive(TEST_ARCHIVE_URL);
		descriptor.getSounds();
		
		try {
			descriptor.playSound("YAWN");
		} catch (UnplayableSoundBiteException e) {
			// No guarantee that machine running tests can play sounds, so swallow this.
			e.printStackTrace();
		}
	}
	
	public void testPlayMp3() {
		descriptor.setSoundArchive(TEST_ARCHIVE_URL);
		descriptor.getSounds();
		
		try {
			try {
				descriptor.playSound("burp");
			} catch (UnplayableSoundBiteException e) {
				// As expected
				assertTrue(e.getCause() instanceof UnsupportedAudioFileException);
			}
		} catch (Exception e) {
			// No guarantee that machine running tests can play sounds, so swallow this.
		}
	}
	
	public void testSetSoundEvents() throws Exception {
		// Force reindex
		descriptor.setSoundArchive(TEST_ARCHIVE_URL);
		descriptor.getSounds();
		
		ArrayList<SoundEvent> formSubmision = new ArrayList<SoundEvent>();
		
		// Invalid: no sound ID
		formSubmision.add(new SoundEvent(null, Result.ABORTED.toString(), true, false, false, false, false));
		// Valid: missing sound ID, but don't discard
		formSubmision.add(new SoundEvent("no such sound", Result.ABORTED.toString(), true, false, false, false, false));
		// Invalid: invalid build result
		formSubmision.add(new SoundEvent("YAWN", null, true, false, false, false, false));
		formSubmision.add(new SoundEvent("YAWN", "no such result", true, false, false, false, false));
		// Invalid: no previous build state
		formSubmision.add(new SoundEvent("JSneeze", Result.FAILURE.toString(), false, false, false, false, false));
		// Valid
		formSubmision.add(new SoundEvent("YAWN", Result.SUCCESS.toString(), false, true, false, true, false));
		formSubmision.add(new SoundEvent("doh", Result.NOT_BUILT.toString(), true, false, true, false, true));
		
		instance.setSoundEvents(formSubmision);
		
		List<SoundEvent> events = instance.getSoundEvents();
		
		assertEquals(3, events.size());
		assertEquals("no such sound", events.get(0).getSoundId());
		assertEquals("YAWN", events.get(1).getSoundId());
		assertEquals("doh", events.get(2).getSoundId());
	}
	
	public void testGetSoundEventFor() throws Exception {
		ArrayList<SoundEvent> events = new ArrayList<SoundEvent>();
		
		events.add(new SoundEvent("JSneeze", Result.FAILURE.toString(), true, false, false, false, false));
		events.add(new SoundEvent("YAWN", Result.SUCCESS.toString(), false, false, false, true, false));
		events.add(new SoundEvent("doh", Result.UNSTABLE.toString(), false, false, false, false, true));
		
		// Force reindex
		descriptor.setSoundArchive(TEST_ARCHIVE_URL);
		descriptor.getSounds();
		
		instance.setSoundEvents(events);
		
		assertEquals("JSneeze", instance.getSoundEventFor(Result.FAILURE, Result.NOT_BUILT).getSoundId());
		assertEquals("JSneeze", instance.getSoundEventFor(Result.FAILURE, null).getSoundId());
		assertEquals("YAWN", instance.getSoundEventFor(Result.SUCCESS, Result.UNSTABLE).getSoundId());
		assertEquals("doh", instance.getSoundEventFor(Result.UNSTABLE, Result.SUCCESS).getSoundId());
		assertNull("Should not play on Fa->Fa", instance.getSoundEventFor(Result.UNSTABLE, Result.FAILURE));
		assertNull("Should not play on NB->Fa", instance.getSoundEventFor(Result.UNSTABLE, Result.NOT_BUILT));
		assertNull("Should not play on Ab->Fa", instance.getSoundEventFor(Result.UNSTABLE, Result.ABORTED));
		assertNull("Should not play on Un->Fa", instance.getSoundEventFor(Result.UNSTABLE, Result.UNSTABLE));
	}
}
