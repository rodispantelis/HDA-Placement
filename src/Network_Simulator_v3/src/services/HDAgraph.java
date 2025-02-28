package services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import network.Codec;

/** Hyper-Distributed Application graph; represents Virtual Nodes (VN) and virtual links of the HDA components */

public class HDAgraph {
	/** graph in Edge Vector format */
	int graph[];
	/** node weights */
	int nodew[];
	/** edge weights */
	int edgew[]; 
	/** node accumulated weights */
	int nodeacw[]; 
	/** sorted nodes */
	int nodesort[];
	/** number of edges */
	public int edges; 
	/** number of nodes */
	public int nodes; 
	/** maximum node weight */
	public int maxnodew; 
	/** maximum edge weight */
	public int maxedgew; 
	/** maximum accumulated edge weight */
	public int maxacedgew; 
	/** minimum node weight */
	public int minnodew; 
	/** minimum edge weight */
	public int minedgew;
	/** spatial constraints, preferred domains */
	public int spatial[];
	/** mapping to a datacenter as assigned by controller */
	public int mapping[];
	/** does it have spatial constraints? */
	boolean hasspatial=false;		
	//default demands in case they are not defined on the HDA-graph
	/** default node weight in case it is not defined */
	int defnodew=1; 
	/** default edge weight in case it is not defined */
	int defedgew=100; 
	/** default cpu demand  in case it is not defined */
	int cpudemand=0; 
	/** default bandwidth demand  in case it is not defined */
	int banddemand=0;
	/** name of the file HDA-graph is stored */
	String filename=" ";
	Codec codec=new Codec();
	
	/** maps this graph as a partition of a larger graph 
	 * stores for every node of this graph, its place (node ID) in the larger graph */
	int[] partitioning;
	
	/** ingress and outgress data flows for subgraphs that are defined as partitions of a larger graph 
	 * for every node in the partiion set 0 if the node is not part of an ingress or outgress data flow;
	 * set 1 for ingress flow; 2 for outgress flow; 3 for bidirectional flows with other partitions */
	int[] segflows;
	/** bandwidth demands for links between partitions */
	int[] segbands;
	
	// constructor for loading specifications from file
	public HDAgraph(String filename){
		this.filename=filename;
		loadEdgeVector();
		segflows=new int[getnodes()];
		segbands=new int[getnodes()];
	}
	
	public HDAgraph(int vns, int lowcap, int maxcap, int lowband, int maxband, int branchnum){
		gengraph(vns, lowcap, maxcap, lowband, maxband, branchnum);
	}
	
	public HDAgraph(int[] graph, int[] nodew, int[] edgew, int[] spatial, 
			int[] partitioning, int[] segflows, int[] segbands){
		this.graph=graph;
		this.nodew=nodew;
		this.edgew=edgew;
		this.spatial=spatial;
		if(spatial[0]==(-1)) {
			this.hasspatial=false;
		}else {
			this.hasspatial=true;
		}
		this.partitioning=partitioning;
		this.segflows=segflows;
		this.segbands=segbands;
		this.edges=edgew.length;
		this.nodes=nodew.length;
		this.segflows=new int[nodew.length];
		this.segbands=new int[nodew.length];
	}
	
	/** sorts nodes in ascending order based on their capacity demands */
	public void nodedemsort() {
		nodesort=new int[nodes];
		int[] temp=new int[nodes];
		for(int s=0;s<nodes;s++) {
			temp[s]=1;
		}
		
		int min=0;
		
		for(int n1=0;n1<nodesort.length;n1++) {
			for(int n2=0;n2<nodes;n2++) {
				if(temp[min]<0) {
					min=n2;
				}
				if(temp[n2]>0) {
					if(nodew[n2]<nodew[min]) {
					min=n2;
					}
				}
			}
			temp[min]=-1;
			nodesort[n1]=min;
			min=0;
		}
	}
	
	/** updates minimum and maximum demand values */
	public void demands() {
		cpudemand=nodew[0];
		nodeacw();
		minnodew=nodew[0];
		maxnodew=nodew[0];
		if(nodes>1) {
			minedgew=edgew[0];
			maxedgew=edgew[0];
			maxacedgew=nodeacw[0];	
		}
		
		for(int n=1;n<nodew.length;n++) {
			if(nodew[n]<minnodew) {
				minnodew=nodew[n];
			}
			if(nodew[n]>maxnodew) {
				maxnodew=nodew[n];
			}
			
			if(nodeacw[n]>maxacedgew) {
				maxacedgew=nodeacw[n];
			}
			
			cpudemand+=nodew[n];
		}
		
		for(int m=0;m<edgew.length;m++) {
			if(edgew[m]<minedgew) {
				minedgew=edgew[m];
			}
			if(edgew[m]>maxedgew) {
				maxedgew=edgew[m];
			}
			banddemand+=edgew[m];
		}
	}
	
	/** computes the total bandwidth demands of all the virtual links connected on every VN */
	public void nodeacw() {
		nodeacw= new int[nodes];
		for(int i=0;i<edges;i++) {
			int[] w=codec.decoder(i);
			nodeacw[w[0]]+=edgew[i];
			nodeacw[w[1]]+=edgew[i];
		}
	}
	
