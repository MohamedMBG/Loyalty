package com.example.loyaltyprogram;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView nameView;
    private TextView emailView;
    private TextView phoneView;
    private TextView birthdayView;
    private TextView pointsView;
    private AlertDialog profileDetailsDialog;
    private static final String BIRTHDAY_STORAGE_PATTERN = "yyyy-MM-dd";
    private static final String BIRTHDAY_DISPLAY_PATTERN = "MMM d, yyyy";

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
        birthdayView = view.findViewById(R.id.tvBirthday);
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

        phoneView.setText(R.string.profile_phone_placeholder);

        PointsRepository pointsRepository = PointsRepository.getInstance(requireContext());

        String storedBirthday = UserPreferences.getUserBirthday(requireContext());
        boolean birthdayBonusAwarded = false;
        if (storedBirthday.isEmpty()) {
            birthdayView.setText(R.string.profile_birthday_placeholder);
        } else {
            Date birthdayDate = parseStoredBirthday(storedBirthday);
            if (birthdayDate != null) {
                String displayDate = formatBirthdayForDisplay(birthdayDate);
                birthdayView.setText(getString(R.string.profile_birthday_value, displayDate));
                birthdayBonusAwarded = pointsRepository.applyBirthdayBonusIfEligible(storedBirthday);
            } else {
                birthdayView.setText(R.string.profile_birthday_placeholder);
            }
        }

        int points = pointsRepository.getPoints();
        pointsView.setText(getString(R.string.profile_points_value, points));

        if (birthdayBonusAwarded) {
            Toast.makeText(requireContext(), R.string.profile_birthday_bonus_awarded,
                    Toast.LENGTH_LONG).show();
        }
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
        TextInputLayout birthdayLayout = dialogView.findViewById(R.id.inputLayoutBirthday);
        TextInputEditText birthdayInput = dialogView.findViewById(R.id.inputBirthday);

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

        String currentBirthdayIso = UserPreferences.getUserBirthday(requireContext());
        if (!currentBirthdayIso.isEmpty()) {
            Date birthdayDate = parseStoredBirthday(currentBirthdayIso);
            if (birthdayDate != null) {
                birthdayInput.setTag(R.id.inputBirthday, currentBirthdayIso);
                birthdayInput.setText(formatBirthdayForDisplay(birthdayDate));
            }
        }

        birthdayInput.setOnClickListener(v -> showBirthdayPicker(birthdayInput, birthdayLayout));
        birthdayInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showBirthdayPicker(birthdayInput, birthdayLayout);
            }
        });

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
                Object birthdayTag = birthdayInput.getTag(R.id.inputBirthday);
                String enteredBirthdayIso = birthdayTag instanceof String
                        ? (String) birthdayTag : "";

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

                if (TextUtils.isEmpty(enteredBirthdayIso)) {
                    birthdayLayout.setError(getString(R.string.profile_edit_birthday_error));
                    hasError = true;
                } else {
                    birthdayLayout.setError(null);
                }

                if (!hasError) {
                    UserPreferences.setUserName(requireContext(), enteredName);
                    UserPreferences.setUserEmail(requireContext(), enteredEmail);
                    UserPreferences.setUserBirthday(requireContext(), enteredBirthdayIso);

                    if (!TextUtils.equals(currentBirthdayIso, enteredBirthdayIso)) {
                        PointsRepository.getInstance(requireContext()).resetBirthdayBonusTracking();
                    }
                    updateProfileUi();
                    Toast.makeText(requireContext(), R.string.profile_details_saved,
                            Toast.LENGTH_SHORT).show();
                    profileDetailsDialog.dismiss();
                }
            });
        });

        profileDetailsDialog.show();
    }

    private void showBirthdayPicker(TextInputEditText birthdayInput, @Nullable TextInputLayout birthdayLayout) {
        Calendar calendar = Calendar.getInstance();
        Object birthdayTag = birthdayInput.getTag(R.id.inputBirthday);
        if (birthdayTag instanceof String && !((String) birthdayTag).isEmpty()) {
            Date storedDate = parseStoredBirthday((String) birthdayTag);
            if (storedDate != null) {
                calendar.setTime(storedDate);
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDate.set(Calendar.MINUTE, 0);
                    selectedDate.set(Calendar.SECOND, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    Date date = selectedDate.getTime();
                    SimpleDateFormat storageFormat = createStorageDateFormat();
                    SimpleDateFormat displayFormat = createDisplayDateFormat();

                    birthdayInput.setTag(R.id.inputBirthday, storageFormat.format(date));
                    birthdayInput.setText(displayFormat.format(date));

                    if (birthdayLayout != null) {
                        birthdayLayout.setError(null);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    @Nullable
    private Date parseStoredBirthday(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        SimpleDateFormat storageFormat = createStorageDateFormat();
        try {
            return storageFormat.parse(isoDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private String formatBirthdayForDisplay(Date date) {
        return createDisplayDateFormat().format(date);
    }

    private SimpleDateFormat createStorageDateFormat() {
        SimpleDateFormat storageFormat = new SimpleDateFormat(BIRTHDAY_STORAGE_PATTERN, Locale.US);
        storageFormat.setLenient(false);
        return storageFormat;
    }

    private SimpleDateFormat createDisplayDateFormat() {
        SimpleDateFormat displayFormat = new SimpleDateFormat(BIRTHDAY_DISPLAY_PATTERN, Locale.getDefault());
        displayFormat.setLenient(false);
        return displayFormat;
    }
}