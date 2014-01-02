#!/usr/bin/python

# Requires the ubuntu python-matplotlib package

import os
import os.path
import sys
import json
import numpy
import pylab

DIRNAME = "stats"
EXTENSION = ".json"

# Main function
def main():
	if (len(sys.argv)-1) % 2 == 1:
		print("Usage: plot_series.py KEY VALUE")
		print("Key examples: N, n, T, t")
		exit(0)
	read_all()
	for i in range(1, len(sys.argv), 2):
		filter_data(sys.argv[i], sys.argv[i+1])
	plot_bynesting()

def read_all():
	global data
	# List stats/ dir
	file_names = os.listdir(DIRNAME)
	file_names = [os.path.join(DIRNAME, x) for x in file_names if x[-len(EXTENSION):] == EXTENSION]
	# Read each file
	for fname in file_names:
		f = open(fname, "rt")
		content = f.read()
		f.close()
		# Parse json
		item = json.loads(content)
		data.append(item)
	# Done
	print "Done reading %d files." % len(data)

def filter_data(key, value):
	global data
	sample = data[0]
	if key in sample.keys():
		filtered_data = [x for x in data if x[key] == value]
	elif key in sample["|results|"]["0"].keys():
		filtered_data = [x for x in data if x["|results|"]["0"][key] == value]
	else:
		print("Unknown key %s" % key)
		exit(0)
	if len(filtered_data) == 0:
		print("No tests matching criteria %s=%s" % (key, value))
		exit(0)
	print("Filtered down to %d tests (for criteria %s=%s)" % (len(filtered_data), key, value))
	data = filtered_data

def plot_bynesting():
	global data
	xclosed = []
	yclosed = []
	xopen = []
	yopen = []
	xflat = []
	yflat = []
	for itm in data:
		if itm["nesting"] == "flat":
			xflat.append(itm["n"])
			yflat.append(itm["sum"])
		elif itm["nesting"] == "closed":
			xclosed.append(itm["n"])
			yclosed.append(itm["sum"])
		elif itm["nesting"] == "open":
			xopen.append(itm["n"])
			yopen.append(itm["sum"])
	pylab.plot(xflat, yflat, "ks")
	pylab.plot(xclosed, yclosed, "gs") 
	pylab.plot(xopen, yopen, "rs")
	pylab.show()
	


# Entry point
if __name__ == "__main__":
	# init globals
	global data
	data = []
	# call main function
	main()
