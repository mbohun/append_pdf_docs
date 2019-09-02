#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSString
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.pdfparser.PDFStreamParser
import org.apache.pdfbox.text.PDFTextStripper

import java.io.File
import java.io.IOException

import groovy.json.JsonOutput

if (this.args.length < 1) {
    println '''
        ERROR: no input PDF file
    '''
    return -1
}

def final pdfFileName = this.args[0]
def final pdf = PDDocument.load(new File(pdfFileName))

def final result = [
    "doc": pdfFileName,
    "pages": [],
    "page-start-offsets": []
]

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

println "${JsonOutput.prettyPrint(JsonOutput.toJson(result))}"
