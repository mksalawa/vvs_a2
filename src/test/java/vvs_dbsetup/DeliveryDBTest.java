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

/**
 * Tests the expected behavior of Deliveries interactions with the database
 * @author fc45701
 * @author fc52214
 */	
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
    
    private boolean hasDelivery(int vat, int delivery_id) throws ApplicationException {
        List<SaleDeliveryDTO> deliveries = SaleService.INSTANCE.getSalesDeliveryByVat(vat).sales_delivery;
        for(SaleDeliveryDTO delivery: deliveries)
            if(delivery.id == delivery_id)
                return true;
        return false;
    }
    
    @Test
    public void addDeliveryTest() throws ApplicationException{
    	int vat = getFirstCustomerVat();
        assumeFalse(hasDelivery(vat, 3));
        SaleService.INSTANCE.addSaleDelivery(1, 3);
        assertTrue(hasDelivery(197672337, 1));
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
