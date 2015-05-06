#!/bin/bash

jar="$1"
output="$2"

if [ ! -f "$jar" ]; then
	echo "jar file not found: $jar"
	exit 1
fi

(echo '#!/usr/bin/java -jar'; cat $jar) > "$output"
chmod +x "$output"

echo "Generated $output"
