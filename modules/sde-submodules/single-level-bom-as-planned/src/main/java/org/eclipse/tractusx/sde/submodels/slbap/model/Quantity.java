package org.eclipse.tractusx.sde.submodels.slbap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Quantity {
	
	private Double quantityNumber;
	private MeasurementUnit measurementUnit;

}
