# jar executable and parameter files

Start the simulation by running.

```

java -Xmx4g -jar HDA_Placement.jar

```

Running parameters for all supported algorithms are defined in the following files

* **HDA_Placement.jar**. Executable .jar file.

* **BtEurope.evind**. PoP-level topology.

* **configuationsfile**. HDA configurations.

* **model.csv**. Pre-trained NN models.

* **parameters**. Parameters for running the simulation.

* **nnprofile**. Artificial Neural Network topology description.

* **settings**. Default configuration for running the Parameter Adjustment Genetic Algorithm.

* **gasettings**. Configuration for running the Neural Network training Genetic Algorithm.

* **profile**. The configuration and the parameters for running the Parameter Adjustment procedure.

output:

```
<Request ID (HDA configuration ID + request serial)>:
[mapping of HDA-graph segments to PoPs]
-------
#<PoP ID>[mapping of components to servers]
#<PoP ID>[mapping of components to servers]
```

example:

```
3000001:
[22, 22, 22, 1, 1]
-------
#22[0, 0, 0]
#1[0, 0]
```

The simulation on default configuration generates a log file for the meta-orchistrator named *simulationresult-distr-HG.csv*

<details>
<summary>fields descriptions</summary>

1.Request serial number

2.Hosted VNs

3.Used bandwidth

4.Available bandwidth

5.Used cpu

6.Used servers; servers that host some VN

7.Remaining cpu

8.Intra-rack traffic

9.Inter-rack traffic

10.Is last embedded rejected?

11.Acceptance ratio

12.Request revenue

13.Embedding cost

14.Cost/Revenue ratio

15.Remaining intra-rack bandwidth

16.Remaining inter-rack bandwidth

17.Request hop count

18.Request ID

</details>

Changing storestats parameter in the parameters file to *false* generates log files for all PoPs named *simulationresult-distr-<PoP ID>.csv*
<details>
<summary>fields descriptions</summary>

1.Request serial number

2.Hosted VNFs

3.Embedded Service Function Chains

4.Used bandwidth

5.Available bandwidth

6.Used cpu

7.Used servers; servers that host some VNF

8.Remaining cpu

9.Intra-rack traffic

10.Inter-rack traffic

11.Intra-server virtual traffic

12.Is last embedded rejected?

13.Acceptance ratio

14.Request revenue

15.Embedding cost

16.Cost/Revenue ratio

17.Used physical links; links with traffic

18.Size of VNF-graph

19.Remaining intra-rack bandwidth

20.Remaining outer-rack bandwidth

21.Request ID

</details>