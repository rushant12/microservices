
package com.broadridge.pdf2print.dataprep.service;

import static com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils.addCmdIPO;
import static com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils.addCmdMCF;
import static com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils.createPTXRecord;
import static com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils.inchToDP;
import static com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils.numToDP;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.broadridge.pdf2print.dataprep.constants.NEW_PAGE_RECS;
import com.broadridge.pdf2print.dataprep.models.BasicDerivedData;
import com.broadridge.pdf2print.dataprep.models.BasicRawData;
import com.broadridge.pdf2print.dataprep.models.FileCoordinate;
import com.broadridge.pdf2print.dataprep.models.FileStats;
import com.broadridge.pdf2print.dataprep.models.MRDFRecord;
import com.broadridge.pdf2print.dataprep.models.RawStmt;
import com.broadridge.pdf2print.dataprep.utils.AnalyzerUtils;
import com.broadridge.pdf2print.dataprep.utils.Constants;
import com.broadridge.pdf2print.dataprep.utils.DataPrepConfigUtils;
import com.broadridge.pdf2print.dataprep.utils.InsertComboUtils;
import com.broadridge.pdf2print.dataprep.utils.MRDFUtils;
import com.broadridge.pdf2print.dataprep.utils.TextServiceUtils;
import com.dst.output.custsys.lib.jolt.Analyzer;
import com.dst.output.custsys.lib.jolt.AnalyzerAdapter;
import com.dst.output.custsys.lib.jolt.DerivedData;
import com.dst.output.custsys.lib.jolt.Messenger;
import com.dst.output.custsys.lib.jolt.MultiFmtr;
import com.dst.output.custsys.lib.jolt.RawData;
import com.dst.output.custsys.lib.jolt.StatementCustomException;
import com.dst.output.custsys.lib.jolt.StatementFormatException;
import com.dst.output.custsys.lib.jolt.StatementNonFatalException;
import com.dst.output.custsys.lib.jolt.StatementSchemaException;
import com.dstoutput.custsys.jafp.AfpByteArrayList;
import com.dstoutput.custsys.jafp.AfpCmd;
import com.dstoutput.custsys.jafp.AfpCmdBAG;
import com.dstoutput.custsys.jafp.AfpCmdBPG;
import com.dstoutput.custsys.jafp.AfpCmdBPT;
import com.dstoutput.custsys.jafp.AfpCmdEAG;
import com.dstoutput.custsys.jafp.AfpCmdEPG;
import com.dstoutput.custsys.jafp.AfpCmdEPT;
import com.dstoutput.custsys.jafp.AfpCmdIMM;
import com.dstoutput.custsys.jafp.AfpCmdIPS;
import com.dstoutput.custsys.jafp.AfpCmdPGD;
import com.dstoutput.custsys.jafp.AfpCmdPTD;
import com.dstoutput.custsys.jafp.AfpCmdRaw;
import com.dstoutput.custsys.jafp.AfpCmdTLE;
import com.dstoutput.custsys.jafp.AfpCmdType;
import com.dstoutput.custsys.jafp.AfpNopGenericZD;
import com.dstoutput.custsys.jafp.AfpRec;
import com.dstoutput.custsys.jafp.AfpSFIntro;
import com.dstoutput.custsys.jafp.SBIN3;
import com.dstoutput.custsys.jafp.UBIN1;
import com.dstoutput.custsys.jafp.UBIN2;
import com.dstoutput.custsys.jafp.UBIN3;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.StringBuffer;
import java.util.Collection;

public class DataprepAnalyzer extends AnalyzerAdapter implements Analyzer {
	MultiFmtr multiFmtr;
	private final OutputService outputService = new OutputService();
	private final AnalyzerUtils analyzerUtils = new AnalyzerUtils();
	private final TextService textService = new TextService();
	// private CustomCheckService customCheckService = new CustomCheckService();
	public static Map<String, String> tleMapPerStatement = null;
	// public static Map<String, String> barCodeMap;
	public static Map<String, String> barCode;
	public static Set<String> landscapePages = new HashSet<>();
	public static Map<Integer, String> customDynamicPerfMap;
	public static long totalSheetCount = 0;
	public static long totalImageCount = 0;
	public static String jobName = "";
	private MRDFRecord mrdfRecord;
	String insertCombo;
	public static Set<String> csvsProcessed = new HashSet<String>();
	private final TextServiceUtils textServiceUtils = new TextServiceUtils();

	public static Map<String, FileStats> reconStats = new HashMap<>();

	private NWCmdLine cmdLine = ReadAFPFile.getCmdLine();
	private AfpRec inputAfpRec;
	// Inits for BNG ENG
	private boolean bngWritten = false;
	private boolean engWritten = false;
	public static List<AfpRec> bngRecs = new ArrayList();
	public static List<AfpRec> engRecs = new ArrayList();
	public static List<Integer> bngPos = new ArrayList();
	public static List<Integer> engPos = new ArrayList();
	List<Integer> engRecIndexList = new ArrayList<>();
	Map<String, AfpRec> afpMap = new HashMap<String, AfpRec>();
	public JSONObject masterDataprepJsonObject;
	private String docAccountNumber = "";
	private List<AfpRec> tleRecords = new ArrayList<>();
	private List<AfpRec> bngRecords = new ArrayList<>();
	private Map<String, String> immSettings = new HashMap<>();
	private List<AfpRec> immRecords = new ArrayList<>();
	private Map<String, String> pSegsSettings = new HashMap<>();
	private List<AfpRec> pSegsRecords = new ArrayList<>();
	Map<String, AfpRec> afpPsegMap = new HashMap<String, AfpRec>();
	public static List<String> errorMessage = new ArrayList<>();
	private Set<String> insertedNops = new HashSet<>();
	private int immCounter = 0;

	double pageDPI = 0.0;
	private final String DOC_SPECIAL_HANDLING_CODE = "78";
	List<FileCoordinate> fileCoordinates = new ArrayList<>();
	private JSONObject firstMailPiece = null;
	private final Set<String> fieldsFromFirstMail = new HashSet<>();
	private boolean isFirstMailProcessed = false;
	private Map<String, String> addressLinesMap = new HashMap<String, String>();
	private static final Pattern SEND_ADDRESS_LINE_PATTERN = Pattern
			.compile("FROM_METADATA\\^send_address_line_(\\d)\\^");
	private static final Map<String, Boolean> fieldFoundTracker = new HashMap<>();
	private static int totalMailPieces = 0;
	private static int processedMailPieces = 0;
	private static final Set<String> unresolvedFields = new HashSet<>();
	private static final Set<String> requiredMetadataFields = new HashSet<>();
	private final List<JSONObject> allMailPiecesSeen = new ArrayList<>();
	
