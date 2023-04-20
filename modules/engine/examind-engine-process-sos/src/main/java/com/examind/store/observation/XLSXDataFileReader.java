/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sis.internal.storage.io.IOUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class XLSXDataFileReader implements DataFileReader {

    private final Workbook workbook;
    private boolean headerAlreadyRead = false;
    private Iterator<Object[]> it = null;

    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");

    public XLSXDataFileReader(Path dataFile) throws IOException {
        final String ext = IOUtilities.extension(dataFile);
        if ("xlsx".equals(ext)){
            File f = null;
            try {
                f = dataFile.toFile();
            } catch (UnsupportedOperationException ex) {
                // don't log. we'll use the inputStream instead
            }
            try {
                if (f == null) {
                    workbook = new XSSFWorkbook(PackageHelper.open(Files.newInputStream(dataFile), true));
                } else {
                    workbook  = new XSSFWorkbook(f);
                }
            } catch (InvalidFormatException ex) {
                throw new IOException(ex);
            }
        } else if ("xls".equals(ext)){
            workbook = new HSSFWorkbook(Files.newInputStream(dataFile));
        } else {
            throw new IllegalArgumentException("Unexpected extension:" + ext);
        }
    }

    @Override
    public Iterator<Object[]> iterator(boolean skipHeaders) {
        final Iterator<Object[]> it = getIterator();
        if (skipHeaders && !headerAlreadyRead && it.hasNext()) it.next();
        return it;
    }

    private Iterator<Object[]> getIterator() {
        if (it == null) {
            final Sheet sheet = workbook.getSheetAt(0);
            final Iterator<Row> rit = sheet.rowIterator();

            it = new Iterator<Object[]>() {


                @Override
                public boolean hasNext() {
                   return rit.hasNext();
                }

                @Override
                public Object[] next() {
                    final Row row = rit.next();
                    int lastColumn = row.getLastCellNum();
                    if (lastColumn > 0) {
                        Object[] results = new Object[lastColumn];
                        for (int cn = 0; cn < lastColumn; cn++) {
                            Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            switch (cell.getCellType()) {
                                case STRING: String value = cell.getRichStringCellValue().getString();
                                             value = value.trim();
                                             results[cn] = value; break;
                                case NUMERIC:
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        results[cn] = cell.getDateCellValue();
                                    } else {
                                        results[cn] =  cell.getNumericCellValue();
                                    } break;
                                case BOOLEAN: results[cn] = cell.getBooleanCellValue(); break;
                                case FORMULA: results[cn] = cell.getCellFormula(); break;
                                default: results[cn] = "";
                            }
                        }
                        return results;
                    } else {
                        LOGGER.finer("Empty line in xls file: " + row.getRowNum());
                        return new String[0];
                    }
                }
            };
        }
        return it;
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    @Override
    public String[] getHeaders() throws IOException {
        final Iterator<Object[]> it = iterator(false);
        headerAlreadyRead = true;
        // at least one line is expected to contain headers information
        if (it.hasNext()) {

            // read headers
            Object[] headers =  it.next();
            String[] results = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                results[i] = (String) headers[i];
            }
            return results;
        }
        throw new IOException("xls headers not found");
    }

}
