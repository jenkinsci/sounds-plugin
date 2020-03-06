# Play sounds on build events

## Overview

This plugin is able to play audio clips locally through sound hardware,
remotely by piping them through an operating system command, or
simultaneously through all browsers that are on a Jenkins page.

When outputting through local hardware, Sounds requires that the
Jenkins' server machine has a working sound card and speaker that is not
in a remote server room where there's no one around to hear it.

Jenkins can execute a command on a remote machine to play a sound sent
through a pipe. This is normally an SSH command to a remote Linux based
host that has a working sound card (e.g. '`ssh <host> play -`').

If you have HTML5 Audio capable browsers you can configure Sounds to
play sounds through all the browsers that are on a Jenkins page.

## Compatibility

For all of the options there are varying degrees of support for the
multitude of audio file formats. The most widely supported (but the most
inefficient) is WAV.

## Triggering Sounds

### Configured build action

A job can be configured to play a sound as part of its action. You can
have jobs dedicated to playing sounds on a schedule, or have the job
play a sound as it does other actions.

### Configured post-build actions

The build configuration allows you to select a sound to be played for
any build state transition. You can configure:

-   for any possible build result
-   a list of previous build results
-   the sound to play

### External trigger

A sound can also be triggered by making an HTTP POST request and passing a
URL to the sound to be played. This enables external tools to play
sounds through Jenkins.

``` syntaxhighlighter-pre
<Jenkins URL>/sounds/playSound?src=file:///home/jenkins/sounds/somesound.wav
```

### Customising the sound archive

#### Making a sound archive

The sound archive is a simple .zip or .jar file containing sound files.

Jenkins analyses each file to detect whether it is a valid sound file
supported by JavaSound. JavaSound supports AIFF, AU and WAV, see
[formats](https://www.oracle.com/technetwork/java/javase/formats-138492.html#Formats).

NOTE: The filename extension does not guarantee that the format is
supported. If you have a file that doesn't play, try passing it though an
conversion tool.

Jenkins ignores the archive folder structure (to simplify packaging) and
uses the file name less extension as an ID, so keep the archive flat, or
avoid having the same filename in multiple folders.

#### Publishing your sound archive

Jenkins Sounds can read an archive from the local filing system, or an
HTTP URL.

-   publish the archive on a local web server and use `http://`
-   put the archive in Jenkins' local filing system and use `file://`

NOTE: Jenkins streams the audio from the archive for each event (to
reduce memory requirements), so make sure it's not too far away.

#### Configure Jenkins to use your archive

Within the Configure System section of Jenkins Management locate the
settings for Jenkins Sounds.

Enter the path to the archive.

-   Absolute `http://` and `file://` URLs will be used unmodified.
-   URIs without a protocol will be assumed to be filing system
    references and converted to `file://` URLs.

After changing the sound archive location URL you must save the form
before being able to test the sounds.

The Test Sound advanced option shows a list of the sounds found in the
archive and their supported format.

## Changelog

### Version 0.6 (Mar 6th, 2020)

-   Security fixes (stop CSRF attacks) https://jenkins.io/security/advisory/2020-01-15/#SECURITY-814
-   Removed JSONP option (for security reasons).
-   Changed many requests to POST, which will likely require external triggers to supply a 'security crumb'.
-   Replaced Prototype.js AJAX with plain JS XMLHttpRequest.
-   Enforced security on administration of the plugin settings.

### Version 0.5 (Jun 25th, 2016)

-   Allow environment variables in piped command
    ([tommikiviniemi-srs](https://github.com/tommikiviniemi-srs)).

### Version 0.4.3 (Oct 8th, 2013)

-   Allowed local mute to be used by anonymous user when security is
    enabled
    ([JENKINS-20081](https://issues.jenkins-ci.org/browse/JENKINS-20081)).
-   Fixed incompatibility with Jenkins version 1.445 and upwards causing
    configuration of system command in pipe mode to fail
    ([JENKINS-13825](https://issues.jenkins-ci.org/browse/JENKINS-13825)).
-   Fixed error message when 'Sound archive location' field is left
    empty
    ([JENKINS-19540](https://issues.jenkins-ci.org/browse/JENKINS-19540)).

### Version 0.4 (Sep 1st, 2011)

-   Major update to provide option where sounds are played
    simultaneously through all browsers showing a Jenkins page.

### Version 0.3 (Mar 1st, 2011)

-   Added capability of piping sounds through a system command (e.g.
    play on remote linux host)
-   Added the sad trombone - the best build breaking sound effect ....
    ever!

### Version 0.2 (Dec 6th, 2009)

-   Internal sound archive restored if empty archive location submitted
-   Use classpath URI to internal archive

### Version 0.1 (Dec 2nd, 2009)

-   Initial release
