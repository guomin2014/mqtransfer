#!/bin/sh


PORT=$1
if [ ! -n "$PORT" ]; then  
	PORT=8080
fi
BASEDIR=`dirname $0`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
PROJECT_NAME="@project.artifactId@";
MAIN_JAR="$PROJECT_NAME-@project.version@.jar";
MAIN_CLASS="com.gm.mqtransfer.bootstrap.TransferBootstrapApplication";
LOG_PATH="/export/logs/$PROJECT_NAME"
GC_PATH=$LOG_PATH/$PORT"-gc.log"
HS_ERR_PATH=$LOG_PATH/$PORT"-hs_err.log"
HEAP_DUMP_PATH=$LOG_PATH/$PORT"-heap_dump.hprof"
TEMP_PATH=$LOG_PATH/temp/
SUCCESS=0
FAIL=9
if [ ! -d $LOG_PATH ]; 
then     
    mkdir -p $LOG_PATH; 
fi
if [ ! -d $TEMP_PATH ]; 
then     
    mkdir -p $TEMP_PATH; 
fi

export CLASSPATH=.:${BASEDIR}/conf:${BASEDIR}/lib/*:${CLASSPATH}

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=$(which java)
    if [ ! -x "$JAVACMD" ] ; then
	  echo  "Error: JAVA_HOME is not defined correctly."
      exit $ERR_NO_JAVA
    fi
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "We cannot execute $JAVACMD"
  exit $ERR_NO_JAVA
fi

JAVA_MAJOR_VERSION=$("$JAVACMD" -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)

JAVA_OPT="${JAVA_OPT} -server -Xms512M -Xmx512M -Xss256K"

if [ -z "$JAVA_MAJOR_VERSION" ] || [ "$JAVA_MAJOR_VERSION" -lt "8" ] ; then
  JAVA_OPTS="${JAVA_OPTS} -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:-UseParNewGC"
else
  JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=10 -XX:InitiatingHeapOccupancyPercent=35 -XX:SoftRefLRUPolicyMSPerMB=0"
fi

if [ -e "$BASEDIR" ]; then
  JAVA_OPTS="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$GC_PATH -XX:+HeapDumpOnOutOfMemoryError -XX:ErrorFile=$HS_ERR_PATH -XX:HeapDumpPath=$HEAP_DUMP_PATH"
fi

JAVA_OPTS="${JAVA_OPTS} -cp ${CLASSPATH}"

# cd "$BASEDIR/lib";

echo "starting application $PROJECT_NAME......"
exec "$JAVACMD" $JAVA_OPTS \
				-Dapp.name="$PROJECT_NAME" \
				-Dapp.port="$PORT" \
				-Dbasedir="$BASEDIR" \
				-Djava.io.tmpdir=$TEMP_PATH \
				-Dsofa.ark.embed.enable=true \
				-Dsofa.ark.embed.static.biz.enable=true \
				-Dcom.alipay.sofa.ark.master.biz="$PROJECT_NAME" \
				-Dlogback.ContextSelector=com.alipay.sofa.ark.common.adapter.ArkLogbackContextSelector \
				$MAIN_CLASS \
				--server.port="$PORT" \
				&
				
for i in {1..10}
do
	jcpid=`ps -ef | grep -v "grep" | grep "$MAIN_CLASS" | grep "app.port=$PORT" | sed -n '1P' | awk '{print $2}'`
	if [ $jcpid ]; then
		echo "The $PROJECT_NAME start finished, PID is $jcpid"
		exit $SUCCESS
	else
		echo "starting the application .. $i"
		sleep 1
	fi
done
echo "$PROJECT_NAME start failure!"