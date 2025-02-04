package orchestrator;

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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/** agent that runs on every node of the network */
public class Agent_PoP {
	/** agent type 
	 * set "hypergraph" for single agent in PoP-level orchestrator, "default" for other use
	 */
	String type="default";
	/** cluster nodes */
	ArrayList<Double[]> nodes=new ArrayList<Double[]>();
	/** cluster nodes sorted */
	ArrayList<Double[]> nodessort=new ArrayList<Double[]>();
	/** demands of virtual nodes */
	ArrayList<Double> vnfdem=new ArrayList<Double>();
	/** temporary solution */
	int[] tempsol; 
	/** solution */
	int[] solution; 
	/** algorithm output */
	int[] output;
	/** output fitness */
	Double fitness;
	/** minimum node demand */
	Double mindem=0.0;
	/** maximum node demand */
	Double maxdem=0.0;
	/** cumulative virtual node demands applied in all attempts to compute a valid mapping 
	 * these include using trained models and training new models */
	Double cumdem=0.0;
	/** HDA-graph size */
	int vnfsize=1;
	/** number of cluster nodes inputted to ANN, this enables
	 * the nodes that the ANN accepts as input to be a subset of the cluster nodes */
	int tem1=0;
	/** maximum HDA size equals the output of the algorithm */
	int maxhdasize=0;
	/** number of ANN input nodes */
	int inputs;

	/** counter for the number of times a solution was computed */
	int cnt=0;
	
	/** Complexity reduction criterion. */
	boolean criterion=true;
	
	double[] resmodel={0.0};
	double[] currentmodel={};
	
	public Agent_PoP() {
		
	}
	
	public Agent_PoP(String type) {
		this.type=type;
	}
	
	public Agent_PoP(boolean criterion) {
		this.criterion=criterion;
	}
	
	/** the computations executed by the agent */
	public void compute(int mod) {

		if(!criterion) {
			cumdem=0.0;
		}
		
		Double cumcap=0.0;
		for(int ns=0;ns<nodes.size();ns++) {
			cumcap+=nodes.get(ns)[1];
		}
		
		mindem=vnfdem.get(0);
		for(int m=0;m<vnfsize;m++) {
			if(vnfdem.get(m)<mindem) {
				mindem=vnfdem.get(m);
			}
			cumdem+=vnfdem.get(m);
		}

		if(cumcap>=cumdem) {
			compute_greedy();
		}else {
			fitness=-1.0;
		}
	}
	
	/** Compute distributed greedy algorithm. */
	public void compute_greedy() {
		fitness=0.0;
		for(int s=0;s<output.length;s++) {
			output[s]=-1;
		}
		
		nodessort.clear();

		//sort cluster nodes
		int min=0;
		for(int n2=0;n2<nodes.size();n2++){
			for(int n3=0;n3<nodes.size();n3++){
				if(nodes.get(n3)[1]<nodes.get(min)[1]){
					min=n3;
				}
			}
			
			Double[] t1=nodes.get(min);
			nodessort.add(t1);

			Double[] t2= {-1.0, 1000.0};
			nodes.set(min, t2);
		}

		for(int i1=0;i1<nodessort.size();i1++) {
			while(nodessort.get(i1)[1]>=mindem) {
				int a=-1;
				Double b=100000000.0;	
				for(int i2=0;i2<vnfsize;i2++) {
					if(output[i2]>(-1)) {
					//go on
					}else {
						if((nodessort.get(i1)[1]-vnfdem.get(i2))>0.0) {
							if(b>nodessort.get(i1)[1]-vnfdem.get(i2)) {
								b=nodessort.get(i1)[1]-vnfdem.get(i2);
								a=i2;
							}
						}
					}
				}
				if(a==-1) {
					break;
				}else {
					output[a]=nodessort.get(i1)[0].intValue();
					fitness+=nodessort.get(i1)[1]-vnfdem.get(a);
					Double[] temp= {nodessort.get(i1)[0], (nodessort.get(i1)[1]-vnfdem.get(a))};
					nodessort.set(i1, temp);
				}
			}
		}
		
		for(int s2=0;s2<output.length;s2++) {
			if(output[s2]==(-1)) {
				fitness=-1.0;
				break;
			}
		}
	}
	
	/** Store data in a file. */
	public void storedatainfile(String dt) {
		String path="";
		String filename="data";
		
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;

		try {
			try {
				fw = new FileWriter(path+filename,true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(dt);//Adds an end to the line
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
	
	/** Flag0 of incoming messages. */
	public void flag0(Double[] a) {
		//initialize function
		nodes.clear();
		nodessort.clear();
		vnfdem.clear();
		fitness=0.0;
		vnfsize=a[2].intValue();
		output=new int[vnfsize];
		solution=new int[vnfsize];
		tempsol=new int[vnfsize];
		
		for(int i=3;i<(vnfsize+3);i++) {
			vnfdem.add(a[i]);
		}
	}
	
	/** Flag1 of incoming messages. */
	public void flag1(Double[] a) {
		//add candidate hosts and their capacities
		Double[] temp2= {a[1],a[2]};
		nodes.add(temp2);
	}

	/** Flag2 of incoming messages. */
	public void flag2(int a) {
		//compute mapping
		compute(a);
	}
	
	/** Flag3 of incoming messages. */
	public void flag3() {
		nodes.clear();
		nodessort.clear();
		solution=new int[1];
		fitness=0.0;
	}

	/** Flag4 of incoming messages. */
	public void flag4(int a) {
		maxhdasize=a;
	}
	
	//getters setters
	
	/** Get computation output. */
	public int[] getoutput() {
		return output;
	}
	
	/** Get fitness of generated mapping. */
	public Double getfitness() {
		return fitness;
	}
	
	/** Get nodes that the agent computes. */
	public int[] getnodes() {
		int[] d2=new int[nodes.size()];
		for(int i2=0;i2<d2.length;i2++) {
			d2[i2]=nodes.get(i2)[0].intValue();
		}
		return d2;
	}
	
	/** Get the weight of the nodes that are computed. */
	public void getnodew() {
		Double[] d2=new Double[vnfdem.size()];
		for(int i2=0;i2<d2.length;i2++) {
			d2[i2]=vnfdem.get(i2);
		}
		System.out.println(Arrays.toString(d2)+"\n--");
	}
	
	/** Get the sorted nodes. **/
	public void getnodessort(int index) {
		int[] d2=new int[nodessort.size()];
		for(int i2=0;i2<d2.length;i2++) {
			d2[i2]=nodessort.get(i2)[index].intValue();
		}
		System.out.println(Arrays.toString(d2));
	}
	
	/** Get cluster sized. **/
	public int getclustersize() {
		return nodes.size();
	}
	
	/** Messages from controller. */
	public void getmessage(network.Message m) {
		if(m.getmessage()[0]==1) {
			flag1(m.getmessage());
		}else if(m.getmessage()[0]==0) {
			flag0(m.getmessage());
		}else if(m.getmessage()[0]==2) {
			flag2(m.getmessage()[1].intValue());
		}else if(m.getmessage()[0]==3) {
			flag3();
		}else if(m.getmessage()[0]==4) {
			flag4(m.getmessage()[1].intValue());
		}
	}
	
	/** Set maximum HDA-graph size equals the output of the algorithm. */
	public void setmaxhdasize(int s) {
		maxhdasize=s;
	}
}
