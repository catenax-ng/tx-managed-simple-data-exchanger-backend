/********************************************************************************
 * Copyright (c) 2022, 2023 T-Systems International GmbH
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.portal.utils;

import java.net.URI;

import org.eclipse.tractusx.sde.portal.api.IPortalExternalServiceApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Component
@RequiredArgsConstructor
public class TokenUtility {

	@Value(value = "${digital-twins.authentication.url}")
	private URI appTokenURI;
	
	@Value(value = "${digital-twins.authentication.clientSecret}")
	private String appClientSecret;

	@Value(value = "${digital-twins.authentication.clientId}")
	private String appClientId;


	private final IPortalExternalServiceApi portalExternalServiceApi;

	@SneakyThrows
	public String getValidJWTTokenforAppTechUser() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "client_credentials");
		body.add("client_id", appClientId);
		body.add("client_secret", appClientSecret);
		var resultBody = portalExternalServiceApi.readAuthToken(appTokenURI, body);

		if (resultBody != null) {
			return resultBody.getAccessToken();
		}
		return null;
	}

	public String getOriginalRequestAuthToken() {
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
				.getHeader("Authorization");
	}

}