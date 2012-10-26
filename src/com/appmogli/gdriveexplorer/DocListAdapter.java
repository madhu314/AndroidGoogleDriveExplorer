package com.appmogli.gdriveexplorer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.drive.model.File;

public class DocListAdapter extends BaseAdapter {

	private Context context = null;
	private List<File> files = new ArrayList<File>();
	private Set<WeakReference<View>> viewSet = new HashSet<WeakReference<View>>();

	public DocListAdapter(Context context) {
		this.context = context;
	}

	public void setNewData(List<File> newFiles) {
		List<File> foldersOnly = new ArrayList<File>();
		List<File> filesOnly = new ArrayList<File>();
		this.files = new ArrayList<File>();
		if(newFiles != null){
			for (File f : newFiles) {
				if (f.getMimeType().equals("application/vnd.google-apps.folder")) {
					foldersOnly.add(f);
				} else {
					filesOnly.add(f);
				}
			}
		}
		
		
		this.files.addAll(foldersOnly);
		this.files.addAll(filesOnly);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return files.size();
	}

	@Override
	public Object getItem(int position) {
		return files.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					R.layout.activyt_doclist_item, null);
			ViewHolder vh = new ViewHolder();
			vh.fileNameView = (TextView) convertView
					.findViewById(R.id.activity_doclist_item_text);
			vh.leftImage = (ImageView) convertView
					.findViewById(R.id.activity_doclist_item_icon);
			vh.rightNav = (ImageView) convertView
					.findViewById(R.id.activity_doclist_item_nav);
			vh.rightBar = convertView
					.findViewById(R.id.activity_doclist_item_rightbar);
			viewSet.add(new WeakReference<View>(convertView));
			convertView.setTag(vh);
		}

		ViewHolder vh = (ViewHolder) convertView.getTag();
		File f = files.get(position);
		if (isFolder(f)) {
			vh.rightBar.setVisibility(View.VISIBLE);
			vh.rightNav.setVisibility(View.VISIBLE);
			vh.leftImage.setImageResource(R.drawable.folder);
		} else {
			vh.rightBar.setVisibility(View.GONE);
			vh.rightNav.setVisibility(View.GONE);
			vh.leftImage.setImageResource(R.drawable.file);
		}

		vh.fileNameView.setText(f.getTitle());

		return convertView;

	}

	public void onDestroy() {
		for (WeakReference<View> viewRef : viewSet) {
			if (viewRef != null && viewRef.get() != null) {
				viewRef.get().setTag(null);
			}
		}
	}

	public boolean isFolder(File f) {
		return f.getMimeType().equals("application/vnd.google-apps.folder");
	}

}

class ViewHolder {
	TextView fileNameView;
	ImageView leftImage;
	ImageView rightNav;
	View rightBar;
}
