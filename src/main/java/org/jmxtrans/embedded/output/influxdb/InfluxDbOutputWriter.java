/*
 * Copyright (c) 2010-2016 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.embedded.output.influxdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.jmxtrans.embedded.ExtendedResultNameStrategy;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.output.AbstractOutputWriter;
import org.jmxtrans.embedded.output.OutputWriter;
import org.jmxtrans.embedded.util.io.IoRuntimeException;
import org.jmxtrans.embedded.util.io.IoUtils;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.jmxtrans.embedded.util.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output writer that pushes JmxTrans query results into an InfluxDB server
 * using its HTTP {@code /write} endpoint.
 *
 * <p>The writer translates each {@link QueryResult} into an
 * {@link InfluxMetric}, batches them into a single HTTP {@code POST} request
 * and tracks the number of consecutive failures against a configurable
 * retry-threshold. Once the threshold is exceeded, the writer silently stops
 * sending further batches until the next {@link #start()} is invoked; this
 * prevents log spam in environments where the InfluxDB server is permanently
 * unavailable.</p>
 *
 * <p>Configuration properties (resolved through {@link AbstractOutputWriter}):</p>
 * <ul>
 *   <li>{@code enabled} (boolean, default {@code true}) &mdash; master switch.</li>
 *   <li>{@code url} (string, default {@code http://127.0.0.1:8086}) &mdash;
 *       base URL of the InfluxDB instance.</li>
 *   <li>{@code database} (string, default {@code Metrics_<local-ip>}) &mdash;
 *       target database name; {@code #hostname#}/{@code #hostaddress#} type
 *       expressions are resolved through an
 *       {@link ExtendedResultNameStrategy}.</li>
 *   <li>{@code user} / {@code password} (optional) &mdash; HTTP basic
 *       credentials.</li>
 *   <li>{@code retentionPolicy} (optional) &mdash; target retention policy.</li>
 *   <li>{@code tags} (optional, comma-separated {@code k=v} list) &mdash;
 *       tags attached to every exported metric.</li>
 *   <li>{@code connectTimeoutMillis} / {@code readTimeoutMillis} &mdash;
 *       connect / read timeouts for the HTTP call.</li>
 *   <li>{@code retryTimes} &mdash; number of tolerated consecutive failures.</li>
 *   <li>{@code proxyHost} / {@code proxyPort} &mdash; optional HTTP proxy.</li>
 * </ul>
 *
 * @author Kristoffer Erlandsson
 * @since 3.0.0
 * @see OutputWriter
 * @see AbstractOutputWriter
 */
public class InfluxDbOutputWriter extends AbstractOutputWriter implements OutputWriter {

    /** Configuration key for the master enable/disable switch. */
	public final static String SETTING_ENABLED = "enabled";

	/** Counter of consecutive export failures; thread-safe via {@link AtomicInteger}. */
	private final AtomicInteger exceptionCounter = new AtomicInteger();

	/** SLF4J logger dedicated to this output writer. */
	private final Logger LOG = LoggerFactory.getLogger(InfluxDbOutputWriter.class);

    /** Fully parsed InfluxDB write endpoint, computed at {@link #start()}. */
    private URL url;

    /** Target database name (resolved by the configured strategy). */
    private String database;

    /** InfluxDB user name; {@code null} if not configured. */
    private String user; // Null if not configured

    /** InfluxDB password; {@code null} if not configured. */
    private String password; // Null if not configured

    /** InfluxDB retention policy; {@code null} if not configured. */
    private String retentionPolicy; // Null if not configured

    /** Tags applied to every exported metric. */
    private List<InfluxTag> tags;

    /** Internal buffer of metrics accumulated between exports. */
    private List<InfluxMetric> batchedMetrics = new ArrayList<InfluxMetric>();

    /** Connect timeout for the HTTP call, in milliseconds. */
    private int connectTimeoutMillis;

    /** Read timeout for the HTTP call, in milliseconds. */
    private int readTimeoutMillis;

