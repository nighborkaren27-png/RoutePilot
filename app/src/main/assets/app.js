'use strict';
let points=[],active=false,map,line,markers=[],cursor,toastTimer,baseMap;
const $=id=>document.getElementById(id);
function say(s){$('toast').textContent=s;$('toast').style.display='block';clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').style.display='none',4000);}
function valid(p){return Array.isArray(p)&&p.length>=2&&Number.isFinite(p[0])&&Number.isFinite(p[1])&&Math.abs(p[0])<=85&&Math.abs(p[1])<=180;}
function dist(a,b){const r=Math.PI/180,x=Math.sin((b[0]-a[0])*r/2),y=Math.sin((b[1]-a[1])*r/2);return 12742017.6*Math.asin(Math.min(1,Math.sqrt(x*x+Math.cos(a[0]*r)*Math.cos(b[0]*r)*y*y)));}
function config(){return {points,speed:Number($('speed').value),target:Number($('target').value),loop:$('loop').checked,compatibility:$('compatibility').checked,center:map? [map.getCenter().lat,map.getCenter().lng]:[31.2304,121.4737],zoom:map?map.getZoom():16,mapSource:$('mapSource').value,tdtKey:$('tdtKey').value.trim()};}
function persist(){Native.save(JSON.stringify(config()));}
function changeMap(){if(!baseMap)return;$('mapDiag').textContent='';baseMap.apply($('mapSource').value,$('tdtKey').value.trim());persist();}
function saveMapKey(){if($('tdtKey').value.trim()&&!RouteMaps.validKey($('tdtKey').value.trim()))return say('Key 格式无效，请粘贴完整密钥，不要填写网址或账号密码');changeMap();}
function mapNetworkError(host,code){
  if(!baseMap||!baseMap.current)return;
  const current=baseMap.current;
  if((current==='hot'&&!host.endsWith('.tile.openstreetmap.fr'))||(current==='osm'&&host!=='tile.openstreetmap.org')||((current==='tdt'||current==='tdtSat')&&!host.endsWith('.tianditu.gov.cn')))return;
  const errors={'-2':'无法解析地图服务器地址','-6':'无法连接地图服务器','-7':'连接中断','-8':'连接超时','-11':'安全连接失败','-16':'资源加载被系统阻止','401':'地图服务拒绝授权，请检查 Key','403':'地图服务拒绝访问，请检查授权或更换地图源','429':'请求额度或频率受限，请稍后重试'};
  $('mapDiag').textContent='连接诊断：'+(errors[String(code)]||('请求失败（'+code+'）'))+'。';
}
function draw(){
  let length=0;for(let i=1;i<points.length;i++)length+=dist(points[i-1],points[i]);
  if($('loop').checked&&points.length>1)length+=dist(points[points.length-1],points[0]);
  $('routeSummary').textContent=`已选 ${points.length} 点 · ${$('loop').checked?'闭环':'单程'} ${length.toFixed(0)} m`;
  if(!map)return;
  if(line)map.removeLayer(line);markers.forEach(m=>map.removeLayer(m));markers=[];
  const route=points.slice();if($('loop').checked&&route.length>1)route.push(route[0]);
  line=L.polyline(route,{color:'#328743',weight:4}).addTo(map);
  // Limit markers for large GPX files while retaining the full route geometry.
  const step=Math.max(1,Math.ceil(points.length/150));
  points.forEach((p,i)=>{if(i%step!==0&&i!==points.length-1)return;markers.push(L.marker(p,{icon:L.divIcon({className:'point',html:String(i+1),iconSize:[24,24],iconAnchor:[12,12]})}).addTo(map));});
}
function add(p){if(active)return say('请先停止模拟再编辑路线');if(!valid(p))return say('请输入有效的 WGS84 经纬度');if(points.length>=10000)return say('最多 10000 个路线点');points.push(p);draw();persist();}
function undo(){if(!active){points.pop();draw();persist();}}
function clearRoute(){if(!active&&confirm('清空当前路线？')){points=[];draw();persist();}}
function centerAt(a,b){if(!valid([a,b]))return;if(map)map.setView([a,b],17);$('lat').value=a.toFixed(6);$('lng').value=b.toFixed(6);persist();}
function jump(append){const a=$('lat').value.trim(),b=$('lng').value.trim();if(!a||!b)return say('请填写纬度和经度');const p=[Number(a),Number(b)];if(!valid(p))return say('坐标超出范围');centerAt(...p);if(append)add(p);}
function importPoints(p){if(active)return say('请先停止模拟');if(!Array.isArray(p)||p.length<2||p.length>10000||!p.every(valid))return say('路线无效：需要 2–10000 个有效坐标点');points=p;draw();if(map)map.fitBounds(L.latLngBounds(points),{padding:[25,25],maxZoom:18});persist();say(`已导入 ${p.length} 个路线点`);}
function pastePoints(){const p=$('coords').value.trim().split(/\n+/).map(s=>s.trim().split(/[,，\s]+/).map(Number));importPoints(p);}
function exportRoute(){if(points.length<2)return say('请先选择至少两个路线点');Native.exportGpx(JSON.stringify(points));}
function begin(){
  const c=config();if(points.length<2)return say('请先选择至少两个路线点');
  if(!$('speed').value.trim()||!Number.isFinite(c.speed)||c.speed<0.5||c.speed>30)return say('速度范围是 0.5–30 km/h');
  if(!$('target').value.trim()||!Number.isFinite(c.target)||c.target<0||c.target>100)return say('目标距离范围是 0–100 km');
  persist();Native.begin(JSON.stringify({points:c.points,speed:c.speed,target:c.target,loop:c.loop,compatibility:c.compatibility}));
}
function updateDiagnostics(s){
  if(!s.started)return;
  const sample=x=>!x||!x.received?'尚未收到位置回调':`${x.count} 次回调 · 精度字段 ${Number(x.accuracy).toFixed(1)} m · 数据距今 ${Number(x.age).toFixed(1)} s · ${x.mock?'系统标记模拟':'系统未标记模拟'}`;
  $('diagnosticNote').textContent=s.note;
  $('diagnosticPermission').textContent=`精确定位权限：${s.permission?'已授予':'未授予'} · GPS 开关：${s.enabled?'开启':'关闭'} · 模拟位置应用：${s.selected?'已选本应用':'未选本应用'}`;
  $('diagnosticGps').textContent='GPS：'+sample(s.gps);$('diagnosticNetwork').textContent='网络定位：'+sample(s.network);
  $('diagnosticGnss').textContent=s.visible<0?'系统卫星状态：尚未收到系统回调，不能据此断言卫星数为 0':`系统卫星状态：可见 ${s.visible} 颗 · 用于定位 ${s.used} 颗（与兼容模式的附加数字无关）`;
}
function updateStatus(s){
  active=!!s.running;$('state').textContent=active?(s.paused?'已暂停':'运行中'):'待开始';$('message').textContent=s.message;
  $('distance').innerHTML=(s.distance/1000).toFixed(2)+' <span class="unit">km</span>';
  const t=Math.floor(s.seconds);$('time').textContent=(Math.floor(t/60)).toString().padStart(2,'0')+':'+(t%60).toString().padStart(2,'0');
  $('start').disabled=active;$('pause').disabled=!active;$('stop').disabled=!active;$('pause').textContent=s.paused?'继续':'暂停';document.querySelectorAll('.edit').forEach(e=>e.disabled=active);
  if(map&&active){if(!cursor)cursor=L.circleMarker([s.lat,s.lng],{radius:8,color:'#fff',weight:3,fillColor:'#2979ff',fillOpacity:1}).addTo(map);else cursor.setLatLng([s.lat,s.lng]);}
  else if(cursor&&map){map.removeLayer(cursor);cursor=null;}
}
let saved={};try{saved=JSON.parse(Native.load());}catch(e){}
if(Array.isArray(saved.points)&&saved.points.length<=10000&&saved.points.every(valid))points=saved.points;
if(Number.isFinite(saved.speed))$('speed').value=saved.speed;if(Number.isFinite(saved.target))$('target').value=saved.target;if(typeof saved.loop==='boolean')$('loop').checked=saved.loop;
if(typeof saved.compatibility==='boolean')$('compatibility').checked=saved.compatibility;
if(['auto','hot','osm','tdt','tdtSat'].includes(saved.mapSource))$('mapSource').value=saved.mapSource;
if(typeof saved.tdtKey==='string'&&saved.tdtKey.length<=128)$('tdtKey').value=saved.tdtKey;
try{
  map=L.map('map',{zoomControl:true}).setView(valid(saved.center)?saved.center:[31.2304,121.4737],Number.isFinite(saved.zoom)?Math.max(2,Math.min(19,saved.zoom)):16);
  baseMap=new RouteMaps.MapController(map,L,text=>$('mapError').textContent=text);changeMap();
  map.on('click',e=>add([e.latlng.lat,((e.latlng.lng+180)%360+360)%360-180]));map.on('moveend',persist);
}catch(e){$('mapError').textContent='地图不可用，请通过坐标或 GPX 设置路线。';}
draw();
