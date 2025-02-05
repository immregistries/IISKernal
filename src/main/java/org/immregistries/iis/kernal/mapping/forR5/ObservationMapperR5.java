package org.immregistries.iis.kernal.mapping.forR5;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR5Condition;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ObservationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.AbstractFhirRequester;
import org.immregistries.iis.kernal.model.ObservationMaster;
import org.immregistries.iis.kernal.model.ObservationReported;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import static org.immregistries.iis.kernal.mapping.MappingHelper.IMMUNIZATION;
import static org.immregistries.iis.kernal.mapping.MappingHelper.PATIENT;

@Service("ObservationMapperR5")
@Conditional(OnR5Condition.class)
public class ObservationMapperR5 implements ObservationMapper<Observation> {
	@Autowired
	private AbstractFhirRequester fhirRequests;

	public Observation fhirResource(ObservationMaster om) {
		Observation o = new Observation();
		o.setId(om.getObservationId());
		if (!om.getVaccinationReportedId().isBlank()) {
			o.addPartOf(new Reference().setReference(IMMUNIZATION + "/" + om.getVaccinationReportedId()));
		}
		if (om.getPatientReportedId() != null) {
			o.setSubject(new Reference().setReference(PATIENT + "/" + om.getPatientReportedId()));
//				o.setSubject(MappingHelper.getFhirReference(PATIENT,PATIENT_REPORTED,observationReported.getPatientReported().getPatientReportedExternalLink()));
		}
		o.setValue(new CodeableConcept(new Coding()
			.setCode(om.getValueCode())
			.setSystem(om.getValueTable())
			.setDisplay(om.getValueLabel())));
		o.setMethod(new CodeableConcept()).getMethod().addCoding()
			.setCode(om.getMethodCode())
			.setSystem(om.getMethodTable())
			.setDisplay(om.getMethodLabel());
		o.addIdentifier(MappingHelper.getFhirIdentifierR5(
			om.getIdentifierTable(), om.getIdentifierCode())); //TODO label
		o.addComponent().setValue(new DateTimeType(om.getObservationDate()))
			.setCode(new CodeableConcept().setText(OBSERVATION_DATE));
		o.addReferenceRange().setText(om.getUnitsLabel())
			.addAppliesTo().setText(om.getUnitsTable())
			.addCoding().setCode(om.getUnitsCode());
		o.addInterpretation().setText(RESULT_STATUS)
			.addCoding().setCode(om.getResultStatus());
		return o;

	}

	public ObservationReported localObjectReportedWithMaster(Observation observation) {
		ObservationReported observationReported = localObjectReported(observation);
		observationReported.setObservationMaster(
			fhirRequests.searchObservationMaster(
				new SearchParameterMap(Observation.SP_IDENTIFIER,new TokenParam().setSystem(observationReported.getIdentifierTable()).setValue(observationReported.getIdentifierCode())))
		);
//                    Observation.IDENTIFIER.exactly().systemAndIdentifier(observationReported.getIdentifierTable(),observationReported.getIdentifierCode())
//			));
		return observationReported;
	}


	public ObservationReported localObjectReported(Observation o) {
		ObservationReported observationReported = new ObservationReported();
		observationReported.setUpdatedDate(o.getMeta().getLastUpdated());
		observationReported.setObservationId(o.getId());
//		observationReported.setVaccinationReportedId(o.getPartOfFirstRep().getId());
		observationReported.setVaccinationReportedId(o.getPartOf().stream().filter(ref -> ref.getReference().startsWith("Immunization/")).findFirst().orElse(new Reference("")).getId());
		observationReported.setPatientReportedId(o.getSubject().getId());
		observationReported.setValueCode(o.getValueCodeableConcept().getCodingFirstRep().getCode());
		observationReported.setValueTable(o.getValueCodeableConcept().getCodingFirstRep().getSystem());
		observationReported.setValueLabel(o.getValueCodeableConcept().getCodingFirstRep().getDisplay());
		observationReported.setMethodCode(o.getMethod().getCodingFirstRep().getCode());
		observationReported.setMethodTable(o.getMethod().getCodingFirstRep().getSystem());
		observationReported.setMethodLabel(o.getMethod().getCodingFirstRep().getDisplay());

//		Identifier identifier = o.getIdentifierFirstRep();
//		observationReported.setIdentifierCode(identifier.getValue());
//		observationReported.setIdentifierTable(identifier.getSystem());
		Coding identifierCoding = o.getCode().getCoding().stream().filter(coding -> coding.getSystem().equals(IDENTIFIER_CODE)).findFirst().orElse(new Coding());
		observationReported.setIdentifierCode(identifierCoding.getCode());
		observationReported.setIdentifierTable(identifierCoding.getSystem());
		observationReported.setIdentifierLabel(identifierCoding.getDisplay());


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

	public ObservationMaster localObject(Observation o) {
		return localObjectReported(o);
	}


}
