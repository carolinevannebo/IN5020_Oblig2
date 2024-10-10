### How to run spread server
1. Navigate to `spread-src-4.0.0/`
2. Run `./daemon/spread -n localhost -c ../spread.conf`

<b>Usage</b>
```
spread [-l y/n] [-n proc_name] [-c config_file]
```

<b>Options</b>
- `-l y/n` Turn on or off logging. Default is off.
- `-n proc_name` Force this daemon to be identified by a specific process name.
- `-c config-file` Use an alternate configuration file config-file instead of ./spread.conf.

<b>Note: </b> enabling logging made the server Exit caused by Alarm(EXIT) on my machine.