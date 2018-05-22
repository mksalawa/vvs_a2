package vvs_webapp;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class CloseSaleTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

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
