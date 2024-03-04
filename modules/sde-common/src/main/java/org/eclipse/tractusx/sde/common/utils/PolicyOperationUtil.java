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
package org.eclipse.tractusx.sde.common.utils;

import static org.eclipse.tractusx.sde.common.enums.PolicyTypeIdEnum.ACCESS;
import static org.eclipse.tractusx.sde.common.enums.PolicyTypeIdEnum.USAGE;

import java.util.List;

import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;

public class PolicyOperationUtil {
	
	private PolicyOperationUtil() {}

	private static List<String> getBPNList(List<Policies> policies) {
		return policies
				.stream()
				.filter(e -> e.getTechnicalKey().equals("BusinessPartnerNumber"))
				.flatMap(e -> e.getValue().stream())
				.toList();
	}
	
	public static List<String> getAccessBPNList(PolicyModel policy) {
		return getBPNList(policy.getPolicies().get(ACCESS.getPolicyTypeValue()));
	}

	public static List<String> getUsageBPNList(PolicyModel policy) {
		return getBPNList(policy.getPolicies().get(USAGE.getPolicyTypeValue()));
	}
	
	public static List<Policies> getAccessPolicies(PolicyModel policy) {
		return policy.getPolicies().get(ACCESS.getPolicyTypeValue());
	}
	
	public static List<Policies> getUsagePolicies(PolicyModel policy) {
		return policy.getPolicies().get(USAGE.getPolicyTypeValue());
	}

}
