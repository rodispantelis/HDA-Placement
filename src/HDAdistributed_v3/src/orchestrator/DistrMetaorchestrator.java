package orchestrator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/*   
Copyright 2022 Panteleimon Rodis

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.util.ArrayList;
import java.util.Arrays;

/*   
Copyright 2024 Panteleimon Rodis

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/** Hypernodegraph orchestrator that runs the distributed multi-threaded algorithm. */
public class DistrMetaorchestrator extends Thread{
	
	/** Network object. */
	network.DCnetwork net;
	/** Simulation iteration, request ID. */
	int iteration=0;
	/** Size of domain registry. */
	int sr;
	/** Servers per rack. */
	int servperrack;
	/** Measures the cluster nodes sent to each server. */
	int[] servercnt;
	/** HDA lifecycle duration. */
	int duration=0;
	/** Size of the cluster of candidate hosts. */
	int clustersize;
	/** Parameter to define cluster size. */
	int fnodes;		
	/** Latest produced mapping for last incoming request. */
	int[] mapping= {0};
	/** Are there spatial constraints? */
	boolean spatial=false;
	/** Solution fitness. */
	Double fitness;
	services.HDAgraph hdagraph;
	/** Is request rejected? */
	boolean reject=false;
	/** Print mapping? */
	boolean printmapping=true;
	/** Solution found? */
	boolean solfound=false;
	network.Codec cod= new network.Codec();
	/** error message */
	String err=" ";
	/** Messages to the agents. */
	Double msg0[];			
	/** Recompute in case of failure? */
	boolean recompute=false;	
	/** Solution edge weights in Edge Vector indexing. */
	double[] solutionew;	
	/** Current model in use. */
	double[] currentmodel= {0.0};
	
	/** Subrequest id, when set >-1 the metaorchestrator processes a HDA-graph
	 * par of the incoming request */
	int subgraphid=-1;
	
	/** Disclose total domain capacity. */
	boolean disclose=true;
	
	/** Agent modes */
	Double mode=2.0;		//agent modes: 1.0 greedy
	/** Maximum HDA-graph size */
	int maxhdasize=0;	
	
	public DistrMetaorchestrator(network.DCnetwork net, Double mode) {
		this.mode=mode;
		this.net=net;
		this.sr=(net.getdomains()*(net.getdomains()-1)/2);
		
		for(int s=0;s<net.getdomains();s++) {
			net.getdomain(s).agent=new Agent_PoP("hypergraph");
		}
	}
	
	/** Get request. */
	public void getrequest(services.HDAgraph hdag) {
		hdagraph=hdag;
	}
	
	/** Run distributed Deep Learning algorithm. */
	public void compute() {

		hdagraph.demands();
		clustersize=fnodes*hdagraph.getnodes();
		net.setiteration(iteration);
		msg0=new Double[3+hdagraph.getnodes()];
			
		msg0[0]=0.0;
		msg0[1]=hdagraph.getmaxnodew()+0.0;
		msg0[2]=hdagraph.getnodes()+0.0;
		
		for(int m=0;m<hdagraph.getnodes();m++) {
			msg0[m+3]=hdagraph.getnodew()[m]*1.0;
		}
		
		solfound=false;

		if(hdagraph.hasspatial()) {
		
			if(!solfound) {
				distributebyconstraint();
				computesolution();
			}
		
		}else {
		
			if(!solfound) {
				distribute0();
				computesolution();
			}
		
			if(!solfound) {
				distribute1m();
				computesolution();
			}
		
			if(!solfound) {
				distribute3m();
				computesolution();
			}
		
			if(!solfound) {
				distribute1();
				computesolution();
			}
		
			if(!solfound) {
				distribute3();
				computesolution();
			}
		}
		
		if(!solfound) {
			reject=true;
		}
		
		if(mapping[0]<0) {
			reject=true;
		}

		if(printmapping && !reject) {
			System.out.println(Arrays.toString(mapping));
		}		
		
		if(!reject) {
			int du=(int) (Math.random()*duration);
			net.setduration(du);
			net.embed(hdagraph, mapping);
			String solnew="";
				solnew=Arrays.toString(solutionew);

			if(solnew.length()>5 && mode<4.0) {
				storemodel(hdagraph.nodes, solnew.subSequence(1, (solnew.length()-1)).toString());
			}
			if(mode<4.0) {
				mode=2.0;
			}
		}else {
			if(mode==2.0) {
				System.out.println(" training NN for SG "+subgraphid);
				reject=false;
				mode=3.0;
				recompute=true;
				compute();
				mode=2.0;
			}else if(mode<4.0){
				if(printmapping) {
					System.out.println(" > Rejection.");
				}
				mode=2.0;
			}else if(mode==4.0){
				if(printmapping) {
					System.out.println(" > Rejection.");
				}
			}
		}
	
		if(mode<4.0) {
			mode=2.0;
		}
	}
	
