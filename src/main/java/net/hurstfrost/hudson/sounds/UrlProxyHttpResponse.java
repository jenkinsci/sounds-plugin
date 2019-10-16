package net.hurstfrost.hudson.sounds;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class UrlProxyHttpResponse implements HttpResponse {

	private final URL url;

	public UrlProxyHttpResponse(URL url) {
		this.url = url;
	}

	@Override
	public void generateResponse(StaplerRequest request, StaplerResponse response, Object node) throws IOException {
		InputStream inputStream = url.openStream();
		
		try {
			byte[]	buffer = new byte[4096];
			int	size = 0;
			
			response.setContentType(getContentType(url));
			response.addHeader("rawUrl", url.toString());
			OutputStream outputStream = response.getOutputStream();
			while (true) {
				int read = inputStream.read(buffer);
				
				if (read > 0) {
					outputStream.write(buffer, 0, read);
					size += read;
					continue;
				}
				
				break;
			}
			response.addHeader("audioSize", "" + size);
			outputStream.flush();
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private static String getContentType(URL url) {
		return (new MimetypesFileTypeMap()).getContentType(url.getPath());
	}
}
