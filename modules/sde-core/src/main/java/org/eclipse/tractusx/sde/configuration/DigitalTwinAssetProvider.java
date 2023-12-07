/********************************************************************************
 * Copyright (c) 2023 T-Systems International GmbH
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.eclipse.tractusx.sde.common.utils.UUIdGenerator;
import org.eclipse.tractusx.sde.core.properties.SdeCommonProperties;
import org.eclipse.tractusx.sde.core.utils.ValueReplacerUtility;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.CreateEDCAssetFacilator;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("default")
public class DigitalTwinAssetProvider {

	private final AssetEntryRequestFactory assetFactory;
	private final EDCGateway edcGateway;
	private final CreateEDCAssetFacilator createEDCAssetFacilator;
	private final SdeCommonProperties sdeCommonProperties;
	private final ValueReplacerUtility valueReplacerUtility;

	@PostConstruct
	@SneakyThrows
	public void init() {

		String assetId = UUIdGenerator.getUuid();
		AssetEntryRequest assetEntryRequest = assetFactory.getAssetRequest("", "Digital twin registry information",
				assetId, "1", "");

		assetEntryRequest.getAsset().getProperties().put("type", "data.core.digitalTwinRegistry");
		assetEntryRequest.getAsset().getProperties().put("registry", sdeCommonProperties.getDigitalTwinRegistry());

		assetEntryRequest.getDataAddress().getProperties().put("oauth2:tokenUrl",
				sdeCommonProperties.getDigitalTwinTokenUrl());
		assetEntryRequest.getDataAddress().getProperties().put("oauth2:clientId",
				sdeCommonProperties.getDigitalTwinClientId());

		if (sdeCommonProperties.isDDTRManagedThirdparty()) {
			assetEntryRequest.getDataAddress().getProperties().put("baseUrl",
					sdeCommonProperties.getDigitalTwinRegistry());
			assetEntryRequest.getDataAddress().getProperties().put("oauth2:scope",
					sdeCommonProperties.getDigitalTwinAuthenticationScope());
			assetEntryRequest.getDataAddress().getProperties().put("oauth2:clientSecretKey", "ddtr-client-secret");
		} else {
			assetEntryRequest.getDataAddress().getProperties().put("baseUrl",
					sdeCommonProperties.getDigitalTwinRegistry() + sdeCommonProperties.getDigitalTwinRegistryURI());
		}

		Map<String, String> inputData = new HashMap<>();
		inputData.put("manufacturerId", sdeCommonProperties.getManufacturerId());
		inputData.put("digitalTwinRegistry", sdeCommonProperties.getDigitalTwinRegistry());

		ObjectNode requestBody = (ObjectNode) new ObjectMapper().readTree(valueReplacerUtility
				.valueReplacerUsingFileTemplate("/edc_request_template/edc_asset_lookup.json", inputData));

		if (!edcGateway.assetExistsLookupBasedOnType(requestBody)) {
			Map<String, String> createEDCAsset = createEDCAssetFacilator.createEDCAsset(assetEntryRequest, List.of(),
					Map.of());
			log.info("Digital twin asset creates :" + createEDCAsset.toString());
		} else {
			log.info("Digital twin asset exists in edc connector, so ignoring asset creation");
		}
	}

	@SneakyThrows
	private String valueReplacer(String requestTemplatePath, Map<String, String> inputData) {
		StringSubstitutor stringSubstitutor1 = new StringSubstitutor(inputData);
		return stringSubstitutor1.replace(requestTemplatePath);
	}
}