    /** Maximum number of tolerated consecutive failures before the writer idles. */
    private int retryTimes;

    /** Master enable flag, loaded from {@link #SETTING_ENABLED}. */
    private boolean enabled;

    /**
     * Optional proxy for the http API calls.
     */
	@Nullable
	private Proxy proxy;

    /**
     * Default no-argument constructor required by the JmxTrans output-writer
     * reflective instantiation.
     */
    public InfluxDbOutputWriter() {

    }

    /**
     * One-shot setup invoked by the JmxTrans runtime before the first
     * invocation of {@link #write(Iterable)}: reads configuration properties,
     * resolves template expressions through
     * {@link ExtendedResultNameStrategy}, parses the InfluxDB endpoint and
     * optionally configures an HTTP proxy.
     */
	@Override
	public void start() {

        enabled = getBooleanSetting(SETTING_ENABLED, true);

        if(!enabled) return;

        //从新定义ResultNameStrategy
        setStrategy(new ExtendedResultNameStrategy());

        String urlStr = getUrl(getStringSetting("url"));
        database = getStrategy().resolveExpression(getDatabase(getStringSetting("database")));
        user = getUser(getStringSetting("user"));
        password = getPassword(getStringSetting("password"));
        retentionPolicy = getStringSetting("retentionPolicy", null);
        String tagsStr = getStringSetting("tags", "");

        tags = InfluxMetricConverter.tagsFromCommaSeparatedString(this.getStrategy(),tagsStr);
        connectTimeoutMillis = getIntSetting("connectTimeoutMillis", 3000);
        readTimeoutMillis = getIntSetting("readTimeoutMillis", 5000);
        retryTimes = getIntSetting("retryTimes", 10);

        url = parseUrlStr(getWriteEndpointForUrlStr(urlStr));

        if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST).isEmpty()) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST), getIntSetting(SETTING_PROXY_PORT)));
		}

        if(LOG.isInfoEnabled()){

			LOG.info("Starting Stackdriver writer connected to '{}', proxy {} ...", url, proxy);
	        LOG.info( "InfluxDbOutputWriter is configured with url=" + urlStr
	                + ", database=" + database
	                + ", user=" + user
	                + ", password=" + (password != null ? "****" : null)
	                + ", tags=" + tagsStr
	                + ", connectTimeoutMills=" + connectTimeoutMillis
	                + ", readTimeoutMillis=" + readTimeoutMillis);
        }
    }

    /**
     * Returns the InfluxDB {@code /write} endpoint URL for the given base URL.
     * A trailing slash on the base URL is preserved.
     *
     * @param urlStr the base InfluxDB URL.
     * @return the {@code /write} URL.
     */
    private String getWriteEndpointForUrlStr(String urlStr) {
        return urlStr + (urlStr.endsWith("/") ? "write" : "/write");
    }

    /**
     * Parses the write URL plus the {@code precision=ms&db=...} query string
     * into a {@link URL} instance. Any malformed URL is wrapped in a
     * {@link RuntimeException}.
     *
     * @param urlStr the write endpoint URL without the query string.
     * @return the parsed URL with database / credentials query parameters.
     */
    private URL parseUrlStr(String urlStr) {
        try {
            return new URL(urlStr + "?" + buildQueryString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds the database / credentials query string used by the InfluxDB
     * {@code /write} endpoint.
     *
     * @return the {@code precision=ms&db=...&u=...&p=...&rp=...} query string.
     */
    private String buildQueryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("precision=ms").append("&db=").append(database);
        appendParamIfNotEmptyOrNull(sb, "u", user);
        appendParamIfNotEmptyOrNull(sb, "p", password);
        appendParamIfNotEmptyOrNull(sb, "rp", retentionPolicy);
        return sb.toString();
    }

    /**
     * Pushes a batch of {@link QueryResult}s to InfluxDB. The writer honours
     * both the master enable flag and the consecutive-failure circuit
     * breaker &mdash; when the failure count exceeds {@link #retryTimes} the
     * call becomes a no-op. All exceptions are caught and logged at the
     * {@code WARN} level so that an unreachable InfluxDB cannot break the
     * surrounding JmxTrans collection loop.
     *
     * @param results the query results accumulated during the previous
     *                collection cycle.
     */
	@Override
	public void write(Iterable<QueryResult> results) {

		try {
			if(!enabled) return;
			if(LOG.isDebugEnabled()){
				LOG.debug("Export to '{}', proxy {} metrics {}", url, proxy, results);
			}
			if( exceptionCounter.get() > retryTimes){
				return;
			}
			for (QueryResult result : results) {
		        if(LOG.isDebugEnabled()){
		        	String msg = result.getName() + " " + result.getValue() + " " + result.getEpoch(TimeUnit.SECONDS);
		        	LOG.debug(msg);
		        }
		        String metricName = result.getName();
		        Object value = result.getValue();

		        InfluxMetric metric = InfluxMetricConverter.convertToInfluxMetric(this.getStrategy(), metricName, value, tags, SystemClock.now());
		        batchedMetrics.add(metric);

			}

	        String body = convertMetricsToLines(batchedMetrics);
	        String queryString = buildQueryString();
	        if(LOG.isDebugEnabled()){
	        	LOG.debug( "Sending to influx (" + url + "):\n" + body);
	        }
	        batchedMetrics.clear();

	        sendMetrics(queryString, body);

		} catch (Exception e) {
			exceptionCounter.incrementAndGet();
			if(LOG.isWarnEnabled()){
				LOG.warn("Failure to send result to InfluxDb '{}' with proxy {}", url, proxy, e);
			}
		}
	}

    /**
     * Opens a fresh {@link HttpURLConnection}, configures timeouts and the
     * {@code POST} method, writes the metrics body, then disconnects.
     *
     * @param queryString the already-built query string (re-built here to
     *                    preserve historical behaviour).
     * @param body        the InfluxDB line-protocol body.
     * @throws IOException if the connection or write fails.
     */
    private void sendMetrics(String queryString, String body) throws IOException {
        HttpURLConnection conn = createAndConfigureConnection();
        try {
            sendMetrics(body, conn);
        } finally {
            IoUtils.closeQuietly(conn);
        }
    }

    /**
     * Sends the batch body to the configured InfluxDB endpoint and verifies
     * the HTTP response code; a non-{@code 2xx} code is wrapped in a
     * {@link RuntimeException} and bumps the failure counter.
     *
     * @param body           the line-protocol body.
     * @param urlConnection  an already-configured HTTP connection.
     * @throws IOException if reading the connection stream fails.
     */
    private void sendMetrics(String body, HttpURLConnection urlConnection) throws IOException {
        writeMetrics(urlConnection, body);
        int responseCode = urlConnection.getResponseCode();
        if (responseCode / 100 != 2) {
        	exceptionCounter.incrementAndGet();
            throw new RuntimeException("Failed to write metrics, response code: " + responseCode  + ", response message: " + urlConnection.getResponseMessage());
        }
        String response = readResponse(urlConnection);
        if(LOG.isDebugEnabled()){
        	LOG.debug("Response from influx: " + response);
        }
    }

    /**
     * Opens a {@link HttpURLConnection} and configures timeouts + method.
     *
     * @return a configured, unconnected {@link HttpURLConnection}.
     * @throws ProtocolException if the {@code POST} method cannot be set.
     */
    private HttpURLConnection createAndConfigureConnection() throws ProtocolException {
        HttpURLConnection conn = openHttpConnection();
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        return conn;
    }

    /**
     * Opens an {@link HttpURLConnection} either directly or via the configured
     * proxy; any I/O failure is wrapped in an {@link IoRuntimeException}.
     *
     * @return a freshly-opened {@link HttpURLConnection}.
     * @throws IoRuntimeException if the URL could not be opened.
     */
    private HttpURLConnection openHttpConnection() {
        try {
    		HttpURLConnection urlConnection = null;
        	if (proxy == null) {
    			urlConnection = (HttpURLConnection) url.openConnection();
    		} else {
    			urlConnection = (HttpURLConnection) url.openConnection(proxy);
    		}
            return urlConnection;
        } catch (Exception e) {
            throw new IoRuntimeException("Failed to create HttpURLConnection to " + url + " - is it a valid HTTP url?",  e);
        }
    }

    /**
     * Writes the request body to the given HTTP connection.
     *
     * @param conn the destination connection.
     * @param body the line-protocol payload.
     * @throws UnsupportedEncodingException never thrown because the project
     *                                      always uses UTF-8.
     * @throws IOException if writing fails.
     */
    private void writeMetrics(HttpURLConnection conn, String body)
            throws UnsupportedEncodingException, IOException {
        byte[] toSendBytes = body.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", Integer.toString(toSendBytes.length));
        OutputStream os = null;
        try {
        	os = conn.getOutputStream();
			os.write(toSendBytes);
            os.flush();
		} finally {
			if(os != null){
				IoUtils2.closeQuietly(os);
			}
		}
    }

    /**
     * Reads and returns the InfluxDB response body as a UTF-8 string.
     *
     * @param conn the connection whose input stream is read.
     * @return the response body, possibly empty.
     * @throws IOException if reading fails.
     * @throws UnsupportedEncodingException never thrown &mdash; the project
     *                                      always uses UTF-8.
     */
    private String readResponse(HttpURLConnection conn) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
        	is = conn.getInputStream();
        	IoUtils2.copy(is, baos);
		} finally {
			if(is != null){
				IoUtils2.closeQuietly(is);
			}
		}
        String response = new String(baos.toByteArray(), "UTF-8");
        return response;
    }

    /**
     * Appends a query parameter to {@code sb} if and only if the value is
     * neither {@code null} nor blank. Values are appended verbatim &mdash; no
     * URL encoding is applied.
     *
     * @param sb        accumulator being built.
     * @param paramName parameter name (without the leading {@code &}).
     * @param paramValue parameter value to append when non-blank.
     */
    private void appendParamIfNotEmptyOrNull(StringBuilder sb, String paramName, String paramValue) {
        if (paramValue != null && !paramValue.trim().isEmpty()) {
            // NB: We do not URL encode anything, from what I understand from the Influx docs,
            // encoded data is not expected.
            sb.append("&").append(paramName).append("=").append(paramValue);
        }

    }

    /**
     * Joins the supplied metrics into the InfluxDB line-protocol body by
     * separating each metric with a newline. Returns an empty string when
     * {@code metrics} is empty.
     *
     * @param metrics the metrics to render.
     * @return the textual body, never {@code null}.
     */
    private String convertMetricsToLines(List<InfluxMetric> metrics) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<InfluxMetric> it = metrics.iterator(); it.hasNext();) {
            InfluxMetric metric = it.next();
            sb.append(metric.toInfluxFormat());
            if (it.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the configured InfluxDB base URL or a sensible default when
     * {@code null} is provided.
     *
     * @param url the configured URL, may be {@code null}.
     * @return the effective base URL, never {@code null}.
     */
	public String getUrl(String url) {
		return url== null ? "http://127.0.0.1:8086" : url;
	}

    /**
     * Returns the configured InfluxDB database name or a sensible default
     * when {@code null} is provided.
     *
     * @param database the configured database name, may be {@code null}.
     * @return the effective database name, never {@code null}.
     */
	public String getDatabase(String database) {
		return database== null ? "Metrics_127.0.0.1" : database;
	}

    /**
     * Returns the configured InfluxDB user name (passthrough; {@code null}
     * means "no HTTP basic auth").
     *
     * @param user the configured user name, may be {@code null}.
     * @return the user name or {@code null}.
     */
	public String getUser(String user) {
		return user;
	}

    /**
     * Returns the configured InfluxDB password (passthrough; {@code null}
     * means "no HTTP basic auth").
     *
     * @param password the configured password, may be {@code null}.
     * @return the password or {@code null}.
     */
	public String getPassword(String password) {
		return password;
	}

}
