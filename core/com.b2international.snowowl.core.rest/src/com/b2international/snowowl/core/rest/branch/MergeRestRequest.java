/*
 * Copyright 2011-2020 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.core.rest.branch;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiParam;

/**
 * @since 4.1
 */
public class MergeRestRequest {

	@ApiParam(required = true)
	@JsonProperty
	@NotEmpty
	private String source;

	@ApiParam(required = true)
	@JsonProperty
	@NotEmpty
	private String target;

	@ApiParam(required = false)
	@JsonProperty
	private String commitComment;
	
	@ApiParam(required = false)
	@JsonProperty
	private String reviewId;
	
	@ApiParam(required = false)
	@JsonProperty
	private boolean squash = true;

	public String getCommitComment() {
		return commitComment;
	}
	
	public String getReviewId() {
		return reviewId;
	}
	
	public String getSource() {
		return source;
	}
	
	public String getTarget() {
		return target;
	}
	
	public boolean isSquash() {
		return squash;
	}
	
}
