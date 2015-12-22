/**
 * 
 */
package de.dotknowledge.xymon;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * @author tschulzhess
 *
 */
public class WidgetProvider extends AppWidgetProvider {
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("WIDGET", "onUpdate");
        
        ContentResolver cr = context.getContentResolver();
    	Cursor cursor = cr.query(XymonStatus.CONTENT_URI, null, null, null, null);
    	int resource = R.drawable.greyball;
    	if (cursor.moveToFirst()) {
	    	resource = cursor.getInt(cursor.getColumnIndex("statusIcon"));
    	}		    	
    	final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);	    	
            views.setImageViewResource(R.id.ball, resource);
            
            // Tell the widget manager
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
