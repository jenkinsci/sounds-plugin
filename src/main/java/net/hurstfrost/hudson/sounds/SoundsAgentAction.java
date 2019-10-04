package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.security.Permission;
import hudson.util.FormValidation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.PLAY_METHOD;
import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor.SoundBite;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import static net.hurstfrost.hudson.sounds.JenkinsSoundsUtils.getJenkinsInstanceOrDie;

/*
 * TODO: Secure Sounds properly
 * TODO: JS sounds agent polling rate should be dictated by this class (slow down when disabled by config).
 * TODO: Implement long-poll (configured by plugin config)
 * TODO: Have a way to filter sounds by category (Hudson view?)
 * TODO: Configure poll interval on global config
 * TODO: Adaptive polling (if many clients, slow them down)
 * TODO: Use more efficient audio format (transcode audio according to browser capability)
 * TODO: Provide JSONP to allow sounds to be played on 3rd party page
 * TODO: Add a Hudson build task to play a sound (not just a Notifier)
 * TODO: Convert Speaks! to use Sounds
 * TODO: Safety controls (a way to flush sound queue, and send 'stop playing' to browsers
 * TODO: Make global config allow more than one sound play option.
 */

/**
 * An {@link ExtensionPoint} that queues audio clips and serves them to a JS polling agent in Jenkins pages.
 * 
 * @author Edward Hurst-Frost
 */
@Extension
public class SoundsAgentAction implements RootAction, Describable<SoundsAgentAction> {
	public static final int IMMEDIATE_POLL_INTERVAL = 100;
	public static final int DEFAULT_POLL_INTERVAL = 2000;
	public static final int MUTED_POLL_INTERVAL = 60000;
	public static final long SOUND_QUEUE_EXPIRATION_PERIOD_MS = 5000;
	public static final long EXPIRY_EXTENSION = 1000;	// Gives a browser this long to come back to fetch an audio stream
	public static final int LATENCY_COMPENSATION = 200;
	protected static final String COOKIE_NAME = "SoundsAgentActionDescriptorVersion";
	protected static final String MUTE_COOKIE_NAME = "SoundsAgentAction_mute";
	
	public static final Permission PERMISSION = Permission.CONFIGURE;

	public String getIconFileName() {
		return "/plugin/sounds/icon/s_on_24x24.png";
    }

    public String getUrlName() {
        return "/sounds";
    }

    public String getDisplayName() {
        return "Sounds";
    }

    public SoundsAgentActionDescriptor getDescriptor() {
        return (SoundsAgentActionDescriptor) getJenkinsInstanceOrDie().getDescriptorOrDie(getClass());
    }

    public static ExtensionList<SoundsAgentAction> all() {
        return getJenkinsInstanceOrDie().getExtensionList(SoundsAgentAction.class);
    }

    public String getRootURL() {
    	// Why doesn't ${rootURL} work in script.jelly?
    	return getJenkinsInstanceOrDie().getRootUrl();
    }

    @Extension
    public static final class SoundsAgentActionDescriptor extends Descriptor<SoundsAgentAction> {
		transient protected	List<TimestampedSound>	wavsToPlay = new ArrayList<TimestampedSound>();
    	
    	transient protected int	version;
    	
		protected boolean globalMute;
		
		public SoundsAgentActionDescriptor() {
			load();
		}

        public static SoundsAgentActionDescriptor getDescriptor() {
            return getJenkinsInstanceOrDie().getDescriptorByType(SoundsAgentActionDescriptor.class);
        }

    	@Override
        public String getDisplayName() {
            return clazz.getSimpleName();
        }

		protected synchronized void addSound(String sound, Integer delay) {
			purgeExpiredSounds(EXPIRY_EXTENSION);
			
			wavsToPlay.add(new ImmediateDataTimestampedSound(sound, System.currentTimeMillis() + (delay==null?DEFAULT_POLL_INTERVAL + LATENCY_COMPENSATION:delay)));
		}

