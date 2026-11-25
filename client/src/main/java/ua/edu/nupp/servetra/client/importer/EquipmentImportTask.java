package ua.edu.nupp.servetra.client.importer;

import javafx.concurrent.Task;
import ua.edu.nupp.servetra.client.api.ApiClient;
import ua.edu.nupp.servetra.client.api.ApiClientException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// [ETAP 4] Background CSV import
// Requirement: Validate and transmit equipment rows without blocking the JavaFX thread.
public class EquipmentImportTask extends Task<ImportReport> {
    private static final Logger LOG = Logger.getLogger(EquipmentImportTask.class.getName());
    private final Path file;
    private final ApiClient api;

    public EquipmentImportTask(Path file, ApiClient api) {
        this.file = file;
        this.api = api;
    }

    @Override
    protected ImportReport call() throws Exception {
        LOG.info("Starting equipment CSV import: " + file.getFileName());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        checkHeader(lines);

        int total = Math.max(0, lines.size() - 1);
        int imported = 0;
        int duplicates = 0;
        int invalid = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (isCancelled()) {
                break;
            }
            int row = i + 1;
            try {
                api.createEquipment(EquipmentCsvMapper.parse(lines.get(i)));
                imported++;
            } catch (ApiClientException error) {
                if ("DATA_CONFLICT".equals(error.code())) {
                    duplicates++;
                } else {
                    failed++;
                }
                addError(errors, row, error.getMessage());
                LOG.warning("CSV row " + row + ": " + error.code());
            } catch (RuntimeException error) {
                invalid++;
                addError(errors, row, error.getMessage());
                LOG.warning("CSV row " + row + ": " + error.getMessage());
            }
            updateProgress(i, Math.max(1, total));
            updateMessage(i + " / " + total);
        }

        ImportReport report = new ImportReport(total, imported, duplicates, invalid, failed, List.copyOf(errors));
        LOG.info("Finished equipment CSV import: total=" + report.total()
                + ", imported=" + report.imported() + ", duplicates=" + report.duplicates()
                + ", invalid=" + report.invalid() + ", failed=" + report.failed());
        return report;
    }

    private static void checkHeader(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        String header = lines.get(0).replace("\uFEFF", "");
        if (!header.equals(EquipmentCsvMapper.HEADER)) {
            throw new IllegalArgumentException("CSV header does not match the documented format");
        }
    }

    private static void addError(List<String> errors, int row, String message) {
        if (errors.size() < 20) {
            errors.add("Row " + row + ": " + message);
        }
    }

    @Override
    protected void failed() {
        super.failed();
        LOG.log(Level.WARNING, "Equipment CSV import failed", getException());
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        LOG.info("Equipment CSV import was cancelled");
    }
}

