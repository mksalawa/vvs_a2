package vvs_htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class NewAddressTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

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
}
