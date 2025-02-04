package simulator;


import java.io.BufferedWriter;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import network.Codec;
import network.FTnetwork;
import network.DCnetwork;
import network.Make;
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

/** Simulator. */
public class Main extends Thread{
	/** number of PoPs */
	int domains=0;
	/** array of networks */
	FTnetwork[] nets;
	/** type of network */
	String type;
	/** multi-DC network represented as a graph */
	DCnetwork hg;
	/** Edge Vector coder-decoder */
	Codec codec=new Codec();						
	/** path to log files */
	String path="";
	/** path to HDA-graph files */
	String hdagraphs;							
	/** servers per rack */
	int servperrack=20;				
	/** k parameter; different usage in different topologies */
	int k=6;
	/** The HDA configurations */
	ArrayList<services.HDAconfiguration> configurations=new ArrayList<services.HDAconfiguration>();						
	/** HDA lifecycle duration */
	int duration=2160;			
	/** number of iterations */
	int iterations=6000;
	/** node capacity */
	Double nodecapacity=20.0;
	/** name of simulation log file */
	String filename="simulationresult-distr.csv";
	/** PoP graph in Edge Vector Index format*/
	String popgraph="";
	/** Broker that partitions a HDA-graph */
	services.Broker broker;
	//services.Broker broker2;
	/** parameters file */
	File parametersfile=new File("parameters");	
	/** specify the HDA-graph file names */
	int r1,r2;
	/** parameter to define number of nodes in the cluster */
	int fnodes;
	/** runtime */
	long totalTime;
	/** algorithm mode */
	Double mode=2.0;		//agent modes;1.0 greedy;2.0 train and run network;
							//3.0 train new network in each request; 4.0 run only stored model
	
	/** Maximum HDA-graph size */
	int maxhdasize=0;
	/** Parameters for random HDA-graph definition. */
	int[] randomparams=new int[5];
	
	/** is spatial constraints defined */
	String setspatial="true";
	
	/** Embedding in PoP-level network is rejected. */
	boolean hgisrejected=false;
	/** Embedding in at least one of the DCs is rejected. */
	boolean domisrejected=false;
	
	int numofconfigurations=1;
	
	String configurationsfile="file";
	
	boolean storestats=true;
	
	boolean deletemodels=true;
	
	/** GA parameters */
	
	/** network traffic classification parameter; used in PAGA */
	int netclasses;
	
	/** GA parameter setup #1 ; populations size; supergenerations; crossover and mutation probabilities;
	  	used in PoP deployment*/
	int popsize=10, generations=10, supergens=2, crossprob=10, mutprob=10;
	
	/** GA parameter {@link Setup} #2; used in DC embedding */
	int popsize2=10, generations2=10, supergens2=2, crossprob2=10, mutprob2=10;
	/** GA boolean parameters for running PAGA; delete previous setups; heuristic population generation */
	boolean[] boolparams=new boolean[3];
	
	/** deviation from spatial constraints*/
	int sdeviation=0;
	
	public ArrayList<Integer> hgmapping=new ArrayList<Integer>();	
	
	double interdcbandwidth=0.0;
	
	HDAgraph[] subgraphs;
	HDAgraph[] subgraphs2;
	HDAgraph hyperlinksgraph=null;
	HDAgraph hyperlinksgraph2=null;
	int[] hyperlinksmapping=null;
	
	/** Read parameters file and initialize simulation. */
	
	public static void main(String[] args) {
		Main m=new Main();
		
	    if(m.parametersfile.exists()) {
	    	
	    	//if file exists continue
	    	m.setparameters();
	    }else {
	    	System.out.println("Parameter file is not found. \n"
	    			+ "The simulation will run on default parameters.\n");
	    }
	    
		File datafile=new File("data");	
		if(datafile.exists()) {
	    	datafile.delete();
	    }
		
		m.init();
	}
	

