package org.thoughtcrime.securesms.components.location;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.session.libsignal.utilities.logging.Log;

import org.session.libsignal.utilities.JsonUtil;

import java.io.IOException;

public class SignalPlace {

  /* Loki - Temporary Placeholders */
  class LatLng {
    double latitude;
    double longitude;
    LatLng(double latitude, double longitude) {
      this.latitude = latitude;
      this.longitude = longitude;
    }
  }

  class Place {
    public CharSequence getName() { return ""; }
    public CharSequence getAddress() { return ""; }
    LatLng getLatLng() { return new LatLng(0, 0); }
  }

  private static final String URL = "https://maps.google.com/maps";
  private static final String TAG = SignalPlace.class.getSimpleName();

  @JsonProperty
  private CharSequence name;

  @JsonProperty
  private CharSequence address;

  @JsonProperty
  private double latitude;

  @JsonProperty
  private double longitude;

  public SignalPlace(Place place) {
    this.name      = place.getName();
    this.address   = place.getAddress();
    this.latitude  = place.getLatLng().latitude;
    this.longitude = place.getLatLng().longitude;
  }

  public SignalPlace() {}

  @JsonIgnore
  public LatLng getLatLong() {
    return new LatLng(latitude, longitude);
  }

  @JsonIgnore
  public String getDescription() {
    String description = "";

    if (!TextUtils.isEmpty(name)) {
      description += (name + "\n");
    }

    if (!TextUtils.isEmpty(address)) {
      description += (address + "\n");
    }

    description += Uri.parse(URL)
                      .buildUpon()
                      .appendQueryParameter("q", String.format("%s,%s", latitude, longitude))
                      .build().toString();

    return description;
  }

  public @Nullable String serialize() {
    try {
      return JsonUtil.toJsonThrows(this);
    } catch (IOException e) {
      Log.w(TAG, e);
      return null;
    }
  }

  public static SignalPlace deserialize(@NonNull  String serialized) throws IOException {
    return JsonUtil.fromJson(serialized, SignalPlace.class);
  }
}
