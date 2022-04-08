package proj.one;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;
//import java.net.URL;

public class import_contract {
    private static final int BATCH_SIZE = 500;
//    private static URL propertyURL = GoodLoader.class
//        .getResource("/loader.cnf");

    private static Connection con = null;
    private static PreparedStatement stmt = null;
    private static PreparedStatement sid = null;
    private static boolean verbose = false;

    private static void openDB(String host, String dbname,
                               String user, String pwd) {
        try {
            //
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
            System.exit(1);
        }
        String url = "jdbc:postgresql://" + host + "/" + dbname;
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);
        try {
            con = DriverManager.getConnection(url, props);
            if (verbose) {
                System.out.println("Successfully connected to the database "
                    + dbname + " as " + user);
            }
            con.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);
        }
        try {
            stmt = con.prepareStatement(
                "insert into contracts(contract_number,contract_date, client_enterprise_id)"
                    +
                    " values(?,?,(select client_enterprise_id from client_enterprises where name = ?))");
        } catch (SQLException e) {
            System.err.println("Insert statement failed");
            System.err.println(e.getMessage());
            closeDB();
            System.exit(1);
        }
    }

    private static void closeDB() {
        if (con != null) {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                con.close();
                con = null;
            } catch (Exception e) {
                // Forget about it
            }
        }
    }

    private static void loadData(String num, Date date, String enter)
        throws SQLException {
        if (con != null) {
            try {
                stmt.setString(1, num);
                stmt.setDate(2, date);
                stmt.setString(3, enter);
                stmt.addBatch();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        String fileName = null;
        boolean verbose = false;

        switch (args.length) {
            case 1:
                fileName = args[0];
                break;
            case 2:
                switch (args[0]) {
                    case "-v":
                        verbose = true;
                        break;
                    default:
                        System.err.println("Usage: java [-v] GoodLoader filename");
                        System.exit(1);
                }
                fileName = args[1];
                break;
            default:
                System.err.println("Usage: java [-v] GoodLoader filename");
                System.exit(1);
        }

//        if (propertyURL == null) {
//            System.err.println("No configuration file (loader.cnf) found");
//            System.exit(1);
//        }
        Properties defprop = new Properties();
        defprop.put("host", "localhost");
        defprop.put("user", "postgres");
        defprop.put("password", "216122");
        defprop.put("database", "proj1");
        Properties prop = new Properties(defprop);
//        try (BufferedReader conf
//                 = new BufferedReader(new FileReader(propertyURL.getPath()))) {
//            prop.load(conf);
//        } catch (IOException e) {
//            // Ignore
//            System.err.println("No configuration file (loader.cnf) found");
//        }
        try (BufferedReader infile
                 = new BufferedReader(new FileReader(fileName))) {
            long start;
            long end;
            String line;
            String[] parts;
            String sur;
            String fir;
            int cnt = 0;
            // Empty target table
            openDB(prop.getProperty("host"), prop.getProperty("database"),
                prop.getProperty("user"), prop.getProperty("password"));
            Statement stmt0;
            if (con != null) {
                stmt0 = con.createStatement();
//                stmt0.execute("truncate table mobile_phone");
                stmt0.close();
            }
            closeDB();
            //
            start = System.currentTimeMillis();
            openDB(prop.getProperty("host"), prop.getProperty("database"),
                prop.getProperty("user"), prop.getProperty("password"));
            while ((line = infile.readLine()) != null) {
                parts = line.split(";");
                if (parts.length > 1) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date d = null;
                    try {
                        d = format.parse(parts[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    assert d != null;
                    Date rig = new Date(d.getTime());
                    loadData(parts[0], rig, parts[2]);
                    cnt++;
                    if (cnt % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                    }
                }
            }
            if (cnt % BATCH_SIZE != 0) {
                stmt.executeBatch();
            }
            con.commit();
            stmt.close();
            closeDB();
            end = System.currentTimeMillis();
            System.out.println(cnt + " records successfully loaded");
            System.out.println("Loading speed : "
                + (cnt * 1000) / (end - start)
                + " records/s");
        } catch (SQLException se) {
            System.err.println("SQL error: " + se.getMessage());
            try {
                con.rollback();
                stmt.close();
            } catch (Exception e2) {
            }
            closeDB();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Fatal error: " + e.getMessage());
            try {
                con.rollback();
                stmt.close();
            } catch (Exception e2) {
            }
            closeDB();
            System.exit(1);
        }
        closeDB();
    }
}

