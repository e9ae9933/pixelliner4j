package org.aliceincradle.pxlsloader;

import java.util.Objects;
import java.util.StringJoiner;

public class PixelLinerKey {
	public int id;
	public double id2;
	
	public PixelLinerKey(int id, double id2) {
		this.id=id;
		this.id2=id2;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==null||getClass()!=o.getClass())
			return false;
		PixelLinerKey that=(PixelLinerKey) o;
		return id==that.id&&Double.compare(id2, that.id2)==0;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id, id2);
	}
	
	@Override
	public String toString() {
		return new StringJoiner(", ", PixelLinerKey.class.getSimpleName()+"[", "]").add("id="+id)
			.add("id2="+id2)
			.add("key=%08X%016X".formatted(id,Double.doubleToRawLongBits(id2)))
			.toString();
	}
}
