package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.EnvVars;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

import hudson.util.VersionNumber;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Notifier} that allows Jenkins to play audio clips as build notifications.
 * 
 * @author Edward Hurst-Frost
 */
public class HudsonSoundsNotifier extends Notifier {
	public static enum PLAY_METHOD { LOCAL, PIPE, BROWSER };
	
	public static class SoundEvent {
		private final String soundId;

		private final Result toResult;

		private final Set<Result>	fromResults;
		
		@DataBoundConstructor
		public SoundEvent(final String soundId, final String toResult, final boolean fromNotBuilt, final boolean fromAborted, final boolean fromFailure, final boolean fromUnstable, final boolean fromSuccess) {
			this.soundId = soundId;
			Result	result = toResult!=null?Result.fromString(toResult):null;
			
			// Must be an exact match to Result, not default to Result.FAILURE.
			if (result != null && !result.toString().equals(toResult)) {
				result = null;
			}
			
			this.toResult = result;
			
			fromResults = new HashSet<Result>();
			setFromNotBuilt(fromNotBuilt);
			setFromAborted(fromAborted);
			setFromFailure(fromFailure);
			setFromUnstable(fromUnstable);
			setFromSuccess(fromSuccess);
		}

		public String getSoundId() {
			return soundId;
		}

		public Result getToBuildResult() {
			return toResult;
		}
		
		public boolean isFromNotBuilt() {
			return fromResults.isEmpty() || fromResults.contains(Result.NOT_BUILT);
		}
		
		public void setFromNotBuilt(boolean b) {
			if (b) {
				fromResults.add(Result.NOT_BUILT);
			} else {
				fromResults.remove(Result.NOT_BUILT);
			}
		}

		public boolean isFromAborted() {
			return fromResults.isEmpty() || fromResults.contains(Result.ABORTED);
		}
		
		public void setFromAborted(boolean b) {
			if (b) {
				fromResults.add(Result.ABORTED);
			} else {
				fromResults.remove(Result.ABORTED);
			}
		}

		public boolean isFromFailure() {
			return fromResults.isEmpty() || fromResults.contains(Result.FAILURE);
		}
		
		public void setFromFailure(boolean b) {
			if (b) {
				fromResults.add(Result.FAILURE);
			} else {
				fromResults.remove(Result.FAILURE);
			}
		}

		public boolean isFromUnstable() {
			return fromResults.isEmpty() || fromResults.contains(Result.UNSTABLE);
		}
		
		public void setFromUnstable(boolean b) {
			if (b) {
				fromResults.add(Result.UNSTABLE);
			} else {
				fromResults.remove(Result.UNSTABLE);
			}
		}

		public boolean isFromSuccess() {
			return fromResults.isEmpty() || fromResults.contains(Result.SUCCESS);
		}
		
		public void setFromSuccess(boolean b) {
			if (b) {
				fromResults.add(Result.SUCCESS);
			} else {
				fromResults.remove(Result.SUCCESS);
			}
		}

		public Set<Result> getFromResults() {
			return fromResults;
		}
	}

	private List<SoundEvent>	soundEvents;
	
	@DataBoundConstructor
	public HudsonSoundsNotifier() {
		// Default constructor
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}
	
	@Override
	public HudsonSoundsDescriptor getDescriptor() {
		return (HudsonSoundsDescriptor) super.getDescriptor();
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        Result result = build.getResult();
        AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
        Result previousResult = previousBuild != null ? previousBuild.getResult() : null;

        SoundEvent event = getSoundEventFor(result, previousResult);

		if (event != null) {
			try {
                EnvVars vars = build.getEnvironment(listener);
				getDescriptor().playSound(event.getSoundId(), vars);
			} catch (UnplayableSoundBiteException e) {
				listener.getLogger().println("Failed to play sound '" + event.getSoundId() + "' : " + e.toString());
			}
		}
		
		return true;
	}

	@Extension
	public static final class HudsonSoundsDescriptor extends BuildStepDescriptor<Publisher> {
        private static final String INTERNAL_ARCHIVE = new ResourceResolver("classpath:sound-archive.zip").toString();

