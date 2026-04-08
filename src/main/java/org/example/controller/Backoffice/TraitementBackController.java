package org.example.controller.Backoffice;

import org.example.entities.Ordonnance;
import org.example.entities.Traitement;
import org.example.services.OrdonnanceService;
import org.example.services.TraitementService;
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

public class TraitementBackController implements Initializable {

    @FXML private ComboBox<Ordonnance> cbOrdonnance;
    @FXML private TextField tfIdUtilisateur, tfDosage, tfFrequence, tfDureeJours;
    @FXML private TextField tfDateDebut, tfDateFin, tfStatus, tfNotes, tfIdProduit, tfSearch;
    @FXML private TableView<Traitement> tableTraitement;
    @FXML private TableColumn<Traitement, Integer> colId, colIdOrdonnance, colIdUtilisateur, colDureeJours, colIdProduit;
    @FXML private TableColumn<Traitement, String> colDosage, colFrequence, colDateDebut, colDateFin, colStatus, colNotes;
    @FXML private VBox listView, addView;
    @FXML private Button btnAddTab;
    @FXML private Label lblTotalTraitements, lblEnCours, lblEnAttente, lblTermines;
    @FXML private Label lblFormTitle;

    private final TraitementService service = new TraitementService();
    private final OrdonnanceService ordService = new OrdonnanceService();
    private ObservableList<Traitement> traitementList;
    private FilteredList<Traitement> filteredList;
    private Traitement selectedTraitement = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("idTraitement"));
        colIdOrdonnance.setCellValueFactory(new PropertyValueFactory<>("idOrdonnance"));
        colIdUtilisateur.setCellValueFactory(new PropertyValueFactory<>("idUtilisateur"));
        colDosage.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        colFrequence.setCellValueFactory(new PropertyValueFactory<>("frequence"));
        colDureeJours.setCellValueFactory(new PropertyValueFactory<>("dureeJours"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colIdProduit.setCellValueFactory(new PropertyValueFactory<>("idProduit"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String s = item.toLowerCase();
                if (s.contains("actif") || s.contains("en cours") || s.contains("valid")) badge.getStyleClass().add("badge-en-cours");
                else if (s.contains("termin") || s.contains("complet")) badge.getStyleClass().add("badge-completee");
                else if (s.contains("annul") || s.contains("suspen")) badge.getStyleClass().add("badge-annulee");
                else badge.getStyleClass().add("badge-expiree");
                setGraphic(badge);
            }
        });

        addActionColumn();
        loadOrdonnances();
        loadData();

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(t -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return (t.getDosage() != null && t.getDosage().toLowerCase().contains(lower))
                    || (t.getStatus() != null && t.getStatus().toLowerCase().contains(lower));
            });
        });
    }

    private void addActionColumn() {
        TableColumn<Traitement, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(100);
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDel = new Button("🗑");
            private final HBox box = new HBox(6, btnEdit, btnDel);
            {
                btnEdit.getStyleClass().add("btn-edit");
                btnDel.getStyleClass().add("btn-delete");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> {
                    Traitement t = getTableView().getItems().get(getIndex());
                    showEditForm(t);
                });
                btnDel.setOnAction(e -> {
                    Traitement t = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Supprimer le traitement #" + t.getIdTraitement() + " ?",
                        ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.YES) {
                            try { service.delete(t.getIdTraitement()); loadData(); }
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
        tableTraitement.getColumns().add(colAction);
    }

    private void loadOrdonnances() {
        try { cbOrdonnance.setItems(FXCollections.observableArrayList(ordService.getAll())); }
        catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()); }
    }

    private void loadData() {
        try {
            traitementList = FXCollections.observableArrayList(service.getAll());
            filteredList = new FilteredList<>(traitementList, p -> true);
            tableTraitement.setItems(filteredList);
            updateStats();
        } catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()); }
    }

    private void updateStats() {
        int total = traitementList.size();
        long enCours = traitementList.stream().filter(t -> {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            return s.contains("actif") || s.contains("en cours") || s.contains("valid");
        }).count();
        long enAttente = traitementList.stream().filter(t -> {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            return s.contains("attente");
        }).count();
        long termines = traitementList.stream().filter(t -> {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            return s.contains("termin") || s.contains("complet");
        }).count();
        lblTotalTraitements.setText(String.valueOf(total));
        lblEnCours.setText(String.valueOf(enCours));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblTermines.setText(String.valueOf(termines));
    }

    private void showListView() {
        listView.setVisible(true); listView.setManaged(true);
        addView.setVisible(false); addView.setManaged(false);
        btnAddTab.setVisible(true); btnAddTab.setManaged(true);
        selectedTraitement = null;
    }

    @FXML
    private void showAddView() {
        listView.setVisible(false); listView.setManaged(false);
        addView.setVisible(true); addView.setManaged(true);
        btnAddTab.setVisible(false); btnAddTab.setManaged(false);
        if (selectedTraitement == null) {
            lblFormTitle.setText("Nouveau Traitement");
            clearForm();
        }
    }

    private void showEditForm(Traitement t) {
        selectedTraitement = t;
        lblFormTitle.setText("Modifier Traitement #" + t.getIdTraitement());
        for (Ordonnance o : cbOrdonnance.getItems()) {
            if (o.getIdOrdonnance() == t.getIdOrdonnance()) { cbOrdonnance.setValue(o); break; }
        }
        tfIdUtilisateur.setText(String.valueOf(t.getIdUtilisateur()));
        tfDosage.setText(t.getDosage());
        tfFrequence.setText(t.getFrequence());
        tfDureeJours.setText(String.valueOf(t.getDureeJours()));
        tfDateDebut.setText(t.getDateDebut());
        tfDateFin.setText(t.getDateFin());
        tfStatus.setText(t.getStatus());
        tfNotes.setText(t.getNotes());
        tfIdProduit.setText(String.valueOf(t.getIdProduit()));
        showAddView();
    }

    @FXML
    private void save() {
        if (!validateFields()) return;
        try {
            int idOrd = cbOrdonnance.getValue().getIdOrdonnance();
            int idUser = Integer.parseInt(tfIdUtilisateur.getText());
            int duree = Integer.parseInt(tfDureeJours.getText());
            int produit = Integer.parseInt(tfIdProduit.getText());

            if (selectedTraitement == null) {
                Traitement t = new Traitement(idOrd, idUser, tfDosage.getText(),
                    tfFrequence.getText(), duree, tfDateDebut.getText(), tfDateFin.getText(),
                    tfStatus.getText(), tfNotes.getText(), produit);
                service.insert(t);
            } else {
                selectedTraitement.setIdOrdonnance(idOrd);
                selectedTraitement.setIdUtilisateur(idUser);
                selectedTraitement.setDosage(tfDosage.getText());
                selectedTraitement.setFrequence(tfFrequence.getText());
                selectedTraitement.setDureeJours(duree);
                selectedTraitement.setDateDebut(tfDateDebut.getText());
                selectedTraitement.setDateFin(tfDateFin.getText());
                selectedTraitement.setStatus(tfStatus.getText());
                selectedTraitement.setNotes(tfNotes.getText());
                selectedTraitement.setIdProduit(produit);
                service.update(selectedTraitement);
            }
            clearForm();
            loadData();
            showListView();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Les champs numériques doivent contenir des nombres valides.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de données: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        if (cbOrdonnance.getValue() == null || tfIdUtilisateur.getText().isEmpty() ||
            tfDosage.getText().isEmpty() || tfFrequence.getText().isEmpty() ||
            tfDureeJours.getText().isEmpty() || tfStatus.getText().isEmpty() || tfIdProduit.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Tous les champs obligatoires doivent être remplis.");
            return false;
        }
        try {
            Integer.parseInt(tfIdUtilisateur.getText());
            Integer.parseInt(tfDureeJours.getText());
            Integer.parseInt(tfIdProduit.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Les champs numériques doivent contenir des nombres valides.");
            return false;
        }
        return true;
    }

    // "Annuler" button goes back to list
    @FXML
    private void cancelForm() {
        clearForm();
        showListView();
    }

    @FXML
    private void clearForm() {
        cbOrdonnance.setValue(null);
        tfIdUtilisateur.clear(); tfDosage.clear(); tfFrequence.clear(); tfDureeJours.clear();
        tfDateDebut.clear(); tfDateFin.clear(); tfStatus.clear(); tfNotes.clear(); tfIdProduit.clear();
        selectedTraitement = null;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
