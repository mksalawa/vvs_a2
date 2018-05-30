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
	public void setup() {

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
		int vat = CustomerService.INSTANCE.getFirstCustomerVat();
		assumeTrue(CustomerService.INSTANCE.hasClient(vat));
		assertThrows(ApplicationException.class, () -> 
			CustomerService.INSTANCE.addCustomer(vat, "FCUL", 217500000));
	}
	/**
	 * b) after the update of a Customer contact, 
	 * 	  the information should be properly saved
	 */
	@Test
	public void updatingContactSavesTheChangesTest() throws ApplicationException {
		int vat = CustomerService.INSTANCE.getFirstCustomerVat();
		CustomerDTO cust = CustomerService.INSTANCE.getCustomerByVat(vat);
		assumeTrue(CustomerService.INSTANCE.hasClient(vat));
		CustomerService.INSTANCE.updateCustomerPhone(vat, cust.phoneNumber + 1);
		assertEquals(cust.phoneNumber + 1, CustomerService.INSTANCE.getCustomerByVat(vat).phoneNumber);
	}
	
	/**
	 * c) after deleting all Customers, the list of all customers should be empty
	 */

	@Test
	public void deletingAllCustomersTest() throws ApplicationException {
		assumeTrue(CustomerService.INSTANCE.getAllCustomers().customers.size() > 0);
		deleteAllCustomers();
		assertEquals(0, CustomerService.INSTANCE.getAllCustomers().customers.size());
	}

	/**
	 * d) after deleting a certain Customer,
	 *    it's possible to add it back without lifting exceptions
	 */
	@Test
	public void addingDeletedCustomerTest() throws ApplicationException {
		int vat = CustomerService.INSTANCE.getFirstCustomerVat();
		assumeTrue(CustomerService.INSTANCE.hasClient(vat));
		CustomerDTO cust = CustomerService.INSTANCE.getCustomerByVat(vat);
		CustomerService.INSTANCE.removeCustomer(vat);
		assertFalse(CustomerService.INSTANCE.hasClient(vat));
		CustomerService.INSTANCE.addCustomer(cust.vat, cust.designation, cust.phoneNumber);
		assertTrue(CustomerService.INSTANCE.hasClient(vat));
	}
	
	private void deleteAllCustomers() throws ApplicationException{
		CustomersDTO customersDTO = CustomerService.INSTANCE.getAllCustomers();
		
		for(CustomerDTO customer : customersDTO.customers) {
			CustomerService.INSTANCE.removeCustomer(customer.vat);
		}
	}
	
	/**
	 * e)After deleting a certain customer, its sales should be
	 *  removed from the database
	 */
	@Test
	public void removeCustomerRemoveSalesTest() throws ApplicationException {
		int vat = CustomerService.INSTANCE.getFirstCustomerVat();
		assumeTrue(CustomerService.INSTANCE.hasClient(vat));
		assumeTrue(SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size() > 0);
		SaleService.INSTANCE.addSale(vat);
		CustomerService.INSTANCE.removeCustomer(vat);
		assertEquals("Size should be zero after deletion",
            0, SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size());
	}
}
