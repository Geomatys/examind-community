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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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

    public XLSXDataFileReader(Path dataFile) throws IOException {
        final String ext = IOUtilities.extension(dataFile);
        if ("xlsx".equals(ext)){
            workbook = new XSSFWorkbook(Files.newInputStream(dataFile));
        } else if ("xls".equals(ext)){
            workbook = new HSSFWorkbook(Files.newInputStream(dataFile));
        } else {
            throw new IllegalArgumentException("Unexpected extension:" + ext);
        }
    }

    @Override
    public Iterator<String[]> iterator(boolean skipHeaders) {
        final Sheet sheet = workbook.getSheetAt(0);
        final Iterator<Row> it = sheet.rowIterator();

        if (skipHeaders) {
            if (it.hasNext()) {
                // skip headers
                it.next();
            }
        }

        return new Iterator<String[]>() {
            @Override
            public boolean hasNext() {
               return it.hasNext();
            }

            @Override
            public String[] next() {
                List<String> results = new ArrayList<>();
                final Row row = it.next();
                int lastColumn = row.getLastCellNum();
                for (int cn = 0; cn < lastColumn; cn++) {
                    Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    switch (cell.getCellType()) {
                        case STRING: results.add(cell.getRichStringCellValue().getString()); break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                results.add(cell.getDateCellValue() + "");
                            } else {
                                String numVal = cell.getNumericCellValue() + "";
                                // special case where integer are transformed to double
                                if (numVal.endsWith(".0")) {
                                    numVal = numVal.substring(0, numVal.length() -2);
                                }
                                results.add(numVal);
                            } break;
                        case BOOLEAN: results.add(cell.getBooleanCellValue() + ""); break;
                        case FORMULA: results.add(cell.getCellFormula() + ""); break;
                        default: results.add("");
                    }
                }
                return results.toArray(new String[0]);
            }
        };
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    @Override
    public String[] getHeaders() throws IOException {
        final Iterator<String[]> it = iterator(false);

        // at least one line is expected to contain headers information
        if (it.hasNext()) {

            // read headers
            return it.next();
        }
        throw new IOException("xls headers not found");
    }

}
