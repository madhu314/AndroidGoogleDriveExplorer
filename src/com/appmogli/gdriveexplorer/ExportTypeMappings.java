package com.appmogli.gdriveexplorer;

import java.util.HashMap;
import java.util.Map;

public class ExportTypeMappings {

	private static final Map<String, String> mimeTypeToReadbleMap = new HashMap<String, String>();
	private static final Map<String, String> readableMapToMimeType = new HashMap<String, String>();
	
	static {
		mimeTypeToReadbleMap.put("application/pdf", "pdf");
		mimeTypeToReadbleMap.put("application/rtf", "rtf");
		mimeTypeToReadbleMap.put("application/vnd.oasis.opendocument.text", "odt");
		mimeTypeToReadbleMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
		mimeTypeToReadbleMap.put("text/html", "html");
		mimeTypeToReadbleMap.put("text/plain", "txt");
		
		for(String key : mimeTypeToReadbleMap.keySet()) {
			readableMapToMimeType.put(mimeTypeToReadbleMap.get(key), key);
		}
	}
	
	public static String getReadableString(String mimeType) {
		return mimeTypeToReadbleMap.get(mimeType);
	}
	
	public static String getExportTypeString(String readbaleString) {
		return readableMapToMimeType.get(readbaleString);
	}
}
