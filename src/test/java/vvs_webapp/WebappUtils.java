package vvs_webapp;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Assert;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WebappUtils {
    public WebappUtils() {
    }

    public static HtmlPage getPage(URL url, List<NameValuePair> reqParams) throws IOException {
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

    public static HtmlPage getCustomerInfoPage(String vat) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("vat", vat));
        return getPage(new URL(WebappTest.APPLICATION_URL + "GetCustomerPageController"), params);
    }

    public static HtmlPage getCustomerSalePage(String vat) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("customerVat", vat));
        return getPage(new URL(WebappTest.APPLICATION_URL + "GetSalePageController"), params);
    }

    public static List<String> getExistingSaleIds(String vat) throws IOException {
        HtmlPage customerSalePage = WebappUtils.getCustomerSalePage(vat);
        List<String> existingSales = new ArrayList<>();
        List<DomElement> sales = customerSalePage.getElementsById("sale-list");
        if (!sales.isEmpty()) {
            HtmlTable saleList = customerSalePage.getHtmlElementById("sale-list");
            // ignore the title row
            for (int i = 1; i < saleList.getRowCount(); i++) {
                existingSales.add(saleList.getRow(i).getCell(0).asText());
            }
        }
        return existingSales;
    }

    public static HtmlPage addCustomer(String vat, String designation, String phone, HtmlPage page) throws Exception {
        // get a specific link
        HtmlAnchor addCustomerLink = page.getAnchorByHref("addCustomer.html");
        // click on it
        HtmlPage nextPage = (HtmlPage) addCustomerLink.openLinkInNewWindow();
        // check if title is the one expected
        Assert.assertEquals("Enter Name", nextPage.getTitleText());

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

    public static HtmlPage removeCustomer(String vat, HtmlPage page) throws Exception {
        // at index, goto Remove case and remove the previous client
        HtmlAnchor removeCustomerLink = page.getAnchorByHref("RemoveCustomerPageController");
        HtmlPage nextPage = (HtmlPage) removeCustomerLink.openLinkInNewWindow();

        HtmlForm removeCustomerForm = nextPage.getForms().get(0);
        removeCustomerForm.getInputByName("vat").setValueAttribute(vat);
        return removeCustomerForm.getInputByName("submit").click();
    }

    public static HtmlPage addSaleToCustomer(String vat, HtmlPage page) throws IOException {
        // add a new sale to the customer
        HtmlAnchor addSaleLink = page.getAnchorByHref("addSale.html");
        HtmlPage nextPage = (HtmlPage) addSaleLink.openLinkInNewWindow();

        // check if title is the one expected
        Assert.assertEquals("New Sale", nextPage.getTitleText());
        // get the page first form and place data in the form
        HtmlForm addSaleForm = nextPage.getForms().get(0);
        addSaleForm.getInputByName("customerVat").setValueAttribute(vat);
        // submit form
        return addSaleForm.getInputByName("submit").click();
    }
}