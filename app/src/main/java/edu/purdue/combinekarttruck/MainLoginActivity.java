package edu.purdue.combinekarttruck;

/*
 * Login activity for vehicle information collection.
 * 
 * @author: Yaguang Zhang
 */

import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.util.Arrays;
import java.util.Locale;

import edu.purdue.combinekarttruck.utils.Utils;

public class MainLoginActivity extends ActionBarActivity {

	// Collected vehicle info.
	private String loginType, loginId;
	private String[] registeredIdStrings;
	static final String CURRENT_LOGIN_TYPE = "currentLoginType";
	static final String REGISTERED_IDS = "registeredIdStrings";

	// Preference file used to store the info.
	private SharedPreferences sharedPref;

	// Background colors. Set them for other activities.
	static final int COLOR_BASIC_GPS_LOGGING = color.white;
	static final int COLOR_ACTIVITY_COMBINE = 17170456; // android.R.color.holo_orange_light;
	static final int COLOR_ACTIVITY_KART = 17170452; // android.R.color.holo_green_light;
	static final int COLOR_ACTIVITY_TRUCK = 17170432; // android.R.color.darker_gray;

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save the user's current setting.
		savedInstanceState.putString(CURRENT_LOGIN_TYPE, loginType);
		savedInstanceState.putCharSequenceArray(REGISTERED_IDS,
				registeredIdStrings);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_login);

		if (savedInstanceState != null) {
			loginType = savedInstanceState.getString(CURRENT_LOGIN_TYPE);
			registeredIdStrings = (String[]) savedInstanceState
					.getCharSequenceArray(REGISTERED_IDS);
		} else {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			Utils.toastStringTextAtCenterWithLargerSize(this, "Initializing...");
			// Get the permission to store data / GPS / Cell, etc.
			Utils.verifyPermissions(this);
		}

		// Check whether the GPS service is on.
		final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps(this);
		}

		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_warning_may_not_working));

		}

		Utils.initDevDepFolderPath(this,
				this.getSharedPreferences(
						getString(R.string.shared_preference_file_key),
						Context.MODE_PRIVATE
				)
		);
	}

	private void buildAlertMessageNoGps(final Activity activity) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.gps_please_enable_it))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.button_ok),
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								startActivity(new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							}
						})
				.setNegativeButton(getString(R.string.button_cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();
								toastStringTextAtCenterWithLargerSize(
										activity,
										activity.getString(R.string.gps_warning_may_not_working));
							}
						});
		final AlertDialog alert = builder.create();
		alert.show();

		TextView textView = (TextView) alert.findViewById(android.R.id.message);
		textView.setTextSize(30);
		Button buttonNeg = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
		buttonNeg.setTextSize(25);
		Button buttonPos = alert.getButton(DialogInterface.BUTTON_POSITIVE);
		buttonPos.setTextSize(25);
	}

	@Override
	protected void onResume() {
		super.onResume();

		/**
		 * Initialization.
		 */

		// Set the sharedPref if we haven't done so.
		if (sharedPref == null) {
			sharedPref = this.getSharedPreferences(
					getString(R.string.shared_preference_file_key),
					Context.MODE_PRIVATE);
		}

		// Load the history record if it's initializing.
		if (loginType == null) {
			loginType = sharedPref.getString(
					getString(R.string.saved_last_type), null);

			loginId = sharedPref.getString(getString(R.string.saved_last_id),
					null);

			// Get registered IDs in the sharedPref if necessary.
			if (registeredIdStrings == null) {
				registeredIdStrings = loadArray(REGISTERED_IDS, this);
			}
		}

		autoFillVehicleInfo();

//		// Connect to the access point specified in Utils if necessary.
//		Utils.reconnectToAccessPoint(Utils.getHostSsid(), Utils.getHostPasswordD(),
//				Utils.isLockToHostSsid(), this);

		// Show the setting info DEBUG_FLAG and GPS_ONLY_FOR_LOC
		TextView textVewCurVersion = (TextView) findViewById(R.id.textViewCurVersion);
		String curVersion = "";
		if(BasicLoggingActivity.getDebugFlag()) {
			curVersion = curVersion + " DebugMod";
		}
		if(!BasicLoggingActivity.getGpsOnlyForLoc()) {
			curVersion = curVersion + " FusedLoc";
		}
		textVewCurVersion.setText(curVersion);
	}

	public void autoFillVehicleInfo() {
		/**
		 * Help the user to finish vehicle info collection process according to
		 * the history record.
		 */
		// Set ID in the editText according to loginId when necessary.
		if (loginId != null) {
			EditText editText = (EditText) findViewById(R.id.editTextLoginVehicleId);
			editText.setText(loginId);
		}

		// Set the background color according to loginType.
		RadioButton checkedRadioButton = null;

		if (loginType != null) {
			if (loginType.equals("C")) {
				checkedRadioButton = (RadioButton) findViewById(R.id.radioButtonCombine);
			} else if (loginType.equals("K")) {
				checkedRadioButton = (RadioButton) findViewById(R.id.radioButtonKart);
			} else if (loginType.equals("T")) {
				checkedRadioButton = (RadioButton) findViewById(R.id.radioButtonTruck);
			}
		}

		if (checkedRadioButton != null) {
			checkedRadioButton.setChecked(true);
			changeBackgroudColor(checkedRadioButton, loginType);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity_login, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_login,
					container, false);
			return rootView;
		}
	}

	/**
	 * Called when the user clicks the radio buttons.
	 **/

	public void onVehicleTypeRadioButtonClicked(View view) {
		boolean checked = ((RadioButton) view).isChecked();

		if (checked) {
			switch (view.getId()) {
			case R.id.radioButtonCombine:
				loginType = "C";
				break;
			case R.id.radioButtonKart:
				loginType = "K";
				break;
			case R.id.radioButtonTruck:
				loginType = "T";
				break;

			}

			changeBackgroudColor(view, loginType);
		}
	}

	private void changeBackgroudColor(View view, String loginTypew) {
		View root = view.getRootView();

		if (loginType.equals("C")) {
			root.setBackgroundColor(getResources().getColor(
					COLOR_ACTIVITY_COMBINE));
		} else if (loginType.equals("K")) {
			root.setBackgroundColor(getResources()
					.getColor(COLOR_ACTIVITY_KART));
		} else if (loginType.equals("T")) {
			root.setBackgroundColor(getResources().getColor(
					COLOR_ACTIVITY_TRUCK));
		}
	}

	/**
	 * Called when the user clicks the registration history button.
	 **/
	public void pickFromList(View view) {

		alertDialogForPickingRegisteredIds(this, registeredIdStrings);

	}

	/**
	 * Called when the user clicks the OK button.
	 **/
	public void logIn(View view) {
		/**
		 * Store the vehicle type and ID.
		 * 
		 * First check whether they are valid.
		 */

		// Has the user complete everything?
		if (loginType == null) {
			toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.toast_type_missing));
			return;
		}

		EditText editText = (EditText) findViewById(R.id.editTextLoginVehicleId);
		loginId = editText.getText().toString();

		if (loginId.toLowerCase(Locale.getDefault()).equals("yaguang")) {
			Toast.makeText(this,
					"Hi, I'm the author. \nSo try another ID!\n=P",
					Toast.LENGTH_LONG).show();
			loginId = "";
			editText.setText(loginId);
		} else if (loginId.toLowerCase(Locale.getDefault()).equals("krogmeier")) {
			Toast.makeText(this,
					"Hi, I'm the professor. \nSo try another ID!\n:)",
					Toast.LENGTH_LONG).show();
			loginId = "";
			editText.setText(loginId);
		}

		if (loginId.equals("")) {
			toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.toast_id_missing));
			return;
		}

		// Also check the registration info of the vehicle with ID loginId. Is
		// the new collected info the same as the record?
		SharedPreferences.Editor editor = sharedPref.edit();
		String registeredType = sharedPref.getString(loginId, null);

		if (registeredType != null) {
			// This ID has already been registered...
			if (!registeredType.equals(loginType)) {
				// But the record is different.
				String stringRegisteredType;

				if (registeredType.equals("C")) {
					stringRegisteredType = getString(R.string.vehicle_combine);
				} else if (registeredType.equals("K")) {
					stringRegisteredType = getString(R.string.vehicle_kart);
				} else if (registeredType.equals("T")) {
					stringRegisteredType = getString(R.string.vehicle_truck);
				} else {
					toastStringTextAtCenterWithLargerSize(this,
							getString(R.string.warning_toast_unknown_vehicle));
					return;
				}

				toastStringTextAtCenterWithLargerSize(this, String.format(
						getString(R.string.toast_already_registered),
						stringRegisteredType));
				return;
			} else {
				// And the record agrees with it. There should be record files
				// for this vehicle.

			}
		} else {
			// This is a new type & ID combination, so we need to register it.
			editor.putString(loginId, loginType);

			System.err.println(registeredIdStrings);
			String[] newRegisterdIds = new String[registeredIdStrings.length + 1];
			System.arraycopy(registeredIdStrings, 0, newRegisterdIds, 0,
					registeredIdStrings.length);
			newRegisterdIds[registeredIdStrings.length] = loginId;
			registeredIdStrings = newRegisterdIds;
			Arrays.sort(registeredIdStrings);
			saveArray(registeredIdStrings, REGISTERED_IDS, this);
		}

		/**
		 * Then store them in SharedPreferences. Also store the ID&Type
		 * combination and registerdIds in alphabetic order to finish the
		 * registration.
		 */
		editor.putString(getString(R.string.saved_last_type), loginType);
		editor.putString(getString(R.string.saved_last_id), loginId);
		editor.commit();

		/**
		 * Change to the corresponding activity according to the vehicle type.
		 */
		Intent intent = null;
		if (loginType.equals("C")) {
			intent = new Intent(this, CombineActivity.class);
		} else if (loginType.equals("K")) {
			intent = new Intent(this, KartActivity.class);
		} else if (loginType.equals("T")) {
			intent = new Intent(this, TruckActivity.class);
		}
		startActivity(intent);
	}

	/**
	 * The function to show message using a modified toast (Centered at the
	 * screen with larger font size).
	 * 
	 * @param stringText
	 */
	public static void toastStringTextAtCenterWithLargerSize(Activity activity,
			String stringText) {

		Context context = activity.getApplicationContext();

		CharSequence text = stringText;
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);

		LinearLayout linearLayout = null;
		linearLayout = (LinearLayout) toast.getView();
		View child = linearLayout.getChildAt(0);
		TextView messageTextView = null;
		messageTextView = (TextView) child;
		messageTextView.setTextSize(30);

		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	/**
	 * Used for pop out dialog for picking history vehicles IDs.
	 */
	public void alertDialogForPickingRegisteredIds(final Activity activity,
			final String[] registeredIdStrings) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.pick_registered_ids).setItems(
				registeredIdStrings, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						loginId = registeredIdStrings[which];
						loginType = activity.getSharedPreferences(
								getString(R.string.shared_preference_file_key),
								Context.MODE_PRIVATE).getString(loginId, null);
						autoFillVehicleInfo();
					}
				});

		final AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Used for storing registered IDs in sharedPref.
	 * 
	 * Ref:
	 * http://stackoverflow.com/questions/12350800/android-how-to-store-array
	 * -of-strings-in-sharedpreferences-for-android
	 */

	public boolean saveArray(String[] array, String arrayName, Context mContext) {
		SharedPreferences prefs = mContext.getSharedPreferences(
				getString(R.string.shared_preference_file_key),
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(arrayName + "_size", array.length);
		for (int i = 0; i < array.length; i++)
			editor.putString(arrayName + "_" + i, array[i]);
		return editor.commit();
	}

	public String[] loadArray(String arrayName, Context mContext) {
		SharedPreferences prefs = mContext.getSharedPreferences(
				getString(R.string.shared_preference_file_key),
				Context.MODE_PRIVATE);
		int size = prefs.getInt(arrayName + "_size", 0);
		String array[] = new String[size];
		for (int i = 0; i < size; i++)
			array[i] = prefs.getString(arrayName + "_" + i, null);
		return array;
	}
}
