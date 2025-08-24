# The DumbSyslogSystem

DumbSyslogSystem is a dumb-simple set of services/applications that provide syslog capabilities.  There are three components to the DumbSyslogSystem:  
- DumbSyslogServer
- DumbSyslogRelay
- DumbSyslogViewer

Their purpose should be mostly self-explanatory, but here's the explanation anyways: DumbSyslogServer provides traditional BSD syslog (RFC 3164) server capabilities.  It also provides a mechanism for the real-time viewing of syslogs as they are received.  DumbSyslogRelay is purely a relay for syslogs.  It simply receives syslogs, and then forwards them to a specified server.  DumbSyslogViewer is primarily designed to view logs as they are received, but will display all logs that are currently stored inside DumbSyslogServer's memory buffer.  DumbSyslogViewer also has the capability to filter messages based on hostname/address, message body content, facility, and severity.  

## DumbSyslogServer Deployment Guide

This guide will walk you through a basic deployment of DumbSyslogServer on a fresh install of Debian 12.

### Prerequisites

Install Debian 12 to you liking (or something similar, but this guide is based on Debian 12 and systemd).  The DumbSyslogSystem is written in Java, so you must have a JVM or JDK installed.  The easiest way to do this on Linux is with SDKMAN.  So follow the documentation available on SDKMAN's website to install a JVM/JDK of your choosing (Temurin is a good choice if you don't have a preference).  The DumbSyslogSystem requires a JVM/JDK of version 21 or higher.  Prebuilt JAR's are currently exported with version 21.0.3.  Installation of the JVM/JDK can be tested with the following:

~~~
java --version
~~~

This should result in the display of the currently installed JVM/JDK version.

### DumbSyslogServer Setup

Download a DumbSyslogSystem release.  Extract the archive, and copy DumbSyslogServer.jar and config_serverdefault to the path of your choosing.  To stay generally consistent with a Debian service deployment, it is recommended to install to "/etc/dumbsyslogserver/".  Make a copy of config_serverdefault and name it "config_server".  Open the file, and edit as needed.  

The configuration options are explained as comments inside of config_server.  Uncomment an appropriate LogStorageLocation, or create one of your liking.  The pre-provided one of "/var/dumbsyslogserver/logs" will work fine.  The default port of 514 will work fine as this is what most systems will also use by default.  DisregardTimestamp will override the self-reported timestamp from the syslog generating system with a timestamp generated on message reception.  The BSD syslog standard does typically use the self-reported timestamp, but if you are not using NTP or do not trust the generating service to report an accurate timestamp then it is recommended to enable this feature.  If you plan on using DumbSyslogViewer, then uncomment InterfacePort.  The default TCP port of 54321 may clash with another service, so be prepared to change that if neccessary.  The default LogStorageHours of 0 is fine.  This will prevent auto deletion of logs.  The default MessageBufferCount of 10,000 is fine.  This does not affect how many syslogs are stored on disk, but only how many are retained in memory at a given time.  This also affects how many previous syslogs can be retrieved from DumbSyslogViewer at a given time.  

Next, a systemd service should be made to run DumbSyslogServer like any other service you may be familiar with on Debian.  The following is an example.  Change and modify as needed and save as /etc/systemd/system/dumbsyslogserver.service  

~~~
[Unit]
Description=Dumb simple syslog server
After=syslog.target network.target

[Service]
SuccessExitStatus=143
User=root
Type=simple
#Restart=always
WorkingDirectory=/etc/dumbsyslogserver
ExecStart=/pathtojdk/bin/java -jar /etc/dumbsyslogserver/DumbSyslogServer.jar
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
~~~

Edit ExecStart to reflect the actual path the the JVM/JDK on your system.  This has to be run as root due to the syslog port (514) being lower than 1025.  If you use a non-standard syslog port, you may be able to run DumbSyslogServer as a non-root user.  Next, we need to reload systemd in order to use the newly created service:

~~~
sudo systemctl daemon-reload
~~~

Now we should be able to start the service:

~~~
sudo systemctl start dumbsyslogserver
~~~

Next, we'll check the status to make sure it is running:

~~~
sudo systemctl status dumbsyslogserver
~~~

Under "Active" you should see "active (running)".  If it does not show as active, look at some of the log statements show from the status command to lead you towards the problem.  Most likely it has to do with java not being set up properly, the path to the JVM/JDK is incorrect in the created service, or something is wrong with config_server.  

Now we can enable this service to automatically run on system startup:

~~~
sudo systemctl enable dumbsyslogserver
~~~

### An Alternative Deployment Strategy

