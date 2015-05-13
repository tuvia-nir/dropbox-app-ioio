package com.android.wephone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DBReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getExtras().getString("Action").equals("START")) {
			Log.d("BroadcastReciever", "onReceive()");
			Intent i = new Intent(context, DBRoulette.class);  
			//i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}

}
