#PROG=/home/talex/harmony-6.0-jre-991881/bin/java
#PROG=/usr/lib/jvm/jdk1.7.0/bin/java
PROG=java
#Defaults ....
nodes=1

reads=50
bench="bank"
objects=10
threads=1
transactions=100

debug=false
verbose=5
instrument=false
sanity=false
nestingmodel="flat"
inner_delay="true"
outer_delay="false"

calls=1
nesting=8
myIP=`ifconfig lo | grep "inet addr:" | awk -F: '{print($2)}' | sed "s/ /:/g" | awk -F: '{print($1)}'`
parentIP=$myIP
machines=1
machine_index=0

time_out=10
time_length=10
time_link=1
time_call=0
comm="aleph.comm.tcp.CommunicationManager"

freeCore=0
java_args=""

#Parameters ....
valid_mode=false
while getopts :c:m:n:t:b:o:r:f:x:v:isdM:I:T:L:K:C:N:zZj: option
do
	case "$option" in
	T)	time_out="$OPTARG";;
	L)	time_length="$OPTARG";;
	K)	time_link="$OPTARG";;
	C)	time_call="$OPTARG";;
	f)	freeCore="$OPTARG";;
        c)      calls="$OPTARG";;
	s)	sanity=true;;
	i)	instrument=true;;
	n)	nodes="$OPTARG";;
	r)	reads="$OPTARG";;
	b)	bench="$OPTARG";;
	o)	objects="$OPTARG";;
	t)	threads="$OPTARG";;
	x)	transactions="$OPTARG";;
	v)	verbose="$OPTARG";;
	d)	debug=true;;
	N)  nestingmodel="$OPTARG";;
	I)	machine_index="$OPTARG";;
	M)	machines="$OPTARG"
		parentIP="10.1.1.24"	#default parent is mario
		;;
	z)	inner_delay="true";;
	Z)	outer_delay="true";;
	j)	java_args="$OPTARG";;
	m)	case "$OPTARG" in
            dtl)    valid_mode=true
                    dir="edu.vt.rt.hyflow.core.dir.dtl2.TrackerDirectory"
                    context="edu.vt.rt.hyflow.core.tm.dtl.Context"
                    cm="edu.vt.rt.hyflow.core.cm.policy.Default"
                    bench="tm.$bench"
                    ;;
			dtl2)	valid_mode=true
				dir="edu.vt.rt.hyflow.core.dir.dtl2.DTL2Directory"
				context="edu.vt.rt.hyflow.core.tm.dtl2.Context"
				cm="edu.vt.rt.hyflow.core.cm.policy.Default"
				bench="tm.$bench"
				;;	
			cf)	valid_mode=true
				dir="edu.vt.rt.hyflow.core.dir.control.ControlFlowDirectory"				
				context="edu.vt.rt.hyflow.core.tm.control.undoLog.Context"
				cm="edu.vt.rt.hyflow.core.cm.policy.Karma"
				bench="tm.$bench"
				;;					
			log)	valid_mode=true
				dir="aleph.dir.cm.home.HomeDirectory"
				context="edu.vt.rt.hyflow.core.tm.undoLog.Context"
				cm="edu.vt.rt.hyflow.core.cm.policy.Timestamp"
				bench="tm.$bench"
				;;					
			dsm)	valid_mode=true
				dir="aleph.dir.home.HomeDirectory"
				context="edu.vt.rt.hyflow.core.tm.empty.Context"
				cm="edu.vt.rt.hyflow.core.cm.policy.Default"
				bench="dsm.$bench"
				;;
			rmi)	valid_mode=true
				context="edu.vt.rt.hyflow.core.tm.empty.Context"
				bench="rmi.$bench"
				instrument=false
				;;
			rw)	valid_mode=true
				context="edu.vt.rt.hyflow.core.tm.empty.Context"
				bench="rmi.$bench.rw"
				instrument=false
				;;
		esac;;
	[?]) 	echo "Usage: $0 [-b benchmark] [-m dtl|cf|log|rmi|rw|dsm] [-n nodes] [-r reads%] [-o objects] [-t threads] [-x transactions] [-v] [-d] [-s] [-i] [-D] [-M machines] [-I machine_index] [-T timeout] [-L transaction_length] [-K link_delay] [-C call_cost] [-f free_core_index] [-c number_of_calls_per_object]
	-b      benchmark, must come before execution model, default [$bench]
	-m 	execution model, mandatory parameter
			dtl	Distributed Transactional Locking
			cf	Control Flow Transactional Memory
			log	Distributed Undo Log
			rmi	Remote Method Invocation with mutual locks
			rw	Remote Method Invocation with read/write locks
			dsm	Distributed Shared Memory
	-n	number of nodes, default [$nodes]
	-r	read transactions percentage, default [$reads%]
	-o	number of objects, default [$objects]
	-t	number of threads per node, default [$threads]
	-x	number of transactions per thread, default [$transactions]
	-v	verbose mode, enbale logging, default [$verbose]
	-d	debug mode, log to file, default [$debug]
	-s	make a sanity check at the end of benchmark (if supported), default [$sanity]
	-i	enable code instrumentation, must come before execution model, default [$instrument]
	-N	default nesting model (options: flat, closed, open), default [$nestingmodel]
	-M      number of participating machines in the experiment, default [$machines]
	-I	index of current machine, used with -M option for more than single machine, default [$index]
	-T	timeout period in milliseconds, default [$time_out]
	-L	transaction processing dealy in milliseconds, default [$time_length]
	-K	network link dealy in milliseconds, deafult [$time_link]
	-C	method call execution cost due to parameter serialization or nodal processing, default [$time_call]
	-f	index of first avaiable core at machine, used at cores > 24, default [$freeCore]
	-c	number of calls per object, useful for RMI and RW models, default [$calls]
	-j	pass arguments to the JVM, default none
			"
		exit 1;;
	esac
