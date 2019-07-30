#!/bin/bash

#echo "INPUT: $1"

# XPath "//html/body/table[26]" (get the 26th table); NOTE: XPath index numbering starts from 1
TABLE_INDEX_ATTACHMENTS=26

table=`cat "$1" | xmllint --html --xmlout - | xmllint --xpath "//html/body/table[${TABLE_INDEX_ATTACHMENTS}]" - `
table_row_count=`echo $table | xmllint --xpath "count(//table/tr)" - `

# NOTE: The first row <tr> of the table is a dummy header (it does NOT contain any data)
#       hence we start from the row 2
#
for i in `seq 2 $table_row_count`
do
    # NOTE: td[1]: date
    #       td[2]: XPath "/font[2]/a/@href" is the attachment file name
    #       td[3]: "Authorised by"
    #       td[4]: Code
    #
    attachment=`echo $table | xmllint --xpath "string(//table/tr[${i}]/td[2]/font[2]/a/@href)" - `
    echo "$attachment"
done
