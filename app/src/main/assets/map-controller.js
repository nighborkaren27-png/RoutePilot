/* Map requests are limited to the currently visible area; no prefetch or bulk download. */
(function(root){
  'use strict';
  const names={hot:'备用地图（OSM France / HOT）',osm:'OpenStreetMap 标准地图',tdt:'天地图 · 道路',tdtSat:'天地图 · 卫星'};
  const validKey=key=>/^[A-Za-z0-9_.-]{8,128}$/.test(key);
  function definitions(id,key){
    const osmCredit='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>';
    if(id==='osm')return [{url:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',options:{maxZoom:22,maxNativeZoom:19,attribution:osmCredit}}];
    if(id==='hot')return [{url:'https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png',options:{maxZoom:22,maxNativeZoom:19,subdomains:'abc',attribution:osmCredit+' | Style: HOT | Tiles: <a href="https://www.openstreetmap.fr/usage/">OSM France</a>'}}];
    if((id==='tdt'||id==='tdtSat')&&validKey(key)){
      const kinds=id==='tdt'?['vec','cva']:['img','cia'];
      return kinds.map((kind,i)=>({url:'https://t{s}.tianditu.gov.cn/'+kind+'_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER='+kind+'&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk='+encodeURIComponent(key),options:{maxZoom:22,maxNativeZoom:18,minZoom:1,subdomains:'01234567',attribution:i===0?'© <a href="https://www.tianditu.gov.cn/">天地图</a>':'',pane:i===0?'tilePane':'mapLabels'}}));
    }
    return [];
  }
  class MapController{
    constructor(map,leaflet,status,timers){this.map=map;this.L=leaflet;this.status=status;this.timers=timers||{set:(fn,ms)=>setTimeout(fn,ms),clear:id=>clearTimeout(id)};this.layers=[];this.generation=0;this.timer=null;this.current=null;}
    clear(){this.generation++;if(this.timer!==null)this.timers.clear(this.timer);this.timer=null;this.layers.forEach(l=>this.map.removeLayer(l));this.layers=[];}
    apply(mode,key){
      this.clear();this.key=String(key||'').trim();this.mode=mode;this.current=null;
      if(!(mode==='auto'||Object.prototype.hasOwnProperty.call(names,mode))){this.status('未知地图源，请重新选择。');return;}
      if((mode==='tdt'||mode==='tdtSat')&&!validKey(this.key)){this.status('天地图需要有效 Key。请展开下方「天地图 Key 设置」，填写后点击保存并加载。');return;}
      this.queue=mode==='auto'?(validKey(this.key)?['tdt','hot','osm']:['hot','osm']):[mode];this.index=0;this.start();
    }
    start(){
      this.clear();const token=this.generation;this.current=this.queue[this.index];const name=names[this.current];const defs=definitions(this.current,this.key);
      this.status('正在连接 '+name+' …');
      this.map.setMaxZoom(defs[0].options.maxZoom);
      const labelPane=this.map.getPane('mapLabels')||this.map.createPane('mapLabels');labelPane.style.zIndex='250';labelPane.style.pointerEvents='none';
      let successes=0,failures=0,transition=false;
      const alive=()=>token===this.generation;
      const finish=()=>{if(this.timer!==null)this.timers.clear(this.timer);this.timer=null;};
      const fail=()=>{
        if(!alive()||transition)return;transition=true;finish();
        if(this.index+1<this.queue.length){this.status(name+' 未能加载，正在切换备用源 …');this.timer=this.timers.set(()=>{if(alive()){this.index++;this.start();}},0);}
        else this.status(name+' 加载失败或超时。请切换地图源；若使用天地图，请检查 Key、授权与额度。也可先用坐标或 GPX 设置路线。');
      };
      const watch=()=>{finish();this.timer=this.timers.set(()=>{if(!alive())return;if(successes===0)fail();else this.status(name+' 已加载部分地图；仍有图片未响应，可点击「重新加载」。');},12000);};
      defs.forEach((def,index)=>{
        const layer=this.L.tileLayer(def.url,Object.assign({crossOrigin:false},def.options));this.layers.push(layer);
        if(index===0){
          layer.on('loading',()=>{if(!alive())return;successes=0;failures=0;watch();});
          layer.on('tileload',()=>{if(!alive()||transition)return;successes++;});
          layer.on('tileerror',()=>{if(!alive()||transition)return;failures++;});
          layer.on('load',()=>{if(!alive()||transition)return;finish();if(successes===0&&failures>0)fail();else this.status(name+(failures>0?' 已部分加载，可点击「重新加载」重试缺失图片。':' 已加载。'));});
        }else{
          layer.on('tileerror',()=>{if(alive())this.status(name+' 的文字标注加载失败，请检查 Key 或重新加载。');});
        }
        layer.addTo(this.map);
      });
      // Covers requests that hang before a loading/error event reaches the page.
      if(this.timer===null)watch();
    }
  }
  const api={MapController,definitions,validKey};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.RouteMaps=api;
})(typeof window!=='undefined'?window:this);
