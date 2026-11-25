package ua.edu.nupp.servetra.client.importer;

import java.util.List;

public record ImportReport(int total, int imported, int duplicates, int invalid, int failed, List<String> errors) {}

