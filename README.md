# CuraVita - Pharmacy Management System

JavaFX desktop application for pharmacy management.

- **Java**: 17
- **JavaFX**: 21.0.1
- **Architecture**: MVC

## Features

- Front Office: Homepage with product search and display
- Back Office: Dashboard with stats, clients, products, orders management
- Modern UI with CSS styling

## Running the Application

Ensure Java 17+ is installed and JAVA_HOME is set.

```bash
mvn clean compile
mvn javafx:run
```

## Project Structure

- `src/main/java/org/example/model/` - Data models
- `src/main/java/org/example/service/` - Business logic services
- `src/main/java/org/example/controller/` - UI controllers
- `src/main/resources/fxml/` - FXML views
- `src/main/resources/css/` - Stylesheets

## ResponseQuestion Admin Module

Access the admin module from the dashboard sidebar using "Reponses".

A standalone launcher is also available for quick testing:
- `org.example.ResponseQuestionAdminLauncher`

The module supports CRUD, filters, pagination, file upload, and PDF export.
