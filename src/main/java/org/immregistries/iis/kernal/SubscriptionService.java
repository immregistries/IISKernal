package org.immregistries.iis.kernal;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.subscription.match.deliver.resthook.SubscriptionDeliveringRestHookSubscriber;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionCanonicalizer;
import ca.uhn.fhir.jpa.subscription.model.ResourceDeliveryMessage;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.server.messaging.BaseResourceMessage;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.immregistries.iis.kernal.servlet.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;


@Service
public class SubscriptionService {
	Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	SubscriptionDeliveringRestHookSubscriber subscriptionDeliveringRestHookSubscriber;
	@Autowired
	SubscriptionCanonicalizer subscriptionCanonicalizer;

	public Subscription searchRelatedSubscription(Immunization baseResource, RequestDetails requestDetails) {
		OrgAccess orgAccess = (OrgAccess) requestDetails.getAttribute("orgAccess");
		/**
		 * define materialization of subscription on immunizaton with
		 * 	- TAG ?
		 * 	- Identifier System ?
		 * 	- $match operations ?
		 */
		Bundle bundle = repositoryClientFactory.newGenericClient(orgAccess).search().forResource(Subscription.class)
			.where(Subscription.STATUS.exactly().code(Enumerations.SubscriptionStatusCodes.ACTIVE.toCode()))
//			.and(Subscription.IDENTIFIER.hasSystemWithAnyCode(baseResource.getIdentifier()))  TODO change
//			.and(Subscription.)  TODO change
			.returnBundle(Bundle.class).execute();
		Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();
//		subscription.gets
		return subscription;
	}

	public String triggerWithResource(Subscription subscription, IBaseResource resource) {
		try {
			OrgAccess orgAccess = ServletHelper.getOrgAccess();

//			ResourceDeliveryMessage resourceDeliveryMessage = new ResourceDeliveryMessage();
//			resourceDeliveryMessage.setSubscription(subscriptionCanonicalizer.canonicalize(subscription));
//			resourceDeliveryMessage.setPartitionId(RequestPartitionId.fromPartitionName(""+orgAccess.getAccessName()));
//			resourceDeliveryMessage.setOperationType(BaseResourceMessage.OperationTypeEnum.UPDATE);
//			resourceDeliveryMessage.setPayload(fhirSystemDao.getContext(), resource, EncodingEnum.JSON);
//			subscriptionDeliveringRestHookSubscriber.handleMessage(resourceDeliveryMessage);

			IGenericClient endpointClient = repositoryClientFactory.newGenericClient(subscription.getEndpoint());
			/**
			 * Adding headers for security requirements
			 */
			AdditionalRequestHeadersInterceptor additionalRequestHeadersInterceptor = new AdditionalRequestHeadersInterceptor();
			for (StringType header : subscription.getHeader()) {
				String[] headerSplit = header.getValue().split(":");
				logger.info("Subscription header is : {}", header.getValue());
				additionalRequestHeadersInterceptor.addHeaderValue(headerSplit[0], headerSplit[1]);
			}
			endpointClient.registerInterceptor(additionalRequestHeadersInterceptor);
			Bundle notificationBundle = new Bundle(Bundle.BundleType.SUBSCRIPTIONNOTIFICATION);
			SubscriptionStatus status = new SubscriptionStatus()
				.setType(SubscriptionStatus.SubscriptionNotificationType.EVENTNOTIFICATION)
				.setStatus(subscription.getStatus())
//				.setSubscription(subscription.getIdentifierFirstRep().getAssigner())
				.setSubscription(new Reference(subscription.getId()))
//				.set
//				.setEventsInNotification(1)
				.setEventsSinceSubscriptionStart(1)
				.setTopic(subscription.getTopic());
			notificationBundle.addEntry().setResource(status);
			notificationBundle.addEntry().setResource((Resource) resource);
			MethodOutcome outcome = endpointClient.create().resource(notificationBundle).execute();
			if (outcome.getResource() != null) {
//				out.println(parser.encodeResourceToString(outcome.getResource()));
			}
			if (outcome.getOperationOutcome() != null) {
//				out.println(parser.encodeResourceToString(outcome.getOperationOutcome()));
			}
			if (outcome.getId() != null) {
//				out.println(outcome.getId());
			}

//				MethodOutcome methodOutcome = localClient.create().resource(parsedResource).execute();
//				List<IPrimitiveType<String>> ids = new ArrayList<>();
//				ids.add(methodOutcome.getId());
//				List<IPrimitiveType<String>> urls = new ArrayList<>();
//				urls.add(new StringType("OperationOutcome?"));
//				subscriptionTriggeringProvider.triggerSubscription(new IdType(subscription.getId()),ids,urls);


			return "Sent";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}

	}
}
