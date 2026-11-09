package cn.routepilot.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import org.xmlpull.v1.*;

public class MainActivity extends Activity {
    private WebView web;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String pendingExport="";
    private LocationListener listener;
    private LocationManager lm;
    private LocationDiagnostics diagnostics;
    private volatile boolean admitted=false;
    private volatile boolean noticeBottomReached=false;
    private final Runnable refresh=new Runnable(){ public void run(){ if(web!=null){web.evaluateJavascript("window.updateStatus && updateStatus("+RouteService.status()+")",null);if(diagnostics!=null)web.evaluateJavascript("window.updateDiagnostics && updateDiagnostics("+diagnostics.status()+")",null);}handler.postDelayed(this,1000); }};
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);lm=(LocationManager)getSystemService(LOCATION_SERVICE);diagnostics=new LocationDiagnostics(this,handler);
        admitted=getPreferences(0).getBoolean("noticeAcceptedOnce",false);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(11,37,36));
        web=new WebView(this);root.addView(web);setContentView(root);
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout()|android.view.WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return insets;});
        else root.setFitsSystemWindows(true);
        web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setUserAgentString(web.getSettings().getUserAgentString()+" RoutePilot/1.4");
        web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"Native");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return true;}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){reportMapError(r,e.getErrorCode());}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){reportMapError(r,e.getStatusCode());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                Uri u=r.getUrl();if(!"route.local".equals(u.getHost()))return null;
                String path=u.getPath();if(path==null||path.contains(".."))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                try{return new WebResourceResponse(path.endsWith(".js")?"application/javascript":path.endsWith(".css")?"text/css":"text/html","UTF-8",getAssets().open(path.substring(1)));}catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
            }
        });
        web.loadUrl(admitted?"https://route.local/index.html":"https://route.local/startup.html");
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("noticeAccepted",admitted);out.putInt("noticeVersion",4);super.onSaveInstanceState(out);}
    private void reportMapError(WebResourceRequest request,int code){
        String host=request.getUrl().getHost();
        if(host==null||!(host.equals("tile.openstreetmap.org")||host.endsWith(".tile.openstreetmap.fr")||host.matches("t[0-7]\\.tianditu\\.gov\\.cn")))return;
        // Do not pass the URL: a Tianditu URL may contain the user's Key.
        runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.mapNetworkError && mapNetworkError("+JSONObject.quote(host)+","+code+")",null);});
    }
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);diagnostics.stop("离开页面，自检已停止");super.onPause();}
    @Override protected void onDestroy(){diagnostics.stop("自检已停止");handler.removeCallbacksAndMessages(null);if(listener!=null)lm.removeUpdates(listener);web.removeJavascriptInterface("Native");web.destroy();web=null;super.onDestroy();}
    private boolean permission(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void askLocation(){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);}
    private void start(String config){
        if(!admitted){toast("请先阅读并确认使用须知");return;}
        if(!permission()){askLocation();toast("请允许精确定位后再次点击开始");return;}
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){toast("请先开启手机定位服务");startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        if(ops.checkOpNoThrow("android:mock_location",android.os.Process.myUid(),getPackageName())!=AppOpsManager.MODE_ALLOWED){toast("请在开发者选项 → 选择模拟位置信息应用中，选择「路线助手」");openDevelopers();return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        try{startForegroundService(new Intent(this,RouteService.class).setAction("start").putExtra("config",config));}catch(Exception e){toast("无法启动："+e.getMessage());}
    }
    private boolean launchSettings(Intent... intents){for(Intent i:intents)try{startActivity(i);return true;}catch(ActivityNotFoundException|SecurityException ignored){}toast("系统未提供可打开的设置页面，请手动打开手机设置");return false;}
    private void openDevelopers(){
        Intent primary=new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        try{startActivity(primary);return;}catch(ActivityNotFoundException|SecurityException ignored){}
        Intent dashboard=new Intent().setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$DevelopmentSettingsDashboardActivity"));
        Intent legacy=new Intent().setComponent(new ComponentName("com.android.settings","com.android.settings.DevelopmentSettings"));
        toast("若未开启开发者选项，请在关于手机中连续点击版本号，再搜索开发者选项");
        launchSettings(dashboard,legacy,new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),new Intent(Settings.ACTION_SETTINGS));
    }
    private void openAppSettings(){launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())),new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),new Intent(Settings.ACTION_SETTINGS));}
    private void locate(){
        if(RouteService.running){toast("请先停止模拟，再获取真实位置");return;}
        if(!permission()){askLocation();return;}
        if(Build.VERSION.SDK_INT>=28 ? !lm.isLocationEnabled() : !(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))){startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}
        try{
            if(listener!=null)lm.removeUpdates(listener);
            listener=new LocationListener(){public void onLocationChanged(Location l){
                if(l.isFromMockProvider())return;
                lm.removeUpdates(this);if(web!=null)web.evaluateJavascript("centerAt("+l.getLatitude()+","+l.getLongitude()+")",null);
            }public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};
            boolean requested=false;
            for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})if(lm.isProviderEnabled(p)){lm.requestLocationUpdates(p,1000,0,listener);requested=true;}
            toast(requested?"正在获取真实位置，请在室外等待":"没有可用的定位服务");
            LocationListener captured=listener;
            handler.postDelayed(()->{lm.removeUpdates(captured);},30000);
        }catch(Exception e){toast("定位失败："+e.getMessage());}
    }
    public class Bridge {
        @JavascriptInterface public String noticeStatus(){return "{\"admitted\":"+admitted+",\"running\":"+RouteService.running+"}";}
        @JavascriptInterface public void noticeReachedEnd(){noticeBottomReached=true;}
        @JavascriptInterface public void enterApp(boolean accepted){runOnUiThread(()->{if(!admitted&&(!accepted||!noticeBottomReached)){toast("请先将免责声明滑到最底部，再勾选确认");return;}getPreferences(0).edit().putBoolean("noticeAcceptedOnce",true).apply();admitted=true;web.loadUrl("https://route.local/index.html");});}
        @JavascriptInterface public void closeIntro(){runOnUiThread(()->{if(admitted)web.loadUrl("https://route.local/index.html");else{if(RouteService.running){RouteService.message="已停止，已释放模拟定位";stopService(new Intent(MainActivity.this,RouteService.class));}finish();}});}
        @JavascriptInterface public void showGuide(){runOnUiThread(()->{diagnostics.stop("查看说明，自检已停止");web.loadUrl("https://route.local/startup.html#guide");});}
        @JavascriptInterface public void diagnose(){runOnUiThread(()->{if(!permission()){askLocation();toast("请允许精确定位后，再点击定位自检");return;}diagnostics.start();});}
        @JavascriptInterface public String load(){return getPreferences(0).getString("route","{}");}
        @JavascriptInterface public void save(String s){if(s.length()<1000000)getPreferences(0).edit().putString("route",s).apply();}
        @JavascriptInterface public void begin(String s){runOnUiThread(()->start(s));}
        @JavascriptInterface public void pause(){runOnUiThread(()->{if(RouteService.running)startService(new Intent(MainActivity.this,RouteService.class).setAction("pause"));});}
        @JavascriptInterface public void stop(){runOnUiThread(()->{if(RouteService.running)startService(new Intent(MainActivity.this,RouteService.class).setAction("stop"));});}
        @JavascriptInterface public void settings(){runOnUiThread(()->openDevelopers());}
        @JavascriptInterface public void mapHelp(){runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://console.tianditu.gov.cn/api/key")));}catch(Exception e){toast("请在浏览器打开天地图开发者控制台申请地图服务 Key");}});}
        @JavascriptInterface public void battery(){runOnUiThread(()->openAppSettings());}
        @JavascriptInterface public void locate(){runOnUiThread(()->MainActivity.this.locate());}
        @JavascriptInterface public void importGpx(){runOnUiThread(()->{if(!RouteService.running)startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),20);});}
        @JavascriptInterface public void exportGpx(String json){runOnUiThread(()->{pendingExport=json;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/gpx+xml").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"route.gpx"),21);});}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            if(request==20){
                JSONArray pts=new JSONArray();
                try(InputStream in=getContentResolver().openInputStream(data.getData())){
                    XmlPullParser parser=android.util.Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(in,null);int nodes=0;
                    for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
                        if(++nodes>200000)throw new IOException("GPX 文件过大");
                        if(event==XmlPullParser.START_TAG && ("trkpt".equals(parser.getName())||"rtept".equals(parser.getName()))){
                            double lat=Double.parseDouble(parser.getAttributeValue(null,"lat")),lng=Double.parseDouble(parser.getAttributeValue(null,"lon"));
                            if(!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>85||Math.abs(lng)>180)throw new IOException("GPX 坐标无效");
                            pts.put(new JSONArray().put(lat).put(lng));if(pts.length()>10000)throw new IOException("最多支持 10000 个路线点");
                        }
                    }
                }
                if(pts.length()<2)throw new IOException("未找到至少两个 trkpt / rtept 路线点");
                if(RouteService.running){toast("请停止后再导入");return;}
                web.evaluateJavascript("importPoints("+pts+")",null);
            }else if(request==21){
                JSONArray p=new JSONArray(pendingExport);StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"RoutePilot\" xmlns=\"http://www.topografix.com/GPX/1/1\"><trk><name>RoutePilot</name><trkseg>");
                for(int i=0;i<p.length();i++)s.append("<trkpt lat=\"").append(p.getJSONArray(i).getDouble(0)).append("\" lon=\"").append(p.getJSONArray(i).getDouble(1)).append("\"/>");
                s.append("</trkseg></trk></gpx>");try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(s.toString().getBytes(StandardCharsets.UTF_8));}toast("路线已导出");
            }
        }catch(Exception e){toast("文件操作失败："+e.getMessage());}
    }
}
