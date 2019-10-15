package com.valvers.dash;

import com.valvers.dash.BluetoothService;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DashActivity extends Activity {

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2; 
    
    private Context mApp;
    
	// Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    //private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBtService = null;
        
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		mApp = (DashApplication)getApplicationContext();
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
          	finish();
            return;
        }
        
        // Set the activity to full screen to get some more pixels :)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.main);
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            
        // Otherwise, setup the chat session
        } else {
            if (mBtService == null) 
            	setupDataLink();
        }
    }
    
    private void setupDataLink() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mBtService = new BluetoothService(this, mHandler);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mBtService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupDataLink();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }
    
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        private int mPacketByteNumber = 0;
        private int mRpm = 0;
        private int mBattery = 0;
        private int mKph = 0;
        private int mFuelPressure = 0;
        private int mOilPressure = 0;
        private int mAirTemp = 0;
        private int mOilTemp = 0;
        private int mWaterTemp = 0;
        
    	@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
            	for (int i=0; i<msg.arg1; i++) {
            		
            		int item = readBuf[i] & 0xFF;

                	switch(mPacketByteNumber)
                	{
                		case 0:
                		case 1:
                		case 2:
                		case 3:
                		case 4:
                		case 5:
                			if (item == 0xFE)
                				mPacketByteNumber++;
                			else
                				mPacketByteNumber = 0;
                			break;
                			
                		case 6:
                			mRpm = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 7:
                			mRpm |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 8:
                			mBattery = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 9:
                			mBattery |= item;
                			mPacketByteNumber++;
                			break;
                			
                		case 10:
                			mKph = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 11:
                			mKph |= item;
                			mPacketByteNumber++;
                			break;
                			
                		case 12:
                			mFuelPressure = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 13:
                			mFuelPressure |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 14:
                			mOilPressure = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 15:
                			mOilPressure |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 16:
                			mAirTemp = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 17:
                			mAirTemp |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 18:
                			mOilTemp = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 19:
                			mOilTemp |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 20:
                			mWaterTemp = item << 8;
                			mPacketByteNumber++;
                			break;
                		case 21:
                			mWaterTemp |= item;
                			mPacketByteNumber++;
                			break;
                		
                		case 22:
                			// Update the information if the data packet
                			// appears to be valid
                			if (item == 0x55)
                			{
                				// TODO: Get rid of this bodge which should
                				// prevent the RPM needle flickering
                				if (mRpm > 100)
                					((DashApplication)mApp).setRpm(mRpm);
                				
                				((DashApplication)mApp).setBattery(mBattery);
                				((DashApplication)mApp).setKph(mKph);
                				((DashApplication)mApp).setFuelPressure(mFuelPressure);
                				((DashApplication)mApp).setOilPressure(mOilPressure);
                				((DashApplication)mApp).setAirTemp(mAirTemp);
                				((DashApplication)mApp).setOilTemp(mOilTemp);
                				((DashApplication)mApp).setWaterTemp(mWaterTemp);
                			}
                			
                			mPacketByteNumber = 0;
                			break;
    
                		default:
                			mPacketByteNumber = 0;
                			break;
                				
                	}
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}