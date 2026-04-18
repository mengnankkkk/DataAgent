/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.service.contextrefiner;

import com.alibaba.cloud.ai.dataagent.service.contextrefiner.dto.ContextRefineRequest;
import com.alibaba.cloud.ai.dataagent.service.contextrefiner.dto.ContextRefineResponse;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class WebClientContextRefinerClient implements ContextRefinerClient {

	private final WebClient.Builder webClientBuilder;

	@Override
	public ContextRefineResponse refine(String endpoint, ContextRefineRequest request, Duration timeout) {
		Objects.requireNonNull(endpoint, "endpoint must not be null");
		Objects.requireNonNull(request, "request must not be null");
		return webClientBuilder.build()
			.post()
			.uri(endpoint)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError,
					response -> response.bodyToMono(String.class).defaultIfEmpty("").map(body -> new IllegalStateException(
							"Context refine request failed, status=%s, body=%s"
								.formatted(response.statusCode().value(), body))))
			.bodyToMono(ContextRefineResponse.class)
			.timeout(timeout == null ? Duration.ofSeconds(30) : timeout)
			.block();
	}

}
