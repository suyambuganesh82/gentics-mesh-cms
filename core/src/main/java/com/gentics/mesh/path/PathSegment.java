package com.gentics.mesh.path;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;

/**
 * A webroot path is build using multiple path segments. Each node is able to provide multiple path segments. Each language of the node may provide different
 * path segments.
 */
public class PathSegment {

	private Node node;
	private GraphField pathField;
	private String languageTag;

	/**
	 * Create a new path segment.
	 * 
	 * @param node
	 *            Node that provides this segment
	 * @param pathField
	 *            Graph field which is the source of the segment
	 * @param languageTag
	 *            Language of the segment field
	 */
	public PathSegment(Node node, GraphField pathField, String languageTag) {
		this.node = node;
		this.pathField = pathField;
		this.languageTag = languageTag;
	}

	/**
	 * Return the node for the segment.
	 * 
	 * @return
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * Return the graph field which provides this segment for the node.
	 * 
	 * @return
	 */
	public GraphField getPathField() {
		return pathField;
	}

	/**
	 * Return the language tag of the node which provides this segment.
	 * 
	 * @return
	 */
	public String getLanguageTag() {
		return languageTag;
	}

}
