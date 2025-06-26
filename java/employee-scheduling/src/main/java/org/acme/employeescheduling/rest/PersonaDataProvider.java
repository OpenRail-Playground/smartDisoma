package org.acme.employeescheduling.rest;

import io.netty.util.internal.StringUtil;
import org.acme.employeescheduling.domain.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class PersonaDataProvider {

    public static void main(String[] args) {
        // only for testing
        var pers = readPersonas();
        System.out.println("Personas: " + pers);
        pers.forEach(persona -> {
            System.out.println("Persona-quals: " + persona.getQualifications().size());
        });
        System.out.println("Personas size: " + pers.size());
    }

    public static List<Resource> readPersonas() {
        URL url = DemandDataProvider.class.getResource("/resource_personas.csv");
        List<Resource> personas = new LinkedList<>();
        int currentId = 0;
        int wrongLines = 0;
        try {
            Reader reader = new FileReader(url.getFile());
            CSVParser parser = new CSVParser(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader());
            var records = parser.getRecords();
            for (CSVRecord csvRecord : records) {
                Resource resource = new Resource();
                resource.setQualifications(new HashSet<>());
                try {
                    resource.setName(Integer.toString(currentId++));
                    resource.setResourceCategory(csvRecord.get("RESOURCECATEGORY_NAME"));
                    for (int i = 1; i <= 25; i++) {
                        String qualification = csvRecord.get("Q" + i);
                        if (!StringUtil.isNullOrEmpty(qualification)) {
                            resource.getQualifications().add(qualification);
                        }
                    }
                    personas.add(resource);
                } catch (Exception e) {
                    wrongLines++;
                    // ignore all invalid input data
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return personas;
    }
}
