package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

import net.hurstfrost.hudson.sounds.HudsonSoundsNotifier.HudsonSoundsDescriptor.SoundBite;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Notifier} that allows Hudson to play audio clips as build notifications..
 * 
 * @author Edward Hurst-Frost
 */
public class HudsonSoundsNotifier extends Notifier {
	public static class SoundEvent {
		private final String soundId;

		private final Result toResult;

		private final Set<Result>	fromResults;
		
		@DataBoundConstructor
		public SoundEvent(@SuppressWarnings("hiding") final String soundId, @SuppressWarnings("hiding") final String toResult, final boolean fromNotBuilt, final boolean fromAborted, final boolean fromFailure, final boolean fromUnstable, final boolean fromSuccess) {
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
			return fromResults.contains(Result.NOT_BUILT);
		}
		
		public void setFromNotBuilt(boolean b) {
			if (b) {
				fromResults.add(Result.NOT_BUILT);
			} else {
				fromResults.remove(Result.NOT_BUILT);
			}
		}

		public boolean isFromAborted() {
			return fromResults.contains(Result.ABORTED);
		}
		
		public void setFromAborted(boolean b) {
			if (b) {
				fromResults.add(Result.ABORTED);
			} else {
				fromResults.remove(Result.ABORTED);
			}
		}

		public boolean isFromFailure() {
			return fromResults.contains(Result.FAILURE);
		}
		
		public void setFromFailure(boolean b) {
			if (b) {
				fromResults.add(Result.FAILURE);
			} else {
				fromResults.remove(Result.FAILURE);
			}
		}

		public boolean isFromUnstable() {
			return fromResults.contains(Result.UNSTABLE);
		}
		
		public void setFromUnstable(boolean b) {
			if (b) {
				fromResults.add(Result.UNSTABLE);
			} else {
				fromResults.remove(Result.UNSTABLE);
			}
		}

		public boolean isFromSuccess() {
			return fromResults.contains(Result.SUCCESS);
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
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
		SoundEvent event = getSoundEventFor(build.getResult(), build.getPreviousBuild()!=null?build.getPreviousBuild().getResult():null);
		
		if (event != null) {
			try {
				getDescriptor().playSound(event.getSoundId());
			} catch (UnplayableSoundBiteException e) {
				listener.getLogger().println("Failed to play sound '" + event.getSoundId() + "' : " + e.toString());
			}
		}
		
		return true;
	}

	@Extension
	public static final class HudsonSoundsDescriptor extends BuildStepDescriptor<Publisher> {
		private static final String INTERNAL_ARCHIVE = HudsonSoundsNotifier.class.getResource("/sound-archive.zip").toString();
		
		private String	soundArchive = INTERNAL_ARCHIVE;
		
		private transient TreeMap<String, SoundBite>	sounds;

		private transient boolean needsReindex;

