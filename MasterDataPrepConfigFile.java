package com.broadridge.pdf2print.dataprep.service;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

public class MasterDataPrepConfigFile {

	private String INSERT_BNG_ENG_RECS;
	private List<Map<String, String>> TRANSLATE_IMM_RECS;
	private List<Map<String, String>> SET_IMM_RECS;
	private List<Map<String, Object>> DELETE_TEXT;
	private List<String> INSERT_FILE_LEVEL_NOPS;
	private List<Map<String, String>> INSERT_FILE_LEVEL_NAMED_NOPS;
	private List<Map<String, String>> INSERT_FILE_HEADER_NAMED_NOPS;
	private List<Map<String, String>> INSERT_FILE_TRAILER_NAMED_NOPS;
	private List<Map<String, String>> INSERT_MAIL_PIECE_LEVEL_TLES;
	private List<Map<String, String>> INSERT_MAIL_PIECE_TLES;
	private List<Map<String, String>> INSERT_MAIL_PIECE_LEVEL_NAMED_NOPS;
	private List<Map<String, String>> INSERT_MAIL_PIECE_NAMED_NOPS;
	private List<Map<String, String>> INSERT_OVERLAYS;
	private List<Map<String, String>> INSERT_PSEGS;
	private Map<String, Object> SCRAPE_SEND_ADDRESS;
	private Map<String, Object> SCRAPE_AND_MOVE_SEND_ADDRESS;
	private List<Map<String, JsonElement>> INSERT_NEW_PAGES;
	private Map<String, String> CDFS;
	private Map<String, String> CHECK_IMM_TABLE;
	private Map<String, Integer> INSERT_ID_HOPPER_LOOKUP;
	private String EACH_PDF_DOCUMENT_STARTS_NEW_SHEET;
	private List<Map<String, Object>> SHIPPING_SH_CODE_LOOKUP;

	// Getters and Setters
	
	public List<Map<String, Object>> getSHIPPING_SH_CODE_LOOKUP() {
		return SHIPPING_SH_CODE_LOOKUP;
	}

	public void setSHIPPING_SH_CODE_LOOKUP(List<Map<String, Object>> sHIPPING_SH_CODE_LOOKUP) {
		SHIPPING_SH_CODE_LOOKUP = sHIPPING_SH_CODE_LOOKUP;
	}

	public Map<String, Integer> getINSERT_ID_HOPPER_LOOKUP() {
		return INSERT_ID_HOPPER_LOOKUP;
	}

	public void setINSERT_ID_HOPPER_LOOKUP(Map<String, Integer> iNSERT_ID_HOPPER_LOOKUP) {
		INSERT_ID_HOPPER_LOOKUP = iNSERT_ID_HOPPER_LOOKUP;
	}


	public String getINSERT_BNG_ENG_RECS() {

		return INSERT_BNG_ENG_RECS;

	}

	public void setINSERT_BNG_ENG_RECS(String INSERT_BNG_ENG_RECS) {

		this.INSERT_BNG_ENG_RECS = INSERT_BNG_ENG_RECS;

	}

	public List<Map<String, String>> getTRANSLATE_IMM_RECS() {

		return TRANSLATE_IMM_RECS;

	}

	public void setTRANSLATE_IMM_RECS(List<Map<String, String>> TRANSLATE_IMM_RECS) {

		this.TRANSLATE_IMM_RECS = TRANSLATE_IMM_RECS;

	}

	public List<Map<String, String>> getSET_IMM_RECS() {

		return SET_IMM_RECS;

	}

	public void setSET_IMM_RECS(List<Map<String, String>> SET_IMM_RECS) {

		this.SET_IMM_RECS = SET_IMM_RECS;

	}

	public List<Map<String, Object>> getDELETE_TEXT() {

		return DELETE_TEXT;

	}

	public void setDELETE_TEXT(List<Map<String, Object>> DELETE_TEXT) {

		this.DELETE_TEXT = DELETE_TEXT;

	}

	public List<String> getINSERT_FILE_LEVEL_NOPS() {

		return INSERT_FILE_LEVEL_NOPS;

	}

	public void setINSERT_FILE_LEVEL_NOPS(List<String> INSERT_FILE_LEVEL_NOPS) {

		this.INSERT_FILE_LEVEL_NOPS = INSERT_FILE_LEVEL_NOPS;

	}

	public List<Map<String, String>> getINSERT_FILE_LEVEL_NAMED_NOPS() {

		return INSERT_FILE_LEVEL_NAMED_NOPS;

	}

	public void setINSERT_FILE_LEVEL_NAMED_NOPS(List<Map<String, String>> INSERT_FILE_LEVEL_NAMED_NOPS) {

		this.INSERT_FILE_LEVEL_NAMED_NOPS = INSERT_FILE_LEVEL_NAMED_NOPS;

	}

	public List<Map<String, String>> getINSERT_FILE_HEADER_NAMED_NOPS() {

		return INSERT_FILE_HEADER_NAMED_NOPS;

	}

	public void setINSERT_FILE_HEADER_NAMED_NOPS(List<Map<String, String>> INSERT_FILE_HEADER_NAMED_NOPS) {

		this.INSERT_FILE_HEADER_NAMED_NOPS = INSERT_FILE_HEADER_NAMED_NOPS;

	}

	public List<Map<String, String>> getINSERT_FILE_TRAILER_NAMED_NOPS() {

		return INSERT_FILE_TRAILER_NAMED_NOPS;

	}

	public void setINSERT_FILE_TRAILER_NAMED_NOPS(List<Map<String, String>> INSERT_FILE_TRAILER_NAMED_NOPS) {

		this.INSERT_FILE_TRAILER_NAMED_NOPS = INSERT_FILE_TRAILER_NAMED_NOPS;

	}

