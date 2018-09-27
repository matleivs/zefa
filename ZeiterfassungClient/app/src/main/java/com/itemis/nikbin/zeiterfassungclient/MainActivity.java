package com.itemis.nikbin.zeiterfassungclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivty";
    private static final int RC_SIGN_IN = 9001;


    // Add workday views
    private EditText mCurrentStartTimeField;
    private Button mAddWorkdayButton;
    private Button mSignInButton;

    // my instance of Firebase Cloud Functions
    private FirebaseFunctions mFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCurrentStartTimeField = findViewById(R.id.field_starttime_date_output);
        mAddWorkdayButton = findViewById(R.id.button_add_workday);
        mAddWorkdayButton.setOnClickListener(this);

        mSignInButton = findViewById(R.id.button_sign_in);
        mSignInButton.setOnClickListener(this);

        // [START initialize_functions_instance]
        mFunctions = FirebaseFunctions.getInstance();
        // [END initialize_functions_instance]
    }

    private void onClickSignIn() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            showSnackbar("Signed in alright");
            return;
        }

        signInWithFirebaseUiAuth();
    }

    private void signInWithFirebaseUiAuth() {
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                showSnackbar("Signed in yeah");
            } else {
                showSnackbar("Error signing in");

                IdpResponse response = IdpResponse.fromResultIntent(data);
//                FirebaseUiException error = response.getError();
//                Log.w(TAG, "signInWithFirebaseUiAuth", error);
                Log.d(TAG, "signInWithFirebaseUiAuth - Error");
            }
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sign_in:
                onClickSignIn();
                break;
            case R.id.button_add_workday:
                onClickAddWorkday();
                break;
        }
    }


    private Task<String> addWorkday(String date, String startTime) {
        // Create the arguments to the Callable Function
        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("startTime", startTime);
        data.put("push", true);

        return mFunctions
                .getHttpsCallable("addWorkday")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        String resultString = "StartTime on " + result.get("date") + ": " + result.get("startTime") + " Uhr.";
                        return resultString;
                    }
                });
    }

    private void onClickAddWorkday() {
        showSnackbar("yes u successfully clicked 1 button");

        String date = getSimpleDate();
        String time = getSimpleTime(); // using default current Time atm; later:add manual set time option
        // TODO Textinputfield to manually enter starttime, or else Snackbar
        // [START call_addWorkday]
        addWorkday(date, time)
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        Log.d(TAG, "addWorkday:onComplete");

                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) e).getCode();
                                Object details = ((FirebaseFunctionsException) e).getDetails();
                                Log.d(TAG, "addWorkday:onFailure Code: " + code.toString());
//                                Log.d(TAG, "addWorkday:onFailure Details: " + details.toString());
                            }
                            Log.w(TAG, "addWorkday:onFailure: " + e.getMessage(), e);
                            showSnackbar("An error occurred.");
                            return;
                        }

                        // if task.isSuccessful:
                        String result = task.getResult();
                        Log.d(TAG, "addWorkday: successful");
                        showSnackbar("Result: " + result);
                        mCurrentStartTimeField.setText(result);
                    }
                });
        // [END call_addWorkday]
    }


    /**
     * Taking Date and Time from the Client side, as the server time could differ from local
     */
    private String getSimpleTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        Date currentTime = new Date();
        return (formatter.format(currentTime));
    }

    private String getSimpleDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date currentTime = new Date();
        return (formatter.format(currentTime));
    }
}
