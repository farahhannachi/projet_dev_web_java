# 🚀 How to Run CuraVita JavaFX Application

## Fixed Issues
✅ AccueilController updated to match new minimal navbar design
✅ Profile dropdown functionality added
✅ All FXML and CSS files verified
✅ Project structure complete

## Running the Application

### Option 1: Using IntelliJ Built-in Maven (RECOMMENDED)

1. **Open IntelliJ IDEA**
   - Go to `File → Open` and select your project folder

2. **Configure Run Configuration**
   - Click `Run → Edit Configurations...`
   - Click the `+` button to add a new configuration
   - Select `Maven` from the dropdown

3. **Setup Maven Configuration**
   - **Name**: `Run CuraVita`
   - **Working directory**: `C:\Users\ihebj\OneDrive\Bureau\Projet_java`
   - **Command line**: `javafx:run`
   - Leave other fields as default

4. **Click OK and Run**
   - Select `Run CuraVita` from the top right dropdown
   - Click the green Run button (or press Shift+F10)

### Option 2: Using Terminal

```bash
# Navigate to project folder
cd C:\Users\ihebj\OneDrive\Bureau\Projet_java

# Compile and run (IntelliJ Maven wrapper)
# Use full path if mvn is not in PATH
"C:\Program Files\IntelliJ IDEA Community Edition\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
```

### Option 3: Using IDE Play Button

1. Right-click on `CuraVitaApp.java` in the project tree
2. Select `Run 'CuraVitaApp'` or `Run 'CuraVitaApp.main()'`
3. The application should launch

## If You Get Errors

### Error: Maven not found
- Use IntelliJ's bundled Maven (Option 1 above)
- Or install Maven: https://maven.apache.org/download.cgi

### Error: FXML file not found
- Ensure all FXML files are in `src/main/resources/fxml/`
  - Accueil.fxml ✓
  - Dashboard.fxml ✓

### Error: Styles not loading
- Ensure `styles.css` is in `src/main/resources/css/`

### Error: ClassNotFoundException
- Clean and rebuild:
  - `Build → Clean Project`
  - `Build → Build Project`

## Application Features

### Front Office (Accueil Page)
- **Slim navbar** with pill shape (50px height)
- **Profile dropdown** - Click profile icon (👤)
  - Dashboard button → Go to back office
  - Profil → Placeholder
  - Logout → Placeholder
- **Search bar** with icon button
- **Minimal hero section** with title and subtitle

### Back Office (Dashboard)
- **Modern sidebar** with menu items
- **"Accueil" button** at bottom to return to front office
- **Stats cards** showing:
  - Total Clients
  - Products
  - Orders
  - Stock Alerts
- **Quick actions** for:
  - New Client
  - Add Product
  - New Order

### Navigation
- Accueil → Click "Dashboard" in profile dropdown
- Dashboard → Click "Accueil" button in sidebar

## Project Structure
```
Projet_java/
├── src/main/java/org/example/
│   ├── CuraVitaApp.java (Main entry point)
│   ├── model/ (Client, Produit, Commande, Stock, etc.)
│   ├── service/ (CRUD services)
│   └── controller/ (AccueilController, DashboardController)
├── src/main/resources/
│   ├── fxml/ (Accueil.fxml, Dashboard.fxml)
│   └── css/ (styles.css)
└── pom.xml (Dependencies & plugins)
```

## Troubleshooting

1. **Clean rebuild**
   ```bash
   mvn clean compile
   ```

2. **Check Java version**
   ```bash
   java -version
   # Should be Java 17 or higher
   ```

3. **Verify all files exist**
   ```bash
   # Check Java classes
   dir src\main\java\org\example\

   # Check resources
   dir src\main\resources\
   ```

## Success!
If the application launches with:
- White pill-shaped navbar at the top
- Hero section with green background
- Profile dropdown working
- Able to navigate to Dashboard

Then everything is working correctly! 🎉

