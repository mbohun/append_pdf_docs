import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.tool.xml.XMLWorkerHelper


def final pdfDoc = new Document()
def final pdfWriter = PdfWriter.getInstance(pdfDoc, new FileOutputStream(outputPdf))
pdfDoc.open()

XMLWorkerHelper.getInstance().parseXHtml(pdfWriter,
                                         pdfDoc,
                                         new FileInputStream(htmlFileName))
pdfDoc.close()
