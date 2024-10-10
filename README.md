# Replicated Bank Account
## The Second Mandatory Programming Assignment
### IN5020/IN9020 Autumn 2024

[Assignment link](https://www.uio.no/studier/emner/matnat/ifi/IN5020/h24/timeplan/in5020_2024_assignment2.pdf)

<b>Todo:</b>
- [x] Client connects to spread server using listener - Caroline
- [ ] Client should join a group whose name is `<account name>` - Caroline
- [ ] Client should wait until all members have joined the group - Caroline
- [ ] File reader - Caroline
- [ ] Implement file reader - Caroline
- [ ] getQuickBalance - Liang
- [ ] getSyncedBalance (naive implementation) - TBD
- [ ] getSyncedBalance (correct implementation) - TBD
- [ ] deposit <amount> - Liang
- [ ] addInterest <percent> - Liang
- [ ] getHistory - Liang
- [ ] checkTxStatus <Transaction.unique_id> - Caroline
- [ ] cleanHistory - Caroline
- [ ] memberInfo - Caroline
- [ ] sleep <duration> - Liang
- [ ] exit - Liang
- [ ] <b>The client supports a single bank account with sequentially consistent replication semantics</b> (all the replicas that do
  not fail go through the same sequence of changes and end up with the same balance value)

<b>Note1:</b> balance of the account can be negative.
<b>Note2:</b> no need to consider the happened-before relationship of every single command
between clients. Only consider the consistent view.


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

### About the spread library
It consists of 12 classes. The imporant ones are:
1. SpreadConnection – represents a connection to the spread daemon
2. SpreadGroup – represents the spread group
3. SpreadMessage – represents message that is either sent or received

### Connection:
```
SpreadConnection connection = new SpreadConnection();
connection.connect(<ip address>, <port number>, <connection name>, 
                    <priority>, <group membership>);
```
<b>Parameters:</b>
- `ip address` – ip address of the host that runs the spread server
- `port number` – port number of the spread server
- `connection name` - unique connection name. unique per client
- `priority` – boolean value to determine whether it’s a priority connection or not. it  doesn’t have any effect.
- `group membership` – denotes whether the group membership messages are received or not. set it to true.

### Spread group:
```
SpreadGroup group = new SpreadGroup();
group.join(connection, <group name>);
```
<b>Parameters:</b>
- `group name` – name of the group

### Spread message:
```
SpreadMessage message = new SpreadMessage();
message.addGroup(group);
message.setFifo();
message.setObject(<message data>);
connection.multicast(message);
```
### Listener:
Group members can receive message using the listener. Spread provides two types of interfaces: 
BasicListener and AdvancedListener. 

To use the listener, the interface should be 
implemented and the corresponding class object should be added with the connection.
```
Listener listener = new Listener();
connection.add(listener);
```