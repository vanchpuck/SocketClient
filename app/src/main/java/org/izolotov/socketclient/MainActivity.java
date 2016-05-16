package org.izolotov.socketclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;


public class MainActivity extends ActionBarActivity {

    /**
     * Андроид не позволяет выполнять взимодействие с сетью в гланом потоке программы, так что
     * создаем отдельный поток для обращения к серверу.
     */
    private class MakeRequestTask extends AsyncTask<Void, Integer, String> {

        private String host;
        private int port;
        private int timeout;

        public MakeRequestTask(String host, int port, int timeout) {
            this.host = host;
            this.port = port;
            this.timeout = timeout;
        }

        @Override
        protected String doInBackground(Void... params) {
            Socket socket = null;
            try{
                socket = new Socket(host, port);
                // Таймаут на чтение из сокета
                socket.setSoTimeout(timeout);
                // Писалка запроса
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // Читалка ответа
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println(requestField.getText().toString());
                return in.readLine();
            } catch (SocketTimeoutException e) {
                return "Сonnection is closed by timeout.";
            } catch(Exception e) {
                return e.getMessage();
            } finally {
                if(socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {}
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            responseField.setText(response);
        }
    }

    private TextView responseField;
    private EditText requestField;

    private MjpegView mv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        responseField = (TextView) findViewById(R.id.response_field);
        requestField = (EditText) findViewById(R.id.request_field);

        mv = (MjpegView) findViewById(R.id.mjpeg_view);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Обработка нажатия кнопок меню
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings :
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Функция вызывается при нажатии на кнопку Send
     * @param view
     */
    public void sendRequest(View view) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Получаем хост и порт из настроек
        String host = sharedPref.getString(getString(R.string.pref_host), null);
        if(host == null) {
            responseField.setText("Host should be specified");
            return;
        }
        String port = sharedPref.getString(getString(R.string.pref_port), null);
        if(port == null) {
            responseField.setText("Port should be specified");
            return;
        }
        String timeout = sharedPref.getString(getString(R.string.pref_timeout), null);
        if(timeout == null) {
            responseField.setText("Timeout should be specified");
            return;
        }

        responseField.setText("waiting for response...");

        try{
            MakeRequestTask task = new MakeRequestTask(host, Integer.valueOf(port), Integer.valueOf(timeout));
            task.execute();
        } catch(Exception e) {
            responseField.setText(e.getMessage());
        }


    }

    /**
     * Метод вызывается при сворачивании окна приложения
     */
    @Override
    public void onPause() {
        super.onPause();
        mv.stopPlayback();
    }

    /**
     * Метод вызывается при разворачивании окна приложения
     */
    @Override
    protected void onResume() {
        super.onRestart();
        mv.setUri(URI.create(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_cam_url), null)));
    }
}
