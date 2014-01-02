#!/usr/bin/python

import os
import sys
import subprocess

if len(sys.argv) < 3:
	print("Usage: ./batch.py benchmark num_nodes")
	exit(0)

iterations = 1
maxnodes = sys.argv[2]
benchmark = sys.argv[1]

skip = 0
if "SKIP" in os.environ.keys():
	skip = int(os.environ["SKIP"])

print maxnodes, benchmark

for nest in ["flat", "closed", "open"]:
	for i in range(iterations):
		for n in [2]:
			if n <= int(maxnodes):
				skip = skip-1
				if skip >= 0:
					print("Skipping one test")
					continue
				print("Running on %d nodes..." % n)
				subprocess.call("rm -R logs/*", shell=True)
				cmd = "./script.sh -n %d -b %s -m dtl -d -N %s %s" % (n, benchmark, nest, " ".join(sys.argv[3:]))
				subprocess.call(["bash", "-c", cmd])
				#print(cmd)
				subprocess.call("./gather_stats.py logs/%s-tm_dtl/" % benchmark, shell=True)
	
subprocess.call("ping.sh", shell=True)

