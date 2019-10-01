package net.hurstfrost.hudson.sounds;

import net.hurstfrost.protocols.ClasspathHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class ResourceResolver {
    static final String[]   INTERNAL = new String[] {"file","classpath"};
    static final String[]   ALL = new String[] {"file","classpath","http","https"};

    private final String urlString;
    private final Set<String> validSchemes;
    private URISyntaxException exception = null;
    private URI uri;

    public ResourceResolver(String url, String[] validSchemes) {
        this.validSchemes = new HashSet<>(Arrays.asList(validSchemes));
        urlString = StringUtils.trimToEmpty(url);

        try {
            uri = new URI(urlString);

            if (getUri().getScheme() == null) {
                uri = new File(urlString).toURI();
            }

            if (!this.validSchemes.contains(uri.getScheme())) {
                exception = new URISyntaxException(url, "Invalid scheme '" + uri.getScheme() + "'");
            }
        } catch (URISyntaxException e) {
            exception = e;
        }
    }

    public ResourceResolver(String soundUrl) {
        this(soundUrl, ResourceResolver.ALL);
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(urlString);
    }

    public boolean isValid() {
        return exception == null;
    }

    public URI getUri() {
        return uri;
    }

    public boolean exists() {
        if (isValid()) {
            try {
                if (uri.getScheme().equals("file")) {
                    return new File(uri).exists();
                }

                if (uri.getScheme().equals("classpath")) {
                    return getInputStream() != null;
                }

                if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
                    // Just assume it exists
                    return true;
                }
            } catch (Exception e) {
                // Drop through
            }
        }

        return false;
    }

    public InputStream getInputStream() throws IOException, URISyntaxException {
        return toURL().openStream();
    }

    @Override
    public String toString() {
        return urlString;
    }

    public URL toURL() throws MalformedURLException, URISyntaxException {
        if (!isValid()) {
            throw exception;
        }

        if (uri.getScheme().equals("classpath")) {
            return new URL(null, uri.toString(), new ClasspathHandler());
        }

        return uri.toURL();
    }
}
