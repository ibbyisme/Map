package com.example.map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.provider.FontsContractCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.geofence.GeoFenceListener;
import com.baidu.geofence.model.DPoint;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.geofence.GeoFence;
import com.baidu.geofence.GeoFenceClient;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.inner.GeoPoint;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.entity.OnEntityListener;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.trace.model.OnTraceListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public LocationClient mlocation;
    public OnEntityListener entityListener;
    public GeoFenceClient geoFenceClient;
    public GeoFenceListener fenceListener;
    private static final String GEOFENCE_BROADCAST_ACTION = "com.example.geofence";
    private LatLngBounds.Builder builder=new LatLngBounds.Builder();
    private MapView mapView;
    private BaiduMap baiduMap;
    public IntentFilter filter;
    double latitude_main=0;
    double longitude_main=0;
    private LBSTraceClient lbstrace;
    public PendingIntent pendingIntent;
    public SmsManager smsManager;
    List<com.baidu.trace.model.LatLng> vertexer=new ArrayList<com.baidu.trace.model.LatLng>();
    List<LatLng> aps=new ArrayList<LatLng>();
    LatLng lat;
    com.baidu.trace.model.LatLng latt;
    Button circle,bangd,history,delet,sure,del,add;
    EditText editText;
    private List<LatLng> trackPoints=new ArrayList<>();
    BDLocationListener bdLocation=new MyBDAbstractLocationListener();
    boolean isfirst=true;
    public String phone=null;
    OnTraceListener traceListener;
    //CreateFenceResponse response;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView=(MapView)findViewById(R.id.map);
        editText=(EditText)findViewById(R.id.edit);
        sure=(Button)findViewById(R.id.sure) ;
        sure.setOnClickListener(new ButtonListen());
        bangd=(Button)findViewById(R.id.button);
        bangd.setOnClickListener(new ButtonListen());
        delet=(Button)findViewById(R.id.dice);
        delet.setOnClickListener(new ButtonListen());
        del=(Button)findViewById(R.id.des);
        del.setOnClickListener(new ButtonListen());
        circle=(Button)findViewById(R.id.kehu);
        circle.setOnClickListener(new ButtonListen());
        add=(Button)findViewById(R.id.add);
        add.setOnClickListener(new ButtonListen());
        pendingIntent=PendingIntent.getBroadcast(this,0,new Intent(),0);
        smsManager=SmsManager.getDefault();
        //history=(Button)findViewById(R.id.map_history);
        //history.setOnClickListener(new ButtonListen());
        filter=new IntentFilter();
        filter.addAction(GEOFENCE_BROADCAST_ACTION);
        registerReceiver(broadcastReceiver,filter);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest
        .permission.ACCESS_FINE_LOCATION,Manifest.permission.SEND_SMS,Manifest.permission.ACCESS_WIFI_STATE
        ,Manifest.permission.INTERNET},0x123);
        geoFenceClient=new GeoFenceClient(getApplicationContext());
        fenceListener=new GeoFenceListener() {
            @Override
            public void onGeoFenceCreateFinished(List<GeoFence> list, int i, String s) {
                if(i==GeoFence.ADDGEOFENCE_SUCCESS){
                    Toast.makeText(MainActivity.this,"围栏添加成功",Toast.LENGTH_LONG).show(); }
                else
                {Toast.makeText(MainActivity.this,"围栏添加失败",Toast.LENGTH_LONG).show();}
            }
        };
        geoFenceClient.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
        geoFenceClient.isHighAccuracyLoc(true);
        geoFenceClient.setGeoFenceListener(fenceListener);
        geoFenceClient.setActivateAction(GeoFenceClient.GEOFENCE_IN);

        //LocationClient
        mlocation=new LocationClient(getApplicationContext());
        mlocation.registerLocationListener(bdLocation);//注册监听函数
        initMap1();
        //-----------------------------------------
        MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLng(lat);
        baiduMap.animateMapStatus(mapStatusUpdate);

    }

    public void creatCircle(){
        DPoint centerPoint=new DPoint(latitude_main,longitude_main);
        System.out.println(latitude_main+"///"+longitude_main);
        LatLng center=new LatLng(latitude_main,longitude_main);

        geoFenceClient.addGeoFence(centerPoint,GeoFenceClient.BD09LL,800,"监控");
        baiduMap.addOverlay(new CircleOptions().center(center).radius(800).
                fillColor(0xAA0000FF).stroke(new Stroke(5,0xAA00ff00)));
    }
    public void creatPloy(){
        ArrayList<DPoint> points=new ArrayList<>();
        List<LatLng> latLngs=new ArrayList<>();
        for(int i=0;i<4;i++)
        {
            DPoint d=new DPoint(vertexer.get(vertexer.size()-1-i).latitude,vertexer.get(vertexer.size()-1-i).longitude);
            LatLng l=new LatLng(vertexer.get(vertexer.size()-1-i).latitude,vertexer.get(vertexer.size()-1-i).longitude);
            latLngs.add(l);
            points.add(d);
        }
        geoFenceClient.addGeoFence(points,GeoFenceClient.BD09LL,"多边形");
        baiduMap.addOverlay(new PolygonOptions().points(latLngs).
                fillColor(0xAAff0000).stroke(new Stroke(5,0xAA00FF00)));
    }
    private BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)){
                Bundle bundle=intent.getExtras();
                String customId=bundle.getString(GeoFence.BUNDLE_KEY_CUSTOMID);
                String fenceId=bundle.getString(GeoFence.BUNDLE_KEY_FENCEID);
                int status = bundle.getInt(GeoFence.BUNDLE_KEY_FENCESTATUS);
                switch (status) {
                    case GeoFence.INIT_STATUS_IN:
                        System.out.println("围栏初始状态:在围栏内--"+fenceId);
                        Toast.makeText(MainActivity.this,"正在围栏内",Toast.LENGTH_LONG).show();
                        smsManager.sendTextMessage(phone,null,"你进入了围栏",pendingIntent,null);
                        break;
                    case GeoFence.INIT_STATUS_OUT:
                        System.out.println("围栏初始状态:在围栏外"+fenceId);
                        smsManager.sendTextMessage(phone,null,"你离开了围栏",pendingIntent,null);
                        Toast.makeText(MainActivity.this,"正在围栏外",Toast.LENGTH_LONG).show();
                        break;
                    case GeoFence.STATUS_LOCFAIL:
                        System.out.println("定位失败,无法判定目标当前位置和围栏之间的状态");
                        Toast.makeText(MainActivity.this,"定位失败,无法判定目标当前位置和围栏之间的状态",Toast.LENGTH_LONG).show();
                        break;
                    case GeoFence.STATUS_IN:
                        System.out.println("进入围栏 "+fenceId);
                        Toast.makeText(MainActivity.this,"进入围栏",Toast.LENGTH_LONG).show();
                        smsManager.sendTextMessage(phone,null,"你进入了围栏",pendingIntent,null);
                        break;
                    case GeoFence.STATUS_OUT:
                        smsManager.sendTextMessage(phone,null,"你离开了围栏",pendingIntent,null);
                        System.out.println("离开围栏 "+fenceId);
                        Toast.makeText(MainActivity.this,"离开围栏",Toast.LENGTH_LONG).show();
                        break;
                    case GeoFence.STATUS_STAYED:
                        System.out.println("在围栏内停留超过10分钟 ");
                        Toast.makeText(MainActivity.this,"在围栏内停留超过10分钟",Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
                if (status != GeoFence.STATUS_LOCFAIL) {
                    if (!TextUtils.isEmpty(customId)) {
                       // sb.append(" customId: " + customId);
                    }
                }
                //String str = sb.toString();
                Message msg = Message.obtain();
                //msg.obj = str;
                msg.what = 2;
                handler.sendMessage(msg);
            }
        }
    };
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    //Toast.makeText(getApplicationContext(), "围栏添加成功",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    int errorCode = msg.arg1;
                    //Toast.makeText(getApplicationContext(), "添加围栏失败", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    //String statusStr = (String) msg.obj;Toast.makeText(MainActivity.this,statusStr,Toast.LENGTH_LONG);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
    }
    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }
    //----------------------------------------------------------
    public void inistart()
    {
        LocationClientOption moption=new LocationClientOption();
        moption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        moption.setOpenGps(true);
        moption.setCoorType("bd0911");
        moption.setScanSpan(2000);
        moption.setIsNeedAddress(true);
        moption.setIsNeedLocationDescribe(true);
        moption.setLocationNotify(false);
        moption.setIgnoreKillProcess(true);
        moption.setIsNeedLocationPoiList(true);
        moption.setIgnoreKillProcess(false);
        moption.setIsNeedAltitude(false);
        mlocation.setLocOption(moption);
    }
    public void initMap1()
    {
        baiduMap=mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);
        lbstrace=new LBSTraceClient(getApplicationContext());
        inistart();
        mlocation.start();
    }


    public void longClickAddMaker(){
        baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener(){
            @Override
            public void onMapLongClick(LatLng latLng){
                if(latLng!=null)
                {
                    longitude_main=latLng.longitude;
                    latitude_main=latLng.latitude;
                    Toast.makeText(MainActivity.this,""+
                            latitude_main+"/--/"+longitude_main,Toast.LENGTH_LONG).show();
                }
            }
        });
    }
