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
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.impl.RowImpl;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

@WebServlet("/close")
public class close extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public close() {
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
		out.printf("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<meta charset=\"UTF-8\"/>\r\n<title>%s</title>\r\n</head>\r\n",  escapeHtml(DBASE));
		out.println("<body bgcolor=\"white\">");

		
		String projekt=request.getParameter("projekt");
		String user=request.getParameter("user");
		String pass=request.getParameter("pass");
		String[] prfe=request.getParameterValues("p");
		
		if (projekt == null && prfe==null)
			out.println("<h5>keine Eingabe</h5>");
		else {
			boolean minone=false;

			try {
				if(prfe != null && user !=null && pass != null) {
					Date dt=new Date();
					Database db = new DatabaseBuilder(new File(DBASE)).setReadOnly(true).open();
					IndexCursor cprojektkopf = CursorBuilder.createCursor(db.getTable("projektkopf").getPrimaryKeyIndex());
					Vector <String> proj=new Vector<String>();
					projekt=prfe[0];
					for(String s:prfe) {
						proj.add(s);
					}
					
					if (cprojektkopf.findFirstRowByEntry(projekt)) {
						String aufnr = cprojektkopf.getCurrentRow().getString("pkkuaufnr");
						String kunde = cprojektkopf.getCurrentRow().getString("pkkunr");

						for(Row row: cprojektkopf.newIterable().setMatchPattern("pkkuaufnr", aufnr)) {
							String pknr = row.getString("pknr");
							if(!row.getString("pkkunr").equals(kunde)) proj.remove(pknr);
						}
					}

					boolean passtest=false;
					for(Row row: CursorBuilder.createCursor(db.getTable("Benutzer")).newIterable()) {
						if(row.getString("bekz").equalsIgnoreCase(user)) {
							String p=row.getString("bepasswd");
							if(p.length()==pass.length()) {
								byte[] coded=p.getBytes(), uncoded=pass.getBytes();
								passtest=true;
								for(int x=0;x<coded.length;x++) {
									if((coded[x]^=0x19)!=uncoded[x]) {
										passtest=false;
										break;
									};
								}
							}
							break;
						}
					}
					
					db.close();
//					out.println("CloseList<br/>");
					
					if(!proj.isEmpty() && passtest) {
						db = new DatabaseBuilder(new File(DBASE)).open();
						
//						out.println("projektkopf<br/>");
						Table pktab=db.getTable("projektkopf");
						for(Row row: CursorBuilder.createCursor(pktab.getPrimaryKeyIndex()).newIterable()) {
							if(proj.contains(row.getString("pknr"))) {
								row.put("pkstatus","FE");
								row.put("pkiende",dt);
								pktab.updateRow(row);
//								out.printf("Close %s<br/>\r\n",row.getString("pknr"));
							}
						}
						
						Table prtab=db.getTable("projekt");
//						out.println("projekt<br/>");
						for(Row row: CursorBuilder.createCursor(prtab.getIndex("prnr")).newIterable()) {
							if(proj.contains(row.getString("prnr"))) {
								row.put("prstatus","FE");
								row.put("pristende",dt);
								if(row.getDate("pristanfang")==null)row.put("pristanfang",dt);
								prtab.updateRow(row);
//								out.printf("Close %s<br/>\r\n",row.getString("prnr"));
							}
						}
					}
					else {
						if(proj.isEmpty())
							out.println("Nichts gefunden<br/>");
						else
							out.println("Password Fehler<br/>");
					}
				}
				
				if(projekt != null) {
					DateFormat formdate = new SimpleDateFormat("E ,dd.MM.yyyy");
					Database db = new DatabaseBuilder(new File(DBASE)).setReadOnly(true).open();
					IndexCursor cprojektkopf = CursorBuilder.createCursor(db.getTable("projektkopf").getPrimaryKeyIndex());
					if (cprojektkopf.findFirstRowByEntry(projekt)) {
						String aufnr = cprojektkopf.getCurrentRow().getString("pkkuaufnr");
						String kunde = cprojektkopf.getCurrentRow().getString("pkkunr");
						Date termin = cprojektkopf.getCurrentRow().getDate("pksende");
	
						// Auftraege
						Map<String, RowImpl> proj = new TreeMap<String, RowImpl>();
	
						proj.put(cprojektkopf.getCurrentRow().getString("pknr"), new RowImpl(cprojektkopf.getCurrentRow()));
	
						for (Row row : cprojektkopf.newIterable().setMatchPattern("pkkuaufnr", aufnr)) {
							String pknr = row.getString("pknr");
							if (!pknr.equals(projekt) && row.getString("pkkunr").equals(kunde))
								proj.put(pknr, new RowImpl(row));
						}
	
						out.println("<table>");
						out.println("<form action=\"\" method=\"post\">");
						out.printf("<tr><td colspan=4>Version 2 (<i>runtime %d ms</i>)</td></tr>\r\n",	new Date().getTime() - start);
						out.println("<tr><th colspan=4 align=\"left\"><a name=\"prue\">Projekte beenden</a></th></tr>");
						out.printf("<tr><td>User:</td><td colspan=\"3\"><input type=\"text\" name=\"user\" value=\"%s\"></td>",user != null?escapeHtml(user):"");
						out.printf("<tr><td>Pass:</td><td colspan=\"3\"><input type=\"password\" name=\"pass\" value=\"%s\"></td>",pass != null?escapeHtml(pass):"");
						out.printf("<tr><th align=\"left\">Kunde</th><th colspan=3>%s</th></tr>\r\n",kunde);
						out.printf("<tr><th align=\"left\">Auftrag</th><th colspan=3>%s</th></tr>\r\n",escapeHtml(aufnr));
						out.printf("<tr><th align=\"left\">Termin</th><th colspan=3>%s</th></tr>\r\n",formdate.format(termin));
						out.println("<tr><th align=\"left\">Projekt</th><th align=\"left\">Info</th><th align=\"right\">Menge</th><th>Status</th></tr>");
	
						for (Map.Entry<String, RowImpl> prentry : proj.entrySet()) {
							out.printf("<tr><td>%s</td>", prentry.getKey());
							out.printf("<td>%s</td><td align=\"right\">%d</td><td align=center><input type=checkbox checked name=\"p\" value=\"%s\"",
									escapeHtml(prentry.getValue().getString("pkbez")),
									prentry.getValue().getInt("pkmenge"),
									prentry.getKey(),prentry.getKey());
							if (prentry.getValue().getString("pkstatus") != null) {
								if(prentry.getValue().getString("pkstatus").equalsIgnoreCase("FE"))
									out.print(" disabled");
								else
									minone=true;
							}
							else
								minone=true;
							out.println("></td><tr>");
						}
						
	//					DateFormat formdate = new SimpleDateFormat("E,dd.MM.yy HH:mm:ss");
	//					DateFormat formdate2 = new SimpleDateFormat("E,dd.MM.yy");
	//pr.getString("pkkunr"), escapeHtml(pr.getString("pkkuaufnr")),formdate2.format(pr.getDate("pksende"))
					} else {
						out.println("<tr><td>Nichts gefunden</td></tr>");
					}
					db.close();
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
					out.println("Klasse:" + element.getClassName() + " Methode:" + element.getMethodName()
							+ " Zeile:" + element.getLineNumber() + "<br/>");
				out.println("</div>");
			}
			out.printf("<tr><td colspan=4><input type=\"submit\" value=\"beenden\"");
			if(!minone) out.printf(" disabled");
			out.println("></td></tr>");
			out.println("</form>");
			out.println("</table>");
		}
		out.println("</body>");
		out.println("</html>");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private String replacecr(String s) {
		return s.replace("\n", "<br/>").replace("\r", "");
	}
}
