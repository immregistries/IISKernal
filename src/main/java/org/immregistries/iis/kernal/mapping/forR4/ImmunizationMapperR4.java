package org.immregistries.iis.kernal.mapping.forR4;

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.immregistries.codebase.client.generated.Code;
import org.immregistries.codebase.client.reference.CodesetType;
import org.immregistries.iis.kernal.fhir.common.annotations.OnR4Condition;
import org.immregistries.iis.kernal.logic.CodeMapManager;
import org.immregistries.iis.kernal.mapping.MappingHelper;
import org.immregistries.iis.kernal.mapping.interfaces.ImmunizationMapper;
import org.immregistries.iis.kernal.mapping.internalClient.FhirRequesterR4;
import org.immregistries.iis.kernal.model.BusinessIdentifier;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service("ImmunizationMapperR4")
@Conditional(OnR4Condition.class)
public class ImmunizationMapperR4 implements ImmunizationMapper<Immunization> {
	@Autowired
	private FhirRequesterR4 fhirRequests;

	public VaccinationReported localObjectReportedWithMaster(Immunization i) {
		VaccinationReported vaccinationReported = this.localObjectReported(i);
		VaccinationMaster vaccinationMaster = fhirRequests.searchVaccinationMaster(
			new SearchParameterMap(Immunization.SP_IDENTIFIER, new TokenParam().setValue(vaccinationReported.getFillerBusinessIdentifier().getValue()))
//			Immunization.IDENTIFIER.exactly().systemAndIdentifier(
//				vaccinationReported.getExternalLinkSystem(),
//				vaccinationReported.getExternalLink())
		);
		if (vaccinationMaster != null) {
			vaccinationReported.setVaccination(vaccinationMaster);
		}
		return vaccinationReported;
	}

