'use strict';
let noticeSession={admitted:false,running:false};
try{noticeSession=JSON.parse(Native.noticeStatus());}catch(e){}
const noticeId=id=>document.getElementById(id);
function noticeTab(name){
  for(const id of ['notice','guide']){
    const chosen=id===name;noticeId(id+'Panel').hidden=!chosen;noticeId(id+'Tab').setAttribute('aria-selected',String(chosen));
  }
  noticeId('reading').scrollTop=0;
}
function noticeReady(){noticeId('enterButton').disabled=!noticeSession.admitted&&!noticeId('noticeRead').checked;}
function noticeEnter(){if(!noticeSession.admitted&&!noticeId('noticeRead').checked)return;Native.enterApp(noticeId('noticeRead').checked);}
if(noticeSession.admitted){
  noticeId('acceptance').hidden=true;noticeId('enterButton').textContent='返回主界面';noticeId('closeButton').textContent='关闭说明';
}
if(noticeSession.running){noticeId('activeReminder').hidden=false;if(!noticeSession.admitted)noticeId('activeReminder').textContent='当前模拟仍在运行。选择「不同意并退出」将同时停止模拟；阅读确认后可进入主界面操作。';}
noticeReady();noticeTab(location.hash==='#guide'?'guide':'notice');
