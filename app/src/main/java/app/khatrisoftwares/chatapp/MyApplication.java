package app.khatrisoftwares.chatapp;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //enables firebase offline
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
