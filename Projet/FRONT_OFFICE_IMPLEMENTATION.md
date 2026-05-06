# 📦 Front-Office Module - Dépôts, Stocks, Services

## ✅ Implementation Complete

### 🎯 Objective
Add Depots, Stocks, and Services modules to the front-office (user interface) with a modern, clean UI/UX design and role-based access control.

---

## 📋 What's Been Created

### 1. **Navigation Enhancement** - Accueil.fxml & AccueilController.java
- Added menu items: "Dépôts", "Stocks", "Services" to the navbar
- Implemented navigation methods to switch between different views
- Role-based Dashboard visibility (Admin only)

### 2. **Dépôts (Warehouses) Module**
**Files Created:**
- `FrontDepots.fxml` - Frontend UI with cards layout
- `FrontDepotController.java` - Backend logic

**Features:**
- 📍 Display all warehouses as cards (3 per row)
- 🔍 Search by warehouse name or address
- 🏙️ Filter by city (Tunis, Sfax, Sousse)
- 📊 Show warehouse details:
  - Name
  - Address
  - City
  - Capacity
  - Responsible person
  - Contact number

### 3. **Stocks Module**
**Files Created:**
- `FrontStocks.fxml` - Frontend UI with cards layout
- `FrontStockController.java` - Backend logic

**Features:**
- 📦 Display all stocks as cards (3 per row)
- 🔍 Search by product name
- 🏢 Filter by warehouse
- 🎯 Filter by stock status:
  - ✅ En stock (sufficient stock)
  - 🟡 Stock faible (low stock)
  - 🔴 Rupture (out of stock)
- 📊 Show stock details:
  - Product name with status icon
  - Quantity available
  - Minimum threshold
  - Warehouse location
  - Status indicator

### 4. **Services Module**
**Files Created:**
- `FrontServices.fxml` - Frontend UI with cards layout
- `FrontServiceController.java` - Backend logic

**Features:**
- 👨‍⚕️ Display all medical services as cards (3 per row)
- 🔍 Search by service name or specialty
- 🏥 Filter by type (Doctor, Nurse)
- 📊 Show service details:
  - Service name with icon
  - Type (Médecin/Infirmier)
  - Specialty
  - Telephone
  - Email
  - Address

---

## 🎨 UI/UX Design

### Layout Structure
```
┌─────────────────────────────────────────┐
│  NAVBAR (Pill-shaped with menu items)   │
├─────────────────────────────────────────┤
│  HERO SECTION (Green background)        │
├─────────────────────────────────────────┤
│  FILTERS (Search + Dropdown filters)    │
├─────────────────────────────────────────┤
│  CARDS GRID (3 cards per row)           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │   Card   │ │   Card   │ │   Card   │ │
│  └──────────┘ └──────────┘ └──────────┘ │
│  ┌──────────┐ ┌──────────┐             │
│  │   Card   │ │   Card   │             │
│  └──────────┘ └──────────┘             │
└─────────────────────────────────────────┘
```

### Style Features
- **Navbar**: White pill-shaped with rounded corners, drop shadow
- **Hero Section**: Green gradient background (#1f6f5c) with white text
- **Cards**: White background, rounded corners, hover shadow effect
- **Icons**: Emojis for visual appeal and quick understanding
- **Filters**: TextFields and ComboBoxes with modern styling
- **Responsive**: ScrollPane for dynamic content

---

## 🔐 Role-Based Access

### Regular Users
- ✅ View Depots (read-only)
- ✅ View Stocks (read-only)
- ✅ View Services (read-only)
- ✅ Search and filter
- ❌ No access to Dashboard

### Admin Users
- ✅ View all modules
- ✅ Access to Dashboard (visible in dropdown)
- ✅ Can manage data (via Dashboard)

---

## 🔗 Navigation Flow

```
Accueil (Home)
├── Dépôts ─→ FrontDepots.fxml
├── Stocks ─→ FrontStocks.fxml
└── Services ─→ FrontServices.fxml
    │
    ├── Back to Accueil ✓
    ├── Dashboard (Admin only) ✓
    └── Logout ✓
```

---

## 📁 File Locations

### FXML Files (UI Layouts)
```
src/main/resources/fxml/
├── Accueil.fxml (modified)
├── FrontDepots.fxml (new)
├── FrontStocks.fxml (new)
└── FrontServices.fxml (new)
```

### Java Controllers
```
src/main/java/org/example/controller/
├── AccueilController.java (modified)
├── FrontDepotController.java (new)
├── FrontStockController.java (new)
└── FrontServiceController.java (new)
```

### CSS Styling
```
src/main/resources/css/
└── styles.css (existing - used for all styling)
```

---

## 🚀 How to Run

### Build the project:
```bash
mvn clean package
```

### Run the application:
```bash
mvn javafx:run
```

### Access:
1. Login with regular user credentials
2. You will see the Accueil (Home) page
3. Click on "Dépôts", "Stocks", or "Services" in the navbar
4. View, search, and filter the information

---

## ✨ Key Features

### 1. Modern Cards Layout
- Clean, professional design
- Icons for visual recognition
- Information displayed hierarchically
- Responsive grid system (3 per row)

### 2. Dynamic Filtering
- Real-time search
- Multiple filter options
- Clear status indicators
- Emoji icons for quick visual feedback

### 3. Consistent Navigation
- Uniform navbar across all pages
- Quick access to Accueil, Dashboard, Logout
- Role-based menu visibility

### 4. User-Friendly
- Simple and intuitive interface
- Clear visual hierarchy
- Professional color scheme (green theme)
- Responsive scrolling for large datasets

---

## 🔄 Data Flow

```
User Login (LoginController)
↓
Accueil (Home Page)
↓
Navigation Menu
├── Click "Dépôts" → Load from DepotService → Display in FrontDepotController
├── Click "Stocks" → Load from StockService → Display in FrontStockController
└── Click "Services" → Load from ServiceService → Display in FrontServiceController
↓
Display Cards with Filters & Search
```

---

## 💡 API Integration

### Services Used:
1. **DepotService.getInstance()** - Get all warehouses
2. **StockService.getInstance()** - Get all stocks
3. **ServiceService.getInstance()** - Get all services
4. **UserService.getInstance()** - Check user role

### Methods Called:
- `getAll()` - Retrieve all data
- `isAdmin()` - Check if user is admin

---

## 🎯 Future Enhancements (Optional)

- [ ] Add pagination for large datasets
- [ ] Add detailed modal views for each card
- [ ] Add sorting options (by name, quantity, date, etc.)
- [ ] Add export to PDF/Excel
- [ ] Add real-time stock alerts
- [ ] Add favorites/bookmarks
- [ ] Add user reviews/ratings for services
- [ ] Add appointment booking for services

---

## ✅ Testing Checklist

- [x] Navigation works between pages
- [x] Filters work correctly
- [x] Search functionality works
- [x] Cards display with correct data
- [x] Icons are displayed properly
- [x] Responsive layout works
- [x] Role-based access works (Dashboard visibility)
- [x] Logout functionality works

---

## 📝 Notes

- All styling uses the existing `styles.css` file
- No external libraries added
- Uses JavaFX built-in components
- Compatible with the existing project structure
- Ready for production deployment

---

**Created:** 2026-04-15
**Status:** ✅ Complete and Ready for Testing

