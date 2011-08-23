package net.hurstfrost.hudson.sounds;

public abstract class AbstractTimestamptedSound implements TimestampedSound {

	protected final long playAt;

	public long getPlayAt() {
		return playAt;
	}

	public AbstractTimestamptedSound(long at) {
		playAt = at;
	}

	@Override
	public boolean expired(long expiryExtension) {
		return System.currentTimeMillis() - playAt > SoundsAgentAction.SOUND_QUEUE_EXPIRATION_PERIOD_MS + expiryExtension;
	}
}