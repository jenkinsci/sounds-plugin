# sounds-plugin
## Play sounds on build events
### Overview
This plugin is able to play audio clips locally through sound hardware, remotely by piping them through an operating system command, or simultaneously through all browsers that are on a Jenkins page.
When outputting through local hardware, Sounds requires that the Jenkins' server machine has a working sound card and speaker that is not in a remote server room where there's no one around to hear it.
Jenkins can execute a command on a remote machine to play a sound sent through a pipe. This is normally an SSH command to a remote Linux based host that has a working sound card (e.g. 'ssh <host> play -').
If you have HTML5 Audio capable browsers you can configure Sounds to play sounds through all the browsers that are on a Jenkins page.
