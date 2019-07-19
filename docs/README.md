```
mbohun@linux-7fb8:~/src/convert-docs-to-pdf> cat test_extracted_pdf_files.out 
docs/test_index_fixed.html_wkhtmltopdf.pdf
docs/test_input_a.pdf
docs/test-xray-pelvis.jpg
docs/test_input_b.pdf
docs/test-xray-pelvis.png
docs/test_input_c.pdf
docs/test_input_jpeg.jpeg
```
```
mbohun@linux-7fb8:~/src/convert-docs-to-pdf> ./append_pdf_docs.groovy test_extracted_pdf_files.out 
input length: 1
input: [test_extracted_pdf_files.out]
INPUT file found: docs/test_index_fixed.html_wkhtmltopdf.pdf
INPUT file found: docs/test_input_a.pdf
INPUT file found: docs/test-xray-pelvis.jpg
INPUT file found: docs/test_input_b.pdf
INPUT file found: docs/test-xray-pelvis.png
INPUT file found: docs/test_input_c.pdf
INPUT file found: docs/test_input_jpeg.jpeg
CHECKING docs/test_index_fixed.html_wkhtmltopdf.pdf is file type supported? ... true
converting docs/test_index_fixed.html_wkhtmltopdf.pdf to PDF...
CHECKING docs/test_input_a.pdf is file type supported? ... true
converting docs/test_input_a.pdf to PDF...
CHECKING docs/test-xray-pelvis.jpg is file type supported? ... true
converting docs/test-xray-pelvis.jpg to PDF...
CHECKING docs/test_input_b.pdf is file type supported? ... true
converting docs/test_input_b.pdf to PDF...
CHECKING docs/test-xray-pelvis.png is file type supported? ... true
converting docs/test-xray-pelvis.png to PDF...
CHECKING docs/test_input_c.pdf is file type supported? ... true
converting docs/test_input_c.pdf to PDF...
CHECKING docs/test_input_jpeg.jpeg is file type supported? ... true
converting docs/test_input_jpeg.jpeg to PDF...
PDFs loaded: [org.apache.pdfbox.pdmodel.PDDocument@6b9ce1bf, org.apache.pdfbox.pdmodel.PDDocument@61884cb1, org.apache.pdfbox.pdmodel.PDDocument@75ed9710, org.apache.pdfbox.pdmodel.PDDocument@4fc5e095, org.apache.pdfbox.pdmodel.PDDocument@435871cb, org.apache.pdfbox.pdmodel.PDDocument@609640d5, org.apache.pdfbox.pdmodel.PDDocument@79da1ec0]
PDFs number of pages: [14, 19, 1, 18, 1, 21, 1]; total: 75
map of start page offsets to merged documents: [0:docs/test_index_fixed.html_wkhtmltopdf.pdf, 14:docs/test_input_a.pdf, 33:docs/test-xray-pelvis.jpg, 34:docs/test_input_b.pdf, 52:docs/test-xray-pelvis.png, 53:docs/test_input_c.pdf, 74:docs/test_input_jpeg.jpeg]
OUTPUT PDF number of pages: 75
FOUND old annotation: file:///home/mbohun/src/convert-docs-to-pdf/docs/test_input_a.pdf; REPLACING WITH: 14=docs/test_input_a.pdf
FOUND old annotation: file:///home/mbohun/src/convert-docs-to-pdf/docs/test_input_b.pdf; REPLACING WITH: 34=docs/test_input_b.pdf
FOUND old annotation: file:///home/mbohun/src/convert-docs-to-pdf/docs/test_input_c.pdf; REPLACING WITH: 53=docs/test_input_c.pdf
OUTPUT PDF number of pages (after createBookmarks): 75
OUTPUT written to: test_pdf_merge-1563521735478.pdf
```
```
mbohun@linux-7fb8:~/src/convert-docs-to-pdf> ls -lahF test_pdf_merge-1563521735478.pdf
-rw-r--r-- 1 mbohun users 8.6M Jul 19 17:35 test_pdf_merge-1563521735478.pdf
```
