package cn.routepilot.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import org.json.*;

/** Observes Android callbacks. Never fabricates GNSS status or location reports. */
final class LocationDiagnostics {
    private final Context context;
    private final LocationManager manager;
    private final Handler handler;
    private Location gps,network;
    private long sessionStartNanos;
    private int gpsCount,networkCount,visible=-1,used=-1;
    private boolean active=false,started=false;
    private String note="尚未自检";
    private final LocationListener listener=new LocationListener(){
        public void onLocationChanged(Location location){if(LocationManager.GPS_PROVIDER.equals(location.getProvider())){gps=new Location(location);gpsCount++;}else{network=new Location(location);networkCount++;}}
        public void onStatusChanged(String p,int s,Bundle b){}
        public void onProviderEnabled(String p){}
        public void onProviderDisabled(String p){}
    };
    private final GnssStatus.Callback gnss=new GnssStatus.Callback(){
        @Override public void onSatelliteStatusChanged(GnssStatus s){visible=s.getSatelliteCount();used=0;for(int i=0;i<visible;i++)if(s.usedInFix(i))used++;}
    };
    private final Runnable timeout=()->stop("自检结束（30 秒）");
    LocationDiagnostics(Context context,Handler handler){this.context=context;this.handler=handler;manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);}
    boolean permitted(){return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    void start(){
        stop("准备自检");gps=null;network=null;gpsCount=0;networkCount=0;visible=-1;used=-1;started=true;sessionStartNanos=SystemClock.elapsedRealtimeNanos();
        if(!permitted()){note="请授予精确定位权限后重试";return;}
        active=true;note="正在监听系统定位回调（30 秒），请保持此页面可见";
        try{
            boolean requested=false;
            for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})if(manager.isProviderEnabled(p)){manager.requestLocationUpdates(p,0,0,listener,Looper.getMainLooper());requested=true;}
            if(!requested){stop("定位源未开启，请检查手机定位开关");return;}
            if(!manager.registerGnssStatusCallback(gnss,handler))note="位置回调监听中；卫星状态注册未成功";
            handler.postDelayed(timeout,30000);
        }catch(Exception e){stop("无法自检："+e.getClass().getSimpleName());}
    }
    void stop(String reason){
        handler.removeCallbacks(timeout);
        if(active){try{manager.removeUpdates(listener);}catch(Exception ignored){}try{manager.unregisterGnssStatusCallback(gnss);}catch(Exception ignored){}note=reason;}
        active=false;
    }
    private JSONObject sample(Location l,int count)throws JSONException{
        JSONObject o=new JSONObject();o.put("count",count).put("received",l!=null);
        if(l!=null){o.put("hasAccuracy",l.hasAccuracy()).put("cached",l.getElapsedRealtimeNanos()<sessionStartNanos).put("accuracy",l.getAccuracy()).put("age",Math.max(0,(SystemClock.elapsedRealtimeNanos()-l.getElapsedRealtimeNanos())/1e9)).put("mock",l.isFromMockProvider());}
        return o;
    }
    JSONObject status(){
        JSONObject o=new JSONObject();
        try{
            AppOpsManager ops=(AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
            o.put("started",started).put("active",active).put("note",note).put("permission",permitted()).put("selected",ops.checkOpNoThrow("android:mock_location",android.os.Process.myUid(),context.getPackageName())==AppOpsManager.MODE_ALLOWED);
            o.put("enabled",manager.isProviderEnabled(LocationManager.GPS_PROVIDER)).put("gps",sample(gps,gpsCount)).put("network",sample(network,networkCount)).put("visible",visible).put("used",used);
        }catch(Exception e){try{o.put("note","读取自检状态失败");}catch(JSONException ignored){}}
        return o;
    }
}
