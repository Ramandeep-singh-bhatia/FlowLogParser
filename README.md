# FlowLogParser - README

## Overview
The **FlowLogParser** is a Java program designed to parse flow logs, process associated lookup tables, and protocol numbers data to produce aggregated output regarding flow log tags and port protocol combination count.

## Assumptions
- The program only supports the default log format (standard flow log format with space-separated values).
- Only version 2 of the flow log format is supported, which include
  
  **Accepted and rejected traffic** 
    ```
    2 123456789012 eni-1a2b3c4d 10.0.1.102 172.217.7.228 1030 31 17 8 4000 1620140661 1620140721 ACCEPT OK
    2 123456789012 eni-5f6g7h8i 10.0.2.103 52.26.198.183 56000 25 6 15 7500 1620140661 1620140721 REJECT OK
    ``` 
  **No data and skipped records**
    ```
    2 123456789010 eni-1235b8ca123456789 - - - - - - - 1431280876 1431280934 - NODATA
    2 123456789010 eni-11111111aaaaaaaaa - - - - - - - 1431280876 1431280934 - SKIPDATA 
    ```
  **IPV6 Traffic** 
    ```
    2 123456789010 eni-1235b8ca123456789 2001:db8:1234:a100:8d6e:3477:df66:f105 2001:db8:1234:a102:3304:8879:34cf:4071 34892 110 6 54 8855 1477913708 1477913820 ACCEPT OK 
    ```
- If the flow log has record other than version 2, it skips the records, make an entry in the error log file and continue processing the rest of the valid version 2 flow log records.
- The `lookup_table.csv` file must contain at least three columns: destination port, protocol number, and a tag.
- The `protocol-numbers.csv` file used is downloaded from the [Protocol Numbers](https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml). It contains the protocol number and name. The protocol number can be a single value or a range (e.g., 6 for TCP or 147-252 for unassigned).
- Any protocol number that is not present in the `protocol-numbers.csv` file will be classified as "unknown"
- If the program encounters any issues with reading files or parsing flow log records, it logs the error to a dedicated error log file, and the output file only contains headers.
- Output file and error log file will have a time stamp as suffix in the name. 

## Files
- **lookup_table.csv**: A CSV file mapping destination ports and protocols to tags.
- **protocol-numbers.csv**: A CSV file mapping protocol numbers to names (e.g., 6 -> "TCP").
- **flow_logs.txt**: A text file containing flow logs that must be parsed.
- **output.txt**: This is the output file where the aggregated results will be written. The file's suffix will have a timestamp.
- **error-log.txt**: This is the error log file in case of an unexpected error. The file's suffix will have a timestamp.

## Requirements
- Java 8 or higher

The following files are expected in the current directory where the program is being executed (if paths are not provided):
- `lookup_table.csv`
- `protocol-numbers.csv`
- `flow_logs.txt`

## Steps to run java program

1. Check if Java is Installed:
   You can check if Java is installed on the machine by running:
   ```bash
   java -version
   ```
   If Java is installed, it will show the version. If not, you need to install Java.

