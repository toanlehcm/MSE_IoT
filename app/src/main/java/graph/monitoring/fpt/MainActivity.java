package graph.monitoring.fpt;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

//import com.hoho.android.usbserial.driver.UsbSerialDriver;
//import com.hoho.android.usbserial.driver.UsbSerialPort;
//import com.hoho.android.usbserial.driver.UsbSerialProber;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    //UsbSerialPort port;

    GraphView graphTemperature, graphLightLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        graphTemperature = findViewById(R.id.graphTemperature);
        //R.id cau lenh ho tro san cua java
        //graphTemperature: ten cua code java - con tro ben java
        //graphTemperature: ten ben html - doi tuong tao ra ben giao dien
        // muc dich maping voi nhau
        graphLightLevel = findViewById((R.id.graphLightLevel));

        // do not understand
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
        //graphTemperature.getViewport().setYAxisBoundsManual(true);
        // end do not understand

        setupBlinkyTimer();

//        openUART();
    }

//    private void openUART() {
//        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
//
//        if (availableDrivers.isEmpty()) {
//            Log.d("UART", "UART is not available");
//
//        }else {
//            Log.d("UART", "UART is available");
//
//            UsbSerialDriver driver = availableDrivers.get(0);
//            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
//            if (connection == null) {
//
//                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
//                manager.requestPermission(driver.getDevice(), usbPermissionIntent);
//
//                manager.requestPermission(driver.getDevice(), PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
//
//                return;
//            } else {
//
//                port = driver.getPorts().get(0);
//                try {
//                    port.open(connection);
//                    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
//
//
//                    //SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, this);
//                    //Executors.newSingleThreadExecutor().submit(usbIoManager);
//                } catch (Exception e) {
//
//                }
//            }
//        }
//    }

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

    private void getDataFromThingSpeak(){
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

    private void setupBlinkyTimer(){
        Timer mTimer = new Timer();
        TimerTask mTask = new TimerTask() {
            @Override
            public void run() {
                //getDataFromThingSpeak();
                sendDataToThingSpeak("1","12");
            }
        };
        mTimer.schedule(mTask, 2000, 5000);
    }


}
