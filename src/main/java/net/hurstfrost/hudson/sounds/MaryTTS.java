package net.hurstfrost.hudson.sounds;

import jenkins.model.Jenkins;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.synthesis.Voice;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.sound.sampled.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;

/*
 * https://github.com/marytts/marytts
 *
 * https://raw.github.com/marytts/marytts/master/download/marytts-components.xml
 *
 *
 * Thanks to <a href="http://jenkins-ci.361315.n4.nabble.com/template/NamlServlet.jtp?macro=user_nodes&user=368682">Jesse Glick-4</a>
 * for tip to use uberClassLoader to get MaryTTS to initialise.
 */
public class MaryTTS {
    private static final ResourceResolver MARY_COMPONENTS_XML = new ResourceResolver("classpath:marytts-components.xml");

    private LocalMaryInterface mary;
    private JenkinsVoiceLibrary jenkinsVoiceLibrary;

    public static void main(String[] args) throws MaryConfigurationException, ClassNotFoundException, IOException, LineUnavailableException, UnplayableSoundBiteException {
        MaryTTS maryTTS = new MaryTTS();

        maryTTS.init();

        System.out.println(maryTTS.mary.getAvailableLocales());
        System.out.println(maryTTS.mary.getAvailableVoices());

        for (Locale locale : maryTTS.mary.getAvailableLocales()) {
            System.out.println(locale);
            for (String voiceName : maryTTS.mary.getAvailableVoices(locale)) {
                Voice voice = Voice.getVoice(voiceName);
                System.out.println(voice);
                System.out.println(voiceName);
                playSoundBite(maryTTS.getAudio("This is a test with voice "+voice+".", voiceName));
            }
        }
    }

    public void init() {
        if (mary == null) {
            Log.info("Initialising MaryTTS");
            ClassLoader orig = Thread.currentThread().getContextClassLoader();
            ClassLoader uberClassLoader = getJenkinsUberClassLoader();
            if (uberClassLoader != null) {
                Thread.currentThread().setContextClassLoader(uberClassLoader);
            }
            try {
                try {
                    mary = new LocalMaryInterface();
                } catch (MaryConfigurationException e) {
                    Log.error("Could not initialize MaryTTS interface: " + e.getMessage(), e);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(orig);
            }
            Collection<Voice> maryVoices = Voice.getAvailableVoices();
            jenkinsVoiceLibrary = new JenkinsVoiceLibrary();

            for (Voice v : maryVoices) {
                jenkinsVoiceLibrary.add(new JenkinsVoice(v.getName()));
            }

            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(MARY_COMPONENTS_XML.getInputStream());

                Node rootChild = document.getDocumentElement().getFirstChild();

                while (rootChild != null) {
                    if (rootChild.getNodeName().equals("voice")) {
                        String voiceId = rootChild.getAttributes().getNamedItem("name").getNodeValue();

                        JenkinsVoice voice = jenkinsVoiceLibrary.getVoice(voiceId);

                        if (voice != null) {
                            String locale = rootChild.getAttributes().getNamedItem("locale").getNodeValue();
                            String gender = rootChild.getAttributes().getNamedItem("gender").getNodeValue();

                            voice.setLocale(LocaleUtils.toLocale(locale));
                            voice.setGender(JenkinsVoice.Gender.fromString(gender));

                            Node voiceChild = rootChild.getFirstChild();
                            while (voiceChild != null) {

                                if (voiceChild.getNodeName().equals("description")) {
                                    voice.setDescription(voiceChild.getTextContent());
                                }
                                if (voiceChild.getNodeName().equals("license")) {
                                    try {
                                        voice.setLicenceUrl(new URL(voiceChild.getAttributes().getNamedItem("href").getNodeValue()));
                                    } catch (MalformedURLException e) {
                                        // Invalid URL
                                    }
                                }
                                voiceChild = voiceChild.getNextSibling();
                            }
                        }
                    }

                    rootChild = rootChild.getNextSibling();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ClassLoader getJenkinsUberClassLoader() {
        return Jenkins.getInstance() != null ? Jenkins.getInstance().getPluginManager().uberClassLoader : null;
    }

    public AudioInputStream getAudio(String text) throws UnplayableSoundBiteException {
        return getAudio(text, null);
    }

    public AudioInputStream getAudio(String text, String voiceName) throws UnplayableSoundBiteException {
        init();

        try {
            if (!StringUtils.isEmpty(voiceName)) {
                mary.setVoice(voiceName);
            }
            return mary.generateAudio(text);
        } catch (Exception e) {
            Log.error("Synthesis failed: " + e.getMessage(), e);
            throw new UnplayableSoundBiteException("Synthesis failed: " + e.getMessage(), e);
        }
    }

    protected static void playSoundBite(AudioInputStream in) throws LineUnavailableException, IOException {
        final AudioFormat baseFormat = in.getFormat();
        AudioFormat  decodedFormat = baseFormat;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
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
    }

    public JenkinsVoiceLibrary getVoiceLibrary() {
        init();
        return jenkinsVoiceLibrary;
    }
}
