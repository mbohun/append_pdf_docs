#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')

import java.io.File
import java.io.IOException

import org.apache.pdfbox.io.MemoryUsageSetting

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDDocument
//import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PageMode
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

import org.apache.pdfbox.multipdf.PageExtractor
import org.apache.pdfbox.multipdf.PDFMergerUtility

if (this.args.length < 1) {
    println '''
        ERROR: no input URL
    '''
    return -1
}

def final html_doc_url = this.args[0]

def final xmlSlurper = new XmlSlurper()
// NOTE: setting these features *HAS* effect, XmlSlurper.parseText() still choked on <!doctype html>
xmlSlurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
// xmlSlurper.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//      WITHOUT:
//          [Fatal Error] index_htmltidy.html:1:10: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
//      WITH:
//          [Fatal Error] index_htmltidy.html:6:3: The element type "meta" must be terminated by the matching end-tag "</meta>".
//      TODO: review all http://apache.org/xml/features if there is a feature for the above tag error problem
//
def final doc = xmlSlurper.parse(html_doc_url)
//println "doc: ${doc}"
//println "doc.name: ${doc.name()}"

def final doc_to_append =
    doc.body.div.find { it.@id == 'main' }.div.find { it.@id == 'important' }.a*.@href*.toString()

doc_to_append.forEach {
    println "INPUT file found: ${it}"
}

// TODO: cleanup
def final out_pdf = "test_pdf_merge-${System.currentTimeMillis()}.pdf"

try {

    def final pdfs = doc_to_append.collect {
        def final pdf_doc_name = it
        //def final info = new PDDocumentInformation()
        //info.setTitle(pdf_doc_name) //TODO: extract the actual basename wihout the file extension (.pdf)

        //PDDocument.metaClass.toString = { ->
        //    pdf_doc_name
        //}

        def final pdf_doc = PDDocument.load(new File(pdf_doc_name))
        //pdf_doc.setDocumentInformation(info)
        pdf_doc
    }
    println "PDFs loaded: ${pdfs}"

    def final pdfs_number_pages = pdfs.collect {
        it.getNumberOfPages()
    }
    println "PDFs number of pages: ${pdfs_number_pages}; total: ${pdfs_number_pages.sum()}"

    def start_page = 0 as Integer
    def final startPageDocName = [:]
    doc_to_append.eachWithIndex { it, i ->
        startPageDocName[start_page] = it // TODO: CLEANUP: make a list of page offsets
        start_page += pdfs_number_pages[i]
    }
    println "map of start page offsets to merged documents: ${startPageDocName}"

    pdfMergerUtility = new PDFMergerUtility()
    //pdfMergerUtility.setDestinationFileName(out_pdf) //

    // TODO: create header from index.html, that will give you header_lenght (in pages)
    def final mem = MemoryUsageSetting.setupMainMemoryOnly()
    def final result = new PDDocument(mem) //TEST: MemoryUsageSetting.setupMainMemoryOnly()
    pdfs.forEach {
        pdfMergerUtility.appendDocument(result,
                                        new PageExtractor(it).extract())
    }

    println "OUTPUT PDF number of pages: ${result.getNumberOfPages()}"

    // NOTE: pdfMergerUtility.mergeDocuments(mem)
    //       works *ONLY* pdfMergerUtility.addSource()

    createBookmarkPerAppendedDoc(result, startPageDocName) //[0:'test_input_a.pdf', 19: 'test_input_b.pdf', 37: 'test_input_c.pdf'])

    println "OUTPUT PDF number of pages (after createBookmarks): ${result.getNumberOfPages()}"

    result.save(out_pdf)
    result.close()

    println "OUTPUT written to: ${out_pdf}"

} catch (Exception e) {
    System.err.println("exception while trying to merge PDF doc: ${out_pdf}; exception: ${e}");
}

PDDocument createBookmarkPerAppendedDoc(final PDDocument document, final Map originalDocs) {
    def final outline = new PDDocumentOutline()
    document.getDocumentCatalog().setDocumentOutline(outline)

    def final pagesOutline = new PDOutlineItem()
    pagesOutline.setTitle("Merged Documents") // patient name history/documents
    outline.addLast(pagesOutline)

    originalDocs.each { docStartPage, docName ->
        def final page = document.getPage(docStartPage)

        def final dest = new PDPageFitWidthDestination()
        dest.setPage(page)

        def final bookmark = new PDOutlineItem()
        bookmark.setDestination(dest)
        bookmark.setTitle(docName)
        pagesOutline.addLast(bookmark)
    }

    pagesOutline.openNode()
    outline.openNode()

    // optional: show the outlines when opening the file
    document.getDocumentCatalog().setPageMode(PageMode.USE_OUTLINES)
    return document
}
