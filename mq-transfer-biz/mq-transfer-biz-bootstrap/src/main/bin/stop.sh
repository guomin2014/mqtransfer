#! /bin/sh


PORT=$1
BASEDIR=`dirname $0`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
PROJECT_NAME="@project.artifactId@"
MAIN_CLASS="$PROJECT_NAME";

if [ ! -n "$PORT" ]; then  
	echo $"Usage: $0 {port}"
	exit $FAIL
fi

echo "stoping application $PROJECT_NAME......"

jcpid=`ps -ef | grep -v "grep" | grep "$MAIN_CLASS" | grep "app.port=$PORT" | sed -n '1P' | awk '{print $2}'`

if [ -z $jcpid ]; then
    echo "$PROJECT_NAME is not started or has been stopped!"
else
    if [ $jcpid ]; then
	    [ -z $jcpid ] || kill -15 $jcpid
		    for i in {1..60}
			do
				jcpid=`ps -ef | grep -v "grep" | grep "$MAIN_CLASS" | grep "app.port=$PORT" | sed -n '1P' | awk '{print $2}'`
				if [ -z $jcpid ]; then
			    	echo "$PROJECT_NAME has been stopped!"
			    	break
			    else
			    	echo "stoping the application .. $i"
			    	sleep 1
			    fi
		    done
	fi
    jcpid=`ps -ef | grep -v "grep" | grep "$MAIN_CLASS" | grep "app.port=$PORT" | sed -n '1P' | awk '{print $2}'`
    if [ $jcpid ]; then
	    [ -z $jcpid ] || kill -9 $jcpid
	    [ $? -eq 0 ] && echo "Stop $PROJECT_NAME OK!" || echo "Stop $PROJECT_NAME Fail!"
	fi
fi