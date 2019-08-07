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
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.multipdf.PageExtractor
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

// TODO:
// - CHECK for resource leaks http://www.pdfbox.org/pdfbox-user-guide/faqs/ (see 1.4)
//
//
//
//println "input length: ${this.args.length}"
println "INPUT (list of files to append): ${this.args}"

// NOTE: consider using the standard groovy CliBuilder for managing the CLI options/args
if (this.args.length < 1) {
    println '''
        ERROR: no input file (with a list of PDF files to append/merge)
    '''
    return -1
}

def final doc_to_append = new File(this.args[0]).collect { it }
doc_to_append.forEach {
    println "    INPUT file found: ${it}"
}

// TODO: cleanup
def final out_pdf = "test_pdf_merge-${System.currentTimeMillis()}.pdf"

try {

    // NOTE: Although we did test/confirm the conversion of images (jpg, png, gif) with pdfbox, at the moment
    //       we are using libreoffice CLI to do the conversion to PDF.
    //
    def final convertor = [
	'pdf':  { file -> PDDocument.load(new File(file)) },
	'jpg':  { file -> createPDDocumentFromImage(file) },
	'jpeg': { file -> createPDDocumentFromImage(file) },
	'png':  { file -> createPDDocumentFromImage(file) },
	'gif':  { file -> createPDDocumentFromImage(file) }
    ]

    // TODO: check if file exists and is readable in a separate step before this?
    //       (as in do not bother with the rest of processing if the input is bogus)
    //
    println "CHECKING if file type supported:"
    def final pdfs = doc_to_append.collect { it ->
        def final ext = it.substring(it.lastIndexOf('.') + 1)
        def final ext_supported_flag = convertor.containsKey(ext)
        println "    CHECKING ${it} is file type supported? ... ${ext_supported_flag}"
        if (ext_supported_flag) {
            convertor[ext](it)
        } else {
            System.exit(-1)
        }
    }
    //println "PDFs loaded: ${pdfs}"

    def final pdfs_number_pages = pdfs.collect {
        it.getNumberOfPages()
    }
    println "PDFs number of pages (per each document): ${pdfs_number_pages}; total: ${pdfs_number_pages.sum()}"

    //NOTE: 1 because we are prepeding

    def start_page = 0 as Integer
    def final startPageDocName = [:] // [0: 'index']
    doc_to_append.eachWithIndex { it, i ->
        startPageDocName[start_page] = it // TODO: CLEANUP: make a list of page offsets
        start_page += pdfs_number_pages[i]
    }
    def buffy = ''<<''
    startPageDocName.each { entry ->
        buffy << '    ' + entry.key + ':' + entry.value + '\n'
    }
    println "map of start page offsets to merged documents:\n${buffy}"

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

    //println "OUTPUT PDF number of pages: ${result.getNumberOfPages()}"

    def final info = new PDDocumentInformation()
    info.setTitle("Merged Documents") //TODO: extract the actual basename wihout the file extension (.pdf)
    result.setDocumentInformation(info)

    // NOTE: pdfMergerUtility.mergeDocuments(mem)
    //       works *ONLY* pdfMergerUtility.addSource()

    createBookmarkPerAppendedDoc(result, startPageDocName) //[0:'test_input_a.pdf', 19: 'test_input_b.pdf', 37: 'test_input_c.pdf'])
    //createAnnotationPerAppendedDoc(result, startPageDocName)
    println 'REPLACING annotations:'
    replaceAnnotations(result, startPageDocName)

    //println "OUTPUT PDF number of pages (after createBookmarks): ${result.getNumberOfPages()}"

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

PDDocument replaceAnnotations(final PDDocument document, final Map originalDocs) {
    document.getPages().each {
        def final page_annotations = it.getAnnotations()
        page_annotations.each {
            if(it instanceof PDAnnotationLink) {
                def final link = (PDAnnotationLink)it
                def final action = link.getAction()
                if(action instanceof PDActionURI) {
                    def final uri = (PDActionURI)action
                    def final oldURI = uri.getURI()
                    if (oldURI.find('^file://')) {
                        def final entry = originalDocs.find { key, value ->
                            //println "...DEBUG: key=${key}, value=${value}";
                            //def final doc_name = (String)value
                            return oldURI.find("${value}\$")
                        }
                        if (entry) {
                            println "    FOUND old annotation: ${oldURI}; REPLACING WITH: ${entry}"
                            def final pageDestination = new PDPageFitWidthDestination()
                            //def final page_no = entry.key as int
                            def final page = document.getPage(entry.key)
                            pageDestination.setPage(page)
                            link.setAction(null) // REMOVE the OLD annotation (action)
                            link.setDestination(pageDestination) // SET the NEW annotation destination (PDPageFitWidthDestination)
                        }
                    }
                    //uri.setURI( newURI );
                }
            }
        }
    }
    return document
}

PDDocument createPDDocumentFromImage(final String fileName) {
    def final doc = new PDDocument()
    def final page = new PDPage()
    doc.addPage(page)

    def final img = PDImageXObject.createFromFileByExtension(new File(fileName), doc)
    println "        DEBUG: created PDImageXObject from: ${fileName}; w=${img.getWidth()} h=${img.getHeight()}"
    page.setMediaBox(new PDRectangle(img.getWidth(), img.getHeight()))
    def final contents = new PDPageContentStream(doc, page)
    contents.drawImage(img, 10, 10, img.getWidth(), img.getHeight())
    contents.close()
    return doc
}