You may not be excited by having to employ a JVM/JDK in order to use DumbSyslogServer.  However, if you choose GraalVM (I recommend the community edition for licensing reasons) as your deployment JVM/JDK, you can optionally natively compile DumbSyslogServer.  Simply call `native-image -jar DumbSyslogServer.jar` and a native executable will be output.  You can then directly call this executable instead of having to call `java -jar DumbSyslogServer`.  You may see an error similar to `It appears as though libz:.a is missing. Please install it.`  As of writing this, this can be satisfied with `sudo apt install libz-dev`  However, packages do change, and this may not be accurate in the future.  Other than satisfying that one specific dependency, you shouldn't have any trouble on a fresh install of Debian 12.  Again, this is not a required step, but it might satisfy a specific need or want that you may have.  If you go this route, you no longer need to call `java` from you systemd service, and you will also be calling the native executable, and not the jar file.

## DumbSyslogRelay Deployment Guide

This is a bare-bones guide for DumbSyslogRelay.  This is also based on Debian 12.

### Prerequisites

The prerequisites are the same as DumbSyslogServer.  So follow that and then return.

### DumbSyslogRelay Setup

This is essentially the same as the DumbSyslogServer setup.  In this case, copy DumbSyslogRelay.jar and config_relaydefault to "/etc/dumbsyslogrelay/".  Make a copy of config_relaydefault and name it "config_relay".  Open the file, and edit as needed.  

There's only two configuration options available for DumbSyslogRelay: SyslogPort and ServerAddress.  These should be self explanatory.

Next, a systemd service should be made to run DumbSyslogServer like any other service you may be familiar with on Debian.  The following is an example.  Change and modify as needed and save as /etc/systemd/system/dumbsyslogrelay.service  

~~~
[Unit]
Description=Dumb simple syslog relay
After=syslog.target network.target

[Service]
SuccessExitStatus=143
User=root
Type=simple
#Restart=always
WorkingDirectory=/etc/dumbsyslogrelay
ExecStart=/pathtojdk/bin/java -jar /etc/dumbsyslogrelay/DumbSyslogRelay.jar
ExecStop=/bin/kill -15 $MAINPID

[Install]
WantedBy=multi-user.target
~~~

Again, edit ExecStart to reflect the actual path the the JVM/JDK on your system.  Again, this has to be run as root due to the syslog port (514) being lower than 1025.  Again, (this is getting repetitive) If you use a non-standard syslog port, you may be able to run DumbSyslogServer as a non-root user.  Again, (sigh) we need to reload systemd in order to use the newly created service:  

~~~
sudo systemctl daemon-reload
~~~

Start the service:

~~~
sudo systemctl start dumbsyslogrelay
~~~

Next, we'll check the status to make sure it is running:

~~~
sudo systemctl status dumbsyslogrelay
~~~

Under "Active" you should see "active (running)".  Again, if it does not show as active, look at some of the log statements show from the status command to lead you towards the problem.

Now we can enable this service to automatically run on system startup:

~~~
sudo systemctl enable dumbsyslogrelay
~~~

### An Alternative Deployment Strategy

You can also use GraalVM's native-image tool to natively compile DumbSyslogRelay.  See the same section for DumbSyslogServer.

## DumbSyslogViewer

DumbSyslogViewer is a Java based desktop app that displays and filters logs in real-time.  If you're on windows, you can download DumbSyslogViewer pre-packaged with a JVM and a native windows launcher so you don't have to separately install Java.

There are three main components to DumbSyslogViewer:  
- Syslog viewer
- Filter editor
- Interactive console

The syslog window displays syslogs in real-time as they are received by DumbSyslogServer.  The filter editor is primarily used to display currently configured filters, but editing can also be performed directly in the window.  The interactive console is how settings are changed and is primarily how filters are created.

### DumbSyslogViewer Usage

First, launch the application.  On windows, if you downloaded the windows release of the viewer, you will have an executable named DumbSyslogViewer.exe.  Simply double-click and the program will launch.  If on a different system, ensure a JVM/JDK is installed and run `java -jar DumbSyslogViewer.jar`  This will launch the viewer.  On the first run, you will have to specify the address/hostname and port of the DumbSyslogServer instance you wish to connect to.  This is accomplished with the `server set` command in the console.  The specific syntax is as follows:  

~~~
server set [hostname/address] [interface port number]
~~~

