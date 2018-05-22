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
import static org.junit.jupiter.api.Assertions.assertThrows;
import webapp.persistence.PersistenceException;
import webapp.services.*;

/**
 * Tests the expected behavior of Customers interactions with the database
 * @author fc45701
 * @author fc52214
 */		
public class CustomersDBTest {

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
	
	/**
	 * a) The SUT does not allow to add a new client with an existing VAT
	 */
	@Test
	public void noDuplicateVATTest() throws ApplicationException {
		//int vat = 197672337;
		assumeTrue(hasClient(CUSTOMER1_VAT));
		assertThrows(Exception.class, () -> 
			CustomerService.INSTANCE.addCustomer(CUSTOMER1_VAT, "FCUL", 217500000));
	}
	/**
	 * b) after the update of a Customer contact, 
	 * 	  the information should be properly saved
	 */
	@Test
	public void updatingContactSavesTheChangesTest() throws ApplicationException {
		//int vat = 197672337;
		CustomerDTO cust = CustomerService.INSTANCE.getCustomerByVat(CUSTOMER1_VAT);
		assumeTrue(hasClient(CUSTOMER1_VAT));
		CustomerService.INSTANCE.updateCustomerPhone(CUSTOMER1_VAT, cust.phoneNumber + 1);
		assertTrue(CustomerService.INSTANCE.getCustomerByVat(CUSTOMER1_VAT).phoneNumber == cust.phoneNumber + 1);
	}
	
	/**
	 * c) after deleting all Customers, the list of all customers should be empty
	 */

	@Test
	public void deletingAllCustomersTest() throws ApplicationException {
		assertTrue(CustomerService.INSTANCE.getAllCustomers().customers.size() > 0);
		deleteAllCustomers();
		assertTrue(CustomerService.INSTANCE.getAllCustomers().customers.size() == 0);
	}

	/**
	 * d) after deleting a certain Customer,
	 *    it's possible to add ir back without lifting exceptions
	 */
	@Test
	public void addingDeletedCustomerTest() throws ApplicationException {
		assumeTrue(hasClient(CUSTOMER1_VAT));
		CustomerDTO cust = CustomerService.INSTANCE.getCustomerByVat(CUSTOMER1_VAT);
		CustomerService.INSTANCE.removeCustomer(CUSTOMER1_VAT);
		assertFalse(hasClient(CUSTOMER1_VAT));
		CustomerService.INSTANCE.addCustomer(cust.vat, cust.designation, cust.phoneNumber);
		assertTrue(hasClient(CUSTOMER1_VAT));
	}
	
	private void deleteAllCustomers() throws ApplicationException{
		CustomersDTO customersDTO = CustomerService.INSTANCE.getAllCustomers();
		
		for(CustomerDTO customer : customersDTO.customers)
			CustomerService.INSTANCE.removeCustomer(customer.vat);
	}
	
	/**
	 * e)After deleting a certain customer, its sales should be
	 *  removed from the database
	 */
	@Test
	public void removeCustomerRemoveSalesTest() throws ApplicationException {
		assumeTrue(hasClient(CUSTOMER1_VAT));
		assumeTrue(SaleService.INSTANCE.getSaleByCustomerVat(CUSTOMER1_VAT).sales.size() > 0);
		SaleService.INSTANCE.addSale(CUSTOMER1_VAT);
		CustomerService.INSTANCE.removeCustomer(CUSTOMER1_VAT);
		assertEquals("Size should be zero after deletion",0,SaleService
				.INSTANCE.getSaleByCustomerVat(CUSTOMER1_VAT).sales.size());
	}
	
}
