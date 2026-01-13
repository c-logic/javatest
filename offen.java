import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.healthmarketscience.jackcess.*;
import com.healthmarketscience.jackcess.impl.RowImpl;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

@WebServlet("/offen")
public class offen extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public offen() {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		long start = new Date().getTime();
		String DBASE = getServletContext().getInitParameter("DBASE");
		if (DBASE == null)
			DBASE = "c:/temp/obserwer.mdb";

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		DateFormat formdate = new SimpleDateFormat("E ,dd.MM.yy");

		out.printf("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n"
				+ "<meta charset=\"UTF-8\"/>\r\n<title>%s</title>\r\n</head>\r\n",  escapeHtml(DBASE));
		out.println("<body bgcolor=\"white\">");
		/*
		 * KUNDE -AUFTRAG --PROJEKT
		 */
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.uuuu");
			String dateFrom = request.getParameter("from");
			String dateTo = request.getParameter("to");

			if (dateFrom != null && dateTo != null) {
				Date dfrom = Date.from(LocalDateTime.of(LocalDate.parse(dateFrom, formatter), LocalTime.of(0, 0, 0))
						.minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant());
				Date dto = Date.from(LocalDate.parse(dateTo, formatter).plusDays(1).atStartOfDay()
						.atZone(ZoneId.systemDefault()).toInstant());

				out.printf("Offene Posten <i>%s</i> < Termin < <i>%s</i><br/>", formdate.format(dfrom),
						formdate.format(dto));
				Database db = new DatabaseBuilder(new File(DBASE)).setReadOnly(true).open();
				// pksende<=NOW pkzugang>=1.1.2017 pkstatus!=FE pknr pkbez
				// pkkunr pkklass (pkinfo)
				Map<String, TreeMap<String, TreeMap<String, RowImpl>>> proj = new TreeMap<String, TreeMap<String, TreeMap<String, RowImpl>>>(); // Kunde,Auftrag,Projekt,Daten
				Map<String, TreeMap<String, RowImpl>> vorg = new HashMap<String, TreeMap<String, RowImpl>>(); //
				for (Row row : CursorBuilder.createCursor(db.getTable("projektkopf")).newIterable()) {//
					if (row.getDate("pkzugang") != null && (row.getDate("pkzugang").after(dfrom)
							&& row.getDate("pksende").after(dfrom) && row.getDate("pksende").before(dto)
							&& (row.getString("pkstatus") == null || (row.getString("pkstatus") != null
									&& !row.getString("pkstatus").equals("FE"))))) {
						String kdnr = row.getString("pkkunr"), kdauf = row.getString("pkkuaufnr"),
								kdproj = row.getString("pknr");

						if (!proj.containsKey(kdnr))
							proj.put(kdnr, new TreeMap<String, TreeMap<String, RowImpl>>());

						if (!proj.get(kdnr).containsKey(kdauf))
							proj.get(kdnr).put(kdauf, new TreeMap<String, RowImpl>());

						proj.get(kdnr).get(kdauf).put(kdproj, new RowImpl(row));
						vorg.put(kdproj, new TreeMap<String, RowImpl>());
					}
				}

				for (Row row : CursorBuilder.createCursor(db.getTable("projekt").getIndex("prnr")).newIterable()) {
					if (vorg.containsKey(row.getString("prnr"))) {
						vorg.get(row.getString("prnr")).put(row.getString("prvonr"), new RowImpl(row));
					}
				}

				long runningTime = new Date().getTime() - start;
				out.printf("Version 2 (<i>runtime %d ms</i>)<br/>\r\n", runningTime);
				out.println("<table border=1><tr><td>Status:</td><td bgcolor=\"yellow\">in Arbeit</td></tr></table>");
				out.println("<br/>");
				out.println("<table>");
				out.println("<tr><th colspan=7 align=\"left\"><a name=\"kndue\">&Uuml;bersicht</a></th></tr>");
				out.println(
						"<tr><th align=\"left\">Kunde</th><th align=\"center\">Vorg&auml;nge ohne<br/>Sollstunden</th><th align=\"center\">Vorg&auml;nge ohne Stempelung<br/>Wochenarbeitstage<br/>(bei 7,8 h pro Tag)</th><th>&nbsp;</th><th>Vorg&auml;nge</th><th>Projekte</th><th>Auftr&auml;ge</th></tr>\r\n");
				double szeitges=0;
				int projges=0;
				int aufges=0;
				int vorges=0;
				for (Map.Entry<String, TreeMap<String, TreeMap<String, RowImpl>>> knd : proj.entrySet()) {
					int projekte = 0, vorgaenge = 0, auftraege=0;
					double sollzeit = 0.;
					for (Map.Entry<String, TreeMap<String, RowImpl>> auf : knd.getValue().entrySet()) {
						auftraege++;
						aufges++;
						for (Map.Entry<String, RowImpl> prj : auf.getValue().entrySet()) {
							projekte++;
							projges++;
							for (Map.Entry<String, RowImpl> vg : vorg.get(prj.getValue().getString("pknr"))
									.entrySet()) {
								vorgaenge++;
								if(vg.getValue().getInt("prizeit")==0) {
									sollzeit += vg.getValue().getDouble("prvzeit");
									szeitges += vg.getValue().getDouble("prvzeit");
								}
							}
						}
					}
					
					out.printf(
							"<tr><td bgcolor=lightgreen><a href=\"#%s\">%s</a></td><td align=\"right\" bgcolor=lightgreen>%8.1f h</td><td align=\"right\" bgcolor=lightgreen>%8.1f d</td><td align=\"right\">&nbsp;</td><td align=\"right\" bgcolor=lightgreen>%4d</td><td align=\"right\" bgcolor=lightgreen>%4d</td><td align=\"right\" bgcolor=lightgreen>%4d</td></tr>\r\n",
							knd.getKey(), knd.getKey(),Math.round(sollzeit / 60.*10.)/10.,Math.round(((double)sollzeit) / 60. / 7.8*10.)/10.,vorgaenge,projekte,auftraege);
				}
				
				out.printf(
						"<tr><td>&nbsp;</td><td align=\"right\" bgcolor=cyan>%8.1f h</td><td align=\"right\" bgcolor=cyan>%8.1f d</td><td align=\"right\">&nbsp;</td><td align=\"right\" bgcolor=cyan>%4d</td><td align=\"right\" bgcolor=cyan>%4d</td><td align=\"right\" bgcolor=cyan>%4d</td></tr>\r\n",
						Math.round(szeitges/60.*10.)/10.,Math.round(((double)szeitges) / 60. / 7.8*10.)/10.,vorges,projges,aufges);
				out.println("</table>");
				out.println("<br/>");
				out.println("<table>");
				out.println(
						"<tr><th align=right>Kunde</th><th align=right>Projekt</th><th align=left>Termin</th><th align=left>Menge</th><th>Info</th></tr>\r\n");

				for (Map.Entry<String, TreeMap<String, TreeMap<String, RowImpl>>> knd : proj.entrySet()) {
					out.printf(
							"<tr bgcolor=lightblue><th align=left><a href=\"#prue\">%s</a></th><th colspan=5><a name=\"%s\">&nbsp;</a>&nbsp;</th></tr>\r\n",
							knd.getKey(), knd.getKey());

					for (Map.Entry<String, TreeMap<String, RowImpl>> auf : knd.getValue().entrySet()) {
						out.printf(
								"<tr bgcolor=lightblue><th>&nbsp;</th><th align=left colspan=4><i>Auftrag &bdquo;%s&rdquo;</i></th></tr>\r\n",
								escapeHtml(auf.getKey()));

						for (Map.Entry<String, RowImpl> prj : auf.getValue().entrySet()) {
							Row row = prj.getValue();
							out.printf(
									"<tr bgcolor=lightblue><td>&nbsp;</td><td align=right bgcolor=%s><a href=\"/app/projekt?projekt=%s\">%s</a></td><td align=right bgcolor=lightgray>%s</td><td align=right bgcolor=lightgray>%s</td><td bgcolor=lightgray>%s</td>\r\n",
									row.getString("pkstatus") == null ? "lightgray" : "yellow", row.getString("pknr"),
									row.getString("pknr"), formdate.format(row.getDate("pksende")),
									row.getInt("pkmenge"), escapeHtml(row.getString("pkbez")));

							boolean first = true;
							double sprvzeit = 0.;
							int sprizeit = 0;
							for (Map.Entry<String, RowImpl> vg : vorg.get(row.getString("pknr")).entrySet()) {
								if (first) {
									out.println(
											"<tr bgcolor=lightblue><td>&nbsp;</td><td align=right>Vorgang</td><td align=right>Sollzeit</td><td align=right>Istzeit</td><td>Info</td></tr>\r\n");
									first = false;
								}
								Row vgr = vg.getValue();
								out.printf(
										"<tr bgcolor=lightblue><td>&nbsp;</td><td align=right bgcolor=lightgray>%s</a></td><td align=right bgcolor=lightgray>%10.1f</td><td align=right bgcolor=lightgray>%d</td><td bgcolor=lightgray>%s</td>\r\n",
										vgr.getString("prvonr"), vgr.getDouble("prvzeit"), vgr.getInt("prizeit"),
										escapeHtml(vgr.getString("prbez")));
								sprvzeit += vgr.getDouble("prvzeit");
								sprizeit += vgr.getInt("prizeit");
							}
							if (!first) {
								out.printf(
										"<tr bgcolor=lightblue><td colspan=2>&nbsp;</td><td align=right bgcolor=gray>%10.1f</td><td align=right bgcolor=gray>%d</td><td bgcolor=gray>Summe</td></tr>",
										sprvzeit, sprizeit);
							}
							out.println("<tr><td colspan=5>&nbsp;</td></tr>");
						}
					}
				}
				out.println("</table>");
				db.close();
			} else {
				out.println("Keine Daten");
			}
		} catch (Exception e) {
			out.println("<div style=\"background-color:red;color:yellow;\">");
			out.println("Fehler" + "<br/>");
			out.println(e.getClass().getName() + "<br/>");// java.lang.NumberFormatException
			out.println(e.getMessage() + "<br/>"); // For input string: "19 %"
			out.println(e.toString() + "<br/>"); // java.lang.NumberFormatException:
			// For input string: "19 %"
			out.println("Kellerspeicherspur<br/>");
			StackTraceElement[] elements = e.getStackTrace();
			for (StackTraceElement element : elements)
				out.println("Klasse:" + element.getClassName() + " Methode:" + element.getMethodName() + " Zeile:"
						+ element.getLineNumber() + "<br/>");
			out.println("</div>");
		} finally {
			out.println("</body>\r\n</html>");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
