```
mbohun@linux-7fb8:~/src/convert-docs-to-pdf> ./test_html_scraping_into_pdf.groovy ./index_only_pdfs.html
INPUT file found: docs/test_input_a.pdf
INPUT file found: docs/test_input_b.pdf
INPUT file found: docs/test_input_c.pdf
OUTPUT: test_pdf_merge-1562659164819.pdf
appending: docs/test_input_a.pdf
appending: docs/test_input_b.pdf
appending: docs/test_input_c.pdf
Jul 09, 2019 5:59:25 PM org.apache.pdfbox.multipdf.PDFMergerUtility appendDocument
WARNING: Removed /IDTree from /Names dictionary, doesn't belong there
Jul 09, 2019 5:59:26 PM org.apache.pdfbox.multipdf.PDFMergerUtility appendDocument
WARNING: Removed /IDTree from /Names dictionary, doesn't belong there
```
```
mbohun@linux-7fb8:~/src/convert-docs-to-pdf> ls -lahF test_pdf_merge-1562659164819.pdf
-rw-r--r-- 1 mbohun users 6.2M Jul  9 17:59 test_pdf_merge-1562659164819.pdf
```
