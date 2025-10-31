package Warehouse;
import java.util.Scanner;
public class Main {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        Warehouse warehouse = new Warehouse();
        warehouse.addObserver(new AlertService());

        System.out.println("===== Warehouse Inventory Tracker =====");

        while(true){
            System.out.println("\nChoose an operation: ");
            System.out.println("1. Add Product");
            System.out.println("2. Receive Shipment");
            System.out.println("3. Fullfill order");
            System.out.println("4. View All Products");
            System.out.println("5. Exit (Leave the Wavehouse)");
            System.out.print("Enter Choice: ");

            int choice = sc.nextInt();
            sc.nextLine(); // consume newLine(enter symbol after int);

            switch(choice){
                case 1: 
                   System.out.print("Enter Product ID: ");
                   int id = sc.nextInt();
                   sc.nextLine(); // consume newline
                   System.out.print("Enter Product Name: ");
                   String name = sc.nextLine();
                   System.out.print("Enter Initial Quantity: ");
                   int qty = sc.nextInt(); 
                   System.out.print("Enter Reorder Threshold: ");
                   int threshold = sc.nextInt();
                   sc.nextLine(); 

                   warehouse.addProduct(new Product(id, name, qty, threshold));
                   break;

                case 2: 
                   System.out.print("Enter Product ID: ");
                   int shipId = sc.nextInt();
                   System.out.println("Enter Quantity to Add: ");
                   int shipQty = sc.nextInt();
                   warehouse.receiveShipment(shipId, shipQty);
                   break;
                
                case 3: 
                    System.out.print("Enter Product ID: ");
                    int orderId = sc.nextInt();
                    System.out.print("Enter Quantity to Fulfill: ");
                    int orderQty = sc.nextInt();
                    warehouse.fulfillOrder(orderId, orderQty);
                    break;

                case 4:
                    warehouse.viewAllProducts();
                    break;
                
                case 5: 
                    System.out.println("Exiting....");
                    sc.close();
                    System.exit(0);
                
                default:
                   System.out.println("Invalid choice! Try again.");
            }
        }


    }
}
