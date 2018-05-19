package vvs_dbsetup;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.*;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.destination.Destination;
import com.ninja_squad.dbsetup.destination.DriverManagerDestination;
import com.ninja_squad.dbsetup.operation.Operation;

import static vvs_dbsetup.DBSetupUtils.*;
import webapp.services.*;

public class DeliveryDBTest {

	private static Destination dataSource;
	
	// the tracker is static because JUnit uses a separate Test instance for every test method.
    private static DbSetupTracker dbSetupTracker = new DbSetupTracker();
	
    @BeforeClass
    public static void setupClass() {
    	startApplicationDatabaseForTesting();
		dataSource = DriverManagerDestination.with(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
    
    @Before
	public void setup() throws SQLException {

		Operation initDBOperations = Operations.sequenceOf(
			  DELETE_ALL
			, INSERT_CUSTOMER_SALE_DELIVERY_DATA
			);
		
		DbSetup dbSetup = new DbSetup(dataSource, initDBOperations);
		
        // Use the tracker to launch the DbSetup. This will speed-up tests 
		// that do not not change the BD. Otherwise, just use dbSetup.launch();
        dbSetupTracker.launchIfNecessary(dbSetup);
		
	}
    
    @Test
    public void queryDeliveryNumberTest() throws ApplicationException {
    	// read-only test: unnecessary to re-launch setup after test has been run
    	dbSetupTracker.skipNextLaunch();
    	
    	int expected = NUM_INIT_DELIVERIES;
    	int actual = 0;
		
		List<CustomerDTO> customers = CustomerService.INSTANCE.getAllCustomers().customers;
		for(CustomerDTO customer: customers)
			actual += SaleService.INSTANCE.getSalesDeliveryByVat(customer.vat).sales_delivery.size();
		
		assertEquals(expected, actual);
    }
    
    @Test
    public void addDeliverySizeTest() throws ApplicationException {
    	SaleService.INSTANCE.addSaleDelivery(1, 1);
    	
    	int size = 0;
		
		List<CustomerDTO> customers = CustomerService.INSTANCE.getAllCustomers().customers;
		for(CustomerDTO customer: customers)
			size += SaleService.INSTANCE.getSalesDeliveryByVat(customer.vat).sales_delivery.size();
		
		assertEquals(NUM_INIT_DELIVERIES + 1, size);
    }
    


}