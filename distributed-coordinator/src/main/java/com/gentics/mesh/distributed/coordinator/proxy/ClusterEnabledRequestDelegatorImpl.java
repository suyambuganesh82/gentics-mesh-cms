package com.gentics.mesh.distributed.coordinator.proxy;

import java.util.HashSet;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.gentics.mesh.distributed.DistributionUtils;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.httpproxy.ProxyRequest;

/**
 * @see RequestDelegator
 */
public class ClusterEnabledRequestDelegatorImpl implements RequestDelegator {

	private static final Logger log = LoggerFactory.getLogger(ClusterEnabledRequestDelegatorImpl.class);

	public static final String MESH_DIRECT_HEADER = "X-Mesh-Direct";

	private final Coordinator coordinator;
	private final HttpClient httpClient;
	private final MeshOptions options;

	private static final Set<Pattern> blacklistPathPatternSet = createBlacklistPatternSet();

	private static final Set<Pattern> whiteListPathPatternSet = createWhitelistPatternSet();

	@Inject
	public ClusterEnabledRequestDelegatorImpl(Coordinator coordinator, Vertx vertx, MeshOptions options) {
		this.coordinator = coordinator;
		this.httpClient = vertx.createHttpClient();
		this.options = options;
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		String requestURI = request.uri();
		String path = request.path();
		HttpMethod method = request.method();

		if (!options.getClusterOptions().isEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegation since clustering is not enabled.");
			}
			rc.next();
			return;
		}

		CoordinatorMode mode = coordinator.getCoordinatorMode();
		if (mode == CoordinatorMode.DISABLED) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegation since coordination is disabled.");
			}
			rc.next();
			return;
		}

		if (isWhitelisted(path) && !isBlacklisted(path)) {
			if (log.isDebugEnabled()) {
				log.debug("URI {" + requestURI + "} with method {" + method.name() + "} is whitelisted. Skipping delegation");
			}
			rc.next();
			return;
		}

		String headerValue = request.getHeader(MESH_DIRECT_HEADER);
		if (headerValue != null && headerValue.equalsIgnoreCase("true")) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator due to direct header");
			}
			rc.next();
			return;
		}

		// In CUD mode we only delegate mutating requests to the master
		if (mode == CoordinatorMode.CUD) {
			if (DistributionUtils.isReadRequest(method, path)) {
				rc.next();
				return;
			}
		}

		MasterServer master = coordinator.getMasterMember();
		if (master == null) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator since no master was elected.");
			}
			rc.next();
			return;
		}
		// We don't need to delegate the request if we are the master
		if (master.isSelf()) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator since we are the master");
			}
			rc.next();
			return;
		}

		redirectToMaster(rc);
	}

	@Override
	public boolean canWrite() {
		if (!options.getClusterOptions().isEnabled() || CoordinatorMode.DISABLED.equals(coordinator.getCoordinatorMode())) {
			return true;
		} else {
			MasterServer master = coordinator.getMasterMember();
			return master != null && master.isSelf();
		}
	}

	@Override
	public void redirectToMaster(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		MasterServer master = coordinator.getMasterMember();
		String host = master.getHost();
		int port = master.getPort();
		if (log.isDebugEnabled()) {
			log.debug("Forwarding request to master {" + master.toString() + "}");
		}

		ProxyRequest proxyRequest = ProxyRequest.reverseProxy(request);
		proxyRequest.putHeader(MESH_DIRECT_HEADER, "true");

		httpClient.request(proxyRequest.getMethod(), port, host, proxyRequest.getURI())
				.compose(proxyRequest::send)
				.onSuccess(proxyResponse -> {
					// Send the proxy response
					proxyResponse.putHeader(MESH_FORWARDED_FROM_HEADER, master.getName());
					proxyResponse.send();
				})
				.onFailure(err -> {
					// Release the request
					proxyRequest.release();

					// Send error
					request.response().setStatusCode(500)
							.send();
				});
	}

	@Override
	public boolean isMaster() {
		return coordinator.isMaster();
	}

	/**
	 * Log the given messages with loglevel <code>TRACE</code>.
	 *
	 * @param label
	 *            A message that will be logged before the headers
	 * @param headers
	 *            The HTTP headers to log
	 */
	private void printHeaders(String label, MultiMap headers) {
		if (log.isTraceEnabled()) {
			log.trace(label + " ({})", headers.size());
			headers.forEach(header -> log.trace("  {}: {}", header.getKey(), header.getValue()));
		}
	}

	/**
	 * End the given <code>forwardRequest</code> optionally with the provided body if it is not <code>null</code>.
	 *
	 * @param forwardRequest
	 *            The proxy request
	 * @param body
	 *            The optional body for the request (may by <code>null</code>)
	 */
	private void proxyEndHandler(HttpClientRequest forwardRequest, Buffer body) {
		if (body == null) {
			forwardRequest.end();
		} else {
			forwardRequest.end(body);
		}
	}

	private void forwardHeaders(HttpServerResponse response, HttpClientResponse forwardResponse) {
		MultiMap headers = forwardResponse.headers();

		for (String headerName : headers.names()) {
			response.putHeader(headerName, headers.getAll(headerName));
		}
	}

	private void forwardHeaders(HttpServerRequest request, HttpClientRequest forwardRequest) {
		MultiMap headers = request.headers();

		for (String headerName : headers.names()) {
			forwardRequest.putHeader(headerName, headers.getAll(headerName));
		}

	}

	/**
	 * Check whether the given path is blacklisted (meaning: should be delegated to the master, even if also whitelisted)
	 * @param path path to check
	 * @return true iff the path is blacklisted
	 */
	public static boolean isBlacklisted(String path) {
		for (Pattern pattern : blacklistPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given path is whitelisted (meaning: should not be delegated to the master)
	 * @param path path to check
	 * @return true iff the path is whitelisted
	 */
	public static boolean isWhitelisted(String path) {
		for (Pattern pattern : whiteListPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	private static Set<Pattern> createBlacklistPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		patterns.add(Pattern.compile("/api/v[0-9]+/admin/processJobs"));
		patterns.add(Pattern.compile("/api/v[0-9]+/admin/jobs/.*/process"));
		return patterns;
	}

	private static Set<Pattern> createWhitelistPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		patterns.add(Pattern.compile("/api/v[0-9]+/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/admin/.*"));
		patterns.add(Pattern.compile("/api/v[0-9]+/health/live/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/health/ready/?"));
		return patterns;
	}

}
