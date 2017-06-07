package com.network.winedia.itineraryhelper;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


class Itinerary {
    private static final String TAG = "Itinerary";
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
            List<Place> place = new ArrayList<>();
            List<Transport> transport = new ArrayList<>();

            public void parseCityObj(JSONObject cityObj) throws JSONException {
                name = cityObj.getString("name");

                JSONObject trObj = cityObj.optJSONObject("travel");
                if (trObj != null) {
                    travel = new Travel();
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

            public void mergeCity(City ct1, City ct2) {
                name = compareStr(name, ct1.name, ct2.name);
                travel.type = compareStr(travel.type, ct1.travel.type, ct2.travel.type);
                travel.content = compareStr(travel.content, ct1.travel.content, ct2.travel.content);
                travel.time = compareInt(travel.time, ct1.travel.time, ct2.travel.time);

                int len = place.size(), len1 = ct1.place.size(), len2 = ct2.place.size();
                for (int i = 0; i < len1; i++) {
                    if (i >= len) {
                        place.add(ct1.place.get(i));
                    } else if (i >= len2 || !place.get(i).equals(ct1.place.get(i))) {
                        place.set(i, ct1.place.get(i));
                    } else {
                        place.set(i, ct2.place.get(i));
                    }
                }
                len = transport.size();
                len1 = ct1.transport.size();
                len2 = ct2.transport.size();
                for (int i = 0; i < len1; i++) {
                    if (i >= len) {
                        transport.add(ct1.transport.get(i));
                    } else if (i >= len2 || !transport.get(i).equals(ct1.transport.get(i))) {
                        transport.set(i, ct1.transport.get(i));
                    } else {
                        transport.set(i, ct2.transport.get(i));
                    }
                }
            }
        }

        int number;
        List<City> city = new ArrayList<>();
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
                hotel = new Hotel();
                hotel.name = hotelObj.getString("name");
                hotel.content = hotelObj.getString("content");
                hotel.href = hotelObj.getString("href");
                hotel.img = hotelObj.getString("img");
            }

            JSONObject budgetObj = routeObj.optJSONObject("budget");
            if (budgetObj != null) {
                budget= new Budget();
                budget.sum = budgetObj.getInt("sum");
                budget.details = budgetObj.getString("details");
            }
        }

        public void mergeRoute(Route rt1, Route rt2) {
            number = compareInt(number, rt1.number, rt2.number);
            content = compareStr(content, rt1.content, rt2.content);

            hotel.name = compareStr(hotel.name, rt1.hotel.name, rt2.hotel.name);
            hotel.content = compareStr(hotel.content, rt1.hotel.content, rt2.hotel.content);
            hotel.href = compareStr(hotel.href, rt1.hotel.href, rt2.hotel.href);
            hotel.img = compareStr(hotel.img, rt1.hotel.img, rt2.hotel.img);

            this.budget.details = compareStr(this.budget.details, rt1.budget.details, rt2.budget.details);
            this.budget.sum = compareInt(this.budget.sum, rt1.budget.sum, rt2.budget.sum);

            int len = city.size(), len1 = rt1.city.size(), len2 = rt2.city.size();
            for (int i = 0; i < len1; i++) {
                if (i >= len) {
                    city.add(rt1.city.get(i));
                } else if (i >= len2) {
                    city.set(i, rt1.city.get(i));
                } else {
                    city.get(i).mergeCity(rt1.city.get(i), rt2.city.get(i));
                }
            }
        }
    }
    class Overview {
        List<String> route = new ArrayList<>();
        String tips, summary, img;

        public void mergeOverview(Overview ov1, Overview ov2) {
            tips = compareStr(tips, ov1.tips, ov2.tips);
            summary = compareStr(summary, ov1.summary, ov2.summary);
            img = compareStr(img, ov1.img, ov2.img);
            int len = this.route.size(), len1 = ov1.route.size(), len2 = ov2.route.size();
            for (int i = 0; i < len1; i++) {
                if (i >= len) {
                    this.route.add(ov1.route.get(i));
                } else if (i >= len2 || !route.get(i).equals(ov1.route.get(i))) {
                    this.route.set(i, ov1.route.get(i));
                } else {
                    this.route.set(i, ov2.route.get(i));
                }
            }
        }
    }

    String id, title, subtitle, time, user;
    int people, budget;
    Overview overview = new Overview();
    List<Route> route = new ArrayList<>();
    String appendix;

    public void parseItiObj(JSONObject itiJsonObj) throws JSONException {
        id = itiJsonObj.getString("id");
        user = itiJsonObj.getString("user");
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

    private String compareStr(String str0, String str1, String str2) {
        if (str0.equals(str1)) return str2;
        return str1;
    }
    private int compareInt(int x0, int x1, int x2) {
        if (x0 == x1) return x2;
        return x1;
    }

    public void mergeItinerary(Itinerary iti1, Itinerary iti2) { // iti1: last version, iti2: new version
        title = compareStr(title, iti1.title, iti2.title);
        subtitle = compareStr(subtitle, iti1.subtitle, iti2.subtitle);
        time = compareStr(time, iti1.time, iti2.time);
        people = compareInt(people, iti1.people, iti2.people);
        budget = compareInt(budget, iti1.budget, iti2.budget);
        appendix = compareStr(appendix, iti1.appendix, iti2.appendix);
        overview.mergeOverview(iti1.overview, iti2.overview);
        int len = route.size(), len1 = iti1.route.size(), len2 = iti2.route.size();
        for (int i = 0; i < len1; i++) {
            if (i >= len) {
                route.add(iti1.route.get(i));
            } else if (i >= len2) {
                route.set(i, iti1.route.get(i));
            } else {
                route.get(i).mergeRoute(iti1.route.get(i), iti2.route.get(i));
            }
        }
    }
}

public class ItineraryManager {
    private static final File sdDir = Environment.getExternalStorageDirectory();
    private static final String TAG = "Itinerary";

    private static Itinerary readItinerary(String path) {
        String itiStr = "";
        Log.i(TAG, "readItinerary: Reading file: " + path);
        try {
            InputStream is = new FileInputStream(path);
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

    public static void readAllItinerary(List<Itinerary> itiList) {
        File root = new File(sdDir + "/itinerary/");
        File files[] = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".iti")) itiList.add(readItinerary(f.getPath()));
            }
        }
    }
    public static void writeItinerary(String id, Itinerary iti) {
        String itiStr = iti.toJsonString();
        Log.i(TAG, "writeItinerary: JSONString: " + itiStr);

        try {
            OutputStream os = new FileOutputStream(sdDir.toString() + "/itinerary/" + id + ".iti");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            writer.write(itiStr);
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
