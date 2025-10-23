import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InventoryApp {
    public static void main(String[] args) throws Exception {
        StockObserver consoleAlert = (product, warehouse) -> {
            System.out.println("[ALERT] " + LocalDateTime.now() + " :: "
                    + "Low stock for " + product.getName()
                    + " - only " + product.getQuantity() + " left! (Warehouse: " + warehouse.getName() + ")");
        };
        Warehouse mumbaiWH = new Warehouse("Mumbai");
        mumbaiWH.registerObserver(consoleAlert);
        mumbaiWH.addProduct(new Product("P-1001", "Laptop", 0, 5));
        mumbaiWH.receiveShipment("P-1001", 10);
        mumbaiWH.fulfillOrder("P-1001", 6);  

        System.out.println("\nSnapshot after demo:");
        System.out.println(mumbaiWH.describeInventory());

        System.out.println("\n[Concurrency demo]");
        Runnable shipper = () -> {
            try { mumbaiWH.receiveShipment("P-1001", 50); }
            catch (Exception e) { System.out.println("Shipper error: " + e.getMessage()); }
        };
        Runnable buyer = () -> {
            try { mumbaiWH.fulfillOrder("P-1001", 40); }
            catch (Exception e) { System.out.println("Buyer error: " + e.getMessage()); }
        };
        Thread t1 = new Thread(shipper, "Shipper");
        Thread t2 = new Thread(buyer, "Buyer");
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println(mumbaiWH.describeInventory());
        System.out.println("\n[Persistence demo]");
        try {
            Path file = Files.createTempFile("mumbai_inventory_", ".txt");
            WarehouseIO.save(mumbaiWH, file);
            Warehouse mumbaiReloaded = WarehouseIO.load("MumbaiReloaded", file);
            System.out.println("Saved to: " + file.toAbsolutePath());
            System.out.println("Reloaded inventory:");
            System.out.println(mumbaiReloaded.describeInventory());
        } catch (Exception e) {
            System.out.println("Persistence not supported in this environment. Skipping. Reason: " + e.getMessage());
        }

        Warehouse delhiWH = new Warehouse("Delhi");
        delhiWH.registerObserver(consoleAlert);
        delhiWH.addProduct(new Product("P-1001", "Laptop", 3, 5));
        delhiWH.addProduct(new Product("P-2002", "Mouse", 25, 10));

        WarehouseRegistry registry = new WarehouseRegistry();
        registry.addWarehouse(mumbaiWH);
        registry.addWarehouse(delhiWH);

        System.out.println("\n[Centralized report across warehouses]");
        System.out.println(registry.generateReport());
    }
}
class InventoryException extends RuntimeException {
    public InventoryException(String message) { super(message); }
}
class Product {
    private final String id;
    private final String name;
    private int quantity;
    private final int reorderThreshold;

    public Product(String id, String name, int initialQty, int reorderThreshold) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Product id is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Product name is required.");
        if (initialQty < 0) throw new IllegalArgumentException("Initial quantity cannot be negative.");
        if (reorderThreshold < 0) throw new IllegalArgumentException("Reorder threshold cannot be negative.");
        this.id = id;
        this.name = name;
        this.quantity = initialQty;
        this.reorderThreshold = reorderThreshold;
    }

    public synchronized void increase(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Increase amount must be positive.");
        if ((long) quantity + amount > Integer.MAX_VALUE) {
            throw new InventoryException("Quantity overflow.");
        }
        quantity += amount;
    }

    public synchronized void decrease(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Decrease amount must be positive.");
        if (amount > quantity) {
            throw new InventoryException("Insufficient stock. Available=" + quantity + ", requested=" + amount);
        }
        quantity -= amount;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public synchronized int getQuantity() { return quantity; }
    public int getReorderThreshold() { return reorderThreshold; }

    public synchronized String toCsv() {
        String safeName = name.replace(",", "\\,");
        return id + "," + safeName + "," + quantity + "," + reorderThreshold;
    }

    public static Product fromCsv(String csvLine) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < csvLine.length(); i++) {
            char c = csvLine.charAt(i);
            if (escape) {
                cur.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == ',') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        if (parts.size() != 4) throw new IllegalArgumentException("Bad CSV: " + csvLine);
        String id = parts.get(0);
        String name = parts.get(1);
        int qty = Integer.parseInt(parts.get(2));
        int thr = Integer.parseInt(parts.get(3));
        return new Product(id, name, qty, thr);
    }

    @Override public String toString() {
        return "Product{id='" + id + "', name='" + name + "', qty=" + getQuantity()
                + ", reorderThreshold=" + reorderThreshold + "}";
    }
}
@FunctionalInterface
interface StockObserver {
    void onLowStock(Product product, Warehouse warehouse);
}

