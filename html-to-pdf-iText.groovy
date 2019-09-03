#!/usr/bin/env groovy

import groovy.xml.XmlUtil

@Grab('net.sourceforge.nekohtml:nekohtml:1.9.22')
import org.cyberneko.html.parsers.SAXParser

@Grapes([
    @Grab(group='com.itextpdf', module='itext7-core', version='7.1.7', type='pom'),
    @Grab(group='com.itextpdf', module='html2pdf', version='2.1.4')
])
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter

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
def final doc = new XmlSlurper(parser).parse(new File(htmlFileName))

// NOTE: Remove the <c></c> tags/elements, because itextpdf doesn't undertand those
//       and it removes them including the text enclosed in <c></c> and that is
//       something we clearly do NOT want
//
def final doc_fixed = XmlUtil.serialize(doc).replace("<c>", "").replace("</c>", "")

// NOTE: Saving the intermediate fixed/adjusted HTML file for review/examination
// of the final PDF.
new File(outputFixedHtml).withWriter { out ->
    out.println "${doc_fixed}"
}

// NOTE: itext7 part (it is much simpler compared to the itext5
def final baseUri = htmlFileName[0..htmlFileName.lastIndexOf(System.getProperty('file.separator'))]
def final converterProperties = new ConverterProperties()
converterProperties.setBaseUri(baseUri);

HtmlConverter.convertToPdf(new ByteArrayInputStream(doc_fixed.getBytes()),
                           new FileOutputStream(outputPdf),
                           converterProperties);

println "OUTPUT: ${outputPdf}"
