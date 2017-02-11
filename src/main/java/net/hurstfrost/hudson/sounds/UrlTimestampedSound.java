/**
 * 
 */
package net.hurstfrost.hudson.sounds;

import hudson.model.Hudson;
import jenkins.model.Jenkins;

import java.net.URL;

import static net.hurstfrost.hudson.sounds.JenkinsSoundsUtils.getJenkinsInstanceOrDie;

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
		
		return getJenkinsInstanceOrDie().getRootUrl() + "sounds/sound?v=" + version;
	}
	
	@Override
	public boolean isCancel() {
		return false;
	}
}
