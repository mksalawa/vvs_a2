package vvs_dbsetup;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.*;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.destination.Destination;
import com.ninja_squad.dbsetup.destination.DriverManagerDestination;
import com.ninja_squad.dbsetup.operation.Operation;

import static vvs_dbsetup.DBSetupUtils.*;
import webapp.services.*;

import static org.junit.Assume.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the expected behavior of Sales interactions with the database
 * @author fc45701
 * @author fc52214
 */	
public class SaleDBTest {

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
			, INSERT_CUSTOMER_SALE_DATA
			);
		
		DbSetup dbSetup = new DbSetup(dataSource, initDBOperations);
		
        // Use the tracker to launch the DbSetup. This will speed-up tests 
		// that do not not change the BD. Otherwise, just use dbSetup.launch();
        dbSetupTracker.launchIfNecessary(dbSetup);
		
	}
    
    @Test
    public void updateNonExistingSaleTest() throws ApplicationException{
    	int id = 20;
    	assumeFalse(SaleService.INSTANCE.hasSale(id));
    	assertThrows(ApplicationException.class, () -> 
			SaleService.INSTANCE.updateSale(id));
    }
    
    @Test
    public void addSaleToCustomerTest() throws ApplicationException {
    	int vat = CustomerService.INSTANCE.getFirstCustomerVat();
    	assumeTrue(CustomerService.INSTANCE.hasClient(vat));
    	int init = SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size();
        SaleService.INSTANCE.addSale(vat);
        assertEquals("Size of adding sales to Customer should increment",init + 1,
        		SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size());
      
    }
    
    
    /**
	 * f) adding a new sale increases the total number of all sales by one
	 */
    @Test
    public void addSaleSizeTest() throws ApplicationException {
    	int vat = CustomerService.INSTANCE.getFirstCustomerVat();
    	SaleService.INSTANCE.addSale(vat);
    	
    	int size = 0;
		
		List<CustomerDTO> customers = CustomerService.INSTANCE.getAllCustomers().customers;
		for(CustomerDTO customer: customers)
			size += SaleService.INSTANCE.getSaleByCustomerVat(customer.vat).sales.size();
		
		assertEquals(NUM_INIT_SALES + 1, size);
    }
    
    
    
}
