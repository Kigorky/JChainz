#!/usr/bin/env python

import requests
from pyquery import PyQuery
import time
import wget

page = 7
root = "https://mvnrepository.com"

# Get page number "page"
r = requests.get(root + '/popular?p=' + str(page))
html = r.text
pq = PyQuery(html)
tag = pq('a')
link = set()
for s in tag:
    if ("/artifact" in s.get('href') and not ("usages" in s.get('href'))):
        link.add(root + s.get('href'))

# Once got all the artifacts ogf page "page" visit them
artifacts = []
artifact_versions = []
for l in link:
    print "Requesting " + l
    r = requests.get(l)
    html = r.text
    time.sleep(10)
    pq = PyQuery(html)
    tag = pq('a')
    for t in tag:
        if (t.get('class') == "vbtn release"):
            # Building list of artifact versions
            artifact_versions.append(l + "/" + t.get('href').split("/")[1])
            print t.get('href')
    artifacts.append(artifact_versions)
    # Bulding global artifacts list of ists
    artifact_versions = []

# Requesting single artifacts jar
for art in artifacts:
    print "Requesting versions " + str(art)
    try:
        html = requests.get(art[0])
        time.sleep(10)
        pq = PyQuery(html.text)
        version_links = pq("a")
        for u in version_links:
            if "jar" in u.get('href'):
                print "Jar url: " + u.get('href')
                wget.download(u.get('href'), "/tmp/jars")
    except Exception as e:
        print e.message
