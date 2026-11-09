'use strict';
let noticeSession={admitted:false,running:false};
try{noticeSession=JSON.parse(Native.noticeStatus());}catch(e){}
const noticeId=id=>document.getElementById(id);
let reachedEnd=!!noticeSession.admitted,currentTab='notice';
function noticeTab(name){
  currentTab=name;
  for(const id of ['notice','guide']){const chosen=id===name;noticeId(id+'Panel').hidden=!chosen;noticeId(id+'Tab').setAttribute('aria-selected',String(chosen));}
  noticeId('reading').scrollTop=0;noticeReady();requestAnimationFrame(noticeScroll);
}
function noticeScroll(){
  const area=noticeId('reading');
  if(currentTab==='notice'&&!reachedEnd&&area.clientHeight>0&&area.scrollTop+area.clientHeight>=area.scrollHeight-3){reachedEnd=true;Native.noticeReachedEnd();}
  noticeReady();
}
function noticeReady(){
  noticeId('noticeRead').disabled=!reachedEnd;
  noticeId('readingHint').textContent=reachedEnd?'已阅读至声明末尾，可勾选确认。':'请在「免责声明」页滑到正文最底部，解锁勾选。';
  noticeId('enterButton').disabled=!noticeSession.admitted&&(!reachedEnd||!noticeId('noticeRead').checked);
}
function noticeEnter(){if(!noticeSession.admitted&&(!reachedEnd||!noticeId('noticeRead').checked))return;Native.enterApp(noticeId('noticeRead').checked);}
if(noticeSession.admitted){noticeId('acceptance').hidden=true;noticeId('readingHint').hidden=true;noticeId('enterButton').textContent='返回主界面';noticeId('closeButton').textContent='关闭说明';}
if(noticeSession.running){noticeId('activeReminder').hidden=false;if(!noticeSession.admitted)noticeId('activeReminder').textContent='当前模拟仍在运行。选择「不同意并退出」将同时停止模拟；阅读确认后可进入主界面操作。';}
noticeId('reading').addEventListener('scroll',noticeScroll,{passive:true});window.addEventListener('resize',noticeScroll);
noticeTab(location.hash==='#guide'?'guide':'notice');
