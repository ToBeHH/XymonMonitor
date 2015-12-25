package de.dotknowledge.xymon;

import java.text.DateFormat;
import java.util.Date;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

public class XymonStatus extends ContentProvider {
	public static final String RED = "red";
	public static final String GREEN = "green";
	public static final String YELLOW = "yellow";
	public static final String UNKNOWN = "unknown";
	public static final String EXCEPTION = "exception";
	public static final String PURPLE = "purple";

	public Date lastChecked;

	public String overallStatus;

	public String detailedStatus;

	public static final Uri CONTENT_URI = Uri
			.parse("content://de.dotknowledge.xymon.status");

	public void updateStatus(View view) {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		String statusText = dateFormat.format(lastChecked) + "\n";

		if (GREEN.equalsIgnoreCase(overallStatus)) {
			statusText = statusText
					+ view.getContext().getString(R.string.status_green);
			view.setBackgroundColor(0xFF006400);
		} else if (YELLOW.equalsIgnoreCase(overallStatus)) {
			statusText = statusText
					+ view.getContext().getString(R.string.status_yellow);
			view.setBackgroundColor(0xFFC0C000);
		} else if (RED.equalsIgnoreCase(overallStatus)) {
			statusText = statusText
					+ view.getContext().getString(R.string.status_red);
			view.setBackgroundColor(0xFFC80000);
		} else if (PURPLE.equalsIgnoreCase(overallStatus)) {
			statusText = statusText
					+ view.getContext().getString(R.string.status_purple);
			view.setBackgroundColor(0xFFC800C8);
		} else if (EXCEPTION.equalsIgnoreCase(overallStatus)) {
			statusText = statusText
					+ view.getContext().getString(R.string.status_exception);
			view.setBackgroundColor(0xFFC80000);
		} else {
			statusText = statusText
					+ view.getContext().getString(R.string.status_unknown);
			view.setBackgroundColor(0xFF000000);
		}

		statusText = statusText.replace("###", timeFormat.format(lastChecked));

		TextView headline = (TextView) view.findViewById(R.id.TextViewHeadline);
		headline.setText(statusText);

		TextView details = (TextView) view.findViewById(R.id.TextViewDetail);
		details.setText(detailedStatus);
	}

	public int getStatusIcon() {
		if (GREEN.equalsIgnoreCase(overallStatus)) {
			return R.drawable.greenball;
		} else if (YELLOW.equalsIgnoreCase(overallStatus)) {
			return R.drawable.yellowball;
		} else if (RED.equalsIgnoreCase(overallStatus)) {
			return R.drawable.redball;
		} else if (PURPLE.equalsIgnoreCase(overallStatus)) {
			return R.drawable.purpleball;
		} else {
			return R.drawable.greyball;
		}
	}

	public static boolean isColor(String status) {
		return RED.equalsIgnoreCase(status) || YELLOW.equalsIgnoreCase(status)
				|| GREEN.equalsIgnoreCase(status);
	}

	public int getColorName() {
		if (GREEN.equalsIgnoreCase(overallStatus)) {
			return R.string.color_green;
		} else if (YELLOW.equalsIgnoreCase(overallStatus)) {
			return R.string.color_yellow;
		} else if (RED.equalsIgnoreCase(overallStatus)) {
			return R.string.color_red;
		} else if (PURPLE.equalsIgnoreCase(overallStatus)) {
			return R.string.color_purple;
		} else {
			return R.string.color_black;
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// cannot delete
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.item/vnd.dotknowledge.xymonstatus";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// cannot insert
		return null;
	}

	@Override
	public boolean onCreate() {
		// cannot create
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		MatrixCursor result = new MatrixCursor(new String[] { "color",
				"colorName", "statusIcon", "detailedStatus", "lastChecked" });
		if (Monitor.currentStatus != null) {
			result.addRow(new Object[] { Monitor.currentStatus.overallStatus,
					Monitor.currentStatus.getColorName(),
					Monitor.currentStatus.getStatusIcon(),
					Monitor.currentStatus.detailedStatus,
					Monitor.currentStatus.lastChecked });
		}
		return result;

	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// cannot update
		return 0;
	}
}