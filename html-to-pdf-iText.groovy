#!/usr/bin/env groovy

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@Grab('com.itextpdf:itextpdf:5.5.13.1')
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.pdf.PdfWriter

@Grab(group='com.itextpdf.tool', module='xmlworker', version='5.5.13.1')
import com.itextpdf.tool.xml.XMLWorkerHelper

if (this.args.length < 1) {
    println '''
        ERROR: no input HTML file
    '''
    return -1
}

def final htmlFileName = this.args[0]
def final outputPdf = htmlFileName + '_' + System.currentTimeMillis() + '.pdf'

def final pdfDoc = new Document()
def final pdfWriter = PdfWriter.getInstance(pdfDoc, new FileOutputStream(outputPdf))
pdfDoc.open()

XMLWorkerHelper.getInstance().parseXHtml(pdfWriter,
                                         pdfDoc,
                                         new FileInputStream(htmlFileName))
pdfDoc.close()

println "OUTPUT: ${outputPdf}"
