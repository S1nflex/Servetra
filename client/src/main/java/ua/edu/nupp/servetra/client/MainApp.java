package ua.edu.nupp.servetra.client;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ua.edu.nupp.servetra.client.api.ApiClient;
import ua.edu.nupp.servetra.client.api.ApiClientException;
import ua.edu.nupp.servetra.client.i18n.I18n;
import ua.edu.nupp.servetra.client.importer.EquipmentImportTask;
import ua.edu.nupp.servetra.client.importer.ImportReport;
import ua.edu.nupp.servetra.client.model.Models.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static ua.edu.nupp.servetra.client.ui.UiFactory.*;
import static ua.edu.nupp.servetra.client.validation.FormParsers.*;

// [ETAP 3] JavaFX application shell.
// All data changes are sent to the server through ApiClient.
public class MainApp extends Application {
    private static final BigDecimal MEDIUM_PRIORITY = new BigDecimal("25");
    private static final BigDecimal HIGH_PRIORITY = new BigDecimal("50");
    private static final BigDecimal CRITICAL_PRIORITY = new BigDecimal("75");

    private final ApiClient api = new ApiClient();
    private final I18n i18n = new I18n();
    private final Preferences prefs = Preferences.userNodeForPackage(MainApp.class);

    private final TableView<Equipment> equipmentTable = new TableView<>();
    private final TableView<Specialist> specialistTable = new TableView<>();
    private final TableView<MaintenanceRequest> requestTable = new TableView<>();
    private final TableView<ServiceRecord> recordTable = new TableView<>();

    private final Label equipmentCount = metric();
    private final Label openCount = metric();
    private final Label criticalCount = metric();
    private final Label completedCount = metric();

