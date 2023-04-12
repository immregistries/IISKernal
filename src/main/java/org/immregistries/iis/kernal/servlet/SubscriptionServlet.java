package org.immregistries.iis.kernal.servlet;


import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.immregistries.iis.kernal.logic.SubscriptionService;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.repository.RepositoryClientFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Only Supported with Fhir R5
 */
public class SubscriptionServlet extends HttpServlet {
	@Autowired
	RepositoryClientFactory repositoryClientFactory;
	@Autowired
	SubscriptionService subscriptionService;

	public static final String PARAM_ACTION = "action";
	public static final String PARAM_MESSAGE = "message";
	public static final String PARAM_HTTP_VERB = "httpVerb";
	public static final String ACTION_SEARCH = "search";
	public static final String PARAM_SUBSCRIPTION_ENDPOINT = "endpoint";
	public static final String PARAM_SUBSCRIPTION_ID = "identifier";
	public static final String PARAM_FORM_LENGTH = "formLength";
	private static final String OPERATION_SAMPLE = "{\n" +
		"  \"resourceType\": \"OperationOutcome\",\n" +
		"  \"id\": \"generated\",\n" +
		"  \"issue\": [\n" +
		"    {\n" +
		"      \"severity\": \"information\",\n" +
		"      \"code\": \"informational\",\n" +
		"      \"location\": \"\",\n" +
		"      \"expression\": \"\",\n" +
		"      \"details\": {\n" +
		"        \"text\": \"Autogenerated Sample\"\n" +
		"      }\n" +
		"    }\n" +
		"  ]\n" +
		"}";

