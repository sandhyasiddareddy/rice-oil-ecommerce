package com.riceoil.platform.imports.application;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ExcelTemplateService {

    public byte[] buildTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("catalog_import");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "SKU", "Product Name", "Category", "Description", "Pack Size", "Unit",
                    "Regular Price", "Stock Quantity", "Image URL", "Published"
            };

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
                sheet.autoSizeColumn(i);
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("RICE-SONA-5KG");
            example.createCell(1).setCellValue("Sona Masoori Rice");
            example.createCell(2).setCellValue("Rice");
            example.createCell(3).setCellValue("Premium daily-use rice");
            example.createCell(4).setCellValue("5");
            example.createCell(5).setCellValue("KG");
            example.createCell(6).setCellValue("499.00");
            example.createCell(7).setCellValue("100");
            example.createCell(8).setCellValue("https://example.com/images/rice.jpg");
            example.createCell(9).setCellValue("false");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate template", ex);
        }
    }
}
