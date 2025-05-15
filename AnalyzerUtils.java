package com.broadridge.pdf2print.dataprep.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.broadridge.pdf2print.dataprep.models.BasicRawData;
import com.broadridge.pdf2print.dataprep.models.MRDFRecord;
import com.broadridge.pdf2print.dataprep.models.RawStmt;
import com.broadridge.pdf2print.dataprep.service.DataprepLoader;
import com.dstoutput.custsys.jafp.AfpByteArrayList;
import com.dstoutput.custsys.jafp.AfpByteString;
import com.dstoutput.custsys.jafp.AfpCmdIMM;
import com.dstoutput.custsys.jafp.AfpCmdIPO;
import com.dstoutput.custsys.jafp.AfpCmdIPS;
import com.dstoutput.custsys.jafp.AfpCmdMCF;
import com.dstoutput.custsys.jafp.AfpCmdNOP;
import com.dstoutput.custsys.jafp.AfpCmdPTX;
import com.dstoutput.custsys.jafp.AfpCmdRaw;
import com.dstoutput.custsys.jafp.AfpCmdTLE;
import com.dstoutput.custsys.jafp.AfpCmdType;
import com.dstoutput.custsys.jafp.AfpColorCMYK;
import com.dstoutput.custsys.jafp.AfpNop;
import com.dstoutput.custsys.jafp.AfpPtxCharSeq;
import com.dstoutput.custsys.jafp.AfpPtxCtrlAMB;
import com.dstoutput.custsys.jafp.AfpPtxCtrlAMI;
import com.dstoutput.custsys.jafp.AfpPtxCtrlSCFL;
import com.dstoutput.custsys.jafp.AfpPtxCtrlSEC;
import com.dstoutput.custsys.jafp.AfpPtxCtrlSTO;
import com.dstoutput.custsys.jafp.AfpPtxCtrlSeq;
import com.dstoutput.custsys.jafp.AfpPtxCtrlTRN;
import com.dstoutput.custsys.jafp.AfpPtxDataItem;
import com.dstoutput.custsys.jafp.AfpRec;
import com.dstoutput.custsys.jafp.AfpSFIntro;
import com.dstoutput.custsys.jafp.AfpTriplet02;
import com.dstoutput.custsys.jafp.AfpTriplet24;
import com.dstoutput.custsys.jafp.AfpTriplet25;
import com.dstoutput.custsys.jafp.AfpTriplet26;
import com.dstoutput.custsys.jafp.SBIN3;
import com.dstoutput.custsys.jafp.UBIN1;
import com.dstoutput.custsys.jafp.UBIN2;
import com.google.gson.JsonObject;

public class AnalyzerUtils {

	private MRDFRecord mrdfRecord = null;

	private String mrdfFileName;

