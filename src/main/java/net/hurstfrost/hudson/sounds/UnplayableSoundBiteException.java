/**
 * 
 */
package net.hurstfrost.hudson.sounds;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor;

@SuppressWarnings("serial")
public class UnplayableSoundBiteException extends Exception {
	private final HudsonSoundsDescriptor.SoundBite soundBite;

	public HudsonSoundsDescriptor.SoundBite getSoundBite() {
		return soundBite;
	}

	public UnplayableSoundBiteException(HudsonSoundsDescriptor.SoundBite bite, Exception e) {
		super(e);
		soundBite = bite;
	}

	public UnplayableSoundBiteException(String message, Exception e) {
		super(message, e);
		soundBite = null;
	}

	public UnplayableSoundBiteException(String message) {
		super(message);
		soundBite = null;
	}
}