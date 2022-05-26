# How to generate statistics for jars info

Run the decompiler first from inside the decopiler folder

Automatically generates Entry Point file

`$ ~/jchainz/tools/decompiler/decompiler.sh ~/jchainz/libraries/ ~/jchainz/tools/decompiler/output/`

Then run statistics script

`$ ~/jchainz/tools/stats/statistics.sh /jchainz/tools/decompiler/output/ysoserial/`
