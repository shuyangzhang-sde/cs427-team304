package edu.uiuc.cs427app;
import edu.uiuc.cs427app.BuildConfig;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.uiuc.cs427app.theme.ThemeContract;
import edu.uiuc.cs427app.theme.ThemeDefaults;
import edu.uiuc.cs427app.theme.ThemeManager;
import edu.uiuc.cs427app.theme.ThemeParser;
import edu.uiuc.cs427app.theme.ThemePromptBuilder;
import edu.uiuc.cs427app.theme.ThemeSpec;

public class MainActivity extends AppCompatActivity {

    private String currentUsername;
    private String currentPrompt;
    private View hider;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DatabaseHelper databaseHelper;
    private ThemeSpec activeTheme = ThemeDefaults.defaultTheme();

    private LinearLayout cityContainer;

    private String currentTheme = "";

    /** Restores the active user and applies the current theme. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentUsername = getIntent().getStringExtra("username");
        currentPrompt = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_PROMPT);
        if (currentUsername == null || currentPrompt == null) {
            navigateToLogin();
            return;
        }

        databaseHelper = new DatabaseHelper(this);

        TextView titleView = findViewById(R.id.textViewTitle);
        titleView.setText("Team 304 - " + currentUsername);

        cityContainer = findViewById(R.id.cityContainer);
        if (cityContainer == null) {
            throw new RuntimeException("cityContainer is null - check activity_main.xml");
        }

        Button buttonNew = findViewById(R.id.buttonAddLocation);
        buttonNew.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCityActivity.class);
            intent.putExtra("username", currentUsername);
            intent.putExtra(ThemeContract.EXTRA_THEME_JSON, currentTheme);
            startActivity(intent);
        });

        Button buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToLogin();
            }
        });

        hider = findViewById(android.R.id.content);
        hider.setVisibility(View.INVISIBLE);
        applyResolvedTheme();
    }

    /** Applies theme when returning to main activity **/
    @Override
    protected void onResume() {
        super.onResume();
        if (databaseHelper == null) return;
        loadCities();
        ThemeManager.applyTheme(this, activeTheme);
    }

    /** Loads all buttons for an added city like weather and map **/
    private void loadCities() {
        cityContainer.removeAllViews();

        ArrayList<String> cities = databaseHelper.getCitiesForUser(currentUsername);

        for (String city : cities) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(12, 12, 12, 12);

            TextView cityName = new TextView(this);
            cityName.setText(city);
            cityName.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            Button mapButton = new Button(this);
            mapButton.setText("Map");
            mapButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("city", city);
                intent.putExtra(ThemeContract.EXTRA_THEME_JSON, currentTheme);
                startActivity(intent);
            });
            Button weatherButton = new Button(this);
            weatherButton.setText(R.string.weather_screen_title);
            weatherButton.setOnClickListener(v -> openWeatherScreen(city));

            Button removeButton = new Button(this);
            removeButton.setText("Remove");
            removeButton.setOnClickListener(v -> {
                databaseHelper.removeCityForUser(currentUsername, city);
                loadCities();
                ThemeManager.applyTheme(this, activeTheme);
            });

            row.addView(cityName);
            row.addView(mapButton);
            row.addView(weatherButton);
            row.addView(removeButton);
            cityContainer.addView(row);
        }
    }

    /**
     * Opens {@link WeatherActivity} for the specified city. The city name originates from
     * the database list for the current user and is passed as {@link WeatherActivity#EXTRA_CITY}.
     *
     * @param city city label associated with the selected row
     */
    private void openWeatherScreen(String city) {
        Intent intent = new Intent(this, WeatherActivity.class);
        intent.putExtra(WeatherActivity.EXTRA_CITY, city);
        intent.putExtra(ThemeContract.EXTRA_THEME_JSON, currentTheme);
        startActivity(intent);
    }

    /** Applies a validated theme, or the default theme when generation fails. */
    private void applyResolvedTheme() {
        String cachedJson = getIntent().getStringExtra(ThemeContract.EXTRA_THEME_JSON);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            currentTheme = cachedJson;
            activeTheme = ThemeParser.parseThemeJsonOrDefault(cachedJson);
            ThemeManager.applyTheme(this, activeTheme);
            hider.setVisibility(View.VISIBLE);
            return;
        }

        executor.execute(() -> {
            try {
                String themeJson = jsonRequest(currentPrompt);
                ThemeSpec theme = ThemeParser.parseThemeJsonOrDefault(themeJson);

                runOnUiThread(() -> {
                    try {
                        currentTheme = themeJson;
                        activeTheme = theme;
                        ThemeManager.applyTheme(this, activeTheme);
                        databaseHelper.updateThemeJson(currentUsername, themeJson);
                    } catch (Throwable uiThrowable) {
                        activeTheme = ThemeDefaults.defaultTheme();
                        ThemeManager.applyTheme(this, activeTheme);
                    }
                    hider.setVisibility(View.VISIBLE);
                });
            } catch (Throwable throwable) {
                runOnUiThread(() -> {
                    activeTheme = ThemeDefaults.defaultTheme();
                    ThemeManager.applyTheme(this, activeTheme);
                    hider.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    /** Sends reqeuest to get user theme from Gemini response **/
    private String jsonRequest(String input) throws Exception {
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(
                new JSONObject().put("text", ThemePromptBuilder.buildUserPrompt(input)))))).put("generationConfig", new JSONObject()
                .put("responseMimeType", "application/json").put("responseJsonSchema", ThemePromptBuilder.buildJsonSchema()));

        HttpURLConnection connection = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_KEY);
        connection.setDoOutput(true);

        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) builder.append(line.trim());

        JSONObject responseJson = new JSONObject(builder.toString());
        JSONArray candidates = responseJson.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String theme = parts.getJSONObject(0).getString("text");

        return theme;
    }

    /** Returns to login page when logging out **/
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Teardown when exiting the app **/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}