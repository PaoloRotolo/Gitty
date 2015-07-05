package com.github.paolorotolo.gitty_reporter;

import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import java.io.IOException;

public abstract class GittyReporter extends AppCompatActivity {

    private EditText bugTitleEditText;
    private EditText bugDescriptionEditText;
    private EditText deviceInfoEditText;
    private String deviceInfo;
    private String targetUser;
    private String targetRepository;
    private String gitUser;
    private String gitPassword;
    private String extraInfo;
    private String gitToken;
    private Boolean enableGitHubLogin = true;
    private Boolean enableGuestGitHubLogin = true;

    @Override
    final protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gitty_reporter_layout);

        // Get Device info and print them in EditText
        deviceInfoEditText = (EditText) findViewById(R.id.device_info);
        getDeviceInfo();
        deviceInfoEditText.setText(deviceInfo);

        init(savedInstanceState);

        final View nextFab = findViewById(R.id.fab_next);
        final View sendFab = findViewById(R.id.fab_send);

        if (!enableGitHubLogin){
            nextFab.setVisibility(View.INVISIBLE);
            sendFab.setVisibility(View.VISIBLE);
        }

        AppCompatCheckBox githubCheckbox = (AppCompatCheckBox) findViewById(R.id.github_checkbox);
        AppCompatButton registerButton = (AppCompatButton) findViewById(R.id.github_register);

        final EditText userName = (EditText) findViewById(R.id.login_username);
        final EditText userPassword = (EditText) findViewById(R.id.login_password);

        if (!enableGuestGitHubLogin){
            githubCheckbox.setChecked(false);
            githubCheckbox.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
        }

        githubCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked){
                            userName.setEnabled(false);
                            userName.setText("");
                            userPassword.setEnabled(false);
                            userPassword.setText("");
                        } else {
                            userName.setEnabled(true);
                            userPassword.setEnabled(true);
                        }
                    }
                }
        );
    }

    public void reportIssue (View v) {
        if (enableGitHubLogin) {
            final AppCompatCheckBox githubCheckbox = (AppCompatCheckBox) findViewById(R.id.github_checkbox);
            EditText userName = (EditText) findViewById(R.id.login_username);
            EditText userPassword = (EditText) findViewById(R.id.login_password);

            if (!githubCheckbox.isChecked()){
                if (validateGitHubLogin()){
                    this.gitUser = userName.getText().toString();
                    this.gitPassword = userPassword.getText().toString();
                    sendBugReport();
                }
            } else {
                this.gitUser = "";
                this.gitPassword = "";
                sendBugReport();
            }
        } else {
            if (validateBugReport()) {
                this.gitUser = "";
                this.gitPassword = "";
                sendBugReport();
            }
        }
    }

    private boolean validateGitHubLogin(){
        EditText userName = (EditText) findViewById(R.id.login_username);
        EditText userPassword = (EditText) findViewById(R.id.login_password);

        if (userName.getText().toString().equals("")){
            showToast("Please enter a vaild username");

            return false;
        } else if (userPassword.getText().toString().equals("")) {
            showToast("Please enter a vaild password");
            return false;
        } else {
            return true;
        }
    }

    private boolean validateBugReport(){
        bugTitleEditText = (EditText) findViewById(R.id.bug_title);
        bugDescriptionEditText = (EditText) findViewById(R.id.bug_description);

        if (bugTitleEditText.getText().toString().equals("")){
            showToast("Please enter a valid title");
            return false;
        } else if (bugDescriptionEditText.getText().toString().equals("")){
            showToast("Please describe your issue");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void sendBugReport(){
        bugTitleEditText = (EditText) findViewById(R.id.bug_title);
        bugDescriptionEditText = (EditText) findViewById(R.id.bug_description);
        final String bugTitle = bugTitleEditText.getText().toString();
        final String bugDescription = bugDescriptionEditText.getText().toString();

        if (extraInfo == null) {
            this.extraInfo = "Nothing to show.";
        } else if (!enableGitHubLogin){
            this.gitUser = "";
            this.gitPassword = "";
        }

        new reportIssue(GittyReporter.this).execute(gitUser, gitPassword, bugTitle, bugDescription, deviceInfo, targetUser, targetRepository, extraInfo, gitToken, enableGitHubLogin.toString());
    }

    public void showLoginPage (View v) {
        if (validateBugReport()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                animateLoginPage();
            } else {
                View loginView = findViewById(R.id.loginFrame);
                View nextFab = findViewById(R.id.fab_next);
                View sendFab = findViewById(R.id.fab_send);

                loginView.setVisibility(View.VISIBLE);
                nextFab.setVisibility(View.INVISIBLE);
                sendFab.setVisibility(View.VISIBLE);
            }
        }
    }

    private void animateLoginPage(){
        final View colorView = findViewById(R.id.material_ripple);
        final View loginView = findViewById(R.id.loginFrame);
        final View nextFab = findViewById(R.id.fab_next);
        final View sendFab = findViewById(R.id.fab_send);

        final AlphaAnimation fadeOutColorAnim = new AlphaAnimation(1.0f, 0.0f);
        fadeOutColorAnim.setDuration(400);
        fadeOutColorAnim.setInterpolator(new AccelerateInterpolator());
        final AlphaAnimation fadeOutFabAnim = new AlphaAnimation(1.0f, 0.0f);
        fadeOutFabAnim.setDuration(400);
        fadeOutFabAnim.setInterpolator(new AccelerateInterpolator());
        final AlphaAnimation fadeInAnim = new AlphaAnimation(0.0f, 1.0f);
        fadeInAnim.setDuration(400);
        fadeInAnim.setInterpolator(new AccelerateInterpolator());

        fadeOutColorAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                loginView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                colorView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeOutFabAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                sendFab.setVisibility(View.VISIBLE);
                sendFab.startAnimation(fadeInAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        int cx = (colorView.getRight());
        int cy = (colorView.getBottom());
        int finalRadius = Math.max(colorView.getWidth(), colorView.getHeight());

        Animator rippleAnim =
                ViewAnimationUtils.createCircularReveal(colorView, cx, cy, 0, finalRadius);

        rippleAnim.setInterpolator(new AccelerateInterpolator());
        rippleAnim.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                colorView.startAnimation(fadeOutColorAnim);
                nextFab.startAnimation(fadeOutFabAnim);
                nextFab.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
            }
        });

        colorView.setVisibility(View.VISIBLE);
        rippleAnim.start();
    }

    public void setTargetRepository(String user, String repository){
        this.targetUser = user;
        this.targetRepository = repository;
    }

    public void setGuestOAuth2Token(String token){
        this.gitToken = token;
    }

    public void setExtraInfo(String info){
        this.extraInfo = info;
    }

    public void enableUserGitHubLogin(boolean enableLogin){
        this.enableGitHubLogin = enableLogin;
    }

    public void enableGuestGitHubLogin(boolean enableGuest){
        this.enableGuestGitHubLogin = enableGuest;
    }

    @Override
    public void onBackPressed() {
        View loginView = findViewById(R.id.loginFrame);
        if (loginView.getVisibility() == View.VISIBLE){
            View nextFab = findViewById(R.id.fab_next);
            View sendFab = findViewById(R.id.fab_send);

            loginView.setVisibility(View.INVISIBLE);
            nextFab.setVisibility(View.VISIBLE);
            sendFab.setVisibility(View.INVISIBLE);
        } else {
            finish();
        }
    }

    public void openGitHubRegisterPage(View v){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/join"));
        startActivity(browserIntent);
    }

    private void getDeviceInfo() {
        try {
            String s = "Debug-infos:";
            s += "\n OS Version: "      + System.getProperty("os.version")      + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
            s += "\n OS API Level: "    + android.os.Build.VERSION.SDK_INT;
            s += "\n Device: "          + android.os.Build.DEVICE;
            s += "\n Model (and Product): " + android.os.Build.MODEL            + " ("+ android.os.Build.PRODUCT + ")";

            s += "\n RELEASE: "         + android.os.Build.VERSION.RELEASE;
            s += "\n BRAND: "           + android.os.Build.BRAND;
            s += "\n DISPLAY: "         + android.os.Build.DISPLAY;
            s += "\n CPU_ABI: "         + android.os.Build.CPU_ABI;
            s += "\n CPU_ABI2: "        + android.os.Build.CPU_ABI2;
            s += "\n HARDWARE: "        + android.os.Build.HARDWARE;
            s += "\n Build ID: "        + android.os.Build.ID;
            s += "\n MANUFACTURER: "    + android.os.Build.MANUFACTURER;
            s += "\n SERIAL: "          + android.os.Build.SERIAL;
            s += "\n USER: "            + android.os.Build.USER;
            s += "\n HOST: "            + android.os.Build.HOST;

            deviceInfo = s;
        } catch (Exception e) {
            Log.e("android-issue-github", "Error getting Device INFO");
        }
    }

    public abstract void init(@Nullable Bundle savedInstanceState);
}