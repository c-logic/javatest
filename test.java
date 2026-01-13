import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.IndexCursor;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.impl.RowImpl;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
/**
 * Servlet implementation class test
 */
@WebServlet("/test")

public class test extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public test() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    
    private int maxbit = 65536; 
    
    private int[] projexist=new int[maxbit/32]; //65535
    
    private boolean getbit(int y) {
   		return (projexist[y/32] & (1<<(y & 31))) != 0;
    }
    
    private boolean setbit(int y) {
   		if(getbit(y))
    		return true;
    	else {
    		projexist[y/32] |= 1<<(y & 31);
    		return false;
    	}
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String DBASE = getServletContext().getInitParameter("DBASE");
		if (DBASE == null)
			DBASE = "c:/temp/obserwer.mdb";
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.printf("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<meta charset=\"UTF-8\"/>\r\n<title>%s</title>\r\n</head>\r\n", escapeHtml(DBASE));
		out.println("<body bgcolor=\"white\">");
		
		try {
			Database db = new DatabaseBuilder(new File(DBASE)).setReadOnly(true).open();
			out.println("projektkopf <- projekt, projektmaterial<br/>\r\nClear Bitmask<br>\r\n");
			for(int t=0;t<projexist.length;t++) {
				projexist[t]=0;
			}
			
			String tab="projektkopf";
			String field="pknr";
			for (Row row : CursorBuilder.createCursor(db.getTable(tab).getPrimaryKeyIndex()).newIterable()) {
				try {
					String o=row.getString(field);
					int p=Integer.parseInt(o);
					if(p>=maxbit) {
						out.println(tab+"??overflow "+o+"<br/>\r\n");
					} else {
						if(setbit(p)) {
							out.println(tab+"??doublette "+o+"<br/>\r\n");
						}
					}
				}
				catch(NumberFormatException e) {
					out.println(tab+"??parseerror \""+row.getString(field)+"\"<br>\r\n");
				}
			}
			
			tab="projektmaterial";
			field="pmapknr";
			for (Row row : CursorBuilder.createCursor(db.getTable(tab)).newIterable()) {//
				try {
					String o=row.getString(field);;
					int p=Integer.parseInt(o);
					if(p>=maxbit) {
						out.println(tab+"??overflow "+o+"<br/>\r\n");
					} else {
						if(!getbit(p)) {
							out.println(tab+"??not found "+o+"<br/>\r\n");
						}
					}
				}
				catch(NumberFormatException e) {
					out.println(tab+"??parseerror \""+row.getString(field)+"\"<br>\r\n");
				}
			}
			
			tab="projekt";
			field="prnr";
			for (Row row : CursorBuilder.createCursor(db.getTable(tab)).newIterable()) {//
				try {
					String o=row.getString(field);
					int p=Integer.parseInt(o);
					if(p>=maxbit) {
						out.println(tab+"??overflow "+o+"<br/>\r\n");
					} else {
						if(!getbit(p)) {
							out.println(tab+"??not found "+o+"<br/>\r\n");
						}
					}
				}
				catch(NumberFormatException e) {
					out.println(tab+"??parseerror \""+row.getString(field)+"\"<br>\r\n");
				}
			}
			out.println("Ready<br>\r\n");
			out.println("projektkopf -> projekt<br/>\r\nClear Bitmask<br>\r\n");
			for(int t=0;t<projexist.length;t++) {
				projexist[t]=0;
			}
			
			tab="projekt";
			field="prnr";
			for (Row row : CursorBuilder.createCursor(db.getTable(tab)).newIterable()) {
				try {
					String o=row.getString(field);
					int p=Integer.parseInt(o);
					if(p>=maxbit) {
						out.println(tab+"??overflow "+o+"<br/>\r\n");
					} else {
						setbit(p);
					}
				}
				catch(NumberFormatException e) {
					out.println(tab+"??parseerror \""+row.getString(field)+"\"<br>\r\n");
				}
			}

			tab="projektkopf";
			field="pknr";
			for (Row row : CursorBuilder.createCursor(db.getTable(tab).getPrimaryKeyIndex()).newIterable()) {
				try {
					String o=row.getString(field);
					int p=Integer.parseInt(o);
					if(p>=maxbit) {
						out.println(tab+"??overflow "+o+"<br/>\r\n");
					} else {
						if(!getbit(p)) {
							out.println(tab+"??not found "+o+"<br/>\r\n");
						}
					}
				}
				catch(NumberFormatException e) {
					out.println(tab+"??parseerror \""+row.getString(field)+"\"<br>\r\n");
				}
			}
			out.println("Ready<br>\r\n");
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
		out.println("</body>");
		out.println("</html>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
