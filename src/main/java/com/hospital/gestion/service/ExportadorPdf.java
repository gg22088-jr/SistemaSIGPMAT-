package com.hospital.gestion.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExportadorPdf {

    private ExportadorPdf() { }

    public static byte[] generar(Informe inf) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Color azul = new Color(20, 54, 93);
            Font fMarca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, azul);
            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font fTexto = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font fCab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font fCelda = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);

            doc.add(new Paragraph("SIGPMAT · Sistema Integral de Gestión de Personal Médico", fMarca));
            doc.add(new Paragraph(inf.titulo(), fTitulo));
            Paragraph periodo = new Paragraph(inf.periodo(), fTexto);
            periodo.setSpacingAfter(10);
            doc.add(periodo);

            List<String> cols = inf.columnas();
            PdfPTable tabla = new PdfPTable(cols.size());
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (String c : cols) {
                PdfPCell celda = new PdfPCell(new Phrase(c, fCab));
                celda.setBackgroundColor(azul);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                celda.setPadding(5);
                tabla.addCell(celda);
            }
            for (List<Object> fila : inf.filas()) agregarFila(tabla, fila, fCelda, null);
            if (inf.totales() != null) agregarFila(tabla, inf.totales(), fTotal, new Color(226, 232, 240));
            doc.add(tabla);

            if (inf.nota() != null) {
                Paragraph nota = new Paragraph(inf.nota(), fTexto);
                nota.setSpacingBefore(10);
                doc.add(nota);
            }
            Paragraph pie = new Paragraph("Generado el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), fTexto);
            pie.setSpacingBefore(4);
            doc.add(pie);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    private static void agregarFila(PdfPTable tabla, List<Object> fila, Font fuente, Color fondo) {
        for (Object v : fila) {
            PdfPCell celda = new PdfPCell(new Phrase(v == null ? "—" : v.toString(), fuente));
            celda.setPadding(4);
            if (v instanceof Number) celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (fondo != null) celda.setBackgroundColor(fondo);
            tabla.addCell(celda);
        }
    }
}