//---------------------------------------------------------------------------------------
    class MylocationListener extends BDAbstractLocationListener{
        @Override
    public void onReceiveLocation(BDLocation bdLocation)
        {
            if(bdLocation==null||mapView==null)
            {
                return;
            }
            if(isfirst){
                LatLng latLng=null;
            }
        }
}
//---------------------------------------------------------------------------------------
    private  class MyBDAbstractLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation){

            MyLocationData locationData=new MyLocationData.Builder().
                    accuracy(bdLocation.getRadius()).
                    direction(bdLocation.getDirection())
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())
                    .build();
            //bdlocat=new BDLocation(bdLocation.getLatitude(),bdLocation.getLongitude());
            latt=new com.baidu.trace.model.LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
            if(isfirst){
                isfirst=false;
                setPosition2Center(baiduMap,bdLocation,true);
            }
        }
    }
    public void setPosition2Center(BaiduMap map, BDLocation bdLocation, Boolean isShowLoc) {
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(bdLocation.getRadius())
                .direction(bdLocation.getRadius()).latitude(bdLocation.getLatitude())
                .longitude(bdLocation.getLongitude()).build();
        //System.out.println(bdLocation);
        System.out.println(bdLocation.getLocType());
        map.setMyLocationData(locData);

        if (isShowLoc) {
            LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(18.0f);
            map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }
    //-------------------------------------------------------------------------//
    public class ButtonListen implements View.OnClickListener
    {
        //String entityName=getImei(MainActivity.this.getApplicationContext());
        public  void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.kehu:
                    longClickAddMaker();
                    break;
                case R.id.add:
                    LatLng latLng1=new LatLng(latitude_main,longitude_main);
                    vertexer.add(new com.baidu.trace.model.LatLng(latitude_main,longitude_main));
                    aps.add(latLng1);
                    break;
                case R.id.des:
                    System.out.println(phone);
                    if(phone==null)
                    {
                        Toast.makeText(MainActivity.this,"请先绑定接收安全信息的手机号码",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        creatCircle();
                    }

                    break;
                case R.id.sure:
                    System.out.println(phone);
                    if(phone==null)
                    {
                        Toast.makeText(MainActivity.this,"请先绑定接收安全信息的手机号码",Toast.LENGTH_LONG).show();
                    }
                    if(vertexer.size()<4)
                    {
                        Toast.makeText(MainActivity.this,"边界点太少，请添加边际点",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        creatPloy();
                    }
                    break;
                case R.id.dice:
                    baiduMap.clear();
                    break;
                case R.id.button:
                    phone=editText.getText().toString();
                    editText.setText("");
            }
        }
    }
}

