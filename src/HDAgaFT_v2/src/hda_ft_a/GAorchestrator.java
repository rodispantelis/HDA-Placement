package hda_ft_a;
import java.io.File;
import java.io.IOException;

import services.HDAgraph;

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

/** DC orchestrator running GA */
public class GAorchestrator extends Thread{
	
	/** Genetic Algorithm parameters */
	int pnodes=0, genes=0, generations=0, supergen=0, mupr=1, crpr=1;
	/** specify the HDA-graph file names */
	int r1,r2;
	/** iteration id */
	int d=0;
	/** HDA-graph length */
	int dd2=0;
	int netstat=0;
	int iteration=0;
	/** Genetic Algorithm */
	GA ga;
	/** PAGA setup database */
	Kdb kdb= new Kdb();
	/** set to true to delete previous knowledge of PAGA */
	boolean deletedb=true;						
	/** set true to use PAGA */
	boolean paga=true;							
	/** use population generation heuristic */
	boolean popgenheuristic=true;				
	/** default setup */
	Setup defsetup;								
	/** GA tuning mechanism (PAGA) */
	hda_ft_b.Tuning tuning = new hda_ft_b.Tuning();	
	/** number of classes of the network based on traffic load */
	int netclasses=4;							
	/** HDA-graph */
	services.HDAgraph cnet;
	/** substrate network */
	network.FTnetwork pnet;
	/** print mapping on screen*/
	boolean printmapping=true;
	/** number of iterations, in each iteration one request is computed */
	int iterations=6000;
	/** embedded VNs maximum life cycle duration */
	int duration=0;
	/** is last request rejected? */
	boolean rejection=false;
	/** path to stored HDAs */
	String EVpath="";							
	/** path to log files */
	String path="";
	/** kdb storage file */
	File kdbf = new File(path+"knowledgeDB.csv");
	/** file to store the adaptation of the optimal setup found by PAGA in the functionality of the GA */
	File adl = new File(path+"adaptationlog.csv");

	
	/** initialize network object, setup database, delete old log files */
	public GAorchestrator(network.FTnetwork net, Setup setup, HDAgraph hdagraph) {
		
		this.pnet=net;
		this.defsetup=setup;
		this.cnet=hdagraph;
		
		if(adl.exists()) {
			adl.delete();
		}
		
		if(!new File("knowledgeDB.csv").exists()) {
			kdb.addsetup(defsetup);
		}
		
		if(kdb.exists(0,0)) {
			kdb.addsetup(defsetup);
		}
	}	

	// setters
	/** set duration */
	public void setduration(int d) {
		duration=d;
	}
	
	/** set simulation iterations */
	public void setiterations(int i) {
		iterations=i;
	}
	
	/** set path to the stored HDA-graph files */
	public void setEVpath(String s){
		EVpath=s;
	}
	
	/** set number of netclasses */
	public void setnetclasses(int cl){
		netclasses=cl;
	}
	
	/** set parameters that determine HDA-graph filenames */
	public void setr1r2(int ra1, int ra2) {
		r1=ra1;
		r2=ra2;
	}
	
	/** set boolean parameters for using PAGA and population generation heuristic
	 * and for deleting previous stored setups in kdb
	 */
	public void setboolparams(boolean[] boolparams) {
		if(boolparams[0]) {
			paga=true;
		}else {
			paga=false;
		}
		
		if(boolparams[1]) {
			deletedb=true;
		}else {
			deletedb=false;
		}
		
		if(boolparams[2]) {
			popgenheuristic=true;
		}else {
			popgenheuristic=false;
		}
		
		if(deletedb) {
			if(kdbf.exists()) {
				kdbf.delete();
			}
		}else {
			if(kdbf.exists()) {
				kdb.loaddb(path+"knowledgeDB.csv");
			}
		}
		
		if(!kdbf.exists()) {
			try {
				kdbf.createNewFile();
				kdb.addsetup(defsetup);
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** set iteration id */
	public void setid(int a) {
		d=a;
	}
	
	/** print mapping on screen */
	public void printmapping(boolean b) {
		printmapping=b;
	}
	
	public int[] getmapping() {
		return ga.best.getbestmapping();
	}
	
	/** initialize simulation */
	public void init() {
		pnet.setiteration(d);

		ga=new GA(pnet);
		ga.setpopgenheuristic(popgenheuristic);
		ga.setduration(duration);
		ga.setid(d);
		ga.printmapping(printmapping);

		initsim1(cnet, cnet.nodes, d);
	}
	
	/** simulation stage 1 */
	public void initsim1(services.HDAgraph hdagraph, int r, int d) {
		iteration=d;
		dd2=r;
		cnet=hdagraph;
		initsim2(defsetup);
	}

	public hda_ft_b.Tuning getwait() {
		return tuning;
	}
	
	/** simulation stage 2 */
	public void initsim2(Setup su){
		ga.loadHDAgraph(cnet);
		double netcost=pnet.getusedcpu()/pnet.gettotalcpu();
		netstat=(int)(netcost*netclasses);
		Setup setup=kdb.find(dd2, netstat);
			
		if(paga) {
			if(setup.vnf>0.001) {
				//if no exact setup for request load closest and initialize parameter tuning
				ga.loadSetup(setup,Double.toString(setup.vnf));
				if(!tuning.shouldwait()) {
					//if tuning is available run the procedure in a thread
					System.out.print("\n@ "+(iteration+1)+" ");
					(new tuningthread()).start();
				}
			}else {
				//if setup is found load it
				ga.loadSetup(setup,Double.toString(setup.vnf));
			}
		}else {
				ga.loadSetup(su, "-1");
		}
		
		//initiate Genetic Algorithm
		ga.init();
		//get result
		rejection=ga.isrejected();
	}
	
	/** is request rejected? */
	public boolean isrejected() {
		return rejection;
	}
	
	/** thread that runs tuning procedure (PAGA) */
	class tuningthread implements Runnable {
		private Thread t;
		private String threadName="parameter adjustment";
		network.FTnetwork pnet2;
		services.HDAgraph cnet2;
		   
		public void run() {
			int tempdd2=dd2;
			int tempnetstat=netstat;
			System.out.print("> "+tempdd2+"-"+tempnetstat+"\n");
			pnet2=pnet;
			cnet2=cnet;

			tuning.loadgraphs(cnet2, pnet2);
			tuning.initTuning();
			kdb.addsetup(new Setup(tempdd2, tempnetstat, 
				tuning.ga.best.getbestphenotype()[0],
				tuning.ga.best.getbestphenotype()[1],
				tuning.ga.best.getbestphenotype()[2],
				tuning.ga.best.getbestphenotype()[3],
				tuning.ga.best.getbestphenotype()[4]));  
		}
		   
		public void start () {
			t = new Thread (this, threadName);
		    t.start ();
		}
	}
}
