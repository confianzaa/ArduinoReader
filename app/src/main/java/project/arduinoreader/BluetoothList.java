package project.arduinoreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothList extends AppCompatActivity {
    ListView btList;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> paired = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_list);

        btList = (ListView) findViewById(R.id.bluetoothList);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter==null){
            Toast.makeText(this, "No bluetooth adapter available!", Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()){
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,0);
        }else{
            showList();
        }

        btList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = paired.get(i);
                Toast.makeText(BluetoothList.this, "device selected: "+device.getName(), Toast.LENGTH_SHORT).show();
                MainActivity.connectionDevice = device;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0){
            showList();
        }
    }

    private void showList() {
        Set<BluetoothDevice> s = bluetoothAdapter.getBondedDevices();
        ArrayList<String> btListName = new ArrayList<>();
        ArrayAdapter ad = new ArrayAdapter(BluetoothList.this, android.R.layout.simple_dropdown_item_1line, btListName);
        btList.setAdapter(ad);
        for(BluetoothDevice device : s){
            paired.add(device);
//            btListName.add(device.getName()+" "+device.getAddress());
            ad.add(device.getName()+" "+device.getAddress());
        }
    }
}
