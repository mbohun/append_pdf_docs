#!/usr/bin/env groovy

import groovy.xml.XmlUtil

@Grab('net.sourceforge.nekohtml:nekohtml:1.9.22')
import org.cyberneko.html.parsers.SAXParser

@Grab('com.itextpdf:itextpdf:5.5.13.1')
import com.itextpdf.text.Document
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
def final outputFileName = htmlFileName + '_' + System.currentTimeMillis()
def final outputFixedHtml = outputFileName + '_fixed.html'
def final outputPdf = outputFileName + '.pdf'

// NOTE: We have to fix/adjust the HTML input (that is most of the time NOT well formed)
//       and hence it would cause com.itextpdf.tool.xml.XMLWorkerHelper throw an
//       exception, and fail.
//
def final parser = new SAXParser()
parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
//parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true)
parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

// NOTE: Remove the <c></c> tags/elements, because itextpdf doesn't undertand those
//       and it removes them including the text enclosed in <c></c> and that is
//       something we clearly do NOT want
//
def final htmlCode = new File(htmlFileName).getText("WINDOWS-1252").replace("<c>", "").replace("</c>", "") //.replace("<c/>", "")
def final doc = new XmlSlurper(parser).parseText(htmlCode)
def final doc_fixed = XmlUtil.serialize(doc)

// NOTE: Saving the intermediate fixed/adjusted HTML file for review/examination
// of the final PDF.
new File(outputFixedHtml).withWriter { out ->
    out.println "${doc_fixed}"
}

def final pdfDoc = new Document()
def final pdfWriter = PdfWriter.getInstance(pdfDoc, new FileOutputStream(outputPdf))
pdfDoc.open()

XMLWorkerHelper.getInstance().parseXHtml(pdfWriter,
                                         pdfDoc,
                                         new ByteArrayInputStream(doc_fixed.getBytes()))

pdfDoc.close()

println "OUTPUT: ${outputPdf}"
