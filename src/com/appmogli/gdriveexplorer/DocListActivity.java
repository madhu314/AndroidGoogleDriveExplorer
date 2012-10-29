package com.appmogli.gdriveexplorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.appmogli.gdriveexplorer.HttpDownloadManager.FileDownloadProgressListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DocListActivity extends ListActivity {

	public static final String KEY_ROOT_FOLDER_ID = "rootFolderId";
	public static final String KEY_AUTH_TOKEN = "authToken";
	protected static final String TAG = "DocListActivity";

	private DocListAdapter listAdapter = null;
	private ProgressDialog progressDialog = null;

	private ArrayList<String> folderStack = new ArrayList<String>();
	private Map<String, List<File>> rootToFilesMap = new HashMap<String, List<File>>();
	private String authToken = null;

	private final HttpTransport transport = AndroidHttp
			.newCompatibleTransport();

	private final JsonFactory jsonFactory = new GsonFactory();
	private final GoogleCredential credential = new GoogleCredential();
	private Drive service;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		String rootFolderId = getIntent().getStringExtra(KEY_ROOT_FOLDER_ID);
		authToken = getIntent().getStringExtra(KEY_AUTH_TOKEN);

		service = new Drive.Builder(transport, jsonFactory, credential)
				.setApplicationName("Google-DriveAndroidSample/1.0")
				.setJsonHttpRequestInitializer(
						new GoogleKeyInitializer(ClientCredentials.KEY))
				.build();
		credential.setAccessToken(authToken);
		Logger.getLogger("com.google.api.client").setLevel(Level.ALL);
		
		folderStack.add(rootFolderId);
		listAdapter = new DocListAdapter(this);
		getListView().setAdapter(listAdapter);
		asyncLoad();

		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				File f = (File) listAdapter.getItem(position);
				if (listAdapter.isFolder(f)) {
					folderStack.add(f.getId());
					asyncLoad();
				} else {
					// download this file and open it in a viewer
					showDownloadOptions(f);
				}

			}
		});
	}

	private void showDownloadOptions(final File f) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Format");
		final Map<String, String> exportLinks = f.getExportLinks();
		if (exportLinks != null) {
			List<String> typesList = new ArrayList<String>();
			for (String exportType : exportLinks.keySet()) {
				String mimeType = ExportTypeMappings.getReadableString(exportType);
				if(mimeType != null) {
					typesList.add(mimeType);
				}
			}
			final String[] types = new String[typesList.size()];
			typesList.toArray(types);

			builder.setItems(types, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String exportType = ExportTypeMappings
							.getExportTypeString(types[which]);
					String downloadLink = exportLinks.get(exportType);
					downloadAndViewFile(downloadLink, f.getTitle() + "."
							+ types[which], exportType, f.getFileSize() == null ? 0 : f.getFileSize());
				}
			});
			builder.setNegativeButton("Cancel", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();

		} else {
			downloadAndViewFile(f.getDownloadUrl(), f.getTitle(),
					f.getMimeType(), f.getFileSize());

		}
	}

	private void downloadAndViewFile(final String downloadUrl, String fileName,
			final String mimeType, final long totalBytes) {
		Log.d(TAG, "donwload link is:" + downloadUrl);
		Log.d(TAG, "File name is:" + fileName);
		java.io.File cacheDir = getExternalCacheDir();
		final java.io.File toFile = new java.io.File(cacheDir, fileName);
		new AsyncTask<Void, Integer, Boolean>() {
			final ProgressDialog fileDownloadProgressDialog = new ProgressDialog(
					DocListActivity.this);
			
			protected void onPreExecute() {
				fileDownloadProgressDialog.setIndeterminate(false);
				fileDownloadProgressDialog.setMax(100);
				fileDownloadProgressDialog.show();
				
			};
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					toFile.createNewFile();

					final HttpDownloadManager downloader = new HttpDownloadManager(
							downloadUrl, toFile.toString(), totalBytes);
					downloader.setListener(new FileDownloadProgressListener() {

						@Override
						public void downloadProgress(long bytesRead, long totalBytes) {
							float progress = (float) bytesRead * 100
									/ (float) totalBytes;
							publishProgress(Math.round(progress));

						}

						@Override
						public void downloadFinished() {
							
						}

						@Override
						public void downloadFailedWithError(Exception e) {
							
							
						}
					});
					return downloader.download(service);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return false;
			}
			
			protected  void onProgressUpdate(Integer[] values) {
				fileDownloadProgressDialog.setProgress(values[0]);
			};
			
			protected void onPostExecute(Boolean result) {
				fileDownloadProgressDialog.cancel();
				
				if(result) {
					Intent viewIntent = new Intent();
					viewIntent.setAction(Intent.ACTION_VIEW);
					viewIntent.setDataAndType(
							Uri.parse("file://" + toFile.toString()), mimeType);
					startActivity(viewIntent);
				} else {
					//
					Toast.makeText(DocListActivity.this,
							"Error downloading file", Toast.LENGTH_SHORT)
							.show();
				}
				
			};
			
		}.execute();
		
		
	}

	private void asyncLoad() {
		new AsyncTask<Void, Void, List<File>>() {
			ProgressDialog dialog = new ProgressDialog(DocListActivity.this);

			protected void onPreExecute() {
				dialog.setMessage("Loading Data...");
				dialog.setIndeterminate(true);
				dialog.show();
			};

			@Override
			protected List<File> doInBackground(Void... params) {
				String rootFolderId = folderStack.get(folderStack.size() - 1);
				if (rootToFilesMap.containsKey(rootFolderId)) {
					return rootToFilesMap.get(rootFolderId);
				} else {
					List<File> result = new ArrayList<File>();
					com.google.api.services.drive.Drive.Children.List fileListRequest;
					try {
//						fileListRequest = service.children().list(rootFolderId);
//						List<ChildReference> children = fileListRequest
//								.execute().getItems();
//						if (children != null) {
//							for (ChildReference child : children) {
//								Get fileRequest = service.files().get(
//										child.getId());
//								File f = fileRequest.execute();
//								Boolean trashed = f.getExplicitlyTrashed();
//								if (trashed == null) {
//									trashed = false;
//								}
//								if (f != null && !trashed) {
//									result.add(f);
//								}
//
//							}
//							rootToFilesMap.put(rootFolderId, result);
//							return result;
//
//						}
						
						com.google.api.services.drive.Drive.Files.List list = service.files().list();
						list.setQ("\"" + rootFolderId + "\"" + " in parents");
						FileList fileList = list.execute();
						result = fileList.getItems();
						if(result != null) {
							rootToFilesMap.put(rootFolderId, result);
							return result;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				return null;

			}

			protected void onPostExecute(List<File> result) {
				dialog.cancel();
				if (result != null) {
					listAdapter.setNewData(result);
				} else {

				}
			};
		}.execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (listAdapter != null) {
			listAdapter.onDestroy();
		}
	}

	@Override
	public void onBackPressed() {
		folderStack.remove(folderStack.size() - 1);
		if (folderStack.isEmpty()) {
			super.onBackPressed();
		} else {
			asyncLoad();
		}

	}
}
