package org.immregistries.iis.kernal.mapping.interfaces;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;

public interface ObservationMapper<Observation extends IBaseResource> extends IisFhirMapperMasterReported<ObservationMaster, ObservationReported, Observation> {
	String IDENTIFIER_CODE = "identifierCode";
	String OBSERVATION_DATE = "observationDate";
	String RESULT_STATUS = "resultStatus";
	String SUBIDENTIFIER_EXTENSION = "http://hl7.org/fhir/uv/v2mappings/ConceptMap/datatype-og-subidentifier-to-extension";

	ObservationReported localObjectReported(Observation i);

	ObservationMaster localObject(Observation i);

	Observation fhirResource(ObservationMaster observationMaster);

	ObservationReported localObjectReportedWithMaster(Observation observation);
}