	/** Read simulation parameters from "parameters" file. */
 	public void setparameters() {
		try{
	    	Scanner scanner = new Scanner(parametersfile);	
	    	while(scanner.hasNext()){
	    		String[] params= scanner.nextLine().split(" ");
	    		if(!params[0].split("")[0].equals("%") && !params[0].split("")[0].equals("")) {
		    		if(params[0].equals("type")) {
		    			type=params[1];
		    		}else if(params[0].equals("servperrack")) {
		    			servperrack=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("domains")) {
		    			domains=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("popgraph")) {
		    			popgraph=params[1];
		    		}else if(params[0].equals("nodecapacity")) {
		    			nodecapacity=Double.parseDouble(params[1]);
		    		}else if(params[0].equals("k")) {
		    			k=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("duration")) {
		    			duration=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("iterations")) {
		    			iterations=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("path")) {
		    			path=params[1];
		    		}else if(params[0].equals("hdagraphs")) {
		    			hdagraphs=params[1];
		    		}else if(params[0].equals("r1")) {
		    			r1=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("r2")) {
		    			r2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("fnodes")) {
		    			fnodes=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("mode")) {
		    			mode=Double.parseDouble(params[1]);
		    		}else if(params[0].equals("maxhdasize")) {
		    			maxhdasize=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("randomparams")) {
		    			for(int x=1;x<=randomparams.length;x++) {
		    				randomparams[x-1]=Integer.parseInt(params[x]);
		    			}
		    		}else if(params[0].equals("popsize")) {
		    			popsize=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("generations")) {
		    			generations=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("supergens")) {
		    			supergens=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("crossprob")) {
		    			crossprob=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("mutprob")) {
		    			mutprob=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("popsize2")) {
		    			popsize2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("generations2")) {
		    			generations2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("supergens2")) {
		    			supergens2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("crossprob2")) {
		    			crossprob2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("mutprob2")) {
		    			mutprob2=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("paga")) {
		    			boolparams[0]=Boolean.parseBoolean(params[1]);
		    		}else if(params[0].equals("deletedb")) {
		    			boolparams[1]=Boolean.parseBoolean(params[1]);
		    		}else if(params[0].equals("interdcbandwidth")) {
		    			interdcbandwidth=Double.parseDouble(params[1]);
		    		}else if(params[0].equals("deletemodels")) {
		    			deletemodels=Boolean.parseBoolean(params[1]);
		    		}else if(params[0].equals("popgenheuristic")) {
		    			boolparams[2]=Boolean.parseBoolean(params[1]);
		    		}else if(params[0].equals("netclasses")) {
		    			netclasses=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("setspatial")) {
		    			setspatial=params[1];
		    		}else if(params[0].equals("configurations")) {
		    			numofconfigurations=Integer.parseInt(params[1]);
		    		}else if(params[0].equals("configurationsfile")) {
		    			configurationsfile=params[1];
		    		}else if(params[0].equals("storestats")) {
		    			if(params[1].equals("true")) {
		    				storestats=true;
		    			}else {
		    				storestats=false;
		    			}
		    		}
	    		}
	    	}
	    	scanner.close();
		}
		catch (IOException e) {
		       e.printStackTrace();
		   }
 	}
 	
 	/** Check parameter validity */
 	public void init() {
 		
 		if(deletemodels) {
 			File storedmodel=new File("model.csv");
 			if(storedmodel.exists() && (mode==2.0 ||  mode==-1)) {
 				storedmodel.delete();
 			}
		
 			File storedmodelhg=new File("model-hg.csv");
 			if(storedmodelhg.exists() && (mode==2.0 ||  mode==-1)) {
 				storedmodelhg.delete();
 			}
 		}
		
 		File storedmodel=new File("deviation.csv");
 			if(storedmodel.exists() && (mode==2.0 ||  mode==-1)) {
				storedmodel.delete();
		}
		
 		if(domains<3 && mode==(-1)) {
			System.out.println("Hybrid Distributed Multiagent Algorithm\n"
					+ "for HDA Placement across Computing Continuum\n");
			
			System.out.println("ERROR: \t Invalid parameter settings.\n"+
					"Hybrid algorithm requires to use topology of more than 2 DCs.");
 		}else if(boolparams[0]==true && domains>1 && (mode==5 || mode==(-1))){
 			System.out.println("ERROR: \t Invalid parameter settings.\n"+
					"PAGA should be used in single DC topology.");
 		}else if(domains>1 && (mode==2 || mode==3 || mode==4)){
 			System.out.println("ERROR: \t Invalid parameter settings.\n"+
					"Distributed Deep Learning should be used in single DC topology.");
 		}else if(mode<(-1) || mode>5 || mode==0){
 			System.out.println("ERROR: \t Invalid parameter settings.\n"+
					"Orchestration algorithm "+mode.intValue()+" is not defined");
 		}else {
 			init2();
 		}
 	}
 	