	/** Compute and get the best solution computed by the distributed agents. */
	public void computesolution() {
		//send message to nodes to compute candidate solution nodes
		Double[] m0={2.0, mode};

		//get the number of running threads before computing the mappings in the agents
		int trds=Thread.getAllStackTraces().keySet().size();
		
		for(int c=0;c<net.getdomains();c++) {
			(new agentthread(m0, c)).start();			//uncomment for MULTI-THREAD
			//sendmessage(new network.Message(m0), c);	//uncomment for SINGLE THREAD
		}
		
		while(Thread.getAllStackTraces().keySet().size()>trds) {
			//wait for threads running in agents to finish
		}
		
		fitness=-1.0;
		
		for(int i1=0;i1<net.getdomains();i1++) {
			Agent_PoP ag=(Agent_PoP) net.getdomain(i1).getagent();
			if(ag.getfitness()>=0) {
				if(fitness<0.0 && ag.getfitness()>=0.0) {
					fitness=ag.getfitness();
					mapping=ag.getoutput();
					solfound=true;
				}else if(ag.getfitness()<fitness) {
					fitness=ag.getfitness();
					mapping=ag.getoutput();
					solfound=true;
				}
			}

			if(mapping[0]<0) {
				fitness=-1.0;
			}else if(mapping.length>1 && net.checkembed(hdagraph, mapping)) {
				fitness=-1.0;
				solfound=false;
			}
			
		}
		
		for(int m=0;m<mapping.length;m++) {
			if(mapping[m]<0) {
				fitness=-1.0;
				solfound=false;
			}
		}
	}
	
