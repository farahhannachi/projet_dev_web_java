package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.example.service.UserService;


public class AccueilController {

    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;


    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search clicked");
    }

    @FXML
    }

    @FXML
    }

    @FXML
    }

    @FXML
    }

    @FXML
    }

    @FXML
    }

    @FXML
    }

    @FXML
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    }

    @FXML
        userService.logout();
    }
}
