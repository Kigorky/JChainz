#!/usr/bin/env python
import sys, os, re, codecs
from os.path import isfile, join

def get_files(dir_path):
    files = []
    for (dirpath, dirnames, filenames) in os.walk(dir_path):
        files += [os.path.join(dirpath, file) for file in filenames if file.endswith('.java')]
    return files

def implements_serializable(file_path):
    with codecs.open(file_path, 'r', encoding='utf-8') as f:
       match = re.search(r'class\s+(.+?)\s+implements[^{]+?Serializable', f.read()) 
       if match:
           return (file_path, match.group(1))
    return None

def redefines_readobject(file_path):
    with codecs.open(file_path, 'r', encoding='utf-8') as f:
       return True if re.search(r'private\s+void\s+readObject\s*\(', f.read()) else False 

def serializable_readobject(file_path):
    with codecs.open(file_path, 'r', encoding='utf-8') as f:
       return True if re.search(r'class\s+(.+?)\s+implements[^{]+?Serializable.*[\s\S]*?private\s+void\s+readObject\s*\(', f.read()) else False 

def methode_invoke_reflection(file_path):
    with codecs.open(file_path, 'r', encoding='utf-8') as f:
       return True if re.search(r'\s*import\s+[^;]*?\.*reflect.*[\s\S]*?\s+.*\.\s*invoke\s*\(', f.read()) else False 

if __name__ == "__main__" :
    if len(sys.argv) < 2:
        print "[!] Missing arguments\n[i] Usage: {0} Lib_dir".format(sys.argv[0])
        sys.exit(1)

    lib_files = get_files(sys.argv[1])

    serializable_readobject_classes = [f for f in lib_files if serializable_readobject(f)]
    print "[+] Entry points classes: "
    for f in serializable_readobject_classes:
        print "\t[+] " + f 
    
    serializable_classes = [x for x in [implements_serializable(f) for f in lib_files] if x is not None]
    print "\n\n[+] Chainable classes: "
    for f in serializable_classes:
        print "\t[+] " + f[0] 
 
    invoke_reflection_classes = [f for f in lib_files if methode_invoke_reflection(f)]
    print "\n\n[+] Reflection classes: "
    for f in invoke_reflection_classes:
        print "\t[+] " + f 


