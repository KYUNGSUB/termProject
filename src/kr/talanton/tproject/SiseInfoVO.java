package kr.talanton.tproject;

import java.util.List;

import lombok.Data;

@Data
public class SiseInfoVO {
	private int totCnt;
	private String ms;
	private List<StockInfoVO> itemList;
}