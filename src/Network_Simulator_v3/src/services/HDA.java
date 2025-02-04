package services;

import java.util.ArrayList;

/** Embedded Hyper-Distributed Applications */

public class HDA {
	/** HDA ID */
	int id=0;
	/** cpu demands of embedded VNs */
	ArrayList<Double[]> embeddedcpu=new ArrayList<Double[]>();
	/** bandwidth demands of embedded VNs */
	ArrayList<Double[]> embeddedband=new ArrayList<Double[]>();
	/** VN lifecycle duration */
	int duration=0;
	/** bandwidth demands */ 
	int banddemand=0;												
	/** in-server virtual traffic */
	Double inserver=0.0;
	
	public HDA(ArrayList<Double[]> embeddedcpu, ArrayList<Double[]> embeddedband) {
		this.embeddedcpu=embeddedcpu;
		this.embeddedband=embeddedband;
	}
	
	public HDA(ArrayList<Double[]> embeddedcpu, ArrayList<Double[]> embeddedband, 
			int duration, int banddemand, Double inserver) {
		this.embeddedcpu=embeddedcpu;
		this.embeddedband=embeddedband;
		this.duration=duration;
		this.banddemand=banddemand;
		this.inserver=inserver;
	}
	
	public HDA(ArrayList<Double[]> embeddedcpu, ArrayList<Double[]> embeddedband, 
			int duration, int banddemand, Double inserver, int id) {
		this.embeddedcpu=embeddedcpu;
		this.embeddedband=embeddedband;
		this.duration=duration;
		this.banddemand=banddemand;
		this.inserver=inserver;
		this.id=id;
	}
	
	/** get id */
	public int getid() {
		return id;
	}
	
	/** get capacity demands */
	public ArrayList<Double[]> getcpu(){
		return embeddedcpu;
	}
	
	/** get bandwidth demands*/
	public ArrayList<Double[]> getband(){
		return embeddedband;
	}
	
	/** get duration lifecycle */
	public int getduration() {
		return duration;
	}
	
	/** get in-server virtual traffic */
	public int getbanddemand() {
		return banddemand;
	}
	
	/** get in-server virtual traffic */
	public Double getinserver() {
		return inserver;
	}
	
	/** set id */
	public void setid(int s) {
		id=s;
	}
	
	/** modify VN lifecycle */
	public void increaseduration() {
		duration++;
	}
	
	/** reduce VN lifecycle */
	public void reduceduration() {
		duration--;
	}
}
