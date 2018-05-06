package scai.elte.strategy;

public class BasePlanItem {
	
	private Integer importance; 
	private Integer supplyThreshold;
	private Integer executorId;
	
	public Integer getExecutorId() {
		return executorId;
	}
	public void setExecutorId(Integer executorId) {
		this.executorId = executorId;
	}
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
