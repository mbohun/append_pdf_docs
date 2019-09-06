#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink

import java.io.File
import java.io.IOException

import groovy.json.JsonOutput
import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor

def final cli = new CliBuilder(usage: 'pdfbox-tool.groovy [options] <PDF file>')
cli.h(longOpt: 'help', 'display usage')
cli.t(longOpt: 'text-extract', 'extract the text from PDF doc', args: 1, type: String)
cli.a(longOpt: 'annotations-list', 'search for PDAnnotations in the PDF doc, and list their details', args: 1, type: String)

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

} catch (Exception e) {
    result['error'] = e.getMessage()
}

println "${JsonOutput.prettyPrint(JsonOutput.toJson(result))}"

def textExtract(result) {
    def final pdf = PDDocument.load(new File(result['doc']))
    
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