2. Install Java (if not installed):
  If Java is not installed, you'll need to install it. The installation steps depend on the operating system you're using.
    * Windows: Download the Java installer from [Oracle's website](https://www.oracle.com/java/technologies/downloads/#java11?er=221886)
    * Linux (Ubuntu): Use the package manager to install OpenJDK:
      ```bash
      sudo apt update
      sudo apt install openjdk-11-jdk
      ```
    * macOS: You can use [Homebrew](https://brew.sh/) to install Java:
      ```bash
      brew install openjdk@11
      ```
      
3. Set the Java Environment Variables:
   Once Java is installed, you should set the JAVA_HOME environment variable and update the PATH.
    * Windows: Set JAVA_HOME and add it to the PATH in the system environment variables.
    * Linux/macOS: You can add these lines to your .bashrc or .zshrc file:
      ```bash
      export JAVA_HOME=/path/to/your/java
      export PATH=$JAVA_HOME/bin:$PATH
      ```
   Then run source ~/.bashrc or source ~/.zshrc to apply the changes.
      
4.  Navigate to the Folder containing the .java File:
    Open the terminal or command prompt and navigate to the directory where your Java source file is located:
     ```bash
      cd /path/to/your/java/file
     ```
5. Compile the program:

   If you have saved the code as FlowLogParser.java, open a terminal and run:
   ```bash
   javac FlowLogParser.java
   ```
   
6. Run the program:

   After compiling, you can run the program using the following command:
   ```bash
    java FlowLogParser
The program will ask for file paths for the lookup table, protocol Numbers, and flow logs file. If you press enter without providing a path and the file name, it will use the default values.
```
Enter the path for the lookup table CSV file (default:$UserDirectory/lookup_table.csv): 
Enter the path for the protocol numbers CSV file (default: $UserDirectory/protocol-numbers.csv): 
Enter the path for the flow logs text file (default: $UserDirectory/flow_logs.txt): 
```

# Test Cases

## 1. Basic Test Case:

**Input Files:**
- `lookup_table.csv` with entries for port and protocol tags.
- `protocol-numbers.csv` with protocol numbers mapped to names.
- `flow_logs.txt` with a standard flow log format (e.g., `2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK`).

**Expected Output:**
- Tag counts for different tags (based on the destination port and protocol).
- Protocol combination counts for destination port and protocol.

---

## 2. No Matching Tag in Lookup Table:

**Input:**
- Flow logs with destination ports and protocols that do not match any tag in the lookup table.

**Expected Output:**
- `"Untagged"` should appear in the tag counts with the correct count.
- If no match is found in the lookup table for port-protocol combinations, the tag will be marked as `"Untagged"`.

---

## 3. Unassigned Protocols:

**Input:**
- Flow logs with protocol numbers that fall into the unassigned range (e.g., protocol number `150`).

**Expected Output:**
- The tag for those entries should be `"unassigned"`, and the corresponding counts should be displayed.

---

## 4. Multiple Protocols:

**Input:**
- Flow logs with the same destination port but different protocols (e.g., TCP and UDP).

**Expected Output:**
- Separate counts for each protocol for the same destination port.
- Correct tagging according to the lookup table and protocol.

---

## 5. Protocol Number Range Handling:

**Input:**
- Protocol numbers that fall within a range (e.g., protocol `147-252`).

**Expected Output:**
- The program should correctly identify and classify protocol name as Unassigned within these ranges.

---

## 6. Flow Logs with No data or skipped records:

**Input:**
- Flow logs with no data or skipped records, such as missing protocol numbers or destination ports.

**Expected Output:**
- The program should gracefully skip these lines and continue processing valid data.

---

## 7. Test with Empty Files:

**Input:**
- Empty CSV files for lookup table, protocol number, or flow logs.

**Expected Output:**
- If the lookup table file is empty, there would not be any tags associated with destination port an protocol combination hence output file will have untagged count for the flow logs.
```
Tag Counts:
Tag,Count
Untagged,15
```
- If protocol number file is empty, the protocol names would be unknow as we have nothing to match the number with the name. Also if there is no protocol name as unknow in lookup table, the output file will have the untagged count for flow logs.
```
Tag Counts:
Tag,Count
Untagged,15

Port/Protocol Combination Counts:
Port,Protocol,Count
143,unknown,1
49155,unknown,1
993,unknown,1
49158,unknown,1
49153,unknown,1
49156,unknown,1
80,unknown,1
110,unknown,2
25,unknown,3
49154,unknown,1
31,unknown,1
49157,unknown,1
```
- If flow log is empty, there is nothing to process and the output file will only have the headers
```
Tag Counts:
Tag,Count

Port/Protocol Combination Counts:
Port,Protocol,Count
```
---

## 8. Test for Large Files:

**Input:**
- A large flow logs file of size 10 MB with thousands of records.

**Expected Output:**
- The program should handle large files efficiently and provide the correct aggregation in the output.

---

## 9. Test with Missing Files:

**Input:**
- Provide non-existing files for any input files (lookup table, protocol numbers, flow logs).

**Expected Output:**
- The program should throw a clear exception and terminate, logging the appropriate error message.
- Example: `"Error: The lookup table file 'lookup_table.csv' does not exist."`
- Error log file will be generated with the error message
- Output file will only have the headers

---

## 10. Error Logging:

**Input:**
- Simulate an error (e.g., by providing a wrong file name).

**Expected Output:**
- The program should log the error in the `error_log.txt` file with details, such as the method name, exception type, or error message.

## 11. Invalid Protocol Number:

**Input:**
- Invalid protocol number in the flow log like x
```
2 123456789012 eni-1a2b3c4d 203.0.113.12 192.168.0.1 80 1024 **x** 12 6000 1620140661 1620140721 ACCEPT OK 
```

**Expected Output:**
- The program should log the error in the `error_log.txt` file with error message and continue to process the rest of the record.
- The program will consider this protocol number as unknow.

# Example Input/Output
## lookup_table.csv:

```csv
dstport,protocol,tag 
25,tcp,sv_P1 
49158,udp,sv_P2 
23,tcp,sv_P1 
31,udp,SV_P3 
443,tcp,sv_P2 
22,tcp,sv_P4 
3389,tcp,sv_P5 
0,icmp,sv_P5 
110,tcp,email 
993,tcp,email 
143,udp,email
```

## protocol-numbers.csv:

```csv
ProtocolNumber,ProtocolName
Decimal,Keyword,Protocol,IPv6 Extension Header,Reference
0,HOPOPT,IPv6 Hop-by-Hop Option,Y,[RFC8200]
1,ICMP,Internet Control Message,,[RFC792]
2,IGMP,Internet Group Management,,[RFC1112]
3,GGP,Gateway-to-Gateway,,[RFC823]
4,IPv4,IPv4 encapsulation,,[RFC2003]
5,ST,Stream,,[RFC1190][RFC1819]
6,TCP,Transmission Control,,[RFC9293]
7,CBT,CBT,,[Tony_Ballardie]
8,EGP,Exterior Gateway Protocol,,[RFC888][David_Mills]
9,IGP,"any private interior gateway             
(used by Cisco for their IGRP)",,[Internet_Assigned_Numbers_Authority]
10,BBN-RCC-MON,BBN RCC Monitoring,,[Steve_Chipman]
11,NVP-II,Network Voice Protocol,,[RFC741][Steve_Casner]
12,PUP,PUP,,"[Boggs, D., J. Shoch, E. Taft, and R. Metcalfe, ""PUP: An
Internetwork Architecture"", XEROX Palo Alto Research Center,
CSL-79-10, July 1979; also in IEEE Transactions on
Communication, Volume COM-28, Number 4, April 1980.][[XEROX]]"
13,ARGUS (deprecated),ARGUS,,[Robert_W_Scheifler]
14,EMCON,EMCON,,[Bich_Nguyen]
15,XNET,Cross Net Debugger,,"[Haverty, J., ""XNET Formats for Internet Protocol Version 4"",
IEN 158, October 1980.][Jack_Haverty]"
16,CHAOS,Chaos,,[J_Noel_Chiappa]
17,UDP,User Datagram,,[RFC768][Jon_Postel]
18,MUX,Multiplexing,,"[Cohen, D. and J. Postel, ""Multiplexing Protocol"", IEN 90,
USC/Information Sciences Institute, May 1979.][Jon_Postel]"
19,DCN-MEAS,DCN Measurement Subsystems,,[David_Mills]
20,HMP,Host Monitoring,,[RFC869][Bob_Hinden]
21,PRM,Packet Radio Measurement,,[Zaw_Sing_Su]
22,XNS-IDP,XEROX NS IDP,,"[""The Ethernet, A Local Area Network: Data Link Layer and
Physical Layer Specification"", AA-K759B-TK, Digital
Equipment Corporation, Maynard, MA.  Also as: ""The
Ethernet - A Local Area Network"", Version 1.0, Digital
Equipment Corporation, Intel Corporation, Xerox
Corporation, September 1980.  And: ""The Ethernet, A Local
Area Network: Data Link Layer and Physical Layer
Specifications"", Digital, Intel and Xerox, November 1982.
And: XEROX, ""The Ethernet, A Local Area Network: Data Link
Layer and Physical Layer Specification"", X3T51/80-50,
Xerox Corporation, Stamford, CT., October 1980.][[XEROX]]"
23,TRUNK-1,Trunk-1,,[Barry_Boehm]
24,TRUNK-2,Trunk-2,,[Barry_Boehm]
25,LEAF-1,Leaf-1,,[Barry_Boehm]
26,LEAF-2,Leaf-2,,[Barry_Boehm]
27,RDP,Reliable Data Protocol,,[RFC908][Bob_Hinden]
28,IRTP,Internet Reliable Transaction,,[RFC938][Trudy_Miller]
29,ISO-TP4,ISO Transport Protocol Class 4,,[RFC905][Robert_Cole]
30,NETBLT,Bulk Data Transfer Protocol,,[RFC969][David_Clark]
31,MFE-NSP,MFE Network Services Protocol,,"[Shuttleworth, B., ""A Documentary of MFENet, a National
Computer Network"", UCRL-52317, Lawrence Livermore Labs,
Livermore, California, June 1977.][Barry_Howard]"
32,MERIT-INP,MERIT Internodal Protocol,,[Hans_Werner_Braun]
33,DCCP,Datagram Congestion Control Protocol,,[RFC4340]
34,3PC,Third Party Connect Protocol,,[Stuart_A_Friedberg]
35,IDPR,Inter-Domain Policy Routing Protocol,,[Martha_Steenstrup]
36,XTP,XTP,,[Greg_Chesson]
37,DDP,Datagram Delivery Protocol,,[Wesley_Craig]
38,IDPR-CMTP,IDPR Control Message Transport Proto,,[Martha_Steenstrup]
39,TP++,TP++ Transport Protocol,,[Dirk_Fromhein]
40,IL,IL Transport Protocol,,[Dave_Presotto]
41,IPv6,IPv6 encapsulation,,[RFC2473]
42,SDRP,Source Demand Routing Protocol,,[Deborah_Estrin]
43,IPv6-Route,Routing Header for IPv6,Y,[Steve_Deering]
44,IPv6-Frag,Fragment Header for IPv6,Y,[Steve_Deering]
45,IDRP,Inter-Domain Routing Protocol,,[Sue_Hares]
46,RSVP,Reservation Protocol,,[RFC2205][RFC3209][Bob_Braden]
47,GRE,Generic Routing Encapsulation,,[RFC2784][Tony_Li]
48,DSR,Dynamic Source Routing Protocol,,[RFC4728]
49,BNA,BNA,,[Gary Salamon]
50,ESP,Encap Security Payload,Y,[RFC4303]
51,AH,Authentication Header,Y,[RFC4302]
52,I-NLSP,Integrated Net Layer Security  TUBA,,[K_Robert_Glenn]
53,SWIPE (deprecated),IP with Encryption,,[John_Ioannidis]
54,NARP,NBMA Address Resolution Protocol,,[RFC1735]
55,Min-IPv4,Minimal IPv4 Encapsulation,,[RFC2004][Charlie_Perkins]
56,TLSP,"Transport Layer Security Protocol        
using Kryptonet key management",,[Christer_Oberg]
57,SKIP,SKIP,,[Tom_Markson]
58,IPv6-ICMP,ICMP for IPv6,,[RFC8200]
59,IPv6-NoNxt,No Next Header for IPv6,,[RFC8200]
60,IPv6-Opts,Destination Options for IPv6,Y,[RFC8200]
61,,any host internal protocol,,[Internet_Assigned_Numbers_Authority]
62,CFTP,CFTP,,"[Forsdick, H., ""CFTP"", Network Message, Bolt Beranek and
Newman, January 1982.][Harry_Forsdick]"
63,,any local network,,[Internet_Assigned_Numbers_Authority]
64,SAT-EXPAK,SATNET and Backroom EXPAK,,[Steven_Blumenthal]
65,KRYPTOLAN,Kryptolan,,[Paul Liu]
66,RVD,MIT Remote Virtual Disk Protocol,,[Michael_Greenwald]
67,IPPC,Internet Pluribus Packet Core,,[Steven_Blumenthal]
68,,any distributed file system,,[Internet_Assigned_Numbers_Authority]
69,SAT-MON,SATNET Monitoring,,[Steven_Blumenthal]
70,VISA,VISA Protocol,,[Gene_Tsudik]
71,IPCV,Internet Packet Core Utility,,[Steven_Blumenthal]
72,CPNX,Computer Protocol Network Executive,,[David Mittnacht]
73,CPHB,Computer Protocol Heart Beat,,[David Mittnacht]
74,WSN,Wang Span Network,,[Victor Dafoulas]
75,PVP,Packet Video Protocol,,[Steve_Casner]
76,BR-SAT-MON,Backroom SATNET Monitoring,,[Steven_Blumenthal]
77,SUN-ND,SUN ND PROTOCOL-Temporary,,[William_Melohn]
78,WB-MON,WIDEBAND Monitoring,,[Steven_Blumenthal]
79,WB-EXPAK,WIDEBAND EXPAK,,[Steven_Blumenthal]
80,ISO-IP,ISO Internet Protocol,,[Marshall_T_Rose]
81,VMTP,VMTP,,[Dave_Cheriton]
82,SECURE-VMTP,SECURE-VMTP,,[Dave_Cheriton]
83,VINES,VINES,,[Brian Horn]
84,IPTM,Internet Protocol Traffic Manager,,[Jim_Stevens][1]
85,NSFNET-IGP,NSFNET-IGP,,[Hans_Werner_Braun]
86,DGP,Dissimilar Gateway Protocol,,"[M/A-COM Government Systems, ""Dissimilar Gateway Protocol
Specification, Draft Version"", Contract no. CS901145,
November 16, 1987.][Mike_Little]"
87,TCF,TCF,,[Guillermo_A_Loyola]
88,EIGRP,EIGRP,,[RFC7868]
89,OSPFIGP,OSPFIGP,,[RFC1583][RFC2328][RFC5340][John_Moy]
90,Sprite-RPC,Sprite RPC Protocol,,"[Welch, B., ""The Sprite Remote Procedure Call System"",
Technical Report, UCB/Computer Science Dept., 86/302,
University of California at Berkeley, June 1986.][Bruce Willins]"
91,LARP,Locus Address Resolution Protocol,,[Brian Horn]
92,MTP,Multicast Transport Protocol,,[Susie_Armstrong]
93,AX.25,AX.25 Frames,,[Brian_Kantor]
94,IPIP,IP-within-IP Encapsulation Protocol,,[John_Ioannidis]
95,MICP (deprecated),Mobile Internetworking Control Pro.,,[John_Ioannidis]
96,SCC-SP,Semaphore Communications Sec. Pro.,,[Howard_Hart]
97,ETHERIP,Ethernet-within-IP Encapsulation,,[RFC3378]
98,ENCAP,Encapsulation Header,,[RFC1241][Robert_Woodburn]
99,,any private encryption scheme,,[Internet_Assigned_Numbers_Authority]
100,GMTP,GMTP,,[[RXB5]]
101,IFMP,Ipsilon Flow Management Protocol,,"[Bob_Hinden][November 1995, 1997.]"
102,PNNI,PNNI over IP,,[Ross_Callon]
103,PIM,Protocol Independent Multicast,,[RFC7761][Dino_Farinacci]
104,ARIS,ARIS,,[Nancy_Feldman]
105,SCPS,SCPS,,[Robert_Durst]
106,QNX,QNX,,[Michael_Hunter]
107,A/N,Active Networks,,[Bob_Braden]
108,IPComp,IP Payload Compression Protocol,,[RFC2393]
109,SNP,Sitara Networks Protocol,,[Manickam_R_Sridhar]
110,Compaq-Peer,Compaq Peer Protocol,,[Victor_Volpe]
111,IPX-in-IP,IPX in IP,,[CJ_Lee]
112,VRRP,Virtual Router Redundancy Protocol,,[RFC9568]
113,PGM,PGM Reliable Transport Protocol,,[Tony_Speakman]
114,,any 0-hop protocol,,[Internet_Assigned_Numbers_Authority]
115,L2TP,Layer Two Tunneling Protocol,,[RFC3931][Bernard_Aboba]
116,DDX,D-II Data Exchange (DDX),,[John_Worley]
117,IATP,Interactive Agent Transfer Protocol,,[John_Murphy]
118,STP,Schedule Transfer Protocol,,[Jean_Michel_Pittet]
119,SRP,SpectraLink Radio Protocol,,[Mark_Hamilton]
120,UTI,UTI,,[Peter_Lothberg]
121,SMP,Simple Message Protocol,,[Leif_Ekblad]
122,SM (deprecated),Simple Multicast Protocol,,[Jon_Crowcroft][draft-perlman-simple-multicast]
123,PTP,Performance Transparency Protocol,,[Michael_Welzl]
124,ISIS over IPv4,,,[Tony_Przygienda]
125,FIRE,,,[Criag_Partridge]
126,CRTP,Combat Radio Transport Protocol,,[Robert_Sautter]
127,CRUDP,Combat Radio User Datagram,,[Robert_Sautter]
128,SSCOPMCE,,,[Kurt_Waber]
129,IPLT,,,[[Hollbach]]
130,SPS,Secure Packet Shield,,[Bill_McIntosh]
131,PIPE,Private IP Encapsulation within IP,,[Bernhard_Petri]
132,SCTP,Stream Control Transmission Protocol,,[Randall_R_Stewart]
133,FC,Fibre Channel,,[Murali_Rajagopal][RFC6172]
134,RSVP-E2E-IGNORE,,,[RFC3175]
135,Mobility Header,,Y,[RFC6275]
136,UDPLite,,,[RFC3828]
137,MPLS-in-IP,,,[RFC4023]
138,manet,MANET Protocols,,[RFC5498]
139,HIP,Host Identity Protocol,Y,[RFC7401]
140,Shim6,Shim6 Protocol,Y,[RFC5533]
141,WESP,Wrapped Encapsulating Security Payload,,[RFC5840]
142,ROHC,Robust Header Compression,,[RFC5858]
143,Ethernet,Ethernet,,[RFC8986]
144,AGGFRAG,AGGFRAG encapsulation payload for ESP,,[RFC9347]
145,NSH,Network Service Header,N,[RFC9491]
146,Homa,Homa,N,[HomaModule][John_Ousterhout]
147-252,,Unassigned,,[Internet_Assigned_Numbers_Authority]
253,,Use for experimentation and testing,Y,[RFC3692]
254,,Use for experimentation and testing,Y,[RFC3692]
255,Reserved,,,[Internet_Assigned_Numbers_Authority]
```

## flow_logs.txt:

```csv
2 123456789012 eni-0a1b2c3d 10.0.1.201 198.51.100.2 443 49153 6 25 20000 1620140761 1620140821 ACCEPT OK 
2 123456789012 eni-4d3c2b1a 192.168.1.100 203.0.113.101 23 49154 6 15 12000 1620140761 1620140821 REJECT OK 
2 123456789012 eni-5e6f7g8h 192.168.1.101 198.51.100.3 25 49155 6 10 8000 1620140761 1620140821 ACCEPT OK 
2 123456789012 eni-9h8g7f6e 172.16.0.100 203.0.113.102 110 49156 6 12 9000 1620140761 1620140821 ACCEPT OK 
2 123456789012 eni-7i8j9k0l 172.16.0.101 192.0.2.203 993 49157 6 8 5000 1620140761 1620140821 ACCEPT OK 
2 123456789012 eni-6m7n8o9p 10.0.2.200 198.51.100.4 143 49158 17 18 14000 1620140761 1620140821 ACCEPT OK 
2 123456789012 eni-1a2b3c4d 192.168.0.1 203.0.113.12 1024 80 6 10 5000 1620140661 1620140721 ACCEPT OK 
2 123456789012 eni-1a2b3c4d 203.0.113.12 192.168.0.1 80 25 6 12 6000 1620140661 1620140721 ACCEPT OK 
2 123456789012 eni-1a2b3c4d 10.0.1.102 172.217.7.228 1030 31 17 8 4000 1620140661 1620140721 ACCEPT OK
2 123456789010 eni-1235b8ca123456789 - - - - - - - 1431280876 1431280934 - NODATA
2 123456789010 eni-11111111aaaaaaaaa - - - - - - - 1431280876 1431280934 - SKIPDATA 
2 123456789012 eni-5f6g7h8i 10.0.2.103 52.26.198.183 56000 25 6 15 7500 1620140661 1620140721 REJECT OK
2 123456789010 eni-1235b8ca123456789 2001:db8:1234:a100:8d6e:3477:df66:f105 2001:db8:1234:a102:3304:8879:34cf:4071 34892 110 6 54 8855 1477913708 1477913820 ACCEPT OK 
2 123456789012 eni-9k10l11m 192.168.1.5 51.15.99.115 49321 25 260 20 10000 1620140661 1620140721 ACCEPT OK 
2 123456789012 eni-1a2b3c4d 192.168.1.6 87.250.250.242 49152 110 6 5 2500 1620140661 1620140721 ACCEPT OK 
2 123456789012 eni-2d2e2f3g 192.168.2.7 77.88.55.80 49153 993 6 7 3500 1620140661 1620140721 ACCEPT OK 
2 123456789012 eni-4h5i6j7k 172.16.0.2 192.0.2.146 49154 143 17 9 4500 1620140661 1620140721 ACCEPT OK 
```

## Expected Output in output.txt:

```txt
Tag Counts:
Tag,Count
sv_P2,1
SV_P3,1
sv_P1,2
email,4
Untagged,7

Port/Protocol Combination Counts:
Port,Protocol,Count
49154,tcp,1
25,tcp,2
80,tcp,1
49157,tcp,1
993,tcp,1
49155,tcp,1
110,tcp,2
49153,tcp,1
49158,udp,1
143,udp,1
49156,tcp,1
31,udp,1
25,unknown,1
```

# Error Handling
Any unexpected error is caught and logged into an error_log.txt file, providing a detailed error message including the type of exception and method where it occurred.
If a file doesn't exist or is unreadable, the program will notify the user and terminate.
Invalid flow log lines (such as incomplete data) are skipped with no disruption to valid data processing.

# Conclusion
This program is intended to provide a simple yet powerful utility for parsing and processing flow logs in a standardized format. For the program to work correctly, the input file format must be followed strictly and only version 2 flow logs format must be used. It has been tested with various scenarios, including edge cases and large datasets.

