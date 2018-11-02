package com.example.hyojin.myrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.hyojin.myrecorder.login.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.example.hyojin.myrecorder.AppController.TAG;

public class MainActivity extends Activity {

    private Button btnLogin;
    private Button btnStreaming;
    private Button btnRecording;
    private Button btnSetting;
    private Button btnWatching;
    private Button btnLinkToRegister;
    final Context c = this;

    private boolean LOGIN_STATE=false;

    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, "onCreate: Already Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "onCreate: Not Granted. Permission Requested", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }


        //Button Setting
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnStreaming =(Button) findViewById(R.id.btnStreaming);
        btnRecording =(Button) findViewById(R.id.btnRecording);
        btnSetting=(Button) findViewById(R.id.btnSetting);
        btnWatching=(Button) findViewById(R.id.btnWatching);
        btnLinkToRegister=(Button) findViewById(R.id.btnLinkToRegisterScreen);

        // Session manager
        session = new SessionManager(getApplicationContext());

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            btnLinkToRegister.setVisibility(View.GONE);
            btnLogin.setText("LOGOUT");
            LOGIN_STATE=true;
        }


        // Link to Register Screen Button Click Event
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        //Recording button Click Event
        btnRecording.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RecordingActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        //////// Check Login, if not move to Login Activity////////
        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if(!LOGIN_STATE) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    //finish();
                }
                else{
                    logoutUser();
                }

            }

        });
        //Watching button Click Event
        btnWatching.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if(!LOGIN_STATE) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    //finish();
                }
                else{
                    LayoutInflater layoutInflaterAndroid = LayoutInflater.from(c);
                    View mView = layoutInflaterAndroid.inflate(R.layout.dialog_watching, null);
                    AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(c);

                    alertDialogBuilderUserInput.setView(mView);
                    final EditText streamerID = (EditText) mView.findViewById(R.id.userInputDialog);

                    alertDialogBuilderUserInput.setCancelable(false)
                            .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogBox, int id) {

                                    getStreamInfo(streamerID.getText().toString());
                                    // ToDo get user input here

                                }
                            }).setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogBox, int id) {
                                            dialogBox.cancel();
                                        }
                                    });

                    AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
                    alertDialogAndroid.show();
                }
            }
        });

        //Streaming button Click Event
        btnStreaming.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if(!LOGIN_STATE) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    //finish();
                }
                else{
                    Intent intent = new Intent(getApplicationContext(), Streaming2Activity.class);
                    startActivity(intent);
                }
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if(!LOGIN_STATE) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    //finish();
                }
                else{
                    Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
                    startActivity(intent);
                }
            }
        });


    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);
        db.deleteUsers();
        btnLinkToRegister.setVisibility(View.VISIBLE);
        btnLogin.setText("LOGIN");
        LOGIN_STATE=false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, "onResume: Granted", Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Streaming information and password check
     * */
    private void getStreamInfo(final String streamer) {
        // Tag used to cancel the request
        String tag_string_req = "req_getStreamInfo";
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_GETSTREAM, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Streaming Info Getting Response: " + response.toString());

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        String private_on = jObj.getString("private_on");
                        final String password = jObj.getString("password");

                        if(private_on.equals("false")) {
                            Toast.makeText(getApplicationContext(), "정보확인!!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getApplicationContext(), WatchingActivity.class);
                            intent.putExtra("streamer", streamer);
                            startActivity(intent);
                        }
                        else{
                            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(c);
                            View mView = layoutInflaterAndroid.inflate(R.layout.dialog_watching, null);
                            AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(c);
                            alertDialogBuilderUserInput.setView(mView);
                            final EditText pwd = (EditText) mView.findViewById(R.id.userInputDialog);
                            pwd.setHint("input password");

                            alertDialogBuilderUserInput.setCancelable(false)
                                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogBox, int id) {
                                            // ToDo get user input here
                                            if(pwd.getText().toString().equals(password)){
                                                Toast.makeText(getApplicationContext(), "정보확인!!", Toast.LENGTH_LONG).show();
                                                Intent intent = new Intent(getApplicationContext(), WatchingActivity.class);
                                                intent.putExtra("streamer", streamer);
                                                startActivity(intent);
                                            }
                                            else
                                                Toast.makeText(getApplicationContext(), "비밀번호틀림", Toast.LENGTH_LONG).show();

                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogBox, int id) {
                                            dialogBox.cancel();
                                        }
                                    });
                            AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
                            alertDialogAndroid.show();

                        }

                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String,String> params = new HashMap<String,String>();
                params.put("streamer", streamer);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

}
