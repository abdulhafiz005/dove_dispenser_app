package com.example.bluetoothconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;


import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintSet;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    private View main_layout;
    private Handler handlerAnimation;
    private Handler retryHendler;
    public static Handler handler;
    public static Boolean retry;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;
    private static final String MY_PREFERENCES = "MyPreferences";

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    final int duration = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // UI Initialization
        SharedPreferences sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
//        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        final Button buttonToggle = findViewById(R.id.buttonToggle);
        final ImageView secondScreen = findViewById(R.id.imgViewSecondView);
        main_layout = findViewById(R.id.constrainLayout);
        handlerAnimation = new Handler(Looper.getMainLooper());
        retryHendler = new Handler();
//        final GifImageView secondScreen = findViewById(R.id.imgViewSecondView);
        buttonToggle.setEnabled(false);
        final WebView webView = findViewById(R.id.webView1);
        webView.getSettings().setJavaScriptEnabled(true);
        getSupportActionBar().hide();
        webView.setVisibility(View.GONE);
        buttonToggle.setVisibility(View.GONE);
        main_layout.post(runnable);

        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);
//        secondScreen.setVisibility(View.VISIBLE);
//        showSecondScreen(secondScreen, buttonToggle);

        // If a bluetooth device has been selected from SelectDeviceActivity
//        deviceName = getIntent().getStringExtra("deviceName");
//        deviceName = sharedPreferences.getString("deviceName", "");
        boolean containsKey = sharedPreferences.contains("deviceName");
//        if (deviceName != null) {
        if(containsKey){
            // Get the device address to make BT Connection
//            deviceAddress = getIntent().getStringExtra("deviceAddress");
            deviceAddress = sharedPreferences.getString("deviceAddress", "");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);
            buttonConnect.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);



            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);//, getApplicationContext(), MainActivity.this);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(false);
                                buttonConnect.setVisibility(View.GONE);
                                buttonToggle.setEnabled(true);
                                buttonToggle.setVisibility(View.VISIBLE);
                                toolbar.setVisibility(View.GONE);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonConnect.setVisibility(View.VISIBLE);
                                buttonToggle.setEnabled(false);
                                buttonToggle.setVisibility(View.GONE);
                                toolbar.setVisibility(View.VISIBLE);
                                break;
                        }
                        break;

//                    case MESSAGE_READ:
//                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
//                        switch (arduinoMsg.toLowerCase()) {
//                            case "led is turned on":
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
//                            case "led is turned off":
//                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
//                                break;
//                        }
//                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, ListKnownDevices.class);
                startActivity(intent);
            }
        });

        WebViewClient rclient = new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                // TODO Auto-generated method stub
                super.onPageStarted(view, url, favicon);
//                Log.d(TAG, "onPageStarted() returned: " + view.getUrl());
//                Toast.makeText(MainActivity.this, view.getUrl(), Toast.LENGTH_LONG).show();
                Uri uri = Uri.parse(url);
                boolean submit = Boolean.parseBoolean(uri.getQueryParameter("customer_posted"));
                if(submit){
                    connectedThread.write("1");
                    webView.setVisibility(View.GONE);
                    buttonToggle.setEnabled(true);
                    buttonToggle.setVisibility(View.VISIBLE);
                    showSecondScreen(secondScreen, buttonToggle);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        };
        webView.setWebViewClient(rclient);

        // Button to ON/OFF LED on Arduino Board
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String cmdText = "1";
//                String btnState = buttonToggle.getText().toString().toLowerCase();
//                switch (btnState) {
//                    case "turn on":
//                        buttonToggle.setText("Turn Off");
//                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
//                        cmdText = "<turn on>";
//                        break;
//                    case "turn off":
//                        buttonToggle.setText("Turn On");
//                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
//                        cmdText = "<turn off>";
//                        break;
//                }
                // Send command to Arduino board
