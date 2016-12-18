package com.sylxjtu.prototype;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

  TextView proto;
  TextView temperature;
  TextView weatherIcon;
  TextView weatherText;
  TextView celsiusSymbol;
  ListView forecastList;
  ProgressBar progressBar;
  ArrayList<String> forecastItems = new ArrayList<>();
  ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    proto = (TextView) findViewById(R.id.proto);
    temperature = (TextView) findViewById(R.id.temperature);
    weatherIcon = (TextView) findViewById(R.id.weatherIcon);
    weatherText = (TextView) findViewById(R.id.weatherText);
    celsiusSymbol = (TextView) findViewById(R.id
        .celsiusSymbol);
    forecastList = (ListView) findViewById(R.id
        .forecastList);
    progressBar = (ProgressBar) findViewById(R.id
        .progressBar);

    Typeface typeface = Typeface.createFromAsset
        (getAssets(), "fonts/weathericons-regular-webfont" +
            ".ttf");
    weatherIcon.setTypeface(typeface);
    weatherIcon.setText(R.string.wi_na);
    celsiusSymbol.setTypeface(typeface);
    celsiusSymbol.setText(R.string.wi_celsius);

    adapter = new ArrayAdapter<>(this, android.R.layout
        .simple_list_item_1, forecastItems);
    forecastList.setAdapter(adapter);

    getLocation();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String
                                             permissions[], @NonNull int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] ==
        PackageManager.PERMISSION_GRANTED) {
      getLocation();
    } else {
      proto.setText("定位失败");
      progressBar.setIndeterminate(false);
    }
  }

  private void error(String info) {
    proto.setText(info);
    weatherIcon.setText(R.string.wi_na);
    progressBar.setIndeterminate(false);
  }

  void getCity(double lat, double lon) {
    String url = String.format(Locale.CHINA,
        getString(R.string.convert_coord_url),
        getString(R.string.api_key), lon, lat);
    new JsonApiTask(url, this) {
      @Override
      void onSuccess(JSONObject obj, Context context)
          throws JSONException {
        String locations = obj.getString("locations");
        String url = String.format(Locale.CHINA,
            getString(R.string.regeo_url),
            getString(R.string.api_key), locations);
        new JsonApiTask(url, context) {
          @Override
          void onSuccess(JSONObject obj, Context context)
              throws JSONException {
            JSONObject res = obj.getJSONObject
                ("regeocode").getJSONObject
                ("addressComponent");
            String adcode = res.getString("adcode");
            proto.setText(res.getString("province") + res
                .getString("city") + res.getString
                ("district"));
            getWeather(adcode);
            getForecast(adcode);
            progressBar.setIndeterminate(false);
          }

          @Override
          void onPending() {
            proto.setText(R.string.getting_city_info);
          }

          @Override
          void onError() {
            error(getString(R.string.network_error));
          }
        }.execute();
      }

      @Override
      void onPending() {

      }

      @Override
      void onError() {
        error(getString(R.string.network_error));
      }
    }.execute();

  }

  void getWeather(String adcode) {
    String url = String.format(Locale.CHINA,
        getString(R.string.weather_url),
        getString(R.string.api_key), adcode);
    new JsonApiTask(url, this) {
      @Override
      void onSuccess(JSONObject obj, Context context)
          throws JSONException {
        JSONObject weatherData = obj.getJSONArray
            ("lives").getJSONObject(0);
        temperature.setText(weatherData.getString
            ("temperature"));
        weatherText.setText(weatherData.getString
            ("weather"));
        String weatherName = weatherData.getString
            ("weather");
        String[] weatherNameMap = getResources()
            .getStringArray(R.array.weather_name_map);
        TypedArray weatherIconMap = getResources()
            .obtainTypedArray(R.array.weather_icon_map);
        for (int i = 0; i < weatherNameMap.length; i++) {
          if (weatherName.equals(weatherNameMap[i])) {
            weatherIcon.setText(weatherIconMap
                .getResourceId(i, -1));
            break;
          }
        }
        weatherIconMap.recycle();
      }

      @Override
      void onPending() {
        temperature.setText(R.string.getting);
      }

      @Override
      void onError() {
        error(getString(R.string.network_error));
      }
    }.execute();
  }

  void getForecast(String adcode) {
    String url = String.format(Locale.CHINA,
        getString(R.string.forecast_url),
        getString(R.string.api_key), adcode);
    new JsonApiTask(url, this) {
      @Override
      void onSuccess(JSONObject obj, Context context)
          throws JSONException {
        adapter.clear();
        JSONArray weatherDatas = obj.getJSONArray
            ("forecasts").getJSONObject(0).getJSONArray
            ("casts");
        for (int i = 0; i < weatherDatas.length(); i++) {
          JSONObject weatherData = weatherDatas
              .getJSONObject(i);
          String t = "";
          t += weatherData.getString("date") + "\n";
          if (!weatherData.getString("dayweather").equals
              ("[]"))
            t += String.format(Locale.CHINA, "白天     %s  " +
                "   %s℃\n", weatherData.getString
                ("dayweather"), weatherData.getString
                ("daytemp"));
          if (!weatherData.getString("nightweather")
              .equals("[]"))
            t += String.format(Locale.CHINA, "晚上     %s  " +
                "   %s℃", weatherData.getString
                ("nightweather"), weatherData.getString
                ("nighttemp"));
          adapter.add(t);
        }
      }

      @Override
      void onPending() {
        adapter.clear();
      }

      @Override
      void onError() {
        error("网络错误");
      }
    }.execute();
  }

  void getLocation() {
    adapter.clear();
    progressBar.setIndeterminate(true);
    LocationManager locationManager = (LocationManager)
        getSystemService(Context.LOCATION_SERVICE);
    LocationListener locationListener = new
        LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        getCity(location.getLatitude(), location
            .getLongitude());
      }

      @Override
      public void onStatusChanged(String provider, int
          status, Bundle extras) {

      }

      @Override
      public void onProviderEnabled(String provider) {

      }

      @Override
      public void onProviderDisabled(String provider) {

      }
    };
    if (ActivityCompat.checkSelfPermission(this, Manifest
        .permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new
          String[]{Manifest.permission
          .ACCESS_FINE_LOCATION}, 0);
      return;
    }
    Location location;
    location = locationManager.getLastKnownLocation
        (LocationManager.PASSIVE_PROVIDER);
    if (location != null) {
      getCity(location.getLatitude(), location
          .getLongitude());
    }
    locationManager.requestLocationUpdates
        (LocationManager.GPS_PROVIDER, 0, 0,
            locationListener);
  }

  public void doRefresh(View view) {
    getLocation();
  }
}
