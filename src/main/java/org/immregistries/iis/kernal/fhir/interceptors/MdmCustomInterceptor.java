package org.immregistries.iis.kernal.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.mdm.dao.MdmLinkDaoSvc;
import ca.uhn.fhir.jpa.mdm.svc.MdmLinkSvcImpl;
import ca.uhn.fhir.jpa.mdm.svc.MdmResourceDaoSvc;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.TransactionLogMessages;
import ca.uhn.fhir.rest.server.messaging.ResourceOperationMessage;
import org.apache.commons.lang3.StringUtils;
import org.immregistries.iis.kernal.fhir.mdm.MdmCustomProvider;
import org.immregistries.iis.kernal.logic.SubscriptionService;
import ca.uhn.fhir.mdm.api.*;
import ca.uhn.fhir.mdm.model.MdmTransactionContext;
import ca.uhn.fhir.mdm.provider.MdmControllerHelper;
import ca.uhn.fhir.mdm.util.GoldenResourceHelper;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.mapping.forR5.ImmunizationMapperR5;
import org.immregistries.vaccination_deduplication.computation_classes.Deterministic;
import org.immregistries.vaccination_deduplication.reference.ComparisonResult;
import org.immregistries.vaccination_deduplication.reference.ImmunizationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;

import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_RECORD;
import static org.immregistries.iis.kernal.InternalClient.FhirRequester.GOLDEN_SYSTEM_TAG;

/**
 * Deprecated
 * replaced by MdmCustomMatchFinder
 */
public class MdmCustomInterceptor {
	Logger logger = LoggerFactory.getLogger(MdmCustomInterceptor.class);
	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	MdmLinkDaoSvc mdmLinkDaoSvc;

	@Autowired
	MdmResourceDaoSvc mdmResourceDaoSvc;
	@Autowired
	MdmLinkSvcImpl mdmLinkSvc;
	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;
	@Autowired
	private MdmControllerHelper myMdmControllerHelper;
	@Autowired
	private IMdmControllerSvc myMdmControllerSvc;
	@Autowired
	private IMdmSubmitSvc myMdmSubmitSvc;
	@Autowired
	private IMdmSettings myMdmSettings;
	@Autowired
	private GoldenResourceHelper myGoldenResourceHelper;
	@Autowired
	MdmCustomProvider mdmProvider;
	@Autowired
	IFhirResourceDao<Immunization> immunizationDao;

	@Autowired
	SubscriptionService subscriptionService;

	private void initialize() {
//		mdmProvider = new MdmCustomProvider(fhirSystemDao.getContext(), this.myMdmControllerSvc, this.myMdmControllerHelper, this.myMdmSubmitSvc, this.myMdmSettings);
	}

	@Hook(Pointcut.MDM_AFTER_PERSISTED_RESOURCE_CHECKED)
	public void test(ResourceOperationMessage resourceOperationMessage, TransactionLogMessages transactionLogMessages, MdmLinkEvent mdmLinkEvent) {
		logger.info("Pointcut test {}", Pointcut.MDM_AFTER_PERSISTED_RESOURCE_CHECKED);
		logger.info("ResourceOperationMessage {}", resourceOperationMessage.getPayloadString());
		logger.info("TransactionLogMessages {}", transactionLogMessages.getValues());
		logger.info("mdmLinkEvent {}", mdmLinkEvent.toString());
		// TODO check why the trigger is not working
	}

