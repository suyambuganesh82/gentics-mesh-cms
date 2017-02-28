package com.gentics.mesh.search;

import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import com.gentics.mesh.util.MeshAssert;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class ProjectSearchEndpointTest extends AbstractMeshTest implements BasicSearchCrudTestcases {

	@Test
	public void testSearchProject() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}

		MeshResponse<ProjectListResponse> future = client()
				.searchProjects(getSimpleQuery("dummy"), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse response = future.result();
		assertEquals(1, response.getData().size());

		future = client().searchProjects(getSimpleQuery("blub"), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(0, response.getData().size());

		future = client().searchProjects(getSimpleTermQuery("name.raw", "dummy"), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {

		final String newName = "newproject";
		ProjectResponse project = createProject(newName);
		try (NoTx noTx = db().noTx()) {
			MeshAssert.assertElement(boot().projectRoot(), project.getUuid(), true);
		}
		MeshResponse<ProjectListResponse> future = client()
				.searchProjects(getSimpleTermQuery("name.raw", newName), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		MeshResponse<ProjectListResponse> future = client()
				.searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());

		deleteProject(project.getUuid());
		future = client().searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		String newProjectName = "updatedprojectname";
		updateProject(project.getUuid(), newProjectName);

		MeshResponse<ProjectListResponse> future = client()
				.searchProjects(getSimpleTermQuery("name.raw", projectName), new PagingParametersImpl().setPage(1).setPerPage(2)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());

		future = client().searchProjects(getSimpleTermQuery("name.raw", newProjectName), new PagingParametersImpl().setPage(1).setPerPage(2))
				.invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());
	}

}