import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * ICT261 Lecture 3 - Customer Manager classroom lab (single file, run with: gradle run
 * or javac/java with the JavaFX modules on the module path).
 *
 * Layers (slide 28): View (this UI) -> Controller (event methods) -> Service -> DAO.
 */
public class CustomerManagerApp extends Application {

    // ---------------------------------------------------------------- Model
    public static class Customer {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty province = new SimpleStringProperty();

        public Customer(String name, String province) {
            this.name.set(name);
            this.province.set(province);
        }
        public String getName() { return name.get(); }
        public String getProvince() { return province.get(); }
        public StringProperty nameProperty() { return name; }
        public StringProperty provinceProperty() { return province; }
    }

    // ------------------------------------------------------------------ DAO
    // Reads and writes stored data. In-memory here; later replace with JDBC/H2.
    static class CustomerDao {
        private final ObservableList<Customer> items = FXCollections.observableArrayList();
        ObservableList<Customer> findAll() { return items; }
        void save(Customer c) { items.add(c); }
        void delete(Customer c) { items.remove(c); }
    }

    // -------------------------------------------------------------- Service
    // Application rules: validate, create, delete.
    static class CustomerService {
        private final CustomerDao dao;
        CustomerService(CustomerDao dao) { this.dao = dao; }

        ObservableList<Customer> customers() { return dao.findAll(); }

        Customer create(String name, String province) {
            String n = name == null ? "" : name.trim();
            if (n.length() < 2) {
                throw new IllegalArgumentException("Name is required (at least 2 characters).");
            }
            if (province == null || province.isBlank()) {
                throw new IllegalArgumentException("Please select a province.");
            }
            boolean duplicate = dao.findAll().stream().anyMatch(c ->
                    c.getName().equalsIgnoreCase(n) && c.getProvince().equals(province));
            if (duplicate) {
                throw new IllegalArgumentException("That customer already exists in " + province + ".");
            }
            Customer c = new Customer(n, province);
            dao.save(c);
            return c;
        }

        void delete(Customer c) { dao.delete(c); }
    }

    // ----------------------------------------------------- View + Controller
    private final CustomerService service = new CustomerService(new CustomerDao());

    private final TextField nameField = new TextField();
    private final ComboBox<String> provinceBox = new ComboBox<>();
    private final TableView<Customer> table = new TableView<>();
    private final Label messageLabel = new Label();

    @Override
    public void start(Stage stage) {
        // 1. Form: name field + province list
        provinceBox.setItems(FXCollections.observableArrayList(
                "Central", "Copperbelt", "Eastern", "Luapula", "Lusaka",
                "Muchinga", "Northern", "North-Western", "Southern", "Western"));
        provinceBox.setPromptText("Select province");
        provinceBox.setMaxWidth(Double.MAX_VALUE);
        nameField.setPromptText("Customer name");

        Label nameLabel = new Label("_Name:");           // Alt+N jumps to the field
        nameLabel.setMnemonicParsing(true);
        nameLabel.setLabelFor(nameField);
        Label provinceLabel = new Label("_Province:");   // Alt+P
        provinceLabel.setMnemonicParsing(true);
        provinceLabel.setLabelFor(provinceBox);

        nameField.setAccessibleText("Customer name");
        provinceBox.setAccessibleText("Province");

        Button addBtn = new Button("_Add");               // Alt+A
        addBtn.setMnemonicParsing(true);
        addBtn.setDefaultButton(true);                    // Enter triggers Add
        addBtn.setOnAction(e -> handleAdd());

        Button deleteBtn = new Button("_Delete");         // Alt+D
        deleteBtn.setMnemonicParsing(true);
        deleteBtn.setOnAction(e -> handleDelete());
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(nameLabel, 0, 0);
        form.add(nameField, 1, 0);
        form.add(provinceLabel, 0, 1);
        form.add(provinceBox, 1, 1);
        form.add(new HBox(10, addBtn, deleteBtn), 1, 2);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1);

        // 2 + 3. ObservableList<Customer> (inside the DAO) shown in a TableView
        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        TableColumn<Customer, String> provCol = new TableColumn<>("Province");
        provCol.setCellValueFactory(d -> d.getValue().provinceProperty());
        table.getColumns().addAll(nameCol, provCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(service.customers());      // table listens to the ObservableList
        table.setPlaceholder(new Label("No customers yet"));
        table.setAccessibleText("Customer table");
        table.setOnKeyPressed(e -> {              // keyboard access: Delete key
            if (e.getCode() == KeyCode.DELETE) handleDelete();
        });

        Label countLabel = new Label();
        countLabel.textProperty().bind(Bindings.size(service.customers()).asString("Customers: %d"));

        VBox root = new VBox(12, form, messageLabel, table, countLabel);
        root.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);

        stage.setTitle("Customer Manager");
        stage.setScene(new Scene(root, 520, 460));
        stage.show();
        nameField.requestFocus();
    }

    // 4. Validate input, then add the customer (slide 27 behaviour)
    private void handleAdd() {
        try {
            service.create(nameField.getText(), provinceBox.getValue());
        } catch (IllegalArgumentException ex) {
            // Keep what the user typed; only tell them what to fix.
            showMessage(ex.getMessage(), false);
            if (provinceBox.getValue() == null && nameField.getText().trim().length() >= 2) {
                provinceBox.requestFocus();       // name kept, request a province
            } else {
                nameField.requestFocus();
            }
            return;
        } catch (RuntimeException ex) {
            // Save operation fails: keep form values for another attempt.
            showMessage("Could not save the customer. Please try again.", false);
            return;
        }
        showMessage("Customer added successfully.", true);
        nameField.clear();
        provinceBox.setValue(null);
        nameField.requestFocus();
    }

    // 5. Confirm deletion of the selected customer
    private void handleDelete() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + " (" + selected.getProvince() + ")?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.delete(selected);             // table updates automatically
            showMessage("Customer deleted.", true);
        }
        // Cancel: nothing changes, selection stays.
    }

    private void showMessage(String text, boolean ok) {
        messageLabel.setStyle("-fx-text-fill: " + (ok ? "green" : "red") + ";");
        messageLabel.setText(text);
    }

    public static void main(String[] args) { launch(args); }
}
