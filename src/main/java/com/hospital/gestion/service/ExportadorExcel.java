package com.hospital.gestion.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExportadorExcel {

    private ExportadorExcel() { }

    public static byte[] generar(Informe inf) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet(inf.tipo());

            Font fTitulo = wb.createFont();
            fTitulo.setBold(true);
            fTitulo.setFontHeightInPoints((short) 14);
            Font fNegrita = wb.createFont();
            fNegrita.setBold(true);
            Font fBlanca = wb.createFont();
            fBlanca.setBold(true);
            fBlanca.setColor(IndexedColors.WHITE.getIndex());

            CellStyle sTitulo = wb.createCellStyle();
            sTitulo.setFont(fTitulo);
            CellStyle sCab = wb.createCellStyle();
            sCab.setFont(fBlanca);
            sCab.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            sCab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sCab.setAlignment(HorizontalAlignment.CENTER);
            sCab.setWrapText(true);
            CellStyle sTot = wb.createCellStyle();
            sTot.setFont(fNegrita);
            sTot.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            sTot.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            poner(hoja.createRow(0), 0, "SIGPMAT · " + inf.titulo(), sTitulo);
            poner(hoja.createRow(1), 0, inf.periodo(), null);
            if (inf.nota() != null) poner(hoja.createRow(2), 0, inf.nota(), null);

            int filaCab = 4;
            List<String> cols = inf.columnas();
            Row cab = hoja.createRow(filaCab);
            for (int c = 0; c < cols.size(); c++) poner(cab, c, cols.get(c), sCab);

            int r = filaCab + 1;
            for (List<Object> fila : inf.filas()) {
                Row row = hoja.createRow(r++);
                for (int c = 0; c < fila.size(); c++) poner(row, c, fila.get(c), null);
            }
            if (inf.totales() != null) {
                Row row = hoja.createRow(r);
                for (int c = 0; c < inf.totales().size(); c++) poner(row, c, inf.totales().get(c), sTot);
            }

            for (int c = 0; c < cols.size(); c++) {
                int max = cols.get(c).length();
                for (List<Object> fila : inf.filas()) {
                    Object v = fila.get(c);
                    if (v != null) max = Math.max(max, v.toString().length());
                }
                hoja.setColumnWidth(c, Math.min(45, Math.max(12, max + 3)) * 256);
            }
            hoja.createFreezePane(0, filaCab + 1);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void poner(Row fila, int col, Object valor, CellStyle estilo) {
        Cell celda = fila.createCell(col);
        if (valor instanceof Number n) celda.setCellValue(n.doubleValue());
        else celda.setCellValue(valor == null ? "—" : valor.toString());
        if (estilo != null) celda.setCellStyle(estilo);
    }
}
