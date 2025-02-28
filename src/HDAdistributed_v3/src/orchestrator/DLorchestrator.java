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

/** Network orchestrator that runs the distributed multi-threaded algorithm. */
public class DLorchestrator extends Thread{
	
	/** Network object. */
	network.FTnetwork net;
	/** Network id. */
	int netid=-1;
	/** Simulation iteration, request ID. */
	int iteration=0;
	/** Size of servers registry. */
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
	/** Produced mapping. */
	int[] mapping= {-1};
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
	
	/** Agent modes */
	Double mode=2.0;		//agent modes;1.0 greedy;2.0 train and run network
							//;3.0 train new network in each request; 4.0 run only stored model
	/** Maximum HDA-graph size */
	int maxhdasize=0;	
	
	/** Construct distributed algorithm object on input of network and algorithm mode. */
	public DLorchestrator(network.FTnetwork net, Double mode) {
		this.mode=mode;
		this.net=net;
		this.servperrack=net.getservperrack();
		this.sr=(net.getservers()*(net.getservers()-1)/2);
		
		for(int s=0;s<net.getservers();s++) {
			net.getserver(s).agent=new Agent();
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
			msg0[m+3]=hdagraph.getnodew()[m]+0.0;
		}

		solfound=false;
		
		//use cluster formation functions distribute1(), distribute3()
		
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
		
		if(!solfound) {
			reject=true;
		}
		
		//print result on screen
		
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
				System.out.println(" training NN in DCs...");
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
		//long startTime=System.nanoTime();
		Double[] m0={2.0, mode};

		//get the number of running threads before computing the mappings in the agents
		int trds=Thread.getAllStackTraces().keySet().size();
		
		for(int c=0;c<net.getservers();c++) {
			(new agentthread(m0, c)).start();			//uncomment for MULTI-THREAD
			//sendmessage(new network.Message(m0), c);	//uncomment for SINGLE THREAD
		}
		
		while(Thread.getAllStackTraces().keySet().size()>trds) {
			//wait for threads running in the agents to finish
		}
				
		fitness=-1.0;
		int mod=0;
		
		for(int i1=0;i1<net.getservers();i1++) {
			Agent ag=(Agent) net.getserver(i1).getagent();
			if(ag.getfitness()>=0 && ag.getfitness()<1000000.0) {
				if(fitness<0.0 && ag.getfitness()>=0.0) {
					fitness=ag.getfitness();
					mapping=ag.getoutput();
					solfound=true;
					mod=i1;
				}else if(ag.getfitness()<fitness && ag.getfitness()<1000000.0) {
					fitness=ag.getfitness();
					mapping=ag.getoutput();
					solfound=true;
					mod=i1;
				}
			}
			
			if(mapping[0]<0) {
				fitness=-1.0;
			}else if(mapping.length>1 && net.checkembed(hdagraph, mapping)) {
				fitness=-1.0;
				solfound=false;
			}
			
		}
		
		if(fitness>=0.0 && fitness<1000000.0) {
			Agent ag=(Agent) net.getserver(mod).getagent();
			solutionew=ag.getresmodel();
		}else {
			Agent ag=(Agent) net.getserver(0).getagent();
			currentmodel=ag.getcurrentmodel();
		}
	}
	
