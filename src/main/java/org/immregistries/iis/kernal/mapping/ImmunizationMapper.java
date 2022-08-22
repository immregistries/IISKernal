package org.immregistries.iis.kernal.mapping;

import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.VaccinationMaster;
import org.immregistries.iis.kernal.model.VaccinationReported;

import java.math.BigDecimal;
import java.util.Date;


public class ImmunizationMapper {
	public static final String CVX = "CVX";
	public static final String MVX = "MVX";
	public static final String NDC = "NDC";
	public static final String INFORMATION_SOURCE = "informationSource";
	public static final String FUNCTION = "iis-sandbox-function";
	public static final String ORDERING = "ordering";
	public static final String ENTERING = "entering";
	public static final String ADMINISTERING = "administering";
	public static final String REFUSAL_REASON_CODE = "refusalReasonCode";
	public static final String BODY_PART = "bodyPart";
	public static final String BODY_ROUTE = "bodyRoute";
	public static final String FUNDING_SOURCE = "fundingSource";
	public static final String FUNDING_ELIGIBILITY = "fundingEligibility";

	public static VaccinationReported getReported(Immunization i) {
		VaccinationReported vaccinationReported = new VaccinationReported();
		fillVaccinationReported(vaccinationReported,i);
		VaccinationMaster vaccinationMaster = getMaster(null,i);
		vaccinationReported.setVaccination(vaccinationMaster);
		vaccinationMaster.setVaccinationReported(vaccinationReported);
		return vaccinationReported;
	}

  /**
   * This method fills a vaccinationReported object with mapped fhir immunization information
   * @param vr the vaccinationReported
   * @param i the Immunization resource
   */
  public static void fillVaccinationReported(VaccinationReported vr, Immunization i) {
	  vr.setVaccinationReportedId(new  IdType(i.getId()).getIdPart());
	  vr.setUpdatedDate(i.getMeta().getLastUpdated());
     vr.setVaccinationReportedExternalLink(MappingHelper.filterIdentifier(i.getIdentifier(),MappingHelper.VACCINATION_REPORTED).getValue());
	  vr.setPatientReportedId(i.getPatient().getReference());

	  vr.setReportedDate(i.getRecorded());
	  vr.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());

	  vr.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
	  vr.setVaccineNdcCode(i.getVaccineCode().getCode(NDC));
	  vr.setVaccineMvxCode(i.getVaccineCode().getCode(MVX));

	  vr.setVaccineMvxCode(i.getManufacturer().getIdentifier().getValue());


	 vr.setAdministeredAmount(i.getDoseQuantity().getValue().toString());
	 vr.setInformationSource(i.getInformationSourceCodeableConcept().getCode(INFORMATION_SOURCE));
    vr.setUpdatedDate(new Date());

    vr.setLotnumber(i.getLotNumber());
    vr.setExpirationDate(i.getExpirationDate());
    switch(i.getStatus()){
        case COMPLETED : vr.setCompletionStatus("CP"); break;
        case ENTEREDINERROR : vr.setActionCode("D"); break;
        case NOTDONE : vr.setCompletionStatus("RE"); break; //Could also be NA or PA
      default:
        break;
    }

    vr.setRefusalReasonCode(i.getReasonFirstRep().getConcept().getCodingFirstRep().getCode());
	 vr.setBodySite(i.getSite().getCodingFirstRep().getCode());
	 vr.setBodyRoute(i.getRoute().getCodingFirstRep().getCode());
	 vr.setFundingSource(i.getFundingSource().getCodingFirstRep().getCode());
	 vr.setFundingEligibility(i.getProgramEligibilityFirstRep().getCodingFirstRep().getCode());

//	 vr.setOrgLocation(LocationMapper.orgLocationFromFhir(i.getLocation()));
//	  vr.setEnteredBy();
//	  vr.setAdministeringProvider(); TODO but not urgent
//	  vr.getOrderingProvider()
  }


  public static VaccinationMaster getMaster(VaccinationMaster vm, Immunization i){
	  if (vm == null){
		  vm = new VaccinationMaster();
	  }
	  vm.setVaccinationId(MappingHelper.filterIdentifier(i.getIdentifier(), MappingHelper.VACCINATION_MASTER).getValue());
	  vm.setAdministeredDate(i.getOccurrenceDateTimeType().getValue());
	  vm.setVaccineCvxCode(i.getVaccineCode().getCode(CVX));
//	  vm.setPatient();
//	  vm.setVaccinationReported();
	  return vm;
  }

  /**
   * This method create the immunization resource based on the vaccinationReported information
   * @param vaccinationMaster the vaccinationMaster
   * @param vr the vaccinationReported
   * @return the Immunization resource
   */
  public static Immunization getFhirResource(VaccinationMaster vaccinationMaster, VaccinationReported vr) {
    Immunization i = new Immunization();
	 if (vaccinationMaster != null ){
		i.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.VACCINATION_MASTER,vaccinationMaster.getVaccinationReported().getVaccinationReportedExternalLink()));
		i.setOccurrence(new DateTimeType(vaccinationMaster.getAdministeredDate()));
		i.getVaccineCode().addCoding().setSystem(CVX).setCode(vaccinationMaster.getVaccineCvxCode());
