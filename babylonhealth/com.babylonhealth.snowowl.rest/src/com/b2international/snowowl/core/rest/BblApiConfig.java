package com.b2international.snowowl.core.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.spring.web.plugins.Docket;


@Configuration
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
			"qwe",
			"sd"
		);

	}	

}
