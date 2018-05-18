package vvs_dbsetup;

import static com.ninja_squad.dbsetup.Operations.*;
import com.ninja_squad.dbsetup.generator.ValueGenerators;
import com.ninja_squad.dbsetup.operation.Insert;
import com.ninja_squad.dbsetup.operation.Operation;

import java.util.GregorianCalendar;

import webapp.persistence.PersistenceException;

/** References:
 *
 * HOME: http://dbsetup.ninja-squad.com/index.html
 * API: http://dbsetup.ninja-squad.com/apidoc/2.1.0/index.html
 * Best practices: http://dbunit.sourceforge.net/bestpractices.html
 * 
 * @author jpn
 */
@SuppressWarnings("all")
public class DBSetupUtils {
	
	public static final String DB_URL = "jdbc:hsqldb:file:src/main/resources/data/hsqldb/cssdb";
	public static final String DB_USERNAME = "SA";
	public static final String DB_PASSWORD = "";
	
	private static boolean appDatabaseAlreadyStarted = false;
	
	public static void startApplicationDatabaseForTesting() {
		
		if (appDatabaseAlreadyStarted)  // just do it once for the entire test suite;
			return;
		
    	try {
			webapp.persistence.DataSource.INSTANCE.connect(DB_URL, DB_USERNAME, DB_PASSWORD);
			appDatabaseAlreadyStarted = true;
		} catch (PersistenceException e) {
			throw new Error("Application DataSource could not be started");
		}
	}
	
	//////////////////////////////////////////
	// Operations for populating test database
	
    public static final Operation DELETE_ALL =
            deleteAllFrom("CUSTOMER", "SALE", "ADDRESS", "SALEDELIVERY");

    public static final int NUM_INIT_CUSTOMERS;
    public static final int NUM_INIT_SALES;
    public static final int NUM_INIT_ADDRESSES;
    public static final int NUM_INIT_DELIVERIES;

    public static final Operation INSERT_CUSTOMER_SALE_DATA;
    public static final Operation INSERT_CUSTOMER_ADDRESS_DATA;
    public static final Operation INSERT_CUSTOMER_SALE_DELIVERY_DATA;
	
	static {
		
		Insert insertCustomers =
			insertInto("CUSTOMER")
            .columns("ID", "DESIGNATION", "PHONENUMBER", "VATNUMBER")
            .values(   1,   "JOSE FARIA",     914276732,   197672337)
            .values(   2,  "LUIS SANTOS",     964294317,   168027852)
            .build();
		
		NUM_INIT_CUSTOMERS = insertCustomers.getRowCount();
		
		Insert insertSales = 
			insertInto("SALE")
            .columns("ID",                            "DATE", "TOTAL", "STATUS", "CUSTOMER_VAT")
            .values(   1,  new GregorianCalendar(2018,01,02),     0.0,      'O',      197672337)
            .values(   2,  new GregorianCalendar(2017,03,25),     0.0,      'O',      197672337)
            .build();
		
		NUM_INIT_SALES = insertSales.getRowCount();
		
		// it's possible to combine dataset samples with 'sequenceOf'
		INSERT_CUSTOMER_SALE_DATA = sequenceOf(insertCustomers, insertSales);
		
		Insert insertAddresses = 
				insertInto("ADDRESS")
                //.withGeneratedValue("ID", ValueGenerators.sequence().startingAt(100L).incrementingBy(1))
                .columns("ID",                             "ADDRESS", "CUSTOMER_VAT")
                .values(    1,          "FCUL, Campo Grande, Lisboa",      197672337)
                .values(    2,         "R. 25 de Abril, 101A, Porto",      197672337)
                .values(    3,"Av Neil Armstrong, Cratera Azul, Lua",      168027852)
                .build();
		
		NUM_INIT_ADDRESSES = insertAddresses.getRowCount();		
		
		INSERT_CUSTOMER_ADDRESS_DATA = sequenceOf(insertCustomers, insertAddresses);
		
		Insert insertDeliveries =
				insertInto("SALEDELIVERY")
				.columns("ID", "SALE_ID", "CUSTOMER_VAT", "ADDRESS_ID")
				.values(    1,         1,      197672337,            2)
				.values(    2,         2,      197672337,            1)
				.build();
		
		NUM_INIT_DELIVERIES = insertDeliveries.getRowCount();
		
		INSERT_CUSTOMER_SALE_DELIVERY_DATA = sequenceOf(insertCustomers, insertAddresses, insertSales,  insertDeliveries);
	}
	
}

