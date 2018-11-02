package com.example.hyojin.myrecorder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

public class RegisterActivity extends Activity {
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private Button btnRegister;
    private Button btnLinkToLogin;
    private Button btnSendauth;
    private Button btnChkauth;
    private Button btnOnly;
    private EditText inputAuthText;
    private EditText inputFullName;
    private EditText inputEmail;
    private EditText inputPassword;
    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputFullName = (EditText) findViewById(R.id.name);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        inputAuthText = (EditText) findViewById(R.id.authtext);
        btnSendauth =(Button)findViewById(R.id.sendauth);
        btnChkauth =(Button)findViewById(R.id.authchk);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnLinkToLogin = (Button) findViewById(R.id.btnLinkToLoginScreen);
        btnOnly = (Button) findViewById(R.id.btnOnly);

        // Progress dialog
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Session manager
        session = new SessionManager(getApplicationContext());

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());
        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        btnSendauth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                if (!email.isEmpty()) {
                    sendAuthEmail(email);
                }else {
                    Toast.makeText(getApplicationContext(), "이메일을 입력해주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });
        btnOnly.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String nickname = inputFullName.getText().toString().trim();
                if (!nickname.isEmpty()) {
                    checkOnlyName(nickname);
                }else {
                    Toast.makeText(getApplicationContext(), "닉네임을 입력해주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnChkauth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String authText = inputAuthText.getText().toString().trim();
                if (!authText.isEmpty()) {
                    SharedPreferences sp = getSharedPreferences(email, MODE_PRIVATE);
                    String salt = sp.getString("salt",null);
                    if(salt==null){
                        Toast.makeText(getApplicationContext(), "인증버튼을 먼저 눌러주세요.", Toast.LENGTH_LONG).show();
                    }
                    else {
                        checkText(authText,salt,email);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "인증문자를 입력해주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Register Button Click event
        btnRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String name = inputFullName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                SharedPreferences sp = getSharedPreferences(email, MODE_PRIVATE);
                int auth = sp.getInt("auth", 0);
                SharedPreferences sp2 = getSharedPreferences(name, MODE_PRIVATE);
                int onlyName = sp2.getInt("only", 0);

                if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                    if(auth==1 && onlyName==1) {
                        registerUser(name, email, password);
                    }
                    else if(onlyName==0){
                        Toast.makeText(getApplicationContext(), "이름 중복확인을 해주세요", Toast.LENGTH_LONG).show();
                    }
                    else if(auth==0){
                        Toast.makeText(getApplicationContext(), "이메일 인증을 해주세요", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "닉네임/이메일/패스워드를 입력해주세요.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    /*nick name check*/
    private void checkOnlyName(final String name){
        String tag_string_req = "req_checking Nickname";

        pDialog.setMessage("Checking Nickname ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_CHECKONLY, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Checking Nickname: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {

                        Toast.makeText(getApplicationContext(), "사용가능한 닉네임 입니다.", Toast.LENGTH_LONG).show();
                        SharedPreferences sp3 = getSharedPreferences(name, MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp3.edit();
                        edit.putInt("only", 1);
                        edit.commit();
                        // Launch login activity
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Checking Nickname Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", name);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Function to make some text for authentication, and mail to the user
     * */
    private void sendAuthEmail(final String email) {
        String tag_string_req = "req_sending";

        pDialog.setMessage("Sending Email ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_EMAIL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Sending Email Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response.substring(response.indexOf("{"),response.lastIndexOf("}")+1));
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String salt = jObj.getString("salt");

                        SharedPreferences sp = getSharedPreferences(email, MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putInt("auth",0);
                        edit.putString("salt", salt);
                        edit.commit();

                        Toast.makeText(getApplicationContext(), "해당 메일에서 인증문자 확인 후 입력해주세요.", Toast.LENGTH_LONG).show();

                        // Launch login activity
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Sending Email Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }
    /**
     * Function to check text for authentication.
     * */

    private void checkText(final String authText, final String salt, final String email) {
        String tag_string_req = "req_checking";

        pDialog.setMessage("Checking Text ... ");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_AUTHENTICATION, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Checking Text Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        String finalText = jObj.getString("finalText");

                        SharedPreferences sp = getSharedPreferences(email, MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putInt("auth", 1);
                        edit.putString("salt", null);
                        edit.commit();

                        Toast.makeText(getApplicationContext(), "인증완료"+finalText, Toast.LENGTH_LONG).show();
                        // Launch login activity

                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "인증오류: " + error.getMessage());
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("salt", salt);
                params.put("authText", authText);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }



    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     * */
    private void registerUser(final String name, final String email, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_register";

        pDialog.setMessage("Registering ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_REGISTER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        // User successfully stored in MySQL
                        // Now store the user in sqlite

                        String uid = jObj.getString("uid");
                        JSONObject user = jObj.getJSONObject("user");
                        String name = user.getString("name");
                        String email = user.getString("email");
                        String created_at = user.getString("created_at");


                        // Inserting row in users table
                        db.addUser(name, email, uid, created_at);
                        Toast.makeText(getApplicationContext(), "등록되었습니다. 로그인하세요.", Toast.LENGTH_LONG).show();

                        // Launch login activity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", name);
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }
}
