package com.network.winedia.itineraryhelper;

import android.os.Environment;

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

/**
 * Created by lihongliang on 2017-06-07.
 */
public class user
{
    String name;
    int flag;
    List<Itinerary> Creat = new ArrayList<>();
    List<Itinerary> Invite = new ArrayList<>();
    List<Itinerary> Share = new ArrayList<>();
    Itinerary cur_i;
    Itinerary last_i;

    public void update() {

    }

    private static final File sdDir = Environment.getExternalStorageDirectory();
    public void saveUserLog() {
        File path = new File(sdDir + "/itinerary/");
        if (!path.exists()) path.mkdir();
        String userStr = "{\"user\":\"" + name + "\"}";
        try {
            OutputStream os = new FileOutputStream(path + "/user.log");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            writer.write(userStr);
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readUserLog() {
        File path = new File(sdDir + "/itinerary/user.log");
        if (!path.exists()) return;
        String userStr = "";

        try {
            InputStream is = new FileInputStream(path);
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine();
            while (line != null) {
                userStr += line;
                line = reader.readLine();
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JSONObject userObj = new JSONObject(userStr);
            name = userObj.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
