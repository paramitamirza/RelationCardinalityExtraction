import sys

mapfile = open("english_links.txt", "r")
mapping = {}
for line in mapfile.readlines():
    mapping[line.strip().split(",")[1]] = line.strip().split(",")[0]

inputfile = open(sys.argv[1], "r")
for line in inputfile.readlines():
    wid = line.strip().split(",")[0]
    num = line.strip().split(",")[1]
    if wid in mapping:
        print wid + "," + mapping[wid] + "," + num