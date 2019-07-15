#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')

import java.io.File
import java.io.IOException

import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PageMode
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
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

def final doc_to_append = new File(this.args[0]).collect { it }
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

    //NOTE: 1 because we are prepeding

    def start_page = 0 as Integer
    def final startPageDocName = [:] // [0: 'index']
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

    // create the index page and doc
    //def final doc_index = new PDDocument()
    //doc_index.addPage(new PDPage())
    //doc_index.close()

    //pdfMergerUtility.appendDocument(result, doc_index)

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
    //createAnnotationPerAppendedDoc(result, startPageDocName)

    println "OUTPUT PDF number of pages (after createBookmarks): ${result.getNumberOfPages()}"

    result.save(out_pdf)
    result.close()

    println "OUTPUT written to: ${out_pdf}"

} catch (Exception e) {
    System.err.println("exception while trying to merge PDF doc: ${out_pdf}; exception: ${e}");
    e.printStackTrace()
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

PDDocument createAnnotationPerAppendedDoc(final PDDocument document, final Map originalDocs) {
        def final INCH = 72 as float

        def final borderULine = new PDBorderStyleDictionary()
        borderULine.setStyle(PDBorderStyleDictionary.STYLE_UNDERLINE)
        borderULine.setWidth((INCH / 72) as float) // 1 point
    //try {
        // TODO: obviously this is a test, the real thing will be the links in the attachment table
        def final page_index = document.getPage(0)
        annotations_index = page_index.getAnnotations() //List<PDAnnotation

        def final pw = page_index.getMediaBox().getUpperRightX() as float
        def final ph = page_index.getMediaBox().getUpperRightY() as float
        def final font = PDType1Font.HELVETICA_BOLD

        def final contents = new PDPageContentStream(document, page_index)
        contents.beginText()

        def final FONT_SIZE = 10

        contents.setFont(font, FONT_SIZE)

        def final start_page_tx = INCH/2 as float
        def final start_page_ty = (ph - FONT_SIZE) as float
        contents.newLineAtOffset(start_page_tx, start_page_ty)

        def final tx = 0 as float
        def final ty = -(INCH / 4) as float
        originalDocs.each { docStartPage, docName ->
            contents.newLineAtOffset(tx, ty)
            contents.showText(docName)
        }

        contents.endText()
        contents.close()
/*
        originalDocs.each { docStartPage, docName ->
            def final page = document.getPage(docStartPage)

            def final dest = new PDPageFitWidthDestination()
            dest.setPage(page)

            def final pageLink = new PDAnnotationLink()
            pageLink.setBorderStyle(borderULine)

            // Set the rectangle containing the link
            def final textWidth = font.getStringWidth(docName) / 1000 * 18 as float
            def final position = new PDRectangle()
            position.setLowerLeftX(INCH as float)
            position.setLowerLeftY(ph - 2 * INCH - 20 as float)  // down a couple of points
            position.setUpperRightX((INCH + textWidth) as float)
            position.setUpperRightY(ph - 2 * INCH as float)
            pageLink.setRectangle(position)

            def final actionGoto = new PDActionGoTo()
            actionGoto.setDestination(dest)
            pageLink.setAction(actionGoto)

            annotations_index.add(pageLink)
        }
*/
      return document
    //}
}
