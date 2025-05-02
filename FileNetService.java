// NOTES: (each note is prepended with a tag name in all caps and this tag may be referred to 
//         in other comments throughout the rest of the config file)
//
// - TABLES_VS_LISTS: lookup tables should be JSON "objects", but lists should be JSON "arrays" 
//
// - OPTIONAL_VS_MANDATORY:  All top-level config elements (JSON "attributes") are optional 
//   (should be no failures or unwanted side-effects to the AFP, due to the config element not being 
//   present).  If a given config element IS present, then all attributes within it (recursively) 
//   are mandatory, unless otherwise specified in comments.  Any missing mandatory elements 
//   will cause the dataprep to ERROR_OUT.
//
// - ERROR_OUT: When any fatal error condition occurs, dataprep will return a exit code of 1, and it 
//   will always attempt to continue processing the rest of the statements if possible (marking each 
//   statement as bad and logging errors before proceeding), before exiting, so that we have a 
//   complete log of errors for all statements before exiting the process. Currently the jolt 
//   framework is being used and it notes such "rejected" statements in jolt_stmt_report.txt when 
//   the proper exception handling mechanisms are used.
//
// - ORDER_OF_OPERATIONS: In general, for any given page, any "scraping" logic (collecting 
//   info) should be done first, followed by any deletions, followed by any additions.  This general 
//   rule avoids race conditions.
//
// - PAGE_NUMBERS: Unless otherwise stated, all page numbers specified in this config refer to 
//   the ORIGINAL page numbers, before any modifications have been applied, on a per-statement basis 
//   (meaning if you see a page number 3 below, it's referring to page 3 of that particular 
//   statement, not the 3rd page in the whole file)
//
// - TEXT_DELETION: To avoid interfering with existing possible relative move commands and other 
//   sub-commands within a PTX record, text deletions should simply be setting the TRN record values 
//   to an empty string only; leave the rest of the PTX as is. 
//
// - TEXT_ADDITION: To avoid conflict with any existing MCF and PTX records, when adding 
//   new text to a page, the new MCF and PTX records should be inserted only after the last existing 
//   PTX record on the page.  Also, there are two ways to specify fonts in all configurations 
//   which use them.  You provide either the coded_font_name, by itself, or provide both the 
//   character_set and code_page. For the values not provided, use a null value. See 
//   INSERT_NEW_PAGES->mcf_recs for example.  Also, see http://pvsweb/fonts/cfl_4_0.htm for our 
//   standard fonts.
//
// - FROM_DERIVEDDATA: We allow special strings which act as variables to be embedded in the values of NOPs or 
//   TLEs.  The special string "FROM_DERIVEDDATA^someVariableName^", is usable from 
//   INSERT_FILE_HEADER_NAMED_NOPS, INSERT_FILE_TRAILER_NAMED_NOPS, INSERT_MAIL_PIECE_NAMED_NOPS, 
//   INSERT_MAIL_PIECE_TLES.  If encountered, the substring between the two 
//   carrots ("^") specify the name of the "variable" to be subtituted at that position within the 
//   larger string value.  For example, if a TLE "value" (second half of the name-value pair) is 
//   specified as "Page Count: FROM_DERIVEDDATA^mailPieceNumPages^ (whole mail piece)", then 
//   the resulting TLE value should be "Page Count: 37 (whole mail piece)", assuming the derived 
//   data value for mailPieceNumPages was 37.
//   Note that different pieces of data are derived at different times and thus are available 
//   only at specific phases of processing.  In particular 3 different phases:
//    1) any info available at the start of processing, such as command line arguments
//    2) any info only available after a given mail piece has finished being processed, such as 
//       final number of pages in the mail piece
//    3) any info only available after all mail pieces have been processed, such as total number 
//       of pages in the whole file
//   ...so, the specific list of available derived values are listed in the comments of each of 
//   the config objects which can use them.  Dataprep will ERROR_OUT if an invalid value is used.
//
// - FROM_METADATA: Similar to "FROM_DERIVEDDATA^someVariableName^", there is a 
//   "FROM_METADATA^someVariableName^" available for use in mail-piece-level TLEs, NOPs and 
//   TRNs. Available values are any MAILPIECE top-level string or numerical (convert numbers to 
//   strings and keep the same number of decimal places as was in the metadata file) value from the 
//   JSON metadata file.  Any null values should be treated as an empty string; however if 
//   someVariableName doesn't exist at all in the metadata, dataprep will ERROR_OUT. (If you are 
//   creating a new config file here and want to see what metadata values are currently available, 
//   see //clientsystems/pfg/pdf2print/MAIN/doc/metadata_template.json for the latest specification, 
//   but the dataprep code is not hardcoded or configured to these values; so it is possible to 
//   insert new metadata values and they should work.) Additionally, as and exception to the general 
//   rule ("top-level"), the address lines specified by the send_address_lines array will be made 
//   available as send_address_line_1 through send_address_line_6, and if any of those lines are not 
//   given in the metadata, just make the value an empty string (rather than failing).
//
// - SEND_ADDRESS_PARSING:
//   - If metadata field, "is_bulk_shipping", is "Y", and neither SCRAPE_SEND_ADDRESS nor 
//     SCRAPE_AND_MOVE_SEND_ADDRESS are populated in the config, then use the metadata 
//     'send_address_lines' in the following logic; otherwise, if one of the scrape configurations
//     exists, then use the scraped address lines in the following logic:
//   - If is_foreign_address (from metadata file) is "Y", then populate DOC_SEND_ADDR_ZIP_CODE
//     statement-level TLE with "NSF" (this tells afgpen not to try to place the IMB or do CASS),
//     and set the DOC_SPECIAL_HANDLING_CODE statement-level TLE to "93" if is_bulk is "Y", 
//     otherwise set it to "78".  This value should override any other DOC_SPECIAL_HANDLING_CODE which 
//     may be set in INSERT_MAIL_PIECE_TLES and/or INSERT_MAIL_PIECE_NAMED_NOPS.
//   - If is_foreign_address is "N", and "is_bulk_shipping", is "Y", use the metadata 'zip_code'
//     to populate the DOC_SEND_ADDR_ZIP_CODE statement-level TLE.
//   - If is_foreign_address is "N", then parse the address lines to find the zipcode
//     zipcode and populate the DOC_SEND_ADDR_ZIP_CODE statement-level TLE.
//     Here is the algorithm for finding the zipcode.  It was taken from ROB (which these 
//     statements were migrated from) and is not very robust, and it does not take into consideration 
//     the format of city and state, but it works, and we're going to do the same, because this system 
//     was created based on migrations from ROB:
//       Loop through all the characters of the last address line, from the end of the 
//       line, toward the front, skipping any spaces or dashes, and collecing all digits 
//       found into a buffer, until you reach any non-digit character (other than spaces or 
//       dashes).  If, when you stop, you have collected 5 or 9 digits, assume that's the zip 
//       code and use it to populate the DOC_SEND_ADDR_ZIP_CODE statement-level TLE; otherwise
//       just populate the TLE with an empty string (in which case afpgen should assign it SH 04).
//
// - ADD to these comments above, ANY OTHER DATAPREP REQUIREMENTS WHICH ARE NOT CONFIG DRIVEN; 
//   the goal is to capture all of dataprep's behavior in this file. However, try to minimize 
//   creating non-config-driven logic and instead make everything as config-driven as possible, so 
//   that it's explicit.  For instance, instead of just adjusting the dataprep code to 
//   automatically add the FILE_STATEMENT_COUNT NOP to every file and noting here that it happens, 
//   put it in the INSERT_FILE_TRAILER_NAMED_NOPS section and use the value 
//   "FROM_DERIVEDDATA:fileNumPackages", as in the example below, so that all dataprep actions are 
//   explicitly shown in this config; making this a clear and complete document of dataprep behavior.