Hostname/address is self explanatory.  Interface port number is the port specified by "InterfacePort" in config_server for DumbSyslogServer.  By default, this port is 54321, but this may need to be changed due to conflicts.  If there is no port specified in config_server (or the parameter is commented out) then you will not be able to connect with DumbSyslogViewer.  Once this initial configuration has be set, the console will prompt you to call "connect".  Once "connect" is called via the console, if everything is configured correctly, you should see the console state that you are connected.  If there are any syslogs present in DumbSyslogServer's buffer you should immediately begin to see them in the main syslog window.  If DumbSyslogServer has not received any syslogs, then you will not see anything in this window until a syslog has been received.  DumbSyslogViewer is primarily designed for real-time syslog monitoring.  Because of this, you cannot view any logs that have been dumped from DumbSyslogServer's buffer.  Syslogs are always saved to disk by DumbSyslogServer (unless configured otherwise or auto-deleted), but they the syslogs avaiable for viewing by DumbSyslogViewer must be present in DumbSyslogServer's buffer.  As mentioned under the DumbSyslogServer Deployment Guide, the buffer size is configurable and by default is 10,000.  As it currently stands, the timestamp in the viewer is always based off of the time that DumbSyslogServer received the messages.  This is in contrary to the BSD syslog standard, but this does prevent syslog order discrepancies within the viewer itself.  If DumbSyslogServer is not configured to override the self-reported timestamp, the original timestamp can be viewed in the syslog archive file-store.

### Filters

In order to understand the filter system, a quick refresher on the BSD syslog format is needed.  The components are:

- PRI
- timestamp
- hostname
- message body
These components are layed out as follows:
<PRI>timestamp hostname messageBodyTheRestOfTheWay

PRI is the facility number (facility means a particular subsystem) multiplied by 8, and then the severity added to it.  Severity level is inverse to the Severity number.  I.e., 0 is highest and 7 is lowest.  Technically the timestamp and hostname are conceptually grouped together as a "header", but it is easier and makes more sense to parse them as two separate components.  FYI, every byte in a BSD syslog message is read as a 1 byte character (ASCII).

There are four filter types:  
- Hostname/address
- Message body
- Facility
- Severity

**Hostname/Address and Message Body filters**

Usage:  

~~~
[inc/exc] [host/msg] "phrase to match" "OR an optional, additional phrase to match" ...
~~~

Hostname/address and message body filters filter based on content contained with the hostname/address and message body.  There are two sub-types: inclusion and exclusion.  Inclusion mandates that a syslog hostname or message body contains a specified phrase in order to be displayed.  Exclusion mandates that a syslog hostname or message body does not contain a specified phrase in order to be displayed.  If more than one phrase is specified in a single filter, then those phrase conditions are OR'd.  I.e., if an inclusion filter has two message body phrases of type "auth" and "session", then a syslog message will be displayed if it contains either the phrases "auth" OR "session".  Similarly, if an exclusion filter has two message body phrases of type "auth" and "session", then a syslog message will be not be displayed if it contain either phrase "auth" OR "session".  You cannot mix different types inside one filter.  It must be composed of only one type (only checking hostnames or only checking message bodies)  Phrases are case sensitive.  A work-around for this is using multiple phrases in one filter (e.g. case Case CASE.)  A phrase can consist of either one non-whitespace word (i.e., aPhraseToMatch), or multiple words with whitespace inclosed quotes (i.e., "a phrase to match").

**Facility Filters**

Usage:

~~~
[inc/exc] fac [facility number 0-23] [an optional, additional facility number 0-23] ...
~~~

A facility filter mandates that a syslog's facility matches one of the specified facility numbers.  There are two sub-types: inclusion and exclusion.  Inclusion mandates that a syslog's facility must contain a specified facility number in order to be displayed.  Exclusion mandates that a syslog's facility must not contain a specified facility number in order to be displayed.  If more than one facility number is specified in a single filter, then those phrase conditions are OR'd.  I.e., if an inclusion filter has two facility numbers of type 0 and 5, then a syslog will be displayed if it contains either facility number 0 OR 5.  Similarly, if an exclusion filter has two facility numbers of type 0 and 5, then a syslog will be not be displayed if it contain either facility numbers 0 OR 5.  
This facility number table is from the BSD syslog specification (RFC 3164):  

~~~
Numerical					Facility
   Code
    0					kernel messages  
    1					user-level messages  
    2					mail system  
    3					system daemons  
    4					security/authorization messages (note 1)  
    5					messages generated internally by syslogd  
    6					line printer subsystem  
    7					network news subsystem  
    8					UUCP subsystem  
    9					clock daemon (note 2)  
   10					security/authorization messages (note 1)  
   11					FTP daemon  
   12					NTP subsystem  
   13					log audit (note 1)  
   14					log alert (note 1)  
   15					clock daemon (note 2)  
   16					local use 0  (local0)  
   17					local use 1  (local1)  
   18					local use 2  (local2)  
   19					local use 3  (local3)  
   20					local use 4  (local4)  
   21					local use 5  (local5)  
   22					local use 6  (local6)  
   23					local use 7  (local7)  

	Table 1.  syslog Message Facilities

Note 1 - Various operating systems have been found to utilize
	Facilities 4, 10, 13 and 14 for security/authorization,
	audit, and alert messages which seem to be similar.
