package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import dagger.Lazy;
import io.micrometer.core.instrument.Timer;

/**
 * @see WriteLock
 */
@Singleton
public class OrientDBWriteLockImpl extends AbstractGenericWriteLock {

	protected ILock clusterLock;
	protected final Lazy<HazelcastInstance> hazelcast;
	protected final boolean isClustered;
	protected final ClusterManager clusterManager;
	private final OrientDBMeshOptions options;

	@Inject
	public OrientDBWriteLockImpl(OrientDBMeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService, ClusterManager clusterManager) {
		super(options, metricsService);
		this.options = options;
		this.hazelcast = hazelcast;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.clusterManager = clusterManager;
	}

	@Override
	public void close() {
		if (isClustered) {
			if (clusterLock != null && clusterLock.isLockedByCurrentThread()) {
				clusterLock.unlock();
			}
		} else {
			localLock.release();
		}
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public WriteLock lock(InternalActionContext ac) {
		if (ac != null && ac.isSkipWriteLock()) {
			return this;
		} else {
			// throw an error, if the cluster topology is currently locked and the option "topology change readonly" is activated
			if (options.getClusterOptions().isTopologyChangeReadOnly() && clusterManager != null
					&& clusterManager.isClusterTopologyLocked()) {
				throw error(SERVICE_UNAVAILABLE, "error_cluster_topology_readonly").setLogStackTrace(false);
			}

			if (isSyncWrites()) {
				Timer.Sample timer = Timer.start();
				long timeout = getSyncWritesTimeoutMillis();
				if (isClustered) {
					try {
						if (clusterLock == null) {
							HazelcastInstance hz = hazelcast.get();
							if (hz != null) {
								this.clusterLock = hz.getLock(GLOBAL_LOCK_KEY);
							}
						}
						if (clusterLock != null) {
							boolean isTimeout = !clusterLock.tryLock(timeout, TimeUnit.MILLISECONDS);
							if (isTimeout) {
								timeoutCount.increment();
								throw new RuntimeException("Got timeout while waiting for write lock.");
							}
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} finally {
						timer.stop(writeLockTimer);
					}
				} else {
					try {
						boolean isTimeout = !localLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
						if (isTimeout) {
							timeoutCount.increment();
							throw new RuntimeException("Got timeout while waiting for write lock.");
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} finally {
						timer.stop(writeLockTimer);
					}
				}
			}
			return this;
		}
	}

	@Override
	public boolean isSyncWrites() {
		return options.getStorageOptions().isSynchronizeWrites();
	}

	@Override
	protected long getSyncWritesTimeoutMillis() {
		return options.getStorageOptions().getSynchronizeWritesTimeout();
	}
}
