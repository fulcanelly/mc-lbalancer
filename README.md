# loadbalancer

### What is it?

This is a tool to make minecraft server work only when it's really needed, by watching proxy connections

### Motivation 

* Some hostings take payments only for uptime.
* Some actions (like a backup) require the server to be off for a continuous time. The plugin dedicated to detect that time
* If you running a few servers on the same machine you can reduce needless load using this
 
### How it works ?

Currently it have only one mode — **all to one**, it means all servers depends from one ([in conifg](src.java/bungee/src/main/resources/config.yml) it called lobby).
If that server is empty — dependent servers will eventually shutdown, if at least one player enter it — dependent will start.


### Installation 

1. Configure, build and run haskell deamon — it responsible to how and when start / stop servers
2. Build and copy front-end plugin to proxy (BungeeCord / Velocity)

### Future plans 

* Currently only one watched server is permitted and rest considered as it's dependent, so that is things to work on.
* Add prediction of online for enabling server in advance 