		private static final int MAX_PIPE_TIMEOUT_SECS = 60;
		private static final int MIN_PIPE_TIMEOUT_SECS = 5;
		private static final int PIPE_TIMEOUT_EXTENDS_SECS = 5;
        
        public static final VersionNumber STAPLER_JSON_BREAKING_CHANGE_VERSION_NUMBER = new VersionNumber("1.445");

        private String	soundArchive = INTERNAL_ARCHIVE;
		
		private PLAY_METHOD	playMethod = PLAY_METHOD.BROWSER;
		
		private String	systemCommand;
		
		private int	pipeTimeoutSecs = MIN_PIPE_TIMEOUT_SECS;
		
		private transient TreeMap<String, SoundBite>	sounds;

		private transient boolean needsReindex;

		public HudsonSoundsDescriptor() {
			load();
			needsReindex = true;
		}

        public static HudsonSoundsDescriptor getDescriptor() {
            return JenkinsSoundsUtils.getJenkinsInstanceOrDie().getDescriptorByType(HudsonSoundsDescriptor.class);
        }

        public List<SoundBite> getSounds() {
			checkIndex();			
			return new ArrayList<SoundBite>(sounds.values());
		}

		private void checkIndex() {
			if (needsReindex) {
				needsReindex = false;
				sounds = rebuildSoundsIndex(soundArchive);
			}
		}

		public SoundBite getSound(String soundId) {
			checkIndex();			
			
			if (sounds != null && soundId != null) {
				return sounds.get(soundId);
			}
			
			return null;
		}
		
		protected static TreeMap<String, SoundBite> rebuildSoundsIndex(String urlString) {
            final TreeMap<String, SoundBite> index = new TreeMap<String, SoundBite>();
			try {
				ZipInputStream zipInputStream = new ZipInputStream(new ResourceResolver(urlString).getInputStream());
				try {
					ZipEntry entry;
					while ((entry = zipInputStream.getNextEntry()) != null) {
						if (!entry.isDirectory()) {
							final String id = getBiteName(entry.getName());
							AudioFileFormat f = null;
							try {
								f = AudioSystem.getAudioFileFormat(new BufferedInputStream(zipInputStream));
							} catch (UnsupportedAudioFileException e) {
								// Oh well
                                System.out.println(e);
                            }
							index.put(id, new SoundBite(id, entry.getName(), urlString, f));
						}
					}
				} finally {
					IOUtils.closeQuietly(zipInputStream);
				}
			} catch (Exception e) {
				// Can't find archive (this would have already been notified by doCheckSoundArchive() )
			}
			
			return index;
		}

		protected static String getBiteName(String path) {
			int	slash = path.lastIndexOf('/');
			if (slash != -1) {
				path = path.substring(slash + 1);
			}
			
			int dot = path.lastIndexOf('.');
			if (dot != -1) {
				path = path.substring(0, dot);
			}
			
			return path;
		}

		public String getSoundArchive() {
			return soundArchive;
		}

		public void setSoundArchive(String archive) {
            ResourceResolver resourceResolver = new ResourceResolver(archive);

            soundArchive = null;

            if (resourceResolver.isValid()) {
                soundArchive = resourceResolver.toString();
            }

            if (StringUtils.isEmpty(soundArchive)) {
                soundArchive = INTERNAL_ARCHIVE;
            }

			// Force index rebuild on next call
			needsReindex = true;
			sounds = null;
		}

		public PLAY_METHOD getPlayMethod() {
			return playMethod;
		}

		public void setPlayMethod(PLAY_METHOD playMethod) {
			this.playMethod = playMethod;
		}

		public String getSystemCommand() {
			return systemCommand;
		}

		public void setSystemCommand(String systemCommand) {
			this.systemCommand = systemCommand;
		}

		public int getPipeTimeoutSecs() {
			return pipeTimeoutSecs;
		}

