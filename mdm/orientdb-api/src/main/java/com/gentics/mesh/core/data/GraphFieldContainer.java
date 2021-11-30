package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.field.list.HibListField;

/**
 * A graph field container (eg. a container for fields of a node) is used to hold i18n specific graph fields.
 */
public interface GraphFieldContainer extends HibFieldContainer, BasicFieldContainer {

	/**
	 * Delete the field edge with the given key from the container.
	 * 
	 * @param key
	 */
	void deleteFieldEdge(String key);

	/**
	 * Get the list field of specified type
	 * 
	 * @param classOfT
	 * @param fieldKey
	 * @return
	 */
	<T extends HibListField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey);
}
