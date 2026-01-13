import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.IndexCursor;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.impl.RowImpl;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

@WebServlet("/projekt")
public class projekt extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public projekt() {
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
		out.printf("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<meta charset=\"UTF-8\"/>\r\n<title>%s</title>\r\n</head>\r\n", escapeHtml(DBASE));
		out.println("<body bgcolor=\"white\">");

		String projekt = request.getParameter("projekt");

		if (projekt == null)
			out.println("<h5>keine Eingabe</h5>");
		else {
			try {
				Database db = new DatabaseBuilder(new File(DBASE)).setReadOnly(true).open();

				Map<String, RowImpl> pers = new TreeMap<String, RowImpl>();

				for (Row per : db.getTable("pers"))
					pers.put(per.getString("penr"), new RowImpl(per));

				IndexCursor cprojektkopf = CursorBuilder.createCursor(db.getTable("projektkopf").getPrimaryKeyIndex());

				if (cprojektkopf.findFirstRowByEntry(projekt)) {
					String aufnr = cprojektkopf.getCurrentRow().getString("pkkuaufnr");
					String kunde = cprojektkopf.getCurrentRow().getString("pkkunr");

					// Auftraege
					Map<String, RowImpl> proj = new TreeMap<String, RowImpl>();

					proj.put(cprojektkopf.getCurrentRow().getString("pknr"), new RowImpl(cprojektkopf.getCurrentRow()));

					for (Row row : cprojektkopf.newIterable().setMatchPattern("pkkuaufnr", aufnr)) {
						String pknr = row.getString("pknr");
						if (!pknr.equals(projekt) && row.getString("pkkunr").equals(kunde))
							proj.put(pknr, new RowImpl(row));
					}

					Map<String, TreeMap<String, RowImpl>> vorg = new HashMap<String, TreeMap<String, RowImpl>>();
					// Vorgaenge
					for (Row row : CursorBuilder.createCursor(db.getTable("projekt").getIndex("prnr")).newIterable())
						if (proj.containsKey(row.getString("prnr"))) {
							if (!vorg.containsKey(row.getString("prnr"))) {
								vorg.put(row.getString("prnr"), new TreeMap<String, RowImpl>());
							}
							// row.getString("prvonr")
							vorg.get(row.getString("prnr")).put(row.getString("prvonr"), new RowImpl(row));
						}

					Map<String, TreeMap<Date, RowImpl>> logaze = new HashMap<String, TreeMap<Date, RowImpl>>();
					// Vorgangszeiten
					for (Row row : CursorBuilder.createCursor(db.getTable("logaze").getIndex("Projekt")).newIterable())
						if (row.getString("laprnralt") != null && vorg.containsKey(row.getString("laprnralt"))
								&& row.getString("lamcode").equals("PROW") && row.getString("lagruppe").equals("GW")) {
							// Insert
							if (!logaze.containsKey(row.getString("lablgnralt"))) {
								logaze.put(row.getString("lablgnralt"), new TreeMap<Date, RowImpl>());
							}
							logaze.get(row.getString("lablgnralt")).put(row.getDate("lazdat"), new RowImpl(row));
						}

					out.println("<table>");
					out.printf("<tr><td colspan=4>Version 2.11 (<i>runtime %d ms</i>)</td></tr>\r\n",
							new Date().getTime() - start);

					out.println("<tr><th colspan=4 align=\"left\"><a name=\"prue\">&Uuml;bersicht</a></th></tr>");
					out.println(
							"<tr><th align=\"left\">Projekt</th><th align=\"left\">Info</th><th align=\"right\">Menge</th><th>Status</th></tr>");

					for (Map.Entry<String, RowImpl> prentry : proj.entrySet()) {
						out.printf("<tr><td><a href=\"#%s\">", prentry.getKey());
						if (prentry.getKey().equals(projekt))
							out.print("<b>");
						out.print(prentry.getKey());
						if (prentry.getKey().equals(projekt))
							out.print("</b>");
						out.printf("</a></td><td>%s</td><td align=\"right\">%d</td><td align=\"right\">",
								escapeHtml(prentry.getValue().getString("pkbez")),
								prentry.getValue().getInt("pkmenge"));
						if (prentry.getValue().getString("pkstatus") == null) {
							out.print("&nbsp");
						} else {
							if (prentry.getValue().getString("pkstatus").equalsIgnoreCase("FE"))
								out.print("fertig");
							else
								out.print("in Arbeit");
						}
					}

					out.println("<tr><td colspan=4>&nbsp;</td></tr>");

					Vector<String> projliste = new Vector<String>(proj.size());
					projliste.add(projekt);
					for (String s : proj.keySet())
						if (!s.equals(projekt))
							projliste.add(s);

					DateFormat formdate = new SimpleDateFormat("E,dd.MM.yy HH:mm:ss");
					DateFormat formdate2 = new SimpleDateFormat("E,dd.MM.yy");

					for (String s : projliste) {
						out.println("<tr><td colspan=4>&nbsp;</td></tr>");
						Row pr = proj.get(s);

						projekt = pr.getString("pknr");
						out.println("<tr><td bgcolor=");
						if (pr.getString("pkstatus") == null)
							out.print("Turquoise");
						else if (pr.getString("pkstatus").equalsIgnoreCase("FE"))
							out.print("lightgreen");
						else
							out.print("yellow");

						out.printf(
								"><a href=\"#prue\">Projekt</a></td><td bgcolor=moccasin>Kunde</td><td colspan=2 bgcolor=orange><a name=\"%s\">Auftrag</a></td>\r\n",
								projekt);
						out.println("</tr>\r\n<tr>");
						out.print("<td bgcolor=\"");

						if (pr.getString("pkstatus") == null)
							out.print("Turquoise");
						else if (pr.getString("pkstatus").equalsIgnoreCase("FE"))
							out.print("lightgreen");
						else
							out.print("yellow");

						out.printf("\">%s</td><td bgcolor=moccasin>%s</td><td colspan=2 bgcolor=orange>%s</td>\r\n",
								pr.getString("pknr"), pr.getString("pkkunr"), escapeHtml(pr.getString("pkkuaufnr")));
						out.println("</tr>\r\n<tr>");
						out.printf(
								"<td valign=top bgcolor=lightpink>Menge</td><td bgcolor=lightpink>%d</td><td bgcolor=lime>Termin</td><td bgcolor=lime>%s</td>\r\n",
								pr.getInt("pkmenge"), formdate2.format(pr.getDate("pksende")));
						out.println("</tr>\r\n<tr>");
						out.printf("<td bgcolor=lightgray>Bezeichnung</td><td colspan=3 bgcolor=WhiteSmoke>%s</td>\r\n",
								escapeHtml(pr.getString("pkbez")));
						out.println("</tr>\r\n<tr>");
						out.printf(
								"<td valign=top bgcolor=lightgray>Info</td><td colspan=3 bgcolor=WhiteSmoke>%s</td>\r\n",
								replacer("info",pr));
						out.println("</tr>");

						if (vorg.containsKey(projekt)) {
							for (Entry<String, RowImpl> row : vorg.get(projekt).entrySet()) {
								Row r = row.getValue();
								out.println("<tr>");
								out.printf("<td bgcolor=lightgreen>%s</td>\r\n", r.getString("prvonr"));
								out.printf("<td bgcolor=lightgreen>%s</td>\r\n", escapeHtml(r.getString("prbez")));
								out.printf("<td align=right bgcolor=yellow>%d min</td>\r\n", r.getInt("prizeit"));
								out.printf("<td align=right bgcolor=lightgreen>%6.2f min</td>\r\n",
										r.getDouble("prvzeit"));
								out.println("</tr>");

								if (logaze.containsKey(r.getString("prblgnr"))) {
									for (Entry<Date, RowImpl> r2 : logaze.get(r.getString("prblgnr")).entrySet()) {
										Row lp = r2.getValue();
										out.println("<tr>");
										out.printf(
												"<td bgcolor=lightyellow>&nbsp;</td><td bgcolor=lightyellow>%s</td><td bgcolor=yellow>%s %s</td>\r\n",
												formdate.format(lp.getDate("lazdat")),
												escapeHtml(pers.get(lp.getString("lapenr")).getString("pevname")),
												escapeHtml(pers.get(lp.getString("lapenr")).getString("penname")));
										out.printf("<td align=right bgcolor=yellow>%d min</td>\r\n",
												lp.getInt("laprdauer"));
										out.println("</tr>");
									}
								}
							}
						}
					}
					
					if (request.getParameter("debug") != null) {
						out.println("<table>");
						for (Map.Entry<String, RowImpl> pr1 : proj.entrySet()) {
							out.printf("<tr><td>projekt</td><td colspan=2>%s</td></tr>\r\n", pr1.getKey());
							for (Map.Entry<String, Object> pr2 : pr1.getValue().entrySet()) {
								out.printf("<tr><td>&nbsp;</td><td>%s</td>=<td>%s</td></tr>\r\n", pr2.getKey(),
										pr2.getValue() != null ? escapeHtml(pr2.getValue().toString()) : "null");
							}
						}
						out.println("</table>");
					}
				} else {
					out.println("<tr><td>Nichts gefunden</td></tr>");
				}
				db.close();
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
					out.println("Klasse:" + element.getClassName() + " Methode:" + element.getMethodName()
							+ " Zeile:" + element.getLineNumber() + "<br/>");
				out.println("</div>");
			}
			out.println("</table>");
		}

		out.println("</body>");
		out.println("</html>");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private String replacer(String s, Row pr) {
		String ret=pr.getString("pkinfo").replace("%AZ%",pr.getInt("pkmenge").toString()).replace("%PL%",pr.getString("pkkuaplnr"));
		return escapeHtml(ret).replace("\n", "<br/>").replace("\r", "");
		//
	}
}
