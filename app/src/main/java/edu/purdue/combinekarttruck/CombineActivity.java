package edu.purdue.combinekarttruck;

/*
 * The activity for combine.
 * 
 * @author: Yaguang Zhang
 * 
 */

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CombineActivity extends BasicGpsLoggingActivity {

	private boolean combineIsUnloading = false;
	private boolean combineDoneUnloading = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		actionBarActivityOnCreate(savedInstanceState);

		setContentView(R.layout.activity_combine);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		try {
			getMLogState().write("% Combine state: not unloading (default)\n");
		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_log_file_create_error));
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("CombineOnStartWrite", e.toString());
		}
	}

	@Override
	public String getLoginType() {
		return getString(R.string.vehicle_combine);
	}

	@Override
	public String getPartialLogFilePath() {
		return getString(R.string.gps_log_file_path_combine);
	}

	@Override
	public void setBackgroundColor() {
		findViewById(R.id.textViewVehicleTypeLabel).getRootView()
				.setBackgroundColor(
						getResources().getColor(
								MainLoginActivity.COLOR_ACTIVITY_COMBINE));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.kart, menu);
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
			View rootView = inflater.inflate(R.layout.fragment_combine,
					container, false);
			return rootView;
		}
	}

	public void changeCombineUnloadingState(View view) {
		Button changeStateButton = (Button) view;

		// Change the text and color (which will be effective) of the button and
		// record the change of
		// state into log file.
		if (combineIsUnloading) {
			// From "unloading" to "not unloading".
			changeStateButton.setText(getString(R.string.kart_not_unloading));
			changeStateButton.setBackgroundColor(getResources().getColor(
					R.color.kart_not_unloading));

			buildAlertMessageDoneUnloading(this);

			long date = System.currentTimeMillis();
			try {
				if (combineDoneUnloading) {
					getMLogState()
							.write(super.getFormatterClock().format(date)
									+ " ("
									+ date
									+ ") Combine state changes to: not unloading");
				} else {
					getMLogState()
							.write(super.getFormatterClock().format(date)
									+ " ("
									+ date
									+ ") Combine state changes to: not unloading");
				}
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.gps_log_file_create_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("CombineChangeStateWrite", e.toString());
			}

		} else {
			// From "not unloading" to "unloading".
			changeStateButton.setText(getString(R.string.kart_unloading));
			changeStateButton.setBackgroundColor(getResources().getColor(
					R.color.kart_unloading));

			long date = System.currentTimeMillis();
			try {
				getMLogState().write(
						super.getFormatterClock().format(date) + " (" + date
								+ ") Combine state changes to: unloading\n");
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.gps_log_file_create_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("CombineChangeStateWrite", e.toString());
			}
		}

		changeStateButton.invalidate();
		// Change the state flag.
		combineIsUnloading = !combineIsUnloading;
	}

	private void buildAlertMessageDoneUnloading(final Activity activity) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(getString(R.string.kart_done_unloading))
				.setCancelable(false)
				.setPositiveButton(getString(R.string.button_yes),
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();

								try {
									getMLogState().write(" (all unloaded)\n");
								} catch (IOException e) {
									Log.e("CombineChangeStateWrite",
											e.toString());
								}
							}
						})
				.setNegativeButton(getString(R.string.button_no),
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();

								try {
									getMLogState().write(
											" (not all unloaded)\n");
								} catch (IOException e) {
									Log.e("CombineChangeStateWrite",
											e.toString());
								}
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

}
