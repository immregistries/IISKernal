package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.Interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.immregistries.iis.kernal.repository.FhirRequester;
import org.immregistries.iis.kernal.repository.FhirRequesterR5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.MappingHelper.*;

@Service("ObservationMapperR5")
@Conditional(OnR5Condition.class)
public class ObservationMapperR5 implements ObservationMapper<Observation> {
	@Autowired
	FhirRequester fhirRequests;
	public static final String IDENTIFIER_CODE = "identifierCode";
	public static final String OBSERVATION_DATE = "observationDate";
	public static final String RESULT_STATUS = "resultStatus";

	public Observation getFhirResource(ObservationReported observationReported)  {
		Observation o = new Observation();
		o.addIdentifier(MappingHelper.getFhirIdentifier(OBSERVATION_REPORTED,observationReported.getObservationReportedId()));
		if (!observationReported.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new Reference().setReference(IMMUNIZATION + "/" + observationReported.getVaccinationReportedId()));
		}
		if(observationReported.getPatientReportedId() != null) {
			o.setSubject(new Reference().setReference(PATIENT + "/"+observationReported.getPatientReportedId()));
//				o.setSubject(MappingHelper.getFhirReference(PATIENT,PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
		}
		o.setValue(new CodeableConcept(new Coding()
			.setCode(observationReported.getValueCode())
			.setSystem(observationReported.getValueTable())
			.setDisplay(observationReported.getValueLabel())));
		o.setMethod(new CodeableConcept()).getMethod().addCoding()
			.setCode(observationReported.getMethodCode())
			.setSystem(observationReported.getMethodTable())
			.setDisplay(observationReported.getMethodLabel());
		o.addIdentifier(MappingHelper.getFhirIdentifier(
			observationReported.getIdentifierTable(),observationReported.getIdentifierCode())); //TODO label
		o.addComponent().setValue(new DateTimeType(observationReported.getObservationDate()))
			.setCode(new CodeableConcept().setText(OBSERVATION_DATE));
		o.addReferenceRange().setText(observationReported.getUnitsLabel())
			.addAppliesTo().setText(observationReported.getUnitsTable())
			.addCoding().setCode(observationReported.getUnitsCode());
		o.addInterpretation().setText(RESULT_STATUS)
			.addCoding().setCode(observationReported.getResultStatus());
		return o;

	}

	public ObservationReported getReportedWithMaster(Observation observation, FhirRequester fhirRequests, IGenericClient fhirClient){
		ObservationReported observationReported = getReported(observation);
		observationReported.setObservation(
			fhirRequests.searchObservationMaster(
                    Observation.IDENTIFIER.exactly().systemAndIdentifier(observationReported.getIdentifierTable(),observationReported.getIdentifierCode())
			));
		return observationReported;
	}


	public ObservationReported getReported(Observation o){
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		observationReported.setObservationReportedId(o.getCode().getCode(OBSERVATION_REPORTED));
//		observationReported.setVaccinationReported();
//		observationReported.setVaccinationReported();
//		observationReported.setPatientReported();
		observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());
		for (Identifier identifier: o.getIdentifier()) {
			switch (identifier.getSystem()) {
				case OBSERVATION: {
					break;
				}
				case OBSERVATION_MASTER: {
					break;
				}
				case OBSERVATION_REPORTED: {
					break;
				} default: {
					observationReported.setIdentifierCode(identifier.getValue());
					observationReported.setIdentifierTable(identifier.getSystem());
				}
			}
		}
		for (Observation.ObservationComponentComponent component: o.getComponent()) {
			if (component.getCode().getText().equals(OBSERVATION_DATE)) {
				observationReported.setObservationDate(component.getValueDateTimeType().getValue());
			}
		}
		observationReported.setUnitsCode(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getCodingFirstRep().getCode());
		observationReported.setUnitsTable(o.getReferenceRangeFirstRep().getAppliesToFirstRep().getText());
		observationReported.setUnitsLabel(o.getReferenceRangeFirstRep().getText());
		observationReported.setResultStatus(o.getInterpretationFirstRep().getCodingFirstRep().getCode());

		return  observationReported;
	}

	public ObservationMaster getMaster(Observation o){
		ObservationMaster observationMaster = new ObservationMaster();
		observationMaster.setObservationId(o.getCode().getCode(OBSERVATION_MASTER));
		observationMaster.setIdentifierCode(o.getCode().getCode(IDENTIFIER_CODE));
//		observationMaster.setPatient(fhirRequests.searchPatientMaster(Patient.IDENTIFIER.exactly().code()));
//		observationMaster.setVaccination();
//		observationMaster.setObservationReported();
		observationMaster.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		return  observationMaster;
	}


}
