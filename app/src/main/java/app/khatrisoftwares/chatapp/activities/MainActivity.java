package app.khatrisoftwares.chatapp.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import app.khatrisoftwares.chatapp.R;

public class MainActivity extends AppCompatActivity {

    Animation leftToRightAnim, rightToLeftAnim, bottomToTopAnim;
    ImageView chatBubbleSent, chatBubbleReceived;
    TextView appNameTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

//        Animations
        leftToRightAnim = AnimationUtils.loadAnimation(this, R.anim.left_to_right_animation);
        rightToLeftAnim = AnimationUtils.loadAnimation(this, R.anim.right_to_left_animation);
        bottomToTopAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top_animation);

        chatBubbleReceived = findViewById(R.id.chatBubbleReceived);
        chatBubbleSent = findViewById(R.id.chatBubbleSent);
        appNameTv = findViewById(R.id.appNameTv);

        chatBubbleReceived.setAnimation(leftToRightAnim);
        chatBubbleSent.setAnimation(rightToLeftAnim);
        appNameTv.setAnimation(bottomToTopAnim);

        new Handler().postDelayed(() -> {
            Intent intent;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                intent = new Intent(MainActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                intent = new Intent(MainActivity.this, LoginActivity.class);
                Pair[] pairs = new Pair[2];
                pairs[0] = new Pair<View,String>(chatBubbleSent,"logo_image");
                pairs[1] = new Pair<View,String>(appNameTv,"logo_text");
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this,pairs);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent,options.toBundle());
            }
        }, 3000);
    }
}
