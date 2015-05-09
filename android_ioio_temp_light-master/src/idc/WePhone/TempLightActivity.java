package idc.WePhone;

import com.androidzeitgeist.ani.transmitter.Transmitter;
import com.androidzeitgeist.ani.transmitter.TransmitterException;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import net.mitchtech.ioio.templight.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;


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
	int insidePhones = 0;



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
		private AnalogInput[] lightInput = new AnalogInput[4];

		@Override
		public void setup() throws ConnectionLostException {
			try {
				lightInput[0] = ioio_.openAnalogInput(PHOTOCELL_PIN1);
				lightInput[1] = ioio_.openAnalogInput(PHOTOCELL_PIN2);
				lightInput[2] = ioio_.openAnalogInput(PHOTOCELL_PIN3);
				lightInput[3] = ioio_.openAnalogInput(PHOTOCELL_PIN4);
				if(numOfphones > 0) {

					// Create an Intent object to send.
					Intent intent = new Intent();

					// Transmitter using default multicast address and port.
					Transmitter transmitter = new Transmitter(); 
					transmitter.transmit(intent);
				}
				enableUi(true);
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}catch (TransmitterException exception) {
				// Handle error
			}
		}

		@Override
		public void loop() throws ConnectionLostException {
			try {

				// Discovering light from 4 sensors.
				for(int i = 0; i < 4; i++) {
					connected[i] = lightInput[i].read() * 100;
					if(numOfphones == 0) {
						if(connected[i] <= 30) {
							insidePhones++;
						}
					}
				}

				// Start
				if(insidePhones >= 2) {
					numOfphones = 1;
					insidePhones = 0;
				} else {
					numOfphones = 0;
					insidePhones = 0;
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