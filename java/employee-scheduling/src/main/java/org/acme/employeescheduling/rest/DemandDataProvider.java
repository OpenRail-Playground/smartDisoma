package org.acme.employeescheduling.rest;

import java.net.URL;

import org.acme.employeescheduling.domain.Demand;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DemandDataProvider {

    public static final DateTimeFormatter FORMATTER_EN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter FORMATTER_DE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static List<Demand> readDemands() {
        URL url = DemandDataProvider.class.getResource("/BSA_Export_Schichten_Mai-bis-August.csv");
//        URL url = DemandDataProvider.class.getResource("/BSA_Export_Schichten_Bern_Mai-August.csv");
        List<Demand> demands = new LinkedList<>();
        int currentId = 0;
        int wrongLines = 0;
        try {
            Reader reader = new FileReader(url.getFile());
            CSVParser parser = new CSVParser(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader());
            var records = parser.getRecords();
            for (CSVRecord csvRecord : records) {
                Demand demand = new Demand();
                try {
                    demand.setId(Integer.toString(currentId++));
                    demand.setStart(getStartTime(csvRecord));
                    demand.setEnd(getEndTime(csvRecord));
                    demand.setConstructionSite(getConstructionSite(csvRecord));
                    demand.setRequiredResourceCategory(getRequiredResourceCategory(csvRecord));
                    demand.setRequiredQualifications(getRequiredQualifications(csvRecord));
                    demands.add(demand);
                } catch (Exception e) {
                    wrongLines++;
                    // ignore all invalid input data
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return demands;
    }

    private static LocalDateTime getStartTime(CSVRecord record) {
        try {
            return LocalDateTime.parse(record.get("Schichtstart"), FORMATTER_EN);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(record.get("Schichtstart"), FORMATTER_DE);
        }
    }

    private static LocalDateTime getEndTime(CSVRecord record) {
        try {
            return LocalDateTime.parse(record.get("Schichtende"), FORMATTER_EN);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(record.get("Schichtende"), FORMATTER_DE);
        }
    }

    private static String getConstructionSite(CSVRecord record) {
        return record.get("BSA-ID");
    }

    private static String getRequiredResourceCategory(CSVRecord record) {
        return record.get("Ressourcenkategorie-Typ");
    }

    private static Set<String> getRequiredQualifications(CSVRecord record) {
        return Set.of(record.get("Qualifikationen").split(","));
    }
}
