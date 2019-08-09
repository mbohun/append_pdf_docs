#!/usr/bin/env groovy

@Grab('net.sourceforge.nekohtml:nekohtml:1.9.21')
import org.cyberneko.html.parsers.SAXParser

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper

import java.io.File
import java.io.IOException

// NOTE: XPath numbering/indexing starts from 1, so the table of attachments is index 26 in XPath.
//       However the XPath-like groovy.util.slurpersupport.GPathResult uses numbering/indexing
//       starting from 0, hence the table of attachments is index 25 in GPathResult.
//
def final TABLE_INDEX_ATTACHMENTS = 25 as int

if (this.args.length < 1) {
    println '''
        ERROR: no input HTML file
    '''
    return -1
}

def final htmlFileName = this.args[0]
def final htmlFile = { new File(htmlFileName) }()

def final htmlFileFlag = htmlFile.isFile()
println "htmlFile: ${htmlFile}; isFile(): ${htmlFileFlag}"
if (!htmlFileFlag) {
    // NOTE: Do *NOT* bother continuing
    System.exit(-1)
}

// TODO: we can FIX the broken HTML code table (with regexp) here before continuing

def final parser = new SAXParser()
parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

// TODO: we can do some checks:
//       1. is this is a filename,filepath OR a URL?
//       2. does the file actually exist, etc.
//
def final doc = new XmlSlurper(parser).parse(htmlFile)

def final attachments = [] as Set
def startPage = 0 as int

// NOTE: From the Attachments table extract all the <a href=""> file names
//       TODO: Do we want to extract the text from <a></a> too?
//
doc.body.table[TABLE_INDEX_ATTACHMENTS].'**'.findAll { it.name() == 'a' }*.'@href'.each {
    def final a = [
        'input': it,
        'start-page': startPage,
        'number-of-pages': 42 // NOTE: in order to get the number of pages the input has to be first converted into PDF
    ]

    startPage = startPage + a['number-of-pages']
    attachments << a
}

attachments.collect { it['input'] }.each { println it }

def final htmlDocPdf = PDDocument.load(new File("test_input-00/Patient No 2 Medical Record/Patient No 2 Medical Record_fixed_adj.pdf"))
def final pdfTextStripper = new PDFTextStripper()

// TEST: we will print the page numbers of the pages containing these strings
def final searchTerms = [
    'Registration Details - Patient No: 2',
    'Attachments',
    'Due Diary Entries'
]

def st = searchTerms.pop()

// NOTE: Normally this would be .size() - 1 BUT PDFBox does *NOT* use
//       the element index 0 in the List of PDPage (in the PDDocument) that is
//       to start numbering pages from 1 (and not from 0).
//
def final lastPageno = htmlDocPdf.getPages().size() // or: htmlDocPdf.getNumberOfPages()
println "lastPageno=${lastPageno}"

// NOTE: This is a *VERY* constrained version, that works only if:
//       - the searchTerms (st) are ordered in the order of their appearance in the PDF
//       - each search term (st) is either UNIQUE (or YOU are interested ONLY in the first match)
//
htmlDocPdf.getPages().eachWithIndex { page, pageno ->
    println "searching for: ${st}"
    println "    DEBUG: pageno=${pageno}"
    pdfTextStripper.setStartPage(pageno)
    pdfTextStripper.setEndPage(pageno)
    def final text = pdfTextStripper.getText(htmlDocPdf)

    if (text.contains(st)) {
        println "${st} found on page: ${pageno}"
        if (searchTerms.size()) {
            st = searchTerms.pop()
        } else {
            System.exit(0)
        }
    }

    // NOTE: this should never happen to us, we basically reached the last page and there was NO match
    if (pageno == lastPageno) {
        println "${st} not found!"
        if (searchTerms.size()) {
            st = searchTerms.pop()
        } else {
            System.exit(0)
        }
    }
}
