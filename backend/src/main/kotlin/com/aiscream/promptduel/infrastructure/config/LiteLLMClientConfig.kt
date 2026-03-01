package com.aiscream.promptduel.infrastructure.config

import com.aiscream.promptduel.infrastructure.litellm.LiteLLMHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class LiteLLMClientConfig {

    @Bean
    fun liteLLMHttpClient(props: LiteLLMProperties): LiteLLMHttpClient {
        val baseUrl = "http://${props.host}:${props.port}"

        val requestFactory = SimpleClientHttpRequestFactory().also {
            it.setConnectTimeout(5_000)
            it.setReadTimeout(props.timeoutMs.toInt())
        }

        val builder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val restClient = if (props.apiKey.isNotBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${props.apiKey}").build()
        } else {
            builder.build()
        }

        val adapter = RestClientAdapter.create(restClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()

        return factory.createClient(LiteLLMHttpClient::class.java)
    }
}
