# 🏭 Event-Driven Warehouse Inventory Management System (Java)

## 📘 Overview

This project simulates a **real-world warehouse inventory management system** built using **Java** and the **Observer Design Pattern**.  
It maintains an **in-memory catalog** of products, automatically monitors stock levels, and triggers **real-time restock alerts** when inventory drops below critical thresholds.

The system demonstrates:
- Robust **object-oriented design (OOP)**
- **Event-based notifications** using the Observer pattern
- **Thread-safe** inventory operations
- **File-based persistence**
- **Multi-warehouse support** with centralized reporting

---

## 🧩 System Components

### 1. `Product` Class
Encapsulates product data and logic.
- Fields: `id`, `name`, `quantity`, `reorderThreshold`
- Methods for increasing/decreasing quantity
- Enforces **encapsulation** and **validation**

### 2. `Warehouse` Class
Acts as the central inventory hub.
- Adds products, receives shipments, fulfills orders
- Detects low-stock events
- Notifies observers (via `StockObserver`)
- Uses `ReentrantReadWriteLock` for **thread safety**

### 3. `StockObserver` Interface
Implements the **Observer pattern** to trigger alerts when stock levels fall below the threshold.

### 4. `WarehouseIO` Class
Handles **persistence**:
- Saves inventory data to a text file (`.txt`)
- Loads data back into memory on restart

### 5. `WarehouseRegistry` Class
Supports **multiple warehouses** and generates **centralized inventory reports**.

---

## ⚙️ Technologies Used
| Component | Technology |
|------------|-------------|
| Language | Java 17+ |
| Design Pattern | Observer |
| Data Structure | HashMap, CopyOnWriteArrayList |
| Concurrency | ReentrantReadWriteLock |
| Persistence | File I/O (Text-based) |
| Alert System | Console-based Event Notification |

---

## 🚀 How to Run

### 1️⃣ Compile
```bash
javac Warehouse.java
