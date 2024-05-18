package com.example.blebeacon;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
public class BeaconActivity { }
public class ScanDispActivity extends ListActivity {
    // Interested UUIDs
    // Reference: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf?v=1715622654160
    private final UUID uuid1 = UUID.fromString("00000002-0000-0000-FDFD-FDFDFDFDFDFD");         //Weather: Service
    private final UUID uuid2 = UUID.fromString("00000001-0000-0000-FDFD-FDFDFDFDFDFD");         //FAN
    private final UUID uuidlighht = UUID.fromString ("10000001-0000-0000-FDFD-FDFDFDFDFDFD");
    //private final UUID uuid_temp = UUID.fromString("00002a1c-0000-1000-8000-00805f9b34fb");         //Characteristic Temp
    private final UUID uuid_temp = convertFromInteger(0x2A1C);
    //private final UUID uuid_temp = convertFromInteger(0x2A1F);
    private final UUID uuid_humidity = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb"); //Characteristic
    private final UUID uuid1_char = convertFromInteger(0x2902);
    private static final int MAX_VALUE = 65535;

    //Reference: https://medium.com/@shahar_avigezer/bluetooth-low-energy-on-android-22bc7310387a/
    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32),LSB);
    }
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;     //Scan Period of 10s
    private LeDeviceListAdapter mleDeviceListAdapter;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt bluetoothGatt;
    ListView listView;
    ArrayList<String> listItems;
    private Button ScanBtn;
    private Button disconnect;
    private TextView instruct;
    private int userInput;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        listItems = new ArrayList<>();
        listView = getListView();
        mleDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mleDeviceListAdapter);
        ScanBtn = findViewById(R.id.Scanbutton);
        instruct = findViewById(R.id.disp);

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
                Log.d(null, "Got Bluetooth Scanner");
                if (bluetoothLeScanner == null) {
                    Toast.makeText(this, "Unable to obtain a BluetoothLeScanner", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "BluetoothManager not available", Toast.LENGTH_SHORT).show();
        }
        //Scan
        ScanBtn.setOnClickListener(v -> {
            if (bluetoothLeScanner != null)
            {
                scanning = false;     //Stop Scanning!
                scanLeDevice();       //Start Scanning!
                Log.d(null, "Starting Scan!");
            }
        });
        //Disconnection
        disconnect = findViewById(R.id.transition);
        disconnect.setOnClickListener(v -> {
            disconnect();
            close();
            instruct.setText("Disconnected");
        });
    }
    // Adapter for holding devices found through scanning.
    // Reference: https://android.googlesource.com/example/bluetooth/le/DeviceScanActivity.java
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanDispActivity.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.list_devices, viewGroup, false);  // Inflate the custom layout
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            @SuppressLint("MissingPermission") final String deviceName = device.getName() == null ? "Unknown Device" : device.getName();
            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }
    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {
            String IPVSWeather = "F6:B6:2A:79:7B:5D";
            String IPVSLight = "F8:20:74:F7:2B:82";
            ScanFilter filterWeather = new ScanFilter.Builder()
                    .setDeviceAddress(IPVSWeather)
                    .build();
            ScanFilter filterLight= new ScanFilter.Builder()
                    .setDeviceAddress(IPVSLight)
                    .build();
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(filterWeather);
            filters.add(filterLight);
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(BleScanCallback);
                }
            }, SCAN_PERIOD);
            //scanning = true;
            //bluetoothLeScanner.startScan(BleScanCallback);
            bluetoothLeScanner.startScan(filters, settings, BleScanCallback);
        } else {
            Toast.makeText(this, "Stopping scan!", Toast.LENGTH_SHORT).show();
            scanning = false;
            bluetoothLeScanner.stopScan(BleScanCallback);
        }
    }
    /*
     * The interface is used to deliver BLE scan results.
     * When results are found, they are added to List Adapter to display to the user
     */
    private final ScanCallback BleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(null, "Scan Result Obtained");
            mleDeviceListAdapter.addDevice(result.getDevice());
            mleDeviceListAdapter.notifyDataSetChanged();
        }
    };
    @SuppressLint("MissingPermission")
    private void setupNotifications()
    {
        BluetoothGattCharacteristic temp = bluetoothGatt.getService(uuid1).getCharacteristic(uuid_temp);
        BluetoothGattDescriptor descriptor_t = temp.getDescriptor(uuid1_char);
        bluetoothGatt.setCharacteristicNotification(temp,true);
        descriptor_t.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor_t);
    }
    @SuppressLint("MissingPermission")
    private void writefan(int value){
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (value & 0xFF);        // Lower byte
        bytes[1] = (byte) ((value >> 8) & 0xFF); // Upper byte
        BluetoothGattCharacteristic shutter = bluetoothGatt.getService(uuid2).getCharacteristic(uuidlighht);
        shutter.setValue(bytes);
        bluetoothGatt.writeCharacteristic(shutter);
    }
    @SuppressLint("MissingPermission")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Toast.makeText(this, "Connecting!", Toast.LENGTH_SHORT).show();
        final BluetoothDevice device = mleDeviceListAdapter.getDevice(position);
        if (device == null) return;
        bluetoothGatt = device.connectGatt(this, false, gattCallback); // BTGatt instance : Conduct Client Operations. Auto connect is false

        if (scanning) {
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback) BleScanCallback);
            scanning = false;
        }
    }
    private int mConnectionState;
    private  String h = "";
    private  String t = "";
    /*Reference Android Connectivity Samples: GitHub*/
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                instruct.setText("CB: Connection to GATT server established");
                bluetoothGatt.discoverServices();
                mConnectionState = STATE_CONNECTED;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                instruct.setText(" CB: Could not Connect to GATT server");
                mConnectionState = STATE_DISCONNECTED;
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                instruct.setText("CB: GATT Services Discoverd");
                Log.d("service",gatt.getDevice().getName());
                Log.d("service",gatt.getServices().get(0).toString());
                if (gatt.getDevice().getAddress().equals("F6:B6:2A:79:7B:5D")) //checks for ipvsweather device
                    setupNotifications();
                if (gatt.getDevice().getAddress().equals("F8:20:74:F7:2B:82"))
                    writefan(userInput);
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("service","desriptor writer");
            BluetoothGattCharacteristic humidity = bluetoothGatt.getService(uuid1).getCharacteristic(uuid_humidity);
            BluetoothGattDescriptor descriptor_h = humidity.getDescriptor(uuid1_char);
            if (!descriptor_h.equals(descriptor))
            {
                bluetoothGatt.setCharacteristicNotification(humidity,true);
                descriptor_h.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor_h);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                instruct.setText("Fan Speed: " +characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0).toString());
                Log.d("service",characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0).toString());
            }
        }
        @SuppressLint("MissingPermission")//REM:Not used
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("service",characteristic.toString());
            BluetoothGattCharacteristic shutter = gatt.getService(uuid2).getCharacteristic(uuidlighht);
            gatt.readCharacteristic(shutter);
        }
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {

            super.onCharacteristicChanged(gatt, characteristic, value);
            if (characteristic.equals(bluetoothGatt.getService(uuid1).getCharacteristic(uuid_temp))){
                t = String.valueOf(decodeTemperatureCharacteristic(characteristic));
            }
            if (characteristic.equals(bluetoothGatt.getService(uuid1).getCharacteristic(uuid_humidity))){
                //Log.d("service","sensor update humidity"+String.valueOf(value));
                h = String.valueOf(convertBytesToFloatH(value));
            }
            instruct.setText("Temperature:" + t +"°C"+ "\n" + "Humidity:" + h+"%");
        }
    };
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            //Not Initialized
            return;
        }
        bluetoothGatt.disconnect();
    }
    @SuppressLint("MissingPermission")
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress; //MAC
    }

    public float convertBytesToFloatH(byte[] data) {
        //int value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF); // big-endian format
        int value = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);   // little-endian format
        return value / 100.0f;                                    // scaling
    }

    public double decodeTemperatureCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.d("TEMPERATURE", "Characteristic is null");
        }
        byte[] data = characteristic.getValue();
        if (data == null || data.length < 5) {
            Log.d("TEMPERATURE", "Invalid data length: " + (data == null ? "null" : data.length));
        }
        // Read the flags byte
        byte flags = data[0];
        boolean isFahrenheit = (flags & 0x01) != 0;
        // Decode IEEE 11073 FLOAT
        byte[] tempBytes = new byte[4];
        System.arraycopy(data, 1, tempBytes, 0, 4);
        ByteBuffer byteBuffer = ByteBuffer.wrap(tempBytes).order(ByteOrder.LITTLE_ENDIAN);
        // Extract magnitude (first three bytes as a 24-bit signed integer)
        int magnitude = byteBuffer.getShort() & 0xFFFF | (byteBuffer.get() << 16);
        // Extract exponent (last byte as a signed 8-bit integer)
        byte exponent = byteBuffer.get();
        // Calculate temperature based on magnitude and exponent
        double temperature = magnitude * Math.pow(10, exponent);
        // Log the temperature
        String unit = isFahrenheit ? "°F" : "°C";
        Log.d("THIS", "Temperature is " + String.format("%.2f", temperature) + unit);
        return temperature;
    }

}