	/** Run simulation procedure. */
	public void init2() {
		//initialization messages
		String across="for HDA Placement across Computing Continuum\n";
		
		if(domains==1) {
			across="for HDA Placement in Data Center\n";
		}
		
		if(mode==5) {
			System.out.println("Genetic Algorithm for HDA Placement\n"+ across);
		}else if(mode==1) {
			System.out.println("Distributed Multiagent Greedy Algorithm\n"+ across);
		}else if(mode==(-1)){
			System.out.println("Hybrid Distributed Multiagent Algorithm\n"+ across);
		}else {
			System.out.println("Distributed Multiagent Deep Learning Algorithm\n"+across);
		}
		
		System.out.println("HDA duration: "+duration);
		
		nets=new FTnetwork[domains];
		
		System.out.println("Number of Points of Presence (Data Centers): "+nets.length+"\n");
		
		//build fat-tree networks
		for(int dom=0;dom<domains;dom++) {
			
			//generate topology (the same for all DCs), set log path and network ID
			Make make=new Make();
			make.makefattree(k, servperrack);
			nets[dom]=make.getnet();
			nets[dom].setpath(path);
			nets[dom].setid(dom);
			nets[dom].setdomain(Integer.toString(dom));
			nets[dom].setfilename("simulationresult-distr-"+dom+".csv");
			File f = new File(path+"simulationresult-distr-"+dom+".csv");
			if(f.exists()) {
				f.delete();
			}
			
			//define node capacity
			for(int s=0;s<nets[dom].getservers();s++) {
				nets[dom].getserver(s).setcpu(nodecapacity);
			}
			//compute network total cpu capacity
			nets[dom].totalcpu();
			//in distributed learning algorithm
			//define the capacity reserved in every server by the agents for their operation
			
			if(mode==2) {
				for(int s=0;s<nets[dom].getservers();s++) {
					nets[dom].getserver(s).addcpuload(0.05);
				}
			}
			
			//generate server registry, used in Unsupervised Deep Learning embedding method
			nets[dom].serverreg();
		}
	
		//print network elements
		System.out.println("Network parameters:");
		System.out.println("type: "+nets[0].gettype()+".k:"+k);
		
		int netracks=0;
		int netservers=0;
		
		for(int c=0;c<nets.length;c++) {
			netracks+=nets[c].getracks();
			netservers+=nets[c].getservperrack()*nets[c].getracks();
		}
		
		System.out.println("racks: "+netracks+"\nservers : "+netservers);
		
		System.out.println("");
		
		//in multi-PoP infrastructures generate the PoP-level network topology
		if(nets.length>1) {
			Make makehg=new Make();
			makehg.sethypergraph(popgraph);
			makehg.makehypergraphFT(nets, interdcbandwidth);
			hg=makehg.gethypergraph();

			//log files
			hg.setfilename("simulationresult-distr-"+"HG"+".csv");
			File f = new File(path+"simulationresult-distr-"+"HG"+".csv");
			if(f.exists()) {
				f.delete();
			}
			
			hg.domainreg();
			hg.totalcpu();

		}
		
		//define HDA configurations
		configurations.clear();
		
		if(numofconfigurations>1) {
			setconfigurationsparams();
		}else {
			configurations.add(new services.HDAconfiguration(0, hdagraphs, "false"));
		}
		
		//Simulation
		
		for(int dt=0;dt<iterations;dt++) {
			
			//choose randomly an HDA configuration
			int d=dt;
			if(numofconfigurations > 1) {
				double rss=Math.random()*configurations.size();
				int rs= (int) rss;
				d=configurations.get(rs).getid()+dt;
				hdagraphs=configurations.get(rs).gethdagraphs();
				setspatial=configurations.get(rs).getsetspatial();
			}else if(domains > 1){
				d=configurations.get(0).getid()+dt;
				hdagraphs=configurations.get(0).gethdagraphs();
				setspatial=configurations.get(0).getsetspatial();
			}
			
			//generate HDA-graph
			services.HDAgraph hdagraph;
			
			int r=(int) (Math.random()*r2);
			r+=r1;
			
			if(!hdagraphs.equals("random")) {
				String hdaset="";
				
				String HDAfile=hdagraphs+"chain"+r+hdaset+"EV";
				hdagraph=new services.HDAgraph(HDAfile);
				//set spatial constraints
				if(domains>1) {
					if(setspatial.equals("true")) {
						hdagraph.setspatial();
					}else if(setspatial.equals("false")) {
						hdagraph.remspatial();
					}else if(setspatial.equals("random")) {
						hdagraph.remspatial();
						
						int randpoint=(int) (Math.random()*hdagraph.getnodes());
						
						if(randpoint==0) {
							randpoint=1;
						}
						
						double randnode=Math.random()*domains;
						
						for(int i1=0;i1<randpoint;i1++) {
							hdagraph.defspatial((int)randnode, i1);
						}
						
						double randnode2=randnode;
						
						while(randnode2==randnode) {
							randnode2=(Math.random()*domains);
						
							if(randnode2>=hdagraph.getnodes()) {
								randnode2=hdagraph.getnodes()-1;
							}
						}
						
						for(int i2=randpoint;i2<hdagraph.getnodes();i2++) {
							hdagraph.defspatial((int)randnode2, i2);
						}
					}	
				}
			}else {
				//HDA-graph constructor (hdas, lowcap, maxcap, lowband, maxband, branchnum)
				hdagraph=new services.
						HDAgraph(r,randomparams[0],randomparams[1],randomparams[2],randomparams[3],randomparams[4]);
				hdagraph.remspatial();

				if(setspatial.equals("random")) {
					hdagraph.remspatial();
					
					int randpoint=(int) (Math.random()*hdagraph.getnodes());
					
					if(randpoint==0) {
						randpoint=1;
					}
					
					double randnode=Math.random()*domains;

					for(int i1=0;i1<randpoint;i1++) {
						hdagraph.defspatial((int)randnode, i1);
					}
					
					double randnode2=randnode;
					
					while(randnode2==randnode) {
						randnode2=(Math.random()*domains);
					
						if(randnode2>=hdagraph.getnodes()) {
							randnode2=hdagraph.getnodes()-1;
						}
					}
					
					for(int i2=randpoint;i2<hdagraph.getnodes();i2++) {
						hdagraph.defspatial((int)randnode2, i2);
					}
				}
			}
			
			//preserve original spatial demands to compute deviation on produced mapping
			int[] spdemands=new int[1];;
			boolean storedev=false;
			
			if(domains>1) {
				spdemands=new int[hdagraph.getspatial().length];
				
				if(hdagraph.hasspatial()) {
					storedev=true;
					spdemands=hdagraph.getspatial();
				}
			}
					
			//graph partitioning parameters
			subgraphs=null;
			subgraphs2=null;
			hyperlinksgraph=null;
			hyperlinksgraph2=null;
			hyperlinksmapping=null;
			
			//Embedding in PoP-level network is rejected.
			hgisrejected=false;
			//Embedding in at least one of the PoPs is rejected.
			domisrejected=false;

			hgmapping.clear();
			(new printout()).start("\n"+(d+1)+":");
			
			//initiate appropriate method for simulation
			if(domains==1 && mode==5) {
				
				singledcGAplacement(d, hdagraph);
				
			} else if(domains>1 && mode==5) {
				
				multipopGAplacement(d, hdagraph);
				
			} else if(domains>2 && mode==-1) {
				
				multipopHybridframework(d, hdagraph);
				
			}else if(domains==1) {

				singledcDDLplacement(d, hdagraph);
				
			}else if(domains>1) {

				multipopGRDplacement(d, hdagraph);
				
			}
			
			//remove embedded HDAs with expired lifetime
			if(domains>1) {
				for(int v=0;v<hg.getnumofembedded();v++) {
					hg.getembeddedHDAs().get(v).reduceduration();
				}
			
				for(int v=0;v<hg.getnumofembedded();v++) {
					if(hg.getembeddedHDAs().get(v).getduration()<=0) {
						(new printout()).start("- removed HDA #"+hg.getembeddedHDAs().get(v).getid());
						hg.delembeddedbyid(hg.getembeddedHDAs().get(v).getid());
					}
				}
			}
			
			//compute deviation for spatial constraints
			if(domains>1 && storedev && !domisrejected && !hgisrejected) {
				sdeviation=0;
				for(int t=0; t<spdemands.length;t++) {
					if(spdemands[t] != hgmapping.get(t)) {
						sdeviation+=hg.gethopcount(spdemands[t],hgmapping.get(t))+1;
					}
				}
				(new storedeviation()).start(d, sdeviation);

			}else if(domains>1){
				sdeviation=-1;
				(new storedeviation()).start(d, sdeviation);
			} 
			else {
				sdeviation=-1;
			}
			
			//store statistics
			if(!domisrejected && !hgisrejected) {
				if(storestats || domains==1) {
					for(int dom=0;dom<domains;dom++) {
						nets[dom].addsuccessful();
						nets[dom].netstats();
						nets[dom].storestats();
					}
				}
				
				if(domains>1) {
					hg.addsuccessful();
					hg.netstats();
					hg.storestats();
				}
				
			}else {
				if(storestats || domains==1) {
					for(int dom=0;dom<domains;dom++) {
						nets[dom].netstats();
						nets[dom].storerejectstats();
					}
				}
				
				if(domains>1) {
					hg.storerejectstats();
				}
			}
		}
	}
	
