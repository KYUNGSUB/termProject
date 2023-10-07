package kr.talanton.tproject;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParameterVO {
	Long pid;
	String name;
	String value;
	Date regdate;
	Date moddate;
}