package com.example.loyaltyprogram;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.*;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.*;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment {

    private static final String EXPECTED_QR = "LOYALTY-STORE-QR-2025";
    private static final int REWARD_POINTS = 5;

    private PreviewView previewView;
    private TextView tvStatus;
    private ExecutorService cameraExecutor;
    private boolean rewarded = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { if (granted) startCamera(); else setStatus("Camera permission denied"); });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scan, container, false);
        previewView = v.findViewById(R.id.previewView);
        tvStatus = v.findViewById(R.id.tvStatus);
        cameraExecutor = Executors.newSingleThreadExecutor();
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        rewarded = false;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyze);

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), selector, preview, analysis);
                setStatus("Align the QR within the frame");
            } catch (Exception e) {
                setStatus("Camera error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }


        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        BarcodeScanning.getClient(options)
                .process(image)
                .addOnSuccessListener(this::handleBarcodes)
                .addOnFailureListener(e -> { /* ignore per-frame */ })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        if (rewarded) return;
        for (Barcode b : barcodes) {
            String value = b.getRawValue();
            if (value != null && value.equals(EXPECTED_QR)) {
                rewarded = true;
                onValidQr();
                break;
            }
        }
    }

    private void onValidQr() {
        vibrate();
        setStatus("QR valid. +" + REWARD_POINTS + " points added!");
        Snackbar.make(requireView(), "Reward applied: +" + REWARD_POINTS, Snackbar.LENGTH_LONG).show();
        PointsRepository.getInstance(requireContext()).addPointsAsync(REWARD_POINTS);
    }

    private void setStatus(String s) {
        if (tvStatus != null) tvStatus.setText(s);
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    private void vibrate() {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = requireContext().getSystemService(VibratorManager.class);
            if (vm != null) vm.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
