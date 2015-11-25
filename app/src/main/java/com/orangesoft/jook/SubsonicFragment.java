package com.orangesoft.jook;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.orangesoft.jook.subsonic.PingRequest;
import com.orangesoft.jook.subsonic.SubsonicConnection;
import com.orangesoft.subsonic.system.Ping;


public class SubsonicFragment extends Fragment
{
    private String host;
    private String user;
    private String password;
    private TextView outputField;
    private SubsonicConnection connection;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subsonic, container, false);
        Button validateButton = (Button) view.findViewById(R.id.validate_button);
        validateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doValidation();
            }
        });
        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        connection = new SubsonicConnection(this.getActivity());
        restorePreferences();
        updateView();
    }

    @Override
    public void onStop()
    {
        connection.close();
        super.onStop();
        savePreferences();
    }

    private void doValidation()
    {
        outputField = (TextView) getActivity().findViewById(R.id.textView);
        outputField.setText("");
        getActivity().setProgressBarIndeterminateVisibility(true);
        EditText hostField = (EditText) getActivity().findViewById(R.id.hostname_input);
        EditText userField = (EditText) getActivity().findViewById(R.id.name_input);
        EditText passwordField = (EditText) getActivity().findViewById(R.id.password_input);

        host = hostField.getText().toString();
        user = userField.getText().toString();
        password = passwordField.getText().toString();

        PingRequest request = new PingRequest( connection.getConnection() );
        connection.sendRequest(request, new PingRequestListener());
        hideKeyboard();
    }

    private void hideKeyboard()
    {
        View view = getActivity().getCurrentFocus();
        if (view != null)
        {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void restorePreferences()
    {
        user = connection.getSubsonicUser();
        host = connection.getSubsonicHost();
        password = connection.getSubsonicPassword();
    }

    private void savePreferences()
    {
        connection.saveConnectionDetails(host, user, password);
    }

    private void updateView()
    {
        EditText hostField = (EditText) getActivity().findViewById(R.id.hostname_input);
        EditText userField = (EditText) getActivity().findViewById(R.id.name_input);
        EditText passwordField = (EditText) getActivity().findViewById(R.id.password_input);
        hostField.setText(host);
        userField.setText(user);
        passwordField.setText(password);
    }

    private final class PingRequestListener implements RequestListener<Ping>
    {
        @Override
        public void onRequestFailure(SpiceException spiceException)
        {
            Toast.makeText(getActivity(),
                    "Error: " + spiceException.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestSuccess(Ping result)
        {
            getActivity().setProgressBarIndeterminateVisibility(false);
            if (result.getStatus()) {
                outputField.setText("Connected to " + host);
            }
            else {
                outputField.setText(result.getFailureMessage());
            }
        }
    }
}
