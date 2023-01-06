package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.immregistries.iis.kernal.mapping.Interfaces.RelatedPersonMapper;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Service
@Conditional(OnR4Condition.class)
public class RelatedPersonMapperR4 implements RelatedPersonMapper<RelatedPerson> {
	public PatientReported fillGuardianInformation(PatientReported patientReported, RelatedPerson relatedPerson){
		patientReported.setGuardianLast(relatedPerson.getNameFirstRep().getFamily());
		if (relatedPerson.getNameFirstRep().getGiven().size() > 0) {
			patientReported.setGuardianFirst(relatedPerson.getNameFirstRep().getGiven().get(0).getValueNotNull());
		}
		if (relatedPerson.getNameFirstRep().getGiven().size() > 1) {
			patientReported.setGuardianMiddle(relatedPerson.getNameFirstRep().getGiven().get(1).getValueNotNull());
		}
		patientReported.setGuardianRelationship(relatedPerson.getRelationshipFirstRep().getCodingFirstRep().getCode());
		return  patientReported;
	}

	public RelatedPerson getFhirRelatedPersonFromPatient(PatientReported pr){
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setPatient(new Reference("Patient/" + pr.getPatientReportedId()));
		relatedPerson.addRelationship().addCoding().setSystem("").setCode(pr.getGuardianRelationship());
		HumanName name = relatedPerson.addName();
		name.setFamily(pr.getGuardianLast());
		name.addGivenElement().setValue(pr.getGuardianFirst());
		name.addGivenElement().setValue(pr.getGuardianMiddle());
		return relatedPerson;
	}
}