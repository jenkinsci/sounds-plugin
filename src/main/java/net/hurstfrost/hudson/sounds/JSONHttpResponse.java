package net.hurstfrost.hudson.sounds;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class JSONHttpResponse implements HttpResponse {

	protected final JSONObject jsonObject;
	
	public JSONHttpResponse(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
		jsonObject.write(rsp.getWriter());
	}

}
