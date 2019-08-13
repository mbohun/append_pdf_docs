#!/usr/bin/env groovy

@Grab('org.apache.pdfbox:pdfbox:2.0.15')
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
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
def final pdfTextStripper = new PDFTextStripper()


def pageno = 0
def final pdfToList = pdf.getPages().collect { page ->
    pdfTextStripper.setStartPage(pageno)
    pdfTextStripper.setEndPage(pageno)
    pageno++

    pdfTextStripper.getText(pdf)
}

//println "pdfToList.size(): ${pdfToList.size()}"
println "${JsonOutput.prettyPrint(JsonOutput.toJson(pdfToList))}"