	/** Single DC DDL algorithm */
	private void singledcDDLplacement(int d, HDAgraph hdagraph) {
		int dom=0;
		
		//spatial constraints do not apply in single DC substrate networks
		hdagraph.remspatial();
		//set the orchestrator object and compute HDA-graph mapping
		nets[dom].orchestrator=new orchestrator.DLorchestrator(nets[dom], mode);
		orchestrator.DLorchestrator orchestrator=(orchestrator.DLorchestrator) nets[dom].getorchestrator();
		orchestrator.setclustersize(fnodes);
		orchestrator.setduration(duration);
		orchestrator.getrequest(hdagraph);
		orchestrator.setiteration(d);
		orchestrator.setmaxhdasize(maxhdasize);
		orchestrator.compute();

		if(orchestrator.isrejected()) {
			domisrejected=true;
		}else {
			domisrejected=false;
		}

		//remove embedded HDAs with expired lifetime
		for(int v=0;v<nets[dom].getnumofembedded();v++) {
			nets[dom].getembeddedHDAs().get(v).reduceduration();
				
			if(domains==1) {
				if(nets[dom].getembeddedHDAs().get(v).getduration()<=0) {
					System.out.println("- removed HDA #"+nets[dom].getembeddedHDAs().get(v).getid());
					nets[dom].delembeddedbyid(nets[dom].getembeddedHDAs().get(v).getid());
				}
			}
		}
	}
	
