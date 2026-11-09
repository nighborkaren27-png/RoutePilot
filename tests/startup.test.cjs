const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const code=fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/startup.js'),'utf8');
function fixture(admitted=false){
 const els={};const calls=[];const get=id=>els[id]||(els[id]={hidden:false,disabled:false,checked:false,textContent:'',scrollTop:0,clientHeight:400,scrollHeight:2000,setAttribute(){},addEventListener(e,cb){this[e]=cb;}});
 const c={document:{getElementById:get},Native:{noticeStatus:()=>JSON.stringify({admitted,running:false}),noticeReachedEnd:()=>calls.push('end'),enterApp:()=>calls.push('enter')},window:{addEventListener(){}},location:{hash:''},requestAnimationFrame:f=>f()};vm.createContext(c);vm.runInContext(code,c);return {c,get,calls,run:s=>vm.runInContext(s,c)};
}
let f=fixture();assert(f.get('noticeRead').disabled);f.get('noticeRead').checked=true;f.run('noticeEnter()');assert.deepEqual(f.calls,[]);
f.run("noticeTab('guide')");f.get('reading').scrollTop=1600;f.run('noticeScroll()');assert(f.get('noticeRead').disabled,'guide bottom must not unlock disclaimer');
f.run("noticeTab('notice')");f.get('noticeRead').checked=false;f.get('reading').scrollTop=1600;f.run('noticeScroll()');assert(!f.get('noticeRead').disabled);assert(f.get('enterButton').disabled);
f.get('noticeRead').checked=true;f.run('noticeReady();noticeEnter()');assert.deepEqual(f.calls,['end','enter']);
f=fixture(true);assert(f.get('acceptance').hidden);assert(!f.get('enterButton').disabled);f.run('noticeEnter()');assert.deepEqual(f.calls,['enter']);
console.log('PASS: cannot bypass bottom gate by checkbox or guide scrolling; read-end and consent both required; accepted help reopens without a new gate');
