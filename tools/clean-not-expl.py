#!/usr/bin/env python3
import os
import json
import pprint as pp

diz = {}
path = "output2"
i = 0
for f in [os.path.join(dp, f) for dp, dn, fn in os.walk(os.path.expanduser(path)) for f in fn]:
	if(f.endswith('/exploitable.flag')):
		i+=1
		classe = f.split("/")[1]
		if classe in diz:
			diz[classe] = diz[classe]+1
		else:
			diz[classe] = 1 
		
pp.pprint(diz)
print(i)
