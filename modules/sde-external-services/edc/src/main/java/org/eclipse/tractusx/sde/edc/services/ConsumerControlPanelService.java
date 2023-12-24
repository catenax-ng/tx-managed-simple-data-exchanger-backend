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

package org.eclipse.tractusx.sde.edc.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.common.entities.UsagePolicies;
import org.eclipse.tractusx.sde.common.enums.PolicyAccessEnum;
import org.eclipse.tractusx.sde.common.enums.UsagePolicyEnum;
import org.eclipse.tractusx.sde.common.exception.ServiceException;
import org.eclipse.tractusx.sde.edc.api.ContractOfferCatalogApi;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConstant;
import org.eclipse.tractusx.sde.edc.entities.database.ContractNegotiationInfoEntity;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.enums.Type;
import org.eclipse.tractusx.sde.edc.facilitator.AbstractEDCStepsHelper;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.ContractNegotiationDto;
import org.eclipse.tractusx.sde.edc.model.contractoffers.ContractOfferRequestFactory;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.util.UtilityFunctions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerControlPanelService extends AbstractEDCStepsHelper {

	private static final String NEGOTIATED = "NEGOTIATED";
	private static final String STATUS = "status";

	private final ContractOfferCatalogApi contractOfferCatalogApiProxy;
	private final ContractNegotiateManagementHelper contractNegotiateManagement;

	private final ContractNegotiationInfoRepository contractNegotiationInfoRepository;
	private final PolicyConstraintBuilderService policyConstraintBuilderService;

	private final ContractOfferRequestFactory contractOfferRequestFactory;

	private final EDRRequestHelper edrRequestHelper;

	private static final Integer RETRY = 5;

	private static final Integer THRED_SLEEP_TIME = 5000;

	public List<QueryDataOfferModel> queryOnDataOffers(String providerUrl, Integer offset, Integer limit,
			String filterExpression) {

		providerUrl = UtilityFunctions.removeLastSlashOfUrl(providerUrl);

		if (!providerUrl.endsWith(protocolPath))
			providerUrl = providerUrl + protocolPath;
		
		String sproviderUrl = providerUrl;

		List<QueryDataOfferModel> queryOfferResponse = new ArrayList<>();

		JsonNode contractOfferCatalog = contractOfferCatalogApiProxy
				.getContractOffersCatalog(contractOfferRequestFactory
						.getContractOfferRequest(sproviderUrl, limit, offset, filterExpression));

		JsonNode jOffer = contractOfferCatalog.get("dcat:dataset");
		if (jOffer.isArray()) {

			jOffer.forEach(
					offer -> queryOfferResponse.add(buildContractOffer(sproviderUrl, contractOfferCatalog, offer)));

		} else {
			queryOfferResponse.add(buildContractOffer(sproviderUrl, contractOfferCatalog, jOffer));
		}

		return queryOfferResponse;
	}

	private QueryDataOfferModel buildContractOffer(String sproviderUrl, JsonNode contractOfferCatalog, JsonNode offer) {

		JsonNode policy = offer.get("odrl:hasPolicy");

		String edcstr = "edc:";

		QueryDataOfferModel build = QueryDataOfferModel.builder()
				.assetId(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_ID))
				.connectorOfferUrl(sproviderUrl)
				.offerId(getFieldFromJsonNode(policy, "@id"))
				.title(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_NAME))
				.type(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_TYPE))
				.description(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_DESCRIPTION))
				.created(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_CREATED))
				.modified(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_MODIFIED))
				.publisher(getFieldFromJsonNode(contractOfferCatalog, edcstr + "participantId"))
				.version(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_VERSION))
				.fileName(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_FILENAME))
				.fileContentType(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_CONTENTTYPE))
				.connectorId(getFieldFromJsonNode(contractOfferCatalog, "edc:participantId")).build();

		checkAndSetPolicyPermission(build, policy);

		return build;
	}

	private void checkAndSetPolicyPermission(QueryDataOfferModel build, JsonNode policy) {

		if (policy != null && policy.isArray()) {
			policy.forEach(pol -> {
				JsonNode permission = pol.get("odrl:permission");
				checkAndSetPolicyPermissionConstraints(build, permission);
			});
		} else if (policy != null) {
			JsonNode permission = policy.get("odrl:permission");
			checkAndSetPolicyPermissionConstraints(build, permission);
		}
	}

	private void checkAndSetPolicyPermissionConstraints(QueryDataOfferModel build, JsonNode permission) {

		JsonNode constraints = permission.get("odrl:constraint");

		EnumMap<UsagePolicyEnum, UsagePolicies> usagePolicies = new EnumMap<>(UsagePolicyEnum.class);

		List<String> bpnNumbers = new ArrayList<>();

		if (constraints != null) {
			JsonNode jsonNode = constraints.get("odrl:and");

			if (jsonNode != null && jsonNode.isArray()) {
				jsonNode.forEach(constraint -> setConstraint(usagePolicies, bpnNumbers, constraint));
			} else if (jsonNode != null) {
				setConstraint(usagePolicies, bpnNumbers, jsonNode);
			}
		}
		build.setTypeOfAccess(!bpnNumbers.isEmpty() ? PolicyAccessEnum.RESTRICTED : PolicyAccessEnum.UNRESTRICTED);
		build.setBpnNumbers(bpnNumbers);
		build.setUsagePolicies(usagePolicies);
	}

	private void setConstraint(Map<UsagePolicyEnum, UsagePolicies> usagePolicies, List<String> bpnNumbers,
			JsonNode jsonNode) {

		String leftOperand = getFieldFromJsonNode(jsonNode, "odrl:leftOperand");
		String rightOperand = getFieldFromJsonNode(jsonNode, "odrl:rightOperand");

		if (leftOperand.equals("BusinessPartnerNumber")) {
			bpnNumbers.add(rightOperand);
		} else {
			Map<UsagePolicyEnum, UsagePolicies> policyResponse = UtilityFunctions.identyAndGetUsagePolicy(leftOperand,
					rightOperand);
			if (policyResponse != null)
				usagePolicies.putAll(policyResponse);
		}
	}

	private String getFieldFromJsonNode(JsonNode jnode, String fieldName) {
		if (jnode.get(fieldName) != null)
			return jnode.get(fieldName).asText();
		else
			return "";
	}

	@Async
	public void subscribeDataOffers(ConsumerRequest consumerRequest, String processId) {

		HashMap<String, String> extensibleProperty = new HashMap<>();
		AtomicReference<String> negotiateContractId = new AtomicReference<>();
		AtomicReference<ContractNegotiationDto> checkContractNegotiationStatus = new AtomicReference<>();

		var recipientURL = UtilityFunctions.removeLastSlashOfUrl(consumerRequest.getProviderUrl());
		
		if (!recipientURL.endsWith(protocolPath))
			recipientURL = recipientURL + protocolPath;
		
		String sproviderUrl = recipientURL;

		Map<UsagePolicyEnum, UsagePolicies> policies = consumerRequest.getPolicies();

		UsagePolicies findFirst = policies.get(UsagePolicyEnum.CUSTOM);

		if (findFirst != null) {
			extensibleProperty.put(UsagePolicyEnum.CUSTOM.name(), findFirst.getValue());
		}

		ActionRequest action = policyConstraintBuilderService.getUsagePolicyConstraints(policies);
		consumerRequest.getOffers().parallelStream().forEach(offer -> {
			try {

				negotiateContractId.set(
						contractNegotiateManagement.negotiateContract(sproviderUrl, consumerRequest.getConnectorId(),
								offer.getOfferId(), offer.getAssetId(), action, extensibleProperty));
				int retry = 3;
				int counter = 1;

				do {
					Thread.sleep(3000);
					checkContractNegotiationStatus
							.set(contractNegotiateManagement.checkContractNegotiationStatus(negotiateContractId.get()));
					counter++;
				} while (checkContractNegotiationStatus.get() != null
						&& !checkContractNegotiationStatus.get().getState().equals("FINALIZED")
						&& !checkContractNegotiationStatus.get().getState().equals("TERMINATED") && counter <= retry);

			} catch (InterruptedException ie) {
				log.error("Exception in subscribeDataOffers" + ie.getMessage());
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.error("Exception in subscribeDataOffers" + e.getMessage());
			} finally {
				ContractNegotiationInfoEntity contractNegotiationInfoEntity = ContractNegotiationInfoEntity.builder()
						.id(UUID.randomUUID().toString()).processId(processId)
						.connectorId(consumerRequest.getConnectorId()).offerId(offer.getOfferId())
						.contractNegotiationId(negotiateContractId != null ? negotiateContractId.get() : null)
						.status(checkContractNegotiationStatus.get() != null
								? checkContractNegotiationStatus.get().getState()
								: "Failed:Exception")
						.dateTime(LocalDateTime.now()).build();

				contractNegotiationInfoRepository.save(contractNegotiationInfoEntity);
			}
		});

	}

	public Map<String, Object> subscribeAndDownloadDataOffers(ConsumerRequest consumerRequest,
			boolean flagToDownloadImidiate) {
		HashMap<String, String> extensibleProperty = new HashMap<>();
		Map<String, Object> response = new ConcurrentHashMap<>();

		var recipientURL = UtilityFunctions.removeLastSlashOfUrl(consumerRequest.getProviderUrl());
		
		if (!recipientURL.endsWith(protocolPath))
			recipientURL = recipientURL + protocolPath;
		
		String sproviderUrl = recipientURL;

		Map<UsagePolicyEnum, UsagePolicies> policies = consumerRequest.getPolicies();

		UsagePolicies findFirst = policies.get(UsagePolicyEnum.CUSTOM);

		if (findFirst != null) {
			extensibleProperty.put(UsagePolicyEnum.CUSTOM.name(), findFirst.getValue());
		}

		ActionRequest action = policyConstraintBuilderService.getUsagePolicyConstraints(policies);
		consumerRequest.getOffers().parallelStream().forEach(offer -> {
			Map<String, Object> resultFields = new ConcurrentHashMap<>();
			try {
				EDRCachedResponse checkContractNegotiationStatus = verifyOrCreateContractNegotiation(
						consumerRequest.getConnectorId(), extensibleProperty, sproviderUrl, action, offer);

				resultFields.put("edr", checkContractNegotiationStatus);

				doVerifyResult(offer.getAssetId(), checkContractNegotiationStatus);

				if (flagToDownloadImidiate)
					resultFields.put("data",
							downloadFile(checkContractNegotiationStatus, consumerRequest.getDownloadDataAs()));

				resultFields.put(STATUS, "SUCCESS");

			} catch (FeignException e) {
				log.error("Feign RequestBody: " + e.request());
				String errorMsg = "Unable to complete subscribeAndDownloadDataOffers because: " + e.contentUTF8();
				log.error(errorMsg);
				prepareErrorMap(resultFields, errorMsg);
			} catch (Exception e) {
				log.error("SubscribeAndDownloadDataOffers Oops! We have -" + e.getMessage());
				String errorMsg = "Unable to complete subscribeAndDownloadDataOffers because: " + e.getMessage();
				prepareErrorMap(resultFields, errorMsg);
			} finally {
				response.put(offer.getAssetId(), resultFields);
			}
		});
		return response;
	}

	@SneakyThrows
	private void doVerifyResult(String assetId, EDRCachedResponse checkContractNegotiationStatus)
			throws ServiceException {

		if (checkContractNegotiationStatus != null
				&& StringUtils.isBlank(checkContractNegotiationStatus.getTransferProcessId())
				&& StringUtils.isNoneBlank(checkContractNegotiationStatus.getAgreementId())) {
			throw new ServiceException("There is valid contract agreement exist for " + assetId
					+ " but intiate data transfer is not completed and no EDR token available, download is not possible");
		}

		String state = Optional.ofNullable(checkContractNegotiationStatus).filter(
				verifyEDRRequestStatusLocal -> NEGOTIATED.equalsIgnoreCase(verifyEDRRequestStatusLocal.getEdrState()))
				.map(EDRCachedResponse::getEdrState)
				.orElseThrow(() -> new ServiceException(
						"Time out!! to get 'NEGOTIATED' EDC EDR status to download data, the current status is '"
								+ checkContractNegotiationStatus.getEdrState() + "'"));
		log.info("The EDR token status :" + state);
	}

	@SneakyThrows
	public EDRCachedResponse verifyOrCreateContractNegotiation(String connectorId,
			Map<String, String> extensibleProperty, String recipientURL, ActionRequest action, Offer offer) {
		// Verify if there already EDR process initiated then skip it for again download
		String assetId = offer.getAssetId();
		List<EDRCachedResponse> eDRCachedResponseList = edrRequestHelper.getEDRCachedByAsset(assetId);
		EDRCachedResponse checkContractNegotiationStatus = verifyEDRResponse(eDRCachedResponseList);

		if (checkContractNegotiationStatus == null) {
			String contractAgreementId = checkandGetContractAgreementId(assetId);
			if (StringUtils.isBlank(contractAgreementId)) {
				log.info("The EDR process was not completed, no 'NEGOTIATED' EDR status found "
						+ "and not valid contract agreementId for " + assetId + ", so initiating EDR process");
				edrRequestHelper.edrRequestInitiate(recipientURL, connectorId, offer.getOfferId(), assetId, action,
						extensibleProperty);
				checkContractNegotiationStatus = verifyEDRRequestStatus(assetId);
			} else {
				log.info("There is valid contract agreement exist for " + assetId
						+ ", so ignoring EDR process initiation");
				checkContractNegotiationStatus = EDRCachedResponse.builder().agreementId(contractAgreementId)
						.assetId(assetId).build();
			}
		} else {
			log.info("There was EDR process initiated " + assetId
					+ ", so ignoring EDR process initiation, going to check EDR status only");
			if (!NEGOTIATED.equals(checkContractNegotiationStatus.getEdrState()))
				checkContractNegotiationStatus = verifyEDRRequestStatus(assetId);
		}

		return checkContractNegotiationStatus;
	}

	@SneakyThrows
	private String checkandGetContractAgreementId(String assetId) {
		List<JsonNode> contractAgreements = contractNegotiateManagement.getAllContractAgreements(assetId,
				Type.CONSUMER.name(), 0, 10);
		String contractAgreementId = null;
		if (!contractAgreements.isEmpty())
			for (JsonNode jsonNode : contractAgreements) {
				ContractNegotiationDto checkContractAgreementNegotiationStatus = contractNegotiateManagement
						.checkContractAgreementNegotiationStatus(getFieldFromJsonNode(jsonNode, "@id"));
				if ("FINALIZED".equals(checkContractAgreementNegotiationStatus.getState())) {
					contractAgreementId = checkContractAgreementNegotiationStatus.getContractAgreementId();
					break;
				}
			}

		return contractAgreementId;
	}

	@SneakyThrows
	public EDRCachedResponse verifyEDRRequestStatus(String assetId) {
		EDRCachedResponse eDRCachedResponse = null;
		String edrStatus = "NewToSDE";
		List<EDRCachedResponse> eDRCachedResponseList = null;
		int counter = 1;
		try {
			do {
				if (counter > 1)
					Thread.sleep(THRED_SLEEP_TIME);
				eDRCachedResponseList = edrRequestHelper.getEDRCachedByAsset(assetId);
				eDRCachedResponse = verifyEDRResponse(eDRCachedResponseList);

				if (eDRCachedResponse != null && eDRCachedResponse.getEdrState() != null)
					edrStatus = eDRCachedResponse.getEdrState();

				log.info("Verifying 'NEGOTIATED' EDC EDR status to download data for '" + assetId
						+ "', The current status is '" + edrStatus + "', Attempt " + counter);
				counter++;
			} while (counter <= RETRY && !NEGOTIATED.equals(edrStatus));

			if (eDRCachedResponse == null) {
				String contractAgreementId = checkandGetContractAgreementId(assetId);
				if (StringUtils.isNoneBlank(contractAgreementId)) {
					eDRCachedResponse = EDRCachedResponse.builder().agreementId(contractAgreementId).assetId(assetId)
							.build();
				} else
					throw new ServiceException("Time out!! unable to get Contract negotiation FINALIZED status");
			}

		} catch (FeignException e) {
			log.error("RequestBody: " + e.request());
			String errorMsg = "FeignExceptionton for asset " + assetId + "," + e.contentUTF8();
			log.error("Response: " + errorMsg);
			throw new ServiceException(errorMsg);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			String errorMsg = "InterruptedException for asset " + assetId + "," + ie.getMessage();
			log.error(errorMsg);
			throw new ServiceException(errorMsg);
		} catch (Exception e) {
			String errorMsg = "Exception for asset " + assetId + "," + e.getMessage();
			log.error(errorMsg);
			throw new ServiceException(errorMsg);
		}
		return eDRCachedResponse;
	}

	private EDRCachedResponse verifyEDRResponse(List<EDRCachedResponse> eDRCachedResponseList) {
		EDRCachedResponse eDRCachedResponse = null;
		if (eDRCachedResponseList != null && !eDRCachedResponseList.isEmpty()) {
			for (EDRCachedResponse edrCachedResponseObj : eDRCachedResponseList) {
				String edrState = edrCachedResponseObj.getEdrState();
				// For EDC connector 5.0 edrState field not supported so checking token
				// validation by calling direct API
				if (NEGOTIATED.equalsIgnoreCase(edrState) || isEDRTokenValid(edrCachedResponseObj)) {
					eDRCachedResponse = edrCachedResponseObj;
					eDRCachedResponse.setEdrState(NEGOTIATED);
					break;
				}
				eDRCachedResponse = edrCachedResponseObj;
			}
		}
		return eDRCachedResponse;
	}

	@SneakyThrows
	private boolean isEDRTokenValid(EDRCachedResponse edrCachedResponseObj) {
		String assetId = edrCachedResponseObj.getAssetId();
		try {
			EDRCachedByIdResponse authorizationToken = getAuthorizationTokenForDataDownload(
					edrCachedResponseObj.getTransferProcessId());
			edrRequestHelper.getDataFromProvider(authorizationToken, authorizationToken.getEndpoint());
		} catch (FeignException e) {
			log.error("FeignException RequestBody: " + e.request());
			String errorMsg = "FeignExceptionton for verifyEDR token " + assetId + "," + e.status() + "::"
					+ e.contentUTF8();
			log.error("FeignException Response: " + errorMsg);

			if (e.status() == 403) {
				log.error("Got 403 as token status so going to try new EDR token: " + errorMsg);
				return false;
			}
		} catch (Exception e) {
			String errorMsg = "Exception for asset in isEDRTokenValid " + assetId + "," + e.getMessage();
			log.error(errorMsg);
		}
		return true;
	}

	@SneakyThrows
	public EDRCachedByIdResponse getAuthorizationTokenForDataDownload(String transferProcessId) {
		return edrRequestHelper.getEDRCachedByTransferProcessId(transferProcessId);
	}

	@SneakyThrows
	public Map<String, Object> downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List<String> assetIdList,
			String type) {
		Map<String, Object> response = new ConcurrentHashMap<>();
		assetIdList.parallelStream().forEach(assetId -> {

			Map<String, Object> downloadResultFields = new ConcurrentHashMap<>();
			try {
				EDRCachedResponse verifyEDRRequestStatus = verifyEDRRequestStatus(assetId);

				downloadResultFields.put("edr", verifyEDRRequestStatus);

				doVerifyResult(assetId, verifyEDRRequestStatus);

				downloadResultFields.put("data", downloadFile(verifyEDRRequestStatus, type));

				downloadResultFields.put(STATUS, "SUCCESS");
			} catch (Exception e) {
				String errorMsg = e.getMessage();
				log.error("We have exception: " + errorMsg);
				prepareErrorMap(downloadResultFields, errorMsg);
			} finally {
				response.put(assetId, downloadResultFields);
			}
		});
		return response;
	}

	private void prepareErrorMap(Map<String, Object> resultFields, String errorMsg) {
		resultFields.put(STATUS, "FAILED");
		resultFields.put("error", errorMsg);
	}

	@SneakyThrows
	private Object downloadFile(EDRCachedResponse verifyEDRRequestStatus, String downloadDataAs) {
		if (verifyEDRRequestStatus != null && NEGOTIATED.equalsIgnoreCase(verifyEDRRequestStatus.getEdrState())) {
			try {
				EDRCachedByIdResponse authorizationToken = getAuthorizationTokenForDataDownload(
						verifyEDRRequestStatus.getTransferProcessId());
				String endpoint = authorizationToken.getEndpoint() + "?type=" + downloadDataAs;
				return edrRequestHelper.getDataFromProvider(authorizationToken, endpoint);
			} catch (FeignException e) {
				log.error("FeignException Download RequestBody: " + e.request());
				String errorMsg = "Unable to download subcribe data offer because: " + e.contentUTF8();
				throw new ServiceException(errorMsg);
			} catch (Exception e) {
				log.error("Exception DownloadFileFromEDCUsingifAlreadyTransferStatusCompleted Oops! We have -"
						+ e.getMessage());
				String errorMsg = "Unable to download subcribe data offer because: " + e.getMessage();
				throw new ServiceException(errorMsg);
			}
		}
		return null;
	}
}
