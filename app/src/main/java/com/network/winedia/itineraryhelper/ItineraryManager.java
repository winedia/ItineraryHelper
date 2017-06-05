package com.network.winedia.itineraryhelper;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

class Route {
    class Hotel {
        String name, content, href, img;
    }

    class Budget {
        int sum;
        String details;
    }

    class City {
        class Travel {
            String type, content;
            int time;
        }

        class Place {
            String name, href, content, img;
            int time;
        }

        class Transport {
            String type;
            int time;
        }

        String name;
        Travel travel;
        List<Place> place;
        List<Transport> transport;

        public void parseCityObj(JSONObject cityObj) throws JSONException {
            name = cityObj.getString("name");

            JSONObject trObj = cityObj.optJSONObject("travel");
            if (trObj != null) {
                travel.type = trObj.getString("type");
                travel.content = trObj.getString("content");
                travel.time = trObj.getInt("time");
            }

            JSONArray placeArr = cityObj.getJSONArray("place");
            for (int i = 0; i < placeArr.length(); i++) {
                JSONObject plObj = placeArr.getJSONObject(i);
                Place pl = new Place();
                pl.name = plObj.getString("name");
                pl.href = plObj.getString("href");
                pl.img = plObj.getString("img");
                pl.content = plObj.getString("content");
                pl.time = plObj.getInt("time");
                place.add(pl);
            }

            JSONArray transportArr = cityObj.getJSONArray("transport");
            for (int i = 0; i < transportArr.length(); i++) {
                JSONObject tpObj = transportArr.getJSONObject(i);
                Transport tp = new Transport();
                tp.type = tpObj.getString("type");
                tp.time = tpObj.getInt("time");
                transport.add(tp);
            }
        }
    }

    int number;
    List<City> city;
    String content;
    Hotel hotel;
    Budget budget;

    public void parseRouteObj(JSONObject routeObj) throws JSONException {
        number = routeObj.getInt("number");

        JSONArray cityArr = routeObj.getJSONArray("city");
        for (int i = 0; i < cityArr.length(); i++) {
            City ct = new City();
            ct.parseCityObj(cityArr.getJSONObject(i));
            city.add(ct);
        }
        content = routeObj.getString("content");
        JSONObject hotelObj = routeObj.optJSONObject("hotel");
        if (hotelObj != null) {
            hotel.name = hotelObj.getString("name");
            hotel.content = hotelObj.getString("content");
            hotel.href = hotelObj.getString("href");
            hotel.img = hotelObj.getString("img");
        }

        JSONObject budgetObj = routeObj.getJSONObject("budget");
        budget.sum = budgetObj.getInt("sum");
        budget.details = budgetObj.getString("details");
    }
}

class Itinerary {
    class Overview {
        List<String> route;
        String tips, summary, img;
    }

    String id, title, subtitle, time;
    int people, budget;
    Overview overview;
    List<Route> route;
    String appendix;

    public void parseItiObj(JSONObject itiJsonObj) throws JSONException {
        id = itiJsonObj.getString("id");
        title = itiJsonObj.getString("title");
        subtitle = itiJsonObj.getString("subtitle");
        time = itiJsonObj.getString("time");
        people = itiJsonObj.getInt("people");
        budget = itiJsonObj.getInt("budget");

        JSONObject overviewObj = itiJsonObj.getJSONObject("overview");
        JSONArray overviewRouteArr = overviewObj.getJSONArray("route");
        for (int i = 0; i < overviewRouteArr.length(); i++) {
            overview.route.add(overviewRouteArr.getString(i));
        }
        overview.tips = overviewObj.getString("tips");
        overview.summary = overviewObj.getString("summary");
        overview.img = overviewObj.getString("img");

        JSONArray routeArr = itiJsonObj.getJSONArray("route");
        for (int i = 0; i < routeArr.length(); i++) {
            Route rt = new Route();
            rt.parseRouteObj(routeArr.getJSONObject(i));
            route.add(rt);
        }
        appendix = itiJsonObj.getString("appendix");
    }

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}

public class ItineraryManager {
    private static final String FILE_PATH = "/storage/itinerary/";
    private static final String TAG = "Itinerary";

    public static Itinerary readItinerary(String id) {
        String itiStr = "";
        try {
            InputStream is = new FileInputStream(FILE_PATH + id + ".iti");
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine();
            while (line != null) {
                itiStr += line;
                line = reader.readLine();
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "readItinerary: JSONString: " + itiStr);
        Itinerary iti = new Itinerary();
        try {
            JSONObject itiJsonObj = new JSONObject(itiStr);
            iti.parseItiObj(itiJsonObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return iti;
    }

    public static void writeItinerary(String id, Itinerary iti) {
        String itiStr = iti.toJsonString();
        Log.i(TAG, "writeItinerary: JSONString: " + itiStr);

        try {
            OutputStream os = new FileOutputStream(FILE_PATH + id + ".iti");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            writer.write(itiStr);
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
