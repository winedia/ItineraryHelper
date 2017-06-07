package com.network.winedia.itineraryhelper;


import android.os.StrictMode;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HTTPUtils {
    private static final String TAG = "HTTPUtils";
    private static final String SERVER_URL = "http://162.105.30.185/post.php";

    private static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    private static String doPost(Map<String, String> params, String urlStr) {
        String result = "";
        try{
            String data = getRequestData(params, "utf-8").toString();
            Log.i(TAG, "doPost: Params:"+ data);

            URL url = new URL(urlStr);
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setConnectTimeout(5000);
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConn.setRequestProperty("Charset", "utf-8");

            urlConn.connect();
            DataOutputStream dop = new DataOutputStream(urlConn.getOutputStream());
            dop.writeBytes(data);
            dop.flush();
            dop.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result += line;
            }
            reader.close();
            urlConn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String doGet(Map<String, String> params, String urlStr) {
        urlStr += "?" + getRequestData(params, "utf-8");
        Log.i(TAG, "doGet: Url:" + urlStr);
        String result = "";
        try {
            URL url = new URL(urlStr);
            HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
            urlConn.setConnectTimeout(3000);
            urlConn.setRequestMethod("Get");
            int code = urlConn.getResponseCode();
            if (code == 200) {
                InputStream is = urlConn.getInputStream();
                OutputStream os = new ByteArrayOutputStream();
                int len;
                byte buffer[] = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                is.close();
                os.close();
                result = os.toString();
            } else {
                Log.i(TAG, "doGet: ErrorCode: " + code);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String syncRequest(Itinerary iti, String user) {
        Map<String, String> params = new HashMap<>();
        params.put("request_type", "SYNC");
        params.put("id", iti.id);
        params.put("user", user);
        params.put("plan", iti.toJsonString());
        String result = doPost(params, SERVER_URL);
        String ret = "";
        try {
            JSONObject resultObj = new JSONObject(result);
            String respType = resultObj.getString("response_type");
            if (respType.equals("SYNC")) {
                ret = resultObj.getString("status");
                if (!ret.equals("SUCCEEDED")) {
                    Log.i(TAG, "syncRequest: ErrorMessage: " + resultObj.getString("error_msg"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Itinerary downloadRequest(String user, String id) {
        Map<String, String> params = new HashMap<>();
        params.put("request_type", "DOWNLOAD");
        params.put("user", user);
        params.put("id",id);
        String result = doPost(params, SERVER_URL);
        Itinerary iti = null;
        try {
            JSONObject resultObj = new JSONObject(result);
            String respType = resultObj.getString("response_type");
            String respId = resultObj.getString("id");
            if (respType.equals("DOWNLOAD") && id.equals(respId)) {
                String status = resultObj.getString("status");
                if (status.equals("SUCCEEDED")) {
                    JSONObject itiObj = resultObj.getJSONObject("plan");
                    iti = new Itinerary();
                    iti.parseItiObj(itiObj);
                } else {
                    Log.i(TAG, "downloadRequest: ErrorMessage: " + resultObj.getString("error_msg"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return iti;
    }

    public static String uploadRequest(String user, int auth) {
        Map<String, String> params = new HashMap<>();
        params.put("request_type", "UPLOAD");
        params.put("user", user);
        params.put("auth", String.valueOf(auth));
        String result = doPost(params, SERVER_URL);
        String ret = "";
        Log.i(TAG, "uploadRequest: result string: " + result);
        try {
            JSONObject resultObj = new JSONObject(result);
            String respType = resultObj.getString("response_type");
            if (respType.equals("UPLOAD")) {
                String status = resultObj.getString("status");
                if (status.equals("SUCCEEDED")) {
                    ret = resultObj.getString("id");
                    Log.i(TAG, "uploadRequest: Id: " + ret);
                } else {
                    Log.i(TAG, "uploadRequest: ErrorMessage: " + resultObj.getString("error_msg"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String forkRequest(String user, String id, int auth, String sourceUser) {
        Map<String, String> params = new HashMap<>();
        params.put("request_type", "FORK");
        params.put("user", user);
        params.put("id", id);
        params.put("auth", String.valueOf(auth));
        params.put("source_user", sourceUser);
        String result = doPost(params, SERVER_URL);
        String ret = "";
        try {
            JSONObject resultObj = new JSONObject(result);
            String respType = resultObj.getString("response_type");
            if (respType.equals("FORK")) {
                String status = resultObj.getString("status");
                if (status.equals("SUCCEEDED")) {
                    ret = resultObj.getString("id");
                } else {
                    Log.i(TAG, "forkRequest: ErrorMessage: " + resultObj.getString("error_msg"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
