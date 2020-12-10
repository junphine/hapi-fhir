package ca.uhn.example.model;

import java.util.Date;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.hl7.fhir.instance.model.api.IIdType;

public class VersionedId {

	static long Y2020 = new Date(2020,0,1,0,0,0).getTime();
	
	@AffinityKeyMapped
	private String id;
	
	private long ver;//timestamps
	
	private VersionedId() {		
	}

	public VersionedId(String id,long ver) {
		this.id = id;
		this.ver = ver;
	}
	
	public VersionedId(IIdType id) {
		this.id = id.getIdPart();
		this.ver = id.getVersionIdPartAsLong();
	}
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getVer() {
		return ver;
	}

	public void setVer(long ver) {
		this.ver = ver;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (int) (ver ^ (ver >>> 32));
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
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (ver != other.ver)
			return false;
		return true;
	}

	public static final synchronized long nextVersion() {
		//不能一小时重启一次
		long timePart = System.currentTimeMillis()-Y2020;
		long cpuPart = System.nanoTime();
		long nextVersion = timePart/(1000*60*60)+cpuPart;
		return timePart;
	}
}
