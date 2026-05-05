package edu.uiuc.cs427app;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeDefaults;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemePromptBuilder;
import edu.uiuc.cs427app.theme.ThemeSpec;

public class AddCityActivity extends AppCompatActivity {

    private EditText editTextCityName;
    private Button buttonSaveCity;
    private Button buttonCancel;
    private String currentUsername;
    private DatabaseHelper databaseHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_city);

        String themeJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (themeJson != null && !themeJson.isEmpty()) {
            ThemeManager.applyTheme(this, ThemeParser.parseThemeJsonOrDefault(themeJson));
        }

        currentUsername = getIntent().getStringExtra("username");
        databaseHelper = new DatabaseHelper(this);

        editTextCityName = findViewById(R.id.editTextCityName);
        buttonSaveCity = findViewById(R.id.buttonSaveCity);
        buttonCancel = findViewById(R.id.buttonCancel);

        buttonSaveCity.setOnClickListener(v -> saveCity());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void saveCity() {
        String cityName = properCap(editTextCityName.getText().toString().trim());

        if (cityName.isEmpty()) {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.cityExistsForUser(currentUsername, cityName)) {
            Toast.makeText(this, "City already added", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSaveCity.setEnabled(false);

        executor.execute(() -> {
            boolean isValid = isValidCity(cityName);

            runOnUiThread(() -> {
                buttonSaveCity.setEnabled(true);

                if (!isValid) {
                    Toast.makeText(
                            AddCityActivity.this,
                            "Invalid city. Please enter a real city name.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                boolean success = databaseHelper.addCityForUser(currentUsername, cityName);
                if (success) {
                    Toast.makeText(AddCityActivity.this, "City added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddCityActivity.this, "Failed to add city", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isValidCity(String cityName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> results = geocoder.getFromLocationName(cityName, 5);
            if (results == null || results.isEmpty()) {
                return false;
            }

            String input = cityName.trim().toLowerCase();

            for (Address address : results) {
                String locality = address.getLocality();
                String subAdmin = address.getSubAdminArea();
                String admin = address.getAdminArea();

                if (locality != null && locality.trim().toLowerCase().equals(input)) {
                    return true;
                }

                if (subAdmin != null && subAdmin.trim().toLowerCase().equals(input)) {
                    return true;
                }

                if (admin != null && admin.trim().toLowerCase().equals(input)) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /** Ensures proper capitalization of city name after its typed for display **/
    private static String properCap(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

/**clean up method**/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}