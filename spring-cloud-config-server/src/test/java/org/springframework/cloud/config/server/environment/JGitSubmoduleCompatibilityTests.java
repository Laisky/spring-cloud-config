/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.config.server.environment;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Local Git behavior retained by the fork's submodule clone support.
 *
 * @author Laisky
 */
public class JGitSubmoduleCompatibilityTests {

	@Rule
	public final TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void cloneReadsConfigurationFromSubmodule() throws Exception {
		File child = this.temporary.newFolder("child");
		try (Git git = Git.init().setDirectory(child).call()) {
			write(child, "application.properties", "submodule.message=hello\n");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("fixture child")
					.setAuthor("Fixture", "fixture@example.test").call();
		}
		File parent = this.temporary.newFolder("parent");
		try (Git git = Git.init().setDirectory(parent).call()) {
			write(parent, "README.txt", "fixture\n");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("fixture parent")
					.setAuthor("Fixture", "fixture@example.test").call();
			try (Repository submodule = git.submoduleAdd().setPath("nested")
					.setURI(child.getAbsolutePath()).call()) {
				assertThat(submodule.resolve("HEAD")).isNotNull();
			}
			git.add().addFilepattern(".gitmodules").addFilepattern("nested").call();
			git.commit().setMessage("fixture submodule")
					.setAuthor("Fixture", "fixture@example.test").call();
		}
		File cloned = this.temporary.newFolder("clone");
		JGitEnvironmentRepository repository = repository(parent, cloned);
		repository.setSearchPaths("nested");
		Environment result = repository.findOne("application", "default", "master");
		assertThat(new File(cloned, "nested/application.properties")).isFile();
		assertThat(result.getPropertySources()).anySatisfy(
				source -> assertThat(source.getSource().get("submodule.message"))
						.isEqualTo("hello"));
		assertThat(result.getVersion()).isNotEmpty();
		assertThat(cloned).isNotEqualTo(parent);
	}

	@Test
	public void rootConfigurationWithoutSubmoduleStillLoads() throws Exception {
		File remote = this.temporary.newFolder("plain");
		try (Git git = Git.init().setDirectory(remote).call()) {
			write(remote, "application.properties", "root.message=hello\n");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("fixture plain")
					.setAuthor("Fixture", "fixture@example.test").call();
		}
		File cloned = this.temporary.newFolder("plain-clone");
		Environment result = repository(remote, cloned).findOne("application", "default",
				"master");
		assertThat(new File(cloned, "application.properties")).isFile();
		assertThat(result.getPropertySources())
				.anySatisfy(source -> assertThat(source.getSource().get("root.message"))
						.isEqualTo("hello"));
	}

	private JGitEnvironmentRepository repository(File remote, File cloned) {
		JGitEnvironmentRepository repository = new JGitEnvironmentRepository(
				new StandardEnvironment(), new JGitEnvironmentProperties());
		// A plain absolute path reaches the clone path; file: URLs read in place.
		repository.setUri(remote.getAbsolutePath());
		repository.setBasedir(cloned);
		return repository;
	}

	private void write(File directory, String name, String content) throws Exception {
		Files.write(new File(directory, name).toPath(),
				content.getBytes(StandardCharsets.UTF_8));
	}

}
