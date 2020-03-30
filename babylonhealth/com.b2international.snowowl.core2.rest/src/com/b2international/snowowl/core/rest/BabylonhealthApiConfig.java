package com.b2international.snowowl.core.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.spring.web.plugins.Docket;

/**
 * @since 7.5
 */
@Configuration
public class BabylonhealthApiConfig extends BaseApiConfig {

	@Override
	public String getApiBaseUrl() {
		return "/bbl";
	}
	
	@Bean
	public Docket coreDocs() {
		return docs(
			getApiBaseUrl(),
			"bbl",
			"1.0",
			"Bbl API",
			"",
			"",
			"",
			"",
			""
		);
	}
	
}
