package cz.kcck.gatekeeper;


import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import cz.kcck.gatekeeper.util.SystemUiHider;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class Main extends Activity {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	private String imageURL;
	
	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	private ImageView image;
	private ImageButton settingsButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		image = (ImageView) findViewById(R.id.imageView1);
		settingsButton = (ImageButton)findViewById(R.id.settings_button);
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = image;

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		settingsButton.setOnTouchListener(
				mDelayHideTouchListener);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(Main.this, GateKeeperPreferenceActivity.class);
				startActivity(i);
			}
		});
	}
	

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		
	}


	Handler downloadHandler = new Handler();
	Runnable downloadRunnable = new Runnable() {
		@Override
		public void run() {
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.execute();
			delayedDownload(5000);
		}
	};
	
	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};
	
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};
	
	@Override
	public void onPause()
	{
		super.onPause();
		mHideHandler.removeCallbacks(mHideRunnable);
		downloadHandler.removeCallbacks(downloadRunnable);
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		imageURL=sharedPref.getString("editText_imageURL", null);
		// Trigger the initial download() shortly after the activity has been created
		delayedDownload(5000);
		
		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedDownload(int delayMillis) {
		downloadHandler.removeCallbacks(downloadRunnable);
		downloadHandler.postDelayed(downloadRunnable, delayMillis);
	}
	
	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	private class ImageLoader extends AsyncTask<Void, Void, Bitmap> {
	    @Override
	    protected Bitmap doInBackground(Void  ... params) {
	    	return downloadDeviceImage();
	    }

	    @Override
	    protected void onPostExecute(Bitmap bMap) {
	    	
	    	if(bMap != null)
	    	{
	    		image.setImageBitmap(bMap);
	    	}
	    }
	      
	    }
	    
	    
	    private Bitmap downloadDeviceImage()
		{
	    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = registerReceiver(null, ifilter);
			
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			float batteryPct = (level / (float)scale)*100;
			Bitmap bMap = null;
			
			try
			{
				URL url = null;
				url = new URL(imageURL+"?batteryPercentage="+(int)batteryPct+"&batteryLevel="+level+"&batteryScale="+scale);
				
			    HttpsURLConnection conn = null;
				conn = (HttpsURLConnection) url.openConnection();
				
	
			    // Create the SSL connection
			    SSLContext sc = null;
	
				sc = SSLContext.getInstance("SSL");
				
				sc.init(null, null, new java.security.SecureRandom());
	
			    conn.setSSLSocketFactory(sc.getSocketFactory());
	
			    // set Timeout and method
			    conn.setReadTimeout(7000);
			    conn.setConnectTimeout(7000);
			    conn.setDoInput(true);
				conn.setRequestMethod("GET");
		
			    InputStream is = null;
			   
		    
				conn.connect();
				is = conn.getInputStream();	
				
				BufferedInputStream buf = new BufferedInputStream(is);
					    
				bMap = BitmapFactory.decodeStream(buf);

				
	            if (is != null) {
	                is.close();
	            }
	            if (buf != null) {
	                
						buf.close();
					
	            }
		    
			} catch (Exception e) {
				Context context = getApplicationContext();
				Handler handler =  new Handler(context.getMainLooper());

			    handler.post( new DisplayExceptionRunnable(e.getMessage()));
				
				e.printStackTrace();
				return null;
			}
	        return bMap;


	    
	  }
	    
	    public class DisplayExceptionRunnable implements Runnable {
	    	  private String message;
	    	  public DisplayExceptionRunnable(String message) {
	    	    this.message = message;
	    	  }

	    	  public void run() {
	    		Context context = getApplicationContext();
				CharSequence text = message;
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
	    	  }
	    	}
	
	
}
