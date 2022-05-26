#!/usr/bin/env python3
import os
import json

THRESHOLD = 80


class Output:
	cluster = ""
	cluster_length = 0
	perc_not_exploitable = 0
	not_exploitable = 0
	perc_exploitable = 0
	exploitable = 0
	
	def __init__(self,cluster,exploitable,not_exploitable):
		self.cluster = cluster
		self.cluster_length = len(cluster)
		self.exploitable = len(exploitable)
		self.not_exploitable = len(not_exploitable)
		self.perc_not_exploitable = round(len(not_exploitable)/len(cluster)*100)
		self.perc_exploitable = round(len(exploitable)/len(cluster)*100)
		
class Differences:
	left = ""
	right = ""
	similarity = 0 
	def __init__(self, left, right, similarity):
		self.left = left
		self.right = right
		self.similarity = similarity
	def __repr__(self): 
		return str([self.left,self.right,self.similarity])		


def dif_contains_pair(differences,first,second):
	for dif in differences:
		if (dif.left==first and dif.right==second) or (dif.left==second and dif.right==first):
			return True
	return False 


def which_clusters(chain):
	return [cluster for cluster in all_clusters if chain in cluster]

def is_valid_cluster(cluster, row_similarities):
	for chain in cluster:
		if len([col for col in row_similarities if col.right == chain]) > 0:
			if not ([col for col in row_similarities if col.right == chain][0].similarity >= THRESHOLD):
				return False
		else:
			return False
	return True

def add_in_cluster(left_path, cluster):
	global all_clusters
	new_all_clusters = []
	for c in all_clusters:
		if c == cluster:
			new_all_clusters += [cluster + [left_path]]
		else:
			new_all_clusters += [c]
	all_clusters = new_all_clusters

def create_cluster(chain, similar_chains):
	global all_clusters
	all_clusters += [similar_chains + [chain]]


def jsonize():
	json_output = []
	for cluster in all_clusters:
		exploitable = []
		not_exploitable = []
		for chain in cluster:
			if(os.path.isfile(chain.replace("chain.txt","exploitable.flag"))):
				exploitable.append(chain)
			else:
				not_exploitable.append(chain)
		
		json_output.append(Output(cluster,exploitable,not_exploitable).__dict__)

	f = open("output.json", "w")
	f.write(json.dumps(json_output))
	f.close()


def print_summary():
	#print("****** ALL CLUSTERS ******")
	#print(all_clusters)
	print("****** NUMBER OF CLUSTERS ******")
	print(len(all_clusters))
	print("****** CARDINALITY OF CLUSTERS ******")
	print([len(c) for c in all_clusters])


def cluster_mean():
	print("****** AVERAGE CHAIN PER CLUSTER ******")
	total_clusterized = 0
	for cluster in all_clusters:
		total_clusterized += len(cluster)
	print(total_clusterized/len(all_clusters))
	
def clusters_per_library():
	print("****** CLUSTERS PER LIBRARY ******")
	cc4 = []
	cc3 = []
	for c in total_classes_in_chains:
		if "collections4" in c:
			cc4.append(c)
		else:
			cc3.append(c)
		
	print("CC3: " + str(len(cc3)) + "\n" + str(cc3))
	print("CC4: " + str(len(cc4)) + "\n" + str(cc4))
	print("AVERAGE CHAIN SIZE: " + str(avg_chains_size/len(file_list)))



all_clusters = []

path = './temp_exploitable_clustering'

differences = []
files = os.listdir(path)
file_list = []

avg_chains_size = 0
total_classes_in_chains = set()

# per ogni file
for f in [os.path.join(dp, f) for dp, dn, fn in os.walk(os.path.expanduser(path)) for f in fn]:
	if(f.endswith('chain.txt')):
		# leggi le righe
		lines = set()
		with open(f) as fp:
			line = fp.readline().strip()
			if len(line)>1 and not "<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>" == line.strip():
				lines.add(line)
			while line:
				line = fp.readline().strip()
				if len(line)>1 and not "<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>" == line.strip():
					lines.add(line)
					avg_chains_size = avg_chains_size + 1
					total_classes_in_chains.add(line.strip().split("<")[1].split(":")[0])
			avg_chains_size = avg_chains_size + 1
		file_list.append((f,lines))



i = 0
for left_path in file_list:
	i += 1
	print('Check ' + str(i) + "/" + str(len(file_list)) + " - " + left_path[0])
	differences = []
	for right_path in file_list:
		if(left_path[0]==right_path[0]):
			continue
		left_lines = left_path[1]
		right_lines = right_path[1]
		dif_intersection = left_lines.intersection(right_lines)
		total_lines = len(left_lines) + len(right_lines)
		dif_instance = Differences(left_path[0],right_path[0],round(((len(dif_intersection)*2)/total_lines)*100,2))
		differences.append(dif_instance)

	# la catena non è ancora stata aggiunta
	added = False
	# queste sono le catene simili
	similar_chains = []
	# prendi le catene che somigliano almeno l'80%
	valid_chains = [d for d in differences if d.similarity >= THRESHOLD]
	for other_chain in valid_chains:
		# sono in un cluster?
		clusters = which_clusters(other_chain.right)
		# per ogni cluster della catena
		for cluster in clusters:
			# controlla se la catena scelta appartiene ai cluster di other_chain
			if is_valid_cluster(cluster, differences):
				add_in_cluster(left_path[0], cluster)
				added = True
			else:
				if not other_chain.right in similar_chains:
					similar_chains += [other_chain.right]

	if not added:
		# crea un nuovo cluster per questa catena
		create_cluster(left_path[0], similar_chains)
		



cluster_mean()

print_summary()

clusters_per_library()
