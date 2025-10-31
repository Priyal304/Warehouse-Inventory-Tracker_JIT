package Warehouse;
import java.util.*;
class Warehouse {
    private Map<Integer, Product>  products = new HashMap<>();
    private List<StockObserver> observers = new ArrayList<>();

    public void addObserver(StockObserver observer){
        observers.add(observer);
    }
    
    // Add new Product
    public void addProduct(Product product){
        if(products.containsKey(product.getId())){
            System.out.println("Product with ID " + product.getId() + "already exists!");
            return;
        }
        products.put(product.getId(), product);
        System.out.println("Added Product : "+product.getName());
    }

    // Receive Shipment (Increase stock)
    public void receiveShipment(int productId, int quantity){
        Product product = products.get(productId);
        if(product == null){
            System.out.println("Invalid Product ID!");
            return;
        }
        product.setQuantity(product.getQuantity() + quantity);
        System.out.println("Received Shipment: " + quantity + " units of " + product.getName() + "(Total: "+ product.getQuantity() + ")");
    }

    //Fullfill Order (decrease stock)
    public void fulfillOrder(int productId, int quantity){
        Product product = products.get(productId);
        if(product == null){
            System.out.println("Invalid Product ID!");
            return;
        }

        if(product.getQuantity() < quantity){
            System.out.println("Insufficient stock for " + product.getName());
            return;
        }

        product.setQuantity(product.getQuantity() - quantity);
        System.out.println("Fulfilled order : " + quantity + "units of "+product.getName() + "(Remaining : " + product.getQuantity() + ")");
    
       if(product.getQuantity() < product.getThreshold()){
          notifyObservers(product);
       }   
    }

    // Notify Observers
    private void notifyObservers(Product product){
        for(StockObserver observer : observers){
            observer.onLowStock(product);
        }
    }


    public void viewAllProducts(){
        if(products.isEmpty()){
            System.out.println("No products available in inventory.");
            return;
        }
        
        System.out.printf("%-5s %-20s %-10s %-10s", "ID", "Name", "Quantity", "Threshold");
        System.out.println("\n------------------------------------------------------------");
        
        for(Product p : products.values()){
           System.out.printf("%-5d %-20s %-10d %-10d%n", 
                          p.getId(), p.getName(), p.getQuantity(), p.getThreshold());
        }
    }
}
