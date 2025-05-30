package com.broadridge.pdf2print.dataprep.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LoadMasterDataPrepConfig {

	private static MasterDataPrepConfigFile masterDataPrepConfigFile = null;

	// This method will load the config once and then use it in subsequent calls
	public static void loadMasterDataprepConfig() throws JSONException, IOException {
		if (masterDataPrepConfigFile == null) {
			// Get config file path from system property
			String configFile = ReadAFPFile.getCmdLine().getConfigFile(); // Assuming cmdLine is initialized correctly
			System.out.println("configFile:" + configFile);

			// Read the config file and convert it into a JSON object
			String jsonString = new String(Files.readAllBytes(Paths.get(configFile)));

			JSONObject masterDataprepJsonObject = new JSONObject(jsonString);

			masterDataPrepConfigFile = new MasterDataPrepConfigFile();
			masterDataPrepConfigFile.setINSERT_BNG_ENG_RECS(masterDataprepJsonObject.optString("INSERT_BNG_ENG_RECS"));

			// Setting TRANSLATE_IMM_RECS (Array of objects)
			JSONArray translateImmRecs = masterDataprepJsonObject.optJSONArray("TRANSLATE_IMM_RECS");
			List<Map<String, String>> translateList = new ArrayList<>();
			if (translateImmRecs != null) {
				for (int i = 0; i < translateImmRecs.length(); i++) {
					JSONObject entry = translateImmRecs.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> translateMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							translateMap.put(key, value);
						}
						translateList.add(translateMap);
					}
				}
			}
			masterDataPrepConfigFile.setTRANSLATE_IMM_RECS(translateList);

			// Setting SET_IMM_RECS (Array of objects)
			JSONArray setImmRecs = masterDataprepJsonObject.optJSONArray("SET_IMM_RECS");
			List<Map<String, String>> setList = new ArrayList<>();
			if (setImmRecs != null) {
				for (int i = 0; i < setImmRecs.length(); i++) {
					JSONObject entry = setImmRecs.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> immMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							immMap.put(key, value);
						}
						setList.add(immMap);
					}
				}
			}
			masterDataPrepConfigFile.setSET_IMM_RECS(setList);

			// Setting DELETE_TEXT (Array of objects with numeric and floating point fields)
			JSONArray deleteText = masterDataprepJsonObject.optJSONArray("DELETE_TEXT");
			List<Map<String, Object>> deleteList = new ArrayList<>();
			if (deleteText != null) {
				for (int i = 0; i < deleteText.length(); i++) {
					JSONObject obj = deleteText.optJSONObject(i);
					if (obj != null) {
						Map<String, Object> map = new HashMap<>();
						map.put("page_num", obj.optInt("page_num"));
						map.put("x1", obj.optDouble("x1"));
						map.put("y1", obj.optDouble("y1"));
						map.put("x2", obj.optDouble("x2"));
						map.put("y2", obj.optDouble("y2"));
						map.put("orientation", obj.optInt("orientation"));
						deleteList.add(map);
					}
				}
			}
			masterDataPrepConfigFile.setDELETE_TEXT(deleteList);

			// Setting INSERT_FILE_LEVEL_NOPS (Array of strings)
			JSONArray insertFileLevelNops = masterDataprepJsonObject.optJSONArray("INSERT_FILE_LEVEL_NOPS");
			List<String> nopsList = new ArrayList<>();
			if (insertFileLevelNops != null) {
				for (int i = 0; i < insertFileLevelNops.length(); i++) {
					String value = insertFileLevelNops.optString(i);
					nopsList.add(value);
				}
			}
			masterDataPrepConfigFile.setINSERT_FILE_LEVEL_NOPS(nopsList);

			JSONArray insertFileHeaderNamedNops = masterDataprepJsonObject
					.optJSONArray("INSERT_FILE_HEADER_NAMED_NOPS");
			List<Map<String, String>> nopsHeaderList = new ArrayList<>();
			if (insertFileHeaderNamedNops != null) {
				for (int i = 0; i < insertFileHeaderNamedNops.length(); i++) {
					JSONObject entry = insertFileHeaderNamedNops.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> namedNopMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							namedNopMap.put(key, value);
						}
						nopsHeaderList.add(namedNopMap);
					}
				}
			}
			masterDataPrepConfigFile.setINSERT_FILE_HEADER_NAMED_NOPS(nopsHeaderList);

			JSONArray insertFileTrailerNamedNops = masterDataprepJsonObject
					.optJSONArray("INSERT_FILE_TRAILER_NAMED_NOPS");
			List<Map<String, String>> nopsTrailerList = new ArrayList<>();
			if (insertFileTrailerNamedNops != null) {
				for (int i = 0; i < insertFileTrailerNamedNops.length(); i++) {
					JSONObject entry = insertFileTrailerNamedNops.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> namedNopMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							namedNopMap.put(key, value);
						}
						nopsTrailerList.add(namedNopMap);
					}
				}
			}
			masterDataPrepConfigFile.setINSERT_FILE_TRAILER_NAMED_NOPS(nopsTrailerList);

			JSONArray insertMailpieceTles = masterDataprepJsonObject.optJSONArray("INSERT_MAIL_PIECE_TLES");
			List<Map<String, String>> mailPieceTleList = new ArrayList<>();
			if (insertMailpieceTles != null) {
				for (int i = 0; i < insertMailpieceTles.length(); i++) {
					JSONObject entry = insertMailpieceTles.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> namedNopMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							namedNopMap.put(key, value);
						}
						mailPieceTleList.add(namedNopMap);
					}
				}
			}
			masterDataPrepConfigFile.setINSERT_MAIL_PIECE_TLES(mailPieceTleList);

			JSONArray insertMailpieceNops = masterDataprepJsonObject.optJSONArray("INSERT_MAIL_PIECE_NAMED_NOPS");
			List<Map<String, String>> mailPieceNopList = new ArrayList<>();
			if (insertMailpieceNops != null) {
				for (int i = 0; i < insertMailpieceNops.length(); i++) {
					JSONObject entry = insertMailpieceNops.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> namedNopMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							namedNopMap.put(key, value);
						}
						mailPieceNopList.add(namedNopMap);
					}
				}
			}
			masterDataPrepConfigFile.setINSERT_MAIL_PIECE_NAMED_NOPS(mailPieceNopList);

			// Setting INSERT_FILE_LEVEL_NAMED_NOPS (Array of objects)
			JSONArray insertFileLevelNamedNops = masterDataprepJsonObject.optJSONArray("INSERT_FILE_LEVEL_NAMED_NOPS");
			List<Map<String, String>> namedNopsList = new ArrayList<>();
			if (insertFileLevelNamedNops != null) {
				for (int i = 0; i < insertFileLevelNamedNops.length(); i++) {
					JSONObject entry = insertFileLevelNamedNops.optJSONObject(i);
					if (entry != null) {
						Iterator<String> keys = entry.keys();
						Map<String, String> nopMap = new HashMap<>();
						while (keys.hasNext()) {
							String key = keys.next();
							String value = entry.optString(key);
							nopMap.put(key, value);
						}
						namedNopsList.add(nopMap);
					}
				}
			}
			masterDataPrepConfigFile.setINSERT_FILE_LEVEL_NAMED_NOPS(namedNopsList);

			// Setting SCRAPE_SEND_ADDRESS (Object with numeric and string fields)
			JSONObject scrapeSendAddress = masterDataprepJsonObject.optJSONObject("SCRAPE_SEND_ADDRESS");
			if (scrapeSendAddress != null) {
				Map<String, Object> scrapeSendMap = new HashMap<>();
				scrapeSendMap.put("x1", scrapeSendAddress.optDouble("x1"));
				scrapeSendMap.put("y1", scrapeSendAddress.optDouble("y1"));
				scrapeSendMap.put("x2", scrapeSendAddress.optDouble("x2"));
				scrapeSendMap.put("y2", scrapeSendAddress.optDouble("y2"));
				scrapeSendMap.put("orientation", scrapeSendAddress.optInt("orientation"));
				scrapeSendMap.put("foreign_sh_code", scrapeSendAddress.optString("foreign_sh_code"));
				masterDataPrepConfigFile.setSCRAPE_SEND_ADDRESS(scrapeSendMap);
			}

			// Setting SCRAPE_AND_MOVE_SEND_ADDRESS (Object with numeric and string fields)
			JSONObject scrapeAndMoveSendAddress = masterDataprepJsonObject
					.optJSONObject("SCRAPE_AND_MOVE_SEND_ADDRESS");
			if (scrapeAndMoveSendAddress != null) {
				Map<String, Object> scrapeAndMoveSendMap = new HashMap<>();

				JSONObject from = scrapeAndMoveSendAddress.optJSONObject("from");
				if (from != null) {
					Map<String, Object> fromMap = new HashMap<>();
					fromMap.put("x1", from.optDouble("x1"));
					fromMap.put("y1", from.optDouble("y1"));
					fromMap.put("x2", from.optDouble("x2"));
					fromMap.put("y2", from.optDouble("y2"));
					fromMap.put("orientation", from.optInt("orientation"));
					scrapeAndMoveSendMap.put("from", fromMap);
				}

				JSONObject to = scrapeAndMoveSendAddress.optJSONObject("to");
				if (to != null) {
					Map<String, Object> toMap = new HashMap<>();
					toMap.put("x1", to.optDouble("x1"));
					toMap.put("y1", to.optDouble("y1"));
					toMap.put("orientation", to.optInt("orientation"));
					toMap.put("new_font", to.optString("new_font"));
					toMap.put("line_spacing", to.optDouble("line_spacing"));
					scrapeAndMoveSendMap.put("to", toMap);
				}

				masterDataPrepConfigFile.setSCRAPE_AND_MOVE_SEND_ADDRESS(scrapeAndMoveSendMap);
			}

			// insert new pages
			Gson gson = new Gson();
			JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
			JsonArray insertNewPages = (JsonArray) jsonObject.get("INSERT_NEW_PAGES");
			List<Map<String, JsonElement>> newPageList = new ArrayList<>();
			if (insertNewPages != null && insertNewPages.size() > 0) {
				for (int i = 0; i < insertNewPages.size(); i++) {
					JsonObject ob = (JsonObject) insertNewPages.get(i);
					newPageList.add(ob.asMap());
				}
				masterDataPrepConfigFile.setINSERT_NEW_PAGES(newPageList);
			}

			JSONArray hopperLookupArray = masterDataprepJsonObject.optJSONArray("INSERT_ID_HOPPER_LOOKUP");
			if (hopperLookupArray != null) {
				Map<String, Integer> hopperLookupMap = new HashMap<>();
				for (int i = 0; i < hopperLookupArray.length(); i++) {
					JSONObject obj = hopperLookupArray.getJSONObject(i);
					Iterator<String> keys = obj.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						int value = obj.optInt(key);
						hopperLookupMap.put(key, value);
					}
				}
				masterDataPrepConfigFile.setINSERT_ID_HOPPER_LOOKUP(hopperLookupMap);
			}

			masterDataPrepConfigFile.setEACH_PDF_DOCUMENT_STARTS_NEW_SHEET(
					masterDataprepJsonObject.optString("EACH_PDF_DOCUMENT_STARTS_NEW_SHEET"));

			// Finish setting the values

			// Parse the SHIPPING_SH_CODE_LOOKUP section
			JSONArray shippingShCodeLookupArray = masterDataprepJsonObject.optJSONArray("SHIPPING_SH_CODE_LOOKUP");
			List<Map<String, Object>> shippingShCodeLookupList = new ArrayList<>();
			if (shippingShCodeLookupArray != null) {
				for (int i = 0; i < shippingShCodeLookupArray.length(); i++) {
					JSONObject entry = shippingShCodeLookupArray.optJSONObject(i);
					if (entry != null) {
						Map<String, Object> map = new HashMap<>();
						map.put("carrier", entry.optString("carrier").toLowerCase());
						map.put("service", entry.optString("service").toLowerCase());
						map.put("sh_code", entry.optInt("sh_code"));
						shippingShCodeLookupList.add(map);
					}
				}
			}
			masterDataPrepConfigFile.setSHIPPING_SH_CODE_LOOKUP(shippingShCodeLookupList);

			// Add Text
			JSONArray addTextToPagesArray = masterDataprepJsonObject.optJSONArray("ADD_TEXT_TO_PAGES");
			List<Map<String, Object>> addTextToPagesList = new ArrayList<>();

			if (addTextToPagesArray != null && addTextToPagesArray.length() > 0) {
				for (int i = 0; i < addTextToPagesArray.length(); i++) {
					JSONObject pageEntry = addTextToPagesArray.optJSONObject(i);
					if (pageEntry != null) {
						Map<String, Object> pageMap = new HashMap<>();

						pageMap.put("page_num", pageEntry.optInt("page_num", -1));
						pageMap.put("bpt_rec", pageEntry.optString("bpt_rec", ""));
						pageMap.put("ept_rec", pageEntry.optString("ept_rec", ""));

						JSONArray mcfArray = pageEntry.optJSONArray("mcf_recs");
						if (mcfArray != null) {
							List<Map<String, Object>> mcfRecs = new ArrayList<>();
							for (int j = 0; j < mcfArray.length(); j++) {
								JSONObject mcfObj = mcfArray.optJSONObject(j);
								if (mcfObj != null) {
									Map<String, Object> mcfMap = new HashMap<>();
									Iterator<String> mcfKeys = mcfObj.keys();
									while (mcfKeys.hasNext()) {
										String key = mcfKeys.next();
										mcfMap.put(key, mcfObj.optString(key));
									}
									mcfRecs.add(mcfMap);
								}
							}
							pageMap.put("mcf_recs", mcfRecs);
						}

						JSONArray ptxArray = pageEntry.optJSONArray("ptx_recs");
						if (ptxArray != null) {
							List<Map<String, Object>> ptxRecs = new ArrayList<>();
							for (int j = 0; j < ptxArray.length(); j++) {
								JSONObject ptxObj = ptxArray.optJSONObject(j);
								if (ptxObj != null) {
									Map<String, Object> ptxMap = new HashMap<>();
									Iterator<String> ptxKeys = ptxObj.keys();
									while (ptxKeys.hasNext()) {
										String key = ptxKeys.next();
										ptxMap.put(key, ptxObj.optString(key));
									}
									ptxRecs.add(ptxMap);
								}
							}
							pageMap.put("ptx_recs", ptxRecs);
						}

						addTextToPagesList.add(pageMap);
					}
				}
			}
			
			masterDataPrepConfigFile.setADD_TEXT_TO_PAGES(addTextToPagesList);

		}

	}

	// This method will provide access to the loaded config
	public static MasterDataPrepConfigFile getConfig() throws JSONException, IOException {
		if (masterDataPrepConfigFile == null) {
			loadMasterDataprepConfig();
		}
		return masterDataPrepConfigFile;
	}
}