{
    "INSERT_BNG_ENG_RECS" : "Y",
        // If this is present and set to "Y", adds up the "page_count" values in the metadata 
        // file for each mailpiece and use that total to know where one mail piece starts and the 
        // next begins; then inserts BNG records before each mailpiece and ENG records after each one.
        // (BNG signifies the start of each mailpiece and ENG the end)
        // This happens up-front, in the loader logic, before any of the analyzer logic, so that the
        // statements can be separated and processed properly through the analyzer and unloader.

	"TRANSLATE_IMM_RECS" : [ // NOT YET IMPLEMENTED
        // Looks at every page and if it finds an input IMM listed here, it replaces it with the corresponding output IMM.
        //   - This happens before SET_IMM_RECS, and should not apply to any new sheets added with
        //     INSERT_NEW_PAGES, which specifies it's own IMM rec.
        // <input IMM name> : <output IMM name>
        { "KJCNHFC1" : "CISC0000" }, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
		{ "DOCD0370" : "AFPBIN2S" },
		{ "DESD0011" : "F2CLM159" },
	],

    "SET_IMM_RECS" : [
        // Inserts the IMM record for the specified pages (just before the BPG record) in each mail piece.
        // If there already was an existing IMM just before that page's BPG record, it will be replaced.
        // <page_num> : <IMM value (also called "CopyGroup")>
        { "1":"TRAY02DP" }, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
        { "5":"TRAY03DP" }
        // Note:
        //  - Remember that once an IMM rec is set, it applies to all following pages until another IMM replaces
        //    it; so you only need to set it once, on the first page, for a whole statement if all the statement's
        //    sheets use the same copygroup.
        //  - Even though most of these entries will be for odd pages (because IMM's apply to 
        //    both sides of the sheet), we could also effectively choose to end a sheet 
        //    prematurely by setting even pages if we want.  Example:
        // "1" : "TRAY02DP"
        // "2" : "TRAY01DP"
        // This would result in afpgen seeing the first page as start of sheet 1, then it 
        // sees the start of sheet 2 (TRAY01DP), in which case it will insert a black back 
        // page on the first sheet with the appropriate factory controls.  This should be a 
        // valid use-case, as it's commonly done via exstream as well.
        //  - This should happen after TRANSLATE_IMM_RECS, so that any IMMs specified in this 
        //    object override any specified in TRANSLATE_IMM_RECS.
        //  - This should not apply to any new sheets added with INSERT_NEW_PAGES, which 
        //    specifies it's own IMM rec.
    ],

    "EACH_PDF_DOCUMENT_STARTS_NEW_SHEET" : "Y", // "Y" or "N" ; NOT YET IMPLEMENTED
        // If "Y", then, after any IMM changes have already been made (TRANSLATE_IMM_RECS, 
        // SET_IMM_RECS), dataprep will add an IMM record just before the BPG record of each PDF 
        // document which was converted into AFP input, if an IMM record doesn't already exist there.
        // - Dataprep knows which BPG records represent the start of a new PDF by looking at 
        //   the metadata MAILPIECE->pdf_documents and comparing page counts to page numbers in memory.
        // - The IMM record inserted will be identical to the IMM which is in effect for the 
        //   previous sheet (which may have been set by TRANSLATE_IMM_RECS or SET_IMM_RECS).  
        //   It is assumed that at least the first sheet of each statement starts with an 
        //   explicit IMM record (as opposed to just implicitly relying on whatever the 
        //   previous IMM record was somewhere in the input file before that statement); so 
        //   that the dataprep can always figure out which IMM is "in effect".  If this is not 
        //   the case, then dataprep will ERROR_OUT.

	"INSERT_ID_HOPPER_LOOKUP" : [ // NOT YET IMPLEMENTED
        // Maps values of MAILPIECE->insert_ids to hopper numbers for construction of the 
        // DOC_INSERT_COMBO TLE, which is subsequently created at the mail package level.
        // All hoppers default to "N" in the TLE value ("NNNNNNNNNNN") and then any IDs which match 
        // a tag here cause a "Y" to be set in the character position of the 11 digit string corresponding to
        // value of the tag. For example, the below should produce: "NNNNYYNNNNY"
        // <insert_id> : <hopper_number>
		{ "ADVERT02" : 5 },
		{ "ADVERT23" : 6 },
        { "VV" : 11 }
	],

	"SHIPPING_SH_CODE_LOOKUP" : [ // NOT YET IMPLEMENTED
        // Maps values of MAILPIECE->shipping_carrier and MAILPIECE->shipping_service (from the 
        // metadata file) to special handling codes.  The matched code will be used to set the 
        // DOC_SPECIAL_HANDLING TLE at the beginning of each mail piece.  Note that those 
        // metadata values may be null or non-existent, in which case they're treated like empty 
        // strings.  Matching is case-insensative and asterisks may be used to match all values.  
        // Because of this wildcard ability, the matches should be attempted against the below 
        // objects in the order in which they are listed here.  For example, given this sample 
        // config, if we have a carrier "Fedex" and service "Ground" the result should be SH 88.
        // Also, note that DOC_SPECIAL_HANDLING TLE may still be overriden by the address processing logic
        // (see SEND_ADDRESS_PARSING).
		{ "carrier" : "Fedex", "service" : "Overnight", sh_code : 87 },
		{ "carrier" : "Fedex", "service" : "*", sh_code : 88 },
		{ "carrier" : "USPS", "service" : "Ground", sh_code : 89 },
		{ "carrier" : "*", "service" : "*", sh_code : 99 } // this is a default catch-all if nothing above matched
	],

    "DELETE_TEXT": [ // NOT YET IMPLEMENTED
        // - Deletes any text found in all PTX records (don't stop at just one TRN or one PTX) 
        //   which have content in the defined box with the defined orientation.
        // - Note TEXT_DELETION at top of file
        // - All deletions should be done before any other text additions for any given page.
        // - If no text is found, print a warning, but do not fail.
        {
            "page_num": 1,
            "x1": 8.2533,
            "y1": 1.0,
            "x2": 8.4145,
            "y2": 1.4881,
            "orientation": 90 // degrees; valid values are: 0, 90, 180, 270; ERROR_OUT if invalid value
        },
        {
            "page_num": 5,
            "x1": 2.35,
            "y1": 2.0,
            "x2": 6.234,
            "y2": 3.81,
            "orientation": 0 // degrees; valid values are: 0, 90, 180, 270
        }
    ],

    "INSERT_FILE_HEADER_NAMED_NOPS" : [
        // file header level, afpgen-formatted (named) NOPS
        // - These should be inserted before the BRG record and in the order defined here
        // - "named" as opposed to a "normal" NOP means that the NOP fits the agreed to format for afpgen input as follows:
        //  <name><null><value><null>
        //  , where <name> is the JSON property, <value> is the JSON value, and <null> is a nul character (\0)
        // - See FROM_DERIVEDDATA notes for dynamic data usage.
        //   Available DERIVEDDATA values are:
        //       corp (taken from command line: -corp)
        //       cycle (taken from command line: -cycle)
        //       rundate (taken from command line: -rundate)
        { "UR_PRODUCT_ID": "NWMI/BILLS" },
        { "FILE_CORP_ID": "FROM_DERIVEDDATA^corp^" }
    ],

    "INSERT_FILE_TRAILER_NAMED_NOPS" : [
        // file trailer level, afpgen-formatted (named) NOPS
        // - These should be inserted after the EDT record and in the order defined here
        // - "named" as opposed to a "normal" NOP means that the NOP fits the agreed to format for afpgen input as follows:
        //  <name><null><value><null>
        //  , where <name> is the JSON property, <value> is the JSON value, and <null> is a nul character (\0)
        // - See FROM_DERIVEDDATA notes for dynamic data usage.
        //   Available DERIVEDDATA values are:
        //       corp (taken from command line: -corp)
        //       cycle (taken from command line: -cycle)
        //       rundate (taken from command line: -rundate)
        //       fileNumPages (from internal count; sum of all mailPieceNumPages)
        //       fileNumSheets (from internal count; sum of all mailPieceNumSheets)
        //       fileNumPackages (from internal count; number of mail pieces in the entire output file)
        { "FILE_STATEMENT_COUNT": "FROM_DERIVEDDATA^fileNumPackages^" },
        { "FILE_TOTAL_AMOUNT_DUE": "0" }
    ],

    "INSERT_MAIL_PIECE_TLES" : [
        // statement (mail package) level, TLE recs (they already have name/value pairs by default, unlike NOPS)
        // - These should be inserted just after the BNG record and in the order defined 
        //   here, after any INSERT_MAIL_PIECE_NAMED_NOPS (for now; the order between tles and 
        //   nops may need adjusting in the future).
        // - See FROM_METADATA and FROM_DERIVEDDATA notes for dynamic data usage.
        //   Available DERIVEDDATA values are:
        //       corp (taken from command line: -corp)
        //       cycle (taken from command line: -cycle)
        //       rundate (taken from command line: -rundate)
        //       mailPieceNumPages (from internal count, after all insertions are accounted for)
        //       mailPieceNumSheets (from internal count, after all insertions are accounted for, including IMMs which could create 1-page sheets)
        { "DOC_PRODUCT_CODE": "PS,QV", },
        { "DOC_SPECIAL_HANDLING_CODE": "99", }, // this is the default; may be overriden by SCRAPE_SEND_ADDRESS
        { "BULK_SHIPPING": "FROM_METADATA^is_bulk^", },
        { "DOC_ACCOUNT_NUMBER": "FROM_METADATA^infact_account_number^", },
        { "UC_DOCUMENT_CLASSIFICATION_01": "late payment letter" }
    ],

    "INSERT_MAIL_PIECE_NAMED_NOPS" : [
        // statement (mail package) level, afpgen-formatted (named) NOPS
        //   - These should be inserted just after the BNG record and after any INSERT_MAIL_PIECE_TLES and in the order defined here
        //   - "named" as opposed to a "normal" NOP means that the NOP fits the agreed to format for afpgen input as follows:
        //    <name><null><value><null>
        //    , where <name> is the JSON property, <value> is the JSON value, and <null> is a nul character (\0)
        // - See FROM_METADATA and FROM_DERIVEDDATA notes for dynamic data usage.
        //   Available DERIVEDDATA values are:
        //       corp (taken from command line: -corp)
        //       cycle (taken from command line: -cycle)
        //       rundate (taken from command line: -rundate)
        //       mailPieceNumPages (from internal count, after all insertions are accounted for)
        //       mailPieceNumSheets (from internal count, after all insertions are accounted for, including IMMs which could create 1-page sheets)
        { "DOC_PRODUCT_CODE": "PS,QV", },
        { "DOC_SPECIAL_HANDLING_CODE": "99", }, // this is the default; may be overriden by SCRAPE_SEND_ADDRESS
        { "BULK_SHIPPING": "FROM_METADATA^is_bulk^", },
        { "DOC_ACCOUNT_NUMBER": "FROM_METADATA^infact_account_number^", },
        { "UC_DOCUMENT_CLASSIFICATION_01": "late payment letter" }
    ],

    "INSERT_OVERLAYS" : [ // NOT YET IMPLEMENTED
        // Inserts IPO records only; the overlays themselves are expected to already exist in natres (the National Resource Respository, used by the printers).
        // The new records will be inserted immediately after the BPG record of the page number listed.
        { 
            "page_num": 1,
            "x_offset": 0.0,
            "y_offset": 0.0,
            "overlay_name": "S1NWTL01"
        },
        { 
            "page_num": 8,
            "x_offset": 0.2,
            "y_offset": 0.2,
            "overlay_name": "S1NWTL08"
        },
        { 
            "page_num": 21,
            "x_offset": 2.289,
            "y_offset": 3.501,
            "overlay_name": "S1NWTL21"
        }
    ],

    "INSERT_PSEGS" : [
        // Inserts IPS records only; the page segments themselves are expected to already exist in natres (the National Resource Respository, used by the printers).
        // The new records will be inserted immediately after the first EAG record of the page number listed.
        { 
            "page_num": 2,
            "x_offset": 0.82,
            "y_offset": 0.8,
            "pseg_name": "S1NWLG02"
        },
        { 
            "page_num": 3,
            "x_offset": 0.82,
            "y_offset": 0.8,
            "pseg_name": "S1NWLG03"
        },
        { 
            "page_num": 18,
            "x_offset": 0.82,
            "y_offset": 0.8,
            "pseg_name": "S1NWLG18"
        }
    ],

    "SCRAPE_SEND_ADDRESS": { // PARTIALLY IMPLEMENTED: non-zero orientations need testing, PTX's with RMI/RMB recs need implementing
        // - Reads all text specified by the below scrape window and text orientation from page 1,
        //   populates DOC_SEND_ADDR_LINE1 through DOC_SEND_ADDR_LINE6 statement-level TLEs 
        //   (same as INSERT_MAIL_PIECE_TLES) in numerically sorted order (same place as specified 
        //   for INSERT_MAIL_PIECE_TLES).  Any unused lines should still be created as TLEs 
        //   with empty strings.
        // - see SEND_ADDRESS_PARSING notes
        // - If no text is found at all in the scrape window, dataprep will ERROR_OUT.
        "x1": 8.2533,
        "y1": 1.0,
        "x2": 8.4145,
        "y2": 1.4881,
        "orientation": 0 // degrees; valid values are: 0, 90, 180, 270
    },

    "SCRAPE_AND_MOVE_SEND_ADDRESS": { // NOT YET IMPLEMENTED
        // 1) Run the same logic as for SCRAPE_SEND_ADDRESS, using the below
        // "from" box (x1/y1 gives top left corner, x2/y2 gives bottom right corner)
        // 2) Delete the scraped text from the page, while adhering to TEXT_DELETION notes.
        // 3) Place the scraped address lines at a new location given by the "to" object 
        // Notice how "to" does not need a bottom right corner, only needs the top-left,
        // to start the first line, and that a change of font/line-spacing is optional.
        "from": {
            "x1": 8.2533,
            "y1": 1.0,
            "x2": 8.4145,
            "y2": 1.4881,
            "orientation": 0 // degrees; valid values are: 0, 90, 180, 270
        },
        "to": {
            // see TEXT_ADDITION notes
            "x1": 8.2533,
            "y1": 1.0,
            "orientation": 0, // degrees; valid values are: 0, 90, 180, 270
            "new_font" : { // optional, creates a new MCF record with new font, just before the new PTX record
                "coded_font_name": null,
                "character_set": "C0CGI080",
                "code_page": "T1E4LAT1",
                "font_description": "Arial CFL Mono 8pt"
                    // This is just to have a human-readable note of which font this is;
                    // used as documentation in this config and in log messages.
                    // It is not used in the actual MCF rec.

            },
            "line_spacing" : 0.15 // optional; increment the Y coordinate by this many inches for
                                  // each new address line added after the first. If unspecified, keep
                                  // the same line-spacing as original text.
        },
    },

    INSERT_NEW_PAGES: [ // NOT YET IMPLEMENTED
        // Can be used to create coversheets or sheets anywhere within the statement;
        // even single-sided sheets (1 page specified)
        {
            "page_num" : 1, // The new page would be inserted before the specified ORIGINAL 
                            // page_num; in this case, becoming the new page 1.  Also if 
                            // multiple entries here specify the same page_num, the output
                            // should be in same order as listed in this config.
                            // (this would happen if inserting a whole sheet, for example)
            "imm_rec": "TRAY02DL", // optional; ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
            "bpg_rec": "FRNTCOVR",
                // - If string is empty("") PageName defaults to page number in file
                // - ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
            "bag_rec": null, // value is ignored
            "mcf_recs": [ // optional
                // see TEXT_ADDITION notes regarding two font specification methods.
                {
                    "id": 1,
                    "coded_font_name": "X0AMJE04", // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "character_set": null, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "code_page": null, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "font_description": "Arial CFL Mono 8pt" // This is just to have a human-readable note of which font this is;
                                                             // used as documentation in this config and in log messages.
                                                             // It is not used in the actual MCF rec.

                },
                {
                    "id": 2,
                    "coded_font_name": null,
                    "character_set": "C0CGI080",
                    "code_page": "T1E4LAT1",
                    "font_description": "Frutiger CFL 11pt"
                }
            ],
            "pgd_rec": {
                "width": 8.5,
                "height": 11,
                "dot_per_inches": 300 // applies to both x and y axis; optional, 1440 is default
            },
            "ptd_rec": {
                "width": 8.5,
                "height": 11
            },
            "eag_rec": null, // value is ignored
            "ipo_recs": [ // optional
                {
                    "x_offset": 0.0,
                    "y_offset": 0.0,
                    "overlay_name": "O1NWTP30" // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                },
                {
                    "x_offset": 0.25,
                    "y_offset": 0.35,
                    "overlay_name": "O1NWTP40"
                }
            ],
            "ips_recs": [ // optional
                { 
                    "x_offset": 0.82,
                    "y_offset": 0.8,
                    "pseg_name": "S1NWTLG0" // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                },
                {
                    "x_offset": 0.305,
                    "y_offset": 6.132,
                    "pseg_name": "S1NWMN01"
                }
            ],
            "bpt_rec" : "ATTNBLK", // optional; ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
            "ptx_recs": [ // optional; see TEXT_ADDITION notes
                { 
                    "x_offset": 0.82,
                    "y_offset": 1.8,
                    "orientation": 0, // degrees; valid values are: 0, 90, 180, 270 (ERROR_OUT if invalid)
                    "font_id": 2,
                    "trn_text": "Department: " // optional; see FROM_METADATA
                },
                {
                    "x_offset": 2.05,
                    "y_offset": 1.8,
                    "orientation": 0,
                    "font_id": 1,
                    "trn_text": "FROM_METADATA^billing_department^"
                }
            ],
            "ept_rec" : "ATTNBLK" // optional; ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
            "epg_rec": "FRNTCOVR", // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
        },
        {
            // This is example of minimum needed for a blank back of a sheet:
            "page_num" : 1,
            "bpg_rec": "BACKCOVR",
            "bag_rec": null,
            "pgd_rec": {
                "width": 8.5,
                "height": 11,
                "dot_per_inches": 300
            },
            "ptd_rec": {
                "width": 8.5,
                "height": 11
            },
            "eag_rec": null,
            "epg_rec": "BACKCOVR"
        }
    ],

    "ADD_TEXT_TO_PAGES" : [  // NOT YET IMPLEMENTED
        {
            "page_num" : 3, // Number of ORIGINAL page to insert a new group of related text onto.
                            // (If adding a new page, the new text should be defined in INSERT_NEW_PAGES, not here.)
            // TODO: my plan was to add new records only to the bottom of pages to not affect prior 
            // records, but we may need to merge fonts into the existing MCF record, after 
            // finding out that the MCF should be within bag/eag.  Not sure if we can create another 
            // bag/eag pair at the bottom of page or not.  Need to test to find out.
            "mcf_recs": [
                // see TEXT_ADDITION notes regarding two font specification methods.
                {
                    "id": 1,
                    "coded_font_name": "X0AMJE04", // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "character_set": null, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "code_page": null, // ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
                    "font_description": "Arial CFL Mono 8pt" // This is just to have a human-readable note of which font this is;
                                                             // used as documentation in this config and in log messages.
                                                             // It is not used in the actual MCF rec.
                },
                {
                    "id": 2,
                    "coded_font_name": null,
                    "character_set": "C0CGI080",
                    "code_page": "T1E4LAT1",
                    "font_description": "Frutiger CFL 11pt"
                }
            ],
            "bpt_rec" : "ATTNBLK", // optional; ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
            "ptx_recs": [ // optional; see TEXT_ADDITION notes
                { 
                    "x_offset": 0.82,
                    "y_offset": 1.8,
                    "orientation": 0, // degrees; valid values are: 0, 90, 180, 270 (ERROR_OUT if invalid)
                    "font_id": 2,
                    "trn_text": "ATTN:" // optional; see FROM_METADATA
                },
                {
                    "x_offset": 0.305,
                    "y_offset": 1.8,
                    "orientation": 0,
                    "font_id": 1,
                    "trn_text": "FROM_METADATA^billing_department^"
                }
            ],
            'ept_rec" : "ATTNBLK" // optional; ERROR_OUT if longer than 8 chars and pad with spaces if less than 8
        },
        {
            "page_num" : 5,
            "mcf_rec": [
                {
                    "id": 1,
                    "coded_font_name": "X0AMJE04",
                    "font_description": "Arial CFL Mono 8pt"
                }
            ],
            "bpt_rec" : "",
            "ptx_recs": [
                { 
                    "x_offset": 2.0,
                    "y_offset": 2.0,
                    "orientation": 0,
                    "font_id": 2,
                    "trn_text": "FOR OFFICIAL USE ONLY!"
                }
            ],
            'ept_rec" : ""
        }
        
    ],

    "CDFS": { // NOT YET IMPLEMENTED
        // Only 4 max available; it is the configuration creator's responsibility to make all 
        // the CDF fields add up to 50 chars when combined with the account number, per the 
        // DOC_ACCOUNT_NUMBER definition.
        "1": {
            "value": "FROM_METADATA^business_unit^",
            "size": 9,
            "justification": "left"
            "padding_char": " "
        },
        "2": {
            "value": "FROM_METADATA^department^",
            "size": 6,
            "justification": "left"
            "padding_char": " "
        },
        "3": {
            "value": "FROM_METADATA^office_number^",
            "size": 3,
            "justification": "right"
            "padding_char": "0"
        },
        "4": null
    }
}
