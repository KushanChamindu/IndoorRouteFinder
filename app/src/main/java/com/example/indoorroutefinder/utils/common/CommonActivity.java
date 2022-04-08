package com.example.indoorroutefinder.utils.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.indoorroutefinder.MainActivity;

public class CommonActivity {
    private static AlertDialog.Builder builder;

    public static void initializeAlertBuilder(Context context) {
        builder = new AlertDialog.Builder(context);
    }

    public static void showDialog(String title, String message) {
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
