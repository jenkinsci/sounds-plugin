/**
 * 
 */
package net.hurstfrost.hudson.sounds;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor;

@SuppressWarnings("serial")
public class UnplayableSoundBiteException extends Exception {
	private final HudsonSoundsDescriptor.SoundBite soundBite;
    private final String sound;

	public UnplayableSoundBiteException(HudsonSoundsDescriptor.SoundBite bite, Exception e) {
		super(e);
		soundBite = bite;
        this.sound = null;
    }

	public UnplayableSoundBiteException(String sound, Exception e) {
		super(e);
        this.sound = sound;
        soundBite = null;
	}

	public UnplayableSoundBiteException(String sound) {
        this.sound = sound;
		soundBite = null;
	}

    public String getSound() {
        return sound;
    }

    public HudsonSoundsDescriptor.SoundBite getSoundBite() {
        return soundBite;
    }
}