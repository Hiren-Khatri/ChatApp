package app.khatrisoftwares.chatapp.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import app.khatrisoftwares.chatapp.R;
import app.khatrisoftwares.chatapp.network.ApiClient;
import app.khatrisoftwares.chatapp.network.ApiService;
import app.khatrisoftwares.chatapp.utilities.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    private String meetingType = null;
    private Ringtone ringtone;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_incoming_invitation);

        ImageView imageMeetingType = findViewById(R.id.imageMeetingType);
         meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);

        if (meetingType != null){
            if (meetingType.equals("video")){
                imageMeetingType.setImageResource(R.drawable.ic_video);
            }else {
                imageMeetingType.setImageResource(R.drawable.ic_audio);
            }
        }

            TextView textFirstChar = findViewById(R.id.textFirstChar);
        TextView textUsername = findViewById(R.id.textUsername);
        TextView textEmail = findViewById(R.id.textEmail);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
         ringtone = RingtoneManager.getRingtone(getApplicationContext(),uri);
        if(!ringtone.isPlaying()){
            ringtone.play();
        }


        String firstName = getIntent().getStringExtra(Constants.KEY_FIRST_NAME);
        if (firstName != null){
            textFirstChar.setText(firstName.substring(0,1).toUpperCase());
        }

        textUsername.setText(String.format(
                "%s %s",
                firstName,
                getIntent().getStringExtra(Constants.KEY_LAST_NAME)
        ));

        textEmail.setText(getIntent().getStringExtra(Constants.KEY_EMAIL));

        ImageView imageAcceptInvitation = findViewById(R.id.imageAcceptInvitation);
        imageAcceptInvitation.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
            if(ringtone.isPlaying())
                ringtone.stop();
        });

        ImageView imageRejectInvitation = findViewById(R.id.imageRejectInvitation);
        imageRejectInvitation.setOnClickListener(v -> {
            sendInvitationResponse(
                    Constants.REMOTE_MSG_INVITATION_REJECTED,
                    getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
            );
            if(ringtone.isPlaying())
                ringtone.stop();
        });
    }

    private void sendInvitationResponse(String type,String receiverToken){
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE,Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE,type);

            body.put(Constants.REMOTE_MSG_DATA,data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

            sendRemoteMessage(body.toString(),type);

        }catch (Exception e){
            Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody,String type){
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeader(),remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()){
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)){

                        try {

                            URL serverURL = new URL("https://meet.jit.si");

                            JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                            builder .setServerURL(serverURL);
                            builder .setWelcomePageEnabled(false);
                            builder .setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));

                            if (meetingType.equals("audio")){
                                builder.setVideoMuted(true);
                            }

                            JitsiMeetActivity.launch(IncomingInvitationActivity.this,builder.build());
                            finish();

                        }catch (Exception e){
                            Toast.makeText(IncomingInvitationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }
                    if (type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)){
                        Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }else {
                    Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type!=null){
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)){
                    if(ringtone.isPlaying())
                        ringtone.stop();
                    Toast.makeText(context, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }
}