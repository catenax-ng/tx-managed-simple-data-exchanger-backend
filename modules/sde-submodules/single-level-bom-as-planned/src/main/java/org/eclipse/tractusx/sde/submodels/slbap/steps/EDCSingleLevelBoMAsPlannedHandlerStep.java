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
package org.eclipse.tractusx.sde.submodels.slbap.steps;

import java.util.Map;

import org.eclipse.tractusx.sde.common.constants.CommonConstants;
import org.eclipse.tractusx.sde.common.exception.CsvHandlerUseCaseException;
import org.eclipse.tractusx.sde.common.exception.ServiceException;
import org.eclipse.tractusx.sde.common.submodel.executor.Step;
import org.eclipse.tractusx.sde.common.submodel.executor.create.steps.impl.EDCHandlerStep;
import org.eclipse.tractusx.sde.submodels.slbap.entity.SingleLevelBoMAsPlannedEntity;
import org.eclipse.tractusx.sde.submodels.slbap.model.SingleLevelBoMAsPlanned;
import org.eclipse.tractusx.sde.submodels.slbap.services.SingleLevelBoMAsPlannedService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class EDCSingleLevelBoMAsPlannedHandlerStep extends Step {

	private final EDCHandlerStep edcHandlerStep;
	private final SingleLevelBoMAsPlannedService singleLevelBoMAsPlannedService;

	@SneakyThrows
	public SingleLevelBoMAsPlanned run(String submodel, SingleLevelBoMAsPlanned input, String processId) {

		String shellId = input.getShellId();
		String subModelId = input.getSubModelId();

		try {
			String assetId = shellId + "-" + subModelId;
			JsonNode asset = edcHandlerStep.getAsset(assetId);
			if (asset == null) {
				edcProcessing(submodel, shellId, subModelId, input);
			} else {
				deleteEDCFirstForUpdate(submodel, input, processId);
				edcProcessing(submodel, shellId, subModelId, input);
				input.setUpdated(CommonConstants.UPDATED_Y);
			}

			return input;
		} catch (Exception e) {
			throw new CsvHandlerUseCaseException(input.getRowNumber(), "EDC: " + e.getMessage());
		}
	}

	@SneakyThrows
	private void deleteEDCFirstForUpdate(String submodel, SingleLevelBoMAsPlanned input, String processId) {
		try {
			SingleLevelBoMAsPlannedEntity singleLevelBoMAsPlannedEntity = singleLevelBoMAsPlannedService
					.readEntity(input.getChildUuid());
			singleLevelBoMAsPlannedService.deleteEDCAsset(singleLevelBoMAsPlannedEntity);

		} catch (Exception e) {
			if (!e.getMessage().contains("404 Not Found")) {
				throw new ServiceException("Unable to delete EDC offer for update: " + e.getMessage());
			}
		}
	}

	@SneakyThrows
	private void edcProcessing(String submodel, String shellId, String subModelId, SingleLevelBoMAsPlanned input) {

		Map<String, String> createEDCOffer = edcHandlerStep.createEDCOffer(submodel, shellId, subModelId,
				input.getParentUuid(), input.getBpnNumbers(), input.getUsagePolicies());

		// EDC transaction information for DB
		input.setAssetId(createEDCOffer.get("assetId"));
		input.setAccessPolicyId(createEDCOffer.get("accessPolicyId"));
		input.setUsagePolicyId(createEDCOffer.get("usagePolicyId"));
		input.setContractDefinationId(createEDCOffer.get("contractDefinitionId"));
	}
}