package net.hurstfrost.hudson.sounds;

public class CancelSoundsMarker extends AbstractTimestamptedSound {

	public CancelSoundsMarker(long at) {
		super(at);
	}

	@Override
	public String getUrl(Integer version) {
		return null;
	}

	@Override
	public boolean isCancel() {
		return true;
	}
}
