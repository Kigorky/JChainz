# How to run ChainzFinder

Build con build.sh

Setup JAVA_HOME (ex: export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 )

Avvia la ricerca
* `2` worker
* `8` maximum depth
* `8` 8GB heap each worker
* `true` apply "Serializable" filter
* `java.lang.reflect.Method.invoke` sink
* `/home/andromeda/jchainz` the PROJECT_HOME: the absolute path of the root of the repository
* `100` number of max chainz to be found each entry point
* `7200` max seconds of search for each entry point 

```
nohup java -cp ./ChainzFinder-1.0-SNAPSHOT-jar-with-dependencies.jar chainz.ChainzFinderRunnerMain 2 8 8 true java.lang.reflect.Method.invoke /home/andromeda/jchainz 100 7200 > ~/ChainzFinder.txt  &
```

`PROJECT_HOME/ChainzFinder/target_jars` contains the jars used by the ChainzFinder


# Run ChainzFinder on a single entry/exit point (example)

```
java -jar ./target/ChainzFinder-1.0-SNAPSHOT-jar-with-dependencies.jar PROJECT_HOME/ChainzFinder/target_jars/commons-collections4-4.2.jar org.apache.commons.collections4.bag.HashBag.readObject:java.lang.reflect.Method.invoke 8 true PROJECT_HOME 100 7200
```