	public void fillFromFhirResource(VaccinationMaster vr, Immunization i) {
		vr.setVaccinationId(StringUtils.defaultString(new IdType(i.getId()).getIdPart()));
		vr.setUpdatedDate(i.getMeta().getLastUpdated());
		for (Identifier identifier : i.getIdentifier()) {
			vr.addBusinessIdentifier(BusinessIdentifier.fromR4(identifier));
		}
		if (i.getPatient() != null && StringUtils.isNotBlank(i.getPatient().getReference())) {
			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference()));
//			vr.setPatientReported(fhirRequests.readPatientReported(i.getPatient().getReference().split("Patient/")[0]));
		}

		vr.setReportedDate(i.getRecorded());
		vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

		i.getVaccineCode().getCoding().forEach(coding -> {
			switch (coding.getSystem()) {
				case CVX: {
					vr.setVaccineCvxCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
				case NDC: {
					vr.setVaccineNdcCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
				case MVX: {
					vr.setVaccineMvxCode(StringUtils.defaultString(coding.getCode()));
					break;
				}
			}
		});

		vr.setVaccineMvxCode(i.getManufacturer().getIdentifier().getValueElement().getValueNotNull());

		vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());

		vr.setInformationSource(StringUtils.defaultString(i.getReportOrigin().getCodingFirstRep().getCode()));
		vr.setUpdatedDate(new Date());

		vr.setLotnumber(StringUtils.defaultString(i.getLotNumber()));
		vr.setExpirationDate(i.getExpirationDate());
		if (i.getStatus() != null) {
			switch (i.getStatus()) {
				case COMPLETED: {
					vr.setCompletionStatus("CP");
					break;
				}
				case ENTEREDINERROR: {
					vr.setActionCode("D");
					break;
				}
				case NOTDONE: {
					vr.setCompletionStatus("RE");
					break;
				} //Could also be NA or PA
				case NULL:
				default:
					vr.setCompletionStatus("");
					break;
			}
		}
		Extension actionCode = i.getExtensionByUrl(ACTION_CODE_EXTENSION);
		if (actionCode != null) {
			if (actionCode.hasValue()) {
				vr.setActionCode(MappingHelper.extensionGetCoding(actionCode).getCode());
			}
		} else {
			vr.setActionCode(null);
		}
		vr.setRefusalReasonCode(StringUtils.defaultString(i.getStatusReason().getCodingFirstRep().getCode()));
		vr.setBodySite(StringUtils.defaultString(i.getSite().getCodingFirstRep().getCode()));
		vr.setBodyRoute(StringUtils.defaultString(i.getRoute().getCodingFirstRep().getCode()));
		vr.setFundingSource(StringUtils.defaultString(i.getFundingSource().getCodingFirstRep().getCode()));
		vr.setFundingEligibility(StringUtils.defaultString(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode()));

		if (i.getLocation() != null && StringUtils.isNotBlank(i.getLocation().getReference())) {
			vr.setOrgLocation(fhirRequests.readOrgLocation(i.getLocation().getReference()));
		}
		for (Immunization.ImmunizationPerformerComponent performer : i.getPerformer()) {
			if (performer.getActor() != null && StringUtils.isNotBlank(performer.getActor().getReference())) {
				switch (performer.getFunction().getCodingFirstRep().getCode()) {
					case ADMINISTERING: {
						vr.setAdministeringProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
					case ORDERING: {
						vr.setOrderingProvider(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
					case ENTERING: {
						vr.setEnteredBy(fhirRequests.readPractitionerPerson(performer.getActor().getReference()));
						break;
					}
				}
			}
		}
	}

	public VaccinationReported localObjectReported(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
		fillFromFhirResource(vaccinationReported, i); // TODO assert not golden record ?
		return vaccinationReported;
	}

	public VaccinationMaster localObject(Immunization i) {
		VaccinationMaster vaccinationMaster = new VaccinationMaster();
		fillFromFhirResource(vaccinationMaster, i); // TODO assert golden record ?
		return vaccinationMaster;
	}

	/**
	 * This method create the immunization resource based on the vaccinationReported information
	 *
	 * @param vr the vaccinationReported
	 * @return the Immunization resource
	 */
	public Immunization fhirResource(VaccinationMaster vr) {
		Immunization i = new Immunization();
		/*
		 * Id
		 */
		i.setId(StringUtils.defaultString(vr.getVaccinationId())); // TODO maybe remove ?
		/*
		 * Identifiers
		 */
		for (BusinessIdentifier businessIdentifier : vr.getBusinessIdentifiers()) {
			i.addIdentifier(businessIdentifier.toR4());
		}
		/*
		 * Patient
		 */
		i.setPatient(new Reference().setReference("Patient/" + vr.getPatientReported().getPatientId()));
		/*
		 * Recorded Date
		 */
		i.setRecorded(vr.getReportedDate());
		/*
		 * Occurrence
		 */
		i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());
		/*
		 * CVX
		 */
		if (!vr.getVaccineCvxCode().isBlank()) {
			i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);
		}
		/*
		 * NDC
		 */
		if (!vr.getVaccineNdcCode().isBlank()) {
			i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
		}
		/*
		 * Manufacturer MVX
		 */
		if (StringUtils.isNotBlank(vr.getVaccineMvxCode())) {
			i.setManufacturer(new Reference().setIdentifier(new Identifier().setSystem(MVX).setValue(vr.getVaccineMvxCode())));
		}
		/*
		 * Administered Amount
		 */
		if (StringUtils.isNotBlank(vr.getAdministeredAmount())) {
			i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
		}

		/*
		 * Lot Number
		 */
		i.setLotNumber(vr.getLotnumber());
		/*
		 * Expiration Date
		 */
		i.setExpirationDate(vr.getExpirationDate());
		/*
		 * Action code Status
		 */
		if (vr.getActionCode() != null) {
			i.addExtension().setUrl(ACTION_CODE_EXTENSION).setValue(new Coding().setCode(vr.getActionCode()).setSystem(ACTION_CODE_SYSTEM));
			if (vr.getActionCode().equals("D")) {
				i.setStatus(Immunization.ImmunizationStatus.ENTEREDINERROR);
			} else {
				switch (vr.getCompletionStatus()) {
					case "CP": {
						i.setStatus(Immunization.ImmunizationStatus.COMPLETED);
						break;
					}
					case "NA":
					case "PA":
					case "RE": {
						i.setStatus(Immunization.ImmunizationStatus.NOTDONE);
						break;
					}
					case "":
					default: {
						//					 i.setStatus(Immunization.ImmunizationStatus.NULL);
						break;
					}
				}
			}
		}
		/*
		 * Status Reason
		 */
		if (vr.getRefusalReasonCode() != null) {
			Coding coding = new Coding().setSystem(REFUSAL_REASON_CODE).setCode(vr.getRefusalReasonCode());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_REFUSAL, vr.getRefusalReasonCode());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			CodeableConcept codeableConcept = new CodeableConcept(coding);
			i.setStatusReason(codeableConcept);
		}
		/*
		 * Body Part
		 */
		if (vr.getBodySite() != null) {
			Coding coding = new Coding().setSystem(BODY_PART_SITE_SYSTEM).setCode(vr.getBodySite());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.BODY_SITE, vr.getBodySite());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.getSite().addCoding(coding);
		}
		/*
		 * Body Route
		 */
		if (vr.getBodyRoute() != null) {
			Coding coding = new Coding().setSystem(BODY_ROUTE_SYSTEM).setCode(vr.getBodyRoute());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.BODY_ROUTE, vr.getBodyRoute());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.getRoute().addCoding(coding);
		}
		/*
		 * Funding Source
		 */
		if (vr.getFundingSource() != null) {
			Coding coding = new Coding().setSystem(FUNDING_SOURCE_SYSTEM).setCode(vr.getFundingSource());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_FUNDING_SOURCE, vr.getFundingSource());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.getFundingSource().addCoding(coding);
		}
		/*
		 * Program Funding Eligibility
		 */
		if (vr.getFundingEligibility() != null) {
			Coding coding = new Coding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.FINANCIAL_STATUS_CODE, vr.getFundingEligibility());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.addProgramEligibility().addCoding(coding);
		}
		/*
		 * Location
		 */
		if (!vr.getOrgLocationId().isBlank()) {
			i.setLocation(new Reference(MappingHelper.LOCATION + "/" + vr.getOrgLocationId()));
		}
		/*
		 * Information Source / Report Origin
		 */
		if (vr.getInformationSource() != null) {
			Coding coding = new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource());
			Code code = CodeMapManager.getCodeMap().getCodeForCodeset(CodesetType.VACCINATION_INFORMATION_SOURCE, vr.getInformationSource());
			if (code != null) {
				coding.setDisplay(code.getLabel());
			}
			i.setReportOrigin(new CodeableConcept(coding));
		}
		/*
		 * Entering Performer
		 */
		if (vr.getEnteredBy() != null) {
			i.addPerformer(performer(vr.getEnteredBy(), ENTERING, ENTERING_DISPLAY));
		}
		/*
		 * Ordering Performer
		 */
		if (vr.getOrderingProvider() != null) {
			i.addPerformer(performer(vr.getOrderingProvider(), ORDERING, ORDERING_DISPLAY));
		}
		/*
		 * Administering Performer
		 */
		if (vr.getAdministeringProvider() != null) {
			i.addPerformer(performer(vr.getAdministeringProvider(), ADMINISTERING, ADMINISTERING_DISPLAY));
		}
		return i;
	}

	private Immunization.ImmunizationPerformerComponent performer(ModelPerson person, String functionCode, String functionDisplay) {
		Immunization.ImmunizationPerformerComponent performer = new Immunization.ImmunizationPerformerComponent();
		performer.setFunction(new CodeableConcept().addCoding(new Coding().setSystem(PERFORMER_FUNCTION_SYSTEM).setCode(functionCode).setDisplay(functionDisplay)));
		Reference actor = null;
		switch (person.getIdentifierTypeCode()) {
			case MappingHelper.PRACTITIONER: {
				actor = new Reference(MappingHelper.PRACTITIONER + "/" + person.getPersonId());
				break;
			}
//		  case MappingHelper.PERSON: { TODO
//			  actor = MappingHelper.getFhirReference(
//				  MappingHelper.PERSON,
//				  MappingHelper.PERSON_MODEL,
//				  person.getPersonId(),
//				  person.getPersonId());
//			  break;
//		  }
//		  default:{
//			  actor = MappingHelper.getFhirReference(MappingHelper.PRACTITIONER, person.getIdentifierTypeCode(), person.getPersonExternalLink(), person.getPersonId());
//		  }
		}
		performer.setActor(actor);
		return performer;
	}


}