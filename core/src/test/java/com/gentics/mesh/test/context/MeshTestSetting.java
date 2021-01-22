package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.context.MeshOptionChanger.NO_CHANGE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.SSLTestMode;
import com.gentics.mesh.test.TestSize;

/**
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	/**
	 * Flag which indicates whether the ES docker container should be started.
	 * 
	 * @return
	 */
	ElasticsearchTestMode elasticsearch() default NONE;

	/**
	 * Setting which indicates what size of test data should be created.
	 * 
	 * @return
	 */
	TestSize testSize() default PROJECT;

	/**
	 * Flag which indicates whether the mesh http server should be started.
	 * 
	 * @return
	 */
	boolean startServer() default false;

	/**
	 * Flag which indicates whether the graph database should run in-memory mode.
	 * 
	 * @return
	 */
	boolean inMemoryDB() default true;

	/**
	 * Flag which indicates whether the graph database server should be started (only if {@link #inMemoryDB()} is false)
	 * 
	 * @return
	 */
	boolean startStorageServer() default false;

	/**
	 * Flag which indicates whether the keycloak server should be used.
	 * 
	 * @return
	 */
	boolean useKeycloak() default false;

	/**
	 * Flag which indicates that the cluster mode should be enabled.
	 * 
	 * @return
	 */
	boolean clusterMode() default false;

	/**
	 * SSL test mode.
	 * 
	 * @return
	 */
	SSLTestMode ssl() default SSLTestMode.OFF;

	/**
	 * Flag which indicates whether the monitoring feature should be enabled.
	 * 
	 * @return
	 */
	boolean monitoring() default true;

	/**
	 * Predefined set of options changers that can be used to alter the mesh options for the test.
	 * 
	 * @return
	 */
	MeshOptionChanger optionChanger() default NO_CHANGE;
	
	/**
	 * {@link MeshOptions} provider, that is also used to change options-related configuration.
	 * 
	 * @return
	 */
	Class<? extends MeshOptionsProvider> optionsProvider() default OrientDBMeshOptionsProvider.class;
}
