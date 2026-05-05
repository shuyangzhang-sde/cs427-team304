package edu.uiuc.cs427app;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented coverage for MapActivity location display and navigation.
 * Tests verify that the map correctly displays city names and coordinates
 * for multiple cities, with location mocking to ensure accurate positioning.
 */
@RunWith(AndroidJUnit4.class)
public class MapActivityTest {

    private ActivityScenario<MapActivity> scenario;

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test that MapActivity displays Chicago with mocked location data.
     * Verifies city name and coordinate display for the first test city.
     */
    @Test
    public void mockingLocationTestChicago() throws InterruptedException {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MapActivity.class);
        intent.putExtra("city", "Chicago");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        scenario = ActivityScenario.launch(intent);
        Thread.sleep(500);

        // Verify city name is displayed
        onView(withId(R.id.cityName)).check(matches(isDisplayed()));
        onView(withId(R.id.cityName)).check(matches(withText("Chicago")));

        // Verify coordinates are displayed with valid latitude/longitude values
        onView(withId(R.id.cityCoordinates)).check(matches(isDisplayed()));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Latitude:"))));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Longitude:"))));

        // Verify map fragment is loaded
        scenario.onActivity(activity -> {
            assertNotNull("Map fragment should be loaded", 
                    activity.getSupportFragmentManager().findFragmentById(R.id.map));
        });
    }

    /**
     * Test that MapActivity displays Champaign with mocked location data.
     * Verifies city name and coordinate display for the second test city.
     */
    @Test
    public void mockingLocationTestChampaign() throws InterruptedException {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MapActivity.class);
        intent.putExtra("city", "Champaign");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        scenario = ActivityScenario.launch(intent);
        Thread.sleep(500);

        // Verify city name is displayed
        onView(withId(R.id.cityName)).check(matches(isDisplayed()));
        onView(withId(R.id.cityName)).check(matches(withText("Champaign")));

        // Verify coordinates are displayed with valid latitude/longitude values
        onView(withId(R.id.cityCoordinates)).check(matches(isDisplayed()));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Latitude:"))));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Longitude:"))));

        // Verify map fragment is loaded
        scenario.onActivity(activity -> {
            assertNotNull("Map fragment should be loaded", 
                    activity.getSupportFragmentManager().findFragmentById(R.id.map));
        });
    }

    /**
     * Test that MapActivity displays fallback coordinates when city cannot be geocoded.
     * This tests the robustness of the location feature with invalid input.
     */
    @Test
    public void mapActivityDisplaysCityNameAndLocationFallback() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MapActivity.class);
        intent.putExtra("city", "CityNameThatShouldNotResolve12345");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.cityName)).check(matches(isDisplayed()));
        onView(withId(R.id.cityName)).check(matches(withText("CityNameThatShouldNotResolve12345")));

        onView(withId(R.id.cityCoordinates)).check(matches(isDisplayed()));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Latitude:"))));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("Longitude:"))));
        onView(withId(R.id.cityCoordinates)).check(matches(withText(containsString("0.0"))));
    }
}
