/********************************************************************************
 * Copyright (c) 2023, 2024 T-Systems International GmbH
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.sde.submodels.pcf.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class Pcf {
	
	private List<ProductOrSectorSpecificRules> productOrSectorSpecificRules;
	private List<SecondaryEmissionFactorSources> secondaryEmissionFactorSources;
	
	private DataQualityRating dataQualityRating;
	
	private List<CrossSectoralStandardsUsed> crossSectoralStandardsUsed;
	
	private Double biogenicCarbonEmissionsOtherThanCO2; // 1.0,
	private Double distributionStagePcfExcludingBiogenic; // 1.5,
	private Double biogenicCarbonWithdrawal; // 0.0,
	private Double distributionStageBiogenicCarbonEmissionsOtherThanCO2; // 1.0,
	
	@SerializedName("extWBCSD_allocationRulesDescription")
	private String extWBCSDAllocationRulesDescription;//In accordance with Catena-X PCF Rulebook
	
	private String exemptedEmissionsDescription;//No exemption
	private Double distributionStageFossilGhgEmissions; // 0.5,
	private Double exemptedEmissionsPercent; // 0.0,
	private String geographyCountrySubdivision;//US-NY
	
	@SerializedName("extTFS_luGhgEmissions")
	private Double extTFSLuGhgEmissions; // 0.3,
	private Double distributionStageBiogenicCarbonWithdrawal; // 0.5,
	private Double pcfIncludingBiogenic; // 1.0,
	private Double aircraftGhgEmissions; // 0.0,
	private Double productMassPerDeclaredUnit; // 0.456,
	
	@SerializedName("extWBCSD_operator")
	private String extWBCSDOperator;//PEF
	
	private String ruleName;//urn:tfs-initiative.com:PCR:The Product Carbon Footprint Guideline for the Chemical Industry:version:v2.0
	
	@SerializedName("extWBCSD_otherOperatorName")
	private String extWBCSDOtherOperatorName;//NSF
	
	@SerializedName("extTFS_allocationWasteIncineration")
	private String extTFSAllocationWasteIncineration;//cut-off
	private Double pcfExcludingBiogenic; // 2.0,
	private String referencePeriodEnd;//2022-12-31T23:59:59Z
	
	@SerializedName("extWBCSD_characterizationFactors")
	private String extWBCSDCharacterizationFactors;//AR5
	private String secondaryEmissionFactorSource;//ecoinvent 3.8
	private Double unitaryProductAmount; // 1000.0,
	private String declaredUnit;//liter
	private String referencePeriodStart;//2022-01-01T00:00:01Z
	private String geographyRegionOrSubregion;//Africa
	private Double fossilGhgEmissions; // 0.5,
	private String boundaryProcessesDescription;//Electricity consumption included as an input in the production phase
	private String geographyCountry;//DE
	
	@SerializedName("extWBCSD_packagingGhgEmissions")
	private Double extWBCSDPackagingGhgEmissions; // 0,
	private Double dlucGhgEmissions; // 0.4,
	private Double carbonContentTotal; // 2.5,
	
	@SerializedName("extTFS_distributionStageLuGhgEmissions")
	private Double extTFSDistributionStageLuGhgEmissions; // 1.1,
	private Double primaryDataShare; // 56.12,
	
	@SerializedName("extWBCSD_packagingEmissionsIncluded")
	private boolean extWBCSDPackagingEmissionsIncluded;//true
	
	@SerializedName("extWBCSD_fossilCarbonContent")
	private Double extWBCSDFossilCarbonContent; // 0.1,
	
	private String crossSectoralStandard;//GHG Protocol Product standard
	
	@SerializedName("extTFS_distributionStageDlucGhgEmissions")
	private Double extTFSDistributionStageDlucGhgEmissions; // 1.0,
	private Double distributionStagePcfIncludingBiogenic; // 0.0,
	private Double carbonContentBiogenic; // 0.0,

}