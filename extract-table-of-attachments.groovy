#!/usr/bin/env groovy

@Grab('net.sourceforge.nekohtml:nekohtml:1.9.21')
import org.cyberneko.html.parsers.SAXParser

// NOTE: XPath numbering/indexing starts from 1, so the table of attachments is index 26 in XPath.
//       However the XPath-like groovy.util.slurpersupport.GPathResult uses numbering/indexing
//       starting from 0, hence the table of attachments is index 25 in GPathResult.
def final TABLE_INDEX_ATTACHMENTS = 25 as int

if (this.args.length < 1) {
    println '''
        ERROR: no input URL
    '''
    return -1
}

def final html_doc_url = this.args[0]

def final parser = new SAXParser()
parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");

def final doc = new XmlSlurper(parser).parse(html_doc_url)

//NOTE: this works too, BUT it is REALLY DISGUSTING (too generic): doc.body.'**'.findAll { node -> node.name() == 'a' }*.'@href'
doc.body.table[TABLE_INDEX_ATTACHMENTS].'**'.findAll { it.name() == 'a' }*.'@href'.each { println it }
