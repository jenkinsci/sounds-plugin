package net.hurstfrost.hudson.sounds;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.PageDecorator;

@Extension
public class SoundsAgentPageDecorator extends PageDecorator {

	public SoundsAgentPageDecorator() {
		super(SoundsAgentPageDecorator.class);
	}

	@Override
	public String getDisplayName() {
		return "Sounds Plugin sound agent";
	}
}
