# 🚀 QUICK START - Run CuraVita Now

## What's Fixed ✅
- AccueilController updated
- FXML layout corrected
- pom.xml enhanced with JavaFX modules
- CSS styling completed

## RUN IN 3 STEPS

### Step 1: Open Terminal
```
Press: Windows Key + R
Type: cmd
Press: Enter
```

### Step 2: Navigate to Project
```bash
cd C:\Users\ihebj\OneDrive\Bureau\Projet_java
```

### Step 3: Run Application
```bash
# Build and run
mvn clean javafx:run

# OR just run
mvn javafx:run
```

## ALTERNATIVE: Use IntelliJ GUI

1. **Open IntelliJ**
2. **File → Open → Select Project**
3. **Top Right: Select Maven Configuration**
   - Choose or create "Run CuraVita"
4. **Click Green Play Button** ▶️

## What You'll See

✨ Application launches with:
- Modern white pill-shaped navbar (top)
- Green "CuraVita" logo
- Menu items: Accueil, Produits, Commandes, etc.
- Search bar + icons
- Profile dropdown (click 👤)
- Minimal hero section (dark green background)

## Test Profile Dropdown

1. Click the profile button (👤)
2. Menu appears with:
   - Profil
   - Dashboard
   - Logout
3. Click "Dashboard" → goes to back office

## Test Navigation

- **To Dashboard**: Click "Dashboard" in profile dropdown
- **Back to Home**: Click "Accueil" button (bottom of sidebar)

## If It Doesn't Work

### Error: "Command execution failed"
```bash
# Try cleaning first
mvn clean

# Then compile
mvn compile

# Then run
mvn javafx:run
```

### Error: "Maven not found"
- Use IntelliJ's bundled Maven (GUI method)
- Or install Maven: https://maven.apache.org/

### Error: Resource not found
- Verify files exist:
  ```bash
  dir src\main\resources\fxml\
  dir src\main\resources\css\
  ```

### Error: Class not found
- Right-click project → Reload Maven

## SUCCESS INDICATORS ✅
- Window opens (1400x900)
- Title: "CuraVita - Gestion de Pharmacie"
- Navbar visible and styled
- Profile dropdown works
- Dashboard accessible
- No errors in console

## ENJOY! 🎉

Your CuraVita pharmacy management system is ready to use!

---
For more details, see: FIXES_APPLIED.md, MAVEN_TROUBLESHOOTING.md

