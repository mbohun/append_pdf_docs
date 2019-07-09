#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import java.io.File;
import java.io.IOException;

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
println "OUTPUT: ${out_pdf}"

try {

    def final pdfMergerUtility = new PDFMergerUtility()
    pdfMergerUtility.setDestinationFileName(out_pdf); //TODO

    def final pdfDocumentInformation = new PDDocumentInformation();
    pdfDocumentInformation.setTitle("merge docs to PDF test title");
    pdfDocumentInformation.setCreator("martin.bohun@gmail.com");
    pdfDocumentInformation.setSubject("merge docs to PDF test subject");

    // TODO: create new index/header but with the index.html a href-s converted to PDF hotlinks/bookmarks
    //
    //pdfMergerUtility.addSource();

    doc_to_append.forEach {
        def final doc_file = it
        println "appending: ${doc_file}"

        // TODO: 1. handle diff file type-s here, i.e. convert everything to PDF
        //       2. add bookmark
        pdfMergerUtility.addSource(new File(doc_file));
    }

    pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

} catch (Exception e) {
    System.err.println("exception while trying to merge PDF doc: ${out_pdf}; exception: ${e}");
}
