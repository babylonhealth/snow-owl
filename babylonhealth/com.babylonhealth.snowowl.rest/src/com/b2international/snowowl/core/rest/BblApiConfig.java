package com.b2international.snowowl.core.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.spring.web.plugins.Docket;

/**
 * The Spring configuration class for Snow Owl's FHIR REST API.
 * 
 * @since 7.5
 */
@Configuration
@ComponentScan("com.b2international.snowowl.core.rest")
public class BblApiConfig extends BaseApiConfig {

	@Override
	public String getApiBaseUrl() {
		return "/bbl";
	}

	@Bean
	public Docket fhirDocs() {
		return docs(
			getApiBaseUrl(), 
			"bbl", 
			"1.0", 
			"BBl API", 
			"wd", 
			"wewqe", 
			"qwe", 
			"qwe
			"sd"
		);

	}	

}
