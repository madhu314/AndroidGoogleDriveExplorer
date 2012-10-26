package com.appmogli.gdriveexplorer;

import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpDownloadManager {

	private String donwloadUrl;
	private String toFile;
	private FileDownloadProgressListener listener;
	
	
	public void setListener(FileDownloadProgressListener listener) {
		this.listener = listener;
	}


	public HttpDownloadManager(String donwloadUrl, String toFile) {
		super();
		this.donwloadUrl = donwloadUrl;
		this.toFile = toFile;
	}
	
	public static interface FileDownloadProgressListener {
		public void downloadProgress(long bytesRead, long totalBytes);
		public void downloadFinished();
		public void downloadFailedWithError(Exception e);
	}
	
	
	public boolean download() {
		try {
            // URL url = new URL(urlString);
            HttpGet httpGet = new HttpGet(donwloadUrl);
            httpGet.addHeader("User-Agent", "AndroidGoogleDriveExplorer");

            HttpClient client = new DefaultHttpClient();
            HttpResponse resp = client.execute(httpGet);
            
            HttpEntity respEntity = resp.getEntity();
        	long totalBytes = respEntity.getContentLength();
    		InputStream in = respEntity.getContent();
    		try {
    			FileOutputStream f = new FileOutputStream(toFile);
    			byte[] buffer = new byte[1024];
    			int len1 = 0;
    			long bytesRead = 0;
    			while ((len1 = in.read(buffer)) > 0) {
    				f.write(buffer, 0, len1);
    				if (listener != null) {
    					bytesRead += len1;
    					listener.downloadProgress(bytesRead, totalBytes);
    				}

    			}
    			f.close();
    		} catch (Exception e) {
    			if (listener != null) {
    				listener.downloadFailedWithError(e);
    			}
    			return false;
    		}
    		if (listener != null) {
    			listener.downloadFinished();
    		}
			return true;

        } catch (Exception ex) {
        	if (listener != null) {
				listener.downloadFailedWithError(ex);
				return false;
			}
        }
		return false;
	}
}