		/**
		 * 
		 * @param url URL to sound resource
    	 * @param delay amount of time to wait before playing sound, or null to enable auto-sync
		 */
		public synchronized void addSound(URL url, Integer delay) {
			purgeExpiredSounds(EXPIRY_EXTENSION);
			
			wavsToPlay.add(new UrlTimestampedSound(url, System.currentTimeMillis() + (delay==null?DEFAULT_POLL_INTERVAL + LATENCY_COMPENSATION:delay)));
		}

		public synchronized void cancelSounds() {
			for (int o = 0; o < wavsToPlay.size(); o++) {
				wavsToPlay.set(o, new CancelSoundsMarker(System.currentTimeMillis()));
			}
			wavsToPlay.add(new CancelSoundsMarker(System.currentTimeMillis()));
		}

		public synchronized TimestampedSound soundAtOffset(int o) {
			purgeExpiredSounds(EXPIRY_EXTENSION);
			
			if (o < version || o - version >= wavsToPlay.size()) {
				return null;
			}
			
			return wavsToPlay.get(o - version);
		}
		
		private void purgeExpiredSounds(long expiryExtension) {
			while (wavsToPlay.size() > 0) {
				TimestampedSound timestampedSound = wavsToPlay.get(0);
				
				if (timestampedSound.expired(expiryExtension)) {
					wavsToPlay.remove(0);
					version++;
					continue;
				}
				
				break;
			}
		}
	    
        public List<SoundBite> getSounds() {
        	HudsonSoundsDescriptor hudsonSoundsDescriptor = HudsonSoundsDescriptor.getDescriptor();
        	
        	return hudsonSoundsDescriptor.getSounds();
		}

		public boolean isGlobalMute() {
			return globalMute;
		}
		
		public void setGlobalMute(boolean mute) {
			globalMute = mute;
			save();
		}
		
        public FormValidation validateUrl(String soundUrl) {
        	if (StringUtils.isEmpty(soundUrl)) {
        		return FormValidation.warning("Missing URL");
        	}

            ResourceResolver resourceResolver = new ResourceResolver(soundUrl);

            if (!resourceResolver.isValid()) {
                return FormValidation.error("Invalid URL");
            }

            if (!resourceResolver.exists()) {
                return FormValidation.error("File not found or not readable");
            }

			return FormValidation.ok();
    	}
        
		public FormValidation doTestSound(@QueryParameter String selectedSound) {
			getJenkinsInstanceOrDie().checkPermission(PERMISSION);

			try {
                HudsonSoundsDescriptor.getDescriptor().playSound(selectedSound, null);
				return FormValidation.ok(String.format("Sound played successfully"));
			} catch (Exception e) {
				return FormValidation.error(String.format("Sound failed : " + e));
			}
		}
		
		public FormValidation doTestUrl(@QueryParameter String soundUrl) {
			getJenkinsInstanceOrDie().checkPermission(PERMISSION);

			try {
				FormValidation response = validateUrl(soundUrl);
				
				if (response.kind != FormValidation.Kind.OK) {
					return response;
				}
                ResourceResolver resourceResolver = new ResourceResolver(soundUrl);
                HudsonSoundsDescriptor.getDescriptor().playSoundFromUrl(resourceResolver.toURL(), null, null);
				return FormValidation.ok(String.format("Sound played successfully"));
			} catch (Exception e) {
				return FormValidation.error(String.format("Sound failed : " + e));
			}
		}

