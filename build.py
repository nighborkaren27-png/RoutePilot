"""Build without Gradle. Python 3.8+, JDK 21, Android SDK platform 36 / build-tools 36.0.0.
Usage: python build.py --jdk PATH --platform PATH_TO_ANDROID_JAR --tools PATH
Uses a local development signing key. Do not use this key for publishing.
"""
from pathlib import Path
import argparse, subprocess, zipfile, shutil, os

root=Path(__file__).resolve().parent
p=argparse.ArgumentParser()
p.add_argument('--jdk',default=os.environ.get('JAVA_HOME'))
p.add_argument('--platform',required=True)
p.add_argument('--tools',required=True)
p.add_argument('--work-dir',help='Optional build and development-key directory')
args=p.parse_args()
if not args.jdk: p.error('Set JAVA_HOME or --jdk to a JDK 21 folder')
jdk=Path(args.jdk).resolve(); tools=Path(args.tools).resolve(); android=Path(args.platform).resolve()
work=Path(args.work_dir).resolve() if args.work_dir else root/'build-local';work.mkdir(parents=True,exist_ok=True)
classes=work/'classes';gen=work/'gen';dex=work/'dex'
for d in (classes,gen,dex):d.mkdir(exist_ok=True)
def run(*a):subprocess.run([str(x) for x in a],check=True,cwd=root)
ext='.exe' if os.name=='nt' else ''
def java(tool):return jdk/'bin'/(tool+ext)
src=root/'app/src/main'
manifest=work/'AndroidManifest.xml'
manifest.write_text((src/'AndroidManifest.xml').read_text(encoding='utf-8').replace('<manifest ', '<manifest package="cn.routepilot.app" ',1),encoding='utf-8')
run(tools/('aapt2'+ext),'compile','--dir',src/'res','-o',work/'res.zip')
run(tools/('aapt2'+ext),'link','-o',work/'base.apk','-I',android,'--manifest',manifest,'--java',gen,'--min-sdk-version','26','--target-sdk-version','36','--version-code','5','--version-name','1.4.0','-A',src/'assets',work/'res.zip')
sources=list((src/'java').rglob('*.java'))+list(gen.rglob('*.java'))
boot=str(tools/'core-lambda-stubs.jar')+os.pathsep+str(android)
run(java('javac'),'-J-Duser.language=en','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-bootclasspath',boot,'-d',classes,*sources)
with zipfile.ZipFile(work/'classes.jar','w',zipfile.ZIP_DEFLATED) as jar:
 for f in classes.rglob('*.class'):jar.write(f,f.relative_to(classes).as_posix())
run(java('java'),'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--min-api','26','--output',dex,work/'classes.jar')
shutil.copy2(work/'base.apk',work/'unsigned.apk')
with zipfile.ZipFile(work/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in dex.glob('*.dex'):z.write(f,f.name)
run(tools/('zipalign'+ext),'-f','-p','4',work/'unsigned.apk',work/'aligned.apk')
key=work/'development.p12'
if not key.exists():run(java('keytool'),'-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','routepilot','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=RoutePilot Development, O=Local, C=CN')
signer=tools/'lib/apksigner.jar'
apk=root.parent/'RoutePilot-1.4.0.apk'
run(java('java'),'-jar',signer,'sign','--ks',key,'--ks-key-alias','routepilot','--ks-pass','pass:android','--key-pass','pass:android','--out',apk,work/'aligned.apk')
run(java('java'),'-jar',signer,'verify','--verbose','--print-certs',apk)
print('APK:',apk)