	public void addingTle(RawStmt rawStmt, int imageCount, int sheetCount, Map<String, String> tleMapPerStatement)
			throws JSONException {
		Map<String, Object> tleMap = new HashMap<>();
		try {
			tleMap = DataPrepConfigUtils.parseTleMap();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		tleMap.forEach((key, value) -> {
			try {
				addingCustomTle(rawStmt, tleMapPerStatement, key, value.toString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		});
		try {

			addZipCodeTle(rawStmt, tleMapPerStatement);

			// addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
			// tleMapPerStatement.getOrDefault(Constants.ROUTING_CODE, ""));

			/*
			 * if(!(DataprepLoader.dataPrepConfig.has("SKIP_DOC_CUST_SEND_IMB") &&
			 * DataprepLoader.dataPrepConfig.getString("SKIP_DOC_CUST_SEND_IMB").equals(
			 * "TRUE"))) { String routingCode =
			 * tleMapPerStatement.getOrDefault(Constants.TRACKING_CODE, ""); String stmtIMB
			 * = routingCode.trim().equals("") ? "NOCUSTSIMB" : routingCode;
			 * addingBRTle(rawStmt.getHeader(), Constants.CUSTOM_CUSTSIMB, stmtIMB); }
			 */

			addingBRTle(rawStmt.getHeader(), Constants.CUSTOM_CUSTSIMB, tleMapPerStatement.get("imb"));
			addingBRTle(rawStmt.getHeader(), Constants.PRODUCT_CODE, DataprepLoader.staticRmConfig == null ? ""
					: DataprepLoader.staticRmConfig.getString("DOC_PRODUCT_CODE"));
			addingBRTle(rawStmt.getHeader(), Constants.IMAGE_COUNT, String.valueOf(imageCount));
			addingBRTle(rawStmt.getHeader(), Constants.SHEET_COUNT, String.valueOf(sheetCount));
			addingBRTle(rawStmt.getHeader(), Constants.DOC_PRODUCT_CODE, DataprepLoader.staticRmConfig == null ? ""
					: DataprepLoader.staticRmConfig.getString("PS_DOC_PRODUCT_CODE"));
			addingBRTle(rawStmt.getHeader(), Constants.DP_PRINT_RELEASE_STATUS_CD,
					DataprepLoader.staticRmConfig == null ? ""
							: DataprepLoader.staticRmConfig.getString("DP_PRINT_RELEASE_STATUS_CD"));
			addingBRTle(rawStmt.getHeader(), Constants.UC_DOCUMENT_TYPE, DataprepLoader.dataPrepConfig == null ? ""
					: DataprepLoader.dataPrepConfig.getString("UC_DOCUMENT_TYPE"));
			addingBRTle(rawStmt.getHeader(), Constants.UR_CLIENT_ID, DataprepLoader.dataPrepConfig == null ? ""
					: DataprepLoader.dataPrepConfig.getString("UR_CLIENT_ID"));
			addingBRTle(rawStmt.getHeader(), Constants.UR_PRODUCT_ID, DataprepLoader.dataPrepConfig == null ? ""
					: DataprepLoader.dataPrepConfig.getString("UR_PRODUCT_ID"));

			if (DataprepLoader.dataPrepConfig != null
					&& DataprepLoader.dataPrepConfig.has("DOC_SPECIAL_HANDLING_CODE_IMB")
					&& tleMapPerStatement.containsKey("imb") && tleMapPerStatement.get("imb").equals("NOCUSTSIMB")) {
				addingBRTle(rawStmt.getHeader(), Constants.SPECIAL_HANDLING_CODE,
						DataprepLoader.dataPrepConfig.getString("DOC_SPECIAL_HANDLING_CODE_IMB"));
			} else if (DataprepLoader.dataPrepConfig != null
					&& DataprepLoader.dataPrepConfig.has("DOC_SPECIAL_HANDLING_CODE")) {
				addingBRTle(rawStmt.getHeader(), Constants.SPECIAL_HANDLING_CODE,
						DataprepLoader.dataPrepConfig.getString("DOC_SPECIAL_HANDLING_CODE"));
			} else {
				addingBRTle(rawStmt.getHeader(), Constants.SPECIAL_HANDLING_CODE, "99");
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void addingCustomTle(RawStmt rawStmt, Map<String, String> tleMapPerStatement, String brCustomTle,
			String afpTleValue) throws UnsupportedEncodingException {
		if (tleMapPerStatement.containsKey(afpTleValue)) {
			/*
			 * if(afpTleValue.equals("UAK1")){ String[] cdfArr =
			 * tleMapPerStatement.get("CUSTOM_CDF").split(","); AfpRec newRec; AfpCmdTLE
			 * tleRec = new AfpCmdTLE(brCustomTle,
			 * String.format("%-31s",tleMapPerStatement.get("UAK")) +
			 * String.format("%-8s",cdfArr[2]) + String.format("%-1s",cdfArr[3]) +
			 * String.format("%-2s",cdfArr[4]) + String.format("%-8s","")); newRec =
			 * tleRec.toAfpRec((short) 0, 0); rawStmt.getHeader().add(newRec); } else {
			 */
			AfpRec newRec;
			AfpCmdTLE tleRec = new AfpCmdTLE(brCustomTle, tleMapPerStatement.get(afpTleValue));
			newRec = tleRec.toAfpRec((short) 0, 0);
			rawStmt.getHeader().add(newRec);
			// }

		}
	}

	public void addingBRTle(List<AfpRec> rawStmtList, String brTle, String brTleValue)
			throws UnsupportedEncodingException {
		AfpRec newRec;
		AfpCmdTLE tleRec = new AfpCmdTLE(brTle, brTleValue);
		newRec = tleRec.toAfpRec((short) 0, 0);
		rawStmtList.add(newRec);
	}

	public AfpRec getTleRec(String brTle, String brTleValue) throws UnsupportedEncodingException {
		AfpRec newRec;
		AfpCmdTLE tleRec = new AfpCmdTLE(brTle, brTleValue);
		return tleRec.toAfpRec((short) 0, 0);
	}

	public String getCsvKey(MRDFRecord mrdfRecord, String jobName) {
		String csvKey = "";
		switch (DataprepLoader.csvType) {
		case "MRDF":
			csvKey = mrdfRecord.getMrdfFileName().substring(16).replace("TXT", "CSV");
			break;
		case "OTP":
			csvKey = "OTP";
			break;
		case "FEDEX":
			csvKey = "CLAIMS_" + jobName + "_FEDEX";
			break;
		}
		return csvKey;
	}

	public void updateAddressData(Map<String, String> tleMapPerStatement, HashMap<String, String> addressData) {

		tleMapPerStatement.put(Constants.REC_ADDRESS1, addressData.get("addr1"));
		tleMapPerStatement.put(Constants.REC_ADDRESS2, addressData.get("addr2"));
		tleMapPerStatement.put(Constants.REC_ADDRESS3, addressData.get("addr3"));
		tleMapPerStatement.put(Constants.REC_ADDRESS4, addressData.get("addr4"));
		tleMapPerStatement.put(Constants.REC_ADDRESS5, addressData.get("addr5"));
		tleMapPerStatement.put(Constants.REC_ADDRESS6, addressData.get("addr6"));
	}

	private void addZipCodeTle(RawStmt rawStmt, Map<String, String> tleMapPerStatement)
			throws UnsupportedEncodingException {

		if (tleMapPerStatement.containsKey("zipCode") && !tleMapPerStatement.get("zipCode").trim().isEmpty()) {
			try {
				String zipcode = "";
				if (tleMapPerStatement.get("zipCode").trim().length() > 5) {
					zipcode = tleMapPerStatement.get("zipCode").substring(0, 5) + "-"
							+ tleMapPerStatement.get("zipCode").substring(5);
				} else {
					zipcode = tleMapPerStatement.get("zipCode").substring(0, 5);
				}

				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE, zipcode);

			} catch (Exception e) {

				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE, "");

			}
		} else {

			if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS6)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS6).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS6)));
			} else if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS5)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS5).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS5)));
			} else if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS4)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS4).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS4)));
			} else if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS3)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS3).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS3)));
			} else if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS2)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS2).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS2)));
			} else if (tleMapPerStatement.containsKey(Constants.REC_ADDRESS1)
					&& !tleMapPerStatement.get(Constants.REC_ADDRESS1).trim().isEmpty()) {
				addingBRTle(rawStmt.getHeader(), Constants.ZIP_CODE,
						extractZipCode(tleMapPerStatement.get(Constants.REC_ADDRESS1)));
			}
		}

	}

	public Map<String, String> captureAllTlePerStatement(List<AfpRec> afpRecList)
			throws ParseException, UnsupportedEncodingException {
		Map<String, String> tleMapPerStatement = new HashMap<>();
		for (AfpRec rec : afpRecList) {
			AfpCmdRaw afpCmdRaw = new AfpCmdRaw(rec);
			AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
			if (afpCmdType == AfpCmdType.TLE) {
				AfpCmdTLE afpCmdTLE = new AfpCmdTLE(afpCmdRaw);
				if (afpCmdTLE.getAttributeNameString().trim().equals("MAILDATA")) {
					tleMapPerStatement.put(afpCmdTLE.getAttributeNameString().trim(),
							afpCmdTLE.getAttributeValueString());
				} else {
					tleMapPerStatement.put(afpCmdTLE.getAttributeNameString().trim(),
							afpCmdTLE.getAttributeValueString().trim());
				}
			}
		}
		return tleMapPerStatement;
	}

	public MRDFRecord captureNOP(List<AfpRec> afpRecList)
			throws ParseException, UnsupportedEncodingException, JSONException {
		Map<String, String> tleMapPerStatement = new HashMap<>();
		String key = "";
		String value = "";
		for (AfpRec rec : afpRecList) {
			AfpCmdRaw afpCmdRaw = new AfpCmdRaw(rec);
			AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
			// System.out.println("AFPcmdTime-->"+afpCmdType.getTla()+"\n");
			if (afpCmdType.getTla().equals("NOP")) {
				/*
				 * AfpCmdTLE afpCmdTLE = new AfpCmdTLE(afpCmdRaw);
				 * if(afpCmdTLE.getAttributeNameString().trim().equals("CUSTOM_MRDF_LINE")){
				 * System.out.println(afpCmdTLE.getAttributeValueString());
				 * //tleMapPerStatement.put(afpCmdTLE.getAttributeNameString().trim(),
				 * afpCmdTLE.getAttributeValueString()); }
				 */
				/*
				 * else { tleMapPerStatement.put(afpCmdTLE.getAttributeNameString().trim(),
				 * afpCmdTLE.getAttributeValueString().trim()); }
				 */
				// System.out.println("Kishore\n");
				;
				AfpCmdNOP afpCmdNop = ((AfpCmdNOP) rec.parse());
				AfpNop nop = AfpNop.parse(afpCmdNop.getBodyBytes());
				key = nop.getNopValue();
				value = afpCmdNop.getResources().asString();

				if (key.contains("CUSTOM_MRDF_NAME")) {
					mrdfFileName = key;

//                    System.out.println(value);
				} else if (key.contains("CUSTOM_MRDF_LINE")) {
					// System.out.println(value);
					// System.out.println(value.substring(17));
					if (key.length() >= 17) {
						mrdfRecord = MRDFUtils.getMRDFRecFromLine(key.substring(17));
						mrdfRecord.setMrdfFileName(mrdfFileName);

						System.out.println("Processing Statement " + MRDFUtils.getAccountNumber(mrdfRecord));
					}
				}

			}
		}
		return mrdfRecord;
	}

	public void removeIMMRecordFromInput(BasicRawData rawData) {
		for (List<AfpRec> pageRec : rawData.rawStmt.getPages()) {
			for (int i = 0; i < pageRec.size(); i++) {
				AfpCmdRaw afpCmdRaw = new AfpCmdRaw(pageRec.get(i));
				AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
				if (afpCmdType == AfpCmdType.IMM) {
					pageRec.remove(pageRec.get(i));
				}
			}
		}
	}

	public String getIMMRecordFromInput(List<AfpRec> pageRec) throws ParseException, UnsupportedEncodingException {
		for (AfpRec element : pageRec) {
			AfpCmdRaw afpCmdRaw = new AfpCmdRaw(element);
			AfpCmdType afpCmdType = afpCmdRaw.getCmdType();
			if (afpCmdType == AfpCmdType.IMM) {
				AfpCmdIMM immRec = ((AfpCmdIMM) element.parse());
				System.out.println(immRec.getResources().asString());
				return immRec.getResources().asString();
			}
		}
		return "NOIMMFOUND";
	}

	private String extractZipCode(String recAddress) {
		String[] matches = Pattern.compile("[0-9]{5}(?:-[0-9]{4})?").matcher(recAddress).results()
				.map(MatchResult::group).toArray(String[]::new);

		if (Pattern.compile("[0-9]{5}(?:-[0-9]{4})?").matcher(recAddress).find()) {
			return matches[0].toString();
		} else {
			return "";
		}
	}

	public JSONObject convertMapToJson(Map<String, String> map) {
		return new JSONObject(map);
	}

	public JSONObject convertObjectMapToJson(Map<String, Object> map) {
		return new JSONObject(map);
	}

	public boolean objectContainsString(Object obj, String target) {
		if (obj instanceof String) {
			String str = (String) obj;
			return str.contains(target);
		} else {
			return false; // Or handle other cases as needed
		}
	}

	public String toJsonArray(Object[] objects) throws IllegalAccessException {
		if (objects == null) {
			return "null";
		}

		StringBuilder jsonBuilder = new StringBuilder("[");
		for (int i = 0; i < objects.length; i++) {
			jsonBuilder.append(toJsonString(objects[i]));
			if (i < objects.length - 1) {
				jsonBuilder.append(",");
			}
		}
		jsonBuilder.append("]");
		return jsonBuilder.toString();
	}

	public String toJsonString(Object obj) throws IllegalAccessException {
		if (obj == null) {
			return "null";
		}

		Class<?> clazz = obj.getClass();
		Field[] fields = clazz.getDeclaredFields();
		StringBuilder jsonBuilder = new StringBuilder("{");

		for (int i = 0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			String name = fields[i].getName();
			Object value = fields[i].get(obj);

			jsonBuilder.append("\"").append(name).append("\":");
			if (value instanceof String) {
				jsonBuilder.append("\"").append(value).append("\"");
			} else if (value instanceof Number || value instanceof Boolean) {
				jsonBuilder.append(value);
			} else if (value instanceof Collection) {
				jsonBuilder.append(toJsonArray(((Collection<?>) value).toArray()));
			} else if (value.getClass().isArray()) {
				jsonBuilder.append(toJsonArray((Object[]) value));
			} else if (value != null && !value.getClass().isPrimitive()) {
				jsonBuilder.append(toJsonString(value));
			} else {
				jsonBuilder.append("null");
			}

			if (i < fields.length - 1) {
				jsonBuilder.append(",");
			}
		}
		jsonBuilder.append("}");
		return jsonBuilder.toString();
	}

	public JSONArray convertHashMapToJsonArray(HashMap<String, String> map) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", entry.getKey());
			jsonObject.put("value", entry.getValue());
			jsonArray.put(jsonObject);
		}
		return jsonArray;
	}

	public String getAddressZipCode(List<String> textArray) throws JSONException {
		String zipCode = "";
		String zipCode5S = "";
		String zipCodePlus4S = "";
		String zipCode9s = "";
		boolean isValidAddress = false;
		String sendAddressZipCode = "";

		System.out.println("Address Array:" + Arrays.asList(textArray)); /// 51654 True 714 False
		if (textArray.size() > 0) {
			for (String addrLine : textArray) {
				System.out.println("addrLine: " + addrLine);
			}
			String lastLine = textArray.get(textArray.size() - 1);
			System.out.println("Text Array size: " + textArray.size());
			System.out.println("lastLine: " + lastLine);
			ArrayList<String> wordArray = new ArrayList<>(Arrays.asList(lastLine.split(" ")));
			zipCode = wordArray.get(wordArray.size() - 1);
			System.out.println("zipCode: " + zipCode);

			if (zipCode.length() == 5) {
				zipCode5S = zipCode;
				sendAddressZipCode = zipCode5S;
			} else if (zipCode.length() == 10) // 5 + '-' + 4
			{
				zipCodePlus4S = zipCode;
				sendAddressZipCode = zipCodePlus4S;
			} else if (zipCode.length() == 9) {
				zipCode9s = zipCode;
				sendAddressZipCode = zipCode9s;
			}

			if (!zipCode5S.equals("")) {
				Pattern zipPattern = Pattern.compile("\\d{5}");
				if (zipPattern.matcher(zipCode5S).find()) {
					isValidAddress = true;
				}
			} else if (!zipCodePlus4S.equals("")) {

				Pattern zipPlus4Pattern = Pattern.compile("\\d{5}-\\d{4}");
				if (zipPlus4Pattern.matcher(zipCodePlus4S).find()) {
					isValidAddress = true;
				}
			} else if (!zipCode9s.equals("")) {
				Pattern zip9Pattern = Pattern.compile("\\d{9}");
				if (zip9Pattern.matcher(zipCode9s).find()) {
					isValidAddress = true;
				}
			} else {
				isValidAddress = false;
			}

			System.out.println("isValidAddress: " + isValidAddress);
		} else {
			System.out.println("isValidAddress: false");
			isValidAddress = false;
		}
		System.out.println("sendAddressZipCode in validate:" + sendAddressZipCode);

		return sendAddressZipCode;
	}

	public HashMap<String, String> populateAddressMap(List<String> textArray, JSONObject metaData, String foreignSHCode)
			throws JSONException {
		// heck if the address is validate
		String zipCodeScraped = getAddressZipCode(textArray);

		int itemIdx = 0;
		String tempCommentText = "";
		String tempCommentTextValue = "";
		HashMap<String, String> addressNOPMap = new LinkedHashMap();

		for (int i = 0; i < 6; i++) {
			int addrLine = i + 1;
			if (itemIdx < textArray.size()) {
				tempCommentText = "DOC_SEND_ADDR_LINE" + (addrLine);
				tempCommentTextValue = textArray.get(i);
				System.out.println("" + tempCommentText + "-" + tempCommentTextValue);
				addressNOPMap.put(tempCommentText, tempCommentTextValue);
				itemIdx++;
			} else {
				tempCommentText = "DOC_SEND_ADDR_LINE" + addrLine;
				tempCommentTextValue = "";
				System.out.println("" + tempCommentText + "-" + tempCommentTextValue);
				addressNOPMap.put(tempCommentText, tempCommentTextValue);

			}
		}
		System.out.println("MetaData" + metaData.toString());
		String isForeignAddress = metaData.getString("is_foreign_address");
		String isbulk = metaData.getString("is_bulk_shipping");
		System.out.println("isForeignAddress " + isForeignAddress);
		System.out.println("Foreigh SH Code " + foreignSHCode);
		if (isForeignAddress.equals("N")) {
			
			tempCommentText = "DOC_SEND_ADDRESS_ZIP_CODE";
			if(isbulk.equals("Y")){
				String zip = metaData.getString("zip_code"); 
				zipCodeScraped = zip;
			}
			tempCommentTextValue = zipCodeScraped;
			System.out.println("" + tempCommentText + "-" + tempCommentTextValue);
			addressNOPMap.put(tempCommentText, tempCommentTextValue);
		} else if ( isForeignAddress.equals("Y")) {
			tempCommentText = "DOC_SEND_ADDRESS_ZIP_CODE";
			tempCommentTextValue = "NSF";
			System.out.println("" + tempCommentText + "-" + tempCommentTextValue);
			addressNOPMap.put(tempCommentText, tempCommentTextValue);

			tempCommentText = "DOC_SPECIAL_HANDLING_CODE";
			tempCommentTextValue = foreignSHCode;
			System.out.println("" + tempCommentText + "-" + tempCommentTextValue);
			addressNOPMap.put(tempCommentText, tempCommentTextValue);
		}

		JSONArray addressJsonArray = convertHashMapToJsonArray(addressNOPMap);

		System.out.println("populateAddressMap:" + addressJsonArray.toString());

		return addressNOPMap;

	}

	public AfpRec addCmdIPS(String aPsegName, SBIN3 aXpsOset, SBIN3 aYpsOset) throws Exception {
		AfpCmdIPS newCmdIPS = new AfpCmdIPS(aPsegName, aXpsOset, aYpsOset);
		return newCmdIPS.toAfpRec((short) 0, 0);
	}

	public int inchToDP(String inch, String dotPerInch) {
		double dp = Double.parseDouble(inch) * Double.parseDouble(dotPerInch);
		return new BigDecimal(dp).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	public static AfpCmdPTX createPTXRecord(short x_pos, short y_pos, String data, Short mcfCode) {
		try {
			ArrayList<AfpPtxDataItem> newPtxDataItems = new ArrayList();
			AfpPtxCtrlSeq newCtrlseq = new AfpPtxCtrlSeq();
			createCtrlSeqPrefix(newPtxDataItems);
			addPtxCtrlSEC(newCtrlseq);
			createPtxCtrlSTO(newCtrlseq);
			createPtxCtrlScfl(newCtrlseq, mcfCode);
			AfpPtxCtrlAMI secondAMI = new AfpPtxCtrlAMI(x_pos, true);
			newCtrlseq.addCtrl(secondAMI);
			AfpPtxCtrlAMB secondAMB = new AfpPtxCtrlAMB(y_pos, true);
			newCtrlseq.addCtrl(secondAMB);
			newCtrlseq.addCtrl(new AfpPtxCtrlTRN(data, true));
			newPtxDataItems.add(newCtrlseq);
			return new AfpCmdPTX(newPtxDataItems);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static AfpCmdMCF addCmdMCF(String characterSet, String codePage, String codedFont, UBIN1 resID) {
		try {
			if (StringUtils.isBlank(characterSet))
				characterSet = "        ";
			if (StringUtils.isBlank(codePage))
				codePage = "        ";
			if (StringUtils.isBlank(codedFont))
				codedFont = "        ";
			
			UBIN2 rglength = new UBIN2(49);

			UBIN1 characterSetFQNType = new UBIN1(134);
			UBIN1 codePageFQNType = new UBIN1(133);
			UBIN1 codedFontFQNType = new UBIN1(142);

			AfpByteString characterSetFQName = new AfpByteString(characterSet);
			AfpByteString codePageFQName = new AfpByteString(codePage);
			AfpByteString codedFontFQName = new AfpByteString(codedFont);

			AfpTriplet02 characterSetTriplet = new AfpTriplet02(characterSetFQNType, characterSetFQName);
			AfpTriplet02 codePageTriplet = new AfpTriplet02(codePageFQNType, codePageFQName);
			AfpTriplet02 codedFontTriplet = new AfpTriplet02(codedFontFQNType, codedFontFQName);

			UBIN1 restype = new UBIN1(5);
			AfpTriplet24 afptriplet24 = new AfpTriplet24(restype, resID);
			UBIN1 ResSNum = new UBIN1(0);
			AfpTriplet25 afptriplet25 = new AfpTriplet25(ResSNum);
			UBIN2 CharRot = new UBIN2(0);
			AfpTriplet26 afptriplet26 = new AfpTriplet26(CharRot);
			AfpByteArrayList bodyBytesList = new AfpByteArrayList();
			bodyBytesList.add(rglength.toBytes());
			bodyBytesList.add(characterSetTriplet.toBytes());
			bodyBytesList.add(codePageTriplet.toBytes());
			bodyBytesList.add(codedFontTriplet.toBytes());
			bodyBytesList.add(afptriplet24.toBytes());
			bodyBytesList.add(afptriplet25.toBytes());
			bodyBytesList.add(afptriplet26.toBytes());
			byte[] bodyBytes = bodyBytesList.toBytes();
			AfpSFIntro sfIntro = new AfpSFIntro(157, 13872010, (short) 0, 0);
			AfpRec mcfCmdRaw = new AfpRec((byte) 90, sfIntro, bodyBytes);
			AfpCmdRaw mcfCmdRawRec = new AfpCmdRaw(mcfCmdRaw);
			AfpCmdMCF mcfCMD = new AfpCmdMCF(mcfCmdRawRec);
			return mcfCMD;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static AfpCmdIPO addCmdIPO(String aPsegName, SBIN3 aXpsOset, SBIN3 aYpsOset) throws Exception {
		AfpCmdIPO newCmdIPO = new AfpCmdIPO(aPsegName, aXpsOset, aYpsOset);
		AfpSFIntro sfIntro = new AfpSFIntro(24, 13873112, (short) 0, 0);
		AfpRec iporawRec = new AfpRec((byte) 90, sfIntro, newCmdIPO.getBodyBytes());
		AfpCmdRaw ipoCmdRaw = new AfpCmdRaw(iporawRec);
		AfpCmdIPO ipoRec = new AfpCmdIPO(ipoCmdRaw);
		return ipoRec;
	}

	public static void createCtrlSeqPrefix(ArrayList<AfpPtxDataItem> newPtxDataItems) {
		Integer seqPrefix = new Integer(43);
		Integer seqClass = new Integer(211);
		byte[] startBytes = new byte[] { seqPrefix.byteValue(), seqClass.byteValue() };
		ByteBuffer escapeChars = ByteBuffer.wrap(startBytes);
		AfpPtxCharSeq escapeCharsSeq = new AfpPtxCharSeq(escapeChars);
		newPtxDataItems.add(escapeCharsSeq);
	}

	public static void addPtxCtrlSEC(AfpPtxCtrlSeq newCtrlseq) throws Exception {
		UBIN1 cyanBits = new UBIN1(8);
		UBIN1 magentaBits = new UBIN1(8);
		UBIN1 yellowBits = new UBIN1(8);
		UBIN1 blackBits = new UBIN1(8);
		UBIN1 cyanBits1 = new UBIN1(0);
		UBIN1 magentaBits1 = new UBIN1(0);
		UBIN1 yellowBits1 = new UBIN1(0);
		UBIN1 blackBits1 = new UBIN1(255);
		byte[] cyan = cyanBits1.toBytes();
		byte[] magenta = magentaBits1.toBytes();
		byte[] yellow = yellowBits1.toBytes();
		byte[] black = blackBits1.toBytes();
		AfpColorCMYK colorBase = new AfpColorCMYK(cyanBits, magentaBits, yellowBits, blackBits, cyan, magenta, yellow,
				black);
		AfpPtxCtrlSEC setExtTextColor = new AfpPtxCtrlSEC(colorBase, true);
		newCtrlseq.addCtrl(setExtTextColor);
	}

	public static void createPtxCtrlSTO(AfpPtxCtrlSeq newCtrlseq) {
		UBIN2 iorntion = new UBIN2(0);
		UBIN2 borntion = new UBIN2(11520);
		AfpPtxCtrlSTO orination = new AfpPtxCtrlSTO(iorntion, borntion, true);
		newCtrlseq.addCtrl(orination);
	}

	public static void createPtxCtrlScfl(AfpPtxCtrlSeq newCtrlseq, int lastFontCodeId) {
		Integer escape = new Integer(lastFontCodeId);
		byte[] endBytes = new byte[] { escape.byteValue() };
		ByteBuffer ending = ByteBuffer.wrap(endBytes);
		AfpPtxCtrlSCFL setCodedFontLocal = new AfpPtxCtrlSCFL(ending, true);
		newCtrlseq.addCtrl(setCodedFontLocal);
	}

	public static int inchToDP(double inch, double dotPerInch) {
		double dpNumber = inch * dotPerInch;
		BigDecimal bd = new BigDecimal(Double.toString(dpNumber));
		bd = bd.setScale(0, RoundingMode.HALF_UP);
		return bd.intValue();
	}

	public static short numToDP(double coordinate) {
		double coordinateValue = coordinate * Double.valueOf("300");
		return (short) ((int) coordinateValue);
	}

	public static String getJSONStringValue(JsonObject ob, String key) {
		String value = "";
		try {
			value = ob.get(key).getAsString();
		} catch (Exception e) {
			System.out.println("key : " + key + " is not present");
		}
		return value;
	}

	public static String getJSONStringValue(JSONObject ob, String key) {
		String value = "";
		try {
			value = ob.getString(key);
		} catch (Exception e) {
			System.out.println("key : " + key + " is not present");
		}
		return value;
	}
	
	public Map<String, String> populateAddressLines(JSONObject mailpiece) {
	    Map<String, String> addressMap = new LinkedHashMap<>();

	    JSONArray addressLines = mailpiece.optJSONArray("send_address_lines");

	    for (int i = 0; i < 6; i++) {
	        String key = "addressline" + (i + 1);
	        String value = "";

	        if (addressLines != null && i < addressLines.length()) {
	            value = addressLines.optString(i, "").trim();
	        }

	        addressMap.put(key, value);
	    }

	    return addressMap;
	}
}
