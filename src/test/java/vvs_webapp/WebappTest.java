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

import static org.junit.Assert.*;

public class WebappTest {

    private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";
    private static final String[] VALID_VATS = new String[] {
        "108136701",
        "108136710",
        "108136728",
        "108136736",
        "108136744",
        "108136752",
        "108136779",
        "108136787",
        "108136795",
        "108136809",
    };

    private static final String CUSTOMER_VAT = "503183504";
    private static final String CUSTOMER_DESIGNATION = "FCUL";
    private static final String CUSTOMER_PHONE = "217500000";

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
        addCustomer(CUSTOMER_VAT, CUSTOMER_DESIGNATION, CUSTOMER_PHONE);
    }

    @After
    public void tearDown() throws Exception {
        removeCustomer(CUSTOMER_VAT);
    }

    private HtmlPage removeCustomer(String vat) throws Exception {
        // at index, goto Remove case and remove the previous client
        HtmlAnchor removeCustomerLink = page.getAnchorByHref("RemoveCustomerPageController");
        HtmlPage nextPage = (HtmlPage) removeCustomerLink.openLinkInNewWindow();

        HtmlForm removeCustomerForm = nextPage.getForms().get(0);
        removeCustomerForm.getInputByName("vat").setValueAttribute(vat);
        return removeCustomerForm.getInputByName("submit").click();
    }

    private HtmlPage addCustomer(String vat, String designation, String phone) throws Exception {
        // get a specific link
        HtmlAnchor addCustomerLink = page.getAnchorByHref("addCustomer.html");
        // click on it
        HtmlPage nextPage = (HtmlPage) addCustomerLink.openLinkInNewWindow();
        // check if title is the one expected
        assertEquals("Enter Name", nextPage.getTitleText());

        // get the page first form:
        HtmlForm addCustomerForm = nextPage.getForms().get(0);

        // place data at form
        HtmlInput vatInput = addCustomerForm.getInputByName("vat");
        vatInput.setValueAttribute(vat);
        HtmlInput designationInput = addCustomerForm.getInputByName("designation");
        designationInput.setValueAttribute(designation);
        HtmlInput phoneInput = addCustomerForm.getInputByName("phone");
        phoneInput.setValueAttribute(phone);
        // submit form
        return addCustomerForm.getInputByName("submit").click();
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
        HtmlPage customerInfoPage = getCustomerInfoPage(CUSTOMER_VAT);
        List<DomElement> addressListElements = customerInfoPage.getElementsById("address-list");
        int INITIAL_ROW_COUNT = 0;
        if (!addressListElements.isEmpty()) {
            INITIAL_ROW_COUNT = customerInfoPage.<HtmlTable>getHtmlElementById("address-list").getRowCount();
        }

        // add address to the customer
        HtmlAnchor addAddressLink = page.getAnchorByHref("addAddressToCustomer.html");
        HtmlPage nextPage = (HtmlPage) addAddressLink.openLinkInNewWindow();
        // check if title is the one expected
        assertEquals("Enter Address", nextPage.getTitleText());
        // get the page first form:
        HtmlForm addAddressForm = nextPage.getForms().get(0);
        // place data in the form
        addAddressForm.getInputByName("vat").setValueAttribute(String.valueOf(CUSTOMER_VAT));
        addAddressForm.getInputByName("address").setValueAttribute(ADDRESS);
        addAddressForm.getInputByName("door").setValueAttribute(DOOR);
        addAddressForm.getInputByName("postalCode").setValueAttribute(POSTAL_CODE);
        addAddressForm.getInputByName("locality").setValueAttribute(LOCALITY);
        // submit form
        HtmlPage reportPage = addAddressForm.getInputByName("submit").click();

        // check if the report page includes the proper values
        String textReportPage = reportPage.asText();
        assertTrue(textReportPage.contains(String.valueOf(CUSTOMER_VAT)));
        assertTrue(textReportPage.contains(ADDRESS));
        assertTrue(textReportPage.contains(DOOR));
        assertTrue(textReportPage.contains(POSTAL_CODE));
        assertTrue(textReportPage.contains(LOCALITY));

        // check the list of the customer's addresses
        HtmlPage updatedCustomerInfoPage = getCustomerInfoPage(CUSTOMER_VAT);

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

    private HtmlPage getCustomerInfoPage(String vat) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("vat", vat));
        return getPage(new URL(APPLICATION_URL + "GetCustomerPageController"), params);
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
        addCustomer(VALID_VATS[0], CUSTOMER_0_NAME, CUSTOMER_PHONE);
        addCustomer(VALID_VATS[1], CUSTOMER_1_NAME, CUSTOMER_PHONE);
        addCustomer(VALID_VATS[2], CUSTOMER_2_NAME, CUSTOMER_PHONE);

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
        removeCustomer(VALID_VATS[0]);
        removeCustomer(VALID_VATS[1]);
        removeCustomer(VALID_VATS[2]);
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
        HtmlPage customerSalePage = getCustomerSalePage(CUSTOMER_VAT);
        List<String> existingSales = new ArrayList<>();
        List<DomElement> sales = customerSalePage.getElementsById("sale-list");
        if (!sales.isEmpty()) {
            HtmlTable saleList = customerSalePage.getHtmlElementById("sale-list");
            // ignore the title row
            for (int i = 1; i < saleList.getRowCount(); i++) {
                existingSales.add(saleList.getRow(i).getCell(0).asText());
            }
        }

        // add new sale
        addSaleToCustomer(String.valueOf(CUSTOMER_VAT));

        // get updated sales of the customer
        HtmlPage updatedCustomerSalePage = getCustomerSalePage(CUSTOMER_VAT);
        HtmlTable saleList = updatedCustomerSalePage.getHtmlElementById("sale-list");
        int salesCount = saleList.getRowCount() - 1; // ignore title row
        assertEquals(1 + existingSales.size(), salesCount);

        for (int i = 1; i < saleList.getRowCount(); i++) {
            HtmlTableRow saleRow = saleList.getRow(i);
            String id = saleRow.getCell(0).asText();
            if (!existingSales.contains(id)) {
                assertEquals(CUSTOMER_VAT, saleRow.getCell(4).asText());   // Customer VAT
                assertEquals("O", saleRow.getCell(3).asText()); // Status
                assertEquals("0.0", saleRow.getCell(2).asText()); // Total
                return;
            }
        }
        fail("Did not find the added sale.");
    }

    private HtmlPage addSaleToCustomer(String vat) throws IOException {
        // add a new sale to the customer
        HtmlAnchor addSaleLink = page.getAnchorByHref("addSale.html");
        HtmlPage nextPage = (HtmlPage) addSaleLink.openLinkInNewWindow();

        // check if title is the one expected
        assertEquals("New Sale", nextPage.getTitleText());
        // get the page first form and place data in the form
        HtmlForm addSaleForm = nextPage.getForms().get(0);
        addSaleForm.getInputByName("customerVat").setValueAttribute(vat);
        // submit form
        return addSaleForm.getInputByName("submit").click();
    }

    private HtmlPage getCustomerSalePage(String vat) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("customerVat", vat));
        return getPage(new URL(APPLICATION_URL + "GetSalePageController"), params);
    }

    private HtmlPage getPage(URL url, List<NameValuePair> reqParams) throws IOException {
        HtmlPage page;
        // Build a GET request
        try (final WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
            WebRequest requestSettings = new WebRequest(url, HttpMethod.GET);
            // Set the request parameters
            requestSettings.setRequestParameters(reqParams);
            page = webClient.getPage(requestSettings);
        }
        return page;
    }

    /**
     * After closing a sale, it is listed as closed.
     */
    @Test
    public void closeSaleTest() throws Exception {

    }

    /**
     * After creating a new customer and a new sale for them, inserting a delivery for that sale, the sale delivery
     * contains all expected information. All intermediate pages have the expected information.
     */
    @Test
    public void saleDeliveryTest() throws Exception {

    }


}





