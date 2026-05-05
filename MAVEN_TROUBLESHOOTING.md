# 🔧 Maven Error - Troubleshooting Guide

## Error Details
```
[ERROR] Failed to execute goal org.openjfx:javafx-maven-plugin:0.0.8:run
[ERROR] Error: Command execution failed. Process exited with an error: 1
```

## Root Causes (Check These)

### 1. **FXML File Errors** ✓ FIXED
- ~~StackPane nesting issue~~ → **RESOLVED**
- ~~Incorrect alignment attributes~~ → **RESOLVED**

### 2. **Missing Module Exports in pom.xml**
The JavaFX maven plugin might need additional configuration.

### 3. **Java Module Issues**
JavaFX requires proper module path configuration.

## Quick Fix Steps

### Step 1: Update pom.xml (Add Module Support)

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
            </configuration>
        </plugin>

        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>org.example.CuraVitaApp</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Step 2: Clean Build

```bash
# In terminal:
cd C:\Users\ihebj\OneDrive\Bureau\Projet_java
mvn clean
mvn compile
```

### Step 3: Try Running Again

```bash
mvn javafx:run
```

## Alternative: Run Directly from IntelliJ

### Option A: Run Configuration
1. Go to `Run → Edit Configurations`
2. Create new Maven configuration
3. Set:
   - **Name**: "CuraVita"
   - **Working directory**: Project root
   - **Command line**: `javafx:run`
4. Click Run

### Option B: Run Main Class Directly
1. Right-click `CuraVitaApp.java`
2. Select `Run 'CuraVitaApp'`

### Option C: Run Gradle (Alternative)
If Maven continues to fail, convert to Gradle (requires setup).

## If Still Getting Error 1

### Check Compilation:
```bash
mvn clean compile -X
```
This will show detailed errors.

### Look for:
- Java syntax errors
- FXML binding errors
- Missing imports
- Resource loading issues

### Common Issues:

**Issue**: FXML not found
**Fix**: Ensure files are in `src/main/resources/fxml/`

**Issue**: CSS not found
**Fix**: Ensure `styles.css` is in `src/main/resources/css/`

**Issue**: Module not found
**Fix**: Add to pom.xml:
```xml
<configuration>
    <modules>
        <module>javafx.controls</module>
        <module>javafx.fxml</module>
        <module>javafx.graphics</module>
    </modules>
</configuration>
```

**Issue**: ClassNotFoundException
**Fix**: 
- Clean: `mvn clean`
- Rebuild: `mvn compile`
- Ensure all Java files compile without errors

## Files to Verify

✅ Check these files exist:
```
src/main/java/org/example/
  ├── CuraVitaApp.java
  ├── controller/
  │   ├── AccueilController.java
  │   └── DashboardController.java
  ├── model/ (6 model classes)
  └── service/ (6 service classes)

src/main/resources/
  ├── fxml/
  │   ├── Accueil.fxml
  │   └── Dashboard.fxml
  └── css/
      └── styles.css

pom.xml (at root)
```

## Nuclear Option: Complete Rebuild

```bash
# 1. Clean everything
mvn clean

# 2. Remove build artifacts
rmdir target /s /q
rmdir src\main\java\..\.. /s /q

# 3. Rebuild from scratch
mvn compile
mvn package -DskipTests

# 4. Try running
mvn javafx:run
```

## Debug with Full Logging

```bash
# This will show EVERYTHING
mvn -X javafx:run 2>&1 | Out-File -FilePath debug_log.txt

# Then search log for actual error
Select-String "ERROR" debug_log.txt
```

## If All Else Fails

### Create Simple Test App:
1. Create minimal test to verify JavaFX works
2. Gradually add components
3. Identify which part breaks

### Use Gradle Instead:
- Maven issues are common with JavaFX
- Gradle is often more reliable
- Consider: `gradle build run`

## SUCCESS SIGNS

When it works, you'll see:
✅ BUILD SUCCESS
✅ Window opens with navbar
✅ Profile dropdown works
✅ Navigation buttons respond

## Still Stuck?

1. **Check Java version**: `java -version` (Should be 17+)
2. **Check Maven version**: `mvn --version`
3. **Verify pom.xml**: No typos or syntax errors
4. **Check resources**: FXML/CSS files are readable
5. **Review error log**: `-X` flag shows actual problem

---

**Last Updated**: April 11, 2026
**Status**: Apply these fixes then try running again

