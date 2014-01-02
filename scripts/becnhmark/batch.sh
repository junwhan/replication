#Set max number of nodex ....
max=16
#Start batch
for ((  n = 1;  n <= max;  n+=1  ))
do
#Forwarding Parameters ....
./script.sh -n $n $*
echo ~~~~~~~~~~~~~~ DONE [$n] ~~~~~~~~~~~~~~~~
done
