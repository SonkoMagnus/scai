package scai.elte.strategy;

public class BasePlanItem {
	
	private Integer importance; //the higher, the more important is the item
	private Integer supplyThreshold;
	
	public Integer getImportance() {
		return importance;
	}
	public void setImportance(Integer importance) {
		this.importance = importance;
	}
	public Integer getSupplyThreshold() {
		return supplyThreshold;
	}
	public void setSupplyThreshold(Integer supplyThreshold) {
		this.supplyThreshold = supplyThreshold;
	}

}
