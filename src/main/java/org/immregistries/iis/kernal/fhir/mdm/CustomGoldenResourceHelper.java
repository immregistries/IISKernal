package org.immregistries.iis.kernal.fhir.mdm;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.api.IMdmSurvivorshipService;
import ca.uhn.fhir.mdm.log.Logs;
import ca.uhn.fhir.mdm.model.CanonicalEID;
import ca.uhn.fhir.mdm.model.MdmTransactionContext;
import ca.uhn.fhir.mdm.util.*;
import ca.uhn.fhir.rest.api.Constants;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static ca.uhn.fhir.context.FhirVersionEnum.R4;
import static ca.uhn.fhir.context.FhirVersionEnum.R5;

/**
 * Custom Made for Accepting R5 and adding any original identifier to golden identifiers
 */
public class CustomGoldenResourceHelper extends GoldenResourceHelper {

	private static final Logger ourLog = Logs.getMdmTroubleshootingLog();

	static final String FIELD_NAME_IDENTIFIER = "identifier";

	@Autowired
	private IMdmSettings myMdmSettings;

	@Autowired
	private EIDHelper myEIDHelper;

	@Autowired
	private IMdmSurvivorshipService myMdmSurvivorshipService;

	@Autowired
	private MdmPartitionHelper myMdmPartitionHelper;

	private final FhirContext myFhirContext;

	@Autowired
	public CustomGoldenResourceHelper(FhirContext theFhirContext) {
		super(theFhirContext);
		myFhirContext = theFhirContext;
	}

	/**
	 * Creates a copy of the specified resource. This method will carry over resource EID if it exists. If it does not exist,
	 * a randomly generated UUID EID will be created.
	 *
	 * @param <T>                      Supported MDM resource type (e.g. Patient, Practitioner)
	 * @param theIncomingResource      The resource that will be used as the starting point for the MDM linking.
	 * @param theMdmTransactionContext
	 */
	@Nonnull
	public <T extends IAnyResource> T createGoldenResourceFromMdmSourceResource(
		T theIncomingResource, MdmTransactionContext theMdmTransactionContext) {
		validateContextSupported();

		// get a ref to the actual ID Field
		RuntimeResourceDefinition resourceDefinition = myFhirContext.getResourceDefinition(theIncomingResource);
		IBaseResource newGoldenResource = resourceDefinition.newInstance();

		myMdmSurvivorshipService.applySurvivorshipRulesToGoldenResource(
			theIncomingResource, newGoldenResource, theMdmTransactionContext);

		// hapi has 2 metamodels: for children and types
		BaseRuntimeChildDefinition goldenResourceIdentifier = resourceDefinition.getChildByName(FIELD_NAME_IDENTIFIER);

		cloneMDMEidsIntoNewGoldenResource(goldenResourceIdentifier, theIncomingResource, newGoldenResource);

		addHapiEidIfNoExternalEidIsPresent(newGoldenResource, goldenResourceIdentifier, theIncomingResource);

		MdmResourceUtil.setMdmManaged(newGoldenResource);
		MdmResourceUtil.setGoldenResource(newGoldenResource);

		// add the partition id to the new resource
		newGoldenResource.setUserData(
			Constants.RESOURCE_PARTITION_ID,
			myMdmPartitionHelper.getRequestPartitionIdForNewGoldenResources(theIncomingResource));

		return (T) newGoldenResource;
	}

