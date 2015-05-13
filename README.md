# Introduction
In a nutshell, ACS is a discrete event based simulator for cloud environments that
aims to provide full support for the simulation of any Cloud Computing Aspect.

# About
Acs is part of a research project in the [University of Oran1 Ahmed Benbella (Algeria)](http://www.univ-oran.dz/).
Acs was developed by Samy Sadi (samy.sadi.contact at gmail) with the
supervision of Belabbas Yagoubi (byagoubi at gmail).

# License
ACS' code is published under the [GNU General Public License, version 3](http://www.gnu.org/licenses/gpl.txt)

Moreover, the source and binary forms of ACS are subject to the following
citation license. By downloading ACS, you agree to cite [the following paper describing ACS](#papers)
in any kind of material you produce where ACS was used to conduct search or experimentation, whether
be it a research paper, dissertation, article, poster, presentation, 
or documentation.

## Papers <a name="papers"></a>
+ Samy Sadi and Belabbas Yagoubi : Acs - advanced cloud simulator: A discrete event based simulator for cloud computing environments.
In: 2nd International Conference on Networking and Advanced Systems. ICNAS 2015. pp. 11–16 (2015). ISBN 978-9931-9142-0-4.

# Downloads
ACS' Jar file can be downloaded [from here](http://github.com/samysadi/acs/releases).

Sample configuration file can be found [here](http://github.com/samysadi/acs/releases).

# Documentation
The Html java documentation can be found [here](http://samysadi.github.io/acs/javadoc/).

# Contributing
We accept Pull Requests. Please use the [google group](https://groups.google.com/d/forum/acs-ml) <acs-ml@googlegroups.com> as the main channel of communication for contributors.

Also make sure to respect the next guidelines:
+ Respect the code style.
+ The simpler diffs the better. For instance, make sure the code was not reformatted.
+ Create separate PR for code reformatting.
+ Make sure your changes does not break anything in existing code. In particular, make sure the existing junit tests can complete successfully.

# Usage
## Minimal working example
Download the latest jar file (see downloads section) and make sure to include it to your build path.
Download the configuration sample file (see downloads section) and unzip it somewhere in your file system.

Create a new class and write the following code:
```java
	public static void main(String[] args) throws Exception {
		Simulator simulator = FactoryUtils.generateSimulator("C:\\acs\\config\\main.config"); //assuming this is the path where you have extracted the downloaded configuration
		simulator.start();
	}
```

## Implementing your own models and algorithms
Assume you want to define a custom VirtualMachine placement policy.
The interface that defines Vm's placement is VmPlacementPolicy in the com.samysadi.acs.service.vmplacement
package.
All you have to do is define your new class that implements that interface, and add a configuration line to tell
ACS to use the new policy.

Assuming your new implementation is named MyVmPlacementPolicy in the package mypackage, then
you have to add the following configuration line (preferably in the classes.config file):
```
VmPlacementPolicy_Class=mypackage.MyVmPlacementPolicy
```

Regarding the implementation, note that you can use simply override the AbstractVmPlacementPolicy.
Such implementation will look something like this:
```java
package mypackage;
public class MyVmPlacementPolicy extends VmPlacementPolicyAbstract {
	@Override
	protected Host _selectHost(VirtualMachine vm, List<Host> poweredOnHosts, List<Host> excludedHosts) {
		Host bestHost;
		Iterator<Host> it = new ShuffledIterator<Host>(poweredOnHosts);
		for (Host candidate: poweredOnHosts) {
			if (excludedHosts != null && excludedHosts.contains(candidate))
				continue;
			if (candidate.getPowerState() == PowerState.ON) {
				final double s = computeHostScore(vm, candidate);
				if (s>0) {
					//Your own code to select the best host
				}
			}
		}
		return bestHost;
	}
}
```

## Mastering the configuration files
Configuration files can be used to modify different simulation parameters
as the network topology, the number of hosts, the configuration of those hosts and so forth.
They can also be used to define user behaviours by generating different workloads.

In another hand, you can also define which output you want to generate (and where you want the output to be saved)
using configuration files.

Besides, configuration files can be used to define custom implementations regarding different parts of ACS.

Make sure to have a look at the configurations samples to learn more about this.  


