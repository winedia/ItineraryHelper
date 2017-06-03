package com.network.winedia.itineraryhelper;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPUtils {
    private static final String SERVER_IP = "";
    private static final int SERVER_PORT = 8000;
    private static final int TIMEOUT = 5000;
    private static final String TAG = "TCP Service";

    private static InputStream cis = null;
    private static OutputStream cos = null;
    private static Socket clientSocket = null;
    private static boolean isConnected = false;

    private static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    private static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    private static Itinerary parseMessageToIti(String message, int type) throws JSONException {
        JSONObject msgObj = new JSONObject(message);
        if (type == 1 || type == 3) {
            JSONArray targetUsrs = msgObj.getJSONArray("target_users");
            boolean found = false;
            for (int i = 0; i < targetUsrs.length(); i++) {
                if (targetUsrs.getString(i).equals(MainActivity.user)) {
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        String id = msgObj.getString("id");
        JSONObject itiObj = msgObj.getJSONObject("plan");
        Itinerary iti = new Itinerary();
        iti.parseItiObj(itiObj);
        return iti;
    }

    public static void connectToServer() {
        try {
            clientSocket = new Socket();
            SocketAddress addr = new InetSocketAddress(SERVER_IP, SERVER_PORT);
            clientSocket.connect(addr, TIMEOUT);

            cis = clientSocket.getInputStream();
            cos = clientSocket.getOutputStream();
            isConnected = true;
            Log.i(TAG, "connectToServer: Connected");
            Thread recvThread = new Thread(new ReceiveThread(), "recvThread");
            recvThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectFromServer() {
        if (isConnected) {
            isConnected = false;
            try {
                clientSocket.close();
                Log.i(TAG, "disconnectFromServer: Disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean sendMessage(int type, String message) {
        try {
            if (isConnected && !clientSocket.isOutputShutdown()) {
                byte[] buffer;
                if (type != 8 && type != 10) {
                    int len = message.length();
                    buffer = new byte[len + 8];
                    System.arraycopy(intToByteArray(type), 0, buffer, 0, 4);
                    System.arraycopy(intToByteArray(len), 0, buffer, 4, 4);
                    System.arraycopy(message.getBytes(), 0, buffer, 8, len);
                } else {
                    buffer = new byte[24];
                    System.arraycopy(intToByteArray(type), 0, buffer, 0, 4);
                    System.arraycopy(message.getBytes(), 0, buffer, 4, 20);
                }
                cos.write(buffer);
                cos.flush();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    static class ReceiveThread implements Runnable {
        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            int len;
            if (clientSocket != null && isConnected) {
                try {
                    while (!clientSocket.isInputShutdown() && (len = cis.read(buffer)) != -1) {
                        if (len > 0) {
                            byte[] b = new byte[4];
                            byte[] strBuffer;
                            System.arraycopy(buffer, 0, b, 0, 4);
                            int type = byteArrayToInt(b);
                            if (type != 9 && type != 11) {
                                System.arraycopy(buffer, 4, b, 0, 4);
                                int length = byteArrayToInt(b);
                                strBuffer = new byte[length];
                                System.arraycopy(buffer, 8, strBuffer, 0, length);
                            } else {
                                strBuffer = new byte[20];
                                System.arraycopy(buffer, 4, strBuffer, 0, 20);
                            }
                            procReceivedMessage(type, new String(strBuffer));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void procReceivedMessage(int type, String message) {
            Log.i(TAG, "procReceivedMessage: Type: "+ type + "; Message: " + message);
            Itinerary iti = null;
            try {
                switch (type) {
                    case 9: // Login response
                        Log.i(TAG, "procReceivedMessage: Login message: " + message);
                        break;
                    case 11: // Logout response
                        Log.i(TAG, "procReceivedMessage: Logout message: " + message);
                        break;
                    case 1: // Shared file
                        iti = parseMessageToIti(message, type);
                        break;
                    case 3: // Invited file
                        iti = parseMessageToIti(message, type);
                        break;
                    case 5: // Merge request file
                        iti = parseMessageToIti(message, type);
                        break;
                    case 7: // Broadcasted file
                        iti = parseMessageToIti(message, type);
                        break;
                    case 15: // Error message
                        JSONObject msgObj = new JSONObject(message);
                        int reqNum = msgObj.getInt("request_number");
                        String errMsg = msgObj.getString("error_message");
                        Log.i(TAG, "procReceivedMessage: Error message of request " + reqNum + ": " + errMsg);
                        break;
                    default:
                        Log.i(TAG, "procReceivedMessage: Unrecognized message type");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SendMessageObject {
        String source_user;
        String[] target_users;
        String id;
        Itinerary plan;
    }

    public static boolean shareRequest(Itinerary iti, String[] target_users, String id) {
        SendMessageObject msgObj = new SendMessageObject();
        msgObj.source_user = MainActivity.user;
        msgObj.target_users = target_users;
        msgObj.id = id;
        msgObj.plan = iti;
        Gson gson = new Gson();
        String message = gson.toJson(msgObj);
        Log.i(TAG, "shareRequest: Message: " + message);
        return sendMessage(0, message);
    }

    public static boolean inviteRequest(Itinerary iti, String[] target_users, String id) {
        SendMessageObject msgObj = new SendMessageObject();
        msgObj.source_user = MainActivity.user;
        msgObj.target_users = target_users;
        msgObj.id = id;
        msgObj.plan = iti;
        Gson gson = new Gson();
        String message = gson.toJson(msgObj);
        Log.i(TAG, "inviteRequest: Message: " + message);
        return sendMessage(2, message);
    }

    public static boolean mergeRequest(Itinerary iti, String id) {
        SendMessageObject msgObj = new SendMessageObject();
        msgObj.source_user = MainActivity.user;
        msgObj.id = id;
        msgObj.plan = iti;
        Gson gson = new Gson();
        String message = gson.toJson(msgObj);
        Log.i(TAG, "mergeRequest: Message: " + message);
        return sendMessage(4, message);
    }

    public static boolean broadcastRequest(Itinerary iti, String id) {
        SendMessageObject msgObj = new SendMessageObject();
        msgObj.source_user = MainActivity.user;
        msgObj.id = id;
        msgObj.plan = iti;
        Gson gson = new Gson();
        String message = gson.toJson(msgObj);
        Log.i(TAG, "broadcastRequest: Message: " + message);
        return sendMessage(6, message);
    }

    public static boolean loginRequest(String user) {
        return sendMessage(8, user);
    }

    public static boolean logoutRequest(String user) {
        return sendMessage(10, user);
    }
}
