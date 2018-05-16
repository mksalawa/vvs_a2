package webapp.services;

import java.util.List;

public class SalesDeliveryDTO {
	public final List<SaleDeliveryDTO> sales_delivery;

	public SalesDeliveryDTO(List<SaleDeliveryDTO> sales_delivery) {
		this.sales_delivery = sales_delivery;
	}
}
