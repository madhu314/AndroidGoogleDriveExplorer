package com.appmogli.gdriveexplorer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;

public class HttpDownloadManager {

	private String donwloadUrl;
	private String toFile;
	private FileDownloadProgressListener listener;
	private long totalBytes;

	public void setListener(FileDownloadProgressListener listener) {
		this.listener = listener;
	}

	public HttpDownloadManager(String donwloadUrl, String toFile, long totalBytes) {
		super();
		this.donwloadUrl = donwloadUrl;
		this.toFile = toFile;
		this.totalBytes = totalBytes;
	}

	public static interface FileDownloadProgressListener {
		public void downloadProgress(long bytesRead, long totalBytes);

		public void downloadFinished();

		public void downloadFailedWithError(Exception e);
	}

	public boolean download(Drive service) {
		HttpResponse respEntity = null;
		try {
			// URL url = new URL(urlString);
			respEntity = service.getRequestFactory()
					.buildGetRequest(new GenericUrl(donwloadUrl)).execute();
			InputStream in = respEntity.getContent();
			if(totalBytes == 0) {
				totalBytes = respEntity.getContentLoggingLimit();
			}
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

		} catch (IOException ex) {
			if (listener != null) {
				listener.downloadFailedWithError(ex);
				return false;
			}
		} finally {
			if(respEntity != null) {
				try {
					respEntity.disconnect();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return false;
	}
}
