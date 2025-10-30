package com.example.loyaltyprogram;

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

    private TextView nameView;
    private TextView emailView;
    private TextView phoneView;
    private TextView pointsView;
    private AlertDialog profileDetailsDialog;

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

        nameView = view.findViewById(R.id.tvName);
        emailView = view.findViewById(R.id.tvEmail);
        phoneView = view.findViewById(R.id.tvPhone);
        pointsView = view.findViewById(R.id.tvPoints);

        view.findViewById(R.id.cardProfile)
                .setOnClickListener(v -> showProfileDetailsDialog(false));

        updateProfileUi();

        if (!UserPreferences.isProfileComplete(requireContext())) {
            showProfileDetailsDialog(true);
        }
    }

    @Override
    public void onDestroyView() {
        if (profileDetailsDialog != null && profileDetailsDialog.isShowing()) {
            profileDetailsDialog.dismiss();
        }
        profileDetailsDialog = null;
        super.onDestroyView();
    }

    private void updateProfileUi() {
        String storedName = UserPreferences.getUserName(requireContext());
        if (storedName.isEmpty()) {
            storedName = getString(R.string.profile_name_placeholder);
        }
        nameView.setText(storedName);

        String storedEmail = UserPreferences.getUserEmail(requireContext());
        if (storedEmail.isEmpty()) {
            storedEmail = getString(R.string.profile_email_placeholder);
        }
        emailView.setText(storedEmail);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhoneNumber() != null) {
            phoneView.setText(user.getPhoneNumber());
        } else {
            phoneView.setText(R.string.profile_phone_placeholder);
        }

        int points = PointsRepository.getInstance(requireContext()).getPoints();
        pointsView.setText(getString(R.string.profile_points_value, points));
    }

    private void showProfileDetailsDialog(boolean force) {
        if (profileDetailsDialog != null && profileDetailsDialog.isShowing()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_profile_details, null);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.inputLayoutName);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.inputLayoutEmail);
        TextInputEditText nameInput = dialogView.findViewById(R.id.inputName);
        TextInputEditText emailInput = dialogView.findViewById(R.id.inputEmail);

        String currentName = UserPreferences.getUserName(requireContext());
        if (!currentName.isEmpty()) {
            nameInput.setText(currentName);
            nameInput.setSelection(currentName.length());
        }

        String currentEmail = UserPreferences.getUserEmail(requireContext());
        if (!currentEmail.isEmpty()) {
            emailInput.setText(currentEmail);
            emailInput.setSelection(currentEmail.length());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(force ? R.string.profile_details_title : R.string.profile_edit_details_title)
                .setView(dialogView)
                .setPositiveButton(R.string.profile_details_save, null);

        if (!force) {
            builder.setNegativeButton(android.R.string.cancel, null);
        }

        profileDetailsDialog = builder.create();
        profileDetailsDialog.setCancelable(!force);
        profileDetailsDialog.setCanceledOnTouchOutside(!force);

        profileDetailsDialog.setOnShowListener(dialog -> {
            Button saveButton = profileDetailsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String enteredName = nameInput.getText() != null
                        ? nameInput.getText().toString().trim()
                        : "";
                String enteredEmail = emailInput.getText() != null
                        ? emailInput.getText().toString().trim()
                        : "";

                boolean hasError = false;
                if (enteredName.isEmpty()) {
                    nameLayout.setError(getString(R.string.profile_edit_name_error));
                    hasError = true;
                } else {
                    nameLayout.setError(null);
                }

                if (enteredEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(enteredEmail).matches()) {
                    emailLayout.setError(getString(R.string.profile_edit_email_error));
                    hasError = true;
                } else {
                    emailLayout.setError(null);
                }

                if (!hasError) {
                    UserPreferences.setUserName(requireContext(), enteredName);
                    UserPreferences.setUserEmail(requireContext(), enteredEmail);
                    updateProfileUi();
                    Toast.makeText(requireContext(), R.string.profile_details_saved,
                            Toast.LENGTH_SHORT).show();
                    profileDetailsDialog.dismiss();
                }
            });
        });

        profileDetailsDialog.show();
    }
}