        public void playSound(SoundBite soundBite, Integer afterDelayMs) throws UnplayableSoundBiteException {
            try {
                ZipInputStream zipInputStream = new ZipInputStream(new ResourceResolver(soundBite.url).getInputStream());
                try {
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        if (!entry.getName().equals(soundBite.entryName)) {
                            continue;
                        }

                        final BufferedInputStream stream = new BufferedInputStream(zipInputStream);
                        playSound(stream, afterDelayMs);
                        IOUtils.closeQuietly(stream);
                    }
                } finally {
                    IOUtils.closeQuietly(zipInputStream);
                }
            } catch (UnsupportedAudioFileException e) {
                throw new UnplayableSoundBiteException(soundBite, e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected void playSound(InputStream stream, Integer afterDelayMs) throws UnsupportedAudioFileException, IOException {
            AudioInputStream source = HudsonSoundsDescriptor.asAudioInputStream(stream);

            ByteArrayOutputStream dest = new ByteArrayOutputStream();
            AudioSystem.write(source, AudioFileFormat.Type.WAVE, dest);
            String encodeBase64String = "data:audio/wav;base64," + new String(Base64.encodeBase64(dest.toByteArray(), false), "UTF-8");
            addSound(encodeBase64String, afterDelayMs);
        }

        protected void playSound(URL url, Integer afterDelayMs) {
            addSound(url, afterDelayMs);
        }
    }
    
    public HttpResponse doPlaySound(@QueryParameter String src, @QueryParameter Integer delay) {
    	if (StringUtils.isEmpty(src)) {
        	return FormValidation.error("Missing src parameter");
    	}

    	URL url = null;

    	try {
    		url = new URL(src);
    	} catch (MalformedURLException e) {
    		// src isn't a valid URL
    	}
    	
    	if (url == null && src.startsWith("data:") || url != null && url.getProtocol().equalsIgnoreCase("data")) {
    		// NOTE: This seems to fail due to request size limitation of Jetty
    		
    		// It's an immediate audio resource, transcoding not yet supported
    		getDescriptor().addSound(src, null);
    		return FormValidation.ok();
    	}
    	
    	if (url == null) {
    		return FormValidation.error("Invalid URL");
    	}

		// Try to load audio locally
		try {
			URLConnection connection = url.openConnection();
			getDescriptor().playSound(connection.getInputStream(), delay);
			return FormValidation.ok();
		} catch (Exception e) {
			if (url.getProtocol().toLowerCase().startsWith("http")) {
				// Sound could not be interpreted by Java, but maybe HTML5 could do better, so send URL straight through
                getDescriptor().playSound(url, delay);
				return FormValidation.ok();
			} else if (url.getProtocol().toLowerCase().startsWith("file")) {
				// file:// urls can't be sent to browser, so stream the file to them
                getDescriptor().playSound(url, delay);
				return FormValidation.ok();
			}

			return FormValidation.error(e, "Unplayable sound (" + e + ")");
		}
    }
    
    public HttpResponse doCancelSounds() {
        getJenkinsInstanceOrDie().checkPermission(PERMISSION);

		SoundsAgentActionDescriptor descriptor = getDescriptor();
		
		descriptor.cancelSounds();
		
    	return HttpResponses.forwardToPreviousPage();
    }
    
    public String getNextSound() {
    	final StaplerRequest request = Stapler.getCurrentRequest();
    	
    	Integer	version = null;
    	
    	try {
			version = Integer.valueOf(request.getParameter("version"));
		} catch (NumberFormatException e) {
			// Missing or invalid version
		}
    	
		return doGetSounds(request, Stapler.getCurrentResponse(), version).jsonObject.toString();
    }
    
    public JSONHttpResponse doGetSounds(StaplerRequest req, StaplerResponse rsp, @QueryParameter Integer version) {
    	final JSONObject jsonObject = new JSONObject();

    	if (HudsonSoundsDescriptor.getDescriptor().getPlayMethod() == PLAY_METHOD.BROWSER) {
    		SoundsAgentActionDescriptor descriptor = getDescriptor();
    		synchronized (descriptor) {
    			int	newVersion = descriptor.version + descriptor.wavsToPlay.size();
    			jsonObject.element("p", DEFAULT_POLL_INTERVAL);

    			if (version == null) {
    				// Fall back to cookie (saves a page refresh from missing sound events)
    				Cookie[] cookies = req.getCookies();
                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if (cookie.getName().equals(COOKIE_NAME)) {
                                try {
                                    version = Integer.parseInt(cookie.getValue());
                                } catch (NumberFormatException e) {
                                    // Invalid version number ignored
                                }
                            }
                        }
                    }
    			}

    			if (version != null && !isMuted(req)) {
    				final TimestampedSound sound = descriptor.soundAtOffset(version);

    				if (sound != null) {
    					if (!sound.expired(0) && !sound.isCancel()) {
        					jsonObject.element("play", sound.getUrl(version));
        					newVersion = version + 1;
        					long	delayBy = sound.getPlayAt() - System.currentTimeMillis();
        					if (delayBy > 0) {
        						jsonObject.element("d", delayBy);
        					}
        					jsonObject.element("p", IMMEDIATE_POLL_INTERVAL);
        				}
    					
    					if (sound.isCancel()) {
        					jsonObject.put("x", true);
    					}
    				}
    			}

    			if (isMuted(req)) {
    				jsonObject.element("p", MUTED_POLL_INTERVAL);
    				newVersion = -1;
    			}

    			jsonObject.element("v", newVersion);
    			Cookie cookie = new Cookie(COOKIE_NAME, "" + newVersion);
    			cookie.setPath("/");
    			cookie.setMaxAge(-1);
    			rsp.addCookie(cookie);
    		}
    	} else {
			jsonObject.element("p", MUTED_POLL_INTERVAL);
    	}

