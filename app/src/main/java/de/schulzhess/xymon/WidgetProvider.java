package de.schulzhess.xymon;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * @author tschulzhess
 */
public class WidgetProvider extends AppWidgetProvider {

    public static final String TAG = XymonStatus.TAG + "WIDGET";

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got intent " + intent.getAction());


        }
    };

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");

        ContentResolver cr = context.getContentResolver();
        int resource = R.drawable.greyball;
        Cursor cursor = cr.query(XymonStatus.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst())
                resource = cursor.getInt(cursor.getColumnIndex("statusIcon"));
            else
                Log.e(TAG, "Cannot get status from content URL! - cursor is empty");
            cursor.close();
        } else {
            Log.e(TAG, "Cannot get status from content URL! - cursor is null");
        }

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            views.setImageViewResource(R.id.ball, resource);

            // Tell the widget manager
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