class Warehouse {
    private final String name;
    private final Map<String, Product> catalog = new HashMap<>();
    private final List<StockObserver> observers = new CopyOnWriteArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Warehouse(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Warehouse name required.");
        this.name = name;
    }

    public String getName() { return name; }

    public void registerObserver(StockObserver observer) {
        if (observer == null) throw new IllegalArgumentException("Observer cannot be null.");
        observers.add(observer);
    }

    public void addProduct(Product product) {
        lock.writeLock().lock();
        try {
            Objects.requireNonNull(product, "Product cannot be null.");
            if (catalog.containsKey(product.getId())) {
                throw new InventoryException("Product ID already exists: " + product.getId());
            }
            catalog.put(product.getId(), product);
        } finally {
            lock.writeLock().unlock();
        }
        checkAndNotifyIfLow(product);
    }
    public void receiveShipment(String productId, int amount) {
        Product p = getExisting(productId);
        p.increase(amount);
        checkAndNotifyIfLow(p);
    }
    public void fulfillOrder(String productId, int amount) {
        Product p = getExisting(productId);
        p.decrease(amount);
        checkAndNotifyIfLow(p);
    }
    private Product getExisting(String productId) {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("Product ID required.");
        lock.readLock().lock();
        try {
            Product p = catalog.get(productId);
            if (p == null) throw new InventoryException("No such product: " + productId);
            return p;
        } finally {
            lock.readLock().unlock();
        }
    }
    private void checkAndNotifyIfLow(Product p) {
        boolean low;
        int qty = p.getQuantity(); 
        low = qty <= p.getReorderThreshold();
        if (low) {
            for (StockObserver obs : observers) {
                try { obs.onLowStock(p, this); }
                catch (Exception ignored) { }
            }
        }
    }
    public String describeInventory() {
        lock.readLock().lock();
        try {
            if (catalog.isEmpty()) return "(empty)";
            StringBuilder sb = new StringBuilder();
            sb.append("Warehouse ").append(name).append(" inventory:\n");
            catalog.values().stream()
                    .sorted(Comparator.comparing(Product::getId))
                    .forEach(p -> sb.append(" - ").append(p.toString()).append("\n"));
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
    public List<Product> listProductsSnapshot() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(catalog.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void putOrReplace(Product p) {
        lock.writeLock().lock();
        try {
            catalog.put(p.getId(), p);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
class WarehouseIO {
    public static void save(Warehouse w, Path file) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("# Warehouse:" + w.getName());
            for (Product p : w.listProductsSnapshot()) {
                lines.add(p.toCsv());
            }
            Files.write(file, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new InventoryException("Failed to save: " + e.getMessage());
        }
    }

    public static Warehouse load(String warehouseName, Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            Warehouse w = new Warehouse(warehouseName);
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) continue;
                Product p = Product.fromCsv(line);
                w.putOrReplace(p);
            }
            return w;
        } catch (IOException e) {
            throw new InventoryException("Failed to load: " + e.getMessage());
        }
    }
}
class WarehouseRegistry {
    private final Map<String, Warehouse> warehouses = new HashMap<>();

    public synchronized void addWarehouse(Warehouse w) {
        Objects.requireNonNull(w, "Warehouse cannot be null.");
        if (warehouses.containsKey(w.getName())) {
            throw new InventoryException("Duplicate warehouse name: " + w.getName());
        }
        warehouses.put(w.getName(), w);
    }

    public synchronized Warehouse get(String name) {
        Warehouse w = warehouses.get(name);
        if (w == null) throw new InventoryException("Unknown warehouse: " + name);
        return w;
    }
    public synchronized Map<String, Integer> totalStockByProduct() {
        Map<String, Integer> totals = new HashMap<>();
        for (Warehouse w : warehouses.values()) {
            for (Product p : w.listProductsSnapshot()) {
                totals.merge(p.getId(), p.getQuantity(), Integer::sum);
            }
        }
        return totals;
    }
    public synchronized String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Central Inventory Report ===\n");
        for (Warehouse w : warehouses.values()) {
            sb.append(w.describeInventory()).append("\n");
        }
        sb.append("Aggregated totals:\n");
        Map<String, Integer> totals = totalStockByProduct();
        totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(" - ").append(e.getKey())
                        .append(" -> totalQty=").append(e.getValue()).append("\n"));
        return sb.toString();
    }
}