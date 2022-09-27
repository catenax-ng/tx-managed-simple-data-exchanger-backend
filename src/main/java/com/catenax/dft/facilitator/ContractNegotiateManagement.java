/********************************************************************************
 * Copyright (c) 2022 T-Systems International GmbH
 * Copyright (c) 2022 Contributors to the CatenaX (ng) GitHub Organisation
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

package com.catenax.dft.facilitator;

import com.catenax.dft.api.ContractApi;
import com.catenax.dft.entities.edc.request.policies.ConstraintRequest;
import com.catenax.dft.mapper.ContractMapper;
import com.catenax.dft.model.contractnegotiation.AcknowledgementId;
import com.catenax.dft.model.contractnegotiation.ContractNegotiations;
import com.catenax.dft.model.contractnegotiation.ContractNegotiationsResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractNegotiateManagement extends AbstractEDCStepsHelper {

    private final ContractApi contractApi;
    private final ContractMapper contractMapper;

    @SneakyThrows
    public String negotiateContract(String offerId, String provider, String assetId, List<ConstraintRequest> constraintRequests, HashMap<String, String> extensibleProperty) {

        ContractNegotiations contractNegotiations = contractMapper.prepareContractNegotiations(offerId,
                assetId, provider, constraintRequests);
        contractNegotiations.getOffer().getPolicy().setExtensibleProperties(extensibleProperty);

        AcknowledgementId acknowledgementId = contractApi.contractnegotiations(contractNegotiations,
                getAuthHeader());
        return acknowledgementId.getId();
    }

    @SneakyThrows
    public ContractNegotiationsResponse checkContractNegotiationStatus(String negotiateContractId) {

        return contractApi.checkContractNegotiationsStatus(negotiateContractId, getAuthHeader());

    }
}