//                connectedThread.write(cmdText);
                progressBar.setVisibility(View.VISIBLE);
                view.setVisibility(view.GONE);
                buttonConnect.setVisibility(view.GONE);
                webView.loadUrl("https://tryunilever.com/pages/dove-gym-activation");
                webView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void showSecondScreen(View imageView, View startBtn){
        imageView.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.GONE);
        final int[] count = {0};
        new CountDownTimer(10000, 300) { // 5000 = 5 sec

            public void onTick(long millisUntilFinished) {
                count[0]++;
                String word = String.valueOf(count[0]);
                String file = "screen2";
                String fileName = file+word;
                int resourceId = getResources().getIdentifier(fileName, "drawable", getPackageName());
//                Log.d(TAG, "onTick() returned: " + fileName + " : " + resourceId);
                imageView.setBackgroundResource(resourceId);
                if(count[0] == 9){
                    count[0] = -1;
                }
            }

            public void onFinish() {
                imageView.setVisibility(View.GONE);
                startBtn.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        private Context mContext;
        private Activity mActivity;
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address){//, Context context, Activity activity) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
//            mContext = context;
//            mActivity = activity;
//            if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(mActivity,
//                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
//                        1);
//            }
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(mActivity,
//                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
//                        2);
//
//            }
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

//        public void run() {
//            byte[] buffer = new byte[1024];  // buffer store for the stream
//            int bytes = 0; // bytes returned from read()
//            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    /*
//                    Read from the InputStream from Arduino until termination character is reached.
//                    Then send the whole String message to GUI Handler.
//                     */
//                    buffer[bytes] = (byte) mmInStream.read();
//                    String readMessage;
//                    if (buffer[bytes] == '\n'){
//                        readMessage = new String(buffer,0,bytes);
//                        Log.e("Arduino Message",readMessage);
//                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
//                        bytes = 0;
//                    } else {
//                        bytes++;
//                    }
//                    Log.d(TAG, "run() returned: " + createConnectThread);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    break;
//                }
//            }
//        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

//    Runnable runnable = new Runnable() {
//        int currentIndex = 0;
//
//        @Override
//        public void run() {
//            // Create the ObjectAnimator to change the background image
//            ObjectAnimator animator = ObjectAnimator.ofInt(main_layout, "backgroundResource", imageResources[currentIndex]);
//            animator.setDuration(duration);
//            animator.start();
//
//            currentIndex = (currentIndex + 1) % imageResources.length; // Move to the next image resource
//
//            // Schedule the next animation after the specified duration
//            handlerAnimation.postDelayed(this, duration);
//        }
//    };

    Runnable runnable = new Runnable() {
        int count = 0;

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    count++;
                    String word = String.valueOf(count);
                    String file = "screen1";
                    String fileName = file+word;
                    int resourceId = getResources().getIdentifier(fileName, "drawable", getPackageName());
                    main_layout.setBackgroundResource(resourceId);
                }
            });
            if(count == 2){
                count = -1;
            }

            // Schedule the next animation after the specified duration
            main_layout.postDelayed(this, duration);
        }
    };

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case 1:
//                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
//                            1);
//                }
//                break;
//            case 2:
//                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(MainActivity.this,
//                            new String[]{Manifest.permission.BLUETOOTH_SCAN},
//                            2);
//                }
//                break;
//        }
//    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//           ... //Device found
//            }
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                retry = false;
                Log.d(TAG, "onReceive() returned: connected broadcast recever " + retry);
            }
//            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//           ... //Done searching
//            }
//            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//           ... //Device is about to disconnect
//            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                retry = true;
                retryHendler.post(loopRunnable);
                Log.d(TAG, "onReceive() returned: Device disconnected");
            }
        }
    };

    private Runnable loopRunnable = new Runnable() {
        @Override
        public void run() {
//            deviceName = getIntent().getStringExtra("deviceName");
            SharedPreferences sharedPreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
//            deviceName = sharedPreferences.getString("deviceName", "");
            boolean containsKey = sharedPreferences.contains("deviceName");
//        if (deviceName != null) {
            if(containsKey){
//                createConnectThread.cancel();
                while (retry) {
                    try {
                        Log.d(TAG, "onReceive() returned: try broadcast recever " + retry);
//                        deviceAddress = getIntent().getStringExtra("deviceAddress");
                        deviceAddress = sharedPreferences.getString("deviceAddress", "");
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);//, getApplicationContext(), MainActivity.this);
                        createConnectThread.start();
                        retry = false;
                    }catch (Exception e) {
                        Log.d(TAG, "onReceive() returned: catch broadcast recever " + e.getMessage());
                    }
                }
            }

            // Schedule the next iteration after 5 seconds
            retryHendler.postDelayed(this, 5000);
        }
    };
}

