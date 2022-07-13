package org.immregistries.iis.kernal.mapping;


import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r5.model.Enumerations.AdministrativeGender;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;

import java.util.Date;

public class PatientHandler {

	private PatientHandler() {
	}

	private static final String REGISTRY_STATUS_EXTENSION = "registryStatus";
	private static final String REGISTRY_STATUS_INDICATOR = "registryStatusIndicator";
	private static final String ETHNICITY_EXTENSION = "ethnicity";
	private static final String ETHNICITY_SYSTEM = "ethnicity";
	private static final String RACE = "race";
	private static final String RACE_SYSTEM = "race";
	private static final String PUBLICITY_EXTENSION = "publicity";
	private static final String PUBLICITY_SYSTEM = "publicityIndicator";
	private static final String PROTECTION_EXTENSION = "protection";
	private static final String PROTECTION_SYSTEM = "protectionIndicator";
	private static final String YES = "Y";
	private static final String NO = "N";

	public static PatientReported getReported(Patient p) {
		PatientReported patientReported = new PatientReported();
		patientReported.setPatientReportedId(p.getId());
		fillPatientReportedFromFhir(patientReported, p);
		getPatientMasterFromFhir(patientReported.getPatient(), p);
		return patientReported;
	}

	/**
	 * This method set the patientReported information based on the patient information
	 *
	 * @param patientReported the patientReported
	 * @param p               the Patient resource
	 */
	private static void fillPatientReportedFromFhir(PatientReported patientReported, Patient p) {
		patientReported.setPatientReportedId(MappingHelper.filterIdentifier(p.getIdentifier(),MappingHelper.PATIENT_REPORTED).getValue());
		patientReported.setPatientReportedAuthority(p.getManagingOrganization().getIdentifier().getValue());
		patientReported.setPatientBirthDate(p.getBirthDate());
		// Name
		HumanName name = p.getNameFirstRep();
		patientReported.setPatientNameLast(name.getFamily());
		if (name.getGiven().size() > 0) {
			patientReported.setPatientNameFirst(name.getGiven().get(0).getValueNotNull());
		}
		if (name.getGiven().size() > 1) {
			patientReported.setPatientNameMiddle(name.getGiven().get(1).getValueNotNull());
		}

//		patientReported.setPatientMotherMaiden(); TODO
		switch (p.getGender()) {
			case MALE:
				patientReported.setPatientSex("M");
				break;
			case FEMALE:
				patientReported.setPatientSex("F");
				break;
			case OTHER:
			default:
				patientReported.setPatientSex("");
				break;
		}
		int raceNumber = 0;
		for (Coding coding: p.getExtensionByUrl(RACE).getValueCodeableConcept().getCoding()) {
			raceNumber++;
			switch (raceNumber) {
				case 1:{
					patientReported.setPatientRace(coding.getCode());
				}
				case 2:{
					patientReported.setPatientRace2(coding.getCode());
				}
				case 3:{
					patientReported.setPatientRace3(coding.getCode());
				}
				case 4:{
					patientReported.setPatientRace4(coding.getCode());
				}
				case 5:{
					patientReported.setPatientRace5(coding.getCode());
				}
				case 6:{
					patientReported.setPatientRace6(coding.getCode());
				}
			}
		}
		if (p.getExtensionByUrl(ETHNICITY_EXTENSION) != null) {
			patientReported.setPatientEthnicity(p.getExtensionByUrl(ETHNICITY_EXTENSION).getValueCodeType().getValue());
		}

		for (ContactPoint telecom : p.getTelecom()) {
			if (null != telecom.getSystem()) {
				if (telecom.getSystem().equals(ContactPointSystem.PHONE)) {
					patientReported.setPatientPhone(telecom.getValue());
				} else if (telecom.getSystem().equals(ContactPointSystem.EMAIL)) {
					patientReported.setPatientEmail(telecom.getValue());
				}
			}
		}

		if (null != p.getDeceased()) {
			if (p.getDeceased().isBooleanPrimitive()) {
				if (p.getDeceasedBooleanType().booleanValue()) {
					patientReported.setPatientDeathFlag(YES);
				} else {
					patientReported.setPatientDeathFlag(NO);
				}
			}
			if (p.getDeceased().isDateTime()) {
				patientReported.setPatientDeathDate(p.getDeceasedDateTimeType().getValue());
			}
		}
		// Address
		Address address = p.getAddressFirstRep();
		if (address.getLine().size() > 0) {
			patientReported.setPatientAddressLine1(address.getLine().get(0).getValueNotNull());
		}
		if (address.getLine().size() > 1) {
			patientReported.setPatientAddressLine2(address.getLine().get(1).getValueNotNull());
		}
		patientReported.setPatientAddressCity(address.getCity());
		patientReported.setPatientAddressState(address.getState());
		patientReported.setPatientAddressZip(address.getPostalCode());
		patientReported.setPatientAddressCountry(address.getCountry());
		patientReported.setPatientAddressCountyParish(address.getDistrict());

		if (null != p.getMultipleBirth()) {
			if (p.getMultipleBirth().isBooleanPrimitive()) {
				if (p.getMultipleBirthBooleanType().booleanValue()) {
					patientReported.setPatientBirthFlag(YES);
				} else {
					patientReported.setPatientBirthFlag(NO);
				}
			}
		} else {
			patientReported.setPatientBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
		}

		if (p.getExtensionByUrl(PUBLICITY_EXTENSION) != null) {
			patientReported.setPublicityIndicator(p.getExtensionByUrl(PUBLICITY_EXTENSION).getValueCoding().getCode());
			patientReported.setPublicityIndicatorDate(new Date(p.getExtensionByUrl(PUBLICITY_EXTENSION).getValueCoding().getVersion()));
		}

		if (p.getExtensionByUrl(PROTECTION_EXTENSION) != null) {
			patientReported.setProtectionIndicator(p.getExtensionByUrl(PROTECTION_EXTENSION).getValueCoding().getCode());
			if (p.getExtensionByUrl(PROTECTION_EXTENSION).getValueCoding().getVersion() != null) {
				patientReported.setProtectionIndicatorDate(new Date());
			}
		}

		if (p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION) != null) {
			patientReported.setRegistryStatusIndicator(p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION).getValueCoding().getCode());
			patientReported.setRegistryStatusIndicatorDate(new Date(p.getExtensionByUrl(REGISTRY_STATUS_EXTENSION).getValueCoding().getVersion()));

		}


		// patientReported.setRegistryStatusIndicator(p.getActive());
		// Patient Contact / Guardian
		Patient.ContactComponent contact = p.getContactFirstRep();
		patientReported.setGuardianLast(contact.getName().getFamily());
		if (p.getContactFirstRep().getName().getGiven().size() > 0) {
			patientReported.setGuardianFirst(contact.getName().getGiven().get(0).getValueNotNull());
		}
		if (p.getContactFirstRep().getName().getGiven().size() > 1) {
			patientReported.setGuardianMiddle(contact.getName().getGiven().get(1).getValueNotNull());
		}
		patientReported.setGuardianRelationship(contact.getRelationshipFirstRep().getText());


