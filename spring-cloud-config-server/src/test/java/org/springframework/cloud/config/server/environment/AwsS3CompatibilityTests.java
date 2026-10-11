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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.config.ConfigServerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Offline S3 request and stream behavior across the qualified SDK versions.
 *
 * @author Laisky
 */
public class AwsS3CompatibilityTests {

	@Test
	public void propertiesArePreferredWithoutRequestingFallbackFormats() {
		AmazonS3 client = mock(AmazonS3.class);
		S3Object object = object("message=hello\n");
		when(client.getObject(any(GetObjectRequest.class))).thenReturn(object);
		Environment result = repository(client, new ConfigServerProperties())
				.findOne("sample", "default", "label");
		ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor
				.forClass(GetObjectRequest.class);
		verify(client).getObject(request.capture());
		assertThat(request.getValue().getBucketName()).isEqualTo("fixture");
		assertThat(request.getValue().getKey())
				.isEqualTo("label/sample-default.properties");
		assertThat(result.getPropertySources().get(0).getSource().get("message"))
				.isEqualTo("hello");
	}

	@Test
	public void serverOverridesPreservePrecedence() {
		AmazonS3 client = mock(AmazonS3.class);
		when(client.getObject(any(GetObjectRequest.class)))
				.thenReturn(object("message=file\n"));
		ConfigServerProperties properties = new ConfigServerProperties();
		properties.setOverrides(Collections.singletonMap("message", "override"));
		Environment result = repository(client, properties).findOne("sample", "default",
				null);
		assertThat(result.getPropertySources().get(0).getSource().get("message"))
				.isEqualTo("override");
	}

	@Test
	public void absentFilesTryAllFormatsInOrder() {
		AmazonS3 client = mock(AmazonS3.class);
		Throwable failure = catchThrowable(
				() -> repository(client, new ConfigServerProperties()).findOne("sample",
						"default", null));
		assertThat(failure).isInstanceOf(NoSuchRepositoryException.class);
		ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor
				.forClass(GetObjectRequest.class);
		verify(client, times(3)).getObject(request.capture());
		List<String> keys = request.getAllValues().stream().map(GetObjectRequest::getKey)
				.collect(Collectors.toList());
		assertThat(keys).containsExactly("sample-default.properties",
				"sample-default.yml", "sample-default.json");
	}

	@Test
	public void loadedObjectStreamIsClosed() {
		AmazonS3 client = mock(AmazonS3.class);
		TrackingInputStream stream = new TrackingInputStream("message=hello\n");
		S3Object object = new S3Object();
		object.setObjectContent(stream);
		when(client.getObject(any(GetObjectRequest.class))).thenReturn(object);
		repository(client, new ConfigServerProperties()).findOne("sample", "default",
				null);
		assertThat(stream.closed).isTrue();
	}

	private AwsS3EnvironmentRepository repository(AmazonS3 client,
			ConfigServerProperties properties) {
		return new AwsS3EnvironmentRepository(client, "fixture", properties);
	}

	private S3Object object(String content) {
		S3Object object = new S3Object();
		object.setObjectContent(
				new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
		return object;
	}

	private static class TrackingInputStream extends ByteArrayInputStream {

		private boolean closed;

		TrackingInputStream(String content) {
			super(content.getBytes(StandardCharsets.UTF_8));
		}

		@Override
		public void close() throws IOException {
			this.closed = true;
			super.close();
		}

	}

}
