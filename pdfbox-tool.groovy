#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.PDDocumentCatalog
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDField
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField

import java.io.File
import java.io.IOException

import groovy.json.JsonOutput
import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor

import org.w3c.dom.Document
import org.w3c.dom.Element
import groovy.xml.XmlUtil

def final cli = new CliBuilder(usage: 'pdfbox-tool.groovy [options] <PDF file>')
cli.h(longOpt: 'help', 'display usage')
cli.t(longOpt: 'text-extract', 'extract the text from PDF doc', args: 1, type: String)
cli.a(longOpt: 'annotations-list', 'search for PDAnnotations in the PDF doc, and list their details', args: 1, type: String)
cli.f(longOpt: 'form-fields', 'check if the PDF doc contains an AcroForm, and if yes list the form fields', args: 1, type: String)

def final options = cli.parse(args)

if (options.h) {
    cli.usage()
    System.exit(0)
}

def final result = [:]

try {
    if (options['text-extract']) {
        result['doc'] = options['text-extract']
        textExtract(result)
    }
    if (options['annotations-list']) {
        result['doc'] = options['annotations-list']
        annotationsList(result)
    }
    if (options['form-fields']) {
        result['doc'] = options['form-fields']
        formFields(result)
    }

} catch (Exception e) {
    result['error'] = e.getMessage()
}

println "${JsonOutput.prettyPrint(JsonOutput.toJson(result))}"

def textExtract(result) {
    def final pdf = PDDocument.load(new File(result['doc']))
    result["pdf-version"] = pdf.getVersion()
    result["pages-number-total"] = pdf.getPages().size()
    result["pages"] = []
    result["page-start-offsets"] = []

    def offset = 0
    def final allPagesTextBuffer = new StringBuffer()

    def final pdfTextStripper = new PDFTextStripper()
    def final pdfToList = pdf.getPages().eachWithIndex { page, pageno ->
        pdfTextStripper.setStartPage(pageno)
        pdfTextStripper.setEndPage(pageno)

        def final text = pdfTextStripper.getText(pdf)
        result["pages"] << text

        // NOTE: We are merging all page-s text into one StringBuffer (String).
        //       This is required for exact/accurate searching for strings in the
        //       text.
        //       Naive approach is to search for a given string in a text of
        //       one page, BUT that FAILS if your string is pread across multiple
        //       (usually 2 pages). This way we merge/append all pages (text)
        //       into one String, save the page start offset-s into a list of
        //       offsets, then we search for our string-s in the merged string,
        //       and use the list of offsets to identify on which page the string
        //       we are searching for starts.
        //
        allPagesTextBuffer << text

        result["page-start-offsets"] << offset
        offset += text.length()
    }

    def final searchTerms = [
        "\nAttachments Authorised By Code\n",
        "\nDue Diary Entries Authorised By Code\n"
    ]

    result["search-terms-offsets"] = searchTerms.collect {
        def final pos = allPagesTextBuffer.indexOf(it) // we could continue the search from the last match position
        if (pos > -1) {
            def final i = result["page-start-offsets"].findIndexOf { it >= pos }
            if (pos == result["page-start-offsets"][i]) {
                //println "DEBUG: searchTerm=${it}; pos=${pos}; page=${i}"
                i
            } else {
                //println "DEBUG: searchTerm=${it}; pos=${pos}; page=${i-1}"
                i - 1
            }
        } else {
            -1 // pos; searchTerm not found
        }
    }
}

def annotationsList(result) {
    def final pdf = PDDocument.load(new File(result['doc']))
    result["annotations"] = []
    pdf.getPages().eachWithIndex { page, pageno ->
        page.getAnnotations().each { ann ->
            if (ann instanceof PDAnnotationLink) {
                def final link = (PDAnnotationLink)ann
                def final action = link.getAction()
                if(action instanceof PDActionURI) {
                    def final uri = (PDActionURI)action
                    result["annotations"] << uri.getURI()
                }
            }
        }
    }
}

// NOTE: Each PDF doc can contain max 1 AcroForm.
//
def formFields(result) {
    def final pdf = PDDocument.load(new File(result['doc']))
    result["pdf-version"] = pdf.getVersion()
    result["pages-number-total"] = pdf.getPages().size()

    def final documentCatalog = pdf.getDocumentCatalog()
    def final acroForm = documentCatalog.getAcroForm()
    result["pdf-has-acroform"] = (null == acroForm) ? false : true
    if (!result["pdf-has-acroform"]) {
        return
    }

    // NOTE: "XFA forms can be created and used as PDF 1.5 - 1.7 files or as XDP
    //       (XML Data Package). The format of an XFA resource in PDF
    //       is described by the XML Data Package Specification."
    //       https://www.adobe.com/content/dam/acom/en/devnet/acrobat/pdfs/PDF32000_2008.pdf
    //
    //       PDF may contain XFA in XDP format, but XFA may also contain PDF.
    //
    result["acroform-has-xfa"] = acroForm.hasXFA()
    if (result["acroform-has-xfa"]) {
        result["acroform-xfa-if-dynamic"] = acroForm.xfaIsDynamic()
        def final xfa = acroForm.getXFA()
        def final dom = xfa.getDocument().getDocumentElement()
        def final xml = XmlUtil.serialize(dom)

        println "${xml}"
        System.exit(0) //TODO: this is just a TMP solution
        result["xfa"] = xml

    } else {
        // NOTE: This is for *NON* XFA AcroForm processing *ONLY*
        //
        def final fields = acroForm.getFields()
        result["acroform-fields-size"] = fields.size()
        result["acroform-fields"] = fields.collect { field ->
            field.getPartialName()
        }
    }
}
