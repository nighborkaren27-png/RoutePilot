/* SPDX-License-Identifier: GPL-3.0-only
 * Provider compatibility references ZCShou / GoGoGo 1.12.3; see NOTICE.md.
 */
package cn.routepilot.app;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;
import org.json.*;
import java.util.*;

public class RouteService extends Service {
    public static volatile boolean running=false,paused=false;
    public static volatile double traveled=0,seconds=0,lat=0,lng=0;
    public static volatile long sentUpdates=0;
    public static volatile String message="尚未开始";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationManager manager;
    private final List<String> providers=new ArrayList<>();
    private PowerManager.WakeLock wake;
    private RouteEngine engine;
    private double speed,target;
    private boolean compatibility;
    private long last;
    private int ticks=0;
    public static JSONObject status(){
        JSONObject o=new JSONObject();
        try{o.put("running",running).put("paused",paused).put("distance",traveled).put("seconds",seconds).put("lat",lat).put("lng",lng).put("message",message).put("updates",sentUpdates);}catch(JSONException ignored){}
        return o;
    }
    @Override public void onCreate(){super.onCreate();manager=(LocationManager)getSystemService(LOCATION_SERVICE);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("route","路线模拟运行",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        String action=intent==null ? "stop" : intent.getAction();
        if("stop".equals(action)){message="已停止，已释放模拟定位";stopSelf();return START_NOT_STICKY;}
        if("pause".equals(action)){
            if(running){paused=!paused;last=SystemClock.elapsedRealtime();message=paused?"已暂停，保持当前位置":"正在模拟路线";getSystemService(NotificationManager.class).notify(1,notification());}
            else stopSelf();
            return START_NOT_STICKY;
        }
        if(running)return START_NOT_STICKY;
        try{
            startForeground(1,notification());
            JSONObject config=new JSONObject(intent.getStringExtra("config"));
            JSONArray arr=config.getJSONArray("points");double[][] p=new double[arr.length()][2];
            for(int i=0;i<p.length;i++){p[i][0]=arr.getJSONArray(i).getDouble(0);p[i][1]=arr.getJSONArray(i).getDouble(1);}
            engine=new RouteEngine(p,config.optBoolean("loop",true));
            compatibility=config.optBoolean("compatibility",false);
            speed=config.getDouble("speed")/3.6;target=config.getDouble("target")*1000;
            if(!Double.isFinite(speed)||speed<0.5/3.6||speed>30/3.6||!Double.isFinite(target)||target<0||target>100000)throw new IllegalArgumentException("速度或目标距离超出范围");
            if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))throw new IllegalStateException("请先开启手机定位服务");
            for(String name:new String[]{LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER}){
                boolean gps=LocationManager.GPS_PROVIDER.equals(name);
                manager.addTestProvider(name,!gps,gps,!gps,!gps,true,true,true,gps?Criteria.POWER_HIGH:Criteria.POWER_LOW,gps?Criteria.ACCURACY_FINE:Criteria.ACCURACY_COARSE);
                providers.add(name);manager.setTestProviderEnabled(name,true);
            }
            traveled=0;seconds=0;sentUpdates=0;paused=config.optBoolean("hold",false);running=true;message=compatibility?"正在模拟路线 · 卫星位置兼容":"正在模拟路线 · 标准模式";
            if(paused)message="预定位中：保持路线起点，准备好后点击继续开始移动";
            ticks=0;
            wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RoutePilot:route");wake.acquire();
            last=SystemClock.elapsedRealtime();handler.post(tick);
        }catch(Exception e){message=e instanceof SecurityException ? "请授权精确定位，并在开发者选项中选择「路线助手」为模拟位置应用" : "启动失败："+e.getMessage();stopSelf();}
        return START_NOT_STICKY;
    }
    private final Runnable tick=new Runnable(){public void run(){
        if(!running)return;
        try{
            long now=SystemClock.elapsedRealtime();double dt=(now-last)/1000.0;last=now;
            if(!paused){seconds+=dt;traveled+=speed*dt;}
            double end=target>0?target:Double.POSITIVE_INFINITY;
            if(!engine.loop)end=Math.min(end,engine.totalLength);
            boolean done=traveled>=end;traveled=Math.min(traveled,end);
            double[] p=engine.position(traveled);lat=p[0];lng=p[1];
            long wallTime=System.currentTimeMillis(),elapsedNanos=SystemClock.elapsedRealtimeNanos();
            for(String provider:providers){
                boolean gps=LocationManager.GPS_PROVIDER.equals(provider);
                Location l=new Location(provider);l.setLatitude(lat);l.setLongitude(lng);l.setAltitude(0);l.setAccuracy(compatibility?(gps?1:2):5);l.setSpeed(paused||done?0:(float)speed);l.setBearing((float)p[2]);l.setTime(wallTime);l.setElapsedRealtimeNanos(elapsedNanos);
                l.setVerticalAccuracyMeters(5);l.setSpeedAccuracyMetersPerSecond(0.1f);l.setBearingAccuracyDegrees(1);
                if(compatibility&&gps){Bundle extras=new Bundle();extras.putInt("satellites",7);l.setExtras(extras);}
                manager.setTestProviderLocation(provider,l);
            }
            sentUpdates++;
            if(done){message="已完成，已释放模拟定位";stopSelf();return;}
            if(ticks++%(compatibility?50:5)==0)getSystemService(NotificationManager.class).notify(1,notification());
            handler.postDelayed(this,compatibility?100:1000);
        }catch(Exception e){message="模拟已停止："+e.getMessage();stopSelf();}
    }};
    private Notification notification(){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause=PendingIntent.getService(this,1,new Intent(this,RouteService.class).setAction("pause"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,RouteService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,"route").setSmallIcon(cn.routepilot.app.R.drawable.ic_route).setContentTitle(paused?"路线助手 · 已暂停":"路线助手 · 模拟定位").setContentText(String.format(Locale.CHINA,"已移动 %.2f km · 点击返回控制",traveled/1000)).setContentIntent(open).setOngoing(true).addAction(0,paused?"继续":"暂停",pause).addAction(0,"停止",stop).build();
    }
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);running=false;paused=false;
        for(String p:providers)try{manager.removeTestProvider(p);}catch(Exception ignored){}
        providers.clear();if(wake!=null&&wake.isHeld())wake.release();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
