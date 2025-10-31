package Warehouse;

public class AlertService implements StockObserver {
    public void onLowStock(Product product){
        System.out.println("Restock Alert : Low Stock for " + product.getName() + "- only " + product.getQuantity() + " left!");
    }
}