		public HudsonSoundsDescriptor() {
			load();
			needsReindex = true;
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

		public SoundBite getSound(String id) {
			checkIndex();			
			
			if (sounds != null && id != null) {
				return sounds.get(id);
			}
			
			return null;
		}
		
		protected static TreeMap<String, SoundBite> rebuildSoundsIndex(String urlString) {
			final TreeMap<String, SoundBite> index = new TreeMap<String, SoundBite>();
			try {
				URL url = new URL(urlString);
				URLConnection connection = url.openConnection();
				ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream());
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

		protected static String getBiteName(String name) {
			int	slash = name.lastIndexOf('/');
			if (slash != -1) {
				name = name.substring(slash + 1);
			}
			
			int dot = name.lastIndexOf('.');
			if (dot != -1) {
				name = name.substring(0, dot);
			}
			
			return name;
		}

		public String getSoundArchive() {
			return soundArchive;
		}

		public void setSoundArchive(String archive) {
			if (!StringUtils.isEmpty(archive)) {
				soundArchive = toUri(archive);
			} else {
				soundArchive = INTERNAL_ARCHIVE;
			}
			
			// Force index rebuild on next call
			needsReindex = true;
			sounds = null;
		}

		/**
		 * @param archive
		 * @return 
		 */
		private String toUri(String archive) {
			if (archive.startsWith("http://") || archive.startsWith("file:/")) {
				return archive;
			}
			
			// Try to make sense of this as a filing system path
			return new File(archive).toURI().toString();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public boolean configure(final StaplerRequest req, JSONObject json) {
			setSoundArchive(json.optString("soundArchive"));
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Hudson Sounds";
		}

		@Override
		public HudsonSoundsNotifier newInstance(StaplerRequest req, JSONObject formData) {
			HudsonSoundsNotifier m = new HudsonSoundsNotifier();
            m.setSoundEvents(req.bindJSONToList(SoundEvent.class, formData.get("soundEvents")));
			return m;
		}

		public FormValidation doTestSound(@QueryParameter String selectedSound) {
			if (StringUtils.isEmpty(selectedSound)) {
    			return FormValidation.error("Please choose a sound to test.");
			}
			
			try {
				playSound(selectedSound);
			} catch (UnplayableSoundBiteException e) {
    			return FormValidation.error("Failed to make sound '" + selectedSound + "' : " + e.toString());
			}
			
			return FormValidation.ok("Hudson made sound '" + selectedSound + "' successfully.");
		}
		
        public FormValidation doCheckSoundArchive(@QueryParameter final String value) {
        	URI uri;
			try {
				uri = new URI(toUri(value));
			} catch (URISyntaxException e) {
    			return FormValidation.warning("The URL '" + value + "' is invalid (" + e.toString() + ")");
			}
        	
        	if (uri.getScheme().equals("file")) {
        		if (new File(uri).exists()) {
        			return FormValidation.ok();
        		}
        		return FormValidation.warning("File not found '" + uri + "'");
        	} else if (uri.getScheme().equals("http")) {
        		try {
        			URLConnection openConnection = uri.toURL().openConnection();

        			if (openConnection instanceof HttpURLConnection) {
        				HttpURLConnection httpUrlConnection = (HttpURLConnection) openConnection;

        				final int responseCode = httpUrlConnection.getResponseCode();
        				if (responseCode == HttpURLConnection.HTTP_OK) {
        					return FormValidation.ok();
        				}
        				
        				return FormValidation.warning("The URL '" + value + "' is invalid (" + httpUrlConnection.getResponseMessage() + ")");
        			}
        			
        			return FormValidation.warning("The URL '" + value + "' is invalid");
        		} catch (IOException e) {
        			return FormValidation.warning("The URL '" + value + "' is invalid (" + e.toString() + ")");
        		}
        	} else {
        		// Invalid URI
        		return FormValidation.warning("The URI '" + value + "' is invalid");
        	}
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

        protected void playSound(String id) throws UnplayableSoundBiteException {
        	SoundBite soundBite = getSound(id);

        	if (soundBite != null) {
        		try {
        			URL url = new URL(soundBite.url);
        			URLConnection connection = url.openConnection();
        			ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream());
        			try {
						ZipEntry entry;
						while ((entry = zipInputStream.getNextEntry()) != null) {
							if (!entry.getName().equals(soundBite.entryName)) {
								continue;
							}

							final BufferedInputStream stream = new BufferedInputStream(zipInputStream);
							playSoundBite(AudioSystem.getAudioInputStream(stream));
							return;
						}
					} finally {
						IOUtils.closeQuietly(zipInputStream);
					}
        		} catch (Exception e) {
        			throw new UnplayableSoundBiteException(soundBite, e);
        		}
        	}
        	
			throw new UnplayableSoundBiteException("No such sound.");
        }

		protected void playSoundBite(AudioInputStream audioInputStream) throws LineUnavailableException, IOException {
			Info info = new DataLine.Info(SourceDataLine.class, audioInputStream.getFormat());
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

			line.open();
			line.start();
			byte[]	buffer = new byte[8096];
			while (true) {
				int read = audioInputStream.read(buffer);
				if (read <= 0) break;
				line.write(buffer, 0, read);
			}
			line.drain();
			line.close();
		}
	}
	
	@SuppressWarnings("serial")
	public static class UnplayableSoundBiteException extends Exception {
		private final SoundBite soundBite;

		public SoundBite getSoundBite() {
			return soundBite;
		}

		public UnplayableSoundBiteException(SoundBite bite, Exception e) {
			super(e);
			soundBite = bite;
		}

		public UnplayableSoundBiteException(String message) {
			super(message);
			soundBite = null;
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

	public SoundEvent getSoundEventFor(Result result, Result previousResult) {
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
