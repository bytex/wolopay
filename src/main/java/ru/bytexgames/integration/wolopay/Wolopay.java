package ru.bytexgames.integration.wolopay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Wolopay support
 * <p>Description: Support Wolopay system</p>
 * <p>Dependencies: Google GSON, Log4J, Apache-Commons (Base64 codec).</p>
 * Date: 05.02.2015 - 14:21
 *
 * @author Ruslan Balkin <a href="mailto:baron@bytexgames.ru">baron@bytexgames.ru</a>
 * @version 1.0.0.0
 */
@SuppressWarnings({"Convert2Diamond"})
public class Wolopay {

	public static final String PRODUCTION_URL = "https://wolopay.com/api/v1/";

	public static final String SANDBOX_URL = "https://sandbox.wolopay.com/api/v1/";

	public static final String QUERY_ENCODING = "UTF-8";

	private final Logger log = Logger.getLogger(Wolopay.class);

	private final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+0000'");

	private String clientId;

	private String secret;

	private boolean sandbox;

	private boolean debug;

	private Proxy proxy = null;

	private SSLSocketFactory sslSocketFactory = null;

	private URL[] urls = new URL[2];

	public Wolopay() {
		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			urls[0] = new URL(PRODUCTION_URL);
			urls[1] = new URL(SANDBOX_URL);
		} catch (MalformedURLException ignored) {
		}
	}

	public Wolopay(String clientId, String secret) {
		this();
		this.clientId = clientId;
		this.secret = secret;
	}

	public Wolopay(String clientId, String secret, boolean sandbox, boolean debug) {
		this(clientId, secret);
		this.sandbox = sandbox;
		this.debug = debug;
	}

	public URL getEnvironmentURL() {
		return isSandbox() ? urls[1] : urls[0];
	}

	public String getClientId() {
		return clientId;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getSecret() {
		return secret;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setSandbox(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public boolean isDebug() {
		return debug;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Proxy getProxy() {
		return proxy;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public SSLSocketFactory getSslSocketFactory() {
		return sslSocketFactory;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
		this.sslSocketFactory = sslSocketFactory;
	}

	public String createTransactionUrl(String gamerId, String gamerLevel) {
		return createTransactionUrl(gamerId, gamerLevel, null);
	}

	public String createTransactionUrl(String gamerId, String gamerLevel, Map<String, Object> extraPostValues) {
		JsonObject o = createTransaction(gamerId, gamerLevel, extraPostValues);
		if (o == null) return null;
		final JsonElement element = o.get("url");
		return element.getAsString();
	}

	public JsonObject createTransaction(String gamerId, String gamerLevel) {
		return createTransaction(gamerId, gamerLevel, null);
	}

	public JsonObject createTransaction(String gamerId, String gamerLevel, Map<String, Object> extraPostValues) {
		final Map<String, Object> postData = new HashMap<String, Object>();
		postData.put("gamer_id", gamerId);
		postData.put("gamer_level", gamerLevel);
		if (extraPostValues != null) postData.putAll(extraPostValues);
		JsonElement jsonElement = makeRequest("transaction.json", "POST", postData);
		if (jsonElement instanceof JsonObject) {
			return (JsonObject) jsonElement;
		}
		return null;
	}

	/**
	 * Get the transaction
	 *
	 * @param transactionId Transaction ID
	 * @return JsonObject with transaction details
	 */
	public JsonObject getTransaction(String transactionId) {
		try {
			final JsonElement jsonElement = makeRequest("transaction.json?transaction_id=" + URLEncoder.encode(transactionId, QUERY_ENCODING), "GET", null);
			if (jsonElement instanceof JsonObject) {
				return (JsonObject) jsonElement;
			}
		} catch (UnsupportedEncodingException ignored) {
		}
		return null;
	}

	/**
	 * Obtain the transaction details
	 *
	 * @param transactionId Transaction ID
	 * @return JsonObject with transaction details
	 */
	public JsonObject getTransactionInfo(String transactionId) {
		try {
			final JsonElement jsonElement = makeRequest("transaction/info.json?transaction_id=" + URLEncoder.encode(transactionId, QUERY_ENCODING), "GET", null);
			if (jsonElement instanceof JsonObject) {
				return (JsonObject) jsonElement;
			}
		} catch (UnsupportedEncodingException ignored) {
		}
		return null;
	}

	/**
	 * Check whether the transaction is completed
	 *
	 * @param transactionId Transaction ID
	 * @return true, if transaction is completed
	 */
	public boolean isTransactionCompleted(String transactionId) {
		final JsonObject transaction = getTransaction(transactionId);
		if (transaction == null) return false;
		final JsonObject statusCategory = transaction.getAsJsonObject("status_category");
		if (statusCategory == null) return false;
		final JsonPrimitive id = statusCategory.getAsJsonPrimitive("id");
		return id != null && id.getAsInt() == 200;
	}

	/**
	 * This function is used to know how long does the gamer takes to do a first purchase
	 *
	 * @param gamerId Gamer ID
	 * @return JSON with gamer details
	 */
	public JsonElement createGamer(String gamerId) {
		final Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("gamer_id", gamerId);
		return makeRequest("gamer.json", "POST", p);
	}

	/**
	 * Create a promotional code
	 *
	 * @param promoCode promo code
	 * @param articleId article ID
	 * @return JSON with promo code details
	 */
	@SuppressWarnings("UnusedDeclaration")
	public JsonElement createPromotionalCode(String promoCode, String articleId) {
		final Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("promo_code", promoCode);
		p.put("article_id", articleId);
		return makeRequest("promo_code.json", "POST", p);
	}

	/**
	 * Create a promotional code
	 *
	 * @param promoCode promo code
	 * @param articleId article ID
	 * @return JSON with promo code details
	 */
	@SuppressWarnings("UnusedDeclaration")
	public JsonElement updatePromotionalCode(String promoCode, String articleId) {
		final Map<String, Object> p = new LinkedHashMap<String, Object>();
		p.put("promo_code", promoCode);
		p.put("article_id", articleId);
		return makeRequest("promo_code.json", "PUT", p);
	}

	protected final Random random = new Random();

	public static String readAll(final Reader reader) throws IOException {
		final StringBuilder sb = new StringBuilder();
		char[] buffer = new char[2 * 1024]; // двух килобайт должно хватить чаще всего
		try {
			int readBytes;
			while ((readBytes = reader.read(buffer, 0, buffer.length)) != -1) {
				sb.append(buffer, 0, readBytes);
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Convert the HTTP query string to Map&lt;String, String&gt;
	 *
	 * @param queryString query string (e.g. a=b&amp;c=d)
	 * @param encoding    query encoding (typically UTF-8)
	 * @return resulting map. E.g. {"a":"b", "c": "d"}
	 */
	public static LinkedHashMap<String, String> parse(String queryString, String encoding) {
		final LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
		String[] parts = queryString.split("&");
		for (String pair : parts) {
			int idx = pair.indexOf("=");
			if (idx < 0) {
				out.put(pair, "");
			} else {
				String key = pair.substring(0, idx);
				String value = pair.substring(idx + 1);
				try {
					out.put(URLDecoder.decode(key, encoding), URLDecoder.decode(value, encoding));
				} catch (UnsupportedEncodingException ignored) {
				}
			}
		}
		return out;
	}

	/**
	 * Send the HTTP request to the Wolopay server
	 *
	 * @param query  path (i.e. gamer.json)
	 * @param method HTTP method (GET / POST / PUT / whatever)
	 * @param values query parameters
	 * @return parsed response
	 */
	protected JsonElement makeRequest(String query, String method, Map<String, Object> values) {
		final URL url;
		try {
			url = new URL(getEnvironmentURL(), query);
		} catch (MalformedURLException e) {
			log.error("Invalid URL", e);
			return null;
		}
		final HttpURLConnection conn = connect(url);
		if (conn == null) {
			return null;
		}
		try {
			conn.setRequestMethod(method);
			conn.setRequestProperty("X-WSSE", generateWSSEHeader());
			conn.setUseCaches(false);
			conn.setDoInput(true);
			if (values != null) {
				conn.setDoOutput(true);
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, Object> entry : values.entrySet()) {
					if (sb.length() > 0) sb.append('&');
					final Object v = entry.getValue();
					sb.append(URLEncoder.encode(entry.getKey(), QUERY_ENCODING));
					if (v != null) {
						sb.append('=').append(URLEncoder.encode(v.toString(), QUERY_ENCODING));
					}
				}
				conn.setRequestProperty("Content-Length", Integer.toString(sb.length()));
				final DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				final String s = sb.toString();
				wr.writeBytes(s);
				wr.flush();
				wr.close();
				if (debug) {
					log.debug("Wrote " + s + " to " + url);
				}
			}
			int statusCode = conn.getResponseCode();
			if (statusCode >= 400 && isDebug()) {
				log.warn("URL " + url + " returned response code " + statusCode + ": " + conn.getResponseMessage());
				final InputStream errorStream = conn.getErrorStream();
				if (errorStream != null) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
					log.debug("Error was: " + readAll(reader));
				}
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			JsonElement json = new JsonParser().parse(reader);
			if (isDebug()) {
				log.debug(json);
			}
			return json;
		} catch (IOException e) {
			log.error("Exception occured while querying", e);
		}
		return null;
	}

	/**
	 * Connect to the specified URL, using proxy (and custom SSL factory if necessary)
	 *
	 * @param url URL to connect
	 * @return URL connection
	 */
	protected HttpURLConnection connect(URL url) {
		try {
			final Proxy proxy = getProxy();
			HttpURLConnection connection = proxy != null ? (HttpURLConnection) url.openConnection(proxy) : (HttpURLConnection) url.openConnection();
			if (connection instanceof HttpsURLConnection) {
				final SSLSocketFactory sslSocketFactory = getSslSocketFactory();
				if (sslSocketFactory != null) {
					((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
				}
			}
			return connection;
		} catch (IOException e) {
			log.error("Cannot connect to " + url, e);
			return null;
		}
	}

	/**
	 * Create a WSSE token
	 *
	 * @return token string
	 */
	protected String generateWSSEHeader() {
		final byte[] randomBytes = new byte[32];
		random.nextBytes(randomBytes);
		final MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.update(randomBytes);
			final byte[] rawNonce = md5.digest();
			final String nonce = Base64.encodeBase64String(rawNonce);
			final String created = isoFormat.format(new Date());

			final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update(rawNonce);
			sha1.update(created.getBytes());
			sha1.update(getSecret().getBytes());
			final String digest = Base64.encodeBase64String(sha1.digest());

			return String.format("UsernameToken Username=\"%s\", PasswordDigest=\"%s\", Nonce=\"%s\", Created=\"%s\"", getClientId(), digest, nonce, created);
		} catch (NoSuchAlgorithmException ignored) {
		}
		return ""; // Should never be here
	}

	/**
	 * Check whether the request was signed properly
	 *
	 * @param request HTTP request
	 * @return true, if everything is fine
	 */
	public boolean isAValidRequest(HttpServletRequest request) {
		final Map<String, String> params = Wolopay.parse(request.getQueryString(), QUERY_ENCODING);
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			sb.append(entry.getValue());
		}
		sb.append(getSecret());
		try {
			final MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update(sb.toString().getBytes());
			final String header = request.getHeader("Authorization");
			return header != null && header.equalsIgnoreCase("Signature " + Hex.encodeHexString(sha1.digest()));
		} catch (NoSuchAlgorithmException ignored) {
		}
		return false;
	}
}
