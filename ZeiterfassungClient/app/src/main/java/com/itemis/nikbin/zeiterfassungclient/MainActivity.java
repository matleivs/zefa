package com.itemis.nikbin.zeiterfassungclient;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivty";
    private static final int RC_SIGN_IN = 9001;


    // Add workday views
    private EditText mCurrentStartTimeField;
    private Button mAddWorkdayButton;

    // my instance of Firebase Cloud Functions
    private FirebaseFunctions mFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCurrentStartTimeField = findViewById(R.id.field_starttime_date_output);
        mAddWorkdayButton = findViewById(R.id.button_add_workday);
        mAddWorkdayButton.setOnClickListener(this);


        // [START initialize_functions_instance]
        mFunctions = FirebaseFunctions.getInstance();
        // [END initialize_functions_instance]
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_add_workday:
                onClickAddWorkday();
                break;
        }
    }


    /**
     * Date and Time Has to come from the Client side, as the server time could differ from local
     */
    private String getSimpleTime() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "HH:mm");
        Date currentTime = new Date();
        return (formatter.format(currentTime));
    }

    private String getSimpleDate() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd.MM.yyyy");
        Date currentTime = new Date();
        return (formatter.format(currentTime));
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
                            return;
                        }

                        // if task.isSuccessful:
                        String result = task.getResult();
                        Log.d(TAG, "addWorkday: successful");
                        mCurrentStartTimeField.setText(result);
                    }
                });
        // [END call_addWorkday]
    }
}
