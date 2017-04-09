package project.arduinoreader;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button search, stop, msg, bluetooth, close;
    TextView lat, lng, loc, intentdata;
    LocationManager locationManager;
    LocationListener locationListener;
    EditText editText;
    public static BluetoothDevice connectionDevice = null;
    BluetoothSocket socket;
    InputStream istream;
    OutputStream ostream;
    Thread readThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new mylocation();
        lat = (TextView) findViewById(R.id.textView);
        lng = (TextView) findViewById(R.id.textView2);
        loc = (TextView) findViewById(R.id.textView3);
        intentdata = (TextView) findViewById(R.id.intentdata);
        editText = (EditText) findViewById(R.id.editText);
        bluetooth = (Button) findViewById(R.id.bluetooth);
        close = (Button) findViewById(R.id.closeBluetooth);
        msg = (Button) findViewById(R.id.sendLocation);
        search = (Button) findViewById(R.id.searchLocation);
        stop = (Button) findViewById(R.id.stop);

        bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,BluetoothList.class);
                startActivityForResult(i,0);
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConnection();
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocation();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               stopLocation();
            }
        });

        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendsms();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0){
            if(data==null){
                intentdata.setText("No intentdata available");
            }else{
                intentdata.setText(data.toString());
            }

            if(connectionDevice!=null){
                try {
                    initiateConnection();
                    intentdata.setText("Waiting for data from Bluetooth");
                    listenData();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "error in connection!", Toast.LENGTH_SHORT).show();
                    Log.v("MainActivity",e.toString());
                }
            }
        }
    }

    private void initiateConnection() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        socket = connectionDevice.createRfcommSocketToServiceRecord(uuid);
        socket.connect();
        istream = socket.getInputStream();
        ostream = socket.getOutputStream();
    }

    private void listenData() {
        Toast.makeText(this, "listening for data!", Toast.LENGTH_SHORT).show();
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()){
                    Handler handler = new Handler();
                    try {
                        int bytecount = istream.available();
                        if(bytecount>0){
                            byte[] raw = new byte[bytecount];
                            istream.read(raw);
                            final String data = new String(raw,"UTF-8");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    intentdata.setText(data);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        readThread.start();
    }

    private void closeConnection() {
        if(readThread!=null && !readThread.isInterrupted())
            readThread.interrupt();
        try {
            if(istream!=null)
                istream.close();
            if(ostream!=null)
                ostream.close();
            if(socket!=null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendsms(){
        String text = (String) loc.getText();
        if(text.equals("")){
            Toast.makeText(MainActivity.this, "No location found yet!", Toast.LENGTH_SHORT).show();
            return;
        }
        String no = editText.getText().toString();
        if(no.equals("")){
            Toast.makeText(this, "No recipient found!", Toast.LENGTH_SHORT).show();
            return;
        }
//        SmsManager smsManager = SmsManager.getDefault();
//        smsManager.sendTextMessage(no,null,text,null,null);
        sendSMS(no,text);
    }

    private void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    public void startLocation(){
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Toast.makeText(this, "network: " + (isNetworkEnabled?"true":"false"), Toast.LENGTH_SHORT).show();
        String locationProvider = null;
        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(MainActivity.this, "No Location Provider Available!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            if (isGpsEnabled) {
                locationProvider = LocationManager.GPS_PROVIDER;
            } else if (locationProvider == null) {
                locationProvider = LocationManager.NETWORK_PROVIDER;
            }
        }

//        locationProvider = LocationManager.NETWORK_PROVIDER;

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

    }

    public void stopLocation(){
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    public class mylocation implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            lat.setText(String.valueOf(latitude));
            lng.setText(String.format("%.7f", longitude));       // "%1$.2f, %2$.2f, lat,lng"

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
            try {
                List<Address> myadd = geocoder.getFromLocation(latitude, longitude, 10);
                Address myfinaladd = myadd.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < myfinaladd.getMaxAddressLineIndex(); i++) {
                    sb.append(myfinaladd.getAddressLine(i));
                    sb.append("\n");
                }
                loc.setText(sb);
            } catch (IOException e) {
                e.printStackTrace();
                loc.setText(e.toString());
            }

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {
            //startLocation();
            Toast.makeText(MainActivity.this, "providerEnabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String s) {
            stopLocation();
            Toast.makeText(MainActivity.this, "Location Provider Disabled!", Toast.LENGTH_SHORT).show();
        }
    }
}