	@Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
	public void invoke(IBaseResource theResource, RequestDetails theRequestDetails) {
		initialize();
		if (theResource instanceof Immunization) {
			Immunization immunization = (Immunization) theResource;
			logger.info("Custom MDM applied for Immunization");

//			if(immunization.hasMeta() && immunization.getMeta().getTag(GOLDEN_SYSTEM_TAG,GOLDEN_RECORD) != null) { // if is golden record
//				return;
//			}
			if (immunization.getPatient() == null) {
				throw new InvalidRequestException("No patient specified");
			}
			Deterministic comparer = new Deterministic();
			ComparisonResult comparison;
			org.immregistries.vaccination_deduplication.Immunization i1 = toVaccDedupImmunization(immunization, theRequestDetails);
			org.immregistries.vaccination_deduplication.Immunization i2;

			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			ServletRequestDetails servletRequestDetails = new ServletRequestDetails();
//			servletRequestDetails.setServer((RestfulServer) theRequestDetails.getServer());
			servletRequestDetails.setServletRequest(request);
			servletRequestDetails.setTenantId(PartitionCreationInterceptor.extractPartitionName(theRequestDetails));
			MdmTransactionContext mdmTransactionContext = new MdmTransactionContext(MdmTransactionContext.OperationType.CREATE_RESOURCE);
			mdmTransactionContext.setResourceType("Immunization");

			IBundleProvider bundleProvider;
			SearchParameterMap searchParameterMap = new SearchParameterMap()
				.add("_tag", new TokenParam()
					.setSystem(GOLDEN_SYSTEM_TAG)
					.setValue(GOLDEN_RECORD));

			if (immunization.getPatient().getReference() != null) {
				searchParameterMap.add("patient", new ReferenceParam()
					.setMdmExpand(true) // Including other patients entities
					.setValue(immunization.getPatient().getReference()));
			} else if (immunization.getPatient().getIdentifier() != null) {
				searchParameterMap.add("identifier", new TokenParam()
					.setSystem(immunization.getPatient().getIdentifier().getSystem())
					.setValue(immunization.getPatient().getIdentifier().getValue()));
			} else {
				throw new InvalidRequestException("No patient specified");
			}
			bundleProvider = immunizationDao.search(searchParameterMap, theRequestDetails);
			logger.info("Potential matches found: {}", bundleProvider.sizeOrThrowNpe());

			boolean hasMatch = false;
			for (IBaseResource resource : bundleProvider.getAllResources()) {
				Immunization golden_i = (Immunization) resource;
				i2 = toVaccDedupImmunization(golden_i, theRequestDetails);
				comparison = comparer.compare(i1, i2);
				String matching_level = (golden_i.getPatient().equals(immunization.getPatient())) ? "MATCH" : "POSSIBLE_MATCH";
				logger.info("Matching level with Immunization {} : {}", golden_i.getId(), matching_level);

				if (comparison.equals(ComparisonResult.EQUAL)) {
					mdmProvider.createLink(
						new StringType("Immunization/" + golden_i.getId().split("Immunization/")[1]),
						new StringType("Immunization/" + immunization.getId().split("Immunization/")[1]),
						new StringType(matching_level),
						servletRequestDetails
					);
					hasMatch = true;
//					Subscription subscription = client.search().forResource(Subscription.class).where()
//					subscriptionService.triggerWithResource(subscription,golden_i);
					break;
				}
			}
			if (!hasMatch) {
				/**
				 * Create golden resource, currently made by mdm itself
				 */
//				IAnyResource golden = myGoldenResourceHelper.createGoldenResourceFromMdmSourceResource(immunization,mdmTransactionContext);
//				golden.setUserData(Constants.RESOURCE_PARTITION_ID, RequestPartitionId.fromPartitionName(PartitionCreationInterceptor.extractPartitionName(theRequestDetails)));
//				mdmLinkSvc.updateLink(golden,immunization,MdmMatchOutcome.NEW_GOLDEN_RESOURCE_MATCH,MdmLinkSourceEnum.MANUAL,mdmTransactionContext);
			}
		}
	}

	private org.immregistries.vaccination_deduplication.Immunization toVaccDedupImmunization(Immunization immunization, RequestDetails theRequestDetails){
		org.immregistries.vaccination_deduplication.Immunization i1 = new org.immregistries.vaccination_deduplication.Immunization();
		i1.setCVX(immunization.getVaccineCode().getCode(ImmunizationMapperR5.CVX));
		if(immunization.hasManufacturer()){
			i1.setMVX(immunization.getManufacturer().getReference().getIdentifier().getValue());
		}
		try {
			if (immunization.hasOccurrenceStringType()){
				i1.setDate(immunization.getOccurrenceStringType().getValue()); // TODO parse correctly
			} else if (immunization.hasOccurrenceDateTimeType()) {
				i1.setDate(immunization.getOccurrenceDateTimeType().getValue());
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		i1.setLotNumber(immunization.getLotNumber());

		if (immunization.getPrimarySource()) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else if (immunization.hasInformationSource()
			&& immunization.getInformationSource().getConcept() != null
			&& StringUtils.isNotBlank(immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR5.INFORMATION_SOURCE))
			&& immunization.getInformationSource().getConcept().getCode(ImmunizationMapperR5.INFORMATION_SOURCE).equals("00")) {
			i1.setSource(ImmunizationSource.SOURCE);
		} else {
			i1.setSource(ImmunizationSource.HISTORICAL);
		}

		if (immunization.hasInformationSource()) { // TODO improve organisation naming and designation among tenancy or in resource info
			if (immunization.getInformationSource().getReference() != null) {
				if (immunization.getInformationSource().getReference().getIdentifier() != null) {
					i1.setOrganisationID(immunization.getInformationSource().getReference().getIdentifier().getValue());
				} else if (immunization.getInformationSource().getReference().getReference() != null
					&& immunization.getInformationSource().getReference().getReference().startsWith("Organisation/")) {
					i1.setOrganisationID(immunization.getInformationSource().getReference().getReference()); // TODO get organisation name from db
				}
			}
		}
		if ((i1.getOrganisationID() == null || i1.getOrganisationID().isBlank()) && theRequestDetails != null) {
			i1.setOrganisationID(PartitionCreationInterceptor.extractPartitionName(theRequestDetails));
		}
		logger.info("Organisation id {}", i1);
		return i1;
	}

}
