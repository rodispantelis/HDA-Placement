package services;

/** Set HDA parameters configuration*/

public class HDAconfiguration {
	int id=0;
	String hdagraphs="/EVgraphs/";
	String setspatial="true";
	
	public HDAconfiguration(int id, String hdagraphs, String setspatial) {
		this.id=id;
		this.hdagraphs=hdagraphs;
		this.setspatial=setspatial;
	}

	public int getid() {
		return id;
	}
	
	public String gethdagraphs() {
		return hdagraphs;
	}
	
	public String getsetspatial() {
		return setspatial;
	}
}
