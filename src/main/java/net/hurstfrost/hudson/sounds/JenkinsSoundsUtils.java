package net.hurstfrost.hudson.sounds;

import jenkins.model.Jenkins;

class JenkinsSoundsUtils {
    static Jenkins getJenkinsInstanceOrDie() {
        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance != null) {
            return instance;
        }

        throw new AssertionError("Jenkins.getInstance() is null");
    }
}