	/** Cluster formation function #0. */
	public void distribute0() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous VNF-chain data
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getservreg(r2);
			//check demands
			if(net.getserver(t2[0]).getavailablecpu()>=hdagraph.minnodew &&
					net.getserver(t2[1]).getavailablecpu()>=hdagraph.minnodew) {
							addnodes(t2[0],t2[1]);
			}
		}
	}
	
	/** Cluster formation function #1. */
	public void distribute1() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous VNF-chain data
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		//for every server check if it can host the minimum demand and does not exit maximum VNF demand
		//if so use its adjacent nodes as candidate hosts
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getservreg(r2);
			//check demands
			if(net.getserver(t2[0]).getavailablecpu()<=hdagraph.maxnodew &&
					net.getserver(t2[1]).getavailablecpu()<=hdagraph.maxnodew &&
						(getminband(t2[0], t2[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
							addnodes(t2[0],t2[1]);
			}
		}
		
	}
	
	/** Cluster formation function #1m. */
	public void distribute1m() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server

		//initialize distributed computation, erase previous VNF-chain data
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		//for every server check if it can host the minimum demand and does not exit maximum VNF demand
		//if so use its adjacent nodes as candidate hosts
		for(int r2=(sr-1);r2>(-1);r2--) {
			int[] t2=net.getservreg(r2);
			//check demands
			if(net.getserver(t2[0]).getavailablecpu()<=hdagraph.maxnodew &&
					net.getserver(t2[1]).getavailablecpu()<=hdagraph.maxnodew &&
						(getminband(t2[0], t2[1]).intValue()*1000)>=(hdagraph.minedgew*1.0)) {
							addnodes(t2[0],t2[1]);
			}
		}
		
	}
	
	/** Cluster formation function #2. */
	public void distribute2() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server

		//initialize cluster formation
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		//for every pair of servers check if they can host the maximum demand VNF
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getservreg(r);
			
			if(net.getserver(t[0]).getavailablecpu()>hdagraph.maxnodew &&
					net.getserver(t[1]).getavailablecpu()>hdagraph.maxnodew &&
						(getminband(t[0], t[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
							addnodes(t[0],t[1]);
			}
		}
	}
	
	/** Cluster formation function #3. */
	public void distribute3() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server
		
		//initialize distributed computation, erase previous VNF-chain data		
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		//for every pair of servers check demands
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getservreg(r);
			
			if(net.getserver(t[0]).getavailablecpu()>=hdagraph.minnodew &&
					net.getserver(t[1]).getavailablecpu()>=hdagraph.minnodew &&
						(getminband(t[0], t[1]).intValue()*1000)>=(hdagraph.maxacedgew*1.0)) {
							addnodes(t[0],t[1]);
			}
		}
	}
	
	/** Cluster formation function #3m. */
	public void distribute3m() {
		servercnt=new int[net.getservers()];	//measures the cluster nodes sent to each server
		
		//initialize distributed computation, erase previous VNF-chain data		
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg0), s);
		}
		
		for(int sn=0;sn<net.getservers();sn++) {
			addnode(sn);
		}
		
		//for every pair of servers check demands
		//if so use its adjacent nodes as candidate hosts
		for(int r=(sr-1);r>(-1);r--) {
			int[] t=net.getservreg(r);
			
			if(net.getserver(t[0]).getavailablecpu()>=hdagraph.minnodew &&
					net.getserver(t[1]).getavailablecpu()>=hdagraph.minnodew &&
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
	
	/** Print successful mapping. */
	public void printmapping() {
		System.out.println("#"+netid+":"+Arrays.toString(mapping));
	}
	
	// getters setters
	/** Get minimum available bandwidth between two nodes. */
	private Double getminband(int a, int b) {
		int[] temp=net.getserverpath(a, b);
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
	}
	
	/** Set VNF lifecyle. */
	public void setduration(int d) {
		duration=d;
	}
	
	/** Set cluster size. */
	public void setclustersize(int fnodes1) {
		fnodes=fnodes1;
	}

	/** Set maximum node size. */
	public void setmaxhdasize(int maxsize) {
		Double[] msg4={4.0, maxsize*1.0};
		for(int s=0;s<net.getservers();s++) {
			sendmessage(new network.Message(msg4), s);
		}
		maxhdasize=maxsize;
	}
	
	/** Set true to print the output on screen. */
	public void setprintmapping(boolean b){
		printmapping=b;
	}
	
	/** Set iteration. */
	public void setiteration(int s) {
		iteration=s;
	}
	
	/** Set domain id. */
	public void setnetid(int s) {
		netid=s;
	}
	
	/** Set mode. */
	public void setmode(double a) {
		mode=a;
	}
	
	/** Set iteration. */
	public int getnetid() {
		return netid;
	}
	
	/** Set latest computed mapping */
	public void setmapping(int[] m) {
		mapping=m;
	}
	
	/** Get latest computed mapping */
	public int[] getmapping() {
		return mapping;
	}
	
	/** Get iteration. */
	public int getiteration() {
		return iteration;
	}
	
	/** Add another node to cluster by sending message to the agent. */
	private void addnode(int a) {
			Double[] h2={(1+0.0), (a+0.0),net.getserver(a).getavailablecpu()};
			sendmessage(new network.Message(h2), a);
			servercnt[a]++;
	}
	
	/** Send candidate hosts to distributed functions. */
	private void addnodes(int a, int b) {
		if(servercnt[b]<clustersize) {
			Double[] h1={(1+0.0), (a+0.0),net.getserver(a).getavailablecpu()};
			sendmessage(new network.Message(h1), b);
			servercnt[b]++;
		}
		
		if(servercnt[a]<clustersize) {
			Double[] h2={(1+0.0), (b+0.0),net.getserver(b).getavailablecpu()};
			sendmessage(new network.Message(h2), a);
			servercnt[a]++;
		}
	}
	
	/** Send messages to servers. */
	private void sendmessage(network.Message m, int server) {
		Agent agen=(Agent) net.getserver(server).getagent();
		agen.getmessage(m);
	}
	
	/** Check bandwidth. */
	public boolean chkbnd(int snode, int vnode) {
		
		boolean r=true;
		int[] tmap=mapping;
		tmap[vnode]=snode;
		Double[] tempbandw=new Double[net.links.size()];
		
		int[] tempload=new int[net.getservers()];
		
		for(int n=0; n<hdagraph.getnodes();n++) {
			if(tmap[n]>(-1)) {
				if((hdagraph.getnodew()[n]+tempload[net.getserver(tmap[n]).getid()]+
						net.getserver(tmap[n]).getcpuload()) >(net.getserver(tmap[n]).getcpu())) {
					r=false;
					err="CPU constraint";
				}else {
					tempload[net.getserver(tmap[n]).getid()]+=(hdagraph.getnodew()[n]);
				}
			}
		}
		
		if(r) {
			for(int i=0;i<tempbandw.length;i++) {
				tempbandw[i]=0.0;
			}

			for(int l=0;l<hdagraph.getgraph().length;l++) {
				if(hdagraph.getgraph()[l]>0) {
					int[] t1=cod.decoder(l);
					if(tmap[t1[0]]>=0 && tmap[t1[1]]>=0) {
						int[] t2=net.getserverpath(tmap[t1[0]],tmap[t1[1]]);
						for(int tt=0;tt<(t2.length-1);tt++) {
							if(t2[tt]!=t2[tt+1] && t2[tt]>=0 && t2[tt+1]>=0) {
								int tempve=cod.coder(t2[tt],t2[tt+1]);
								tempbandw[tempve]+=(0.0+(hdagraph.getedgew()[l]/1000.0));
								if((net.links.get(tempve).getload()+tempbandw[tempve]) >
													net.links.get(tempve).getcapacity()) {
										r=false;
										err="Bandwidth constraint";
								}	
							}
						}
					}
				}
			}
		}
		return r;
	}
	
	
	/** Store model. */
	public void storemodel(int sz, String b) {

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;

		try {
			try {
				fw = new FileWriter("model.csv",true);
			} catch (IOException e) {
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






