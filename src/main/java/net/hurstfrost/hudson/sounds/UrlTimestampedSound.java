/**
 * 
 */
package net.hurstfrost.hudson.sounds;

import hudson.model.Hudson;

import java.net.URL;

class UrlTimestampedSound extends AbstractTimestamptedSound {
	protected final URL	url;
	
	public UrlTimestampedSound(URL u, long a) {
		super(a);
		url = u;
	}

	public URL getRawUrl() {
		return url;
	}

	@Override
	public String getUrl(Integer version) {
		if (url.getProtocol().toLowerCase().startsWith("http")) {
			return url.toString();
		}
		
		return Hudson.getInstance().getRootUrl() + "sounds/sound?v=" + version;
	}
}