	/** Single DC GA algorithm */
	private void singledcGAplacement(int d, HDAgraph hdagraph) {
		int dom=0;

		nets[dom].totalcpu();
		nets[dom].setiteration(d);
		
		//spatial constraints do not apply in single DC substrate networks
		hdagraph.remspatial();
		
		//construct orchestrator object and set GA parameters
		hda_ft_a.GAorchestrator orchestrator=new hda_ft_a.GAorchestrator(nets[dom],
				new hda_ft_a.Setup(0.0, 0.0, popsize2, generations2, supergens2, crossprob2, mutprob2),
						hdagraph);
		 
		if(boolparams[0]==true) {
		  for (Thread t : Thread.getAllStackTraces().keySet()) {
			  if (t.getName().equals("parameter adjustment")) {
				  orchestrator.getwait().setwait(true);
			  }
		  }
		}

		//set the orchestrator object and compute HDA-graph mapping
		orchestrator.setduration(duration);
		orchestrator.setiterations(iterations);
		orchestrator.setEVpath(hdagraphs);
		orchestrator.setr1r2(r1, r2);
		orchestrator.setboolparams(boolparams);
		orchestrator.setnetclasses(netclasses);
		orchestrator.setid(d);
		orchestrator.init();
		
		boolparams[1]=false;
		
		if(orchestrator.isrejected()) {
			domisrejected=true;
		}else {
			domisrejected=false;
		}
		
		//remove HDAs with expired lifetime
		for(int v=0;v<nets[dom].getnumofembedded();v++) {
			nets[dom].getembeddedHDAs().get(v).reduceduration();
		}
		
		for(int v=0;v<nets[dom].getnumofembedded();v++) {
			if(nets[dom].getembeddedHDAs().get(v).getduration()<=0) {
				System.out.println("- removed HDA #"+nets[dom].getembeddedHDAs().get(v).getid());
				nets[dom].delembeddedbyid(nets[dom].getembeddedHDAs().get(v).getid());
			}
		}
	}
	
