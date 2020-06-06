package app.khatrisoftwares.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {
    SwitchCompat postSwitch;

    SharedPreferences sp;
    SharedPreferences.Editor editor;

    //constant of topic
    private static final String TOPIC_POST_NOTIFICATION = "POST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        postSwitch = findViewById(R.id.postSwitch);

        //init sp
        sp = getSharedPreferences("Notification_SP", MODE_PRIVATE);
        boolean isPostEnabled = sp.getBoolean("" + TOPIC_POST_NOTIFICATION, false);

        // if enabled check switch ,other wise by default uncheck
        if (isPostEnabled){
            postSwitch.setChecked(true);
        } else {
            postSwitch.setChecked(false);
        }

        //  chane listener
        postSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //edit switch state
                editor = sp.edit();
                editor.putBoolean(""+TOPIC_POST_NOTIFICATION,isChecked);
                editor.apply();


                if (isChecked) {
                    subscribePostNotification();//call to subscribe
                } else {
                    unsubscribePostNotification();//call to unsubscribe
                }
            }
        });
    }

    private void unsubscribePostNotification() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(""+TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "You will not receive post notifications";
                        if (!task.isSuccessful()){
                            msg = "UnSubscription failed";
                        }
                        Snackbar.make(findViewById(android.R.id.content),msg,Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimary)).show();
                    }
                });
    }

    private void subscribePostNotification() {
        FirebaseMessaging.getInstance().subscribeToTopic(""+TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "You will receive post notifications";
                        if (!task.isSuccessful()){
                            msg = "Subscription failed";
                        }
                        Snackbar.make(findViewById(android.R.id.content),msg,Snackbar.LENGTH_SHORT).setBackgroundTint(getColor(R.color.colorPrimary)).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }
}