	/**
	 * used to manually trigger
	 *
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		// TODO action as manual trigger with content
		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}
		IGenericClient localClient = repositoryClientFactory.newGenericClient(session);


		String subscriptionId = req.getParameter(PARAM_SUBSCRIPTION_ID);

		resp.setContentType("text/html");
		PrintWriter out = new PrintWriter(resp.getOutputStream());
		HomeServlet.doHeader(out, session, "IIS Sandbox - SubscriptionsResult");

		try {
			Bundle searchBundle = localClient.search().forResource(Subscription.class)
				.where(Subscription.IDENTIFIER.exactly().identifier(subscriptionId)).returnBundle(Bundle.class).execute();
//			Subscription subscription = localClient.read().resource(Subscription.class).withId(subscriptionId).execute();

			if (searchBundle.hasEntry()) {
				String[] messages = req.getParameterValues(PARAM_MESSAGE);
				String[] httpVerbs = req.getParameterValues(PARAM_HTTP_VERB);
				if (messages.length == httpVerbs.length) {
					IParser parser;
					List<Pair<String, Bundle.HTTPVerb>> parsedResources = new ArrayList<>();
					for (int i = 0; i < messages.length; i++) {
						String message = messages[i];
						if (!message.isBlank()) {
							parsedResources.add(new MutablePair<>(message, Bundle.HTTPVerb.valueOf(httpVerbs[i])));
						}
					}

					Subscription subscription = (Subscription) searchBundle.getEntryFirstRep().getResource();
					String result = subscriptionService.triggerWithResource(subscription, parsedResources);
					out.println(result);
				} else {
					out.println("Incorrect parameters length");
				}

			} else {
				out.println("NO SUBSCRIPTION FOUND FOR THIS IDENTIFIER");
			}
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace(out);
			e.printStackTrace(System.err);
			// TODO add exception handling interceptor
		}
		out.flush();
		out.close();
//		doGet(req, resp);
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {

		HttpSession session = req.getSession(true);
		OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
		if (orgAccess == null) {
			RequestDispatcher dispatcher = req.getRequestDispatcher("home");
			dispatcher.forward(req, resp);
			return;
		}
		resp.setContentType("text/html");
		IGenericClient fhirClient = repositoryClientFactory.newGenericClient(session);
		PrintWriter out = new PrintWriter(resp.getOutputStream());

		String subscriptionId = req.getParameter(PARAM_SUBSCRIPTION_ID);
		if (subscriptionId == null) {
			printSearchAndSelect(req,resp,out,fhirClient);
		} else {
			printTools(req,resp,out,fhirClient, subscriptionId);
		}


		out.flush();
		out.close();
	}

	private void printTools(HttpServletRequest req, HttpServletResponse resp,
									PrintWriter out,IGenericClient fhirClient, String subscriptionId) {
		HttpSession session = req.getSession(true);
		try {
			HomeServlet.doHeader(out, session, "IIS Sandbox - Subscriptions");
			ServletInputStream servletInputStream = req.getInputStream();

			String[] initialMessages = new String[]{OPERATION_SAMPLE};

			if (servletInputStream.isReady() && !servletInputStream.isFinished()) {
				initialMessages[0] = new String(servletInputStream.readAllBytes()); // TODO check array
			} else if (req.getParameter(PARAM_MESSAGE) != null) {
				initialMessages = req.getParameterValues(PARAM_MESSAGE);
			}

			int initialMessageLength = initialMessages.length;
			int formLength = initialMessageLength;
			if (req.getParameter(PARAM_FORM_LENGTH) != null) {
				formLength = Math.max(Integer.parseInt(req.getParameter(PARAM_FORM_LENGTH)), initialMessages.length);
			}


			Subscription subscription;
			Bundle bundle = fhirClient.search().forResource(Subscription.class)
				.where(Subscription.IDENTIFIER.exactly().code(subscriptionId)).returnBundle(Bundle.class).execute();
			if (bundle.hasEntry()) {
				subscription = (Subscription) bundle.getEntryFirstRep().getResource();
				printSubscription(out, subscription);
				out.println("<form action=\"" + req.getRequestURI() + "\" method=\"Get\" target=\"_blank\">");
				out.println("<input type=\"hidden\" name=\"" + PARAM_FORM_LENGTH + "\" value=\"" + (formLength + 1) + "\"/>");
				for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
					for (String value : param.getValue()) {
						if (!param.getKey().equals(PARAM_FORM_LENGTH) && !param.getKey().equals("submit")) {
							out.println("<input type=\"hidden\" name=\"" + param.getKey() + "\" value=\"" + value.strip() + "\"/>");
						}
					}
				}
				out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Open tab with extra slot\"/>");
				out.println("</form>");

				out.println("<form action=\"subscription?identifier=" + subscription.getIdentifierFirstRep().getValue() + "\" method=\"POST\" target=\"_blank\">");
				for (int i = 0; i < formLength; i++) {
					out.println("<div class=\"w3-half\">");
					out.println("<select class=\"w3-select w3-border\" name=\"" + PARAM_HTTP_VERB + "\">" +
						"<option value=\"PUT\" selected>PUT</option>" +
						"<option value=\"POST\">POST</option>" +
						"<option value=\"DELETE\">DELETE<option>" +
						"</select>");
					out.println("<textarea class=\"w3-input w3-border\" name=\"" + PARAM_MESSAGE + "\"rows=\"15\" cols=\"160\">");
					if (initialMessageLength > i) {
						out.println(initialMessages[i]);
					}
					out.println("</textarea>");
					out.println("</div>");
				}
				out.println("<div class=\"w3-threequarter\"><h4>Send FHIR Resource to subscriber</h4>");
				out.println("	<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\"submit\" value=\"Submit\"/>");
				out.println("</div>");
				out.println("</form>");


			} else {
				out.println("<div class=\"w3-panel w3-yellow\"><p>Not Found</p></div>");
			}
			HomeServlet.doFooter(out,session);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	public static void printSearchAndSelect(HttpServletRequest req, HttpServletResponse resp,
														 PrintWriter out, IGenericClient fhirClient) {
		HttpSession session = req.getSession(true);
		try {
			Bundle bundle;

			String endpoint = req.getParameter(PARAM_SUBSCRIPTION_ENDPOINT) == null ? "" : req.getParameter(PARAM_SUBSCRIPTION_ENDPOINT);
			String action = req.getParameter(PARAM_ACTION);
			if (action != null) {
				if (action.equals(ACTION_SEARCH)) {
					bundle = fhirClient.search().forResource(Subscription.class)
						.where(Subscription.URL.matches().value(endpoint)).returnBundle(Bundle.class).execute();
				} else {
					bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
				}
			} else {
				bundle = fhirClient.search().forResource(Subscription.class).returnBundle(Bundle.class).execute();
			}

			HomeServlet.doHeader(out, session, "IIS Sandbox - Subscriptions");

			out.println("    <div class=\"w3-container w3-half w3-margin-top\">");
			out.println("    <h3>Search Subscription</h3>");
			out.println("	  <form method=\"GET\" action=\"subscription\" class=\"w3-container w3-card-4\">");
			out.println("      <input class=\"w3-input\" type=\"text\" name=\""
				+ PARAM_SUBSCRIPTION_ENDPOINT + "\" value=\"" + endpoint + "\"/>");
			out.println("      <label>ENDPOINT</label>");
			out.println("<input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
				+ PARAM_ACTION + "\" value=\"" + ACTION_SEARCH + "\"/>");
			out.println("    </form>");
			out.println("    </div>");

			out.println("  <div class=\"w3-container\">");

			if (bundle.hasEntry()) {
				out.println(
					"<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
				out.println("  <tr class=\"w3-green\">");
				out.println("    <th>Name</th>");
				out.println("    <th>Endpoint</th>");
				out.println("    <th>Status</th>");
				out.println("  </tr>");
				out.println("  <tbody>");
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				int count = 0;
				for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
					Subscription subscription = (Subscription) entry.getResource();
					count++;
					if (count > 100) {
						break;
					}
					String link = "subscription?" + PARAM_SUBSCRIPTION_ID + "="
						+ subscription.getIdentifierFirstRep().getValue(); // TODO or id
					out.println("  <tr>");
					out.println("    <td><a href=\"" + link + "\">"
						+ subscription.getName() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">"
						+ subscription.getEndpoint() + "</a></td>");
					out.println("    <td><a href=\"" + link + "\">"
						+ subscription.getStatus() + "</a></td>");
					out.println("  </tr>");
				}
				out.println("  </tbody>");
				out.println("</table></div>");

				if (count > 100) {
					out.println("<em>Only the first 100 are shown</em>");
				}
			} else {
				out.println("<div class=\"w3-panel w3-yellow\"><p>No Records Found</p></div>");
			}
			HomeServlet.doFooter(out,session);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

	}

	public void printSubscription(PrintWriter out, Subscription subscription) {
		SimpleDateFormat sdfDate = new SimpleDateFormat("MM/dd/yyyy");
		out.println("    <div class=\"w3-container w3-half w3-margin\">");
		out.println("<table class=\"w3-table w3-bordered w3-striped w3-border test w3-hoverable\">");
		out.println("  <tbody>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Name</th>");
		out.println("    <td>" + subscription.getName() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Identifier</th>");
		out.println("    <td>" + subscription.getIdentifierFirstRep().getValue() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Topic</th>");
		out.println("    <td>" + subscription.getTopicElement().getValue() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Endpoint</th>");
		out.println("    <td>" + subscription.getEndpoint() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Status</th>");
		out.println("    <td>" + subscription.getStatus() + "</td>");
		out.println("  </tr>");
		out.println("  <tr>");
		out.println("    <th class=\"w3-green\">Content Type</th>");
		out.println("    <td>" + subscription.getContentType()+ "</td>");
		out.println("  </tr>");
		out.println("  </tbody>");
		out.println("</table>");
		out.println("</div>");
	}

}