	/** generate random HDA-graph */
	public void gengraph(int vns, int lowcap, int maxcap, int lowband, int maxband, int branchnum) {
		
		nodes=vns;
    	edges=nodes*(nodes-1)/2;
		graph=new int[edges];
		edgew=new int[edges];
		nodew=new int[nodes];
		spatial=new int[nodes];
		segflows=new int[nodes];
		segbands=new int[nodes];
		
		int[] leaves=new int[branchnum];
		
		for(int l1=0;l1<leaves.length;l1++) {
			leaves[l1]=0;
		}
		
		int nextnode=1;
		int temp0=(int) (Math.random()*(maxcap-lowcap)+lowcap);
		nodew[0]=temp0;
		
		while(nextnode<nodes) {
			
			double r1=Math.random();
			int leaf=(int)(r1*(leaves.length*1.0));
		
			int temp1=codec.coder(leaves[leaf], nextnode);
			graph[temp1]=3;
			
			int temp2=(int) (Math.random()*(maxband-lowband)+lowband);
			edgew[temp1]=temp2;
			
			leaves[leaf]=nextnode;
			
			int temp3=(int) (Math.random()*(maxcap-lowcap)+lowcap);
			nodew[nextnode]=temp3;
			
			if(leaves.length>1) {
				for(int l2=0;l2<leaves.length;l2++) {
					if(Math.random()>0.5) {
						leaves[l2]=nextnode;
					}
				}
			}
		
			nextnode++;
		}
		
		demands();
	}
	
	/** load graph from file in edge vector format */
	public void loadEdgeVector(){		
		ArrayList<Integer> tempev = new ArrayList<>();
		int max=1;
		
		try{
			File file = new File (filename);
	    	Scanner scanner = new Scanner(file);

	    	while(scanner.hasNext()){
	    		String[] tokens= scanner.nextLine().split(",");
	    		if(tokens.length==1){
	    			int tev = Integer.parseInt(tokens[0]);
	    			tempev.add(tev);
	    		}
	    		else{
	    			for(int t=0;t<tokens.length;t++){
	    				int tev = Integer.parseInt(tokens[t]);
	    				tempev.add(tev);
	    			}
	    		}
	    	}
	    	scanner.close();
		    
	    	if(tempev.size()>0){
	    		int nd[]=codec.decoder(1+tempev.size());
	    		if((nd[0]+1)>max){
	    			max=(nd[0]);
	    		}
	    		if((nd[1]+1)>max){
	    			max=(nd[1]);
	    		}
	    	}else {
	    		max=1;
	    	}
 	
	    	nodes=max;
	    	edges=nodes*(nodes-1)/2;
			graph=new int[edges];

			if(new File (filename+"-nodes").isFile()){
				loadnodew(filename+"-nodes");
			}else{
				nodew=new int[nodes];
				for(int nn=0;nn<nodes;nn++){
					nodew[nn]=defnodew;
				}
			}
				
			if(new File (filename+"-edges").isFile()){
					loadedgew(filename+"-edges");
				}else{
					edgew=new int[edges];
					for(int nn=0;nn<edges;nn++){
						edgew[nn]=defedgew;
					}
				}
			
		}
		catch (IOException e) {
		       e.printStackTrace();
		   }
		for(int c1=0;c1<edges;c1++){
			graph[c1]=tempev.get(c1);
		}
	}
	
	/** load node demands from file */
	public void loadnodew(String filename){
		ArrayList<Integer> tempev = new ArrayList<>();
		try{
			File file = new File (filename);
	    	Scanner scanner = new Scanner(file);    	
	    	while(scanner.hasNext()){
	    		String[] tokens= scanner.nextLine().split(",");
	    		if(tokens.length==1){
	    			int tev = Integer.parseInt(tokens[0]);
	    			tempev.add(tev);
	    		}
	    		else{
	    			for(int t=0;t<tokens.length;t++){
	    				int tev = Integer.parseInt(tokens[t]);
	    				tempev.add(tev);
	    			}
	    		}
	    	}

	    	scanner.close();
	    	nodew= new int[nodes];
			for(int c1=0;c1<nodes;c1++){
				nodew[c1]=tempev.get(c1);
			}
			
		}
		catch (IOException e) {
		       e.printStackTrace();
		   }
	}
	
	/** load node spatial constraints
	 * determines the preferred domain for hosting the VN */
	public void loadspatial(String filename){
		ArrayList<Integer> tempev = new ArrayList<>();
		try{
			File file = new File (filename);
	    	Scanner scanner = new Scanner(file);    	
	    	while(scanner.hasNext()){
	    		String[] tokens= scanner.nextLine().split(",");
	    		if(tokens.length==1){
	    			int tev = Integer.parseInt(tokens[0]);
	    			tempev.add(tev);
	    		}
	    		else{
	    			for(int t=0;t<tokens.length;t++){
	    				int tev = Integer.parseInt(tokens[t]);
	    				tempev.add(tev);
	    			}
	    		}
	    	}

	    	scanner.close();
	    	spatial= new int[nodes];
			for(int c1=0;c1<nodes;c1++){
				spatial[c1]=tempev.get(c1);
			}
			
		}
		catch (IOException e) {
		       e.printStackTrace();
		   }
	}
	
