package vvs_htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NewSaleTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

    private static final String CUSTOMER_VAT = "503183504";
    private static final String CUSTOMER_DESIGNATION = "FCUL";
    private static final String CUSTOMER_PHONE = "217500000";
    private static final String SALE_STATUS_OPEN = "O";

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
}
