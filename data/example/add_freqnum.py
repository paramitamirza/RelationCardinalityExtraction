import sys

fin1 = open(sys.argv[1], "r")
fin2 = open(sys.argv[2], "r")
freqnum = {}
for line in fin1.readlines():
    line = line.strip()
    freqnum[line.split(",")[0]] = line.split(",")[1]

fout = open(sys.argv[3], "w")
for line in fin2.readlines():
    line = line.strip()
    id = line.split(",")[0]
    fout.write(id + "," + line.split(",")[1] + "," + line.split(",")[2] + "," + freqnum[id] + "," + line.split(",")[3] + "," + line.split(",")[4] + "\n")

