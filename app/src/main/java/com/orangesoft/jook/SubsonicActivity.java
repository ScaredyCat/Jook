package com.orangesoft.jook;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.PingRequest;
import com.orangesoft.jook.subsonic.SubsonicBaseActivity;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.subsonic.system.Ping;

public class SubsonicActivity extends SubsonicBaseActivity
{

    private String host;
    private String user;
    private String password;
    private TextView outputField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subsonic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle("Subsonic");
        Button validateButton = (Button) findViewById(R.id.validate_button);
        validateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doValidation();
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        restorePreferences();
        updateView();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        savePreferences();
    }

    @Override
    public void fetchData()
    {
        PingRequest request = new PingRequest( connection.getConnection() );
        connection.sendRequest(request, new PingRequestListener(this));
    }

    private void doValidation()
    {
        outputField = (TextView) findViewById(R.id.textView);
        outputField.setText("");
        setProgressBarIndeterminateVisibility(true);
        EditText hostField = (EditText) findViewById(R.id.hostname_input);
        EditText userField = (EditText) findViewById(R.id.name_input);
        EditText passwordField = (EditText) findViewById(R.id.password_input);

        host = hostField.getText().toString();
        user = userField.getText().toString();
        password = passwordField.getText().toString();

        fetchData();
        hideKeyboard();
    }

    private void hideKeyboard()
    {
        View view = getCurrentFocus();
        if (view != null)
        {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.
                    INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.
                    HIDE_NOT_ALWAYS);
        }
    }

    private void restorePreferences()
    {
        user = connection.getUser();
        host = connection.getHost();
        password = connection.getPassword();
    }

    private void savePreferences()
    {
        connection.saveConnectionDetails(host, user, password);
    }

    private void updateView()
    {
        EditText hostField = (EditText) findViewById(R.id.hostname_input);
        EditText userField = (EditText) findViewById(R.id.name_input);
        EditText passwordField = (EditText) findViewById(R.id.password_input);
        hostField.setText(host);
        userField.setText(user);
        passwordField.setText(password);
    }

    private final class PingRequestListener implements RequestListener<Ping>
    {
        Activity activity;

        public PingRequestListener(Activity activity)
        {
            this.activity = activity;
        }

        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(activity,
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(Ping result)
        {
            activity.setProgressBarIndeterminateVisibility(false);
            outputField = (TextView) findViewById(R.id.textView);
            if (result.getStatus()) {
                outputField.setText("Connected to " + host);
            }
            else {
                outputField.setText(result.getFailureMessage());
            }
        }
    }
}
