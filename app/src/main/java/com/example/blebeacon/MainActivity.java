package com.example.blebeacon;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<String[]> mPermissionLauncher;
    private boolean isBLUETOOTH_SCAN = false;
    private boolean isBLUETOOTH_ADVERTISE = false;
    private boolean isBLUETOOTH_CONNECT = false;
    private boolean isACCESS_COARSE_LOCATION = false;
    private boolean isACCESS_FINE_LOCATION = false;
    private Button toscanButton;
    private BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private TextView display;
    private float distance;
    private String url;
    private String uid;
    private float temperature;
    private float voltage;
    Map<String, Object> results = new HashMap<>();

    // Calibration Constants
    private static final int RSSI_AT_1M = -98;            //-17+(-41)
    private static final double PATH_LOSS_EXPONENT = 2.0; //FIGURE OUT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Stop previous scanning upon creation
        scanning = false;
        /*
         * Setting up BluetoothScanner for Discovering Nearby BLE Devices
         * Bluetooth Manager is used to obtain an instance of BluetoothAdapter
         * Bluetooth adapter represents local device bluetooth adapter, required for performing bluetooth tasks
         */
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                // Get the BluetoothLeScanner
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner == null) {
                    Toast.makeText(this, "Unable to obtain a BluetoothLeScanner", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "BluetoothManager not available", Toast.LENGTH_SHORT).show();
        }

        toscanButton = findViewById(R.id.button);
        display = findViewById(R.id.disp);

        toscanButton.setOnClickListener(v -> {
            Log.d("SCAN BUTTON", "Scan Button Pressed");
            scanLeDevice();
        });

        mPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            //Update Permissions
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_SCAN = result.get(android.Manifest.permission.BLUETOOTH_SCAN);
                }
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_ADVERTISE = result.get(android.Manifest.permission.BLUETOOTH_ADVERTISE);
                }
                if (result.get(android.Manifest.permission.BLUETOOTH_SCAN) != null) {
                    isBLUETOOTH_CONNECT = result.get(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
                if (result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION) != null) {
                    isACCESS_COARSE_LOCATION = result.get(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if (result.get(android.Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                    isACCESS_FINE_LOCATION = result.get(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }

            }
        });
        requestPermission();
    }

    /*Check if Permission has been Granted and update the boolean flags for permission*/
    private void requestPermission() {
        isBLUETOOTH_SCAN = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_ADVERTISE = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;

        isBLUETOOTH_CONNECT = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

        isACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        isACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<String>();

        if (!isBLUETOOTH_SCAN) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
        }
        if (!isBLUETOOTH_ADVERTISE) {
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if (!isBLUETOOTH_CONNECT) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (!isACCESS_COARSE_LOCATION) {
            permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!isACCESS_FINE_LOCATION) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Request any permissions that have not been granted
        if (!permissions.isEmpty()) {
            mPermissionLauncher.launch(permissions.toArray(new String[0]));
        }
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {
            Log.d("SCAN", "Scanning Now...");
            bluetoothAdapter.startLeScan(BleScanCallback);
        } else {
            scanning = false;
            bluetoothAdapter.stopLeScan(BleScanCallback);
        }
    }

    private final BluetoothAdapter.LeScanCallback BleScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //Log.d("BLEBEACONDATA", " RSSI: " + rssi + ",Byte Data: " + Arrays.toString(scanRecord));
            distance = calculateDistance(rssi);
            results.put("distance", distance);
            results = identifyEddystoneFrame(scanRecord);
            updateDisplay();
        }
    };
    private void updateDisplay() {
        StringBuilder displayText = new StringBuilder();
        displayText.append("UID: ").append(results.get("uid")).append("\n");
        displayText.append("Distance: ").append(results.get("distance")).append("m\n");
        displayText.append("Temperature: ").append(results.get("temperature")).append("°C\n");
        displayText.append("Voltage: ").append(results.get("voltage")).append("mV\n");
        displayText.append("URL: ").append(results.get("url"));//.append("\n");

        display.setText(displayText.toString());
    }
    //TODO: Calibrate
    /**
     * RSSI to meter
     * Distance = 10^((Measured Power - Instant RSSI)/(10*N)).
     * N is the constant for the environmental factor (2-4)
     * The measured power is the RSSI value at one meter
     */
    public float calculateDistance(int rssi) {
        float filteredRSSI = maFilter(rssi);
        float dist = (float) Math.pow(10, (RSSI_AT_1M - filteredRSSI) / (10 * PATH_LOSS_EXPONENT));
        Log.d("DISTANCEINMETERS", String.valueOf(dist));
        display.setText("RSSI: " + rssi + "dBm \n" );
        display.setText("Distance: " + String.format("%.2f", dist) + "m \n" );
        return dist;
    }
    /*Reference Lecture Slide*/
    private Map<String, Object> identifyEddystoneFrame(byte[] scanRecord) {
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index] & 0xFF;
            if (length == 0) break;
            int frameType = scanRecord[index + 11] & 0xFF;
            switch (frameType) {
                case 0x00:
                    //Log.d("EddystoneFrame", "UID Frame");
                    uid = getUID(scanRecord);
                    break;
                case 0x10:
                    //Log.d("EddystoneFrame", "URL Frame");
                    int indexURL = 0;
                    byte[] urlbytes = new byte[100];
                    for(int i=13;i<scanRecord.length;i++) {
                        urlbytes[indexURL] = scanRecord[i];
                        indexURL++;
                    }
                    url = getURL(urlbytes);
                    break;
                case 0x20:
                    //Log.d("EddystoneFrame", "TLM Frame");
                    //VTG
                    voltage = getVoltage(scanRecord);
                    //TEMPERATURE
                    temperature = getTemperature(scanRecord);
                    break;
            }
            index += length + 1;
        }
        results.put("uid", uid);
        results.put("url", url);
        results.put("voltage", voltage);
        results.put("temperature", temperature);

        return results;
    }

    //Moving Average Filter to filer RSSI
    private final int windowSize = 100; //Vary and Check
    private int head = 0;
    private float[] window;
    public float maFilter(int rssi){
        window[head] = rssi;
        head = (head+1)%windowSize;
        float sum = 0;
        for (float value:window){
            sum+=value;
        }
        return sum/windowSize;
    }
    public int getVoltage(byte[] scanRecord){
        byte[] Voltbytes = new byte[2];
        Voltbytes[0] = (byte) (scanRecord[13] & 0xFF); // MSB //Big Endian
        Voltbytes[1] = (byte) (scanRecord[14] & 0xFF); // LSB
        int voltage = ((Voltbytes[0] & 0xFF) << 8) | (Voltbytes[1] & 0xFF);
        Log.d("TLMFrame", "Voltage: " + voltage + " mV");
        display.setText("Distance: " + String.format("%.2f", voltage) + "m \n" );
        return voltage;
    }

    public float getTemperature(byte[] scanRecord){
        byte[] Tempbytes = new byte[2];
        Tempbytes[0] = (byte) (scanRecord[15] & 0xFF);        // Lower byte //Big Endian
        Tempbytes[1] = (byte) (scanRecord[16]& 0xFF);         // Upper byte
        // Convert the fractional part to a decimal
        double fractionalDecimal = Tempbytes[1] / 256.0;      //LSB/256
        Log.d("DEBUG", "Raw MSB byte: " + String.format("%02X", Tempbytes[0]));
        Log.d("DEBUG", "Raw LSB byte: " + String.format("%02X", Tempbytes[1]));
        double temperature = Tempbytes[0] + fractionalDecimal;
        Log.d("TLMFrame", "Temperature: " + temperature + "C");
        display.setText("Distance: " + String.format("%.2f", temperature) + "m \n" );
        return (float) temperature;
    }
    public String getUID(byte[] scanRecord){
        //Test
        return "MCL05";
    }

    private String getURL(byte[] urlbytes){
        String str = null ;
        char character;
        for (int i =0;i< urlbytes.length; i++){
            if(i == 0){
                if(urlbytes[i] == 0x00){
                    str = str + "http://www.";
                }
                else if(urlbytes[i] == 0x01){
                    str = str + "https://www.";
                }
                else if(urlbytes[i] == 0x02){
                    str = str + "http://";
                }
                else if(urlbytes[i] == 0x03){
                    str = str + "https://";
                }
            }
            else if((urlbytes[i] == 0x00) || (urlbytes[i] == 0x01)) {
                if(urlbytes[i] == 0x00) {
                    str = str + ".com/";
                }
                else if(urlbytes[i] == 0x01) {
                    str = str + ".org/";
                }
            }
            else {
                character = (char) urlbytes[i];
                str = str + character;
            }
        }
        return str;
    }
}