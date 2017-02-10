package net.hurstfrost.hudson.sounds;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class MaryTTSTest {

    private MaryTTS maryTTS;

    @Before
    public void before() {
        maryTTS = new MaryTTS();
    }

    @Test
    public void numberOfAvailableVoices() {
        Collection<JenkinsVoice> voices = maryTTS.getVoiceLibrary().getAvailableVoices();

        assertEquals(8, voices.size());
    }

    @Test
    public void voiceMetaData() {
        JenkinsVoice voice = maryTTS.getVoiceLibrary().getVoice("enst-camille-hsmm");

        assertEquals("enst-camille-hsmm", voice.getId());
        assertEquals("A female French hidden semi-Markov model voice, built at Télécom ParisTech (ENST) using data recorded by Camille Dianoux", voice.getDescription());
        assertEquals("http://mary.dfki.de/download/by-sa-3.0.html", voice.getLicenceUrl().toString());
        assertEquals("fr", voice.getLocale().getLanguage());
        assertEquals("", voice.getLocale().getCountry());
        assertEquals(JenkinsVoice.Gender.female, voice.getGender());

        voice = maryTTS.getVoiceLibrary().getVoice("dfki-poppy-hsmm");

        assertEquals("dfki-poppy-hsmm", voice.getId());
        assertEquals("A female British English hidden semi-Markov model voice", voice.getDescription());
        assertEquals("http://mary.dfki.de/download/by-nd-3.0.html", voice.getLicenceUrl().toString());
        assertEquals("en", voice.getLocale().getLanguage());
        assertEquals("GB", voice.getLocale().getCountry());
        assertEquals(JenkinsVoice.Gender.female, voice.getGender());

        voice = maryTTS.getVoiceLibrary().getVoice("dfki-spike-hsmm");

        assertEquals("dfki-spike-hsmm", voice.getId());
        assertEquals("A male British English hidden semi-Markov model voice", voice.getDescription());
        assertEquals("http://mary.dfki.de/download/by-nd-3.0.html", voice.getLicenceUrl().toString());
        assertEquals("en", voice.getLocale().getLanguage());
        assertEquals("GB", voice.getLocale().getCountry());
        assertEquals(JenkinsVoice.Gender.male, voice.getGender());
    }

    @Test
    public void noNulls() {
        JenkinsVoice v = new JenkinsVoice("id");

        assertNotNull(v.getId());
        assertNotNull(v.getDescription());
    }
}
