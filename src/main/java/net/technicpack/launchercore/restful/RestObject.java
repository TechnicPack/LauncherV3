package net.technicpack.launchercore.restful;

import com.google.gson.Gson;
import net.technicpack.launchercore.exception.RestfulAPIException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public class RestObject {
	private static final Gson gson = new Gson();

	private String error;

	public boolean hasError() {
		return error != null;
	}

	public String getError() {
		return error;
	}

	public static <T extends RestObject> T getRestObject(Class<T> restObject, String url) throws RestfulAPIException {
		InputStream stream = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(15000);

			stream = conn.getInputStream();
			String data = IOUtils.toString(stream);
			T result = gson.fromJson(data, restObject);

			if (result.hasError()) {
				throw new RestfulAPIException("Error in response: " + result.getError());
			}

			return result;
		} catch (SocketTimeoutException e) {
			throw new RestfulAPIException("Timed out accessing URL [" + url + "]", e);
		}  catch (MalformedURLException e) {
			throw new RestfulAPIException("Invalid URL [" + url + "]", e);
		} catch (IOException e) {
			throw new RestfulAPIException("Error accessing URL [" + url + "]", e);
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}
}