		public void setPipeTimeoutSecs(int pipeTimeout) {
			this.pipeTimeoutSecs = Math.max(MIN_PIPE_TIMEOUT_SECS, Math.min(MAX_PIPE_TIMEOUT_SECS, pipeTimeout));
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public boolean configure(final StaplerRequest req, JSONObject json) {
			setSoundArchive(json.optString("soundArchive"));
			JSONObject	playMethod = json.optJSONObject("playMethod");
			if (playMethod != null) {
				try {
					PLAY_METHOD method = PLAY_METHOD.valueOf(playMethod.getString("value"));
					setPlayMethod(method);
                    
                    if (method == PLAY_METHOD.PIPE) {
                        JSONObject pipeConfig = playMethod;

                        if (Hudson.getVersion().isOlderThan(STAPLER_JSON_BREAKING_CHANGE_VERSION_NUMBER)) {
                            pipeConfig = json;
                        }

                        setSystemCommand(pipeConfig.optString("systemCommand"));
                        setPipeTimeoutSecs(pipeConfig.optInt("pipeTimeoutSecs"));
                    }
				} catch (Exception e) {
					Log.debug("Exception setting play method", e);
				}
			}
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Jenkins Sounds";
		}

		@Override
		public HudsonSoundsNotifier newInstance(StaplerRequest req, JSONObject formData) {
			HudsonSoundsNotifier m = new HudsonSoundsNotifier();
	        m.setSoundEvents(req.bindJSONToList(SoundEvent.class, formData.get("soundEvents")));
			return m;
		}

		public FormValidation doTestSound(@QueryParameter String selectedSound, @QueryParameter String soundArchive, @QueryParameter String playMethod, @QueryParameter String systemCommand, @QueryParameter int pipeTimeoutSecs) {
			if (StringUtils.isEmpty(selectedSound)) {
				return FormValidation.error("Please choose a sound to test.");
			}
			
			setSoundArchive(soundArchive);
			setPipeTimeoutSecs(pipeTimeoutSecs);
			setSystemCommand(systemCommand);

			try {
				playSound(selectedSound);
			} catch (UnplayableSoundBiteException e) {
				String	message = e.getMessage();
				if (StringUtils.isEmpty(message)) {
					message = e.toString();
				}
				return FormValidation.error("Failed to make sound '" + selectedSound + "' : " + message);
			}

			return FormValidation.ok("Jenkins made sound '" + selectedSound + "' successfully.");
		}
		
	    public FormValidation doCheckPipeTimeout(@QueryParameter final int value) {
	    	if (value > MAX_PIPE_TIMEOUT_SECS || value < MIN_PIPE_TIMEOUT_SECS) {
	    		return FormValidation.warning(String.format("Pipe timeout is invalid, valid range %d - %ds.", MIN_PIPE_TIMEOUT_SECS, MAX_PIPE_TIMEOUT_SECS));
	    	}
	    	
			return FormValidation.ok();
	    }
	    
	    public FormValidation doCheckSystemCommand(@QueryParameter final String systemCommand) {
	    	if (StringUtils.isEmpty(systemCommand)) {
	    		return FormValidation.warning(String.format("Enter a system command to pipe the sound file to"));
	    	}
	    	
			return FormValidation.ok();
	    }

        /**
         * Checks that a resource can be found at the specified location.
         *
         * @param value the value parameter from the request
         * @return a FormValidation
         */
	    public FormValidation doCheckSoundArchive(@QueryParameter final String value) {
            ResourceResolver resourceResolver = new ResourceResolver(value);

			if (!resourceResolver.isValid()) {
				return FormValidation.warning("The URL '" + value + "' is invalid (" /*+ e.toString() + ")"*/);
			}

	    	if (resourceResolver.exists()) {
                return FormValidation.ok();
            }

            return FormValidation.warning("Resource not found at '" + value + "'");
	    }

	    public static class SoundBite {
			public final String	id;

			public final String	entryName;
			
			public final String	url;
			
			public final AudioFileFormat	format;
			
			public SoundBite(final String _id, final String _entryName, final String _url, final AudioFileFormat _format) {
				id = _id;
				entryName = _entryName;
				url = _url;
				format = _format;
			}
			
			public String getId() {
				return id;
			}

			public String getEntryName() {
				return entryName;
			}

			public String getUrl() {
				return url;
			}

			public AudioFileFormat getFormat() {
				return format;
			}
			
			public String getDescription() {
				if (format == null) {
					return id + " (unsupported format)";
				}
				
				return id + " (" + format.getType() + ")";
			}
			
			@Override
			public String toString() {
				return getDescription();
			}
		}
	    
	    protected void playSoundFromUrl(URL url, Integer afterDelayMs, EnvVars vars) throws UnplayableSoundBiteException {
	    	switch (playMethod) {
	    		case BROWSER:
                    SoundsAgentAction.SoundsAgentActionDescriptor.getDescriptor().playSound(url, afterDelayMs);
    				break;
	    		default:
	    	    	try {
	    				playSoundFromInputStream(url.openStream(), vars);
	    			} catch (Exception e) {
	        			throw new UnplayableSoundBiteException(url.toString(), e);
	    			}
	    	}
		}

	    protected void playSound(String id) throws UnplayableSoundBiteException {
	    	playSound(id, null, null);
	    }

	    protected void playSound(String id, EnvVars vars) throws UnplayableSoundBiteException {
	    	playSound(id, null, vars);
	    }
	    
	    protected void playSound(String id, Integer afterDelayMs, EnvVars vars) throws UnplayableSoundBiteException {
	    	SoundBite soundBite = getSound(id);

	    	if (soundBite != null) {
	    		switch (playMethod) {
		    		case BROWSER:
                        SoundsAgentAction.SoundsAgentActionDescriptor.getDescriptor().playSound(soundBite, afterDelayMs);
                        return;
		    		default:
			    		try {
                            InputStream soundBiteInputStream = getSoundBiteInputStream(soundBite);
			    			
							if (soundBiteInputStream != null) {
								playSoundFromInputStream(soundBiteInputStream, vars);
								return;
							}
			    		} catch (Exception e) {
			    			throw new UnplayableSoundBiteException(soundBite, e);
			    		}
	    		}
	    	}
	    	
			throw new UnplayableSoundBiteException("No such sound.");
	    }

		private void playSoundFromInputStream(InputStream soundInputStream, EnvVars vars) throws LineUnavailableException, IOException, UnsupportedAudioFileException, Exception {
			try {
				switch (playMethod) {
					case LOCAL:
						playSoundBite(asAudioInputStream(soundInputStream));
						break;
					case PIPE:
						playSoundBite(soundInputStream, vars != null ? vars.expand(systemCommand) : systemCommand);
						break;
                    case BROWSER:
                        SoundsAgentAction.SoundsAgentActionDescriptor.getDescriptor().playSound(soundInputStream, null);
                        break;
				}
			} finally {
				IOUtils.closeQuietly(soundInputStream);
			}
		}

        static AudioInputStream asAudioInputStream(InputStream soundBiteInputStream) throws IOException, UnsupportedAudioFileException {
            if (soundBiteInputStream instanceof AudioInputStream) {
                return (AudioInputStream) soundBiteInputStream;
            }

            return AudioSystem.getAudioInputStream(soundBiteInputStream);
        }

        protected InputStream getSoundBiteInputStream(SoundBite soundBite) throws IOException, URISyntaxException {
			ZipInputStream zipInputStream = new ZipInputStream(new ResourceResolver(soundBite.url).getInputStream());

			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (!entry.getName().equals(soundBite.entryName)) {
					continue;
				}

				return new BufferedInputStream(zipInputStream);
			}
			
			throw new IOException("Unable to get input stream for " + soundBite);
	    }
	    
