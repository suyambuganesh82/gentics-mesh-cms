package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.test.AbstractDBTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ProjectTest extends AbstractDBTest {

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ContentService contentService;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testCreation() {

		Project project = new Project("test");
		projectRepository.save(project);
		project = projectRepository.findOne(project.getId());
		assertNotNull(project);
		assertEquals("test", project.getName());
	}

	@Test
	@Transactional
	public void testProjectFileLocating() {

		Language english = languageService.findByName("english");
		@SuppressWarnings("rawtypes")
		GenericFile file = projectRepository.findFileByPath(DummyDataProvider.PROJECT_NAME, "/subtag/english.html");
		assertNotNull("A file within the given path should be found.", file);
		Content content = (Content) file;
		assertEquals("The content of the found file did not match the expected one.", DummyDataProvider.ENGLISH_CONTENT, content.getContent(english));
	}

}
