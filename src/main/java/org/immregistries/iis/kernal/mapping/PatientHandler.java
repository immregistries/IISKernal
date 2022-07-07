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

	public static PatientReported getPatientReportedFromFhir(Patient p) {
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
		patientReported.setPatientReportedId(MappingHelper.filterIdentifier(p.getIdentifier(),"PatientReported").getValue());
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
		for (Coding coding: p.getExtensionByUrl("race").getValueCodeableConcept().getCoding()) {
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
		patientReported.setPatientEthnicity(p.getExtensionByUrl("ethnicity").getValueCodeType().getValue());

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
					patientReported.setPatientDeathFlag("Y");
				} else {
					patientReported.setPatientDeathFlag("N");
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
					patientReported.setPatientBirthFlag("Y");
				} else {
					patientReported.setPatientBirthFlag("N");
				}
			}
		} else {
			patientReported.setPatientBirthOrder(String.valueOf(p.getMultipleBirthIntegerType()));
		}

		patientReported.setPublicityIndicator(p.getExtensionByUrl("publicity").getValueCoding().getCode());
		patientReported.setPublicityIndicatorDate(new Date(p.getExtensionByUrl("publicity").getValueCoding().getVersion()));

		patientReported.setProtectionIndicator(p.getExtensionByUrl("protection").getValueCoding().getCode());
		patientReported.setProtectionIndicatorDate(new Date(p.getExtensionByUrl("protection").getValueCoding().getVersion()));

		patientReported.setRegistryStatusIndicator(p.getExtensionByUrl("registryStatus").getValueCoding().getCode());
		patientReported.setRegistryStatusIndicatorDate(new Date(p.getExtensionByUrl("registryStatus").getValueCoding().getVersion()));

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
		patientMaster.setPatientId(MappingHelper.filterIdentifier(p.getIdentifier(),"PatientReported").getValue());

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
			p.addIdentifier(MappingHelper.getFhirIdentifier("PatientMaster", pm.getPatientId()));
			HumanName name = p.addName();
			name.setFamily(pm.getPatientNameLast());
			name.addGivenElement().setValue(pm.getPatientNameFirst());
			name.addGivenElement().setValue(pm.getPatientNameMiddle());
			p.setBirthDate(pm.getPatientBirthDate());
		}
		if (pr != null) {
			p.addIdentifier(MappingHelper.getFhirIdentifier("PatientReported", pr.getPatientReportedExternalLink()));
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
			raceExtension.setUrl("race");
			CodeableConcept race = new CodeableConcept().setText("race");
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
			p.addExtension("ethnicity",new CodeType().setSystem("ethnicity").setValue(pr.getPatientEthnicity()));
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
			} else if (pr.getPatientDeathFlag().equals("Y")) {
				p.setDeceased(new BooleanType(true));
			} else if (pr.getPatientDeathFlag().equals("N")) {
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
			} else if (pr.getPatientBirthFlag().equals("Y")) {
				p.setMultipleBirth(new BooleanType(true));
			}

			Extension publicity =  p.addExtension();
			publicity.setUrl("publicity");
			publicity.setValue(
				new Coding().setSystem("publicityIndicator")
					.setCode(pr.getPublicityIndicator()));
			if (pr.getPublicityIndicatorDate() != null) {
				publicity.getValueCoding().setVersion(pr.getPublicityIndicatorDate().toString());
			}
			Extension protection =  p.addExtension();
			protection.setUrl("protection");
			protection.setValue(
				new Coding().setSystem("protectionIndicator")
					.setCode(pr.getProtectionIndicator()));
			if (pr.getProtectionIndicatorDate() != null) {
				protection.getValueCoding().setVersion(pr.getProtectionIndicatorDate().toString());
			}

			Extension registryStatus =  p.addExtension();
			registryStatus.setUrl("registryStatus");
			registryStatus.setValue(
				new Coding().setSystem("registryStatusIndicator")
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