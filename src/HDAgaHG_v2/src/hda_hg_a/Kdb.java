package hda_hg_a;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

	/** stores and retrieves GA setups */
public class Kdb {
	
	ArrayList<Setup> kdb = new ArrayList<Setup>();
	Setup nosetup=new Setup(-1, -1, -1, -1, -1, -1, -1);
	String path2="";
	File kdbf = new File(path2+"knowledgeDB.csv");
	File adl = new File(path2+"adaptationlog.csv");

	/** Find and retrieve closest setup */
	public Setup find(int vnf, int netstat) {
		
		int ind=0;
		double dev=0;

		dev=Math.sqrt(Math.pow((kdb.get(ind).vnf-vnf), 2)
					+Math.pow((kdb.get(ind).netstat-netstat), 2));
		
		for(int i=0;i<kdb.size();i++) {
			double temp=0;
			temp=Math.sqrt(Math.pow((kdb.get(i).vnf-vnf), 2)
						+Math.pow((kdb.get(i).netstat-netstat), 2));
			if(temp<dev) {
				dev=temp;
				ind=i;
			}
		}
	
		Setup r=new Setup(dev, dev,
			kdb.get(ind).chromes, kdb.get(ind).generations, kdb.get(ind).supergens, 
			kdb.get(ind).crossprob, kdb.get(ind).mutprob);
			
		adaptation(dev);
		return r;
	}
	
	/** add setup to db */
	public void addsetup(Setup n) {
		kdb.add(n);
		
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
		try {
			try {
				fw = new FileWriter(path2+"knowledgeDB.csv",true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(n.getSetup());//Adds an end to the line
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
	
	/** load setups from file */
	public void loaddb(String f) {
		
		try{
	    	Scanner scanner = new Scanner(kdbf);

	    	while(scanner.hasNext()){
	    		String[] tokens= scanner.nextLine().split(",");
	    		int[] t=new int[tokens.length];
	    		
	    		for(int i=0;i<t.length;i++) {
	    			t[i]=Integer.parseInt(tokens[i]);
	    		}
	    		
	    		kdb.add(new Setup(t[0],t[1],t[2],t[3],t[4],t[5],t[6]));
	    	}
	    	scanner.close();
			
		}
		catch (IOException e) {
		       e.printStackTrace();
		}
	}

	/** create log file of how close are the retrieved setups to optimal */
	public void adaptation(double dev1) {

        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
		try {
			try {
				fw = new FileWriter(path2+"adaptationlog.csv",true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.println(dev1);//Adds an end to the line
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
}
