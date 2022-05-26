#!/usr/bin/env python
import sys
import subprocess
import os
import glob


# Please note that if you kill the analysis during runner executions, 
# you should probably rebuild the project with $ build.sh (or create manually the config.properties file)

def updateconfig(classpath):
    configfile = './config.properties'
    conf = []
    with open(configfile) as fp:
        line = fp.readline()
        while line:
            if not "targetJarPath" in line:
                conf.append(line)
            else:
                # Please edit this if rerun ChainzFinder or run on maven 
                conf.append("targetJarPath=" + classpath)
            line = fp.readline()


    # Write the new config file
    newconfig = open(configfile,'w') 
    for l in conf:
        newconfig.write(l)
    newconfig.close()


def chainzfile(path): 
    filename = path.split("/")[-1]
    print("Filename:\n\t" + filename)
    # Build the list of chains
    chainslist = []
    classpath = []
    with open(path) as fp:
        line = fp.readline()
        while line:
            line = fp.readline()
            if("CLASSPATH" in line):
                classpath=line.split(":")[-1]
                updateconfig(classpath)
            if not "==>" in line:
                continue
            chainslist.append(line)
    
    print("Classpath JAR: " + classpath)
    print("Chains: " + str(len(chainslist)) + "\n")

    # Run the Analyzer
    for c in chainslist:
        try:
            process = subprocess.Popen(bashCommand + "'" + c + "'", shell=True, executable="/bin/bash")
            status = process.wait()
            print("Exit code: " + str(status) + "\n")
        except:
            process.kill()

def chainzdir(path):
    filelist=[]
    for folder in glob.glob(path + "/*"): # For each folder in path
        for f in glob.glob(folder + "/*.chains"): # For each list of files in path 
            filelist.append(f) # Append a new file
    for f in filelist:
        print("[DEBUG] Currently analyzing... " + f)
        chainzfile(f)

def main():
    # Input file path or dir path
    if(len(sys.argv)!=2):
        print("Give a path of chains dir or file of ChainzFinder")
        exit()

    # Setup
    path = sys.argv[1] 
    isDirectory = os.path.isdir(path)
    print("\nPath:\n\t" + path)

    if(isDirectory):
        chainzdir(path)
    else:
        chainzfile(path)



# Command
bashCommand="time java -jar ./target/ChainzAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar -c "
main()
