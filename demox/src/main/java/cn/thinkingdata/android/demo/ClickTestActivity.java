package cn.thinkingdata.android.demo;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import cn.thinkingdata.android.ThinkingDataTrackViewOnClick;

import java.util.ArrayList;
import java.util.Calendar;

public class ClickTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_click_test);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabHost tabHost = findViewById(R.id.tab_host);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("Button").setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("Widgets").setContent(R.id.tab2));
        tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("Others").setContent(R.id.tab3));

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            // tabId是newTabSpec第一个参数设置的tab页名，并不是layout里面的标识符id
            public void onTabChanged(String tabId) {
                if (tabId.equals("tab1")) {
                    Toast.makeText(ClickTestActivity.this, "Button Tab Selected", Toast.LENGTH_SHORT).show();
                }
                if (tabId.equals("tab2")) {
                    Toast.makeText(ClickTestActivity.this, "Widgets Tab Selected", Toast.LENGTH_SHORT).show();
                }
                if (tabId.equals("tab3")) {
                    Toast.makeText(ClickTestActivity.this, "Others Tab Selected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        RadioGroup radioGroup = findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                Toast.makeText(ClickTestActivity.this, "RadioGroup Checked: " + i, Toast.LENGTH_SHORT).show();
            }
        });

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        RatingBar ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {

            }
        });

        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] os = getResources().getStringArray(R.array.OS);
                Toast.makeText(ClickTestActivity.this, "Spinner Item Selected: " + os[i], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });

        CheckBox checkBox = findViewById(R.id.checkbox_button);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {


            }
        });

        Switch switchButton = findViewById(R.id.switch_button);
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.test_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_item1:
                Toast.makeText(ClickTestActivity.this, "Menu 1 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_item2:
                Toast.makeText(ClickTestActivity.this, "Menu 2 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_item3:
                Toast.makeText(ClickTestActivity.this, "Menu 3 selected", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void checkboxClicked(View view) {
        Toast.makeText(ClickTestActivity.this, "Checkbox Clicked", Toast.LENGTH_SHORT).show();
    }

    @ThinkingDataTrackViewOnClick
    public void switchClicked(View view) {
        Toast.makeText(ClickTestActivity.this, "SwitchButton Clicked", Toast.LENGTH_SHORT).show();
    }

    public void radioButtonClicked(View view) {
        Toast.makeText(ClickTestActivity.this, "RadioButton Clicked", Toast.LENGTH_SHORT).show();

    }

    @ThinkingDataTrackViewOnClick
    public void toggleButtonClicked(View view) {
        Toast.makeText(ClickTestActivity.this, "ToggleButton Clicked", Toast.LENGTH_SHORT).show();
    }

    public void imageViewClicked(View view) {
        Toast.makeText(ClickTestActivity.this, "ImageView Clicked", Toast.LENGTH_SHORT).show();
    }

    public void showTimePickerDialog(View view) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            Log.d("test", "" + hourOfDay + ", " + minute);
        }
    }

    public void showDatePickerDialog(View view) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, dayOfMonth);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

        }
    }

    public void showMultiChoiceDialog(View view) {
        DialogFragment newFragment = new MultiChoiceDialogFragment();
        newFragment.show(getSupportFragmentManager(), "multiChoice");
    }

    // Where we track the selected items
    final static ArrayList mSelectedItems = new ArrayList();

    public static class MultiChoiceDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Set the dialog title
            builder.setTitle(R.string.title_multi_choice)
                    // Specify the list array, the items to be selected by default (null for none),
                    // and the listener through which to receive callbacks when items are selected
                    .setMultiChoiceItems(R.array.languages, null,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which,
                                                    boolean isChecked) {
                                    if (isChecked) {
                                        // If the user checked the item, add it to the selected items
                                        mSelectedItems.add(which);
                                    } else if (mSelectedItems.contains(which)) {
                                        // Else, if the item is already in the array, remove it
                                        mSelectedItems.remove(Integer.valueOf(which));
                                    }
                                }
                            })
                    // Set the action buttons
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK, so save the mSelectedItems results somewhere
                            // or return them to the component that opened the dialog
                        }
                    });

            return builder.create();
        }
    }
}