	/** multi-domain DDL algorithm */
	private void multipopGRDplacement(int d, HDAgraph hdagraph) {

		graphpartitioning(hdagraph, hdagraph.getspatial());
		subgraphs=broker.getsubgraphs();
		hyperlinksgraph=broker.gethypernodegraph();

		int[] hyperlinksmapping=new int[subgraphs.length];
		ArrayList<int[]> mappings=new ArrayList<int[]>();
		ArrayList<int[][]> DCmappings=new ArrayList<int[][]>();
		
		for(int s=0;s<subgraphs.length;s++) {
			//initialize local orchestrators
			for(int ns=0;ns<nets.length;ns++) {
				nets[ns].orchestrator=new orchestrator.DLorchestrator(nets[ns], mode);
			}

			//set the meta-orchestrator object and compute HDA-graph mapping
			hg.HPorchestrator=new orchestrator.DistrMetaorchestrator(hg, mode);
			
			orchestrator.DistrMetaorchestrator HPorchestrator=(orchestrator.DistrMetaorchestrator) hg.getorchestrator();
			HPorchestrator.setprintmapping(false);
			HPorchestrator.setsubgraphid(s);
			HPorchestrator.setclustersize(fnodes);
			HPorchestrator.setduration(duration);
			HPorchestrator.getrequest(subgraphs[s]);
			HPorchestrator.setiteration(d);
			HPorchestrator.setmaxhdasize(maxhdasize);
			HPorchestrator.compute();

			if(HPorchestrator.isrejected() && subgraphs[s].hasspatial()) {
				subgraphs[s].remspatial();
				//set the meta-orchestrator object and compute HDA-graph mapping
				hg.HPorchestrator=new orchestrator.DistrMetaorchestrator(hg, mode);
				HPorchestrator=(orchestrator.DistrMetaorchestrator) hg.getorchestrator();
				HPorchestrator=new orchestrator.DistrMetaorchestrator(hg, mode);
				HPorchestrator.setprintmapping(false);
				HPorchestrator.setsubgraphid(s);
				HPorchestrator.setclustersize(fnodes);
				HPorchestrator.setduration(duration);
				HPorchestrator.getrequest(subgraphs[s]);
				HPorchestrator.setiteration(d);
				HPorchestrator.setmaxhdasize(maxhdasize);
				HPorchestrator.compute();

			}
		
			if(HPorchestrator.isrejected()) {
				hgisrejected=true;
				hg.delembeddedbyid(d);
				break;

			}else {
				graphpartitioning(subgraphs[s], HPorchestrator.getmapping());
				subgraphs2=broker.getsubgraphs();
				
				if(subgraphs2.length==1) {
					subgraphs2[0]=subgraphs[s];
				}

				for(int s2=0;s2<subgraphs2.length;s2++) {		
					//set the local orchestrator and compute HDA-subgraph mapping
					int dom=HPorchestrator.getmapping()[subgraphs2[s2].getpartitioning()[0]];
					orchestrator.DLorchestrator DCorchestrator=(orchestrator.DLorchestrator) nets[dom].getorchestrator();
					DCorchestrator.setnetid(dom);
					DCorchestrator.setprintmapping(false);
					DCorchestrator.setclustersize(fnodes);
					DCorchestrator.setduration(duration);
					DCorchestrator.getrequest(subgraphs2[s2]);
					DCorchestrator.setspatial(false);
					DCorchestrator.setiteration(d);
					DCorchestrator.setmaxhdasize(maxhdasize);
					DCorchestrator.compute();

					if(DCorchestrator.isrejected()) {
						hg.delembeddedbyid(d);
						domisrejected=true;
						DCmappings.clear();
						break;
					}else {
						DCmappings.add(new int[][] {DCorchestrator.getmapping(),{dom}});
					}

					if(subgraphs2.length>1 && s2>0) {
						//do nothing
					}else {
						mappings.add(HPorchestrator.getmapping());
					}
					if(s2<hyperlinksmapping.length) {
						hyperlinksmapping[s2]=HPorchestrator.getmapping()[0];
					}
					subgraphs2[s2].setmapping(HPorchestrator.getmapping());
				}
			}
		}

		
		if(!domisrejected && !hgisrejected) {

			StringBuilder str = new StringBuilder();
			
			hg.embed(hyperlinksgraph, hyperlinksmapping);
			
			str.append("[");
			for(int t=0;t<mappings.size();t++) {
				for(int t2=0;t2<mappings.get(t).length;t2++) {
					str.append(mappings.get(t)[t2]);
					hgmapping.add(mappings.get(t)[t2]);
					if(t<(mappings.size()-1) || t2<(mappings.get(t).length-1)) {
						str.append(", ");
					}
				}
			}
			str.append("]\n");
			

			str.append("-------\n");
			
			//print mappings in DCs
			for(int dcm=0;dcm<DCmappings.size();dcm++) {
				str.append("#"+DCmappings.get(dcm)[1][0]+Arrays.toString(DCmappings.get(dcm)[0])+"\n");
			}

			(new printout()).start(str.toString());

		}else {
			(new printout()).start("> Rejection.");
		}
	}
	
