package com.oa.task2do;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Avishay on 18/02/14.
 */
public class AlertDialog extends DialogFragment {
    int selection=0;
    boolean editMode =false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());

        // set title
        alertDialogBuilder.setTitle("ALERT");

        // set dialog message
        alertDialogBuilder
                .setMessage("do you want to delete?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // Return input text to activity
                        selection=1;
                        Intent data = new Intent();
                        //data.putExtra("timePicker", timePicker);
                        data.putExtra("selection", selection);
                        DialogListener activity = (DialogListener) getActivity();
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!ALERT CLASS WORKS YES!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        activity.onFinishEditDialog(data);
                        editMode=true;
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        selection=0;
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!ALERT CLASS WORKS NO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        editMode=false;
                        dialog.cancel();
                    }
                });

        // create alert dialog
        android.app.AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        //alertDialog.show();
        //return choice;
        return alertDialog;
    }



}