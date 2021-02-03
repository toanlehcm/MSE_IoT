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

public class MainActivity extends Activity {

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    UsbSerialPort port;

    GraphView graphTemperature, graphLightLevel;
    Button btnOne, btnTwo, btnThree, btnFour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // connect to ThingSpeak
        graphTemperature = findViewById(R.id.graphTemperature);
        graphLightLevel = findViewById((R.id.graphLightLevel));

        DataPoint[] dataPointTemp = new DataPoint[5];
        dataPointTemp[0] = new DataPoint(0, 30);
        dataPointTemp[1] = new DataPoint(1, 31);
        dataPointTemp[2] = new DataPoint(2, 31);
        dataPointTemp[3] = new DataPoint(3, 29);
        dataPointTemp[4] = new DataPoint(4, 29);

        LineGraphSeries<DataPoint> seriesTemp = new LineGraphSeries<>(dataPointTemp);

        showDataOnGraph(seriesTemp, graphTemperature);

        graphTemperature.getViewport().setMinY(0);
        graphTemperature.getViewport().setMaxY(60);
        graphTemperature.getViewport().setYAxisBoundsManual(true);

        setupBlinkyTimer();

        openUART("TEST");

        btnOne = findViewById(R.id.btnOne);
        btnTwo = findViewById(R.id.btnTwo);
        btnThree = findViewById(R.id.btnThree);
        btnFour = findViewById(R.id.btnFour);

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
    }

    // TODO: /** Called when the user touches the button */
//    public void sendMessageBtn1(View view) { openUART("1"); }
//
//    public void sendMessageBtn2(View view) {
//        openUART("2");
//    }
//
//    public void sendMessageBtn3(View view) {
//        openUART("3");
//    }
//
//    public void sendMessageBtn4(View view) {
//        openUART("4");
//    }

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

//                    SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
//                    Executors.newSingleThreadExecutor().submit(usbIoManager);
                } catch (Exception e) {

                }
            }
        }
    }

    // TODO: connect Thing Speak
    private void showDataOnGraph(LineGraphSeries<DataPoint> series, GraphView graph){
        if(graph.getSeries().size() > 0){
            graph.getSeries().remove(0);
        }
        graph.addSeries(series);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
    }

    private void sendDataToThingSpeak(String ID, String value){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();

        String apiURL = "https://api.thingspeak.com/update?api_key=EJA7L9H9IX7R3P0M&field1=" + value;

        Request request = builder.url(apiURL).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                String jsonString = response.body().string();
                Log.d("ABC", jsonString);//jsonString la chuoi lay tu server tren thing speak
            }
        });
    }

    private void getDataFromThingSpeak(int so_luong){
        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String apiURL = "https://api.thingspeak.com/channels/1281623/feeds.json?results=5"; //"https://api.thingspeak.com/channels/976324/feeds.json?results=5";
        Request request = builder.url(apiURL).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                String jsonString = response.body().string();
                try{
                    JSONObject jsonData = new JSONObject(jsonString);
                    JSONArray jsonArray = jsonData.getJSONArray("feeds");
                    double temp0 = jsonArray.getJSONObject(0).getDouble("field1");
                    double temp1 = jsonArray.getJSONObject(1).getDouble("field1");
                    double temp2 = jsonArray.getJSONObject(2).getDouble("field1");
                    double temp3 = jsonArray.getJSONObject(3).getDouble("field1");
                    double temp4 = jsonArray.getJSONObject(4).getDouble("field1");

                    LineGraphSeries<DataPoint> seriesTemp = new LineGraphSeries<>(new DataPoint[]
                            {   new DataPoint(0, temp0),
                                new DataPoint(1, temp1),
                                new DataPoint(2, temp2),
                                new DataPoint(3, temp3),
                                new DataPoint(4, temp4)
                            });

                    showDataOnGraph(seriesTemp, graphTemperature);


                }catch (Exception e){}
            }
        });
    }

    // TODO set time out
    private void setupBlinkyTimer(){
        Timer mTimer = new Timer();
        TimerTask mTask = new TimerTask() {
            @Override
            public void run() {
                //getDataFromThingSpeak();
                sendDataToThingSpeak("1","14");
            }
        };
        mTimer.schedule(mTask, 2000, 5000);
    }

    // TODO: microbit to phone
    String buffer = "";
//    @Override
//    public void onNewData(byte[] data) {
//        buffer += new String(data);
////        buffer = "12!123qwe#124";
//        TextView myAwesomeTextView = (TextView)findViewById(R.id.txtMessage);
//
//        int SoC = buffer.indexOf("!");
//        int EoC = buffer.indexOf("#");
//
//        if (SoC >= 0 && EoC > SoC) {
//            String cmd = buffer.substring(SoC + 1, EoC);
//            myAwesomeTextView.setText(cmd);
//            buffer = buffer.substring(EoC + 1, buffer.length());
//
//            Log.d("ABC", cmd);
//            Log.d("ABC", buffer);
//        }
//    }
}