	    private static class ProcessKiller extends Thread {
			private final Process p;
			
			private long	killAfter;
			
			private boolean	didDestroy;

			public ProcessKiller(Process p, int	timeoutSecs) {
				this.p = p;
				killAfter = System.currentTimeMillis() + timeoutSecs * 1000;
				start();
			}
			
			@Override
			public void run() {
				try {
					while (true) {
						long	sleep = killAfter - System.currentTimeMillis();

						if (sleep > 0) {
							Thread.sleep(sleep);
						}

						if (System.currentTimeMillis() >= killAfter) {
							try {
								p.exitValue();
							} catch (IllegalThreadStateException e) {
								didDestroy = true;
								p.destroy();
							}
							break;
						}
					}
				} catch (InterruptedException e) {
					// Ignored
				}
			}

			public void progressing(int extendBySecs) {
				killAfter = Math.max(killAfter, System.currentTimeMillis() + extendBySecs * 1000);
			}
			
			public boolean didDestroy() {
				return didDestroy;
			}
	    }

	    private void playSoundBite(InputStream soundIn, String systemCommand) throws Exception {
	    	Process p = Runtime.getRuntime().exec(systemCommand);
	    	
	    	OutputStream soundOut = p.getOutputStream();

	    	ProcessKiller processKiller = new ProcessKiller(p, pipeTimeoutSecs);
	    	
	    	Exception	playException = null;

			try {
				byte[]	buffer = new byte[2<<16];
				while (true) {
					int	read = soundIn.read(buffer);
					if (read <= 0) {
						break;
					}
					soundOut.write(buffer, 0, read);
					processKiller.progressing(PIPE_TIMEOUT_EXTENDS_SECS);
				}
				IOUtils.copy(soundIn, soundOut);
				IOUtils.closeQuietly(soundOut);
			} catch (IOException e) {
				playException = e;
			}
			
			processKiller.progressing(5);
	    	
	    	p.waitFor();
	    	
	    	if (processKiller.didDestroy()) {
	    		throw new RuntimeException("Sound pipe was unresponsive and was killed after " + pipeTimeoutSecs + "s");
	    	}
	    	
	    	if (p.exitValue() != 0) {
	    		throw new RuntimeException("Sound pipe process returned non-zero exit status (" + p.exitValue() + ")");
	    	}
	    	
	    	if (playException != null) {
	    		throw playException;
	    	}
	    }