        rsp.setContentType("application/json");

    	return new JSONHttpResponse(jsonObject);
    }

    /**
     * Stream out the specified sound.
     *
     * @param request request to extract sound parameters from
     * @param response unused
     * @return the sound resource or an error
     */
	public HttpResponse doSound(StaplerRequest request, StaplerResponse response) {
    	final SoundsAgentActionDescriptor descriptor = getDescriptor();
    	Integer	version = null;
    	
    	try {
			version = Integer.parseInt(request.getParameter("v"));
	    	
			final TimestampedSound sound = descriptor.soundAtOffset(version);
			
			if (sound != null && sound instanceof UrlTimestampedSound) {
				UrlTimestampedSound	urlTimestampedSound = (UrlTimestampedSound) sound;
				
				return new UrlProxyHttpResponse(urlTimestampedSound.getRawUrl());
			}
		} catch (NumberFormatException e) {
			// Bad version
		}
		
		final int finalVersion = version!=null?version:0;

		return new HttpResponse() {
			@Override
			public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
				rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "No sound at version " + finalVersion + "(" + descriptor.version + ":" + descriptor.wavsToPlay + ")");
			}
		};
    }
    
    public HttpResponse doLocalMute(StaplerRequest req, StaplerResponse rsp) {

		Cookie cookie = new Cookie(MUTE_COOKIE_NAME, "mute");
		cookie.setPath("/");
		
    	if (isLocalMute(req)) {
    		cookie.setMaxAge(0);
    	} else {
    		cookie.setMaxAge(-1);
    	}
    	
		rsp.addCookie(cookie);
		
    	return HttpResponses.forwardToPreviousPage();
    }
    
    public boolean isLocalMute() {
		return isLocalMute(Stapler.getCurrentRequest());
    }

    public boolean isLocalMute(StaplerRequest req) {
		Cookie[] cookies = req.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(MUTE_COOKIE_NAME)) {
                    return true;
                }
            }
        }

		return false;
    }

    public HttpResponse doGlobalMute(StaplerRequest req, StaplerResponse rsp) {
        getJenkinsInstanceOrDie().checkPermission(PERMISSION);
    	
    	getDescriptor().setGlobalMute(!getDescriptor().isGlobalMute());
    	getDescriptor().cancelSounds();
		
    	return HttpResponses.forwardToPreviousPage();
    }
    
    public boolean isGlobalMute() {
		return getDescriptor().isGlobalMute();
    }

    protected boolean isMuted(StaplerRequest req) {
		return isGlobalMute() || isLocalMute(req);
	}
}