	/** Cluster formation function based on spatial constraint. */
	public void distributebyconstraint() {
	
		servercnt=new int[net.getdomains()];
	
		//initialize distributed computation, erase previous HDA data
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
			if((hdagraph.spatial[0]==s) &&
					(net.getdomain(s).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes())) {
				addnode(s);
			}
		}
	}
	
	
	/** Cluster formation function #0. */
	public void distribute0() {
		
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous HDA data
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every server check if it can host the minimum demand and does not exit maximum VN demand
		//if so use its adjacent nodes as candidate hosts
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getdomreg(r2);
				
			if(net.getdomain(t2[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t2[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes()){
								addnodes(t2[0],t2[1]);
			}
		}
	}
	
	/** Cluster formation function #1. */
	public void distribute1() {
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous HDA data
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every server check if it can host the minimum demand and does not exit maximum VN demand
		//if so use its adjacent nodes as candidate hosts
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getdomreg(r2);
			//check demands
			
			if(net.getdomain(t2[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t2[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes() &&
								(getminband(t2[0], t2[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
									addnodes(t2[0],t2[1]);
			}
		}
	}
	
	/** Cluster formation function #1m. */
	public void distribute1m() {
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous HDA data
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every server check if it can host the minimum demand and does not exit maximum VN demand
		//if so use its adjacent nodes as candidate hosts
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getdomreg(r2);
			//check demands
			if(net.getdomain(t2[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t2[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes() &&
								(getminband(t2[0], t2[1]).intValue()*1000)>=(hdagraph.minedgew*1.0)) {
									addnodes(t2[0],t2[1]);
			}
		}
	}
	
	/** Cluster formation function #2. */
	public void distribute2() {
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server

		//initialize cluster formation
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every pair of servers check if they can host the maximum demand VN
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getdomreg(r);
			//check demands
			if(net.getdomain(t[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes() &&
								(getminband(t[0], t[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
									addnodes(t[0],t[1]);
			}
		}
	}
	
	/** Cluster formation function #3. */
	public void distribute3() {
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server
		
		//initialize distributed computation, erase previous HDA data		
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every pair of servers check demands
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getdomreg(r);
			//check demands
			if(net.getdomain(t[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes() &&
								(getminband(t[0], t[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
									addnodes(t[0],t[1]);
			}
		}
	}
	
	/** Cluster formation function #3m. */
	public void distribute3m() {
		servercnt=new int[net.getdomains()];	//measures the cluster nodes sent to each server
		
		//initialize distributed computation, erase previous HDA data		
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg0), s);
		}

		//for every pair of servers check demands
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getdomreg(r);
			//check demands
			if(net.getdomain(t[0]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
					hdagraph.cpugetdemand()/hdagraph.getnodes()
						&& net.getdomain(t[1]).getavailablenodecpu()[0]/net.getdomain(0).getservers() >= 
							hdagraph.cpugetdemand()/hdagraph.getnodes() &&
								(getminband(t[0], t[1]).intValue()*1000)>=(hdagraph.minedgew*1.0)) {
									addnodes(t[0],t[1]);
			}
		}
	}
	
	/** Request rejected? */
	public boolean isrejected() {
		return reject;
	}
	
	/** Is node in list? */
	public boolean inlist(int n, ArrayList<Integer> pnodes) {
		boolean r=false;
		ArrayList<Integer> temp=pnodes;
		
		for(int i=0; i<temp.size();i++) {
			if(n==temp.get(i)) {
				r=true;
				break;
			}
		}
		
		return r;
	}
	
	/** Print last successful mapping. */
	public void printmapping() {
		System.out.println(Arrays.toString(mapping));
	}
	
	// getters setters
	/** Get minimum available bandwidth between two nodes. */
	private Double getminband(int a, int b) {
		int[] temp=net.domainpath(a, b);
		Double minband=net.getband(temp[0], temp[1]);
		
		for(int t=2;t<temp.length;t++) {
			if(minband>net.getband(temp[t-1], temp[t])) {
				minband=net.getband(temp[t-1], temp[t]);
			}
		}
		return minband;
	}
	
	/** Apply or deprecate spatial constraints. */
	public void setspatial(boolean tf) {
		spatial=tf;
		if(tf) {
			hdagraph.setspatial();
		}else{
			hdagraph.remspatial();
		}
	}
	
	/** Set HDA lifecyle. */
	public void setduration(int d) {
		duration=d;
	}
	
	/** Set latest computed mapping. */
	public void setmapping(int[] m) {
		mapping=m;
	}
	
	/** Set cluster size. */
	public void setclustersize(int fnodes1) {
		fnodes=fnodes1;
	}

	/** Set maximum node size. */
	public void setmaxhdasize(int maxsize) {
		Double[] msg4={4.0, maxsize*1.0};
		for(int s=0;s<net.getdomains();s++) {
			sendmessage(new network.Message(msg4), s);
		}
		maxhdasize=maxsize;
	}
	
	/** Set subgraph id when the metaorchestrator processes a HDA-subgraph that is a part of the incoming request */
	public void setsubgraphid(int a) {
		subgraphid=a;
	}
	
	/** Set true to print the output on screen. */
	public void setprintmapping(boolean b){
		printmapping=b;
	}
	
	/** Set disclosure */
	public void setdisclose(boolean a) {
		disclose=a;
	}
	
	/** Set iteration. */
	public void setiteration(int s) {
		iteration=s;
	}
	
	/** Get latest computed mapping. */
	public int[] getmapping() {
		return mapping;
	}
	
	/** Get iteration. */
	public int getiteration() {
		return iteration;
	}
	
	/** Add another node to cluster by sending message to the agent. */
	private void addnode(int a) {
		Double[] h2={(1+0.0), (a+0.0),net.getdomain(a).getavailablenodecpu()[0]};
		sendmessage(new network.Message(h2), a);
		servercnt[a]++;
	}
	
	/** Send candidate hosts to distributed functions. */
	private void addnodes(int a, int b) {
		if(servercnt[b]<clustersize) {
			Double[] h1={(1+0.0), (a+0.0),net.getdomain(a).getavailablenodecpu()[0]};
			sendmessage(new network.Message(h1), b);
			servercnt[b]++;
		}

		if(servercnt[a]<clustersize) {
			Double[] h2={(1+0.0), (b+0.0),net.getdomain(b).getavailablenodecpu()[0]};
			sendmessage(new network.Message(h2), a);
			servercnt[a]++;
		}
	}
	
	/** Send messages to servers. */
	private void sendmessage(network.Message m, int server) {
		Agent_PoP agen=(Agent_PoP) net.getdomain(server).getagent();
		agen.getmessage(m);
	}
	
	/** Store model. */
	public void storemodel(int sz, String b) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;

		try {
			try {
				fw = new FileWriter("model-hg.csv",true);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(sz+"|"+b);
			pw.flush();
		}finally {
	        try {
	             pw.close();
	             bw.close();
	             fw.close();
	        } 
	        catch (IOException io) { 
	        }
		}
	}
	
	
	/** Thread that runs agent. */
	class agentthread implements Runnable {
		private Thread t;
		private String threadName="agent";
		network.Message nm;
		int c;
		   
		public agentthread(Double[] m0, int c) {
			this.c=c;
			this.nm=new network.Message(m0);
		}
		   
		public void run() {
			sendmessage(nm, c);   
		}
		   
		public void start () {
		    t = new Thread (this, threadName);
		    t.start ();
		}
	}
}






