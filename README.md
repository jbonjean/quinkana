# Quinkana

Tail **logstash** TCP output.

## Installation

RPM, DEB, JAR and self-contained executable are provided.

## Usage

```
usage: quinkana tail|list [OPTIONS]

examples:
	 quinkana list
	 quinkana tail --fields severity programname message --exclude severity=debug --include host=c1.example.com
	 quinkana tail -H 10.69.101.200 -P 9090 -f severity host programname message -x severity=debug severity=info

Tail logstash TCP output.

positional arguments:
  {tail,list}

optional arguments:
  -h, --help             show this help message and exit
  -H HOST, --host HOST   logstash host (default: localhost)
  -P PORT, --port PORT   logstash TCP port (default: 9090)
  -f FIELDS [FIELDS ...], --fields FIELDS [FIELDS ...]
                         fields to display
  -i FILTER [FILTER ...], --include FILTER [FILTER ...]
                         include filter (OR), example host=example.com
  -x FILTER [FILTER ...], --exclude FILTER [FILTER ...]
                         exclude filter (OR, applied after include), example: severity=debug
  -s, --single           display single result and exit
  --version              output version information and exit
```

## Examples

```
quinkana list
quinkana tail --fields host severity programname message --exclude severity=debug --include host=sflstack-controller
quinkana tail --fields host severity programname message --exclude severity=debug
quinkana tail -H 10.69.101.200 -P 9090 -f severity host programname message -x severity=debug
quinkana tail -H 10.69.101.200 -f severity host programname message -i programname=ceilometer-alarm-notifier
```

## Setup

All you need is add the following output to your **logstash** configuration:

```
tcp {
	host => '0.0.0.0'
	port => '9090'
	mode => 'server'
}
```

For example, a typical configuration looks like:
```
input {
	udp {
		port => 10514
		codec => json
		type => syslog
	}
}
output {
	elasticsearch {
		host => localhost
		protocol => http
	}
	tcp {
		host => '0.0.0.0'
		port => '9090'
		mode => 'server'
	}
}
```

## Copying

quinkana is Copyright (C) 2015 Julien Bonjean <julien@bonjean.info><br />
See the file COPYING for information of licensing and distribution.
