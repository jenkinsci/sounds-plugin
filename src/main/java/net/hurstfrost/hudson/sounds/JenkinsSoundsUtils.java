package net.hurstfrost.hudson.sounds;

import jenkins.model.Jenkins;

public class JenkinsSoundsUtils {
    static Jenkins getJenkinsInstanceOrDie() {
        Jenkins instance = Jenkins.getInstance();

        if (instance != null) {
            return instance;
        }

        throw new AssertionError("Jenkins.getInstance() is null");
    }
}
