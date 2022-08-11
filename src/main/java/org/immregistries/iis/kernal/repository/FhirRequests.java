package org.immregistries.iis.kernal.repository;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.gclient.IUpdateWithQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.*;
import org.immregistries.iis.kernal.model.*;
import org.immregistries.iis.kernal.model.ModelPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.immregistries.iis.kernal.mapping.MappingHelper.OBSERVATION_REPORTED;
import static org.immregistries.iis.kernal.mapping.MappingHelper.PATIENT_REPORTED;

@Component
public class FhirRequests {
	@Autowired
	IFhirResourceDao<Patient> patientDao;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;

	Logger logger = LoggerFactory.getLogger(FhirRequests.class);

	public PatientMaster searchPatientMaster(IGenericClient fhirClient, ICriterion... where) {
		PatientMaster patientMaster = null;
		Bundle bundle = searchGoldenRecord(Patient.class,fhirClient, where);
		if (bundle.hasEntry()) {
			patientMaster = PatientMapper.getMaster(new PatientMaster(),(Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientMaster;
	}
	public PatientReported searchPatientReported(IGenericClient fhirClient, ICriterion... where) {
		PatientReported patientReported = null;
		Bundle bundle = searchRegularRecord(Patient.class,fhirClient, where);
		if (bundle.hasEntry()) {
			patientReported = PatientMapper.getReported((Patient) bundle.getEntryFirstRep().getResource());
		}
		return patientReported;
	}

	public List<PatientReported> searchPatientReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<PatientReported> patientReportedList = new ArrayList<>();
		Bundle bundle = searchRegularRecord(Patient.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			patientReportedList.add(PatientMapper.getReported((Patient) entry.getResource()));
		}
		return patientReportedList;
	}
	public VaccinationReported searchVaccinationReported(IGenericClient fhirClient, ICriterion... where) {
		VaccinationReported vaccinationReported = null;
		Bundle bundle = search(Immunization.class,fhirClient, where);
		if (bundle.hasEntry()) {
			vaccinationReported = ImmunizationMapper.getReported((Immunization) bundle.getEntryFirstRep().getResource());
		}
		return vaccinationReported;
	}
	public List<VaccinationReported> searchVaccinationReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<VaccinationReported> vaccinationReportedList = new ArrayList<>();
		Bundle bundle = search(Immunization.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			vaccinationReportedList.add(ImmunizationMapper.getReported((Immunization) entry.getResource()));
		}
		return vaccinationReportedList;
	}
	public ObservationReported searchObservationReported(IGenericClient fhirClient, ICriterion... where) {
		ObservationReported observationReported = null;
		Bundle bundle = search(Observation.class,fhirClient, where);
		if (bundle.hasEntry()) {
			observationReported = ObservationMapper.getReported((Observation) bundle.getEntryFirstRep().getResource());
		}
		return observationReported;
	}
	public List<ObservationReported> searchObservationReportedList(IGenericClient fhirClient, ICriterion... where) {
		List<ObservationReported> observationReportedList = new ArrayList<>();
		Bundle bundle = search(Observation.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			observationReportedList.add(ObservationMapper.getReported((Observation) entry.getResource()));
		}
		return observationReportedList;
	}
	public OrgLocation searchOrgLocation(IGenericClient fhirClient, ICriterion... where) {
		OrgLocation orgLocation = null;
		Bundle bundle = search(Observation.class,fhirClient, where);
		if (bundle.hasEntry()) {
			orgLocation = LocationMapper.orgLocationFromFhir((Location) bundle.getEntryFirstRep().getResource());
		}
		return orgLocation;
	}
	public List<OrgLocation> searchOrgLocationList(IGenericClient fhirClient, ICriterion... where) {
		List<OrgLocation> locationList = new ArrayList<>();
		Bundle bundle = search(Location.class,fhirClient, where);
		for (Bundle.BundleEntryComponent entry: bundle.getEntry()) {
			locationList.add(LocationMapper.orgLocationFromFhir((Location) entry.getResource()));
		}
		return locationList;
	}


	public ModelPerson searchPerson(IGenericClient fhirClient, ICriterion... where) {
		ModelPerson modelPerson = null;
		Bundle bundle = search(org.hl7.fhir.r5.model.Person.class,fhirClient, where);
		if (bundle.hasEntry()) {
			modelPerson = PersonMapper.getModelPerson((org.hl7.fhir.r5.model.Person) bundle.getEntryFirstRep().getResource());
		}
		return modelPerson;
	}


	private Bundle searchGoldenRecord(Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
												 IGenericClient fhirClient,
												 ICriterion... where) {
		Bundle bundle =search(aClass,fhirClient,where);
		if (bundle.hasEntry()) {
			List<Bundle.BundleEntryComponent> list = bundle.getEntry().stream()
				// filter the golden records
				.filter(entry -> entry.getResource().getMeta().getTag().stream().anyMatch(coding -> coding.getCode().equals("GOLDEN_RECORD")))
				.collect(Collectors.toList());
			bundle.setEntry(list);
		}
		return bundle;
	}

	private Bundle searchRegularRecord(Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
												 IGenericClient fhirClient,
												 ICriterion... where) {
		Bundle bundle =search(aClass,fhirClient,where);
		if (bundle.hasEntry()) {
			List<Bundle.BundleEntryComponent> list = bundle.getEntry().stream()
				// filter out the golden records
				.filter(entry -> entry.getResource().getMeta().getTag().stream().noneMatch(coding -> coding.getCode().equals("GOLDEN_RECORD")))
				.collect(Collectors.toList());
			bundle.setEntry(list);
		}
		return bundle;
	}
	private Bundle search(
		Class<? extends org.hl7.fhir.instance.model.api.IBaseResource> aClass,
		IGenericClient fhirClient,
		ICriterion... where) {
		try {
			IQuery<Bundle> query = fhirClient.search().forResource(aClass).returnBundle(Bundle.class);
			int size = where.length;
			if (size > 0) {
				query = query.where(where[0]);
			}
			int i = 1;
			while (i < size) {
				query = query.and(where[i]);
				i++;
			}
			return  query.execute();
		} catch (ResourceNotFoundException e) {
			return  new Bundle();
		}
	}

	public PatientReported savePatientReported(IGenericClient fhirClient, PatientReported patientReported) {
		Patient patient =  PatientMapper.getFhirResource( null,patientReported);
//		Patient patient = new Patient();
//		PatientHandler.fillFhirResource(patient, null,patientReported);
		MethodOutcome outcome = save(fhirClient,patient,
			Patient.IDENTIFIER.exactly().systemAndIdentifier(PATIENT_REPORTED,patientReported.getPatientReportedExternalLink()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			patientReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			patientReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return patientReported;
	}

	public ObservationReported saveObservationReported(IGenericClient fhirClient, ObservationReported observationReported) {
		Observation observation = ObservationMapper.getFhirResource(null,observationReported);
		MethodOutcome outcome = save(fhirClient,observation,
			Observation.IDENTIFIER.exactly().systemAndIdentifier(OBSERVATION_REPORTED,observationReported.getObservationReportedId()));
		if (outcome.getCreated() != null && outcome.getCreated()) {
			observationReported.setPatientReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			observationReported.setPatientReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return observationReported;
	}

	public VaccinationReported saveVaccinationReported(IGenericClient fhirClient, VaccinationReported vaccinationReported) {
		Immunization immunization = ImmunizationMapper.getFhirResource( null, vaccinationReported);
		MethodOutcome outcome = save(fhirClient,immunization,
			Immunization.IDENTIFIER.exactly()
				.systemAndIdentifier(MappingHelper.VACCINATION_REPORTED, vaccinationReported.getVaccinationReportedExternalLink())
		);
		if (outcome.getCreated()){
			vaccinationReported.setVaccinationReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}
	public VaccinationReported saveVaccinationReported(IGenericClient fhirClient, VaccinationMaster vaccinationMaster, VaccinationReported vaccinationReported) {
		Immunization immunization = ImmunizationMapper.getFhirResource( vaccinationMaster, vaccinationReported);
		MethodOutcome outcome = save(fhirClient,immunization,
			Immunization.IDENTIFIER.exactly()
				.systemAndIdentifier(MappingHelper.VACCINATION_REPORTED, vaccinationReported.getVaccinationReportedExternalLink())
			);
		if (outcome.getCreated() != null && outcome.getCreated()){
			vaccinationReported.setVaccinationReportedId(outcome.getId().getIdPart());
		} else if (!outcome.getResource().isEmpty()) {
			vaccinationReported.setVaccinationReportedId(outcome.getResource().getIdElement().getIdPart());
		}
		return vaccinationReported;
	}

	private MethodOutcome save(IGenericClient fhirClient, IBaseResource resource, ICriterion... where) {
		MethodOutcome outcome;
		try {
			IUpdateTyped query = fhirClient.update().resource(resource);
			int size = where.length;
			if (size > 0) {
				query = query.conditional().where(where[0]);
			}
			int i = 1;
			while (i < size) {
				query = ((IUpdateWithQuery) query).and(where[i]);
				i++;
			}
			outcome = query.execute();
		} catch (ResourceNotFoundException e){
			outcome = fhirClient.create().resource(resource)
				.execute();
		}
		return outcome;
	}

}