//		i.setPatient(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_MASTER, vaccinationMaster.getPatient().getPatientExternalLink()));
	 }
	 if (vr != null) {
		 i.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.VACCINATION_REPORTED, vr.getVaccinationReportedExternalLink()));

		 i.setPatient(MappingHelper.getFhirReference(MappingHelper.PATIENT,MappingHelper.PATIENT_REPORTED, vr.getPatientReported().getPatientReportedExternalLink(), vr.getPatientReported().getPatientReportedId()));
//		 i.setPatient(new Reference().setReference("Patient/"+ vr.getPatientReported().getPatientReportedId()));
		 i.setRecorded(vr.getReportedDate());
		 i.getOccurrenceDateTimeType().setValue(vr.getAdministeredDate());

		 if (!i.getVaccineCode().hasCoding("CVX")) {
			 i.getVaccineCode().addCoding().setCode(vr.getVaccineCvxCode()).setSystem(CVX);

		 }
		 i.getVaccineCode().addCoding().setCode(vr.getVaccineNdcCode()).setSystem(NDC);
//		 i.getVaccineCode().addCoding().setCode(vr.getVaccineMvxCode()).setSystem(MVX);
		 i.setManufacturer(MappingHelper.getFhirReference(MappingHelper.ORGANISATION,MVX,vr.getVaccineMvxCode()));

		 i.setDoseQuantity(new Quantity().setValue(new BigDecimal(vr.getAdministeredAmount())));
		 i.setInformationSource(new CodeableConcept(new Coding().setSystem(INFORMATION_SOURCE).setCode(vr.getInformationSource()))); // TODO change system name
		 i.setLotNumber(vr.getLotnumber());
		 i.setExpirationDate(vr.getExpirationDate());

		 if (vr.getActionCode().equals("D")) {
			 i.setStatus(Immunization.ImmunizationStatusCodes.ENTEREDINERROR);
		 } else {
			 switch(vr.getCompletionStatus()) {
				 case "CP" : {
					 i.setStatus(Immunization.ImmunizationStatusCodes.COMPLETED);
					 break;
				 }
				 case "NA" :
				 case "PA" :
				 case "RE" : {
					 i.setStatus(Immunization.ImmunizationStatusCodes.NOTDONE);
					 break;
				 }
				 case "" : {
					 i.setStatus(Immunization.ImmunizationStatusCodes.NULL);
					 break;
				 }
				 default: break;
			 }
		 }
		 i.addReason().setConcept(new CodeableConcept(new Coding(REFUSAL_REASON_CODE,vr.getRefusalReasonCode(),vr.getRefusalReasonCode())));
		 i.getSite().addCoding().setSystem(BODY_PART).setCode(vr.getBodySite());
		 i.getRoute().addCoding().setSystem(BODY_ROUTE).setCode(vr.getBodyRoute());
		 i.getFundingSource().addCoding().setSystem(FUNDING_SOURCE).setCode(vr.getFundingSource());
		 i.addProgramEligibility().addCoding().setSystem(FUNDING_ELIGIBILITY).setCode(vr.getFundingEligibility());


		 Location location  = LocationMapper.fhirLocation(vr.getOrgLocation()); // TODO save it ?
		 i.setLocation(new Reference(location));

		 if (vr.getEnteredBy() != null) {
			 i.addPerformer()
				 .setFunction(new CodeableConcept().addCoding(new Coding().setSystem(FUNCTION).setCode(ENTERING)))
				 .setActor(MappingHelper.getFhirReference(MappingHelper.PERSON,MappingHelper.PERSON_MODEL, vr.getEnteredBy().getPersonId()));
		 }
		 if (vr.getOrderingProvider() != null) {
			 i.addPerformer()
				 .setFunction(new CodeableConcept().addCoding(new Coding().setSystem(FUNCTION).setCode(ORDERING)))
				 .setActor(MappingHelper.getFhirReference(MappingHelper.PERSON, MappingHelper.PERSON_MODEL, vr.getOrderingProvider().getPersonId()));
		 }
		 if (vr.getAdministeringProvider() != null) {
			 i.addPerformer()
				 .setFunction(new CodeableConcept().addCoding(new Coding().setSystem(FUNCTION).setCode(ADMINISTERING)))
				 .setActor(MappingHelper.getFhirReference(MappingHelper.PERSON, MappingHelper.PERSON_MODEL, vr.getAdministeringProvider().getPersonId()));
		 }
	 }
    return i;
  }


}