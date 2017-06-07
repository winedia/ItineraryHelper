package com.network.winedia.itineraryhelper;

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
}
