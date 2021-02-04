package graph.monitoring.fpt;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jjoe64.graphview.GraphView;

import java.io.IOException;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

//connect server io.adafruit.com
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.nio.charset.Charset;

public class MainActivity extends Activity implements SerialInputOutputManager.Listener {

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;

    Button btnOne, btnTwo, btnThree, btnFour;
    MQTTService mqttService;
    Button btnOn, btnOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openUART("TEST");

        btnOne = findViewById(R.id.btnOne);
        btnTwo = findViewById(R.id.btnTwo);
        btnThree = findViewById(R.id.btnThree);
        btnFour = findViewById(R.id.btnFour);
        btnOn = findViewById(R.id.btnOnLed);
        btnOff = findViewById(R.id.btnOffLed);

        btnOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUART("1");
            }
        });

        btnTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUART("2");
            }
        });

        btnThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUART("3");
            }
        });

        btnFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openUART("4");
            }
        });

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDataMQTT("1");
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDataMQTT("0");
            }
        });

        // TODO: connect adafruit
        mqttService = new MQTTService(this);
        mqttService.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt", "Recieved: " + message.toString());
                if (message.toString().equals("1")) {
                    port.write(("5#").getBytes(), 1000);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView myAwesomeTextView = (TextView)findViewById(R.id.txtMessage);
                            myAwesomeTextView.setText("Turn on");
                        }
                    });

                } else {
                    port.write(("6#").getBytes(), 1000);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView myAwesomeTextView = (TextView)findViewById(R.id.txtMessage);
                            myAwesomeTextView.setText("Turn off");
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    // TODO: phone connect microbit by usb
    private void openUART(String message) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Log.d("UART", "UART is not available");

        }else {
            Log.d("UART", "UART is available");

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {

                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
                manager.requestPermission(driver.getDevice(), usbPermissionIntent);

                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));

                return;
            } else {

                port = driver.getPorts().get(0);
                try {
                    port.open(connection);
                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    port.write((message+"#").getBytes(), 1000);

                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                } catch (Exception e) {

                }
            }
        }
    }

    // TODO: microbit to phone
    String buffer = "";
    @Override
    public void onNewData(byte[] data) {
        buffer += new String(data);
        TextView myAwesomeTextView = (TextView)findViewById(R.id.txtMessage);

        int SoC = buffer.indexOf("!");
        int EoC = buffer.indexOf("#");

        if (SoC >= 0 && EoC > SoC) {
            String cmd = buffer.substring(SoC + 1, EoC);
            myAwesomeTextView.setText(cmd);
            buffer = buffer.substring(EoC + 1, buffer.length());

            if (cmd.equals("Turn on")) {
                sendDataMQTT("1");
            } else {
                sendDataMQTT("0");
            }

            Log.d("ABCD", cmd);
            Log.d("ABCDE", buffer);
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    // TODO: connect server adafruit
    private void sendDataMQTT(String data) {

        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);//update current status when open app.

        byte[] b = data.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        Log.d("Test", "Publish :" + msg);
        try {
            mqttService.mqttAndroidClient.publish("ToanLeThanh/f/BBC_LED2_Feed", msg);
        } catch (MqttException e) {
        }
    }
}
