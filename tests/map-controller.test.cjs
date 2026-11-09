const assert=require('node:assert/strict');
const {MapController,definitions,validKey}=require('../app/src/main/assets/map-controller.js');
function fixture(){
  const pending=new Map();let next=1;const timers={set:(f)=>{const id=next++;pending.set(id,f);return id;},clear:id=>pending.delete(id)};
  const map={maxZoom:19,layers:new Set(),pane:null,setMaxZoom(z){this.maxZoom=z;},getPane(){return this.pane;},createPane(){return this.pane={style:{}};},removeLayer(l){this.layers.delete(l);}};
  const L={tileLayer:(url,options)=>({url,options,events:{},on(e,f){this.events[e]=f;return this;},addTo(m){m.layers.add(this);if(this.events.loading)this.events.loading();return this;},fire(e){if(this.events[e])this.events[e]();}})};
  const messages=[];const controller=new MapController(map,L,s=>messages.push(s),timers);
  const tick=()=>{const entry=pending.entries().next().value;assert(entry,'expected scheduled timer');pending.delete(entry[0]);entry[1]();};
  return {controller,map,messages,tick,pending};
}
{
 const f=fixture(),c=f.controller;c.apply('auto','');assert.equal(c.current,'hot');
 const old=c.layers[0];old.fire('tileerror');old.fire('load');f.tick();assert.equal(c.current,'osm');assert.equal(f.map.layers.size,1);
 const count=f.messages.length;old.fire('tileload');old.fire('load');assert.equal(f.messages.length,count,'stale events must be ignored');
 c.layers[0].fire('tileload');c.layers[0].fire('load');assert.match(f.messages.at(-1),/已加载/);assert.equal(f.pending.size,0);
}
{
 const f=fixture(),c=f.controller;c.apply('auto','');f.tick();f.tick();assert.equal(c.current,'osm');f.tick();assert.match(f.messages.at(-1),/失败或超时/);assert.equal(f.pending.size,0,'no infinite retry loop');
}
{
 const f=fixture(),c=f.controller;c.apply('tdt','');assert.equal(f.map.layers.size,0);assert.match(f.messages.at(-1),/需要有效 Key/);
 const key='0123456789abcdef0123456789abcdef';c.apply('auto',key);assert.equal(c.current,'tdt');assert.equal(c.layers.length,2);assert.equal(f.map.maxZoom,22);assert.equal(c.layers[0].options.maxNativeZoom,18);
 assert(c.layers[0].url.includes('/vec_w/wmts?'));assert(c.layers[1].url.includes('/cva_w/wmts?'));assert(c.layers[0].url.includes('tk='+key));assert(!f.messages.join().includes(key));
 c.apply('tdtSat',key);assert(c.layers[0].url.includes('/img_w/wmts?'));assert(c.layers[1].url.includes('/cia_w/wmts?'));assert.equal(f.map.layers.size,2);
 c.apply('hot','');assert.equal(f.map.layers.size,1);assert.equal(f.map.maxZoom,22);
 c.apply('invalid','');assert.equal(f.map.layers.size,0);assert.equal(f.pending.size,0);
}
{
 const f=fixture(),c=f.controller;c.apply('hot','');c.layers[0].fire('tileload');c.layers[0].fire('tileerror');c.layers[0].fire('load');assert.match(f.messages.at(-1),/部分加载/);assert.equal(c.current,'hot');
 c.apply('hot','');c.layers[0].fire('tileload');f.tick();assert.match(f.messages.at(-1),/部分地图/);
}
assert(validKey('0123456789abcdef0123456789abcdef'));assert(!validKey('https://bad/key'));assert.equal(definitions('tdt','').length,0);
assert.match(definitions('hot','')[0].options.attribution,/OSM France/);assert.match(definitions('osm','')[0].options.attribution,/OpenStreetMap contributors/);
console.log('PASS: provider definitions, missing Key, finite automatic fallback, stalled-request timeout, stale events, partial loads, source cleanup, attribution, no Key in status');
