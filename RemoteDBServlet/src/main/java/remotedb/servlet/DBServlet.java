package remotedb.servlet;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/DBServlet")
public class DBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
       // doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        final String CONFIG_FILE = "/db_config.txt";
        String configFilePath = request.getServletContext().getRealPath(CONFIG_FILE);

        String host = "";
        String port = "";
        String db = "";
        String user = "";
        String password = "";
        int colNum = 0;
        String header_holder = "";   //default "|" as the delimiter
        String configSecret = "";
        String sql_statement = "";

        boolean inSqlBlock = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
        	
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!inSqlBlock) {
                    if (line.startsWith("host=")) {
                        host = line.substring("host=".length()).trim();
                    } 
                    else if (line.startsWith("port=")) {
                        port = line.substring("port=".length()).trim();
                    }            
                    else if (line.startsWith("database=")) {
                        db = line.substring("database=".length()).trim();
                    } 
                    else if (line.startsWith("username=")) {
                        user = line.substring("username=".length()).trim();
                    } 
                    else if (line.startsWith("password=")) {
                        password = line.substring("password=".length()).trim();
                    } 
                    else if (line.startsWith("col_num=")) {
                        colNum = Integer.parseInt(line.substring("col_num=".length()).trim());
                    } 
                    else if (line.equalsIgnoreCase("sql_statements_start")) {
                        inSqlBlock = true;
                    }
                    else if (line.startsWith("secret=")) {
                        configSecret = line.substring("secret=".length()).trim();
                    }
                    else if (line.startsWith("header_holder=")) {
                        header_holder = line.substring("header_holder=".length()).trim();
                    }                    
                } 
                 else {
                    	if (line.equalsIgnoreCase("sql_statements_end")) {
                    		inSqlBlock = false;
                    	} 
                    	else if (!line.isEmpty()) {
                    		sql_statement += line + "\n";
                          }
                  }
            }

            System.out.println("Loaded DB config from: " + configFilePath);
        }
        
        
        String requestSecret = request.getParameter("secret");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        if (requestSecret == null || !requestSecret.equals(configSecret)) {
            out.println("<h3>Access Denied: Invalid or missing secret.</h3>");
            return;
        }

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?";


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            out.println("<h3>MySQL JDBC Driver Not Found</h3>");
            e.printStackTrace(out);
            return;
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement stmt = conn.createStatement()) {

            out.println("<h2>Remote DB Connection Success!</h2>");

                try (ResultSet rs = stmt.executeQuery(sql_statement)) {
                	String result = "";

                    while (rs.next()) {
                    	result += rs.getString(1);
                        for (int i = 2; i <= colNum; i++) {                       	
                        	result = result + " | " + rs.getString(i);
                        }
                        result += "@";
                    }
                    result = header_holder + "@" + result;
                    out.println(result.toString());
                } 
                catch (SQLException e) {
                    out.println("<p><strong>Error executing query:</strong> " + sql_statement + "</p>");
                    e.printStackTrace(out);
                }

        } catch (SQLException e) {
            out.println("<h3>Database Connection Failed</h3>");
            e.printStackTrace(out);
        }
    }
}
