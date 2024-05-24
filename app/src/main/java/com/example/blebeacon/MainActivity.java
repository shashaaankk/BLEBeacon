package com.example.blebeacon;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private BluetoothLeScanner bluetoothLeScanner;
    BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private TextView display;
    private float distance = 0;
    private String url = null;
    private String uid = null;
    private  float temperature = 0;
    private  float voltage = 0;
    private static boolean isPressed = false;
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

        Button toscanButton = findViewById(R.id.button);
        display = findViewById(R.id.disp);

        toscanButton.setOnClickListener(v -> {
            isPressed = !isPressed;
            if(isPressed) {
                scanLeDevice();
            }
            else{
                display.setText("Press start to start reading the beacon, stop to stop reading.");
                distance = 0;
                url = null;
                uid = null;
                temperature = 0;
                voltage = 0;
                results.clear();
            }
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
            //results = identifyEddystoneFrame(scanRecord);
            results = identifyEddystoneFrames(scanRecord);
            updateDisplay();
        }
    };
    private void updateDisplay() {
        StringBuilder displayText = new StringBuilder();
        displayText.append("N.ID: ").append(results.get("uid")).append("\n");
        displayText.append("Distance: ").append(results.get("distance")).append("m\n");
        displayText.append("Temperature: ").append(results.get("temperature")).append("°C\n");
        displayText.append("Voltage: ").append(results.get("voltage")).append("mV\n");
        displayText.append("URL: ").append(results.get("url"));
        if(isPressed)
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
        //Log.d("DISTANCEINMETERS", String.valueOf(dist));
        dist = (float) (Math.round(dist*Math.pow(10,2))/Math.pow(10,2));
        return dist;
    }
    /*Reference Lecture Slide*/
    private Map<String, Object> identifyEddystoneFrames(byte[] scanRecord) {
        int index = 0;
        while (index < scanRecord.length) {
            int length = scanRecord[index] & 0xFF;
            if (length == 0) break;

            int type = scanRecord[index + 1] & 0xFF;
            if (type == 0x16) { // 0x16 indicates Service Data
                int serviceUUID = ((scanRecord[index + 3] & 0xFF) << 8) | (scanRecord[index + 2] & 0xFF);
                if (serviceUUID == 0xFEAA) { // Eddystone Service UUID
                    int frameType = scanRecord[index + 4] & 0xFF;
                    if (frameType == 0x00) { // UID Frame
                        byte[] namespaceId = new byte[10], instanceId = new byte[6];
                        int namespaceIndex = 0;
                        int instanceIndex = 0;
                        // Namespace ID (bytes 13-22 in the scanRecord)
                        for (int i = 13; i < 23; i++) {
                            namespaceId[namespaceIndex] = scanRecord[i];
                            namespaceIndex++;
                        }
                        // Instance ID (bytes 22-27 in the scanRecord)
                        for (int i = 23; i < 29; i++) {
                            instanceId[instanceIndex] = scanRecord[i];
                            instanceIndex++;
                        }
                        uid = getUID(namespaceId, instanceId);
                        Log.d("NEW", "URL: " + url);
                    } else if (frameType == 0x10) { //URL Frame
                        int indexURL = 0;
                        byte[] urlbytes = new byte[100];
                        for(int i=13;i<scanRecord.length;i++) {
                            urlbytes[indexURL] = scanRecord[i];
                            indexURL++;
                        }
                        url = getURL(urlbytes);
                        Log.d("NEW", "URL: " + url);
                    } else if (frameType == 0x20) { //TLM Frame
                        //BATTERY VOLTAGE
                        voltage = getVoltage(scanRecord);
                        Log.d("NEW", "Voltage: " + voltage + " mV");
                        //TEMPERATURE
                        temperature = getTemperature(scanRecord);
                        Log.d("NEW", "Temperature: " + temperature + "C");

                    }
                }
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
    private float[] window = new float[windowSize];
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
        return voltage;
    }

    public float getTemperature(byte[] scanRecord){
        byte[] Tempbytes = new byte[2];
        Tempbytes[0] = (byte) (scanRecord[15] & 0xFF);        // Lower byte //Big Endian
        Tempbytes[1] = (byte) (scanRecord[16]& 0xFF);         // Upper byte
        // Convert the fractional part to a decimal
        double fractionalDecimal = Tempbytes[1] / 256.0;      //LSB/256
        Log.d("DEBUGTEMPERATURE", "Raw MSB byte: " + String.format("%02X", Tempbytes[0]));
        Log.d("DEBUGTEMPERATURE", "Raw LSB byte: " + String.format("%02X", Tempbytes[1]));
        double temperature = Tempbytes[0] + fractionalDecimal;
        Log.d("TLMFrame", "Temperature: " + temperature + "C");
        return (float) temperature;
    }
    public String getUID(byte[] namespaceId, byte[] instanceId){
        String UID;
        // Convert to hex strings
        String namespaceIdHex = bytesToHex(namespaceId);
        String instanceIdHex = bytesToHex(instanceId);
        UID = namespaceIdHex + "\nI.ID: " + instanceIdHex;
        
        Log.d("UIDFrame", "Namespace ID: " + namespaceIdHex);
        Log.d("UIDFrame", "Instance ID: " + instanceIdHex);
        return UID;
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
            //sb.append(":");
        }
        //sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String getURL(byte[] urlbytes){
        String str = null ;
        char character;
        for (int i =0;i< urlbytes.length; i++){
            if(i == 0){
                if(urlbytes[i] == 0x00){
                    str = "http://www.";
                }
                else if(urlbytes[i] == 0x01){
                    str = "https://www.";
                }
                else if(urlbytes[i] == 0x02){
                    str = "http://";
                }
                else if(urlbytes[i] == 0x03){
                    str = "https://";
                }
            }
            else if((urlbytes[i] == 0x00) || (urlbytes[i] == 0x01)) {
                if(urlbytes[i] == 0x00) {
                    str = str + ".com";
                    break;
                }
                else if(urlbytes[i] == 0x01) {
                    str = str + ".org";
                    break;
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