	/** multi-domain GA algorithm */
	private void multipopGAplacement(int d, HDAgraph hdagraph) {
		ArrayList<int[]> mappings=new ArrayList<int[]>();		
		ArrayList<int[][]> DCmappings=new ArrayList<int[][]>();

		hg.totalcpu();
		
		for(int ns=0;ns<nets.length;ns++) {
			nets[ns].setstorestats(storestats);
			nets[ns].totalcpu();
		}

		//set the meta-orchestrator object and compute HDA-graph mapping
		hda_hg_a.GAmetaorchestrator HPorchestrator=new hda_hg_a.GAmetaorchestrator(hg,
				new hda_hg_a.Setup(0.0, 0.0, popsize, generations, supergens, crossprob, mutprob),
					hdagraph);
			
		HPorchestrator.setduration(duration);
		HPorchestrator.setiterations(iterations);
		HPorchestrator.setr1r2(r1, r2);
		HPorchestrator.setboolparams(boolparams);
		HPorchestrator.setnetclasses(netclasses);
		HPorchestrator.setid(d);
		HPorchestrator.printmapping(false);
		HPorchestrator.init();

		if(HPorchestrator.isrejected()) {
			hgisrejected=true;
			hg.delembeddedbyid(d);
			domisrejected=true;
		}else {
			graphpartitioning(hdagraph, HPorchestrator.getmapping());
		
			subgraphs=broker.getsubgraphs();

			for(int s2=0;s2<subgraphs.length;s2++) {

				int dom=HPorchestrator.getmapping()[subgraphs[s2].getpartitioning()[0]];
					
				//set the local orchestrator and compute HDA-subgraph mapping
				hda_ft_a.GAorchestrator DCorchestrator=new hda_ft_a.GAorchestrator(nets[dom],
						new hda_ft_a.Setup(0.0, 0.0, popsize2, generations2, supergens2, crossprob2, mutprob2),
								subgraphs[s2]);
				
				DCorchestrator.setduration(duration);
				DCorchestrator.setiterations(iterations);
				DCorchestrator.setr1r2(r1, r2);
				DCorchestrator.setboolparams(boolparams);
				DCorchestrator.setnetclasses(netclasses);
				DCorchestrator.setid(d);
				DCorchestrator.printmapping(false);
				DCorchestrator.init();

				if(DCorchestrator.isrejected()) {
					hg.delembeddedbyid(d);
					domisrejected=true;
					DCmappings.clear();
					break;
				}else {
					DCmappings.add(new int[][] {DCorchestrator.getmapping(),{dom}});
				}

				if(subgraphs.length>1 && s2>0) {
					//do nothing
				}else {
					mappings.add(HPorchestrator.getmapping());
				}

				subgraphs[s2].setmapping(HPorchestrator.getmapping());
			}
		}


		if(!domisrejected && !hgisrejected) {
			//print computed mapping
			StringBuilder str = new StringBuilder();
			str.append("[");
			for(int t=0;t<mappings.size();t++) {
				for(int t2=0;t2<mappings.get(t).length;t2++) {
					str.append(mappings.get(t)[t2]);
					hgmapping.add(mappings.get(t)[t2]);
					if(t<(mappings.size()-1) || t2<(mappings.get(t).length-1)) {
						str.append(", ");
					}
				}
			}
			str.append("] \n");
			str.append("-------\n");
			
			//print mappings in DCs
			for(int dcm=0;dcm<DCmappings.size();dcm++) {
				str.append("#"+DCmappings.get(dcm)[1][0]+Arrays.toString(DCmappings.get(dcm)[0])+"\n");
			}
			
			(new printout()).start(str.toString());
			
		}else {
			(new printout()).start("> Rejection.");
		}
	}
	
	/** multi-domain GA\DDL algorithm */
	private void multipopHybridframework(int d, HDAgraph hdagraph) {

		ArrayList<int[]> mappings=new ArrayList<int[]>();
		
		ArrayList<int[][]> DCmappings=new ArrayList<int[][]>();
		
		for(int ns=0;ns<nets.length;ns++) {
			nets[ns].setstorestats(storestats);
			nets[ns].totalcpu();
		}

		//set the meta-orchestrator object and compute HDA-graph mapping
		hda_hg_a.GAmetaorchestrator hporchestrator=new hda_hg_a.GAmetaorchestrator(hg,
				new hda_hg_a.Setup(0.0, 0.0, popsize, generations, supergens, crossprob, mutprob),
					hdagraph);
			
		hporchestrator.setduration(duration);
		hporchestrator.setiterations(iterations);
		hporchestrator.setr1r2(r1, r2);
		hporchestrator.setboolparams(boolparams);
		hporchestrator.setnetclasses(netclasses);
		hporchestrator.setid(d);
		hporchestrator.printmapping(false);
		hporchestrator.init();
			
		if(hporchestrator.isrejected()) {
			hgisrejected=true;
			hg.delembeddedbyid(d);
			domisrejected=true;
		}else {
			graphpartitioning(hdagraph, hporchestrator.getmapping());
				
			subgraphs=broker.getsubgraphs();

			for(int s2=0;s2<subgraphs.length;s2++) {
					
				int dom=hporchestrator.getmapping()[subgraphs[s2].getpartitioning()[0]];
				//set the local orchestrator and compute HDA-subgraph mapping
				orchestrator.DLorchestrator dlorchestrator=new orchestrator.DLorchestrator(nets[dom], 2.0);
				dlorchestrator.setnetid(dom);
				dlorchestrator.setprintmapping(false);
				dlorchestrator.setclustersize(fnodes);
				dlorchestrator.setduration(duration);
				dlorchestrator.getrequest(subgraphs[s2]);
				dlorchestrator.setspatial(false);
				dlorchestrator.setiteration(d);
				dlorchestrator.setmaxhdasize(maxhdasize);
				dlorchestrator.compute();
				if(dlorchestrator.isrejected()) {
					hg.delembeddedbyid(d);
					domisrejected=true;
					DCmappings.clear();
					break;
				}else {
					DCmappings.add(new int[][] {dlorchestrator.getmapping(),{dom}});
				}

				if(subgraphs.length>1 && s2>0) {
					//do nothing
				}else {
					mappings.add(hporchestrator.getmapping());
				}

				subgraphs[s2].setmapping(hporchestrator.getmapping());
			}
		}
		
		if(!domisrejected && !hgisrejected) {
			
			StringBuilder str = new StringBuilder();

			for(int t=0;t<mappings.size();t++) {
				str.append(Arrays.toString(mappings.get(t))+"\n-------\n");
				for(int t2=0;t2<mappings.get(t).length;t2++) {
					hgmapping.add(mappings.get(t)[t2]);
				}
			}

			//print mappings in DCs
			for(int dcm=0;dcm<DCmappings.size();dcm++) {
				str.append("#"+DCmappings.get(dcm)[1][0]+Arrays.toString(DCmappings.get(dcm)[0])+"\n");
			}
			
			(new printout()).start(str.toString());
			
		}else {
			(new printout()).start("> Rejection.");

		}
	}
	