		protected void playSoundBite(AudioInputStream in) throws LineUnavailableException, IOException {
			final AudioFormat baseFormat = in.getFormat();
			AudioFormat  decodedFormat = baseFormat;	//new AudioFormat(
//	                AudioFormat.Encoding.PCM_SIGNED,
//	                baseFormat.getSampleRate(),
//	                8,
//	                baseFormat.getChannels(),
//	                baseFormat.getChannels(),
//	                baseFormat.getSampleRate(),
//	                false);
//			AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
			Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(decodedFormat);
			line.start();
			byte[]	buffer = new byte[8096];
			while (true) {
				int read = in.read(buffer);
				if (read <= 0) break;
				line.write(buffer, 0, read);
			}
			line.drain();
			line.stop();
			line.close();
//			din.close();
		}
	}
	
	public List<SoundEvent> getSoundEvents() {
		return soundEvents;
	}

	public void setSoundEvents(List<SoundEvent> newSounds) {
		ArrayList<SoundEvent> validatedList = new ArrayList<SoundEvent>();
		
		for (SoundEvent sound : newSounds) {
			if (sound.toResult != null && sound.getSoundId() != null && !sound.getFromResults().isEmpty()) {
				validatedList.add(sound);
			}
		}
		
		this.soundEvents = validatedList;
	}

	public SoundEvent getSoundEventFor(@Nullable Result result, @Nullable Result previousResult) {
		if (CollectionUtils.isEmpty(soundEvents)) {
			return null;
		}
		
		SoundEvent	foundEvent = null;
		for (SoundEvent event : soundEvents) {
			if (event.toResult.equals(result)) {
				if (!CollectionUtils.isEmpty(event.fromResults) && event.fromResults.contains(previousResult!=null?previousResult:Result.NOT_BUILT)) {
					foundEvent = event;
					if (getDescriptor().getSound(foundEvent.getSoundId()) != null) {
						break;
					}
					// Keep looking for valid sound ID
				}
			}
		}
		
		return foundEvent;
	}
}
