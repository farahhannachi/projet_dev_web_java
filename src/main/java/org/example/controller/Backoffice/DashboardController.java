package org.example.controller.Backoffice;

import org.example.MainApp;
import javafx.fxml.FXML;

public class DashboardController {

    @FXML
    private void goToBackoffice() {
        MainApp.navigateTo("/Backoffice/Backoffice.fxml", "CuraVita - Backoffice");
    }

    @FXML
    private void goToFrontoffice() {
        MainApp.navigateTo("/Frontoffice/Frontoffice.fxml", "CuraVita - Frontoffice");
    }
}