Note 2 - Various operating systems have been found to utilize
	both Facilities 9 and 15 for clock (cron/at) messages.
~~~

FYI, all inclusion and exclusion filters are structurally and syntactically the same type of filter, but operate differently based on what the first field type is (hostname, message, or facility).  

**Severity Filters**

Usage:  

~~~
severity [minimum severity level 0-7]
~~~

A severity filter mandates that a syslog's severity meets a minimum threshold in order to be displayed.  A severity of 7 represents the most insignificant of messages (debug messages).  A severity of 0 represents the most significant of messages (emergency messages).  The following severity level table is from the BSD Syslog specification (RFC 3164)  

~~~
Numerical					Severity  
  Code 

   0			Emergency: system is unusable  
   1			Alert: action must be taken immediately  
   2			Critical: critical conditions  
   3			Error: error conditions  
   4			Warning: warning conditions  
   5			Notice: normal but significant condition  
   6			Informational: informational messages  
   7			Debug: debug-level messages  

   Table 2. syslog Message Severities
~~~

**Combined filter behavior**  
If there are multiple filters active at one time, then the filters are logically AND'd together.  In other words, a syslog must meet the requirements of ALL filters in order to be displayed.  Because filters are only logically AND'd, filter order does not matter.

**Filter save behavior**  
Filters are automatically saved to disk when they are applied.  If you close DumbSyslogViewer after applying filters, they will be re-applied on startup.

**Filter Application**  
The primary way in which filters should be configured is through the console CLI.  Typically, you will add or remove a filter (i.e., inc msg "auth", rem 1, etc...) and then, if needed, inspect the filter in the editor window.  After you are satisfied with the filter configuration, you will call `apply` through the console CLI.  If there are no issues, DumbSyslogViewer will reload DumbSyslogServer's syslog buffer and evaulate the syslogs via the applied filters.  If a syslog meets the filter requirements, it will be displayed.  If it does not, it simply will not be displayed.  The viewer does not control the server at all; it is merely a filtered window into the server's syslog buffer.  

**Filter Editing**  
DumbSyslogViewer uses SOFFIT (github.com/cguy777/JSoffit) as the framework for filter serialization and management.  The filter editor window is primarily in place to provide a quick reference of what filters are configured, but they can also be edited directly in the window.  The "number" field inside a filter is purely for reference purposes and has no logical affect on filter behavior whatsoever.  In fact, you can remove the number fields and then apply the filters with no consequence.  The number field will automatically be re-added after `apply` has been called.  The specifics of filter syntax will not be discussed in length minus a few tips and tricks.  However, after adding a few filters through the CLI and observing them in the editing window, it should be mostly obvious how they are structured.

**Manual Filter Editing Info**  
The primary reason to use the filter editor is to add or remove individual fields within a filter.  Consider the following situation.  Imagine you have an inclusion filter that is evaluating on message body content with the phrases "auth" and "-- MARK --".  That filter will look like this:  

~~~
Inclusion {
	number "0"
	message "auth"
	message "-- MARK --"
}
~~~

Now suppose that you want to add the additional message body phrase "log rotation".  Instead of calling `rem 0` to remove the filter and then `inc msg "auth" "-- MARK --" "log rotation"` you could just manually add another message field to the existing filter to look like this:

~~~
Inclusion {
	number "0"
	message "auth"
	message "-- MARK --"
	message "log rotation"
}
~~~

You could encounter a filter with numerous fields that you would rather not have to completely re-input through the CLI.  Manually editing the filter could save you time and prevent mistakes in this scenario.  However, as stated previously, inclusion and exclusion filters must only contain one type.  I.e., they can only be evaluating hostnames, or only evaluating facility numbers.  

Consider the following inclusion filter:

~~~
Inclusion {
	number "0"
	hostname "domain-controller"
	message "auth"
}
~~~

This filter will operate as an inclusion filter operating on hostnames/addresses only.  After apply is called, the filter will be reformatted without the message field:

~~~
Inclusion {
	number "0"
	hostname "domain-controller"
}
~~~

Again, this is why it is typically recommended to only create filters through the CLI: it prevents confusing mistakes like this and is typically faster.

### native-image for DumbSyslogViewer?

Currently there is a windows build of DumbSyslogViewer that has a self-contained JVM with a native launcher.  If you so desire, you can also use the jpackage too to do the same for linux or any other OS with full-featured JDKs available.  However, at this time, you cannot natively compile DumbSyslogViewer with GraalVM's native-image tool.  Well, you can compile it, but it won't run.  There's currently an issue with Java Swing (the GUI framework that currently powers the viewer) and the native-image tool.  Since Java Swing is a dead framework, this realistically isn't an issue that will be resolved.