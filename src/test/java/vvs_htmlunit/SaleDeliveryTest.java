package vvs_htmlunit;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class SaleDeliveryTest {

    public static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

    private static final String CUSTOMER_VAT = "503183504";
    private static final String CUSTOMER_DESIGNATION = "FCUL";
    private static final String CUSTOMER_PHONE = "217500000";

    private static final String CUSTOMER_ADDRESS = "Rua Casal Ribeiro";
    private static final String CUSTOMER_ADDRESS_DOOR = "111";
    private static final String CUSTOMER_ADDRESS_POSTAL_CODE = "1234-123";
    private static final String CUSTOMER_ADDRESS_LOCALITY = "Lisboa";

    private static HtmlPage page;
    private List<NameValuePair> customerVatParams;

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
        WebappUtils.addAddressToCustomer(CUSTOMER_VAT, CUSTOMER_ADDRESS, CUSTOMER_ADDRESS_DOOR,
            CUSTOMER_ADDRESS_POSTAL_CODE, CUSTOMER_ADDRESS_LOCALITY, page);
        customerVatParams = new ArrayList<>();
        customerVatParams.add(new NameValuePair("vat", CUSTOMER_VAT));
    }

    @After
    public void tearDown() throws Exception {
        WebappUtils.removeCustomer(CUSTOMER_VAT, page);
    }

    /**
     * After creating a new customer, a new sale for them and inserting a delivery for that sale, the sale delivery
     * contains all expected information. All intermediate pages have the expected information.
     */
    @Test
    public void saleDeliveryTest() throws Exception {
        // add sale
        HtmlTableRow addedSale = WebappUtils.addAndGetSale(CUSTOMER_VAT, page);
        assertNotNull("Did not find the added sale.", addedSale);

        // get existing sale deliveries
        List<String> existingDeliveryIds = WebappUtils.getExistingDeliveryIds(CUSTOMER_VAT);

        HtmlPage addSaleDeliveryPage =
            WebappUtils.getPage(new URL(SaleDeliveryTest.APPLICATION_URL + "AddSaleDeliveryPageController"), customerVatParams);
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
        HtmlTable saleDeliveryTable = WebappUtils.getPage(
            new URL(SaleDeliveryTest.APPLICATION_URL + "GetSaleDeliveryPageController"), customerVatParams)
            .getHtmlElementById("sale-delivery-list");
        String addedDeliveryId = updatedDeliveryIds.get(0);
        List<HtmlTableRow> deliveries = saleDeliveryTable.getRows().stream()
            .filter(r -> r.getCell(0).asText().equals(addedDeliveryId))
            .collect(Collectors.toList());
        assertEquals(1, deliveries.size());

        assertEquals(addedSaleId, deliveries.get(0).getCell(1).asText());
        assertEquals(addressId, deliveries.get(0).getCell(2).asText());
    }
}