	/**
	 * If there are no external EIDs on the incoming resource, create a new HAPI EID on the new Golden Resource.
	 */
	// TODO GGG ask james if there is any way we can convert this canonical EID into a generic STU-agnostic IBase.
	private <T extends IAnyResource> void addHapiEidIfNoExternalEidIsPresent(
		IBaseResource theNewGoldenResource,
		BaseRuntimeChildDefinition theGoldenResourceIdentifier,
		IAnyResource theSourceResource) {

		List<CanonicalEID> eidsToApply = myEIDHelper.getExternalEid(theNewGoldenResource);
		if (!eidsToApply.isEmpty()) {
			return;
		}
//		if(Objects.equals(myFhirContext.getResourceType(theSourceResource), "Patient")) {
//			if (myFhirContext.getVersion().equals(R5)) {
//				org.hl7.fhir.r5.model.Patient patient = (org.hl7.fhir.r5.model.Patient) theSourceResource;
//				for (org.hl7.fhir.r5.model.Identifier identifier: patient.getIdentifier()) {
////					CanonicalIdentifier canonicalIdentifier = IdentifierUtil.identifierDtFromIdentifier(identifier);
//					RuntimeResourceDefinition resourceDefinition = myFhirContext.getResourceDefinition(theSourceResource);
//					BaseRuntimeChildDefinition resourceIdentifier = resourceDefinition.getChildByName("identifier");
//					TerserUtil.cloneEidIntoResource(myFhirContext, resourceIdentifier, identifier,theSourceResource);
////					cloneEidIntoResource(the);
//
//				}
//
//			} else if (myFhirContext.getVersion().equals(R4)) {
//				org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) theSourceResource;
//
//			}
//		}

		CanonicalEID hapiEid = myEIDHelper.createHapiEid();
		theGoldenResourceIdentifier
			.getMutator()
			.addValue(theNewGoldenResource, IdentifierUtil.toId(myFhirContext, hapiEid));

		// set identifier on the source resource
		cloneEidIntoResource(myFhirContext, theSourceResource, hapiEid);
	}


	/**
	 * TODO Here is experimental IIS Sandbox functionality adding all identifiers
	 *
	 */
	//
	private void cloneMDMEidsIntoNewGoldenResource(
		BaseRuntimeChildDefinition theGoldenResourceIdentifier,
		IAnyResource theIncomingResource,
		IBase theNewGoldenResource) {
		String incomingResourceType = myFhirContext.getResourceType(theIncomingResource);
		String mdmEIDSystem = myMdmSettings.getMdmRules().getEnterpriseEIDSystemForResourceType(incomingResourceType);

//		if (mdmEIDSystem == null) {
//			return;
//		}

		// FHIR choice types - fields within fhir where we have a choice of ids
		IFhirPath fhirPath = myFhirContext.newFhirPath();
		List<IBase> incomingResourceIdentifiers =
			theGoldenResourceIdentifier.getAccessor().getValues(theIncomingResource);

		for (IBase incomingResourceIdentifier : incomingResourceIdentifiers) {
			Optional<IPrimitiveType> incomingIdentifierSystem =
				fhirPath.evaluateFirst(incomingResourceIdentifier, "system", IPrimitiveType.class);
			if (incomingIdentifierSystem.isPresent()) {
				String incomingIdentifierSystemString =
					incomingIdentifierSystem.get().getValueAsString();
				/**
				 * Experimental
				 */
				if (true) {
//				if (Objects.equals(incomingIdentifierSystemString, mdmEIDSystem)) {
					ourLog.debug(
						"Incoming resource EID System {} matches EID system in the MDM rules.  Copying to Golden Resource.",
						incomingIdentifierSystemString);
					ca.uhn.fhir.util.TerserUtil.cloneEidIntoResource(
						myFhirContext,
						theGoldenResourceIdentifier,
						incomingResourceIdentifier,
						theNewGoldenResource);
				} else {
					ourLog.debug(
						"Incoming resource EID System {} differs from EID system in the MDM rules {}.  Not copying to Golden Resource.",
						incomingIdentifierSystemString,
						mdmEIDSystem);
				}
			} else {
				ourLog.debug("No EID System in incoming resource.");
			}
		}
	}

	private void validateContextSupported() {
		FhirVersionEnum fhirVersion = myFhirContext.getVersion().getVersion();
		if (fhirVersion == R5 || fhirVersion == R4) {
			return;
		}
		throw new UnsupportedOperationException(Msg.code(1489) + "Version not supported: "
			+ myFhirContext.getVersion().getVersion());
	}

}
