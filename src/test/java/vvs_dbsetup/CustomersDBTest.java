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
			, INSERT_CUSTOMER_ADDRESS_DATA
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
		int vat = 503183504;
		//assumeTrue(hasClient(vat));
		assertFalse(hasClient(vat));
		CustomerService.INSTANCE.addCustomer(vat, "TESTE", 217500001);
		//assertTrue(hasClient(vat));
		assertThrows(Exception.class, () -> 
			CustomerService.INSTANCE.addCustomer(vat, "FCUL", 217500000));
	}
	/**
	 * b) after the update of a costumer contact, 
	 * 	  the information should be properly saved
	 *	TODO
	 */
	@Test
	public void updatingContactSavesTheChangesTest() throws ApplicationException {
		int vat = 503183504;
		int phoneNumber = 917774773;
		assertFalse(hasClient(vat));
		CustomerService.INSTANCE.addCustomer(vat, "TESTE", phoneNumber + 1);
		assertTrue(CustomerService.INSTANCE.getCustomerByVat(vat)
				.phoneNumber == phoneNumber + 1);
		CustomerService.INSTANCE.updateCustomerPhone(vat, phoneNumber);
		assertTrue(CustomerService.INSTANCE.getCustomerByVat(vat).phoneNumber == phoneNumber);
	}
	
	/**
	 * c) after deleting all costumers, the list of all customers should be empty
	 */

	@Test
	public void deletingAllCustomersTest() throws ApplicationException {
		assertTrue(CustomerService.INSTANCE.getAllCustomers().customers.size() != 0);
		deleteAllCostumers();
		assertTrue(CustomerService.INSTANCE.getAllCustomers().customers.size() == 0);
	}

	/**
	 * d) after deleting a certain costumer,
	 *    it's possible to add ir back without lifting exceptions
	 *    TODO
	 */
	@Test
	public void addingDeletedCostumerTest() throws ApplicationException {
		int vat = 503183504;
		String des = "FCUL";
		int phoneNumber = 217500000;
		CustomerService.INSTANCE.addCustomer(vat, des, phoneNumber);
		assertTrue(hasClient(vat));
		CustomerService.INSTANCE.removeCustomer(vat);
		assertFalse(hasClient(vat));
		CustomerService.INSTANCE.addCustomer(vat, des, phoneNumber);
		assertTrue(hasClient(vat));
	}
	
	private void deleteAllCostumers() throws ApplicationException{
		CustomersDTO customersDTO = CustomerService.INSTANCE.getAllCustomers();
		
		for(CustomerDTO customer : customersDTO.customers)
			CustomerService.INSTANCE.removeCustomer(customer.vat);
	}
	
	/**
	 * e)After deleting a certain customer, its sales should be
	 *  removed from the database
	 *  TODO -> The sales are not removed from the database!!!
	 */
	@Test
	public void salesRemovedTest() throws ApplicationException {
		int vat = 503183504;
		assertFalse(hasClient(vat));
		CustomerService.INSTANCE.addCustomer(vat, "FCUL", 217500000);
		SaleService.INSTANCE.addSale(vat);
		assertTrue(SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size() == 1);
		CustomerService.INSTANCE.removeCustomer(vat);
		assertFalse(hasClient(vat));
		//assertThrows(Exception.class, () -> 
		//	SaleService.INSTANCE.getSaleByCustomerVat(vat));
		assertEquals("Size should be zero",0,SaleService.INSTANCE.getSaleByCustomerVat(vat).sales.size());
	}
	
}
