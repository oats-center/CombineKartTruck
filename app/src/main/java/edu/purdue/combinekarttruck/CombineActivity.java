package edu.purdue.combinekarttruck;

/*
 * The activity for combine.
 * 
 * @author: Yaguang Zhang
 * 
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import edu.purdue.combinekarttruck.utils.Utils;

public class CombineActivity extends BasicLoggingActivity {

	private boolean combineIsUnloading = false;
	private boolean combineDoneUnloading = false;

	@Override
	public String getPartialLogFilePath() {
		return this.getSharedPref().getString(Utils.SAVED_FOLDER_PATH,
				null) + getString(R.string.gps_log_file_path_combine);
	}

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

		if(!getLogStateFlag()) {
			Button stateButton = (Button) findViewById(R.id.ButtonChangeKartUnloadingState);
			stateButton.setVisibility(View.GONE);
			stateButton.setOnClickListener(null);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(!getLogStateFlag()) {
			LogFileWrite(getLogStateFlag(), getmLogFileState(),
					"% Combine state: not unloading (default)\n",
					"CombineOnStartWrite");
		}
	}

	@Override
	public String getLoginType() {
		return getString(R.string.vehicle_combine);
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
			String string;

			if (combineDoneUnloading) {
				string = super.getFormatterClock().format(date)
						+ " ("
						+ date
						+ ") Combine state changes to: not unloading";
			} else {
				string = super.getFormatterClock().format(date)
						+ " ("
						+ date
						+ ") Combine state changes to: not unloading";
			}

			LogFileWrite(getLogStateFlag(), getmLogFileState(),
					string, "CombineChangeStateWrite");


		} else {
			// From "not unloading" to "unloading".
			changeStateButton.setText(getString(R.string.kart_unloading));
			changeStateButton.setBackgroundColor(getResources().getColor(
					R.color.kart_unloading));

			long date = System.currentTimeMillis();

			LogFileWrite(getLogStateFlag(), getmLogFileState(),
					super.getFormatterClock().format(date) + " (" + date
							+ ") Combine state changes to: unloading\n",
					"CombineChangeStateWrite");
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

								LogFileWrite(getLogStateFlag(), getmLogFileState(),
										" (all unloaded)\n",
										"CombineChangeStateWrite");
							}
						})
				.setNegativeButton(getString(R.string.button_no),
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
												final int id) {
								dialog.cancel();

								LogFileWrite(getLogStateFlag(), getmLogFileState(),
										" (not all unloaded)\n",
										"CombineChangeStateWrite");
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
