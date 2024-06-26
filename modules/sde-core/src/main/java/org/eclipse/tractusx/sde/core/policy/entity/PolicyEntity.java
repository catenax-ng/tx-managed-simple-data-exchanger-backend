/********************************************************************************
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.core.policy.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.tractusx.sde.common.entities.Policies;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Table(name = "policy_tbl")
@Entity
@Data
public class PolicyEntity {

	@Id
	@Column(name = "uuid")
	private String uuid;

	@Column(name = "policy_name")
	private String policyName;
	
	@Convert(converter = PoliciesListToStringConverter.class)
	@Column(name = "access_policies", columnDefinition = "TEXT")
	@JsonProperty(value = "access_policies")
	private List<Policies> accessPolicies;
	
	@Convert(converter = PoliciesListToStringConverter.class)
	@Column(name = "usage_policies", columnDefinition = "TEXT")
	@JsonProperty(value = "usage_policies")
	private List<Policies> usagePolicies;
	
	@Column(name = "last_updated_time")
	private LocalDateTime lastUpdatedTime;
	
}
