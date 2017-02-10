package net.hurstfrost.hudson.sounds;

import java.net.URL;
import java.util.Locale;

public class JenkinsVoice {
    enum Gender {
        male, female;
        public static Gender fromString(String gender) {
            if (gender.toLowerCase().startsWith("f")) {
                return female;
            }
            return male;
        }
    }

    private final String id;
    private String description;
    private URL licenceUrl;
    private Locale locale;
    private Gender gender;

    public JenkinsVoice(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description!=null?description:id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getLicenceUrl() {
        return licenceUrl;
    }

    public void setLicenceUrl(URL licenceUrl) {
        this.licenceUrl = licenceUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }
}