	public List<Map<String, String>> getINSERT_MAIL_PIECE_LEVEL_TLES() {

		return INSERT_MAIL_PIECE_LEVEL_TLES;

	}

	public List<Map<String, String>> getINSERT_MAIL_PIECE_TLES() {

		return INSERT_MAIL_PIECE_TLES;

	}

	public void setINSERT_MAIL_PIECE_TLES(List<Map<String, String>> INSERT_MAIL_PIECE_TLES) {

		this.INSERT_MAIL_PIECE_TLES = INSERT_MAIL_PIECE_TLES;

	}

	public void setINSERT_MAIL_PIECE_LEVEL_TLES(List<Map<String, String>> INSERT_MAIL_PIECE_LEVEL_TLES) {

		this.INSERT_MAIL_PIECE_LEVEL_TLES = INSERT_MAIL_PIECE_LEVEL_TLES;

	}

	public List<Map<String, String>> getINSERT_MAIL_PIECE_LEVEL_NAMED_NOPS() {

		return INSERT_MAIL_PIECE_LEVEL_NAMED_NOPS;

	}

	public void setINSERT_MAIL_PIECE_LEVEL_NAMED_NOPS(List<Map<String, String>> INSERT_MAIL_PIECE_LEVEL_NAMED_NOPS) {

		this.INSERT_MAIL_PIECE_LEVEL_NAMED_NOPS = INSERT_MAIL_PIECE_LEVEL_NAMED_NOPS;

	}

	public List<Map<String, String>> getINSERT_MAIL_PIECE_NAMED_NOPS() {

		return INSERT_MAIL_PIECE_NAMED_NOPS;

	}

	public void setINSERT_MAIL_PIECE_NAMED_NOPS(List<Map<String, String>> INSERT_MAIL_PIECE_NAMED_NOPS) {

		this.INSERT_MAIL_PIECE_NAMED_NOPS = INSERT_MAIL_PIECE_NAMED_NOPS;

	}

	public List<Map<String, String>> getINSERT_OVERLAYS() {

		return INSERT_OVERLAYS;

	}

	public void setINSERT_OVERLAYS(List<Map<String, String>> INSERT_OVERLAYS) {

		this.INSERT_OVERLAYS = INSERT_OVERLAYS;

	}

	public List<Map<String, String>> getINSERT_PSEGS() {

		return INSERT_PSEGS;

	}

	public void setINSERT_PSEGS(List<Map<String, String>> INSERT_PSEGS) {

		this.INSERT_PSEGS = INSERT_PSEGS;

	}

	public Map<String, Object> getSCRAPE_SEND_ADDRESS() {

		return SCRAPE_SEND_ADDRESS;

	}

	public void setSCRAPE_SEND_ADDRESS(Map<String, Object> SCRAPE_SEND_ADDRESS) {

		this.SCRAPE_SEND_ADDRESS = SCRAPE_SEND_ADDRESS;

	}

	public Map<String, Object> getSCRAPE_AND_MOVE_SEND_ADDRESS() {

		return SCRAPE_AND_MOVE_SEND_ADDRESS;

	}

	public void setSCRAPE_AND_MOVE_SEND_ADDRESS(Map<String, Object> SCRAPE_AND_MOVE_SEND_ADDRESS) {

		this.SCRAPE_AND_MOVE_SEND_ADDRESS = SCRAPE_AND_MOVE_SEND_ADDRESS;

	}

	public List<Map<String, JsonElement>> getINSERT_NEW_PAGES() {

		return INSERT_NEW_PAGES;

	}

	public void setINSERT_NEW_PAGES(List<Map<String, JsonElement>> INSERT_NEW_PAGES) {

		this.INSERT_NEW_PAGES = INSERT_NEW_PAGES;

	}

	public Map<String, String> getCDFS() {

		return CDFS;

	}

	public void setCDFS(Map<String, String> CDFS) {

		this.CDFS = CDFS;

	}

	public Map<String, String> getCHECK_IMM_TABLE() {

		return CHECK_IMM_TABLE;

	}

	public void setCHECK_IMM_TABLE(Map<String, String> CHECK_IMM_TABLE) {

		this.CHECK_IMM_TABLE = CHECK_IMM_TABLE;

	}

	// Inner classes for nested objects

	public static class DeleteText {

		private int page_num;

		private double x1, y1, x2, y2;

		private int iOrientation;

		public int getPage_num() {

			return page_num;

		}

		public void setPage_num(int page_num) {

			this.page_num = page_num;

		}

		public double getX1() {

			return x1;

		}

		public void setX1(double x1) {

			this.x1 = x1;

		}

		public double getY1() {

			return y1;

		}

		public void setY1(double y1) {

			this.y1 = y1;

		}

		public double getX2() {

			return x2;

		}

		public void setX2(double x2) {

			this.x2 = x2;

		}

		public double getY2() {

			return y2;

		}

		public void setY2(double y2) {

			this.y2 = y2;

		}

		public int getiOrientation() {

			return iOrientation;

		}

		public void setiOrientation(int iOrientation) {

			this.iOrientation = iOrientation;

		}

	}
		public String getEACH_PDF_DOCUMENT_STARTS_NEW_SHEET() {
		return EACH_PDF_DOCUMENT_STARTS_NEW_SHEET;
	}

	public void setEACH_PDF_DOCUMENT_STARTS_NEW_SHEET(String eACH_PDF_DOCUMENT_STARTS_NEW_SHEET) {
		EACH_PDF_DOCUMENT_STARTS_NEW_SHEET = eACH_PDF_DOCUMENT_STARTS_NEW_SHEET;
	}

	// Additional classes for other nested structures can be added similarly

}
