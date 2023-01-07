package ca.uhn.fhir.jpa.starter.BulkQuery;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.provider.BaseJpaResourceProviderPatient;
import ca.uhn.fhir.jpa.rp.r5.GroupResourceProvider;
import ca.uhn.fhir.jpa.starter.annotations.OnR5Condition;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hibernate.Session;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.model.MessageReceived;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.servlet.PopServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;


@Controller
@Conditional(OnR5Condition.class)
public class BulkQueryGroupProviderR5 extends GroupResourceProvider {
	Logger logger = LoggerFactory.getLogger(BulkQueryGroupProviderR5.class);

	@Autowired
	BaseJpaResourceProviderPatient<Patient> patientProvider;

	@Autowired
	IFhirSystemDao fhirSystemDao;
	@Autowired
	IFhirResourceDao<Group> fhirResourceGroupDao;

	public BulkQueryGroupProviderR5() {
		super();
		setDao(fhirResourceGroupDao);
	}

	/**
	 * Group/123/$export
	 */
	@Operation(name = JpaConstants.OPERATION_EXPORT
		, idempotent = true, manualResponse = true
	)
	public void groupInstanceExport(

		javax.servlet.http.HttpServletRequest theServletRequest,

		@IdParam
			IdType theId,

		@Description(formalDefinition = "The format for the requested Bulk Data files to be generated as per FHIR Asynchronous Request Pattern. Defaults to application/fhir+ndjson. The server SHALL support Newline Delimited JSON, but MAY choose to support additional output formats. The server SHALL accept the full content type of application/fhir+ndjson as well as the abbreviated representations application/ndjson and ndjson.")
		@OperationParam(name = "_outputFormat")
			UnsignedIntType theOutputFormat,

		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
		@OperationParam(name = Constants.PARAM_COUNT, typeName = "unsignedInt")
			IPrimitiveType<Integer> theCount,

		@Description(shortDefinition = "Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
		@OperationParam(name = Constants.PARAM_OFFSET, typeName = "unsignedInt")
			IPrimitiveType<Integer> theOffset,

		@Description(shortDefinition = "Only return resources which were last updated as specified by the given range")
		@OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1)
			DateRangeParam theLastUpdated,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _content filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_CONTENT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
			List<IPrimitiveType<String>> theContent,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _text filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TEXT, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
			List<IPrimitiveType<String>> theNarrative,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _filter filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_FILTER, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
			List<IPrimitiveType<String>> theFilter,

		@Description(shortDefinition = "Filter the resources to return only resources matching the given _type filter (note that this filter is applied only to results which link to the given patient, not to the patient itself or to supporting resources linked to by the matched resources)")
		@OperationParam(name = Constants.PARAM_TYPE, min = 0, max = OperationParam.MAX_UNLIMITED, typeName = "string")
			List<IPrimitiveType<String>> theTypes,

		@Sort
			SortSpec theSortSpec,

		ServletRequestDetails theRequestDetails
	) throws IOException {
		Session dataSession = PopServlet.getDataSession();
		try {
			Bundle bundle = new Bundle();
			Group group = read(theServletRequest, theId, theRequestDetails);
			if (theOutputFormat == null) {
//			theRequestDetails.se
			}
			IParser parser = fhirResourceGroupDao.getContext().newNDJsonParser();
			for (Group.GroupMemberComponent member : group.getMember()) {
				if (member.getEntity().getReference().split("/")[0].equals("Patient")) {
					Bundle patientBundle = new Bundle();
					IBundleProvider bundleProvider = patientProvider.patientInstanceEverything(theServletRequest, new IdType(member.getEntity().getReference()), theCount, theOffset, theLastUpdated, theContent, theNarrative, theFilter, theTypes, theSortSpec, theRequestDetails);
					for (IBaseResource resource : bundleProvider.getAllResources()) {
						patientBundle.addEntry().setResource((Resource) resource);
					}

					bundle.addEntry().setResource(patientBundle);
				}
			}

			MessageReceived ndJson = new MessageReceived();
			OrgAccess orgAccess = (OrgAccess) theRequestDetails.getAttribute("orgAccess");

			ndJson.setOrgMaster(orgAccess.getOrg());
			ndJson.setMessageResponse(parser.encodeResourceToString(bundle));
			ndJson.setReportedDate(new Date());
			int id = (int) dataSession.save(ndJson);
			logger.info("{}/ndjson?tenantId={}&ndJsonId={}", theRequestDetails.getCompleteUrl().split("/fhir")[0], theRequestDetails.getTenantId(), id);
//			return parser.encodeResourceToString(bundle);

//			parser.encodeResourceToWriter(bundle,theRequestDetails.getServletResponse().getWriter());
			theRequestDetails.getServletResponse().getWriter().print(ndJson.getMessageResponse());
			theRequestDetails.getServletResponse().getWriter().close();
		} catch (Exception e) {
			throw e;
		} finally {
			dataSession.close();
		}
	}
}