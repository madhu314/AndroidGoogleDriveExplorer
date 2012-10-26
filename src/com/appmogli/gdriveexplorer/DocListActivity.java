package com.appmogli.gdriveexplorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

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

public class DocListActivity extends ListActivity {

	public static final String KEY_ROOT_FOLDER_ID = "rootFolderId";
	public static final String KEY_AUTH_TOKEN = "authToken";

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
		folderStack.add(rootFolderId);
		listAdapter = new DocListAdapter(this);
		getListView().setAdapter(listAdapter);
		asyncLoad();
		
		getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				File f = (File) listAdapter.getItem(position);
				if(listAdapter.isFolder(f)) {
					folderStack.add(f.getId());
					asyncLoad();
				}
				
			}
		});
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
						fileListRequest = service.children().list(rootFolderId);
						List<ChildReference> children = fileListRequest
								.execute().getItems();
						if (children != null) {
							for (ChildReference child : children) {
								Get fileRequest = service.files().get(
										child.getId());
								File f = fileRequest.execute();
								Boolean trashed = f.getExplicitlyTrashed();
								if(trashed == null) {
									trashed = false;
								} 
								if (f != null && !trashed) {
									result.add(f);
								}

							}
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
				if(result != null) {
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
		if(folderStack.isEmpty()) {
			super.onBackPressed();
		} else {
			asyncLoad();
		}
		
	}
}
