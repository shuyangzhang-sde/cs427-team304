package edu.uiuc.cs427app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Live weather instrumentation tests for two cities.
 * Assertions verify that the UI reflects the same location-specific data produced by
 * the repository for each city at runtime.
 */
@RunWith(AndroidJUnit4.class)
public class WeatherActivityLiveWeatherTest {

    private ActivityScenario<WeatherActivity> scenario;

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
            scenario = null;
        }
        try {
            Intents.release();
        } catch (IllegalStateException ignored) {
            // Intents may not be initialized when setup fails.
        }
    }

    @Test
    public void liveWeatherForChicagoMatchesGroundTruth() throws Exception {
        assertLiveWeatherForCity("Chicago");
    }

    @Test
    public void liveWeatherForChampaignMatchesGroundTruth() throws Exception {
        assertLiveWeatherForCity("Champaign");
    }

    private void assertLiveWeatherForCity(String cityName) throws Exception {
        WeatherResult expectedResult = WeatherRepository.fetchCurrentWeather(
                cityName, BuildConfig.WEATHER_API_KEY);
        Assert.assertTrue(
                "Weather API pre-check failed for " + cityName + ": " + expectedResult.getErrorMessage(),
                expectedResult.isOk() && expectedResult.getData() != null);

        WeatherData expected = expectedResult.getData();

        Intents.init();
        intending(hasComponent(WeatherInsightsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WeatherActivity.class);
        intent.putExtra(WeatherActivity.EXTRA_CITY, cityName);
        scenario = ActivityScenario.launch(intent);

        // Small demo pause to let network fetch + UI bind complete for video readability.
        Thread.sleep(6000);

        onView(withId(R.id.weatherError))
                .check(matches(withEffectiveVisibility(GONE)));
        onView(withId(R.id.weatherCityName)).check(matches(withText(expected.getCityDisplayName())));
        onView(withId(R.id.weatherTemperature))
                .check(matches(withText("Temperature: " + expected.getTemperatureLine())));
        onView(withId(R.id.weatherCondition))
                .check(matches(withText("Conditions: " + expected.getConditionLabel())));
        onView(withId(R.id.weatherHumidity))
                .check(matches(withText("Humidity: " + expected.getHumidityLine())));
        onView(withId(R.id.weatherWind))
                .check(matches(withText("Wind: " + expected.getWindLine())));

        onView(withId(R.id.weatherInsightsButton))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
                .perform(click());

        Assert.assertTrue(
                "Expected WeatherInsightsActivity launch intent for city " + cityName,
                hasWeatherInsightsLaunch(cityName, Intents.getIntents()));
    }

    private static boolean hasWeatherInsightsLaunch(String expectedCityName, List<Intent> intents) {
        for (Intent sentIntent : intents) {
            if (sentIntent == null || sentIntent.getComponent() == null) {
                continue;
            }
            String className = sentIntent.getComponent().getClassName();
            if (!WeatherInsightsActivity.class.getName().equals(className)) {
                continue;
            }
            String sentCity = sentIntent.getStringExtra(WeatherInsightsActivity.EXTRA_CITY_NAME);
            String weatherJson = sentIntent.getStringExtra(WeatherInsightsActivity.EXTRA_WEATHER_JSON);
            if (expectedCityName.equals(sentCity) && weatherJson != null && !weatherJson.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}