	/** Partition HDA-graph into subgraphs on given mapping*/
	private void graphpartitioning(HDAgraph g, int[] m) {
		broker=new services.Broker(g, hg);
		broker.decompose(m);
	}
	
	/** Print embedded HDAs */
	public void printhdas(int d) {
		System.out.println(nets[0].getembeddedHDAs().size());
		for(int i=0;i<nets[0].getembeddedHDAs().size();i++) {
			System.out.println(">"+d+":"+nets[0].getembeddedHDAs().get(i).getid());
		}
	}
	
	/** set HDA configurations parameters */
	public void setconfigurationsparams() {
		
		String[] params=new String[1];
	
		try{
			File stfile=new File(configurationsfile);
	    	Scanner scanner = new Scanner(stfile);

	    	while(scanner.hasNext()){
	    		params=scanner.nextLine().split(",");
	    		
	    		configurations.add(new services.HDAconfiguration(Integer.parseInt(params[0]),params[1],params[2]));
	    	}
		
	    	scanner.close();
		}
		catch (IOException e) {
	       e.printStackTrace();
	   }
	}
	
	/** Compute runtime. */
	public void time() {
		totalTime=0;
		iterations=1;
		long startTime=System.nanoTime();
		init();
		long endTime=System.nanoTime();
		totalTime=(endTime - startTime)/1000;
		System.out.print("\n"+totalTime);
	}
	
	/** Thread for printing messages in output */
	class printout implements Runnable {
		   private Thread t;
		   private String threadName="Supergeneration";
		   String s;
		   
		   public void run() {
			   System.out.println(s);
		   }
		   
		   public void start (String s) {
			   this.s=s;
		         t = new Thread (this, threadName);
		         t.start ();
		   }
	}
	
	/** Thread for printing messages in output */
	class printinline implements Runnable {
		   private Thread t;
		   private String threadName="Supergeneration";
		   String s;
		   
		   public void run() {
			   System.out.print(s);
		   }
		   
		   public void start (String s) {
			   this.s=s;
		         t = new Thread (this, threadName);
		         t.start ();
		   }
	}
	
	/** Thread for storing constraint deviation */
	class storedeviation implements Runnable {
		   private Thread t;
		   private String threadName="storedeviation";
		   int iter;
		   int dev;
		   
		   public void run() {
		        FileWriter fw = null;
		        BufferedWriter bw = null;
		        PrintWriter pw = null;
		        
				try {
					try {
						fw = new FileWriter("deviation.csv",true);
					} catch (IOException e) {
						e.printStackTrace();
					}
					bw = new BufferedWriter(fw);
					pw = new PrintWriter(bw);
					pw.println((iter+1)+";"+dev);
					pw.flush();
				}finally {
			        try {
			             pw.close();
			             bw.close();
			             fw.close();
			        } catch (IOException io) { 
			        	}
				}
		   }
		   
		   public void start (int iter, int dev) {
			   this.iter=iter;
			   this.dev=dev;
		         t = new Thread (this, threadName);
		         t.start ();
		   }
	}
}

