#!/usr/bin/python

# Requires installing ubuntu package: python-scitools

import os
import os.path
import sys
import hashlib
import numpy
import json
import time, datetime

def main():
	assert len(sys.argv) > 1
	process_folder(sys.argv[1])
	summarize()
	save_output()

def process_folder(dirname):
	# Check argument
	assert os.path.exists(dirname), "Given directory doesn't exist: %s" % dirname
	assert os.path.isdir(dirname), "Given name is not a directory: %s" % dirname
	# Expand filenames
	filelist = os.listdir(dirname)
	filelist = [x for x in filelist if x[-7:] == ".result"]
	# Visit each file
	for filename in filelist:
		process_file(dirname, filename)
		
def process_file(dirname, filename):
	# Read contents
	f = open(os.path.join(dirname, filename))
	lines = f.readlines()
	f.close()
	# Update unique id
	global digest
	digest.update("".join(lines))
	# Check correctness
	assert len(lines) == 9, \
		"Invalid file length, result file must contain exactly one test (%s -> %d lines)" % \
		(filename, len(lines))
	# Parse
	global results
	results[int(filename[:-7])] = parse_result(lines)

def parse_result(lines):
	res = {}
	lines = [x.strip() for x in lines if len(x) > 3]
	#~~~~~~~~~~~~~~Mon Jul 11 18:27:29 EDT 2011~~~~~~~~~~~~~~
	res["date"] = lines[0].replace("~", "")
	#Hashtable-TM  n=12, t=1, o=10, x=100, c=1, %=50, T=10, C=1, L=10, K=1, N=flat, Host=lost.
	res["bench"], tmp = lines[1].split("  ")
	tmp = tmp[:-1]
	tmp = tmp.split(", ")
	for x in tmp:
		a, b = x.split("=")
		res[a] = b
	#Throughput: 13.197836
	#Reads: 49
	#Writes: 51
	#Aborts: 8
	#Conflicts: 0
	#Local Conflicts: 0
	#Forwarding: 27
	for x in lines[2:]:
		a, b = x.split(": ")
		res[a] = b
	return res

def summarize():
	global results, output
	# Create numpy array
	data = numpy.array([ float(results[key]["Throughput"]) for key in results.keys()], )
	# Stats
	output["average"] = numpy.average(data)
	output["mean"] = data.mean()
	output["min"] = data.min()
	output["max"] = data.max()
	output["std"] = data.std()
	output["var"] = data.var()
	output["sum"] = data.sum()
	# Identifier
	output["hash"] = digest.hexdigest()
	# Properties
	output["hosts"] = ",".join(set( [results[key]["Host"] for key in results.keys()] ))
	output["n"] = results[0]["n"]
	output["bench"] = results[0]["bench"]
	output["date"] = results[0]["date"]
	output["nesting"] = results[0]["N"]
	# Original data
	output["|results|"] = results

def save_output():
	global results
	dirname = "stats"
	jdata = json.dumps(output, sort_keys=True, indent=4)
	filename = time.strftime("%Y-%m-%d_%H-%M-%S", time.strptime(output["date"], "%a %b %d %H:%M:%S %Z %Y"))
	filename = filename + ".json"
	if not os.path.exists(dirname):
		os.makedirs(dirname)
	f = open(os.path.join(dirname, filename), "w")
	f.write(jdata)
	f.close()
	print("Wrote %s/%s" % (dirname, filename) )

if __name__ == "__main__":
	# Init globals
	global results, digest, output
	results = {}
	digest = hashlib.sha1()
	output = {}
	# Call main function	
	main();