    private Stage stage;
    private boolean dark;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.getIcons().add(new javafx.scene.image.Image(resource("/images/logo.png")));
        dark = prefs.getBoolean("dark", false);
        rebuild();
    }

    private void rebuild() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(topBar());

        TabPane tabs = new TabPane(
                tab(t("nav.overview"), dashboard()),
                tab(t("nav.equipment"), equipmentPane()),
                tab(t("nav.requests"), requestPane()),
                tab(t("nav.specialists"), specialistPane()),
                tab(t("nav.records"), serviceRecordPane())
        );
        root.setCenter(tabs);

        Scene scene = new Scene(root, 1380, 820);
        scene.getStylesheets().add(resource("/styles/base.css"));
        String themeFile = dark ? "/styles/dark.css" : "/styles/light.css";
        scene.getStylesheets().add(resource(themeFile));

        stage.setScene(scene);
        stage.setTitle("SERVETRA - " + t("app.subtitle"));
        stage.show();
        refreshAll();
    }

    private Node topBar() {
        Label brand = new Label("SERVETRA");
        brand.getStyleClass().add("brand");

        Label subtitle = new Label(t("app.subtitle"));
        subtitle.getStyleClass().add("subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> language = new ComboBox<>(
                FXCollections.observableArrayList("Українська", "English")
        );
        String currentLanguage;
        if (i18n.locale().getLanguage().equals("en")) {
            currentLanguage = "English";
        } else {
            currentLanguage = "Українська";
        }
        language.setValue(currentLanguage);
        language.setOnAction(event -> changeLanguage(language.getValue()));

        ToggleButton theme = new ToggleButton(dark ? t("theme.light") : t("theme.dark"));
        theme.setOnAction(event -> changeTheme());

        javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(resource("/images/logo.png")));
        logo.setFitWidth(48);
        logo.setFitHeight(48);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        HBox bar = new HBox(12, logo, brand, subtitle, spacer, language, theme);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 18, 14, 18));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private void changeLanguage(String selectedLanguage) {
        if ("English".equals(selectedLanguage)) {
            i18n.setLocale(Locale.ENGLISH);
        } else {
            i18n.setLocale(Locale.forLanguageTag("uk"));
        }
        rebuild();
    }

    private void changeTheme() {
        dark = !dark;
        prefs.putBoolean("dark", dark);
        rebuild();
    }

    private Node dashboard() {
        GridPane metrics = new GridPane();
        metrics.setHgap(16);
        metrics.setVgap(16);
        metrics.setPadding(new Insets(22));
        metrics.add(card(t("dashboard.equipment"), equipmentCount), 0, 0);
        metrics.add(card(t("dashboard.open"), openCount), 1, 0);
        metrics.add(card(t("dashboard.critical"), criticalCount), 2, 0);
        metrics.add(card(t("dashboard.completed"), completedCount), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            metrics.getColumnConstraints().add(column);
        }

        Button refresh = button("action.refresh");
        refresh.setOnAction(event -> refreshDashboard());

        Label explanation = new Label(t("dashboard.explanation"));
        explanation.setWrapText(true);
        explanation.getStyleClass().add("muted");

        VBox box = new VBox(16, metrics, new Separator(), explanation, refresh);
        box.setPadding(new Insets(18));
        return box;
    }

    private Node equipmentPane() {
        configureEquipmentTable();

        TextField inventory = field();
        TextField name = field();
        TextField category = field();
        TextField manufacturer = field();
        TextField model = field();
        TextField serial = field();
        TextField location = field();
        TextField department = field();
        Spinner<Integer> criticality = scoreSpinner();
        DatePicker due = new DatePicker();
        CheckBox active = new CheckBox(t("field.active"));
        active.setSelected(true);

        GridPane equipmentForm = form();
        int row = 0;
        addRow(equipmentForm, row++, t("equipment.inventory"), inventory);
        addRow(equipmentForm, row++, t("field.name"), name);
        addRow(equipmentForm, row++, t("equipment.category"), category);
        addRow(equipmentForm, row++, t("equipment.manufacturer"), manufacturer);
        addRow(equipmentForm, row++, t("equipment.model"), model);
        addRow(equipmentForm, row++, t("equipment.serial"), serial);
        addRow(equipmentForm, row++, t("equipment.location"), location);
        addRow(equipmentForm, row++, t("equipment.department"), department);
        addRow(equipmentForm, row++, t("equipment.criticality"), criticality);
        addRow(equipmentForm, row++, t("equipment.nextService"), due);
        addRow(equipmentForm, row, "", active);

        Button create = button("action.create");
        Button update = button("action.update");
        Button delete = button("action.delete");
        Button reload = button("action.refresh");
        Button importCsv = button("import.action");

        create.setOnAction(event -> createEquipment(
                inventory, name, category, manufacturer, model, serial,
                location, department, criticality, due, active
        ));
        update.setOnAction(event -> updateEquipment(
                inventory, name, category, manufacturer, model, serial,
                location, department, criticality, due, active
        ));
        delete.setOnAction(event -> deleteEquipment());
        reload.setOnAction(event -> refreshEquipment(null));

        TextField search = field();
        search.setPromptText(t("action.search"));
        Button searchButton = button("action.search");
        searchButton.setOnAction(event -> refreshEquipment(search.getText()));
        HBox searchBar = new HBox(8, search, searchButton);
        HBox.setHgrow(search, Priority.ALWAYS);

        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        Label importStatus = new Label(t("import.idle"));
        importStatus.setWrapText(true);
        importCsv.setOnAction(event -> startImport(importCsv, progress, importStatus));

        equipmentTable.getSelectionModel().selectedItemProperty().addListener(
                (property, oldValue, selected) -> fillEquipmentForm(
                        selected, inventory, name, category, manufacturer, model,
                        serial, location, department, criticality, due, active
                )
        );

        VBox right = new VBox(
                12,
                equipmentForm,
                actions(create, update, delete, reload),
                new Separator(),
                importCsv,
                progress,
                importStatus
        );
        right.setPadding(new Insets(14));
        right.setPrefWidth(420);

        VBox left = new VBox(10, searchBar, equipmentTable);
        VBox.setVgrow(equipmentTable, Priority.ALWAYS);
        left.setPadding(new Insets(14));
        return split(left, right);
    }

    private void configureEquipmentTable() {
        equipmentTable.getColumns().setAll(
                column("ID", Equipment::id, 55),
                column(t("equipment.inventory"), Equipment::inventoryNumber, 125),
                column(t("field.name"), Equipment::name, 190),
                column(t("equipment.category"), Equipment::category, 90),
                column(t("equipment.location"), Equipment::location, 150),
                column("F1", Equipment::criticalityScore, 60),
                column(t("equipment.nextService"), Equipment::nextServiceDue, 105),
                column(t("field.active"), Equipment::active, 65)
        );
    }

    private Equipment equipmentFromForm(
            Long id,
            TextField inventory,
            TextField name,
            TextField category,
            TextField manufacturer,
            TextField model,
            TextField serial,
            TextField location,
            TextField department,
            Spinner<Integer> criticality,
            DatePicker due,
            CheckBox active
    ) {
        return new Equipment(
                id, inventory.getText(), name.getText(), category.getText(),
                manufacturer.getText(), model.getText(), serial.getText(),
                location.getText(), department.getText(), criticality.getValue(),
                due.getValue(), active.isSelected()
        );
    }

    private void createEquipment(
            TextField inventory, TextField name, TextField category,
            TextField manufacturer, TextField model, TextField serial,
            TextField location, TextField department, Spinner<Integer> criticality,
            DatePicker due, CheckBox active
    ) {
        Equipment value = equipmentFromForm(
                null, inventory, name, category, manufacturer, model, serial,
                location, department, criticality, due, active
        );
        async(() -> api.createEquipment(value), ignored -> refreshEquipment(null));
    }

    private void updateEquipment(
            TextField inventory, TextField name, TextField category,
            TextField manufacturer, TextField model, TextField serial,
            TextField location, TextField department, Spinner<Integer> criticality,
            DatePicker due, CheckBox active
    ) {
        Equipment selected = selected(equipmentTable);
        if (selected == null) {
            return;
        }

        Equipment value = equipmentFromForm(
                selected.id(), inventory, name, category, manufacturer, model,
                serial, location, department, criticality, due, active
        );
        async(() -> api.updateEquipment(selected.id(), value), ignored -> refreshEquipment(null));
    }

    private void deleteEquipment() {
        Equipment selected = selected(equipmentTable);
        if (selected == null || !confirmDelete()) {
            return;
        }
        async(() -> {
            api.deleteEquipment(selected.id());
            return null;
        }, ignored -> refreshAll());
    }

    private void fillEquipmentForm(
            Equipment value, TextField inventory, TextField name, TextField category,
            TextField manufacturer, TextField model, TextField serial,
            TextField location, TextField department, Spinner<Integer> criticality,
            DatePicker due, CheckBox active
    ) {
        if (value == null) {
            return;
        }
        inventory.setText(value.inventoryNumber());
        name.setText(value.name());
        category.setText(value.category());
        manufacturer.setText(textOrEmpty(value.manufacturer()));
        model.setText(textOrEmpty(value.model()));
        serial.setText(textOrEmpty(value.serialNumber()));
        location.setText(value.location());
        department.setText(textOrEmpty(value.department()));
        criticality.getValueFactory().setValue(value.criticalityScore());
        due.setValue(value.nextServiceDue());
        active.setSelected(value.active());
    }

    private Node specialistPane() {
        specialistTable.getColumns().setAll(
                column("ID", Specialist::id, 55),
                column(t("specialist.name"), Specialist::fullName, 210),
                column(t("specialist.email"), Specialist::email, 210),
                column(t("specialist.specialization"), Specialist::specialization, 170),
                column(t("specialist.phone"), Specialist::phone, 130),
                column(t("field.active"), Specialist::active, 65)
        );

        TextField name = field();
        TextField email = field();
        TextField specialization = field();
        TextField phone = field();
        CheckBox active = new CheckBox(t("field.active"));
        active.setSelected(true);

        GridPane specialistForm = form();
        int row = 0;
        addRow(specialistForm, row++, t("specialist.name"), name);
        addRow(specialistForm, row++, t("specialist.email"), email);
        addRow(specialistForm, row++, t("specialist.specialization"), specialization);
        addRow(specialistForm, row++, t("specialist.phone"), phone);
        addRow(specialistForm, row, "", active);

        Button create = button("action.create");
        Button update = button("action.update");
        Button delete = button("action.delete");
        Button reload = button("action.refresh");

        create.setOnAction(event -> createSpecialist(name, email, specialization, phone, active));
        update.setOnAction(event -> updateSpecialist(name, email, specialization, phone, active));
        delete.setOnAction(event -> deleteSpecialist());
        reload.setOnAction(event -> refreshSpecialists());

        specialistTable.getSelectionModel().selectedItemProperty().addListener(
                (property, oldValue, selected) -> fillSpecialistForm(
                        selected, name, email, specialization, phone, active
                )
        );

        VBox right = new VBox(12, specialistForm, actions(create, update, delete, reload));
        right.setPadding(new Insets(14));
        right.setPrefWidth(420);
        return split(padded(specialistTable), right);
    }

    private Specialist specialistFromForm(
            Long id, TextField name, TextField email, TextField specialization,
            TextField phone, CheckBox active
    ) {
        return new Specialist(
                id, name.getText(), email.getText(), specialization.getText(),
                phone.getText(), active.isSelected()
        );
    }

    private void createSpecialist(
            TextField name, TextField email, TextField specialization,
            TextField phone, CheckBox active
    ) {
        Specialist value = specialistFromForm(null, name, email, specialization, phone, active);
        async(() -> api.createSpecialist(value), ignored -> refreshSpecialists());
    }

    private void updateSpecialist(
            TextField name, TextField email, TextField specialization,
            TextField phone, CheckBox active
    ) {
        Specialist selected = selected(specialistTable);
        if (selected == null) {
            return;
        }
        Specialist value = specialistFromForm(
                selected.id(), name, email, specialization, phone, active
        );
        async(() -> api.updateSpecialist(selected.id(), value), ignored -> refreshSpecialists());
    }

    private void deleteSpecialist() {
        Specialist selected = selected(specialistTable);
        if (selected == null || !confirmDelete()) {
            return;
        }
        async(() -> {
            api.deleteSpecialist(selected.id());
            return null;
        }, ignored -> refreshAll());
    }

    private void fillSpecialistForm(
            Specialist value, TextField name, TextField email,
            TextField specialization, TextField phone, CheckBox active
    ) {
        if (value == null) {
            return;
        }
        name.setText(value.fullName());
        email.setText(value.email());
        specialization.setText(textOrEmpty(value.specialization()));
        phone.setText(textOrEmpty(value.phone()));
        active.setSelected(value.active());
    }

    private Node requestPane() {
        configureRequestTable();

        TextField equipment = field();
        TextField specialist = field();
        TextField summary = field();
        TextArea description = area();
        TextArea diagnostics = area();
        Spinner<Integer> severity = scoreSpinner();
        Spinner<Integer> downtime = scoreSpinner();
        DatePicker due = new DatePicker();
        ComboBox<RequestStatus> transition = new ComboBox<>(
                FXCollections.observableArrayList(RequestStatus.values())
        );

        GridPane requestForm = form();
        int row = 0;
        addRow(requestForm, row++, t("request.equipment"), equipment);
        addRow(requestForm, row++, t("request.specialistOptional"), specialist);
        addRow(requestForm, row++, t("request.summary"), summary);
        addRow(requestForm, row++, t("request.description"), description);
        addRow(requestForm, row++, t("request.diagnostics"), diagnostics);
        addRow(requestForm, row++, t("request.severity"), severity);
        addRow(requestForm, row++, t("request.downtime"), downtime);
        addRow(requestForm, row++, t("request.due"), due);
        addRow(requestForm, row, t("request.nextStatus"), transition);

        Button create = button("action.create");
        Button update = button("action.update");
        Button change = button("request.transition");
        Button delete = button("action.delete");
        Button reload = button("action.refresh");

        create.setOnAction(event -> createRequest(
                equipment, specialist, summary, description, diagnostics,
                severity, downtime, due
        ));
        update.setOnAction(event -> updateRequest(
                equipment, specialist, summary, description, diagnostics,
                severity, downtime, due
        ));
        change.setOnAction(event -> transitionRequest(transition.getValue()));
        delete.setOnAction(event -> deleteRequest());
        reload.setOnAction(event -> refreshRequests(null));

        ComboBox<RequestStatus> filter = new ComboBox<>(
                FXCollections.observableArrayList(RequestStatus.values())
        );
        filter.setPromptText(t("request.allStatuses"));
        Button apply = button("action.filter");
        Button clear = button("action.clear");
        apply.setOnAction(event -> refreshRequests(filter.getValue()));
        clear.setOnAction(event -> clearRequestFilter(filter));

        requestTable.getSelectionModel().selectedItemProperty().addListener(
                (property, oldValue, selected) -> fillRequestForm(
                        selected, equipment, specialist, summary, description,
                        diagnostics, severity, downtime, due
                )
        );

        HBox filterBar = new HBox(8, filter, apply, clear);
        VBox left = new VBox(10, filterBar, requestTable);
        VBox.setVgrow(requestTable, Priority.ALWAYS);
        left.setPadding(new Insets(14));

        VBox right = new VBox(12, requestForm, actions(create, update, change, delete, reload));
        right.setPadding(new Insets(14));
        right.setPrefWidth(430);
        return split(left, right);
    }

    private void configureRequestTable() {
        requestTable.getColumns().setAll(
                column("ID", MaintenanceRequest::id, 55),
                column(t("request.number"), MaintenanceRequest::requestNumber, 105),
                column(t("request.equipment"), MaintenanceRequest::equipmentId, 85),
                column(t("request.specialist"), MaintenanceRequest::specialistId, 85),
                column(t("request.summary"), MaintenanceRequest::summary, 190),
                column(t("request.status"), value -> status(value.status()), 125),
                column("F1", MaintenanceRequest::criticalitySnapshot, 50),
                column("F2", MaintenanceRequest::severityScore, 50),
                column("F3", MaintenanceRequest::downtimeImpactScore, 50),
                column("I", MaintenanceRequest::priorityIndex, 65),
                column(t("request.due"), MaintenanceRequest::dueAt, 145)
        );

        requestTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(MaintenanceRequest item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "priority-low", "priority-medium",
                        "priority-high", "priority-critical"
                );
                if (!empty && item != null && item.priorityIndex() != null) {
                    getStyleClass().add(priorityClass(item.priorityIndex()));
                }
            }
        });
    }

    private MaintenanceRequest requestFromForm(
            Long id, TextField equipment, TextField specialist, TextField summary,
            TextArea description, TextArea diagnostics, Spinner<Integer> severity,
            Spinner<Integer> downtime, DatePicker due
    ) {
        LocalDateTime dueAt = null;
        if (due.getValue() != null) {
            dueAt = due.getValue().atTime(17, 0);
        }
        return new MaintenanceRequest(
                id, null, positiveLong(equipment.getText(), "equipmentId"),
                optionalPositiveLong(specialist.getText()), summary.getText(),
                description.getText(), diagnostics.getText(), null, 0,
                severity.getValue(), downtime.getValue(), null, dueAt,
                null, null, null
        );
    }

    private void createRequest(
            TextField equipment, TextField specialist, TextField summary,
            TextArea description, TextArea diagnostics, Spinner<Integer> severity,
            Spinner<Integer> downtime, DatePicker due
    ) {
        MaintenanceRequest value = requestFromForm(
                null, equipment, specialist, summary, description,
                diagnostics, severity, downtime, due
        );
        async(() -> api.createRequest(value), ignored -> refreshRequests(null));
    }

    private void updateRequest(
            TextField equipment, TextField specialist, TextField summary,
            TextArea description, TextArea diagnostics, Spinner<Integer> severity,
            Spinner<Integer> downtime, DatePicker due
    ) {
        MaintenanceRequest selected = selected(requestTable);
        if (selected == null) {
            return;
        }
        MaintenanceRequest value = requestFromForm(
                selected.id(), equipment, specialist, summary, description,
                diagnostics, severity, downtime, due
        );
        async(() -> api.updateRequest(selected.id(), value), ignored -> refreshRequests(null));
    }

    private void transitionRequest(RequestStatus nextStatus) {
        MaintenanceRequest selected = selected(requestTable);
        if (selected == null) {
            return;
        }
        if (nextStatus == null) {
            showError(new IllegalArgumentException(t("validation.status")));
            return;
        }
        async(() -> api.transition(selected.id(), nextStatus), ignored -> refreshAll());
    }

    private void deleteRequest() {
        MaintenanceRequest selected = selected(requestTable);
        if (selected == null || !confirmDelete()) {
            return;
        }
        async(() -> {
            api.deleteRequest(selected.id());
            return null;
        }, ignored -> refreshAll());
    }

    private void clearRequestFilter(ComboBox<RequestStatus> filter) {
        filter.setValue(null);
        refreshRequests(null);
    }

    private void fillRequestForm(
            MaintenanceRequest value, TextField equipment, TextField specialist,
            TextField summary, TextArea description, TextArea diagnostics,
            Spinner<Integer> severity, Spinner<Integer> downtime, DatePicker due
    ) {
        if (value == null) {
            return;
        }
        equipment.setText(String.valueOf(value.equipmentId()));
        if (value.specialistId() == null) {
            specialist.setText("");
        } else {
            specialist.setText(String.valueOf(value.specialistId()));
        }
        summary.setText(value.summary());
        description.setText(textOrEmpty(value.description()));
        diagnostics.setText(textOrEmpty(value.diagnosticNotes()));
        severity.getValueFactory().setValue(value.severityScore());
        downtime.getValueFactory().setValue(value.downtimeImpactScore());
        if (value.dueAt() == null) {
            due.setValue(null);
        } else {
            due.setValue(value.dueAt().toLocalDate());
        }
    }

    private Node serviceRecordPane() {
        recordTable.getColumns().setAll(
                column("ID", ServiceRecord::id, 55),
                column(t("record.request"), ServiceRecord::requestId, 85),
                column(t("record.specialist"), ServiceRecord::specialistId, 85),
                column(t("record.type"), ServiceRecord::workType, 110),
                column(t("record.description"), ServiceRecord::description, 300),
                column(t("record.performed"), ServiceRecord::performedAt, 145),
                column(t("record.duration"), ServiceRecord::durationMinutes, 90),
                column(t("record.cost"), ServiceRecord::costAmount, 90)
        );

        TextField request = field();
        TextField specialist = field();
        TextField duration = field();
        TextField cost = field();
        ComboBox<WorkType> type = new ComboBox<>(
                FXCollections.observableArrayList(WorkType.values())
        );
        TextArea description = area();
        DatePicker performed = new DatePicker(LocalDate.now());

        GridPane recordForm = form();
        int row = 0;
        addRow(recordForm, row++, t("record.request"), request);
        addRow(recordForm, row++, t("record.specialist"), specialist);
        addRow(recordForm, row++, t("record.type"), type);
        addRow(recordForm, row++, t("record.description"), description);
        addRow(recordForm, row++, t("record.performed"), performed);
        addRow(recordForm, row++, t("record.duration"), duration);
        addRow(recordForm, row, t("record.cost"), cost);

        Button create = button("action.create");
        Button update = button("action.update");
        Button delete = button("action.delete");
        Button reload = button("action.refresh");

        create.setOnAction(event -> createRecord(
                request, specialist, type, description, performed, duration, cost
        ));
        update.setOnAction(event -> updateRecord(
                request, specialist, type, description, performed, duration, cost
        ));
        delete.setOnAction(event -> deleteRecord());
        reload.setOnAction(event -> refreshRecords());

        recordTable.getSelectionModel().selectedItemProperty().addListener(
                (property, oldValue, selected) -> fillRecordForm(
                        selected, request, specialist, type, description,
                        performed, duration, cost
                )
        );

        VBox right = new VBox(12, recordForm, actions(create, update, delete, reload));
        right.setPadding(new Insets(14));
        right.setPrefWidth(430);
        return split(padded(recordTable), right);
    }

    private ServiceRecord recordFromForm(
            Long id, TextField request, TextField specialist, ComboBox<WorkType> type,
            TextArea description, DatePicker performed, TextField duration, TextField cost
    ) {
        LocalDateTime performedAt = null;
        if (performed.getValue() != null) {
            performedAt = performed.getValue().atTime(12, 0);
        }
        return new ServiceRecord(
                id, positiveLong(request.getText(), "requestId"),
                positiveLong(specialist.getText(), "specialistId"), type.getValue(),
                description.getText(), performedAt,
                optionalNonNegativeInt(duration.getText()),
                optionalNonNegativeDecimal(cost.getText())
        );
    }

    private void createRecord(
            TextField request, TextField specialist, ComboBox<WorkType> type,
            TextArea description, DatePicker performed, TextField duration, TextField cost
    ) {
        ServiceRecord value = recordFromForm(
                null, request, specialist, type, description, performed, duration, cost
        );
        async(() -> api.createRecord(value), ignored -> refreshRecords());
    }

    private void updateRecord(
            TextField request, TextField specialist, ComboBox<WorkType> type,
            TextArea description, DatePicker performed, TextField duration, TextField cost
    ) {
        ServiceRecord selected = selected(recordTable);
        if (selected == null) {
            return;
        }
        ServiceRecord value = recordFromForm(
                selected.id(), request, specialist, type, description,
                performed, duration, cost
        );
        async(() -> api.updateRecord(selected.id(), value), ignored -> refreshRecords());
    }

    private void deleteRecord() {
        ServiceRecord selected = selected(recordTable);
        if (selected == null || !confirmDelete()) {
            return;
        }
        async(() -> {
            api.deleteRecord(selected.id());
            return null;
        }, ignored -> refreshRecords());
    }

    private void fillRecordForm(
            ServiceRecord value, TextField request, TextField specialist,
            ComboBox<WorkType> type, TextArea description, DatePicker performed,
            TextField duration, TextField cost
    ) {
        if (value == null) {
            return;
        }
        request.setText(String.valueOf(value.requestId()));
        specialist.setText(String.valueOf(value.specialistId()));
        type.setValue(value.workType());
        description.setText(value.description());
        performed.setValue(value.performedAt().toLocalDate());
        duration.setText(value.durationMinutes() == null
                ? "" : String.valueOf(value.durationMinutes()));
        cost.setText(value.costAmount() == null
                ? "" : value.costAmount().toPlainString());
    }

    // [ETAP 4] Run CSV import outside the JavaFX application thread.
    private void startImport(Button trigger, ProgressBar progress, Label status) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("SERVETRA - " + t("app.subtitle"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );

        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        trigger.setDisable(true);
        EquipmentImportTask task = new EquipmentImportTask(file.toPath(), api);
        progress.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> importSucceeded(task, trigger, progress, status));
        task.setOnFailed(event -> importFailed(task, trigger, progress, status));
        task.setOnCancelled(event -> importCancelled(trigger, progress, status));

        Thread thread = new Thread(task, "servetra-equipment-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void importSucceeded(
            EquipmentImportTask task, Button trigger, ProgressBar progress, Label status
    ) {
        finishImport(trigger, progress, status);
        ImportReport report = task.getValue();
        String summary = t(
                "import.summary", report.total(), report.imported(),
                report.duplicates(), report.invalid(), report.failed()
        );
        if (!report.errors().isEmpty()) {
            summary = summary + "\n" + String.join("\n", report.errors());
        }
        status.setText(summary);
        refreshAll();
    }

    private void importFailed(
            EquipmentImportTask task, Button trigger, ProgressBar progress, Label status
    ) {
        finishImport(trigger, progress, status);
        status.setText(t("import.failed"));
        showError(task.getException());
    }

    private void importCancelled(Button trigger, ProgressBar progress, Label status) {
        finishImport(trigger, progress, status);
        status.setText(t("import.cancelled"));
    }

    private static void finishImport(Button trigger, ProgressBar progress, Label status) {
        progress.progressProperty().unbind();
        status.textProperty().unbind();
        trigger.setDisable(false);
    }

    private void refreshAll() {
        refreshEquipment(null);
        refreshSpecialists();
        refreshRequests(null);
        refreshRecords();
        refreshDashboard();
    }

    private void refreshEquipment(String query) {
        async(
                () -> api.equipment(query),
                values -> equipmentTable.setItems(FXCollections.observableArrayList(values))
        );
    }

    private void refreshSpecialists() {
        async(
                api::specialists,
                values -> specialistTable.setItems(FXCollections.observableArrayList(values))
        );
    }

    private void refreshRequests(RequestStatus status) {
        async(
                () -> api.requests(status),
                values -> requestTable.setItems(FXCollections.observableArrayList(values))
        );
    }

    private void refreshRecords() {
        async(
                () -> api.records(null),
                values -> recordTable.setItems(FXCollections.observableArrayList(values))
        );
    }

    private void refreshDashboard() {
        async(
                () -> new DashboardData(api.equipment(null), api.requests(null)),
                this::showDashboard
        );
    }

    private void showDashboard(DashboardData data) {
        int open = 0;
        int critical = 0;
        int completed = 0;

        for (MaintenanceRequest request : data.requests()) {
            if (request.status() == RequestStatus.COMPLETED) {
                completed++;
            } else {
                open++;
            }
            BigDecimal priority = request.priorityIndex();
            if (priority != null && priority.compareTo(CRITICAL_PRIORITY) >= 0) {
                critical++;
            }
        }

        equipmentCount.setText(String.valueOf(data.equipment().size()));
        openCount.setText(String.valueOf(open));
        criticalCount.setText(String.valueOf(critical));
        completedCount.setText(String.valueOf(completed));
    }

    private record DashboardData(
            List<Equipment> equipment,
            List<MaintenanceRequest> requests
    ) {
    }

    private <T> void async(Callable<T> operation, Consumer<T> success) {
        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };

        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> showError(task.getException()));

        Thread thread = new Thread(task, "servetra-http");
        thread.setDaemon(true);
        thread.start();
    }

    private <T> T selected(TableView<T> table) {
        T value = table.getSelectionModel().getSelectedItem();
        if (value == null) {
            showError(new IllegalArgumentException(t("validation.select")));
        }
        return value;
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                t("dialog.delete"),
                ButtonType.YES,
                ButtonType.NO
        );
        alert.setHeaderText(t("dialog.deleteTitle"));
        ButtonType result = alert.showAndWait().orElse(ButtonType.NO);
        return result == ButtonType.YES;
    }

    private void showError(Throwable error) {
        String message = errorMessage(error);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(t("error.title"));
        alert.showAndWait();
    }

    private String errorMessage(Throwable error) {
        if (error instanceof ApiClientException apiError) {
            String key = "error." + apiError.code();
            try {
                return t(key);
            } catch (Exception ignored) {
                return apiError.getMessage();
            }
        }
        if (error == null || error.getMessage() == null) {
            return t("error.unknown");
        }
        return error.getMessage();
    }

    private String status(RequestStatus value) {
        return t("status." + value.name().toLowerCase(Locale.ROOT));
    }

    private static String priorityClass(BigDecimal value) {
        if (value.compareTo(MEDIUM_PRIORITY) < 0) {
            return "priority-low";
        }
        if (value.compareTo(HIGH_PRIORITY) < 0) {
            return "priority-medium";
        }
        if (value.compareTo(CRITICAL_PRIORITY) < 0) {
            return "priority-high";
        }
        return "priority-critical";
    }

    private String t(String key, Object... args) {
        return i18n.text(key, args);
    }

    private String resource(String value) {
        return getClass().getResource(value).toExternalForm();
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private Button button(String key) {
        return new Button(t(key));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
