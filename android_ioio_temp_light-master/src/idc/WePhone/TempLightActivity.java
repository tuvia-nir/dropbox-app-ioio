package idc.WePhone;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import net.mitchtech.ioio.templight.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


public class TempLightActivity extends AbstractIOIOActivity {
	
	private static final String START = "START";
	private static final String STOP = "STOP";
	private final int PHOTOCELL_PIN1 = 35;
	private final int PHOTOCELL_PIN2 = 34;
	private final int PHOTOCELL_PIN3 = 33;
	private final int PHOTOCELL_PIN4 = 32;

	TextView mLightTextView;
	SeekBar mLightSeekBar;
	float[] connected = new float[4];
	

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
		
		// correct initialization?
	    receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
		mLightTextView = (TextView) findViewById(R.id.tvLight);
		mLightSeekBar = (SeekBar) findViewById(R.id.sbLight);
		
		// Starting discovery for peers.
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
		    @Override
		    public void onSuccess() {
		    	Toast.makeText(getApplicationContext(), "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
		    	
		    }

		    // is it necessary?
		    @Override
		    public void onFailure(int reasonCode) {
		    	Toast.makeText(getApplicationContext(), "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
		    }
		});
		manager.connect(channel, config, new ActionListener() {

		    @Override // why should i take it off?
		    public void onSuccess() {
		    	Toast.makeText(getApplicationContext(), "Connect success",
                        Toast.LENGTH_SHORT).show();		    }

		    public void onFailure(int reasonCode) {
		    	Toast.makeText(getApplicationContext(), "connect Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
		    }
		});

		// Open a socket connection - only if there are 2 phones in slot??
		Context context = getApplicationContext();
		String host;
		int port;
		int len;
		byte buf[]  = new byte[512];
		Socket socket = new Socket();
		try {
		    /**
		     * Create a client socket with the host,
		     * port, and timeout information.
		     */
		    socket.bind(null);
		    socket.connect((new InetSocketAddress(host, port)), 500);

		    /**
		     * Create a byte stream from a JPEG file and pipe it to the output stream
		     * of the socket. This data will be retrieved by the server device.
		     */
		    OutputStream outputStream = socket.getOutputStream();
		    InputStream inputStream = null;
		    ContentResolver cr = context.getContentResolver();
		    inputStream = cr.openInputStream(Uri.parse(START));
		    while ((len = inputStream.read(buf)) != -1) {
		    	outputStream.write(buf, 0, len);
		    } 
		    outputStream.close();
		    inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}/* finally {    should we close socket or leave connected until the end?
		    if (socket != null) {
		        if (socket.isConnected()) {
		            try {
		                socket.close();
		            } catch (IOException e) {
		                //catch logic
		            }
		        }
		    }
		} */
		enableUi(false);
	}
	
	/* register the broadcast receiver with the intent values to be matched */
	@Override
	protected void onResume() {
	    super.onResume();
	    registerReceiver(receiver, intentFilter);
	}
	
	/* unregister the broadcast receiver */
	@Override
	protected void onPause() {
	    super.onPause();
	    unregisterReceiver(receiver);
	}
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private AnalogInput[] lightInput = new AnalogInput[4];
		
		@Override
		public void setup() throws ConnectionLostException {
			try {
				lightInput[0] = ioio_.openAnalogInput(PHOTOCELL_PIN1);
				lightInput[1] = ioio_.openAnalogInput(PHOTOCELL_PIN2);
				lightInput[2] = ioio_.openAnalogInput(PHOTOCELL_PIN3);
				lightInput[3] = ioio_.openAnalogInput(PHOTOCELL_PIN4);
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {
				
				// Discovering light from 4 sensors.
				for(int i = 0; i < 4; i++) {
					connected[i] = lightInput[i].read() * 100;
				}
				
				setSeekBar((int) (connected[0]));
				setText(Float.toString((connected[0])));
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