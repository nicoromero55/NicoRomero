package com.adedo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ADedo extends Activity {

    //Facebook
    CallbackManager callbackManager;
    GraphRequest request;
    private RelativeLayout fakeButton;
    private String radioChecked = "";
    private boolean inserted = false;
    private RadioGroup radioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        retrieveFromPref();
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_adedo);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fakeButton = (RelativeLayout) findViewById(R.id.fakeButton);
        fakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ADedo.this, "Seleccione si es chofer o pasajero", Toast.LENGTH_SHORT).show();
            }
        });

        if (inserted) {
            fakeButton.setVisibility(View.GONE);
            radioGroup.setVisibility(View.GONE);
        }

        initializeFacebook();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isLoggedIn()) {
            Intent i = new Intent(ADedo.this, Principal.class);
            SharedPreferences prefs = getSharedPreferences(Chofer.MY_PREFS_NAME, MODE_PRIVATE);

            String email = prefs.getString("email", "");
            String name = prefs.getString("name", "");
            String first_name = prefs.getString("first_name", "");
            String last_name = prefs.getString("last_name", "");
            if (!(email.isEmpty() || name.isEmpty() || first_name.isEmpty() || last_name.isEmpty())) {
                i.putExtra("email", email);
                i.putExtra("name", name);
                i.putExtra("first_name", first_name);
                i.putExtra("last_name", last_name);
                startActivity(i);
            }
        }
    }

    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    public void retrieveFromPref() {
        SharedPreferences prefs = getSharedPreferences(Chofer.MY_PREFS_NAME, MODE_PRIVATE);
        inserted = prefs.getBoolean("Inserted", false);
    }

    public void comenzar(View view) {
        Intent i = new Intent(ADedo.this, Principal.class);
        startActivity(i);
    }

    public void calificaciones(View view) {
        Intent i = new Intent(ADedo.this, Lista_calificaciones.class);
        startActivity(i);
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_chofer:
                if (checked)
                    radioChecked = "chofer";
                fakeButton.setVisibility(View.GONE);
                break;
            case R.id.radio_pasajero:
                if (checked)
                    radioChecked = "pasajero";
                fakeButton.setVisibility(View.GONE);
                break;
        }
    }

    private void initializeFacebook() {
        callbackManager = CallbackManager.Factory.create();


        final LoginButton loginButton = (LoginButton) findViewById(R.id.signin_button_fb);
        loginButton.setText("Login");
        loginButton.setReadPermissions(Arrays.asList("public_profile, email"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                // Application code
                                try {
                                    String email = (object.has("email")) ? object.getString("email") : "";
                                    String name = (object.has("name")) ? object.getString("name") : "";
                                    String first_name = (object.has("first_name")) ? object.getString("first_name") : "";
                                    String last_name = (object.has("last_name")) ? object.getString("last_name") : "";

                                    SharedPreferences settings = getSharedPreferences(Chofer.MY_PREFS_NAME, MODE_PRIVATE);
                                    SharedPreferences.Editor prefEditor = settings.edit();
                                    prefEditor.putString("email", email);
                                    prefEditor.putString("name", name);
                                    prefEditor.putString("first_name", first_name);
                                    prefEditor.putString("last_name", last_name);
                                    prefEditor.commit();

                                    Intent i = null;

                                    if (!inserted) {
                                        if (radioChecked == "chofer") {
                                            i = new Intent(ADedo.this, Chofer.class);
                                        } else {
                                            i = new Intent(ADedo.this, Pasajero.class);
                                        }
                                    } else {
                                        i = new Intent(ADedo.this, Principal.class);
                                    }

                                    i.putExtra("email", email);
                                    i.putExtra("name", name);
                                    i.putExtra("first_name", first_name);
                                    i.putExtra("last_name", last_name);
                                    startActivity(i);
                                    finish();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender, birthday,first_name,last_name");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {
                int i = 0;
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), "Error de conexión", Toast.LENGTH_SHORT);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        LoginManager.getInstance().logOut();
    }
}
