package com.example.lihongliang.trapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by lihongliang on 2017-06-04.
 */
public class login extends AppCompatActivity
{
    private Button b;
    private TextView t;
    private String name;
    public static user my;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        b = (Button)findViewById(R.id.dl);
        t = (TextView)findViewById(R.id.name);
        b.setOnClickListener(new CListner());
    }

    class CListner implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            name = t.getText().toString();
            my.name = name;
            TCPUtils.loginRequest(name);
            Intent it = new Intent(login.this, manager.class);
            startActivity(it);
        }
    }
}
