package vvs_webapp;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class WebappTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";
    private static final String[] VALID_VATS = new String[] {
        "108136701",
        "108136710",
        "108136728",
        "108136736",
        "108136744",
    };

    private static final String CUSTOMER_VAT = "503183504";
    private static final String CUSTOMER_DESIGNATION = "FCUL";
    private static final String CUSTOMER_PHONE = "217500000";
    private static final String SALE_STATUS_OPEN = "O";
    private static final String SALE_STATUS_CLOSED = "C";

    private static HtmlPage page;

    @SuppressWarnings("Duplicates")
    @BeforeClass
    public static void setUpClass() throws Exception {
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
    
            // possible configurations needed to prevent JUnit tests to fail for complex HTML pages
            webClient.setJavaScriptTimeout(15000);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            
            page = webClient.getPage(APPLICATION_URL);
            assertEquals(200, page.getWebResponse().getStatusCode()); // OK status
        }
    }

    @Before
    public void setUp() throws Exception {
        WebappUtils.addCustomer(CUSTOMER_VAT, CUSTOMER_DESIGNATION, CUSTOMER_PHONE, page);
    }

    @After
    public void tearDown() throws Exception {
        WebappUtils.removeCustomer(CUSTOMER_VAT, page);
    }

    /**
     * After inserting a new address for an existing customer, the table of addresses of that client includes
     * that address and its total row size increases by one.
     */
    @Test
    public void insertAddressTest() throws Exception {
        final String ADDRESS = "address";
        final String DOOR = "12";
        final String POSTAL_CODE = "123-456";
        final String LOCALITY = "locality";
        // check the list of the customer's addresses
        HtmlPage customerInfoPage = WebappUtils.getCustomerInfoPage(CUSTOMER_VAT);
        List<DomElement> addressListElements = customerInfoPage.getElementsById("address-list");
        int INITIAL_ROW_COUNT = 0;
        if (!addressListElements.isEmpty()) {
            INITIAL_ROW_COUNT = customerInfoPage.<HtmlTable>getHtmlElementById("address-list").getRowCount();
        }

        // add address to the customer
        HtmlPage reportPage = WebappUtils.addAddressToCustomer(CUSTOMER_VAT, ADDRESS, DOOR, POSTAL_CODE, LOCALITY, page);

        // check if the report page includes the proper values
        String textReportPage = reportPage.asText();
        assertTrue(textReportPage.contains(CUSTOMER_VAT));
        assertTrue(textReportPage.contains(ADDRESS));
        assertTrue(textReportPage.contains(DOOR));
        assertTrue(textReportPage.contains(POSTAL_CODE));
        assertTrue(textReportPage.contains(LOCALITY));

        // check the list of the customer's addresses
        HtmlPage updatedCustomerInfoPage = WebappUtils.getCustomerInfoPage(CUSTOMER_VAT);

        HtmlTable addressTable = updatedCustomerInfoPage.getHtmlElementById("address-list");
        List<HtmlTableRow> rows = addressTable.getRows();
        assertEquals(INITIAL_ROW_COUNT + 1, rows.size());

        for (HtmlTableRow row : rows) {
            if (row.getCell(0).asText().equals(ADDRESS) &&
                row.getCell(1).asText().equals(DOOR) &&
                row.getCell(2).asText().equals(POSTAL_CODE) &&
                row.getCell(3).asText().equals(LOCALITY)) {
                return;
            }
        }
        fail("Could not find the added address.");
    }

    /**
     * After inserting new customers, all the information is properly listed in the List All Customers use case.
     */
    @Test
    public void insertNewCustomersTest() throws Exception {
        // make sure the customers to be added are not in the DB
        // PROBLEM: due to caching, the cleanup of the DB at the end of the test does not work if these are executed.
        //          Manual cache clearing did not solve the issue.
        // removeCustomer(VALID_VATS[0]);
        // removeCustomer(VALID_VATS[1]);
        // removeCustomer(VALID_VATS[2]);

        // get all customers
        HtmlPage allCustomersPage = (HtmlPage) page.getAnchorByHref("GetAllCustomersPageController").openLinkInNewWindow();
        final HtmlTable initialCustomers = allCustomersPage.getHtmlElementById("clients");

        String CUSTOMER_0_NAME = "John Snow";
        String CUSTOMER_1_NAME = "John Brown";
        String CUSTOMER_2_NAME = "Joe Black";
        WebappUtils.addCustomer(VALID_VATS[0], CUSTOMER_0_NAME, CUSTOMER_PHONE, page);
        WebappUtils.addCustomer(VALID_VATS[1], CUSTOMER_1_NAME, CUSTOMER_PHONE, page);
        WebappUtils.addCustomer(VALID_VATS[2], CUSTOMER_2_NAME, CUSTOMER_PHONE, page);

        // get all customers again
        HtmlPage updatedAllCustomersPage = (HtmlPage) page.getAnchorByHref("GetAllCustomersPageController").openLinkInNewWindow();
        final HtmlTable updatedCustomers = updatedAllCustomersPage.getHtmlElementById("clients");

        assertEquals(3 + initialCustomers.getRowCount(), updatedCustomers.getRowCount());
        List<Integer> foundCustomers = new ArrayList<>();
        for (HtmlTableRow customerRow : updatedCustomers.getRows()) {
            checkCustomerRow(0, CUSTOMER_0_NAME, CUSTOMER_PHONE, customerRow, foundCustomers);
            checkCustomerRow(1, CUSTOMER_1_NAME, CUSTOMER_PHONE, customerRow, foundCustomers);
            checkCustomerRow(2, CUSTOMER_2_NAME, CUSTOMER_PHONE, customerRow, foundCustomers);
        }
        assertEquals("Did not find all added customers.", 3, foundCustomers.size());

        // cleanup - for some reason not working??? caching pages?
        WebappUtils.removeCustomer(VALID_VATS[0], page);
        WebappUtils.removeCustomer(VALID_VATS[1], page);
        WebappUtils.removeCustomer(VALID_VATS[2], page);
    }

    private void checkCustomerRow(int index, String customerName, String customerPhone, HtmlTableRow customerRow,
                                  List<Integer> foundCustomers) {
        if (customerRow.getCell(2).asText().equals(VALID_VATS[index])) {
            // check if already found
            if (foundCustomers.contains(index)) {
                fail("Double entry for vat: " + VALID_VATS[index]);
            }
            assertEquals(customerRow.getCell(0).asText(), customerName);
            assertEquals(customerRow.getCell(1).asText(), customerPhone);

            foundCustomers.add(index);
        }
    }

    /**
     * A new sale is listed as an open sale for the respective customer.
     */
    @Test
    public void newSaleIsOpenTest() throws Exception {
        // get existing sales of the customer
        List<String> existingSales = WebappUtils.getExistingSaleIds(CUSTOMER_VAT);

        // add new sale
        WebappUtils.addSaleToCustomer(CUSTOMER_VAT, page);
        // get updated sales of the customer
        HtmlPage updatedCustomerSalePage = WebappUtils.getCustomerSalePage(CUSTOMER_VAT);
        HtmlTable saleList = updatedCustomerSalePage.getHtmlElementById("sale-list");
        int salesCount = saleList.getRowCount() - 1; // ignore title row
        assertEquals(1 + existingSales.size(), salesCount);

        // check the added sale status
        HtmlTableRow addedSale = getAddedSale(saleList, existingSales);
        assertNotNull("Did not find the added sale.", addedSale);
        assertEquals(CUSTOMER_VAT, addedSale.getCell(4).asText());   // Customer VAT
        assertEquals(SALE_STATUS_OPEN, addedSale.getCell(3).asText()); // Status
        assertEquals("0.0", addedSale.getCell(2).asText()); // Total
    }

    private HtmlTableRow getAddedSale(HtmlTable saleList, List<String> existingSales) {
        for (int i = 1; i < saleList.getRowCount(); i++) {
            HtmlTableRow saleRow = saleList.getRow(i);
            String id = saleRow.getCell(0).asText();
            if (!existingSales.contains(id)) {
                return saleList.getRow(i);
            }
        }
        return null;
    }

    /**
     * After closing a sale, it is listed as closed.
     */
    @Test
    public void closeSaleTest() throws Exception {
        // add sale and check the status (OPEN)
        HtmlTableRow addedSale = addAndGetSale(CUSTOMER_VAT);
        assertNotNull("Did not find the added sale.", addedSale);
        assertEquals(SALE_STATUS_OPEN, addedSale.getCell(3).asText());
        String addedSaleId = addedSale.getCell(0).asText();

        // close the sale
        HtmlPage saleStatusPage = (HtmlPage) page.getAnchorByHref("UpdateSaleStatusPageController").openLinkInNewWindow();
        HtmlForm removeSaleForm = saleStatusPage.getForms().get(0);
        removeSaleForm.getInputByName("id").setValueAttribute(addedSaleId);
        removeSaleForm.getInputByName("submit").click();

        // go to sale list again
        saleStatusPage = (HtmlPage) page.getAnchorByHref("UpdateSaleStatusPageController").openLinkInNewWindow();
        HtmlTable updatedSaleList = saleStatusPage.getHtmlElementById("sale-list");
        // check the status of the sale (CLOSED)
        for (HtmlTableRow saleRow : updatedSaleList.getRows()) {
            if (saleRow.getCells().size() > 0 && saleRow.getCell(0).asText().equals(addedSaleId)) {
                assertEquals("Closed sale " + addedSaleId + " should have status closed.", SALE_STATUS_CLOSED, saleRow.getCell(3).asText());
                return;
            }
        }
        fail("Did not find the closed sale.");
    }

    private HtmlTableRow addAndGetSale(String vat) throws IOException {
        // get existing sales of the customer
        List<String> existingSales = WebappUtils.getExistingSaleIds(vat);
        // add new sale
        WebappUtils.addSaleToCustomer(vat, page);
        // get updated sales of the customer
        HtmlPage updatedCustomerSalePage = WebappUtils.getCustomerSalePage(vat);
        return getAddedSale(updatedCustomerSalePage.getHtmlElementById("sale-list"), existingSales);
    }

    /**
     * After creating a new customer, a new sale for them and inserting a delivery for that sale, the sale delivery
     * contains all expected information. All intermediate pages have the expected information.
     */
    @Test
    public void saleDeliveryTest() throws Exception {
        // add sale
        HtmlTableRow addedSale = addAndGetSale(CUSTOMER_VAT);
        assertNotNull("Did not find the added sale.", addedSale);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("vat", CUSTOMER_VAT));
        // add address to make sure there is at least one
        WebappUtils.addAddressToCustomer(CUSTOMER_VAT, "addr", "d", "post", "loc", page);
        // get existing sale deliveries
        List<String> existingDeliveryIds = WebappUtils.getExistingDeliveryIds(CUSTOMER_VAT);

        HtmlPage addSaleDeliveryPage =
            WebappUtils.getPage(new URL(WebappTest.APPLICATION_URL + "AddSaleDeliveryPageController"), params);
        String addressId = addSaleDeliveryPage.<HtmlTable>getHtmlElementById("address-list").getRow(1).getCell(0).asText();
        // check if addedSaleId is in the list
        String addedSaleId = addedSale.getCell(0).asText();
        HtmlTable saleTable = addSaleDeliveryPage.getHtmlElementById("sale-list");
        List<String> saleIds = saleTable.getRows().subList(1, saleTable.getRowCount()).stream()
            .map(r -> r.getCell(0).asText())
            .collect(Collectors.toList());
        assertTrue(saleIds.contains(addedSaleId));

        // add sale delivery
        HtmlForm deliveryForm = addSaleDeliveryPage.getForms().get(0);
        deliveryForm.getInputByName("addr_id").setValueAttribute(addressId);
        deliveryForm.getInputByName("sale_id").setValueAttribute(addedSaleId);
        deliveryForm.getInputByName("submit").click();

        // get current delivery ids and compare with initial ones
        List<String> updatedDeliveryIds = WebappUtils.getExistingDeliveryIds(CUSTOMER_VAT);
        updatedDeliveryIds.removeAll(existingDeliveryIds);
        assertEquals(1, updatedDeliveryIds.size());

        // check new entry in delivery table
        HtmlTable saleDeliveryTable = WebappUtils.getPage(new URL(WebappTest.APPLICATION_URL + "GetSaleDeliveryPageController"), params).getHtmlElementById("sale-delivery-list");
        String addedDeliveryId = updatedDeliveryIds.get(0);
        List<HtmlTableRow> deliveries = saleDeliveryTable.getRows().stream()
            .filter(r -> r.getCell(0).asText().equals(addedDeliveryId))
            .collect(Collectors.toList());
        assertEquals(1, deliveries.size());

        assertEquals(addedSaleId, deliveries.get(0).getCell(1).asText());
        assertEquals(addressId, deliveries.get(0).getCell(2).asText());
    }
}





