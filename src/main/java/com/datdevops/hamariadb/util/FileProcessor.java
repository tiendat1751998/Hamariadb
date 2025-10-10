package com.datdevops.hamariadb.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FileProcessor {

    public List<String[]> processCSVFile(MultipartFile file) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header row
                }

                String[] fields = line.split(",");
                if (fields.length >= 4) { // Minimum required fields
                    records.add(fields);
                }
            }
        }

        log.info("Processed {} records from CSV file", records.size());
        return records;
    }

    public List<String[]> processTextFile(MultipartFile file) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Assuming tab-separated values for text files
                String[] fields = line.split("\t");
                if (fields.length >= 4) { // Minimum required fields
                    records.add(fields);
                }
            }
        }

        log.info("Processed {} records from text file", records.size());
        return records;
    }

    public boolean validateFileSize(MultipartFile file) {
        return file.getSize() <= Constants.MAX_FILE_SIZE;
    }

    public boolean validateFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return false;

        return Validator.isValidFileType(fileName, Constants.ALLOWED_FILE_TYPES);
    }

    public String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastIndex = fileName.lastIndexOf('.');
        return lastIndex == -1 ? "" : fileName.substring(lastIndex + 1).toLowerCase();
    }
}
