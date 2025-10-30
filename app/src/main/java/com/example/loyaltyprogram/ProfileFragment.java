package com.example.loyaltyprogram;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "loyalty_prefs";
    private static final String KEY_USER_NAME = "user_name";

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView nameView = view.findViewById(R.id.tvName);
        TextView phoneView = view.findViewById(R.id.tvPhone);
        TextView pointsView = view.findViewById(R.id.tvPoints);
        //View editNameButton = view.findViewById(R.id.btnEditName);

        String storedName = getStoredName();
        if (storedName.isEmpty()) {
            storedName = getString(R.string.profile_name_placeholder);
        }
        nameView.setText(storedName);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhoneNumber() != null) {
            phoneView.setText(user.getPhoneNumber());
        } else {
            phoneView.setText(R.string.profile_phone_placeholder);
        }

        int points = PointsRepository.getInstance(requireContext()).getPoints();
        pointsView.setText(getString(R.string.profile_points_value, points));

        //editNameButton.setOnClickListener(v -> showNameEditDialog(nameView));
    }

    private void showNameEditDialog(TextView targetView) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_name_input, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.inputLayoutName);
        TextInputEditText inputEditText = dialogView.findViewById(R.id.inputName);
        String currentName = getStoredName();
        if (!currentName.isEmpty()) {
            inputEditText.setText(currentName);
            inputEditText.setSelection(currentName.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_edit_name_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_edit_name_save, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String enteredName = inputEditText.getText() != null
                        ? inputEditText.getText().toString().trim()
                        : "";
                if (enteredName.isEmpty()) {
                    inputLayout.setError(getString(R.string.profile_edit_name_error));
                } else {
                    inputLayout.setError(null);
                    storeName(enteredName);
                    targetView.setText(enteredName);
                    Toast.makeText(requireContext(), R.string.profile_name_updated, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private String getStoredName() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    private void storeName(String name) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }
}