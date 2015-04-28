package idc.WePhone;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import net.mitchtech.ioio.templight.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;



public class TempLightActivity extends AbstractIOIOActivity {
	
	private final int PHOTOCELL_PIN = 35;
	
	TextView mLightTextView;
	SeekBar mLightSeekBar;
	float isConnected;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		mLightTextView = (TextView) findViewById(R.id.tvLight);
		mLightSeekBar = (SeekBar) findViewById(R.id.sbLight);

		enableUi(false);
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private AnalogInput mLightInput;

		@Override
		public void setup() throws ConnectionLostException {
			try {
				mLightInput = ioio_.openAnalogInput(PHOTOCELL_PIN);
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				
				final float lightReading = mLightInput.read();
				isConnected = lightReading * 100;
				setSeekBar((int) (isConnected));
				setText(Float.toString((isConnected)));
				if((int)isConnected < 30) {
					// Send signal.
				} else {
					// Cancel signal.
				}
				sleep(10);
				
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
	}

	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mLightSeekBar.setEnabled(enable);
			}
		});
	}

	private void setSeekBar(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mLightSeekBar.setProgress(value);
			}
		});
	}

	private void setText(final String lightStr) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mLightTextView.setText(lightStr);
			}
		});
	}
	
}