	/** load edge demands from file */
	public void loadedgew(String filename){
		ArrayList<Integer> tempev = new ArrayList<>();
		try{
			File file = new File (filename);
	    	Scanner scanner = new Scanner(file);
	    	
	    	while(scanner.hasNext()){
	    		String[] tokens= scanner.nextLine().split(",");
	    		if(tokens.length==1){
	    			int tev = Integer.parseInt(tokens[0]);
	    			tempev.add(tev);
	    		}
	    		else{
	    			for(int t=0;t<tokens.length;t++){
	    				int tev = Integer.parseInt(tokens[t]);
	    				tempev.add(tev);
	    			}
	    		}
	    	}

	    	scanner.close();
	    	edgew=new int[edges];
			for(int c1=0;c1<edges;c1++){
				edgew[c1]=tempev.get(c1);
			}
			
		}
		catch (IOException e) {
		       e.printStackTrace();
		   }
	}
	
	//getters and setters
	
	public void makespatial(int[] a) {
		spatial=a;
	}
	
	/** set spatial constraints on or off */
	public void setspatial(){
		if(new File(filename+"-spatial").exists()) {
			loadspatial(filename+"-spatial");
			hasspatial=true;
		}else {
			spatial=new int[nodes];
			for(int i=0;i<spatial.length;i++) {
				spatial[i]=-1;
			}
			hasspatial=false;
		}
	}
	
	/** define spatial constraint in a single node of the substrate network */
	public void defspatial(int a){
		for(int i=0;i<spatial.length;i++) {
			spatial[i]=a;
		}
		hasspatial=true;
	}
	
	/** define spatial constraint of a single virtual node in a single node of the substrate network */
	public void defspatial(int a, int v){
		spatial[v]=a;
		
		hasspatial=true;
	}
	
	/** set spatial constraints on or off */
	public void setspatialconstraint(boolean b){
			hasspatial=b;
	}
	
	/** remove spatial constraints */
	public void remspatial(){
		spatial= new int[nodes];
		for(int i=0;i<spatial.length;i++) {
			spatial[i]=-1;
		}
		hasspatial=false;
	}
	
	/** are there spatial constraints? */
	public boolean hasspatial() {
		return hasspatial;
	}
	
	/** set partitioning array, used when this HDAgraph object represents a partition of a larger HDA-graph
	 * the partitioning array maps the VNs of this graph as nodes of the larger graph */
	public void setpartitioning(int[] apartitioning) {
		partitioning=apartitioning;
	}
	
	/** set mapping as computed by the controller */
	public void setmapping(int[] a) {
		mapping=a;
	}
	
	/** get stored mapping */
	public int[] getmapping() {
		return mapping;
	}
	
	/** when this HDA-graph object represents a partition of a larger HDA-graph
	 * get the partitioning array that maps the VNs of this graph as nodes of the larger graph */
	public int[] getpartitioning() {
		return partitioning;
	}
	
	/** get spatial constraints */
	public int[] getspatial() {
		return spatial;
	}
	
	/** get number of edges */
	public int getedges() {
		return edges;
	}
	
	/** get number of nodes */
	public int getnodes() {
		return nodes;
	}
	
	/** get maximum capacity demand */
	public int getmaxnodew() {
		return maxnodew;
	}
	
	/** get maximum bandwidth demand */
	public int getmaxacedgew() {
		return maxacedgew;
	}
	
	/** get minimum capacity demand */
	public int getminnodew() {
		return minnodew;
	}
	
	/** get minimum bandwidth demands */
	public int getminedgew() {
		return minedgew;
	}
	
	/** get graph in Edge Vector format */
	public int[] getgraph() {
		return graph;
	}
	
	/** get capacity demands for all nodes */
	public int[] getnodew() {
		return nodew;
	}
	
	/** get bandwidth demands for all edges */
	public int[] getedgew() {
		return edgew;
	}
	
	/** get demand for single virtual node */
	public int getnodedem(int w) {
		return nodew[w];
	}

	/** get the total bandwidth demands form the links connected in given node */
	public int getnodeacw(int i) {
		return nodeacw[i];
	}

	/** computes the total bandwidth demands for all nodes */
	public int[] getnodeacw() {
		return nodeacw;
	}
	
	/** get total capacity demand */
	public int cpugetdemand() {
		return cpudemand;
	}
	
	/** get total bandwidth demand */
	public int getbanddemand() {
		return banddemand;
	}
	
	/** get sorted nodes */
	public int getsortednode(int i){
		return nodesort[i];
	}
	
	/** get flows between partitions */
	public int[] getsegflows() {
		return segflows;
	}
	/** get bandwidth demands between partitions */
	public int[] getsegbands() {
		return segbands;
	}
}








