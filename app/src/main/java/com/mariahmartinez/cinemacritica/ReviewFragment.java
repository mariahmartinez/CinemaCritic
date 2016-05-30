package com.mariahmartinez.cinemacritica;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ReviewFragment extends Fragment {

    private ArrayAdapter<String> mReviewAdapter;

    public ReviewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle main events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.reviewfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchReviewTask reviewTask = new FetchReviewTask();

            reviewTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        List<String> movieList = new ArrayList<>();

        mReviewAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_review, // The name of the layout ID.
                        R.id.list_item_review_textview, // The ID of the textview to populate.
                        movieList);

        View rootView = inflater.inflate(R.layout.fragment_review, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_review);
        listView.setAdapter(mReviewAdapter);
        FetchReviewTask reviewTask = new FetchReviewTask();

        reviewTask.execute();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = mReviewAdapter.getItem(position);
                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    public class FetchReviewTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchReviewTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String reviewString = null;

            String format = "json";
            int numMovies = 20;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "https://api.nytimes.com/svc/movies/v2/reviews/all/.json";
                final String APPID_PARAM = "api-key";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(APPID_PARAM, "9c5104a364ff45cb9e7d289ae6098537")
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to NYTimesAPI, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                reviewString = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getReviewDataFromJson(reviewString, numMovies);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mReviewAdapter.clear();
                for(String reviewString : result) {
                    mReviewAdapter.add(reviewString);
                }
            }
        }

        private String[] getReviewDataFromJson(String forecastJsonStr, int numMovies) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String REVIEW_LIST = "results";
            final String REVIEW_TITLE = "display_title";
//            final String REVIEW_LINKS = "links";
//            final String REVIEW_MULTIMEDIA = "multimedia";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray reviewArray = forecastJson.getJSONArray(REVIEW_LIST);

            String[] resultStrs = new String[numMovies];

            for(int i = 0; i < reviewArray.length(); i++) {
                String description;
//                String displayTitle;
//                String mpaaRating;
//                String criticsPick;
//                String byLine;
//                String headline;
//                String summary;
//                String link;
//                String imageSource;

                // Get the JSON object representing the day
                JSONObject reviewObject = reviewArray.getJSONObject(i);
                description = reviewObject.getString(REVIEW_TITLE);

                resultStrs[i] = description;
            }

            return resultStrs;

        }
    }
}