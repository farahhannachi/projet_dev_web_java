package org.example.controller.Backoffice;

import org.example.entities.Ordonnance;
import org.example.services.OrdonnanceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class OrdonnanceBackController implements Initializable {

    @FXML private TextField tfIdUtilisateur, tfNumero, tfDateOrdonnance, tfDateExpiration;
    @FXML private TextField tfStatut, tfNoteMedical, tfSignatureMedecin, tfSearch;
    @FXML private TableView<Ordonnance> tableOrdonnance;
    @FXML private TableColumn<Ordonnance, Integer> colId, colIdUtilisateur;
    @FXML private TableColumn<Ordonnance, String> colNumero, colDateOrdonnance, colStatut;
    @FXML private TableColumn<Ordonnance, Void> colActions;
    @FXML private VBox listView, addView;
    @FXML private Button btnAddTab;
    @FXML private Label lblFormTitle;
    @FXML private Label lblTotalOrd, lblActives, lblExpirees, lblAnnulees;

    private final OrdonnanceService service = new OrdonnanceService();
    private ObservableList<Ordonnance> ordonnanceList;
    private FilteredList<Ordonnance> filteredList;
    private Ordonnance selectedOrdonnance = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("idOrdonnance"));
        colIdUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colDateOrdonnance.setCellValueFactory(new PropertyValueFactory<>("dateOrdonnance"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroOrdonnance"));

        // Status column with colored badges
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add(getBadgeStyle(item));
                setGraphic(badge);
            }
        });

        // Actions column with edit + delete buttons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏ Modifier");
            private final Button btnDel = new Button("🗑 Supprimer");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDel.getStyleClass().add("btn-delete");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    Ordonnance o = getTableView().getItems().get(getIndex());
                    showEditForm(o);
                });
                btnDel.setOnAction(e -> {
                    Ordonnance o = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer l'ordonnance " + o.getNumeroOrdonnance() + " ?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            try { service.delete(o.getIdOrdonnance()); loadData(); }
                            catch (SQLException ex) { showAlert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()); }
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        loadData();

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(o -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return (o.getSignatureMedecin() != null && o.getSignatureMedecin().toLowerCase().contains(lower))
                    || (o.getNumeroOrdonnance() != null && o.getNumeroOrdonnance().toLowerCase().contains(lower))
                    || (o.getStatut() != null && o.getStatut().toLowerCase().contains(lower));
            });
        });
    }

    private String getBadgeStyle(String statut) {
        if (statut == null) return "badge-expiree";
        String s = statut.toLowerCase();
        if (s.contains("activ") || s.contains("en cours") || s.contains("valid")) return "badge-en-cours";
        if (s.contains("complet") || s.contains("termin")) return "badge-completee";
        if (s.contains("annul")) return "badge-annulee";
        return "badge-expiree";
    }

    private void loadData() {
        try {
            ordonnanceList = FXCollections.observableArrayList(service.getAll());
            filteredList = new FilteredList<>(ordonnanceList, p -> true);
            tableOrdonnance.setItems(filteredList);
            updateStats();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur chargement: " + e.getMessage());
        }
    }

    private void updateStats() {
        int total = ordonnanceList.size();
        long actives = ordonnanceList.stream().filter(o -> {
            String s = o.getStatut() != null ? o.getStatut().toLowerCase() : "";
            return s.contains("activ") || s.contains("en cours") || s.contains("valid");
        }).count();
        long expirees = ordonnanceList.stream().filter(o -> {
            String s = o.getStatut() != null ? o.getStatut().toLowerCase() : "";
            return s.contains("expir");
        }).count();
        long annulees = ordonnanceList.stream().filter(o -> {
            String s = o.getStatut() != null ? o.getStatut().toLowerCase() : "";
            return s.contains("annul");
        }).count();
        lblTotalOrd.setText(String.valueOf(total));
        lblActives.setText(String.valueOf(actives));
        lblExpirees.setText(String.valueOf(expirees));
        lblAnnulees.setText(String.valueOf(annulees));
    }

    private void showListView() {
        listView.setVisible(true); listView.setManaged(true);
        addView.setVisible(false); addView.setManaged(false);
        btnAddTab.setVisible(true); btnAddTab.setManaged(true);
        selectedOrdonnance = null;
    }

    @FXML
    private void showAddView() {
        listView.setVisible(false); listView.setManaged(false);
        addView.setVisible(true); addView.setManaged(true);
        btnAddTab.setVisible(false); btnAddTab.setManaged(false);
        if (selectedOrdonnance == null) {
            lblFormTitle.setText("Nouvelle Ordonnance");
            clearFields();
        }
    }

    private void showEditForm(Ordonnance o) {
        selectedOrdonnance = o;
        lblFormTitle.setText("Modifier Ordonnance " + o.getNumeroOrdonnance());
        tfIdUtilisateur.setText(String.valueOf(o.getIdUtilisateur()));
        tfNumero.setText(o.getNumeroOrdonnance());
        tfDateOrdonnance.setText(o.getDateOrdonnance());
        tfDateExpiration.setText(o.getDateExpiration());
        tfStatut.setText(o.getStatut());
        tfNoteMedical.setText(o.getNoteMedical());
        tfSignatureMedecin.setText(o.getSignatureMedecin());
        showAddView();
    }

    @FXML
    private void save() {
        if (!validateFields()) return;
        try {
            int idUtilisateur = Integer.parseInt(tfIdUtilisateur.getText());
            if (selectedOrdonnance == null) {
                Ordonnance o = new Ordonnance(idUtilisateur, tfNumero.getText(),
                    tfDateOrdonnance.getText(), tfDateExpiration.getText(), tfStatut.getText(),
                    tfNoteMedical.getText(), false,
                    tfSignatureMedecin.getText());
                service.insert(o);
            } else {
                selectedOrdonnance.setIdUtilisateur(idUtilisateur);
                selectedOrdonnance.setNumeroOrdonnance(tfNumero.getText());
                selectedOrdonnance.setDateOrdonnance(tfDateOrdonnance.getText());
                selectedOrdonnance.setDateExpiration(tfDateExpiration.getText());
                selectedOrdonnance.setStatut(tfStatut.getText());
                selectedOrdonnance.setNoteMedical(tfNoteMedical.getText());
                selectedOrdonnance.setSignatureMedecin(tfSignatureMedecin.getText());
                service.update(selectedOrdonnance);
            }
            clearFields();
            selectedOrdonnance = null;
            loadData();
            showListView();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "ID Utilisateur doit être un nombre valide.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de données: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        if (tfIdUtilisateur.getText().isEmpty() || tfNumero.getText().isEmpty() ||
            tfDateOrdonnance.getText().isEmpty() || tfDateExpiration.getText().isEmpty() ||
            tfStatut.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Tous les champs obligatoires doivent être remplis.");
            return false;
        }
        try { Integer.parseInt(tfIdUtilisateur.getText()); }
        catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "ID Utilisateur doit être un nombre valide.");
            return false;
        }
        return true;
    }

    @FXML
    private void cancelForm() {
        clearFields();
        selectedOrdonnance = null;
        showListView();
    }

    private void clearFields() {
        tfIdUtilisateur.clear(); tfNumero.clear(); tfDateOrdonnance.clear();
        tfDateExpiration.clear(); tfStatut.clear(); tfNoteMedical.clear();
        tfSignatureMedecin.clear();
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
