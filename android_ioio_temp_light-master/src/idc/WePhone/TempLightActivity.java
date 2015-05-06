package idc.WePhone;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import idc.WePhone.ioio.templight.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;



public class TempLightActivity extends AbstractIOIOActivity {
	
	private final int PHOTOCELL_PIN1 = 35;
	private final int PHOTOCELL_PIN2 = 34;
	private final int PHOTOCELL_PIN3 = 33;
	private final int PHOTOCELL_PIN4 = 32;

	TextView mLightTextView;
	SeekBar mLightSeekBar;
	float isConnected1;
	float isConnected2;
	float isConnected3;
	float isConnected4;

	int numOfphones = 0;
    private WifiP2pManager manager;
    private Channel channel;
    private BroadcastReceiver receiver = null; // how to use this reciever?
    private final IntentFilter intentFilter = new IntentFilter();

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		// initialize intent filter and wifimanager + channel.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);
		mLightTextView = (TextView) findViewById(R.id.tvLight);
		mLightSeekBar = (SeekBar) findViewById(R.id.sbLight);

		enableUi(false);
	}

	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private AnalogInput mLightInput1;
		private AnalogInput mLightInput2;
		private AnalogInput mLightInput3;
		private AnalogInput mLightInput4;

		@Override
		public void setup() throws ConnectionLostException {
			try {
				mLightInput1 = ioio_.openAnalogInput(PHOTOCELL_PIN1);
				mLightInput2 = ioio_.openAnalogInput(PHOTOCELL_PIN2);
				mLightInput3 = ioio_.openAnalogInput(PHOTOCELL_PIN3);
				mLightInput4 = ioio_.openAnalogInput(PHOTOCELL_PIN4);
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				
				// Discovering light from sensor1 - should duplicate is it correct 4 readings??
				final float lightReading1 = mLightInput1.read();
				isConnected1 = lightReading1 * 100;
				final float lightReading2 = mLightInput2.read();
				isConnected2 = lightReading2 * 100;
				final float lightReading3 = mLightInput3.read();
				isConnected3 = lightReading3 * 100;
				final float lightReading4 = mLightInput4.read();
				isConnected4 = lightReading4 * 100;
				setSeekBar((int) (isConnected1));
				setText(Float.toString((isConnected1)));
				
				// Starting discovery for peers.
				manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
				    @Override
				    public void onSuccess() {
				    	Toast.makeText(getApplicationContext(), "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
				    	if((int)isConnected1 < 30) {
				    	numOfphones++;
				    	}
				    	if((int)isConnected2 < 30) {
					    	numOfphones++;
				    	}
				    	if((int)isConnected3 < 30) {
					    	numOfphones++;
				    	}
				    	if((int)isConnected4 < 30) {
					    	numOfphones++;
				    	}
				    	manager.notify(); // how to transfer a message to start application?
				    	
				    }

				    // is it necessary?
				    @Override
				    public void onFailure(int reasonCode) {
				    	Toast.makeText(getApplicationContext(), "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
				    }
				});
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