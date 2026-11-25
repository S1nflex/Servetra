package ua.edu.nupp.servetra.client.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Function;

// [ETAP 3] JavaFX presentation helpers
// Requirement: Keep reusable control construction separate from application orchestration.
public final class UiFactory {
    private UiFactory() {}

    public static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public static TextField field() {
        TextField field = new TextField();
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static TextArea area() {
        TextArea area = new TextArea();
        area.setPrefRowCount(2);
        area.setWrapText(true);
        return area;
    }

    public static Spinner<Integer> scoreSpinner() {
        Spinner<Integer> spinner = new Spinner<>(0, 100, 50);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    public static Label metric() {
        Label label = new Label("-");
        label.getStyleClass().add("metric-value");
        return label;
    }

    public static VBox card(String title, Label value) {
        Label caption = new Label(title);
        caption.getStyleClass().add("metric-label");
        VBox card = new VBox(8, caption, value);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(18));
        return card;
    }

    public static GridPane form() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(9);
        ColumnConstraints first = new ColumnConstraints(140);
        ColumnConstraints second = new ColumnConstraints();
        second.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(first, second);
        return grid;
    }

    public static void addRow(GridPane grid, int row, String label, Node control) {
        Label caption = new Label(label);
        caption.getStyleClass().add("form-label");
        grid.add(caption, 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    public static HBox actions(Button... buttons) {
        HBox box = new HBox(8, buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox padded(Node node) {
        VBox box = new VBox(node);
        box.setPadding(new Insets(14));
        VBox.setVgrow(node, Priority.ALWAYS);
        return box;
    }

    public static SplitPane split(Node left, Node right) {
        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.70);
        return split;
    }

    public static <S, T> TableColumn<S, T> column(String title, Function<S, T> value, double width) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}
