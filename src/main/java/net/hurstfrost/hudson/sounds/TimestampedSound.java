/**
 * 
 */
package net.hurstfrost.hudson.sounds;

interface TimestampedSound {
	long getPlayAt();
	
	boolean expired(long expiryExtension);
	
	String getUrl(Integer version);
}