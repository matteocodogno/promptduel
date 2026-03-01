package com.aiscream.promptduel.infrastructure.config

import com.aiscream.promptduel.infrastructure.litellm.LiteLLMHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.ImportHttpServices
import java.time.Duration

@Configuration
@ImportHttpServices(group = "litellm", types = [LiteLLMHttpClient::class])
class LiteLLMClientConfig(
    private val props: LiteLLMProperties,
) {
    @Bean
    fun liteLLMGroupConfigurer(): RestClientHttpServiceGroupConfigurer =
        RestClientHttpServiceGroupConfigurer { groups ->
            groups.filterByName("litellm").forEachClient { _, builder ->
                val requestFactory =
                    SimpleClientHttpRequestFactory().also {
                        it.setConnectTimeout(Duration.ofSeconds(5))
                        it.setReadTimeout(Duration.ofMillis(props.timeoutMs))
                    }
                builder
                    .requestFactory(requestFactory)
                    .baseUrl("http://${props.host}:${props.port}")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                if (props.apiKey.isNotBlank()) {
                    builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.apiKey}")
                }
            }
        }
}
