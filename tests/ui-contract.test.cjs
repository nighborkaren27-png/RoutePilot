const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const elements=new Map();function element(id){if(!elements.has(id))elements.set(id,{value:'',checked:false,disabled:false,textContent:'',innerHTML:'',style:{}});return elements.get(id);}
const initial={points:[[31.23,121.47],[31.231,121.471]],speed:8,target:2,loop:true,compatibility:true,tdtKey:'private-local-test-key'};
const calls=[];const context={document:{getElementById:element,querySelectorAll:()=>[element('compatibility'),element('speed')]},Native:{load:()=>JSON.stringify(initial),save:()=>{},begin:s=>calls.push(JSON.parse(s))},setTimeout:()=>1,clearTimeout:()=>{},confirm:()=>true,console};
vm.createContext(context);vm.runInContext(fs.readFileSync(path.join(__dirname,'../app/src/main/assets/app.js'),'utf8'),context);
// Native input.value always returns a string, including after assigning a number.
for(const e of elements.values())e.value=String(e.value);
assert.equal(element('compatibility').checked,true,'saved mode must restore');
vm.runInContext('begin()',context);assert.equal(calls[0].compatibility,true);assert(!('tdtKey' in calls[0]),'map key must not enter service payload');
context.diag={started:true,note:'自检结束',permission:true,enabled:true,selected:true,gps:{received:true,count:25,accuracy:1,age:0.1,mock:true},network:{received:false},visible:-1,used:-1};
vm.runInContext('updateDiagnostics(diag)',context);assert.match(element('diagnosticGps').textContent,/25 次回调/);assert.match(element('diagnosticGps').textContent,/系统标记模拟/);assert.match(element('diagnosticGnss').textContent,/不能据此断言卫星数为 0/);
context.diag.visible=12;context.diag.used=8;vm.runInContext('updateDiagnostics(diag)',context);assert.match(element('diagnosticGnss').textContent,/可见 12 颗/);assert.match(element('diagnosticGnss').textContent,/用于定位 8 颗/);
context.diag.gps.hasAccuracy=false;context.diag.gps.cached=true;context.diag.active=false;vm.runInContext('updateDiagnostics(diag)',context);assert.match(element('diagnosticGps').textContent,/精度字段 未知/);assert.match(element('diagnosticGps').textContent,/缓存位置/);assert.match(element('diagnosticNote').textContent,/检测已结束/);
vm.runInContext('begin(true)',context);assert.equal(calls[1].hold,true,'pre-position must start paused');
context.state={running:true,paused:false,distance:1,seconds:1,message:'running'};vm.runInContext('updateStatus(state)',context);assert.equal(element('compatibility').disabled,true);
context.state.running=false;vm.runInContext('updateStatus(state)',context);assert.equal(element('compatibility').disabled,false);
console.log('PASS: saved compatibility mode, service payload excludes map key, diagnostic callback display, missing GNSS is not zero, controls lock while active');
