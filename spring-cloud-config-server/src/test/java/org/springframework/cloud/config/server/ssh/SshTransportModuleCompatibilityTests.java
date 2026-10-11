/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.config.server.ssh;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Default SSH transport remains available after the JGit module split.
 *
 * @author Laisky
 */
public class SshTransportModuleCompatibilityTests {

	@Test
	public void defaultFactoryRetainsJschTransport() {
		SshSessionFactory previous = SshSessionFactory.getInstance();
		try {
			SshSessionFactory.setInstance(null);
			assertThat(SshSessionFactory.getInstance())
					.isInstanceOf(JschConfigSessionFactory.class);
		}
		finally {
			SshSessionFactory.setInstance(previous);
		}
	}

}
