package net.hurstfrost.hudson.sounds;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JenkinsVoiceLibrary {
    private final Map<String, JenkinsVoice> availableVoices;

    public JenkinsVoiceLibrary() {
        this.availableVoices = new HashMap<>();
    }

    public Collection<JenkinsVoice> getAvailableVoices() {
        return availableVoices.values();
    }

    public void add(JenkinsVoice jenkinsVoice) {
        availableVoices.put(jenkinsVoice.getId(), jenkinsVoice);
    }

    public JenkinsVoice getVoice(String id) {
        return availableVoices.get(id);
    }
}
