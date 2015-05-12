/*
 * Copyright (c) 2010-11 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.android.wephone;

import java.io.File;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidzeitgeist.ani.discovery.Discovery;
import com.androidzeitgeist.ani.discovery.DiscoveryException;
import com.androidzeitgeist.ani.discovery.DiscoveryListener;
import com.dropbox.android.sample.R;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;


public class DBRoulette extends Activity {
	private static final String TAG = "DBRoulette";
	private static final String ACTION_CMD = "Action";


	///////////////////////////////////////////////////////////////////////////
	//                      Your app-specific settings.                      //
	///////////////////////////////////////////////////////////////////////////

	// Replace this with your app key and secret assigned by Dropbox.
	// Note that this is a really insecure way to do this, and you shouldn't
	// ship code which contains your key & secret in such an obvious way.
	// Obfuscation is good.
	private static final String APP_KEY =    "c3ni1eiw3ackhz5";
	private static final String APP_SECRET = "io385nt1sgfs2qb";

	///////////////////////////////////////////////////////////////////////////
	//                      End app-specific settings.                       //
	///////////////////////////////////////////////////////////////////////////

	// You don't need to change these, leave them alone.
	private static final String ACCOUNT_PREFS_NAME = "prefs";
	private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
	private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	private static final boolean USE_OAUTH1 = false;

	DropboxAPI<AndroidAuthSession> mApi;

	private boolean mLoggedIn;

	// Android widgets
	private Button mSubmit;
	private LinearLayout mDisplay;
	private Button mCamera;
	private Button mSlides;
	private Button mGallery;
	private Button mRoulette;

	private ImageView mImage;

	private final String PHOTO_DIR = "/Photos/";

	private static final int NEW_PICTURE = 1;
	private static final int GALLERY_PICTURE = 2;
	private String mCameraFileName;
	private String mGalleryFileName;
	//All the timer members plus handler
	Timer timer;
	TimerTask timerTask;
	boolean slideShowActive = false;
	static int index = 0;
	final Handler timerHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mCameraFileName = savedInstanceState.getString("mCameraFileName");
		}

		// We create a new AuthSession so that we can use the Dropbox API.
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);

		// Basic Android widgets
		setContentView(R.layout.main);
		checkAppKeySetup();

		mSubmit = (Button)findViewById(R.id.auth_button);
		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					logOut();
				} else {
					// Start the remote authentication
					if (USE_OAUTH1) {
						mApi.getSession().startAuthentication(DBRoulette.this);
					} else {
						mApi.getSession().startOAuth2Authentication(DBRoulette.this);
					}
				}
			}
		});

		mDisplay = (LinearLayout)findViewById(R.id.logged_in_display);

		// This is where a photo is displayed
		mImage = (ImageView)findViewById(R.id.image_view);

		// This is the button to take a photo
		mCamera = (Button)findViewById(R.id.photo_button);

		mCamera.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				// Picture from camera
				intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

				// This is not the right way to do this, but for some reason, having
				// it store it in
				// MediaStore.Images.Media.EXTERNAL_CONTENT_URI isn't working right.

				Date date = new Date();
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.US);

				String newPicFile = df.format(date) + ".jpg";
				String outPath = new File(Environment.getExternalStorageDirectory(), newPicFile).getPath();
				File outFile = new File(outPath);

				mCameraFileName = outFile.toString();
				Uri outuri = Uri.fromFile(outFile);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
				Log.i(TAG, "Importing New Picture: " + mCameraFileName);
				try {
					startActivityForResult(intent, NEW_PICTURE);
				} catch (ActivityNotFoundException e) {
					showToast("There doesn't seem to be a camera.");
				}
			}
		});

		// This is the button to take a photo from the gallery
		mGallery = (Button)findViewById(R.id.gallery_button);


		mGallery.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// Picture from gallery
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				//intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);


				Date date = new Date();
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.US);


				String newPicFile = df.format(date) + ".jpg";
				String outPath = new File(Environment.getExternalStorageDirectory(), newPicFile).getPath();
				File outFile = new File(outPath);

				mGalleryFileName = outFile.toString();
				Uri outuri = Uri.fromFile(outFile);
				//intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
				Log.i(TAG, "Importing New Picture: " + mCameraFileName);
				try {
					startActivityForResult(Intent.createChooser(intent,"Please select:"), GALLERY_PICTURE); 
				} catch (ActivityNotFoundException e) {
					showToast("There doesn't seem to be a camera.");
				}
			}
		});

		// Start the server socket.
		
		// This is the button for the slide show
		mSlides = (Button)findViewById(R.id.slides_button);

		// This is the button to present the pictures slides
		mSlides = (Button)findViewById(R.id.slides_button);
		//TODO
		//!!!!!!!!!!!!!!!!ASK SHAY HOW TO GET NUMBER OF PHOTOS IN ANOTHER CLASS!!!!!!!!!!
		//!!!!!!!!!!!!!!!!ASK SHAY HOW TO STOP TIMER ONCLICK AND RESUME IT FROM WHERE IT STOPED ON CLICK!!!!!!!!!!!!!!!!
		//	final int numberOfPhotos = numberPhotos.GetNumberOfPhotos();
		mSlides.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				slideShowActive = true;
				startTimer();
			}
		});

		// (1) Implement a listener

		DiscoveryListener listener = new DiscoveryListener() {
		    public void onDiscoveryStarted() {
		        // The discovery has been started in the background and is now waiting
		        // for incoming Intents.
		    }

		    public void onDiscoveryStopped() {
		        // The discovery has been stopped. The listener won't be notified for
		        // any incoming Intents anymore.
		    }

		    public void onDiscoveryError(Exception exception) {
		        // A (network) error has occured that prevents the discovery from working
		        // probably. The actual Exception that has been thrown in the background
		        // thread is passed to this method. A call of this method is almost always
		        // followed by a call to onDiscoveryStopped()
		    }
		    DBReciever DBReciever = new DBReciever();
		    
		    public void onIntentDiscovered(InetAddress address, Intent intent) {
		    	String Action = intent.getExtras().getString(ACTION_CMD);
		    	if(Action == "START") {
		    		slideShowActive = true;
					startTimer();	
		    	} else if(Action == "STOP") {
		    		if(timer != null) {
			    		timer.cancel();
			    		timer = null;
			    	}
		    	}
		    }
		};
		// (2) Create and start a discovery

		Discovery discovery = new Discovery();
		discovery.setDiscoveryListener(listener);

		// Start discovery
		try {
			discovery.enable();
		} catch (DiscoveryException e) {
			e.printStackTrace();
		} 

		// necessary?
		//discovery.disable() // Stop discovery
		
		// Display the proper UI state if logged in or not
		setLoggedIn(mApi.getSession().isLinked());

	}

	public void startTimer() {
		//set a new Timer
		timer = new Timer();
		//initialize the TimerTask's job
		initializeTimerTask();
		//schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
		timer.schedule(timerTask, 100, 10000); //
	}


	private void initializeTimerTask() {
	
		//Stopping/Resuming the pictures slide show in case somebody wants to
		//Just by clicking on the screen.
		mImage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (slideShowActive) {
					
					//stop the timer, if it's not already null
					if (timer != null) {
						timer.cancel();
						timer = null;
						slideShowActive = false;
					}
				} else {
						slideShowActive = true;
						startTimer();
					}
				}
			});
		
		//Downloading and resenting the pictures
		timerTask = new TimerTask() {
			public void run() {
				timerHandler.post(new Runnable() {
					public void run() {
						DownloadPicture download = new DownloadPicture(DBRoulette.this, mApi, PHOTO_DIR, mImage, index);
						download.execute();
						index++;
					}
				});
			}

		};
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("mCameraFileName", mCameraFileName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		AndroidAuthSession session = mApi.getSession();

		// The next part must be inserted in the onResume() method of the
		// activity from which session.startAuthentication() was called, so
		// that Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				storeAuth(session);
				setLoggedIn(true);
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
				Log.i(TAG, "Error authenticating", e);
			}
		}
	}

	// This is what gets called on finishing a media piece to import
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == NEW_PICTURE) {
			// return from file upload
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = null;
				if (data != null) {
					uri = data.getData();
				}
				if (uri == null && mCameraFileName != null) {
					uri = Uri.fromFile(new File(mCameraFileName));
				}
				File file = new File(mCameraFileName);

				if (uri != null) {
					UploadPicture upload = new UploadPicture(this, mApi, PHOTO_DIR, file);
					upload.execute();
				}
			} else {
				Log.w(TAG, "Unknown Activity Result from mediaImport: "
						+ resultCode);
			}
		} else if (requestCode == GALLERY_PICTURE) {

			// return from file upload
			if (resultCode == Activity.RESULT_OK) {

				/*            	
            	ArrayList<Parcelable> list = data.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            	for (Parcelable parcel : list) {
            		Uri uri = (Uri)parcel;

                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    for (int i = 0; i < filePathColumn.length; i++) {
                        String picturePath = cursor.getString(columnIndex);
                    	File file = new File(picturePath);
                    	UploadPicture upload = new UploadPicture(DBRoulette.this, mApi, PHOTO_DIR, file);
                    	upload.execute();
					}
                    cursor.close();

            	}
				 */          	


				Uri uri = null;
				if (data != null) {
					uri = data.getData();

					String possiblePath = getPath(getApplicationContext(), uri);

					File file = new File(possiblePath);
					UploadPicture upload = new UploadPicture(DBRoulette.this, mApi, PHOTO_DIR, file);
					upload.execute();
				}


			} else {
				Log.w(TAG, "Unknown Activity Result from mediaImport: "
						+ resultCode);
			}
		}
	}

	private void logOut() {
		// Remove credentials from the session
		mApi.getSession().unlink();

		// Clear our stored keys
		clearKeys();
		// Change UI state to display logged out version
		setLoggedIn(false);
	}

	/**
	 * Convenience function to change UI state based on being logged in
	 */
	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
			mSubmit.setText("Unlink from Dropbox");
			mDisplay.setVisibility(View.VISIBLE);
		} else {
			mSubmit.setText("Link with Dropbox");
			mDisplay.setVisibility(View.GONE);
			mImage.setImageDrawable(null);
		}
	}

	private void checkAppKeySetup() {
		// Check to make sure that we have a valid app key
		if (APP_KEY.startsWith("CHANGE") ||
				APP_SECRET.startsWith("CHANGE")) {
			showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the DBRoulette ap before trying it.");
			finish();
			return;
		}

		// Check if the app has set up its manifest properly.
		Intent testIntent = new Intent(Intent.ACTION_VIEW);
		String scheme = "db-" + APP_KEY;
		String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
		testIntent.setData(Uri.parse(uri));
		PackageManager pm = getPackageManager();
		if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
			showToast("URL scheme in your app's " +
					"manifest is not set up correctly. You should have a " +
					"com.dropbox.client2.android.AuthActivity with the " +
					"scheme: " + scheme);
			finish();
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local
	 * store, rather than storing user name & password, and re-authenticating each
	 * time (which is not to be done, ever).
	 */
	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

		if (key.equals("oauth2:")) {
			// If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
			session.setOAuth2AccessToken(secret);
		} else {
			// Still support using old OAuth 1 tokens.
			session.setAccessTokenPair(new AccessTokenPair(key, secret));
		}
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a local
	 * store, rather than storing user name & password, and re-authenticating each
	 * time (which is not to be done, ever).
	 */
	private void storeAuth(AndroidAuthSession session) {
		// Store the OAuth 2 access token, if there is one.
		String oauth2AccessToken = session.getOAuth2AccessToken();
		if (oauth2AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, "oauth2:");
			edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
			edit.commit();
			return;
		}
		// Store the OAuth 1 access token, if there is one.  This is only necessary if
		// you're still using OAuth 1.
		AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
		if (oauth1AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
			edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
			edit.commit();
			return;
		}
	}

	private void clearKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		Editor edit = prefs.edit();
		edit.clear();
		edit.commit();
	}

	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

	/**
	 * Get a file path from a Uri. This will get the the path for Storage Access
	 * Framework Documents, as well as the _data field for the MediaStore and
	 * other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @author paulburke
	 */
	public static String getPath(final Context context, final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}

			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] {
						split[1]
				};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {

			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();

			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @param selection (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection,
			String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {
				column
		};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}


	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
}