	@Override
	public DerivedData validate(Messenger messenger, RawData rawDataIn) throws StatementSchemaException,
			StatementFormatException, StatementCustomException, StatementNonFatalException, IOException {
		multiFmtr = MultiFmtr.getInstance();
		mrdfRecord = null;
		BasicRawData rawData = (BasicRawData) rawDataIn;
		RawStmt derivedRawStmt = null;
		try {
			derivedRawStmt = modifyAFP(rawData);
			derivedRawStmt.setMrdfRecord(mrdfRecord);
			outputService.updateStatementDetails(derivedRawStmt, rawData.inputFileName);
			outputService.updatePSVData(derivedRawStmt, rawData.inputFileName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return new BasicDerivedData(derivedRawStmt, rawData);
	}

	private RawStmt modifyAFP(BasicRawData rawData) throws IOException, ParseException, JSONException, Exception {
		RawStmt rawStmt = rawData.rawStmt;
		int imageCount = rawData.rawStmt.getPages().size();
		int sheetCount = 0;
		if (rawData.rawStmt.getPages().size() % 2 == 0) {
			sheetCount = rawData.rawStmt.getPages().size() / 2;
		} else {
			sheetCount = rawData.rawStmt.getPages().size() / 2 + 1;
		}

		// SEND_ADDRESS_SCRPAE
		addressLinesMap = analyzerUtils.populateAddressLines(rawData.rawStmt.getMetaData());
		// extract address from position
		List<List<AfpRec>> updatedStmtPages = new ArrayList<>(); // List to hold the updated pages
		List<FileCoordinate> fileCoordinatesList = new ArrayList<>();
		MasterDataPrepConfigFile config = LoadMasterDataPrepConfig.getConfig();
		List<HashMap<String, String>> addressList = new ArrayList<>();
		System.out.println("MAIL PIECE IN RAW DATA:" + rawData.rawStmt.getMetaData());
		List<AfpRec> headers = rawStmt.getHeader();
		List<List<AfpRec>> docAddressAfpRecList = new ArrayList<>();
		List<AfpRec> docAddress = new ArrayList<>();
		String foreignSHCode = DOC_SPECIAL_HANDLING_CODE;

		Map<String, Object> sendAddressMap = config.getSCRAPE_SEND_ADDRESS();
		Map<String, Object> scrapeAndMoveAddressMap = config.getSCRAPE_AND_MOVE_SEND_ADDRESS();
		Map<String, Object> finalSendAddressMap = new HashMap<>();
		HashMap<String, Object> moveAddressMmap = new HashMap<>();
		if (scrapeAndMoveAddressMap != null) {
			Object fromAddress = scrapeAndMoveAddressMap.get("from");
			moveAddressMmap = new Gson().fromJson(fromAddress.toString(), HashMap.class);
		}

		// Load config Hopper Lookup
		Map<String, Integer> hopperLookupConfig = config.getINSERT_ID_HOPPER_LOOKUP();
		if (hopperLookupConfig == null) {
			hopperLookupConfig = new HashMap<>();
		}
		System.out.println("Hopper Lookup ====> " + hopperLookupConfig);

		List<Map<String, Object>> shippingSHCodeLookupConfig = config.getSHIPPING_SH_CODE_LOOKUP();

		List<Map<String, String>> tle = config.getINSERT_MAIL_PIECE_TLES();
		List<Map<String, String>> nop = config.getINSERT_MAIL_PIECE_NAMED_NOPS();
		String isBulkShipping = rawData.rawStmt.getMetaData().getString("is_bulk_shipping");
		JSONObject item = new JSONObject();
		System.out.println("isBulkShipping:" + isBulkShipping);
		List<String> addressObject = new ArrayList<>();
		List<String> addressLines = new ArrayList<>();
		if (isBulkShipping.equalsIgnoreCase("Y")) {
			foreignSHCode = "93";
			if ((scrapeAndMoveAddressMap == null) && (sendAddressMap == null)) {
				addressLines = new ArrayList<>(addressLinesMap.values());
				docAddress = setAddressTLE(rawData, addressLines, addressList, docAddressAfpRecList, foreignSHCode);
			}
		}
		if (null != sendAddressMap) {
			finalSendAddressMap = sendAddressMap;
		} else if (null != moveAddressMmap) {
			finalSendAddressMap = moveAddressMmap;
		}
		if (null != finalSendAddressMap && !finalSendAddressMap.isEmpty()) {
			item = analyzerUtils.convertObjectMapToJson(finalSendAddressMap);

			JSONArray sendAddressArray = new JSONArray();
			List<String> scrapedAdressLines = new ArrayList<>();

			HashMap<String, String> addressMap = new LinkedHashMap<>();

			updatedStmtPages = new ArrayList<>();
			double sx1 = 0.0;
			double sy1 = 0.0;
			;
			double sx2 = 0.0;
			;
			double sy2 = 0.0;
			;
			int iOrientations = 0;
			int pageNumber = 0;

			docAddress = extractAddressPopulateTLE(rawData, fileCoordinatesList, finalSendAddressMap, sendAddressArray,
					scrapedAdressLines, addressList, docAddressAfpRecList, sx1, sy1, sx2, sy2, iOrientations,
					foreignSHCode);

			for (Map<String, String> map : addressList) {
				System.out.println(" List of Addresses:{");
				for (Map.Entry<String, String> entry : map.entrySet()) {
					System.out.println("  " + entry.getKey() + ": " + entry.getValue());
				}
				System.out.println("}");
			}

		}
		headers = rawStmt.getHeader();
		headers.addAll(docAddress);
		rawStmt.setHeader(headers);

		// Add mail piece TLEs and NOP's

		try {

			int bngCounter = 0;

			JSONObject mailPiece = rawData.rawStmt.getMetaData();

			Set<String> insertedTles = new HashSet<>();
			Set<String> insertedNops = new HashSet<>();
			List<AfpRec> hdrList = rawStmt.getHeader();

			for (AfpRec hdr : hdrList) {
				if (hdr.getTla().equals("BNG")) {
					bngCounter++;
					if (mailPiece != null) {
						List<AfpRec> newTleRecords = new ArrayList<>(rawStmt.getHeader());
						newTleRecords = insertMailPieceLevelTles(rawStmt.getHeader(), newTleRecords, mailPiece,
								insertedTles, tle, addressList, shippingSHCodeLookupConfig);

						rawStmt.setHeader(newTleRecords);
					}

					if (mailPiece != null) {
						List<AfpRec> newNopRecords = new ArrayList<>(rawStmt.getHeader());
						newNopRecords = insertMailPieceLevelNamedNops(rawStmt.getHeader(), newNopRecords, mailPiece,
								insertedNops, nop, addressList);

						rawStmt.setHeader(newNopRecords);
					}

					if (mailPiece != null) {
						List<AfpRec> comboTleRecords = new ArrayList<>(rawStmt.getHeader());
						comboTleRecords = insertDocInsertComboTle(mailPiece, rawStmt.getHeader(),
								Collections.singletonList(hopperLookupConfig), insertedTles);
						rawStmt.setHeader(comboTleRecords);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ADD IMM Records

		int bpgCounter = 0;
		Map<String, AfpRec> imms = setImmRecs(rawStmt.getPages(), immSettings, immRecords);
		System.out.println("IMMS map sixe:" + imms.size());
		for (List<AfpRec> pagesL : rawStmt.getPages()) {
			List<AfpRec> updatedPages = new ArrayList<>();

			for (AfpRec page : pagesL) {

				if (page != null && "BPG".equals(page.getTla())) {
					bpgCounter++;

					if (imms != null && imms.containsKey(String.valueOf(bpgCounter))) {
						AfpRec immRecord = imms.get(String.valueOf(bpgCounter));

						if (immRecord != null) {
							updatedPages.add(immRecord);
						} else {
							System.out.println("IMM record for BPG " + bpgCounter + " is null.");
						}
					}
				}

				updatedPages.add(page);
			}

			updatedStmtPages.add(updatedPages);
		}

		rawStmt.setPages(updatedStmtPages);
	
		// Add IPS Record
		int bpgCounterForPSegs = 0;
		Map<String, AfpRec> pSegs = insertPageSegs(rawStmt.getPages(), pSegsSettings, pSegsRecords);
		List<List<AfpRec>> updatedStmtIPS = processPageSegments(rawStmt.getPages(), pSegs, bpgCounterForPSegs);
		rawStmt.setPages(updatedStmtIPS);
		
		// EACH_PDF_DOCUMENT_STARTS_NEW_SHEET
		String eachPdfNewSheet = config.getEACH_PDF_DOCUMENT_STARTS_NEW_SHEET();
		if (null != eachPdfNewSheet && eachPdfNewSheet.equalsIgnoreCase("Y")) {
			HashMap<Integer, Integer> newSheets = getNewSheetPages(rawData);
			Map<String, AfpRec> immAfpMap = addIMMForDocument(rawStmt.getPages(), newSheets);

			List<List<AfpRec>> modifiedStmtPages = setIMMRecToPage(rawStmt.getPages(), immAfpMap);
			rawStmt.setPages(modifiedStmtPages);
		}

		
		
	
		String filename = rawData.inputFileName;
		if (reconStats.containsKey(filename)) {
			FileStats fs = reconStats.get(filename);
			fs.addStatements(1);
			fs.addSheets(sheetCount);
			fs.addImages(imageCount);
		} else {
			FileStats fs = new FileStats(1, sheetCount, imageCount);
			reconStats.put(filename, fs);
		}

		totalSheetCount = totalSheetCount + sheetCount;
		totalImageCount = totalImageCount + imageCount;

		barCode = DataPrepConfigUtils.readBarCodeforInsertAndPerf(rawStmt.getPages());

		if (DataprepLoader.dataPrepConfig != null && DataprepLoader.dataPrepConfig.has("LANDSCAPE")) {
			landscapePages.clear();
			landscapePages = DataPrepConfigUtils.readPagesForLandscape(rawStmt.getPages());
		}

		// Capturing the TLE from AFP
		tleMapPerStatement = analyzerUtils.captureAllTlePerStatement(rawStmt.getHeader());
		mrdfRecord = analyzerUtils.captureNOP(rawStmt.getHeader());

		if (mrdfRecord != null) {
			insertCombo = InsertComboUtils.calculateInsertComboLogic(mrdfRecord);

			// Load Data from NCOA File to Populate TLEs
			String docAccNum = MRDFUtils.getAccountNumber(mrdfRecord);

			String key = docAccNum.substring(0, 8) + ".CSV_" + docAccNum.substring(8, 19);

			// if (DataprepLoader.csvKeyMap.containsKey(key)) {
			// StringBuilder sbLine = new StringBuilder(DataprepLoader.csvKeyMap.get(key));
			// sbLine.append(",").append(docAccNum);
			// sbLine.append(",").append(cmdLine.getCorp());
			// sbLine.append(",").append(cmdLine.getRundate());
			// sbLine.append(",").append(cmdLine.getCycle());
			// DataprepLoader.csvKeyMap.put(key, sbLine.toString());
			// }

			String ncoaAccNum = mrdfRecord.getJobId() + mrdfRecord.getPieceId();

			String customFile = tleMapPerStatement.get("CUSTOM_INPUT_FILE_NAME");

			jobName = customFile.split("-")[0];

			boolean csvCheck = getAddressFromCsv(ncoaAccNum, jobName);

			if (csvCheck) {
				loadNCOAIntoTleMap(tleMapPerStatement, docAccNum, ncoaAccNum);
			}

			loadRetAddrIntoTleMap(tleMapPerStatement);

			if (DataprepLoader.dataPrepConfig != null && DataprepLoader.dataPrepConfig.has("CORPSPLIT")
					&& !mrdfRecord.getImb().isBlank()) {
				tleMapPerStatement.put("imb", mrdfRecord.getImb().substring(0, 20));
				tleMapPerStatement.put("zipCode", mrdfRecord.getImb().substring(20));
			} else {
				tleMapPerStatement.put("imb", "NOCUSTSIMB");
				tleMapPerStatement.put("zipCode", mrdfRecord.getZipCode());
			}

		}
		if (DataprepLoader.dataPrepConfig != null && DataprepLoader.dataPrepConfig.has("INSERT_COMBO_TABLE")) {
			JSONObject insertComboTable = DataprepLoader.dataPrepConfig.getJSONObject("INSERT_COMBO_TABLE");
			if (insertComboTable.has(jobName)) {
				tleMapPerStatement.put("INSERTCOMBO", insertComboTable.getString(jobName));
			}
		} else if (insertCombo != null) {
			tleMapPerStatement.put("INSERTCOMBO", insertCombo);
		}
		tleMapPerStatement.put("DocClassification",
				DataprepLoader.closeOutConfig == null ? "null" : DataprepLoader.closeOutConfig.getString("CDF1"));
		tleMapPerStatement.put("PlanName", "");
		tleMapPerStatement.put("ParticipantName", "");
		tleMapPerStatement.put("LineOfBusiness",
				DataprepLoader.closeOutConfig == null ? "null" : DataprepLoader.closeOutConfig.getString("CDF2"));

		// Adding BR TLE's to modified AFP output
		// analyzerUtils.addingTle(rawStmt, imageCount, sheetCount, tleMapPerStatement);

		// Delete Text
		fileCoordinates.clear();
		List<Map<String, Object>> deleteTextConfig = config.getDELETE_TEXT();
		for (Map<String, Object> deleteEntry : deleteTextConfig) {
			int pagenum = (Integer) deleteEntry.get("page_num");
			double x1 = (Double) deleteEntry.get("x1");
			double y1 = (Double) deleteEntry.get("y1");
			double x2 = (Double) deleteEntry.get("x2");
			double y2 = (Double) deleteEntry.get("y2");
			int iOrientation = (Integer) deleteEntry.get("orientation");
			fileCoordinates.add(new FileCoordinate(x1, y1, x2, y2, iOrientation, pagenum));
		}
		extractedRemoveMethod(rawData);

		// update MICR for checks
		if (DataprepLoader.dataPrepConfig != null && DataprepLoader.dataPrepConfig.has("MICR_UPDATE")) {
			List<FileCoordinate> fileCoordinates = new ArrayList<>();
			JSONArray micrUpdate = DataprepLoader.dataPrepConfig.getJSONArray("MICR_UPDATE");
			for (int i = 0; i < micrUpdate.length(); i++) {
				JSONObject ptx = micrUpdate.getJSONObject(i);
				double x1 = Double.parseDouble(ptx.get("x1").toString());
				double y1 = Double.parseDouble(ptx.get("y1").toString());
				double x2 = Double.parseDouble(ptx.get("x2").toString());
				double y2 = Double.parseDouble(ptx.get("y2").toString());
				int iOrientation = Integer.parseInt(ptx.get("iOrientation").toString());
				fileCoordinates.add(new FileCoordinate(x1, y1, x2, y2, iOrientation));
			}
			// update micr text if it found in the coordinates
			customDynamicPerfMap = new HashMap<>();
			int pageCounter = 0;
			for (List<AfpRec> page : rawData.rawStmt.getPages()) {
				pageCounter++;
				boolean micrFound = false;
				try {
					/*
					 * if(DataprepLoader.dataPrepConfig.has("CUSTOM_CHECK")){
					 * if(customCheckService.checkForMICR(page, fileCoordinates)){ micrFound=true;
					 * if(!(DataprepLoader.dataPrepConfig.has("SKIP_CHECK_IMM") &&
					 * analyzerUtils.getIMMRecordFromInput(page).equals("T1DPRGLR"))) {
					 * customCheckService.updateMICR(page, fileCoordinates); }else {
					 * micrFound=false; } } }else { if(textService.checkForMICR(page,
					 * fileCoordinates)){ micrFound=true;
					 * textService.updateMICR(page,fileCoordinates); } }
					 */
					if (DataprepLoader.dataPrepConfig != null && DataprepLoader.dataPrepConfig.has("CUSTOM_PROFILE")) {
						if (micrFound) {
							customDynamicPerfMap.put(pageCounter, "00");
						} else {
							customDynamicPerfMap.put(pageCounter, "01");
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		// ADD Text
		List<Map<String, Object>> addTextToPagesConfig = config.getADD_TEXT_TO_PAGES();
		if (addTextToPagesConfig != null && !addTextToPagesConfig.isEmpty()) {
			textService.insertTextBlocksIntoPages(rawStmt, addTextToPagesConfig);
		}

		insertNewPages(rawStmt, config);

		// removing the IMM record from input file.
		// if(DataprepLoader.checkIMMConfig!=null &&
		// !DataprepLoader.checkIMMConfig.has(jobName)) {
		// analyzerUtils.removeIMMRecordFromInput(rawData);
		// }
		return rawStmt;
	}

	private boolean getAddressFromCsv(String ncoaAccNum, String jobName) {
		// TODO Auto-generated method stub
		try {
			if (DataprepLoader.csvAddressMap.size() > 0) {
				String csvKey = analyzerUtils.getCsvKey(mrdfRecord, jobName);
				for (Entry<String, HashMap<String, HashMap<String, String>>> entry : DataprepLoader.csvAddressMap
						.entrySet()) {

					if (entry.getKey().contains(csvKey)) {
						HashMap<String, HashMap<String, String>> csvAcctData = new HashMap<String, HashMap<String, String>>();
						csvAcctData = DataprepLoader.csvAddressMap.get(entry.getKey());

						if (csvAcctData.containsKey(ncoaAccNum)) {
							HashMap<String, String> addressData = csvAcctData.get(ncoaAccNum);
							analyzerUtils.updateAddressData(tleMapPerStatement, addressData);
							csvsProcessed.add(entry.getKey());
							return true;
						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private void loadNCOAIntoTleMap(Map<String, String> tleMapPerStatement, String accountNumber, String ncoaAccNum) {
		// TODO Auto-generated method stub
		Map<String, String> ncoaDataMap = new HashMap<String, String>();
		if (NCOAFileService.ncoaMap.size() > 0) {
			if (NCOAFileService.ncoaMap.containsKey(ncoaAccNum)) {
				ncoaDataMap = NCOAFileService.ncoaMap.get(ncoaAccNum);
				ncoaDataMap.forEach((key, value) -> {
					switch (key) {
					case "addr1":
						tleMapPerStatement.put(Constants.REC_ADDRESS2, value);
						break;
					case "addr2":
						tleMapPerStatement.put(Constants.REC_ADDRESS3, value);
						break;
					case "addr3":
						tleMapPerStatement.put(Constants.REC_ADDRESS4, value);
						break;
					case "addr4":
						tleMapPerStatement.put(Constants.REC_ADDRESS5, value);
						break;
					case "addr5":
						tleMapPerStatement.put(Constants.REC_ADDRESS6, value);
						break;
					default:
						tleMapPerStatement.put(Constants.REC_ADDRESS1, mrdfRecord.getSendAddr1());
					}
				});
			}
		} else {

			ArrayList<String> addressLines = new ArrayList<String>();
			if (!mrdfRecord.getSendAddr1().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr1());
			}
			if (!mrdfRecord.getSendAddr2().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr2());
			}
			if (!mrdfRecord.getSendAddr3().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr3());
			}
			if (!mrdfRecord.getSendAddr4().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr4());
			}
			if (!mrdfRecord.getSendAddr5().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr5());
			}
			if (!mrdfRecord.getSendAddr6().isBlank()) {
				addressLines.add(mrdfRecord.getSendAddr6());
			}

			HashMap<String, String> addressData = new HashMap<String, String>();

			addressData = DataPrepConfigUtils.updateAddressesInAddressLines(addressData, addressLines);

			tleMapPerStatement.put(Constants.REC_ADDRESS1, addressData.get("addr1"));
			tleMapPerStatement.put(Constants.REC_ADDRESS2, addressData.get("addr2"));
			tleMapPerStatement.put(Constants.REC_ADDRESS3, addressData.get("addr3"));
			tleMapPerStatement.put(Constants.REC_ADDRESS4, addressData.get("addr4"));
			tleMapPerStatement.put(Constants.REC_ADDRESS5, addressData.get("addr5"));
			tleMapPerStatement.put(Constants.REC_ADDRESS6, addressData.get("addr6"));
		}

		tleMapPerStatement.put("CDF", accountNumber);
		tleMapPerStatement.put("NOCDF", ncoaAccNum.substring(1));
	}

	private void loadRetAddrIntoTleMap(Map<String, String> tleMapPerStatement) throws JSONException {
		List<String> addresses = new ArrayList<>();
		if (!mrdfRecord.getRetAddr1().isBlank()) {
			addresses.add(mrdfRecord.getRetAddr1());
		}
		if (!mrdfRecord.getRetAddr2().isBlank()) {
			addresses.add(mrdfRecord.getRetAddr2());
		}
		if (!mrdfRecord.getRetAddr3().isBlank()) {
			addresses.add(mrdfRecord.getRetAddr3());
		}
		if (!mrdfRecord.getRetAddr4().isBlank()) {
			addresses.add(mrdfRecord.getRetAddr4());
		}

		if (addresses.size() == 4) {
			tleMapPerStatement.put(Constants.RET_ADDRESS4, addresses.get(3));
		}
		if (addresses.size() >= 3) {
			tleMapPerStatement.put(Constants.RET_ADDRESS3, addresses.get(2));
		}
		if (addresses.size() >= 2) {
			tleMapPerStatement.put(Constants.RET_ADDRESS2, addresses.get(1));
		}
		if (addresses.size() >= 1) {
			tleMapPerStatement.put(Constants.RET_ADDRESS1, addresses.get(0));
		}

	}

	@Override
	public Analyzer createNewInstance() throws IOException {
		return null;
	}

	public Map<String, AfpRec> setImmRecs(List<List<AfpRec>> afpRecList, Map<String, String> immSettings,
			List<AfpRec> immRecords) {
		try {
			if (this.cmdLine == null || this.cmdLine.getConfigFile() == null) {
				throw new IllegalArgumentException("Config file path is null.");
			}
			String configFile = this.cmdLine.getConfigFile();
			String jsonString = new String(Files.readAllBytes(Paths.get(configFile)));
			JSONObject jsonObject = new JSONObject(jsonString);

			JSONArray setImmRecsArray = new JSONArray();
			if (jsonObject.has("SET_IMM_RECS")) {
				setImmRecsArray = jsonObject.getJSONArray("SET_IMM_RECS");
			}

			if (immSettings == null) {
				immSettings = new HashMap<>();
			}
			for (int i = 0; i < setImmRecsArray.length(); i++) {
				JSONObject entry = setImmRecsArray.getJSONObject(i);
				Iterator<String> keys = entry.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					String value = entry.optString(key, null);
					if (value != null) {
						immSettings.put(key, value);
					}
				}
			}

			if (afpRecList == null || afpRecList.isEmpty()) {
				return null;
			}

			if (immRecords == null) {
				immRecords = new ArrayList<>();
			}

			int pageNumber = 0;
			for (List<AfpRec> pagesList : afpRecList) {
				for (AfpRec page : pagesList) {
					AfpRec afpRec1 = page;
					if (afpRec1 == null) {
						continue;
					}
					if ("BPG".equals(afpRec1.getTla())) {
						pageNumber++;
						String pageNumberStr = String.valueOf(pageNumber);
						if (immSettings != null && immSettings.containsKey(pageNumberStr)) {
							String immToUse = immSettings.get(pageNumberStr);
							if (immToUse != null && !immToUse.isEmpty()) {
								immCounter++;
								afpMap = setImmForPage(afpRec1, immToUse, pageNumberStr, immRecords);

							}
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return afpMap;
	}

	private Map<String, AfpRec> setImmForPage(AfpRec afpRec, String immName, String pageNumberStr,
			List<AfpRec> immRecords) {
		try {
			if (afpRec != null && immRecords != null) {
				AfpCmdIMM afpCmdImm = new AfpCmdIMM(String.format("%-8s", immName));
				AfpRec updatedRec = afpCmdImm.toAfpRec((short) 0, 0);
				if (updatedRec != null) {
					immRecords.add(updatedRec);
					if (afpMap != null) {
						afpMap.put(pageNumberStr, updatedRec);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return afpMap;
	}

	public List<AfpRec> setSendAddressTles(HashMap<String, String> docAddressRecords) {
		boolean tleaddedflag = false;
		List<AfpRec> addressTleRecords = new ArrayList<>();
		try {

			Gson gson = new Gson();
			String jsonString = gson.toJson(docAddressRecords);
			System.out.println("jsonString:" + jsonString);
			JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
			Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();

			for (Map.Entry<String, JsonElement> entry : entrySet) {
				String key = entry.getKey();
				JsonElement value = entry.getValue();

				String tleValue = value.getAsString();
				if (tleValue != null) {
					String tleKey = key + ":" + tleValue;
					System.out.println("tleKey String:" + tleKey);

					AfpRec newRec;
					newRec = (new AfpCmdTLE(key, tleValue)).toAfpRec((short) 0, 0);
					addressTleRecords.add(newRec);

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Address TLE sixe:" + addressTleRecords.size());
		addressTleRecords.forEach(System.out::println);
		return addressTleRecords;
	}

	private List<AfpRec> extractAddressPopulateTLE(BasicRawData rawData, List<FileCoordinate> fileCoordinatesList,
			Map<String, Object> sendAddressMap, JSONArray sendAddressArray, List<String> scrapedAdressLines,
			List<HashMap<String, String>> addressList, List<List<AfpRec>> docAddressAfpRecList, double sx1, double sy1,
			double sx2, double sy2, int iOrientations, String foreignSHCode) throws JSONException, Exception {
		List<AfpRec> docAddress;
		HashMap<String, String> addressMap;
		Integer orientationFromMap = null;
		
		for (Map.Entry<String, Object> entry : sendAddressMap.entrySet()) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", entry.getKey());
			jsonObject.put("value", entry.getValue());
			System.out.println("key:value" + entry.getKey() + ":" + entry.getValue());
			if (entry.getKey().equals(("x1")))
				sx1 = Double.parseDouble(entry.getValue().toString());
			if (entry.getKey().equals(("y1")))
				sy1 = Double.parseDouble(entry.getValue().toString());
			if (entry.getKey().equals(("x2")))
				sx2 = Double.parseDouble(entry.getValue().toString());
			if (entry.getKey().equals(("y2")))
				sy2 = Double.parseDouble(entry.getValue().toString());
			if (entry.getKey().equals(("orientation")))
				orientationFromMap = Integer.parseInt(entry.getValue().toString());

			sendAddressArray.put(jsonObject);
		}
		
	    if (orientationFromMap != null) {
	        iOrientations = orientationFromMap;
	    }

	    // Orientation value validation before using it
	    List<Integer> validOrientations = Arrays.asList(0, 90, 180, 270);
	    if (!validOrientations.contains(iOrientations)) {
	        System.err.println("Invalid orientation value for Address Scraping in configuration: " + iOrientations
	                + ". Valid values are: 0, 90, 180, 270.");
	        System.exit(1);
	    }
		
		fileCoordinatesList.add(new FileCoordinate(sx1, sy1, sx2, sy2, iOrientations));

		try {
			List<AfpRec> pages = rawData.rawStmt.getPages().get(0);
			scrapedAdressLines = textService.extractTextAt(pages, fileCoordinatesList);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		System.out.println("Address:" + Arrays.asList(scrapedAdressLines));

		if (scrapedAdressLines.isEmpty()) {

			docAccountNumber = rawData.rawStmt.getMetaData().getString("infact_account_number");
			System.out.println("FATAL No address lines scraped for" + docAccountNumber);
			errorMessage.add("FATAL No address lines scraped for" + docAccountNumber);
		}
		docAddress = setAddressTLE(rawData, scrapedAdressLines, addressList, docAddressAfpRecList, foreignSHCode);
		return docAddress;
	}

	private List<AfpRec> setAddressTLE(BasicRawData rawData, List<String> adressLines,
			List<HashMap<String, String>> addressList, List<List<AfpRec>> docAddressAfpRecList, String foreignSHCode)
			throws JSONException {
		List<AfpRec> docAddress;
		HashMap<String, String> addressMap;
		addressMap = analyzerUtils.populateAddressMap(adressLines, rawData.rawStmt.getMetaData(), foreignSHCode);
		addressList.add(addressMap);
		docAddress = setSendAddressTles(addressMap);
		docAddressAfpRecList.add(docAddress);
		return docAddress;
	}

	public Map<String, AfpRec> insertPageSegs(List<List<AfpRec>> afpRecList, Map<String, String> pSegsSettings,
			List<AfpRec> pSegsRecords) {
		try {
			if (this.cmdLine == null || this.cmdLine.getConfigFile() == null) {
				throw new IllegalArgumentException("Config file path is null.");
			}
			String configFile = this.cmdLine.getConfigFile();
			String jsonString = new String(Files.readAllBytes(Paths.get(configFile)));
			JSONObject jsonObject = new JSONObject(jsonString);

			if (jsonObject == null || !jsonObject.has("INSERT_PSEGS")) {
				System.out.println("INSERT_PSEGS not found in the configuration.");
				return null;
			}

			JSONArray insertPsegsArray = jsonObject.getJSONArray("INSERT_PSEGS");

			if (pSegsSettings == null) {
				pSegsSettings = new HashMap<>();
			}

			for (int i = 0; i < insertPsegsArray.length(); i++) {
				JSONObject entry = insertPsegsArray.getJSONObject(i);

				String pageNum = String.valueOf(entry.getInt("page_num"));
				String psegName = entry.getString("pseg_name");
				String xOffset = entry.getString("x_offset");
				String yOffset = entry.getString("y_offset");

				if (psegName != null && !psegName.isEmpty()) {
					pSegsSettings.put(pageNum, psegName);
					pSegsSettings.put(pageNum + "_x_offset", xOffset);
					pSegsSettings.put(pageNum + "_y_offset", yOffset);
				}
			}

			if (afpRecList == null || afpRecList.isEmpty()) {
				System.out.println("No records to process.");
				return null;
			}

			if (pSegsRecords == null) {
				pSegsRecords = new ArrayList<>();
			}

			int pageNumber = 0;
			int pgdCounter = 0;
			for (List<AfpRec> pagesList : afpRecList) {
				for (AfpRec afpRec1 : pagesList) {
					if (afpRec1 == null) {
						continue;
					}
					if ("PGD".equals(afpRec1.getTla())) {
						pgdCounter++;
						String pgdStr = String.valueOf(pgdCounter);
						if (pSegsSettings != null && pSegsSettings.containsKey(pgdStr)) {
							AfpCmdRaw afpCmdRaw = new AfpCmdRaw(afpRec1);
							AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
							if (afpCmdType == AfpCmdType.PGD) {
								PrintStream printStreamOriginal = textServiceUtils.hideSystemout();
								AfpCmdPGD pgd = ((AfpCmdPGD) afpRec1.parse());
								System.setOut(printStreamOriginal);
								pageDPI = textServiceUtils.getDPIFromPGD(pgd);
								System.out.println("Page DPI====>" + pageDPI);
							}
						}
					}

					if (afpRec1 != null && "EAG".equals(afpRec1.getTla())) {
						pageNumber++;
						String pageNumberStr = String.valueOf(pageNumber);

						if (pSegsSettings != null && pSegsSettings.containsKey(pageNumberStr)) {
							String pSegToUse = pSegsSettings.get(pageNumberStr);
							if (pSegToUse != null && !pSegToUse.isEmpty()) {
								System.out.println("Printing Page Number====>" + pageNumber);
								System.out.println("Printing InsertPageArraySize====>" + insertPsegsArray.length());

								// Loop through insertPsegsArray and match the page_num
								boolean configFound = false;
								for (int i = 0; i < insertPsegsArray.length(); i++) {
									JSONObject pSegConfig = insertPsegsArray.getJSONObject(i);
									int configPageNum = pSegConfig.getInt("page_num");

									if (configPageNum == pageNumber) {
										String xOffset = pSegConfig.getString("x_offset");
										String yOffset = pSegConfig.getString("y_offset");

										afpPsegMap = setPSegForPage(afpRec1, pSegToUse, pageNumberStr, pSegsRecords,
												xOffset, yOffset, pageDPI);
										configFound = true;
										break;
									}
								}
								if (!configFound) {
									System.out.println("No segment configuration for page: " + pageNumber);
								}
							}
						}
					}
				}
			}
			return afpPsegMap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Map<String, AfpRec> setPSegForPage(AfpRec afpRec, String pSegName, String pageNumberStr,
			List<AfpRec> pSegRecords, String xOffset, String yOffset, double DPI) {
		try {
			if (afpRec != null && pSegRecords != null) {
				SBIN3 aXpsOset = new SBIN3(analyzerUtils.inchToDP(xOffset, Double.toString(DPI)));
				SBIN3 aYpsOset = new SBIN3(analyzerUtils.inchToDP(yOffset, Double.toString(DPI)));
				AfpRec updatedRec = analyzerUtils.addCmdIPS(pSegName, aXpsOset, aYpsOset);
				if (updatedRec != null) {
					if (!pSegRecords.contains(updatedRec)) {
						pSegRecords.add(updatedRec);
						if (afpPsegMap != null) {
							afpPsegMap.put(pageNumberStr, updatedRec);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return afpPsegMap;
	}

	private List<List<AfpRec>> processPageSegments(List<List<AfpRec>> pages, Map<String, AfpRec> pSegs,
			int bpgCounterForPSegs) {
		List<List<AfpRec>> updatedStmtIPS = new ArrayList<>();

		for (List<AfpRec> pagesList1 : pages) {
			List<AfpRec> updatedPagesIPS = new ArrayList<>();

			for (AfpRec pageIPS : pagesList1) {
				if (pageIPS != null && "EAG".equals(pageIPS.getTla())) {
					bpgCounterForPSegs++;

					if (pSegs != null && pSegs.containsKey(String.valueOf(bpgCounterForPSegs))) {
						AfpRec pSegsRecord = pSegs.get(String.valueOf(bpgCounterForPSegs));
						if (pSegsRecord != null) {
							updatedPagesIPS.add(pageIPS);
							updatedPagesIPS.add(pSegsRecord);
						} else {
							System.out.println("Pseg record for BPG " + bpgCounterForPSegs + " is null.");
						}
					} else {
						updatedPagesIPS.add(pageIPS);
					}
				} else {
					updatedPagesIPS.add(pageIPS);
				}
			}

			updatedStmtIPS.add(updatedPagesIPS);
		}

		return updatedStmtIPS;
	}

	public List<AfpRec> insertMailPieceLevelTles(List<AfpRec> afpRecList, List<AfpRec> tleRecords, JSONObject mailPiece,
	        Set<String> insertedTles, List<Map<String, String>> tle, List<HashMap<String, String>> addressList,
	        List<Map<String, Object>> shippingSHCodeLookupConfig) {

	    try {
	        if (afpRecList == null || tleRecords == null || insertedTles == null || mailPiece == null || tle == null) {
	            return tleRecords;
	        }

	        insertedTles.clear();
	        List<Map<String, String>> tleConfig = getDynamicConfig(mailPiece, tle, "FROM_METADATA:");
	        List<AfpRec> currentMailPieceTleRecords = new ArrayList<>();
	        boolean hasSHCodeTLE = false;

	        for (Map<String, String> tleEntry : tleConfig) {
	            for (Map.Entry<String, String> entry : tleEntry.entrySet()) {
	                String key = entry.getKey();
	                Object rawValue = entry.getValue();

	                String formattedValue;
	                if (rawValue instanceof Number) {
	                    double val = ((Number) rawValue).doubleValue();
	                    formattedValue = (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
	                } else {
	                    formattedValue = String.valueOf(rawValue);
	                }

	                // Replace FROM_DERIVEDDATA^ and FROM_METADATA^ in the value
	                Pattern derivedPattern = Pattern.compile("FROM_DERIVEDDATA\\^([^\\^]+)\\^");
	                Matcher derivedMatcher = derivedPattern.matcher(formattedValue);
	                StringBuffer derivedBuffer = new StringBuffer();
	                while (derivedMatcher.find()) {
	                    String varName = derivedMatcher.group(1);
	                    String replacement = (String) getDynamicCmdLineValue(varName);
	                    if (replacement == null) System.exit(1);
	                    derivedMatcher.appendReplacement(derivedBuffer, Matcher.quoteReplacement(replacement));
	                }
	                derivedMatcher.appendTail(derivedBuffer);
	                formattedValue = derivedBuffer.toString();

	                Pattern metaPattern = Pattern.compile("FROM_METADATA\\^([^\\^]+)\\^");
	                Matcher metaMatcher = metaPattern.matcher(formattedValue);
	                StringBuffer metaBuffer = new StringBuffer();
	                while (metaMatcher.find()) {
	                    String varName = metaMatcher.group(1);
	                    Object val = getValueFromMailPiece(mailPiece, varName);
	                    String resolved;
	                    if (val instanceof Number) {
	                        double d = ((Number) val).doubleValue();
	                        resolved = (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
	                    } else {
	                        resolved = String.valueOf(val);
	                    }
	                    if (resolved == null) resolved = "";
	                    metaMatcher.appendReplacement(metaBuffer, Matcher.quoteReplacement(resolved));
	                }
	                metaMatcher.appendTail(metaBuffer);
	                formattedValue = metaBuffer.toString();

	                String resolvedValue = resolveDynamicPlaceholdersWithSendAddressOverride(formattedValue, mailPiece);
	                String tleKey = key + ":" + resolvedValue;
	                boolean isSHCodeTLE = false;

	                for (HashMap<String, String> scrapeAddressMap : addressList) {
	                    if (key.equals("DOC_SPECIAL_HANDLING_CODE") &&
	                            scrapeAddressMap.containsKey("DOC_SPECIAL_HANDLING_CODE")) {
	                        isSHCodeTLE = true;
	                    }
	                }

	                if (!insertedTles.contains(tleKey) && !isSHCodeTLE) {
	                    AfpRec newRec = new AfpCmdTLE(key, resolvedValue).toAfpRec((short) 0, 0);
	                    currentMailPieceTleRecords.add(newRec);
	                    insertedTles.add(tleKey);
	                }
	            }
	        }

	        String derivedSHCode = lookupSpecialHandlingCode(mailPiece, shippingSHCodeLookupConfig);
	        if (derivedSHCode != null) {
	            String tleKey = "DOC_SPECIAL_HANDLING_CODE:" + derivedSHCode;
	            if (!insertedTles.contains(tleKey)) {
	                AfpRec shRec = new AfpCmdTLE("DOC_SPECIAL_HANDLING_CODE", derivedSHCode).toAfpRec((short) 0, 0);
	                currentMailPieceTleRecords.add(shRec);
	                insertedTles.add(tleKey);
	            }
	        }

	        tleRecords.addAll(currentMailPieceTleRecords);

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	    return tleRecords;
	}
	
	public List<AfpRec> insertMailPieceLevelNamedNops(List<AfpRec> afpRecList, List<AfpRec> nopRecords,
	        JSONObject mailPiece, Set<String> insertedNops, List<Map<String, String>> nop,
	        List<HashMap<String, String>> addressList) {

	    try {
	        if (afpRecList == null || nopRecords == null || insertedNops == null || mailPiece == null || nop == null) {
	            return nopRecords;
	        }

	        insertedNops.clear();
	        List<Map<String, String>> nopConfig = getDynamicConfig(mailPiece, nop, "FROM_METADATA:");
	        List<AfpRec> currentMailPieceNopRecords = new ArrayList<>();

	        for (Map<String, String> nopEntry : nopConfig) {
	            for (Map.Entry<String, String> entry : nopEntry.entrySet()) {
	                String key = entry.getKey();
	                Object rawValue = entry.getValue();

	                String formattedValue;
	                if (rawValue instanceof Number) {
	                    double val = ((Number) rawValue).doubleValue();
	                    formattedValue = (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
	                } else {
	                    formattedValue = String.valueOf(rawValue);
	                }

	                // Replace FROM_DERIVEDDATA^ and FROM_METADATA^
	                Pattern derivedPattern = Pattern.compile("FROM_DERIVEDDATA\\^([^\\^]+)\\^");
	                Matcher derivedMatcher = derivedPattern.matcher(formattedValue);
	                StringBuffer derivedBuffer = new StringBuffer();
	                while (derivedMatcher.find()) {
	                    String varName = derivedMatcher.group(1);
	                    String replacement = (String) getDynamicCmdLineValue(varName);
	                    if (replacement == null) System.exit(1);
	                    derivedMatcher.appendReplacement(derivedBuffer, Matcher.quoteReplacement(replacement));
	                }
	                derivedMatcher.appendTail(derivedBuffer);
	                formattedValue = derivedBuffer.toString();

	                Pattern metaPattern = Pattern.compile("FROM_METADATA\\^([^\\^]+)\\^");
	                Matcher metaMatcher = metaPattern.matcher(formattedValue);
	                StringBuffer metaBuffer = new StringBuffer();
	                while (metaMatcher.find()) {
	                    String varName = metaMatcher.group(1);
	                    Object val = getValueFromMailPiece(mailPiece, varName);
	                    String resolved;
	                    if (val instanceof Number) {
	                        double d = ((Number) val).doubleValue();
	                        resolved = (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
	                    } else {
	                        resolved = String.valueOf(val);
	                    }
	                    if (resolved == null) resolved = "";
	                    metaMatcher.appendReplacement(metaBuffer, Matcher.quoteReplacement(resolved));
	                }
	                metaMatcher.appendTail(metaBuffer);
	                formattedValue = metaBuffer.toString();

	                String resolvedValue = resolveDynamicPlaceholdersWithSendAddressOverride(formattedValue, mailPiece);
	                String nopKey = key + ":" + resolvedValue;

	                for (HashMap<String, String> scrapeAddressMap : addressList) {
	                    if (key.equals("DOC_SPECIAL_HANDLING_CODE")
	                            && scrapeAddressMap.containsKey("DOC_SPECIAL_HANDLING_CODE")) {
	                        for (Map.Entry<String, String> scrapeEntry : scrapeAddressMap.entrySet()) {
	                            String scrapeKey = scrapeEntry.getKey();
	                            String scrapeValue = scrapeEntry.getValue();

	                            if (scrapeKey.equals("DOC_SPECIAL_HANDLING_CODE")) {
	                                AfpRec newRec = new AfpNopGenericZD(scrapeKey, scrapeValue).toAfpRec((short) 0, 0);
	                                currentMailPieceNopRecords.add(newRec);
	                                insertedNops.add(scrapeKey + ":" + scrapeValue);
	                            }
	                        }
	                    }
	                }

	                if (!insertedNops.contains(nopKey)) {
	                    AfpRec newRec = new AfpNopGenericZD(key, resolvedValue).toAfpRec((short) 0, 0);
	                    currentMailPieceNopRecords.add(newRec);
	                    insertedNops.add(nopKey);
	                }
	            }
	        }

	        nopRecords.addAll(currentMailPieceNopRecords);

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	    return nopRecords;
	}

	public List<AfpRec> insertFileLevelNamedNops(
	        List<AfpRec> afpRecList,
	        Set<String> insertedNops,
	        List<Map<String, String>> insertFileLevelNamedNopsL,
	        JSONObject mailPiece) {

	    try {
	        if (this.cmdLine == null) {
	            throw new IllegalStateException("cmdLine is null");
	        }

	        String configFile = this.cmdLine.getConfigFile();
	        if (configFile == null || configFile.isEmpty()) {
	            throw new IllegalArgumentException("Config file path is null or empty");
	        }

	        String jsonString = new String(Files.readAllBytes(Paths.get(configFile)));
	        if (jsonString.isEmpty()) {
	            throw new IllegalArgumentException("JSON string is empty");
	        }

	        JSONObject jsonObject = new JSONObject(jsonString);

	        if (insertFileLevelNamedNopsL == null || insertFileLevelNamedNopsL.isEmpty()) {
	            return afpRecList;
	        }

	        JSONArray sendAddressLines = mailPiece.optJSONArray("send_address_lines");
	        if (sendAddressLines == null) {
	            sendAddressLines = new JSONArray();
	        }

	        for (Map<String, String> map : insertFileLevelNamedNopsL) {
	            JSONObject item = analyzerUtils.convertMapToJson(map);
	            if (item == null) continue;

	            Iterator<String> keys = item.keys();
	            while (keys.hasNext()) {
	                String key = keys.next();
	                Object value = item.opt(key);
	                if (value == null) value = "";

	                boolean addedNopBeforeBrg = false;

	                if (afpRecList != null && !afpRecList.isEmpty()) {
	                    for (int j = 0; j < afpRecList.size(); j++) {
	                        AfpRec afpRec1 = afpRecList.get(j);
	                        if (afpRec1 != null && "BRG".equalsIgnoreCase(afpRec1.getTla().trim())) {
	                            if (!addedNopBeforeBrg) {
	                                String nopValue = "";

	                                if (value instanceof String) {
	                                    String valueStr = (String) value;
	                                    nopValue = valueStr;

	                                    Pattern derivedPattern = Pattern.compile("FROM_DERIVEDDATA\\^([^\\^]+)\\^");
	                                    Matcher derivedMatcher = derivedPattern.matcher(nopValue);
	                                    StringBuffer derivedBuffer = new StringBuffer();
	                                    while (derivedMatcher.find()) {
	                                        String varName = derivedMatcher.group(1);
	                                        String replacement = (String) getDynamicCmdLineValue(varName);
	                                        if (replacement == null) {
	                                            System.exit(1);
	                                        }
	                                        derivedMatcher.appendReplacement(derivedBuffer,
	                                                Matcher.quoteReplacement(replacement));
	                                    }
	                                    derivedMatcher.appendTail(derivedBuffer);
	                                    nopValue = derivedBuffer.toString();

	                                    nopValue = getAddressLineFromMetadata(sendAddressLines, nopValue);

	                                    Pattern metaPattern = Pattern.compile("FROM_METADATA\\^([^\\^]+)\\^");
	                                    Matcher metaMatcher = metaPattern.matcher(nopValue);
	                                    StringBuffer metaBuffer = new StringBuffer();
	                                    while (metaMatcher.find()) {
	                                        String varName = metaMatcher.group(1);
	                                        String replacement = getValueFromMailPiece(mailPiece, varName);
	                                        if (replacement == null) replacement = "";
	                                        metaMatcher.appendReplacement(metaBuffer,
	                                                Matcher.quoteReplacement(replacement));
	                                    }
	                                    metaMatcher.appendTail(metaBuffer);
	                                    nopValue = metaBuffer.toString();
	                                }

	                                String nopKey = key + ":" + nopValue;
	                                if (!insertedNops.contains(nopKey)) {
	                                    AfpRec newRec = new AfpNopGenericZD(key, nopValue).toAfpRec((short) 0, 0);
	                                    afpRecList.add(j, newRec);
	                                    insertedNops.add(nopKey);
	                                }

	                                addedNopBeforeBrg = true;
	                            }
	                        }
	                    }
	                }
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	    return afpRecList;
	}
	
	private String resolveDynamicPlaceholders(String input, JSONObject mailPiece) {
		if (input == null)
			return null;

		// Replace FROM_DERIVEDDATA^...^
		Pattern derivedPattern = Pattern.compile("FROM_DERIVEDDATA\\^([^\\^]+)\\^");
		Matcher derivedMatcher = derivedPattern.matcher(input);
		StringBuffer derivedBuffer = new StringBuffer();
		while (derivedMatcher.find()) {
			String varName = derivedMatcher.group(1);
			String value = (String) getDynamicCmdLineValue(varName);
			if (value == null) {
				System.err.println("Error: Derived data value for " + varName + " not found.");
				System.exit(1);
			}
			derivedMatcher.appendReplacement(derivedBuffer, Matcher.quoteReplacement(value));
		}
		derivedMatcher.appendTail(derivedBuffer);

		// Replace FROM_METADATA^...^
		Pattern metaPattern = Pattern.compile("FROM_METADATA\\^([^\\^]+)\\^");
		Matcher metaMatcher = metaPattern.matcher(derivedBuffer.toString());
		StringBuffer finalBuffer = new StringBuffer();
		while (metaMatcher.find()) {
			String varName = metaMatcher.group(1);
			String value = getValueFromMailPiece(mailPiece, varName);
			if (value == null)
				value = ""; // treat null as empty string
			metaMatcher.appendReplacement(finalBuffer, Matcher.quoteReplacement(value));
		}
		metaMatcher.appendTail(finalBuffer);

		return finalBuffer.toString();
	}

	private List<Map<String, String>> getDynamicConfig(JSONObject mailPiece, List<Map<String, String>> configList,
			String prefix) {
		if (configList == null || mailPiece == null) {
			return new ArrayList<>();
		}

		List<Map<String, String>> dynamicConfig = new ArrayList<>();

		for (Map<String, String> config : configList) {
			Map<String, String> dynamicEntry = new HashMap<>();

			for (Map.Entry<String, String> entry : config.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				if (value != null && value.startsWith(prefix)) {
					String fieldName = value.substring(prefix.length());
					value = getValueFromMailPiece(mailPiece, fieldName);
				}

				dynamicEntry.put(key, value);
			}

			dynamicConfig.add(dynamicEntry);
		}

		return dynamicConfig;
	}

	public String getValueFromMailPiece(JSONObject mailPiece, String fieldName) {
	    try {
	        if (fieldName == null) {
	            return "";
	        }

	        if (mailPiece == null) {
	            throw new IllegalArgumentException("Error: mailPiece is null.");
	        }

	        // Keep track of all mail pieces we've seen
	        allMailPiecesSeen.add(mailPiece);

	        // Handle "send_address_line_[1-6]" fields
	        if (fieldName.matches("send_address_line_[1-6]")) {
	            int index = Integer.parseInt(fieldName.substring("send_address_line_".length())) - 1;
	            if (mailPiece.has("send_address_lines")) {
	                JSONArray addressLines = mailPiece.optJSONArray("send_address_lines");
	                if (addressLines != null && index < addressLines.length()) {
	                    return addressLines.optString(index, "");
	                }
	            }
	            return "";
	        }

	        // Check current mailPiece first
	        if (mailPiece.has(fieldName)) {
	            Object value = mailPiece.get(fieldName);
	            return (value == null || value.equals(JSONObject.NULL)) ? "" : value.toString();
	        }

	        // If not in current, check all previously seen mail pieces
	        for (JSONObject seenMail : allMailPiecesSeen) {
	            if (seenMail != null && seenMail.has(fieldName)) {
	                Object value = seenMail.get(fieldName);
	                return (value == null || value.equals(JSONObject.NULL)) ? "" : value.toString();
	            }
	        }

	        // Field not found anywhere â€” throw exception
	        throw new RuntimeException("Field '" + fieldName + "' not found in any mail piece.");

	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("Error retrieving value for field '" + fieldName + "': " + e.getMessage(), e);
	    }
	}

	public List<AfpRec> insertTrailerNamedNops(
	        List<AfpRec> afpRecList,
	        Set<String> insertedNops,
	        List<Map<String, String>> insertTrailerNamedNopsL,
	        int sheetCount,
	        int pageCount,
	        JSONObject mailPiece,
	        int packages) {

	    try {
	        if (afpRecList == null || insertedNops == null || insertTrailerNamedNopsL == null || insertTrailerNamedNopsL.isEmpty()) {
	            return afpRecList;
	        }

	        JSONArray sendAddressLines = mailPiece.optJSONArray("send_address_lines");
	        if (sendAddressLines == null) {
	            sendAddressLines = new JSONArray();
	        }
	        while (sendAddressLines.length() < 6) {
	            sendAddressLines.put("");
	        }

	        List<AfpRec> nopsToInsert = new ArrayList<>();

	        for (Map<String, String> map : insertTrailerNamedNopsL) {
	            JSONObject item = analyzerUtils.convertMapToJson(map);
	            if (item == null) continue;

	            Iterator<String> keys = item.keys();
	            while (keys.hasNext()) {
	                String key = keys.next();
	                Object rawValue = item.opt(key);
	                if (rawValue == null) rawValue = "";

	                String metadataValue;
	                if (rawValue instanceof Number) {
	                    double val = ((Number) rawValue).doubleValue();
	                    metadataValue = (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
	                } else {
	                    metadataValue = String.valueOf(rawValue);
	                }

	                String nopValue = processNopValue(
	                        metadataValue,
	                        sendAddressLines,
	                        mailPiece,
	                        sheetCount,
	                        pageCount,
	                        packages
	                );

	                if (!insertedNops.contains(key)) {
	                    AfpRec newRec = new AfpNopGenericZD(key, nopValue).toAfpRec((short) 0, 0);
	                    nopsToInsert.add(newRec);
	                    insertedNops.add(key);
	                }
	            }
	        }

	        for (int j = 0; j < afpRecList.size(); j++) {
	            AfpRec rec = afpRecList.get(j);
	            if (rec != null && "EDT".equalsIgnoreCase(rec.getTla().trim())) {
	                for (int k = nopsToInsert.size() - 1; k >= 0; k--) {
	                    afpRecList.add(j + 1, nopsToInsert.get(k));
	                }
	                break;
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        System.exit(1);
	    }

	    return afpRecList;
	}


	private String processNopValue(String nopValue, JSONArray sendAddressLines, JSONObject mailPiece,
		    int sheetCount, int pageCount, int packages) {

		    // Handle FROM_DERIVEDDATA
		    Pattern derivedPattern = Pattern.compile("FROM_DERIVEDDATA\\^([^\\^]+)\\^");
		    Matcher derivedMatcher = derivedPattern.matcher(nopValue);
		    StringBuffer derivedBuffer = new StringBuffer();
		    while (derivedMatcher.find()) {
		        String varName = derivedMatcher.group(1);
		        String replacement = getDynamicDerivedDataValue(varName, sheetCount, pageCount, packages);
		        if (replacement == null) replacement = "";
		        derivedMatcher.appendReplacement(derivedBuffer, Matcher.quoteReplacement(replacement));
		    }
		    derivedMatcher.appendTail(derivedBuffer);
		    nopValue = derivedBuffer.toString();

		    // Handle address line replacements
		    nopValue = getAddressLineFromMetadata(sendAddressLines, nopValue);

		    // Handle FROM_METADATA
		    Pattern metaPattern = Pattern.compile("FROM_METADATA\\^([^\\^]+)\\^");
		    Matcher metaMatcher = metaPattern.matcher(nopValue);
		    StringBuffer metaBuffer = new StringBuffer();
		    while (metaMatcher.find()) {
		        String varName = metaMatcher.group(1);
		        String replacement = getValueFromMailPiece(mailPiece, varName);
		        if (replacement == null) replacement = "";
		        metaMatcher.appendReplacement(metaBuffer, Matcher.quoteReplacement(replacement));
		    }
		    metaMatcher.appendTail(metaBuffer);

		    return metaBuffer.toString();
		}

	private String resolveDynamicVariablesWithContext(String input, JSONObject mailPiece, int sheetCount, int pageCount,
			int packages) {
		if (input == null)
			return "";

		// Regular expression to find placeholders like "FROM_METADATA^var^" or
		// "FROM_DERIVEDDATA^var^"
		Pattern pattern = Pattern.compile("FROM_(DERIVEDDATA|METADATA)\\^([^\\^]+)\\^");
		Matcher matcher = pattern.matcher(input);
		StringBuffer result = new StringBuffer();

		while (matcher.find()) {
			String type = matcher.group(1); // DERIVEDDATA or METADATA
			String varName = matcher.group(2); // The variable name
			String replacement = "";

			// Resolve derived data
			if ("DERIVEDDATA".equals(type)) {
				replacement = getDynamicDerivedDataValue(varName, sheetCount, pageCount, packages);
				if (replacement == null) {
					replacement = ""; // Fallback to empty string if not found
				}
			} else if ("METADATA".equals(type)) {
				// Resolve metadata value
				replacement = getValueFromMailPiece(mailPiece, varName);
				if (replacement == null) {
					replacement = ""; // Fallback to empty string if not found
				}
			}

			// Append the resolved value in place of the placeholder
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}

		// Append the rest of the string that is not part of any placeholder
		matcher.appendTail(result);
		return result.toString(); // Return the final resolved string
	}

	private String resolveDynamicPlaceholdersWithSendAddressOverride(String rawValue, JSONObject mailPiece) {
		Matcher matcher = SEND_ADDRESS_LINE_PATTERN.matcher(rawValue);
		JSONArray addressArray = mailPiece.optJSONArray("send_address_lines");

		while (matcher.find()) {
			int lineIndex = Integer.parseInt(matcher.group(1)) - 1;
			String replacement = "";
			if (addressArray != null && lineIndex >= 0 && lineIndex < addressArray.length()) {
				replacement = addressArray.optString(lineIndex, "");
			}
			rawValue = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
			matcher = SEND_ADDRESS_LINE_PATTERN.matcher(rawValue); // Allow multiple matches
		}

		return resolveDynamicPlaceholders(rawValue, mailPiece);
	}

	private String getDynamicDerivedDataValue(String field, int sheetCount, int pageCount, int packages) {
		switch (field.toLowerCase()) {
		case "filenumpages":
			return String.valueOf(pageCount);
		case "filenumsheets":
			return String.valueOf(sheetCount);
		case "filenumpackages":
			return String.valueOf(packages);
		default:
			return null;
		}
	}

	private List<AfpRec> insertNamedNopRecords(String key, Object value, Set<String> insertedNops,
			List<AfpRec> afpRecList, int index) {
		try {
			String valueStr = String.valueOf(value);
			String nopKey = key + ":" + valueStr; // Consistent with calling method

			if (!insertedNops.contains(nopKey)) {
				AfpRec newRec = new AfpNopGenericZD(key, valueStr).toAfpRec((short) 0, 0);
				afpRecList.add(index, newRec);
				insertedNops.add(nopKey);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return afpRecList;
	}

	private Object getDynamicCmdLineValue(String field) {
		if (this.cmdLine == null) {
			return null;
		}

		switch (field.toLowerCase()) {
		case "rundate":
			return this.cmdLine.getRundate();
		case "corp":
			return this.cmdLine.getCorp() != null ? this.cmdLine.getCorp() : "UNKNOWN";
		case "cycle":
			return this.cmdLine.getCycle() != null ? this.cmdLine.getCycle() : "DEFAULT_CYCLE";
		default:
			return null;
		}
	}

	private Object getDynamicCmdLineValue1(String field, String sheetCount, String pageCount, int packages) {
		if (this.cmdLine == null) {
			return null;
		}

		switch (field.toLowerCase()) {
		case "rundate":
			return this.cmdLine.getRundate();
		case "corp":
			return this.cmdLine.getCorp() != null ? this.cmdLine.getCorp() : "UNKNOWN";
		case "cycle":
			return this.cmdLine.getCycle() != null ? this.cmdLine.getCycle() : "DEFAULT_CYCLE";
		case "filenumpages":
			return pageCount;
		case "filenumsheets":
			return sheetCount;
		case "filenumpackages":
			return packages;
		default:
			return null;
		}
	}

	private int getFileNumSheets(RawStmt rawStmt) {
		return (rawStmt.getPages().size() % 2 == 0) ? (rawStmt.getPages().size() / 2)
				: (rawStmt.getPages().size() / 2 + 1);
	}

	private int getFileNumPages(RawStmt rawStmt) {
		return rawStmt.getPages().size();
	}

	private void extractedRemoveMethod(BasicRawData rawData) {
		List<List<AfpRec>> pagesList = rawData.rawStmt.getPages();
		for (FileCoordinate fileCoordinate : fileCoordinates) {
			try {
				textService.removeTextAt(pagesList, fileCoordinate);
			} catch (Exception e) {
				System.err.println("Error processing page: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void insertNewPages(RawStmt rawStmt, MasterDataPrepConfigFile config) {
		List<Map<String, JsonElement>> elements = config.getINSERT_NEW_PAGES();
		if (elements != null && elements.size() > 0) {
			elements = new ArrayList<>(config.getINSERT_NEW_PAGES());
			Function<Map<String, JsonElement>, List<AfpRec>> createNewPage = mp -> {
				List<AfpRec> records = new ArrayList<>();
				try {
					Consumer<AfpCmd> addToRecords = cmdRec -> {
						try {
							if (cmdRec != null) {
								AfpRec rec = cmdRec.toAfpRec((short) 0, 0);
								records.add(rec);
							}
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					};
					mp.forEach((key, value) -> {
						int pageNum = 0;
						if (key.equalsIgnoreCase("page_num"))
							pageNum = value.getAsInt();
						NEW_PAGE_RECS pageRec = null;
						try {
							pageRec = NEW_PAGE_RECS.valueOf(key.toUpperCase());
						} catch (Exception e) {
 
						}
						AfpCmd afpCMD = null;
						Function<JsonElement, String> defaultValue = jsonElm -> {
							if (jsonElm == null || jsonElm == JsonNull.INSTANCE)
								return "";
							return jsonElm.getAsString();
						};
						if (pageRec != null) {
							switch (pageRec) {
							case BPG_REC:
								afpCMD = new AfpCmdBPG(value.getAsString());
								break;
							case BPT_REC:
								afpCMD = new AfpCmdBPT(defaultValue.apply(value));
								break;
							case EPT_REC:
								afpCMD = new AfpCmdEPT(defaultValue.apply(value));
								break;
							case BAG_REC:
								afpCMD = new AfpCmdBAG(defaultValue.apply(value));
								break;
							case EAG_REC:
								afpCMD = new AfpCmdEAG(defaultValue.apply(value));
								break;
							case EPG_REC:
								afpCMD = new AfpCmdEPG(value.getAsString());
								break;
							case IMM_REC:
								afpCMD = new AfpCmdIMM(value.getAsString());
								break;
							case IPS_RECS:
								JsonArray ipsRecs = value.getAsJsonArray();
								for (int k = 0; k < ipsRecs.size(); k++) {
									JsonObject ipsJSON = ipsRecs.get(k).getAsJsonObject();
									String pseg_name = ipsJSON.get("pseg_name").getAsString();
									double x_offset = ipsJSON.get("x_offset").getAsDouble();
									double y_offset = ipsJSON.get("y_offset").getAsDouble();
									SBIN3 xOffset = new SBIN3(inchToDP(x_offset, 300));
									SBIN3 yOffset = new SBIN3(inchToDP(y_offset, 300));
									AfpCmd cmd = new AfpCmdIPS(pseg_name, xOffset, yOffset);
									addToRecords.accept(cmd);
								}
								break;
							case MCF_RECS:
								JsonArray mcfArray = value.getAsJsonArray();
								mcfArray.forEach(mcf -> {
									JsonObject ob = mcf.getAsJsonObject();
									int id = ob.get("id").getAsInt();
									String coded_font_name = AnalyzerUtils.getJSONStringValue(ob, "coded_font_name");
									String font_description = AnalyzerUtils.getJSONStringValue(ob, "font_description");
									String character_set = AnalyzerUtils.getJSONStringValue(ob, "character_set");
									String code_page = AnalyzerUtils.getJSONStringValue(ob, "code_page");
									addToRecords.accept(
											addCmdMCF(character_set, code_page, coded_font_name, new UBIN1(id)));
								});
								break;
							case PGD_REC:
								JsonObject pgdJSON = value.getAsJsonObject();
								double dot_per_inches = pgdJSON.get("dot_per_inches").getAsDouble();
								double width = pgdJSON.get("width").getAsDouble();
								double height = pgdJSON.get("height").getAsDouble();
								UBIN1 xpgbase = new UBIN1(0);
								UBIN1 ypgbase = new UBIN1(0);
								UBIN2 xpgunits = new UBIN2(3000);
								UBIN2 ypgunits = new UBIN2(3000);
								UBIN3 xpgsize = new UBIN3(inchToDP(width, dot_per_inches));
								UBIN3 ypgsize = new UBIN3(inchToDP(height, dot_per_inches));
								AfpByteArrayList bodyBytesList = new AfpByteArrayList();
								bodyBytesList.add(xpgbase.toBytes());
								bodyBytesList.add(ypgbase.toBytes());
								bodyBytesList.add(xpgunits.toBytes());
								bodyBytesList.add(ypgunits.toBytes());
								bodyBytesList.add(xpgsize.toBytes());
								bodyBytesList.add(ypgsize.toBytes());
								bodyBytesList.add(xpgbase.toBytes());
								bodyBytesList.add(xpgbase.toBytes());
								bodyBytesList.add(xpgbase.toBytes());
								byte[] bodyBytes = bodyBytesList.toBytes();
								AfpSFIntro sfIntro = new AfpSFIntro(24, 13870767, (short) 0, 0);
								AfpRec pgdRawRec = new AfpRec((byte) 90, sfIntro, bodyBytes);
								AfpCmdRaw pgdCmdRaw = new AfpCmdRaw(pgdRawRec);
								try {
									afpCMD = new AfpCmdPGD(pgdCmdRaw);
								} catch (ParseException e1) {
									e1.printStackTrace();
								}
								break;
							case PTD_REC:
								JsonObject ptdJSON = value.getAsJsonObject();
								UBIN1 xpbase = new UBIN1(0);
								UBIN1 ypbase = new UBIN1(0);
								UBIN2 xpunitvl = new UBIN2(3000);
								UBIN2 ypunitvl = new UBIN2(3000);
								UBIN3 xpextent = new UBIN3(inchToDP(ptdJSON.get("width").getAsDouble(), 300));
								UBIN3 ypextent = new UBIN3(inchToDP(ptdJSON.get("height").getAsDouble(), 300));
								UBIN2 textflags = new UBIN2(0);
								bodyBytesList = new AfpByteArrayList();
								bodyBytesList.add(xpbase.toBytes());
								bodyBytesList.add(ypbase.toBytes());
								bodyBytesList.add(xpunitvl.toBytes());
								bodyBytesList.add(ypunitvl.toBytes());
								bodyBytesList.add(xpextent.toBytes());
								bodyBytesList.add(ypextent.toBytes());
								bodyBytesList.add(textflags.toBytes());
								bodyBytesList.add("".getBytes());
								bodyBytes = bodyBytesList.toBytes();
								sfIntro = new AfpSFIntro(9, 13873563, (short) 0, 0);
								AfpRec ptdRawRec = new AfpRec((byte) 90, sfIntro, bodyBytes);
								AfpCmdRaw ptdCmdRaw = new AfpCmdRaw(ptdRawRec);
								try {
									afpCMD = new AfpCmdPTD(ptdCmdRaw);
								} catch (ParseException e1) {
									e1.printStackTrace();
								}
								break;
							case IPO_RECS:
								JsonArray ipoArray = value.getAsJsonArray();
								ipoArray.forEach(ipoJSON -> {
									String overlay_name = ipoJSON.getAsJsonObject().get("overlay_name").getAsString();
									double x_offset = ipoJSON.getAsJsonObject().get("x_offset").getAsDouble();
									double y_offset = ipoJSON.getAsJsonObject().get("y_offset").getAsDouble();
									try {
										addToRecords.accept(addCmdIPO(overlay_name, new SBIN3(inchToDP(x_offset, 300)),
												new SBIN3(inchToDP(y_offset, 300))));
									} catch (Exception e1) {
										e1.printStackTrace();
									}
								});
								break;
							case PTX_RECS:
								JsonArray ptxArray = value.getAsJsonArray();
								ptxArray.forEach(ptxJSON -> {
									short x_offset = numToDP(ptxJSON.getAsJsonObject().get("x_offset").getAsDouble());
									short y_offset = numToDP(ptxJSON.getAsJsonObject().get("y_offset").getAsDouble());
									short font_id = ptxJSON.getAsJsonObject().get("font_id").getAsShort();
									String trn_text = ptxJSON.getAsJsonObject().get("trn_text").getAsString();
									try {
										JSONArray sendAddressLines = rawStmt.getMetaData()
												.optJSONArray("send_address_lines");
										if (trn_text.contains("send_address_line"))
											trn_text = getAddressLineFromMetadata(sendAddressLines, trn_text);
										else if (trn_text.contains("FROM_METADATA")) {
											Function<String, String> extractMetadataKey = input -> {
												String extractedWord = "";
												String start = "FROM_METADATA^";
												String end = "^";
												int startIndex = input.indexOf(start);
												int endIndex = input.indexOf(end, startIndex + start.length());
												if (startIndex != -1 && endIndex != -1)
													extractedWord = input.substring(startIndex,
															endIndex + end.length());
												return extractedWord;
											};
											String extractedKey = extractMetadataKey.apply(trn_text);
											String metadata_key = extractedKey.split("\\^")[1];
											Object rawValue = rawStmt.getMetaData().opt(metadata_key);
											String metadata_value;

											if (rawValue == null || JSONObject.NULL.equals(rawValue)) {
											    metadata_value = "";
											} else if (rawValue instanceof Number) {
											    double val = ((Number) rawValue).doubleValue();
											    if (val == Math.floor(val)) {
											        metadata_value = String.valueOf((long) val);
											    } else {
											        metadata_value = String.valueOf(val);
											    }
											} else {
											    metadata_value = String.valueOf(rawValue);
											}

											trn_text = trn_text.replace(extractedKey, metadata_value);
										}
									} catch (Exception e1) {
										e1.printStackTrace();
									}
									addToRecords.accept(createPTXRecord(x_offset, y_offset, trn_text, font_id));
								});
								break;
							default:
								break;
							}
							addToRecords.accept(afpCMD);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				return records;
			};
			Collections.reverse(elements);
			elements.forEach(mp -> {
				int pageNum = mp.get("page_num").getAsInt();
				List<AfpRec> recs = createNewPage.apply(mp);
				List<List<AfpRec>> pages = rawStmt.getPages();
				pages.add(pageNum - 1, recs);
			});
		}
	}
	
	public List<AfpRec> insertDocInsertComboTle(JSONObject mailPiece, List<AfpRec> tleRecords,
			List<Map<String, Integer>> hopperLookup, Set<String> insertedTles) {
		try {
			JSONArray insertIdsJson = mailPiece.optJSONArray("insert_ids");
			System.out.println("Insert IDs from metadata: " + insertIdsJson);

			if (insertIdsJson == null || insertIdsJson.length() == 0) {
				return tleRecords;
			}

			List<String> insertIds = new ArrayList<>();
			for (int i = 0; i < insertIdsJson.length(); i++) {
				insertIds.add(insertIdsJson.getString(i));
			}

			String combo = generateDocInsertCombo(insertIds, hopperLookup);
			String tleKey = "DOC_INSERT_COMBO:" + combo;

			if (!insertedTles.contains(tleKey)) {
				AfpRec tleRec = new AfpCmdTLE("DOC_INSERT_COMBO", combo).toAfpRec((short) 0, 0);
				tleRecords.add(tleRec);
				insertedTles.add(tleKey);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error inserting DOC_INSERT_COMBO TLE.");
		}

		return tleRecords;
	}

	public static String generateDocInsertCombo(List<String> insertIds, List<Map<String, Integer>> hopperLookupList) {
		Map<String, Integer> lookupMap = new HashMap<>();
		System.out.println("hopperLookupList => " + hopperLookupList);
		for (Map<String, Integer> map : hopperLookupList) {
			lookupMap.putAll(map);
		}

		char[] combo = "NNNNNNNNNNN".toCharArray(); // 11 positions

		for (String insertId : insertIds) {
			Integer pos = lookupMap.get(insertId);
			if (pos != null && pos >= 1 && pos <= 11) {
				combo[pos - 1] = 'Y';
			}
		}
		String str = new String(combo);
		return new String(combo);
	}

	private List<List<AfpRec>> setIMMRecToPage(List<List<AfpRec>> updatedStmtPages, Map<String, AfpRec> immAfpMap)
			throws ParseException {
		int bpgCounter;
		bpgCounter = 0;
		List<List<AfpRec>> newStmtPages = new ArrayList<>(); // List to hold the updated pages
		List<List<AfpRec>> modifiedStmtPages = new ArrayList<>();
		newStmtPages = updatedStmtPages;
		System.out.println("IMMS map sixe:" + immAfpMap.size());
		Collection<String> immpgSheet = immAfpMap.keySet();
		System.out.println("immpgSheet: " + immpgSheet);
		for (List<AfpRec> pagesList : newStmtPages) {
			List<AfpRec> updatedPages = new ArrayList<>();
			String prevRec = "";
			for (AfpRec page : pagesList) {
				// System.out.println(""+page.getTla());
				if (page != null && "BPG".equals(page.getTla())) {
					bpgCounter++;

					if (immAfpMap != null && immAfpMap.containsKey(String.valueOf(bpgCounter))) {
						AfpRec immRecord = immAfpMap.get(String.valueOf(bpgCounter));

						if (immRecord != null && !prevRec.equalsIgnoreCase("IMM")) {
							if (bpgCounter == 1) {
								System.out.println("IMM for Page 1 is not set by default ");
								System.exit(1);// Ideally the first sheet IMM should be explicitly defined
							}
							updatedPages.add(immRecord);
						}
					}
				}
				if (page != null && "IMM".equals(page.getTLEName())) {
					prevRec = page.getTLEName();

				}

				updatedPages.add(page);
			}

			modifiedStmtPages.add(updatedPages);
		}
		return modifiedStmtPages;
	}

	private Map<String, AfpRec> addIMMForDocument(List<List<AfpRec>> newPages, HashMap<Integer, Integer> newSheets)
			throws UnsupportedEncodingException, ParseException {
		Map<String, AfpRec> immAfpMap = new HashMap<String, AfpRec>();
		Collection<Integer> pgSheet = newSheets.values();
		System.out.println("pgSheet: " + pgSheet);

		int startPgIndex = 0;
		String currentIMMM_MMPName = "";
		for (List<AfpRec> pages : newPages) {
			startPgIndex++;
			List<AfpRec> updatedPages = new ArrayList<>();

			if (pgSheet.contains(startPgIndex)) {
				System.out.println("This is start of PDF needs new sheet" + startPgIndex);
				// Add new IMM here
				AfpCmdIMM afpCmdImm = new AfpCmdIMM(String.format("%-8s", currentIMMM_MMPName));
				AfpRec updatedRec = afpCmdImm.toAfpRec((short) 0, 0);
				if (updatedRec != null) {
					if (immAfpMap != null) {
						immAfpMap.put(Integer.toString(startPgIndex), updatedRec);
					}
				}
			}

			for (AfpRec page : pages) {

				if (page != null && "IMM".equals(page.getTLEName())) {

					Pattern pattern = Pattern.compile("MMPName\\s*:\\s*\"(.*?)\"");
					Matcher matcher = pattern.matcher(page.toEnglish(null));
					if (matcher.find()) {
						String mmpValue = matcher.group(1);
						System.out.println("Extracted MMPName: " + mmpValue);
						currentIMMM_MMPName = mmpValue;
					} else {
						System.out.println("MMPName not found!");
					}
				}
			}
		}
		return immAfpMap;
	}

	private HashMap<Integer, Integer> getNewSheetPages(BasicRawData rawData) throws IOException {
		int totalPageCount = 0;
		int nextStartPage = 1;
		int newPageIndex = 0;
		HashMap<Integer, Integer> newSheetPages = new HashMap<>();
		try {
			JSONArray pdfDocuments = rawData.rawStmt.getMetaData().getJSONArray("pdf_documents");
			System.out.println("Total PDF documents" + pdfDocuments.length());

			for (int j = 0; j < pdfDocuments.length(); j++) {
				newPageIndex++;
				JSONObject pdfDocument = pdfDocuments.getJSONObject(j);
				System.out.println("New Sheet:" + newPageIndex + ", on Page" + nextStartPage);
				int pageCount = pdfDocument.getInt("page_count");
				totalPageCount += pageCount;

				newSheetPages.put(newPageIndex, nextStartPage);
				nextStartPage = pageCount + 1;
			}

		} catch (JSONException e) {
			System.err.println("Error processing JSON: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		return newSheetPages;
	}

	public String lookupSpecialHandlingCode(JSONObject mailPiece, List<Map<String, Object>> config) {
		if (config == null || config.isEmpty()) {
			return null;
		}

		String carrier = Optional.ofNullable(mailPiece.optString("shipping_carrier")).orElse("").trim();
		String service = Optional.ofNullable(mailPiece.optString("shipping_service")).orElse("").trim();

		System.out.println("Looking up SH code for carrier: '" + carrier + "', service: '" + service + "'");

		for (Map<String, Object> entry : config) {
			String configCarrier = Optional.ofNullable((String) entry.get("carrier")).orElse("").trim();
			String configService = Optional.ofNullable((String) entry.get("service")).orElse("").trim();

			if (wildcardMatch(carrier, configCarrier) && wildcardMatch(service, configService)) {
				System.out.println("Matched config: " + entry);
				return String.valueOf(entry.get("sh_code"));
			}
		}

		System.out.println("No match found in config.");
		return null;
	}

	// simple wildcard match helper
	private boolean wildcardMatch(String value, String pattern) {
		if ("*".equals(pattern)) {
			return true;
		}
		return value.equalsIgnoreCase(pattern);
	}
	
	private String getAddressLineFromMetadata(JSONArray sendAddressLines, String nopValue) {
		
		if (sendAddressLines == null) {
		    System.err.println("Missing 'send_address_lines' in metadata while processing value: " + nopValue);
		    System.exit(1);
		}
		
	    Pattern pattern = Pattern.compile("FROM_METADATA\\^send_address_line_(\\d+)\\^");  // changed from (\\d)
	    Matcher matcher = pattern.matcher(nopValue);
	    StringBuffer result = new StringBuffer();

	    while (matcher.find()) {
	        int lineIndex = Integer.parseInt(matcher.group(1)) - 1;
	        String replacement = "";

	        if (lineIndex >= 0 && lineIndex < sendAddressLines.length()) {
	            replacement = sendAddressLines.optString(lineIndex, "");
	        }

	        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
	    }

	    matcher.appendTail(result);
	    return result.toString();
	}

}
