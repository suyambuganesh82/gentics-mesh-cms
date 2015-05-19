package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.ObjectSchema;
import com.gentics.mesh.core.data.service.ObjectSchemaService;
import com.gentics.mesh.core.repository.ObjectSchemaRepository;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.test.AbstractDBTest;

public class ObjectSchemaTest extends AbstractDBTest {

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private ObjectSchemaRepository objectSchemaRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testFindByName() {
		assertNotNull(objectSchemaService.findByName(DemoDataProvider.PROJECT_NAME, "content"));
		assertNull(objectSchemaService.findByName(DemoDataProvider.PROJECT_NAME, "content1235"));
	}

	@Test
	public void testDeleteByObject() {
		ObjectSchema schema = data().getSchema("content");
		objectSchemaService.delete(schema);
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByUUID() {
		ObjectSchema schema = data().getSchema("content");
		try (Transaction tx = graphDb.beginTx()) {

			objectSchemaService.deleteByUUID(schema.getUuid());
			tx.success();
		}
		assertNull(objectSchemaService.findOne(schema.getId()));
	}

	// @Test
	// public void testDeleteWithNoPermission() {
	// UserInfo info = data().getUserInfo();
	// ObjectSchema schema = data().getContentSchema();
	// try (Transaction tx = graphDb.beginTx()) {
	// roleService.revokePermission(info.getRole(), schema, PermissionType.DELETE);
	// objectSchemaService.deleteByUUID(schema.getUuid());
	// tx.success();
	// }
	// assertNotNull(objectSchemaService.findOne(schema.getId()));
	// }

	@Test
	public void testObjectSchemaRootNode() {
		int nSchemasBefore = objectSchemaRepository.findRoot().getSchemas().size();

		ObjectSchema schema = new ObjectSchema("test1235");
		try (Transaction tx = graphDb.beginTx()) {
			objectSchemaRepository.save(schema);
			tx.success();
		}

		int nSchemasAfter = objectSchemaRepository.findRoot().getSchemas().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}
}
