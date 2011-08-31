/**
 * 
 */
package net.hurstfrost.hudson.sounds;

class ImmediateDataTimestampedSound extends AbstractTimestamptedSound {
	protected final String	sound;
	
	public ImmediateDataTimestampedSound(String s, long a) {
		super(a);
		sound = s;
	}

	@Override
	public String getUrl(Integer version) {
		return sound;
	}
	
	@Override
	public boolean isCancel() {
		return false;
	}
}