done

if ! $valid_mode
then
echo "Invalid execution mode, try '$0 --help' for more information."
exit 1
fi

#Needed for batch mode, to enable machine 0 to start early
if ((machine_index!=0))
then
    sleep 30
fi

temp=$freeCore
#Run Benchmark
for ((  i = 0 ;  i < nodes;  i++  ))
do
    if ((i%machines==machine_index))
    then 
	echo "Starting [$i]"
	$PROG -Xmx512m -Djava.security.policy=./no.policy -javaagent:./hyflow.jar \
	  -Dverbose=$verbose -Ddebug=$debug -Dinstrument=$instrument -Dsanity=$sanity \
	  -Dcallcost=$time_call -DlinkDelay=$time_link \
	  -Dtimeout=$time_out -DtransactionLength=$time_length \
	  -DmyIP=$myIP -DparentIP=$parentIP -Dmachines=$machines \
	  -DterminateIdle=0 \
	  -Dthreads=$threads \
	  -DdirectoryManager=$dir \
	  -DcommunicationManager=$comm \
	  -Dcontext=$context \
	  -DcontentionPolicy=$cm \
	  -DdefaultNestingModel=$nestingmodel \
	  -Dnodes=$nodes -Dobjects=$objects -Dnesting=$nesting -Dcalls=$calls -Dtransactions=$transactions -Dreads=$reads \
	  -Dinner_delay=$inner_delay -Douter_delay=$outer_delay \
 	  $java_args \
	  -jar ./hyflow.jar $bench $i - &
	p=`ps -ef|grep "${bench} ${i} -"| grep -v 'grep'|awk '{print $2}'`
	echo taskset -c -p $freeCore $p
	taskset -c -p $freeCore $p
	((freeCore++))
	if ((freeCore==24+temp))
	then
		freeCore=$temp
	fi
    fi
done

#Wait for backgroup processes to finish
c=1
while (( c > 0 ))
do
    sleep 10
    c=`ps -ef|grep "${bench}"| grep -c -v 'grep'`
done
