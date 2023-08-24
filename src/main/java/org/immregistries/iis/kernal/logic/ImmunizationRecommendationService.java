package org.immregistries.iis.kernal.logic;

import org.hl7.fhir.r5.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static org.immregistries.iis.kernal.mapping.Interfaces.ImmunizationMapper.CVX;

@Service
public class ImmunizationRecommendationService {
	private static String[] DATE_CRITERION_CODES = {"30981-5", "30980-7", "59777-3", "59778-1"};
	private static String[] DATE_CRITERION_DISPLAYS = {"Earliest date to give", "Date vaccine due", "Latest date to give immunization", "Date when overdue for immunization"};
	private static String IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-date-criterion";
	private static String IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-status";


	private ImmunizationRecommendation generate(OrgMaster orgMaster) {
		ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
		recommendation.addIdentifier(new Identifier().setValue(UUID.randomUUID().toString().split("-")[0]));
		recommendation.setDate(new Date());
		recommendation = addGeneratedRecommendation(recommendation);
		recommendation.setAuthority(new Reference()
			.setIdentifier(new Identifier().setSystem("IIS-Sandbox/facility").setValue(orgMaster.getOrganizationName()))); //TODO change set system, register as org
		return recommendation;
	}


	public ImmunizationRecommendation generate(OrgMaster orgMaster, PatientReported patientReported) {
		ImmunizationRecommendation recommendation = generate(orgMaster);
		recommendation.setPatient(new Reference()
			.setIdentifier(new Identifier()
				.setValue(patientReported.getExternalLink())
				.setSystem(patientReported.getPatientReportedAuthority())));

		return recommendation;
	}

	public ImmunizationRecommendation generate(OrgMaster orgMaster, Patient patient) {
		ImmunizationRecommendation recommendation = generate(orgMaster);
		recommendation.setPatient(new Reference()
				.setReference("Patient/" + new IdType(patient.getId()).getIdPart())
//			.setIdentifier(patient.getIdentifier().stream()
//				.filter(identifier -> identifier.getSystem().equals(MRN_SYSTEM))
//				.findFirst()
//				.orElse(patient.getIdentifierFirstRep()))
		);
		return recommendation;
	}

	public ImmunizationRecommendation addGeneratedRecommendation(ImmunizationRecommendation recommendation) {
		recommendation.setDate(new Date());

		ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent recommendationComponent = recommendation.addRecommendation()
			.setDescription("Random Sample generated by IIS Sandbox")
			.setForecastStatus(new CodeableConcept().addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM, "due", "Due")));

		Collection<Code> col =  CodeMapManager.getCodeMap().getCodesForTable(CodesetType.VACCINATION_CVX_CODE);
		Code cvx = col.stream().skip((int) (col.size() * Math.random())).findFirst().get();
		recommendationComponent.addVaccineCode().addCoding().setSystem(CVX).setCode(cvx.getValue()).setDisplay(cvx.getLabel());

		int randN = (int) (Math.random() * 4);
		int randDateN = (int) (1 + Math.random() * 15);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.DATE, randDateN);
		calendar.getTime();
		recommendationComponent.addDateCriterion()
			.setValue(calendar.getTime())
			.setCode(new CodeableConcept().addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM, DATE_CRITERION_CODES[randN], DATE_CRITERION_DISPLAYS[randN])));
		return recommendation;
	}

}
