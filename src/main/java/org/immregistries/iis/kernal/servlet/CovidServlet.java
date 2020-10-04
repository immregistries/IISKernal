package org.immregistries.iis.kernal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.iis.kernal.model.OrgAccess;
import org.immregistries.iis.kernal.model.OrgMaster;
import org.immregistries.iis.kernal.model.PatientMaster;
import org.immregistries.iis.kernal.model.PatientReported;
import org.immregistries.iis.kernal.model.VaccinationReported;

@SuppressWarnings("serial")
public class CovidServlet extends HttpServlet {


  private static final String EXPORT_YYYY_MM_DD = "yyyy-MM-dd";

  public static final String ACTION_GENERATE = "Generate";

  public static final String PARAM_ACTION = "action";
  public static final String PARAM_DATE_START = "dateStart";
  public static final String PARAM_DATE_END = "dateEnd";
  public static final String PARAM_CVX_CODES = "cvxCodes";
  public static final String PARAM_INCLUDE_PHI = "includePhi";

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    HttpSession session = req.getSession(true);

    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());
    Session dataSession = PopServlet.getDataSession();
    OrgAccess orgAccess = (OrgAccess) session.getAttribute("orgAccess");
    if (orgAccess == null) {
      RequestDispatcher dispatcher = req.getRequestDispatcher("home");
      dispatcher.forward(req, resp);
      return;
    }

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      String action = req.getParameter(PARAM_ACTION);
      String messageError = null;
      String messageConfirmation = null;
      String dateStartString = req.getParameter(PARAM_DATE_START);
      String dateEndString = req.getParameter(PARAM_DATE_END);
      HomeServlet.doHeader(out, session);


      Date dateStart = null;
      Date dateEnd = null;
      if (dateStartString == null) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        dateStartString = sdf.format(calendar.getTime());
      } else {
        try {
          dateStart = sdf.parse(dateStartString);
        } catch (ParseException pe) {
          messageError = "Start date is unparsable";
        }
      }
      if (dateEndString == null) {
        dateEndString = sdf.format(new Date());
      } else {
        try {
          dateEnd = sdf.parse(dateEndString);
        } catch (ParseException pe) {
          messageError = "End date is unparsable";
        }
      }
      String cvxCodes = req.getParameter(PARAM_CVX_CODES);
      if (StringUtils.isEmpty(cvxCodes)) {
        cvxCodes = "900,901,902";
      }
      boolean includePhi = req.getParameter(PARAM_INCLUDE_PHI) != null;

      if (messageError != null) {
        out.println("  <div class=\"w3-panel w3-red\">");
        out.println("    <p>" + messageError + "</p>");
        out.println("  </div>");
      }
      out.println("    <div class=\"w3-container w3-card-4\">");
      out.println("    <h2>Convert Lab Message (ORU^R01) with SARS-CoV-2 Results to VXU</h2>");
      out.println("    <form method=\"POST\" action=\"covid\" class=\"w3-container w3-card-4\">");
      out.println("          <label>Start Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_START
          + "\" value=\"" + dateStartString + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_DATE_END
          + "\" value=\"" + dateEndString + "\"/>");
      out.println("          <label>End Date</label>");
      out.println("          <label>CVX Codes to Include</label>");
      out.println("          <input class=\"w3-input\" type=\"text\" name=\"" + PARAM_CVX_CODES
          + "\" value=\"" + cvxCodes + "\"/>");
      out.println("          <label>Include PHI</label>");
      out.println("          <input class=\"w3-input\" type=\"checkbox\" name=\""
          + PARAM_INCLUDE_PHI + "\" value=\"Y\"" + (includePhi ? " checked" : "") + "/>");
      out.println(
          "          <input class=\"w3-button w3-section w3-teal w3-ripple\" type=\"submit\" name=\""
              + PARAM_ACTION + "\" value=\"" + ACTION_GENERATE + "\"/>");
      out.println("    </form>");
      if (action != null) {
        if (action.equals(ACTION_GENERATE) && dateStart != null && dateEnd != null) {

          Set<String> cvxCodeSet = new HashSet<>();
          {
            String codes[] = cvxCodes.split("\\,");
            for (String c : codes) {
              if (StringUtils.isNotEmpty(c)) {
                cvxCodeSet.add(c);
              }
            }
          }
          out.print("<textarea cols=\"80\" rows=\"30\">");
          Query query = dataSession.createQuery(
              "from VaccinationReported where reportedDate >= :dateStart and reportedDate <= :dateEnd "
                  + "and patientReported.orgReported = :orgReported");
          query.setParameter("dateStart", dateStart);
          query.setParameter("dateEnd", dateEnd);
          query.setParameter("orgReported", orgAccess.getOrg());
          List<VaccinationReported> vaccinationReportedList = query.list();
          for (VaccinationReported vaccinationReported : vaccinationReportedList) {
            if (cvxCodeSet.contains(vaccinationReported.getVaccineCvxCode())) {
              printLine(out, vaccinationReported, includePhi);
            }
          }
          out.println("</textarea>");
        }
      }
      out.println("    </div>");

    } catch (Exception e) {
      System.err.println("Unable to render page: " + e.getMessage());
      e.printStackTrace(System.err);
    } finally {
      dataSession.close();
    }
    HomeServlet.doFooter(out, session);
    out.flush();
    out.close();
  }

  private void printLine(PrintWriter out, VaccinationReported vaccinationReported,
      boolean includePhi) {

    PatientReported patientReported = vaccinationReported.getPatientReported();
    PatientMaster patient = patientReported.getPatient();

    //    1    IIS recipient ID
    printField(patient.getPatientId(), out);
    //    2    Recipient name: first
    printField(patientReported.getPatientNameFirst(), includePhi, out);
    //    3    Recipient name: middle
    printField(patientReported.getPatientNameMiddle(), includePhi, out);
    //    4    Recipient name: last
    printField(patientReported.getPatientNameLast(), includePhi, out);
    //    5    Recipient date of birth
    printField(patientReported.getPatientBirthDate(), out);
    //    6    Recipient sex
    printField(patientReported.getPatientSex(), out);
    //    7    Recipient address: street
    printField(patientReported.getPatientAddressLine1(), includePhi, out);
    //    8    Recipient address: city
    printField(patientReported.getPatientAddressCity(), includePhi, out);
    //    9    Recipient address:  county
    printField(patientReported.getPatientAddressCountyParish(), out);
    //    10   Recipient address: state
    printField(patientReported.getPatientAddressState(), includePhi, out);
    //    11   Recipient address: zip code
    printField(patientReported.getPatientAddressZip(), out);
    //    12   Recipient race 1
    printField(patientReported.getPatientRace(), includePhi, out);
    //    13   Recipient race 2
    printField("", out);
    //    14   Recipient race 3
    printField("", out);
    //    15   Recipient race 4
    printField("", out);
    //    16   Recipient race 5
    printField("", out);
    //    17   Recipient race 6
    printField("", out);
    //    18   Recipient ethnicity
    printField(patientReported.getPatientEthnicity(), includePhi, out);
    //    19   IIS vaccination event ID
    printField(vaccinationReported.getVaccination().getVaccinationId(), out);
    //    20   Administration date
    printField(vaccinationReported.getAdministeredDate(), out);
    //    21   CVX (product)
    printField(vaccinationReported.getVaccineCvxCode(), out);
    //    22   MVX
    printField(vaccinationReported.getVaccineMvxCode(), out);
    //    23   Lot number: unit of use
    printField(vaccinationReported.getLotnumber(), out);
    //    24   Vaccine expiration date
    printField(vaccinationReported.getExpirationDate(), out);
    //    25   Vaccine administering site
    printField(vaccinationReported.getBodySite(), out);
    //    26   Vaccine route of administration
    printField(vaccinationReported.getBodyRoute(), out);
    //    27   Dose number
    printField("", out);
    //    28   Vaccination series complete
    printField("", out);
    //    29   Sending organization
    printField("", out);
    //    30   Administered at location
    printField("", out);
    //    31   Administered at location: type
    printField("", out);
    //    32   Administration address: street
    printField("", out);
    //    33   Administration address: city
    printField("", out);
    //    34   Administration address: county
    printField("", out);
    //    35   Administration address: state
    printField("", out);
    //    36   Administration address: zip code
    printField("", out);
    //    37   Vaccine administering provider suffix
    printField("", out);
    //    38   Vaccination refusal
    printField("", out);
    //    39   Comorbidity status
    printField("", out);
    //    40   Recipient missed vaccination appointment
    printField("No", out);
    //    41   Serology results 
    printField("", out);
    out.println();
  }

  private void printField(String s, PrintWriter out) {
    if (s != null) {
      out.print(s);
    }
    out.print("\t");
  }

  private void printField(String s, boolean includePhi, PrintWriter out) {
    if (s != null && includePhi) {
      out.print(s);
    }
    out.print("\t");
  }

  private void printField(Date d, boolean includePhi, PrintWriter out) {
    if (d != null && includePhi) {
      SimpleDateFormat sdf = new SimpleDateFormat(EXPORT_YYYY_MM_DD);
      out.print(sdf.format(d));
    }
    out.print("\t");
  }

  private void printField(Date d, PrintWriter out) {
    if (d != null) {
      SimpleDateFormat sdf = new SimpleDateFormat(EXPORT_YYYY_MM_DD);
      out.print(sdf.format(d));
    }
    out.print("\t");
  }

  private void printField(Integer i, PrintWriter out) {
    if (i != null) {
      out.print(i);
    }
    out.print("\t");
  }

  @SuppressWarnings("unchecked")
  public OrgAccess authenticateOrgAccess(String userId, String password, String facilityId,
      Session dataSession) {
    OrgMaster orgMaster = null;
    OrgAccess orgAccess = null;
    {
      Query query = dataSession.createQuery("from OrgMaster where organizationName = ?");
      query.setParameter(0, facilityId);
      List<OrgMaster> orgMasterList = query.list();
      if (orgMasterList.size() > 0) {
        orgMaster = orgMasterList.get(0);
      } else {
        orgMaster = new OrgMaster();
        orgMaster.setOrganizationName(facilityId);
        orgAccess = new OrgAccess();
        orgAccess.setOrg(orgMaster);
        orgAccess.setAccessName(userId);
        orgAccess.setAccessKey(password);
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(orgMaster);
        dataSession.save(orgAccess);
        transaction.commit();
      }

    }
    if (orgAccess == null) {
      orgAccess = authenticateOrgAccessForFacility(userId, password, dataSession, orgMaster);
    }
    return orgAccess;
  }

  public OrgAccess authenticateOrgAccessForFacility(String userId, String password,
      Session dataSession, OrgMaster orgMaster) {
    OrgAccess orgAccess = null;
    Query query = dataSession
        .createQuery("from OrgAccess where accessName = ? and accessKey = ? and org = ?");
    query.setParameter(0, userId);
    query.setParameter(1, password);
    query.setParameter(2, orgMaster);
    List<OrgAccess> orgAccessList = query.list();
    if (orgAccessList.size() != 0) {
      orgAccess = orgAccessList.get(0);
    }
    return orgAccess;
  }
}
