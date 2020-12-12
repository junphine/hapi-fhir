package ca.uhn.example.model;

import java.util.Date;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.hl7.fhir.instance.model.api.IIdType;

public final class VersionedId {

	static long Y2020 = new Date(130,0,1,0,0,0).getTime();
	
	@AffinityKeyMapped
	private String idPart;
	
	private long version;//timestamps
	
	private VersionedId() {		
	}

	public VersionedId(String id,long ver) {
		this.idPart = id;
		this.version = ver;
	}
	
	public VersionedId(IIdType id) {
		this.idPart = id.getIdPart();
		this.version = id.getVersionIdPartAsLong();
	}
	
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((idPart == null) ? 0 : idPart.hashCode());
		result = prime * result + (int) (version ^ (version >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VersionedId other = (VersionedId) obj;
		if (idPart == null) {
			if (other.idPart != null)
				return false;
		} else if (!idPart.equals(other.idPart))
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VersionedId [id=" + idPart + ", ver=" + version + "]";
	}

	public static final synchronized long nextVersion() {
		//不能重复
		long timePart = System.currentTimeMillis();
		//long cpuPart = System.nanoTime();
		//long nextVersion = (timePart-Y2020)/(1000*60*60)+cpuPart;
		while(lastVersion>=timePart)
			timePart++;
		lastVersion = timePart;
		return timePart;
	}
	private static volatile long lastVersion;
}
