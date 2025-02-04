package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.field.FieldType;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Project
 */
public class ProjectImpl extends AbstractMeshCoreVertex<ProjectResponse> implements Project {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		// TODO index to name + unique constraint
		type.createVertexType(ProjectImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(ProjectImpl.class)
			.withField("name", FieldType.STRING)
			.unique());
		addUserTrackingRelation(ProjectImpl.class);
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void addLanguage(HibLanguage language) {
		setUniqueLinkOutTo(toGraph(language), HAS_LANGUAGE);
	}

	@Override
	public Result<? extends Language> getLanguages() {
		return out(HAS_LANGUAGE, LanguageImpl.class);
	}

	@Override
	public void removeLanguage(HibLanguage language) {
		unlinkOut(toGraph(language), HAS_LANGUAGE);
	}

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public TagFamilyRoot getTagFamilyRoot() {
		TagFamilyRoot root = out(HAS_TAGFAMILY_ROOT, TagFamilyRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(TagFamilyRootImpl.class);
			linkOut(root, HAS_TAGFAMILY_ROOT);
		}
		return root;
	}

	@Override
	public SchemaRoot getSchemaContainerRoot() {
		SchemaRoot root = out(HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectSchemaContainerRootImpl.class);
			linkOut(root, HAS_SCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public HibBaseElement getSchemaPermissionRoot() {
		return getSchemaContainerRoot();
	}

	@Override
	public MicroschemaRoot getMicroschemaContainerRoot() {
		MicroschemaRoot root = out(HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(ProjectMicroschemaContainerRootImpl.class);
			linkOut(root, HAS_MICROSCHEMA_ROOT);
		}
		return root;
	}

	@Override
	public Node getBaseNode() {
		return out(HAS_ROOT_NODE, NodeImpl.class).nextOrNull();
	}

	@Override
	public NodeRoot getNodeRoot() {
		NodeRoot root = out(HAS_NODE_ROOT, NodeRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(NodeRootImpl.class);
			linkOut(root, HAS_NODE_ROOT);
		}
		return root;
	}

	@Override
	public void setBaseNode(HibNode baseNode) {
		linkOut(toGraph(baseNode), HAS_ROOT_NODE);
	}

	/**
	 * @deprecated Use Dao method instead.
	 */
	@Override
	@Deprecated
	public void delete(BulkActionContext bac) {
		ProjectDao projectDao = Tx.get().projectDao();
		projectDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public HibBranch getInitialBranch() {
		return getBranchRoot().getInitialBranch();
	}

	@Override
	public HibBranch getLatestBranch() {
		return getBranchRoot().getLatestBranch();
	}

	@Override
	public BranchRoot getBranchRoot() {
		BranchRoot root = out(HAS_BRANCH_ROOT, BranchRootImpl.class).nextOrNull();
		if (root == null) {
			root = getGraph().addFramedVertex(BranchRootImpl.class);
			linkOut(root, HAS_BRANCH_ROOT);
		}
		return root;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		ProjectDao projectRoot = Tx.get().projectDao();
		return projectRoot.getSubETag(this, ac);
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public Result<? extends Node> findNodes() {
		return db().getVerticesTraversal(NodeImpl.class, new String[] { PROJECT_KEY_PROPERTY }, new Object[] { getUuid() });
	}

	@Override
	public Node findNode(String uuid) {
		return db().getVerticesTraversal(NodeImpl.class,
			new String[] { PROJECT_KEY_PROPERTY, "uuid" },
			new Object[] { getUuid(), uuid }).nextOrNull();
	}

	@Override
	public HibBaseElement getBranchPermissionRoot() {
		return getBranchRoot();
	}

	@Override
	public HibBaseElement getTagFamilyPermissionRoot() {
		return getTagFamilyRoot();
	}

	@Override
	public HibBaseElement getNodePermissionRoot() {
		return getNodeRoot();
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}
}
