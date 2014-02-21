package com.oa.task2do;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by Avishay on 18/02/14.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);


//        // Create a new instance of DatePickerDialog and return it
//        return new DatePickerDialog(getActivity(), this, year, month, day).getDatePicker().setMinDate(c.getTimeInMillis())

        DatePickerDialog d = new DatePickerDialog(getActivity(), this, year, month, day);
        DatePicker dp = d.getDatePicker();
        dp.setMinDate(c.getTimeInMillis());
        return d;
    }

    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
        System.out.println("****************************************");
        System.out.println(day+":"+month+1+":"+year);
        System.out.println("****************************************");

        // Return input text to activity
        Intent data = new Intent();
        //---set the data to pass back---
        data.putExtra("year", year);
        data.putExtra("month", month+1);
        data.putExtra("day", day);
        DialogListener activity = (DialogListener) getActivity();
        activity.onFinishEditDialog(data);
        //---closes the activity---
        this.dismiss();
    }
}