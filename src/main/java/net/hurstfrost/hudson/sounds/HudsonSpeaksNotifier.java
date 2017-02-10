package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

/**
 * {@link hudson.tasks.Notifier} that makes Hudson speak using <a href="http://mary.dfki.de/">MaryTTS</a>.
 * 
 * @author Edward Hurst-Frost
 */
public class HudsonSpeaksNotifier extends Notifier {
	private String projectTemplate;

	public String getProjectTemplate() {
		return StringUtils.isEmpty(projectTemplate)?getDescriptor().getAnnouncementTemplate():projectTemplate;
	}

	public void setProjectTemplate(String template) {
		if (StringUtils.isEmpty(template) || template.trim().equals(getDescriptor().getAnnouncementTemplate())) {
			projectTemplate = null;
		} else {
			projectTemplate = template;
		}
	}

	@DataBoundConstructor
	public HudsonSpeaksNotifier() {
		// Default constructor
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}
	
	@Override
	public HudsonSpeaksDescriptor getDescriptor() {
		return (HudsonSpeaksDescriptor) super.getDescriptor();
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
		JellyContext jellyContext = new JellyContext();
		jellyContext.setVariable("build", build);
		jellyContext.setVariable("duration", getDuration(build));

		String announcementMessage;
		final String template = !StringUtils.isEmpty(projectTemplate)?projectTemplate:getDescriptor().getAnnouncementTemplate();
		try {
			announcementMessage = getAnnouncementMessage(jellyContext, template);
		} catch (Exception e) {
			listener.getLogger().println("Hudson failed to interpret your announcement teamplate '" + template + "' : " + e.toString());
			return true;
		}
		
		if (!StringUtils.isEmpty(announcementMessage)) {
			listener.getLogger().println("Hudson speaks '" + announcementMessage + "'");
			try {
				say(announcementMessage);
			} catch (Exception e) {
				listener.getLogger().println("Hudson failed to speak '" + announcementMessage + "' : " + e.toString());
			}
		}

		return true;
	}

	private static void say(String announcementMessage) throws UnplayableSoundBiteException {
        HudsonSoundsNotifier.HudsonSoundsDescriptor.getDescriptor().speak(announcementMessage);
	}

	private static String getAnnouncementMessage(JellyContext jellyContext, String template) throws JellyException, IOException {
		StringWriter writer = new StringWriter();

		final XMLOutput xmlOutput = XMLOutput.createXMLOutput(writer);
		final String script = "<?xml version=\"1.0\"?>\n<j:jelly trim=\"false\" xmlns:j=\"jelly:core\" xmlns:x=\"jelly:xml\" xmlns:html=\"jelly:html\">" + template + "</j:jelly>";

		byte[] bytes = script.getBytes("UTF-8");

		jellyContext.runScript(new InputSource(new ByteArrayInputStream(bytes)), xmlOutput);

		xmlOutput.flush();
		xmlOutput.close();

		return writer.toString().trim();
	}

	private String getDuration(final AbstractBuild<?, ?> build) {
		String durationString = build.getTimestampString();
		durationString = durationString.replaceAll(" sec", " seconds");
		durationString = durationString.replaceAll(" ms", " milli seconds");
		durationString = durationString.replaceAll(" min", " minutes");
		return durationString;
	}

	@Extension
	public static final class HudsonSpeaksDescriptor extends BuildStepDescriptor<Publisher> {
		public static final String DEFAULT_TEMPLATE = "<j:choose>\n"+
		"<j:when test=\"${build.result!='SUCCESS' || build.project.lastBuild.result!='SUCCESS'}\">\n"+
		"Your attention please. Project ${build.project.name}, build number ${build.number}: ${build.result} in ${duration}.\n"+
		"<j:if test=\"${build.result!='SUCCESS'}\"> Get fixing those bugs team!</j:if>\n" +
		"</j:when>\n"+
		"<j:otherwise><!-- Say nothing --></j:otherwise>\n"+
		"</j:choose>";
		
		private String globalTemplate = DEFAULT_TEMPLATE;

		public HudsonSpeaksDescriptor() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public boolean configure(final StaplerRequest req, JSONObject json) {
			globalTemplate = json.optString("announcementTemplate");

			if (StringUtils.isEmpty(globalTemplate)) {
				globalTemplate = DEFAULT_TEMPLATE;
			}

			save();
			return true;
			// return super.configure(req, json);
		}

		@Override
		public String getDisplayName() {
			return "Hudson Speaks!";
		}

		@Override
		public HudsonSpeaksNotifier newInstance(StaplerRequest req, JSONObject formData) {
			HudsonSpeaksNotifier m = new HudsonSpeaksNotifier();
            req.bindParameters(m,"speaks_");
			return m;
		}

		public FormValidation doTestSpeech(@QueryParameter String testPhrase) {
			if (StringUtils.isEmpty(testPhrase)) {
				return FormValidation.errorWithMarkup("<p>Please enter a phrase for Hudson to speak.</p>");
			}
			
			try {
				say(testPhrase);
			} catch (Exception e) {
				return FormValidation.errorWithMarkup("<p>Hudson failed to speak</p><pre>" + Util.escape(Functions.printThrowable(e)) + "</pre>");
			}

			return FormValidation.ok("Hudson said '" + testPhrase + "' successfully.");
		}

		public String getAnnouncementTemplate() {
			return StringUtils.isEmpty(globalTemplate)?DEFAULT_TEMPLATE:globalTemplate;
		}

		public void setAnnouncementTemplate(String newTemplate) {
			this.globalTemplate = StringUtils.isEmpty(newTemplate)?DEFAULT_TEMPLATE:newTemplate;
		}
	}
}
