package vvs_htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NewCustomerTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";
    private static final String[] VALID_VATS = new String[] {
        "108136701",
        "108136710",
        "108136728",
        "108136736",
        "108136744",
    };

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
}