//		PatientMaster patientMaster = patientReported.getPatient();
//		if (patientMaster == null) {
//			patientMaster = new PatientMaster();
//		}
//		if (patientMaster.getPatientNameLast().equals("")) { //TODO improve this condition
//
//			patientMaster.setPatientId(patientReported.getPatientReportedId());
//			patientMaster.setPatientExternalLink(patientReported.getPatientReportedExternalLink());
//			patientMaster.setPatientNameLast(patientReported.getPatientNameLast());
//			patientMaster.setPatientNameFirst(patientReported.getPatientNameFirst());
//			patientMaster.setPatientNameMiddle(patientReported.getPatientNameMiddle());
//			patientMaster.setPatientBirthDate(patientReported.getPatientBirthDate());
//      patientMaster.setPatientPhoneFrag(patientReported.getPatientPhone());
//      patientMaster.setPatientAddressFrag(patientReported.getPatientAddressZip());
//		}


	}

	public static PatientMaster getPatientMasterFromFhir(PatientMaster patientMaster, Patient p) {
		patientMaster.setPatientId(MappingHelper.filterIdentifier(p.getIdentifier(),MappingHelper.PATIENT_REPORTED).getValue());

		if (patientMaster == null) {
			patientMaster = new PatientMaster();
		}
		patientMaster.setPatientId(p.getId());
		patientMaster.setPatientNameFirst(p.getNameFirstRep().getGiven().get(0).getValue());
		if (p.getNameFirstRep().getGiven().size() > 1) {
			patientMaster.setPatientNameMiddle(p.getNameFirstRep().getGiven().get(1).getValue());
		}
		patientMaster.setPatientNameLast(p.getNameFirstRep().getFamily());
		patientMaster.setPatientExternalLink(p.getIdentifierFirstRep().getId());
//	  patientMaster.setPatientAddressFrag();
		return patientMaster;
	}

	public static void getFhirPatient(Patient p, PatientMaster pm, PatientReported pr) {
//		Patient p = new Patient().setBirthDate(new Date());
		if (pm != null) {
			p.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.PATIENT_MASTER, pm.getPatientId()));
			HumanName name = p.addName();
			name.setFamily(pm.getPatientNameLast());
			name.addGivenElement().setValue(pm.getPatientNameFirst());
			name.addGivenElement().setValue(pm.getPatientNameMiddle());
			p.setBirthDate(pm.getPatientBirthDate());
		}
		if (pr != null) {
			p.addIdentifier(MappingHelper.getFhirIdentifier(MappingHelper.PATIENT_REPORTED, pr.getPatientReportedExternalLink()));
//			p.setManagingOrganization(MappingHelper.getFhirReference("","PatientReportedAuthority",pr.getPatientReportedAuthority()));
			p.setBirthDate(pr.getPatientBirthDate());
			if (p.getNameFirstRep() != null) {
				HumanName name = p.addName();
//				name.setUse(HumanName.NameUse.USUAL);
				name.setFamily(pr.getPatientNameLast());
				name.addGivenElement().setValue(pr.getPatientNameFirst());
				name.addGivenElement().setValue(pr.getPatientNameMiddle());
			}
//			p.addName().setUse(HumanName.NameUse.MAIDEN).setFamily(pr.getPatientMotherMaiden()); TODO
			switch (pr.getPatientSex()) {
				case "M":
					p.setGender(AdministrativeGender.MALE);
					break;
				case "F":
					p.setGender(AdministrativeGender.FEMALE);
					break;
				default:
					p.setGender(AdministrativeGender.OTHER);
					break;
			}

			//Race and ethnicity
			Extension raceExtension =  p.addExtension();
			raceExtension.setUrl(RACE);
			CodeableConcept race = new CodeableConcept().setText(RACE_SYSTEM);
			raceExtension.setValue(race);
			if (pr.getPatientRace() != null && !pr.getPatientRace().equals("")) {
				race.addCoding().setCode(pr.getPatientRace());
			}
			if (pr.getPatientRace2() != null && !pr.getPatientRace2().equals("")) {
				race.addCoding().setCode(pr.getPatientRace2());
			}
			if (pr.getPatientRace3() != null && !pr.getPatientRace3().equals("")) {
				race.addCoding().setCode(pr.getPatientRace3());
			}
			if (pr.getPatientRace4() != null && !pr.getPatientRace4().equals("")) {
				race.addCoding().setCode(pr.getPatientRace4());
			}
			if (pr.getPatientRace5() != null && !pr.getPatientRace5().equals("")) {
				race.addCoding().setCode(pr.getPatientRace5());
			}
			if (pr.getPatientRace6() != null && !pr.getPatientRace6().equals("")) {
				race.addCoding().setCode(pr.getPatientRace6());
			}
			p.addExtension(ETHNICITY_EXTENSION,new CodeType().setSystem(ETHNICITY_SYSTEM).setValue(pr.getPatientEthnicity()));
			// telecom
			if (null != pr.getPatientPhone()) {
				p.addTelecom().setSystem(ContactPointSystem.PHONE)
					.setValue(pr.getPatientPhone());
			}
			if (null != pr.getPatientEmail()) {
				p.addTelecom().setSystem(ContactPointSystem.EMAIL)
					.setValue(pr.getPatientEmail());
			}


			if (pr.getPatientDeathDate() != null) {
				p.setDeceased(new DateType(pr.getPatientDeathDate()));
			} else if (pr.getPatientDeathFlag().equals(YES)) {
				p.setDeceased(new BooleanType(true));
			} else if (pr.getPatientDeathFlag().equals(NO)) {
				p.setDeceased(new BooleanType(false));
			}

			Address address = p.addAddress();
			address.addLine(pr.getPatientAddressLine1());
			address.addLine(pr.getPatientAddressLine2());
			address.setCity(pr.getPatientAddressCity());
			address.setCountry(pr.getPatientAddressCountry());
			address.setState(pr.getPatientAddressState());
			address.setDistrict(pr.getPatientAddressCountyParish());
			address.setPostalCode(pr.getPatientAddressZip());

			if (pr.getPatientBirthOrder() != null && !pr.getPatientBirthOrder().equals("")) {
				p.setMultipleBirth(new IntegerType().setValue(Integer.parseInt(pr.getPatientBirthOrder())));
			} else if (pr.getPatientBirthFlag().equals(YES)) {
				p.setMultipleBirth(new BooleanType(true));
			}

			Extension publicity =  p.addExtension();
			publicity.setUrl(PUBLICITY_EXTENSION);
			publicity.setValue(
				new Coding().setSystem(PUBLICITY_SYSTEM)
					.setCode(pr.getPublicityIndicator()));
			if (pr.getPublicityIndicatorDate() != null) {
				publicity.getValueCoding().setVersion(pr.getPublicityIndicatorDate().toString());
			}
			Extension protection =  p.addExtension();
			protection.setUrl(PROTECTION_EXTENSION);
			protection.setValue(
				new Coding().setSystem(PROTECTION_SYSTEM)
					.setCode(pr.getProtectionIndicator()));
			if (pr.getProtectionIndicatorDate() != null) {
				protection.getValueCoding().setVersion(pr.getProtectionIndicatorDate().toString());
			}

			Extension registryStatus =  p.addExtension();
			registryStatus.setUrl(REGISTRY_STATUS_EXTENSION);
			registryStatus.setValue(
				new Coding().setSystem(REGISTRY_STATUS_INDICATOR)
					.setCode(pr.getRegistryStatusIndicator()));
			if (pr.getRegistryStatusIndicatorDate() != null) {
				registryStatus.getValueCoding().setVersion(pr.getRegistryStatusIndicatorDate().toString());
			}

			Patient.ContactComponent contact = p.addContact();
			HumanName contactName = new HumanName();
			contact.setName(contactName);
			contact.addRelationship().setText(pr.getGuardianRelationship());
			contactName.setFamily(pr.getGuardianLast());
			contactName.addGivenElement().setValue(pr.getGuardianFirst());
			contactName.addGivenElement().setValue(pr.getGuardianMiddle());
		}
//		return p;
	}

}