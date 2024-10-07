package com.example.bucks;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class TextToVoiceActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private MediaPlayer mediaPlayer;
    private EditText editText;
    private Button generateVoiceButton;
    private Button downloadVoiceButton;
    private Spinner voiceSelectionSpinner;
    private String generatedVoiceText; // To store the generated voice text
    private boolean isVoiceGenerated = false; // Flag to check if voice is generated
    private ArrayList<Voice> availableVoices; // To hold the available voices

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_voice);

        editText = findViewById(R.id.editText);
        generateVoiceButton = findViewById(R.id.generateVoiceButton);
        downloadVoiceButton = findViewById(R.id.downloadVoiceButton);
        voiceSelectionSpinner = findViewById(R.id.voiceSelectionSpinner);

        // Populate the spinner with voice options
        ArrayList<String> voiceOptions = new ArrayList<>();
        voiceOptions.add("Select Voice");
        voiceOptions.add("Male");
        voiceOptions.add("Female");

        setupVideoBackground();

        // Create and set the adapter for the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voiceOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSelectionSpinner.setAdapter(adapter);

        // Set up TextToSpeech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(TextToVoiceActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        // Get available voices
                        availableVoices = new ArrayList<>();
                        for (Voice voice : textToSpeech.getVoices()) {
                            availableVoices.add(voice);
                        }
                    }
                }
            }
        });

        generateVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateVoice();
            }
        });

        downloadVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadVoice();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void setupVideoBackground() {
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.vid); // Your video file
        TextureView backgroundTextureView = findViewById(R.id.backgroundTextureView);
        backgroundTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getApplicationContext(), videoUri);
                    mediaPlayer.setSurface(new Surface(surface));
                    mediaPlayer.setLooping(true); // Loop the video
                    mediaPlayer.setVolume(0f, 0f); // Mute the video
                    mediaPlayer.setOnPreparedListener(mp -> mp.start()); // Start playback when prepared
                    mediaPlayer.prepareAsync(); // Prepare the media player
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    private void generateVoice() {
        generatedVoiceText = editText.getText().toString();
        if (!generatedVoiceText.isEmpty()) {
            if (voiceSelectionSpinner.getSelectedItem() != null) {
                String selectedVoice = voiceSelectionSpinner.getSelectedItem().toString();
                Log.d("Selected Voice", selectedVoice);

                Voice voiceToUse = null;
                for (Voice voice : availableVoices) {
                    // Get the gender feature from the voice
                    String voiceGender = voice.getFeatures().get("gender"); // Use "gender" key to get the gender

                    if (selectedVoice.equals("Male") && "male".equals(voiceGender)) {
                        voiceToUse = voice;
                        break;
                    } else if (selectedVoice.equals("Female") && "female".equals(voiceGender)) {
                        voiceToUse = voice;
                        break;
                    }
                }

                if (voiceToUse != null) {
                    textToSpeech.setVoice(voiceToUse);
                    textToSpeech.speak(generatedVoiceText, TextToSpeech.QUEUE_FLUSH, null, null);
                    isVoiceGenerated = true; // Set the flag to true
                    findViewById(R.id.downloadVoiceLayout).setVisibility(View.VISIBLE); // Show download button
                } else {
                    Toast.makeText(this, "Please select a valid voice", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please select a voice", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter text to generate voice", Toast.LENGTH_SHORT).show();
        }
    }



    private void downloadVoice() {
        if (isVoiceGenerated) {
            String filePath = getExternalFilesDir(null) + "/generated_voice.wav"; // Specify your path and filename

            // Synthesize the speech to a file
            textToSpeech.synthesizeToFile(generatedVoiceText, null, new File(filePath), "UniqueID");

            Toast.makeText(this, "Voice generated and saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No voice generated yet to download", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
