#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')

import java.io.File
import java.io.IOException

import org.apache.pdfbox.io.MemoryUsageSetting

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PageMode
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem

import org.apache.pdfbox.multipdf.PageExtractor
import org.apache.pdfbox.multipdf.PDFMergerUtility

println "input length: ${this.args.length}"
println "input: ${this.args}"

if (this.args.length < 1) {
    println '''
        ERROR: no input file (with a list of PDF files to append/merge)
    '''
    return -1
}

def final doc_to_append = []

new File(this.args[0]).eachLine {
    doc_to_append << it
}

doc_to_append.forEach {
    println "INPUT file found: ${it}"
}

// TODO: cleanup
def final out_pdf = "test_pdf_merge-${System.currentTimeMillis()}.pdf"

try {

    def final pdfs = doc_to_append.collect {
        PDDocument.load(new File(it))
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

    def final info = new PDDocumentInformation()
    info.setTitle("Merged Documents") //TODO: extract the actual basename wihout the file extension (.pdf)
    result.setDocumentInformation(info)

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
