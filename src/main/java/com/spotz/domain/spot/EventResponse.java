package com.spotz.domain.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// [작성일: 2026-06-09 14:45] 외부 API 응답 데이터 매핑 클래스
// JSON에 정의되지 않은 필드가 있어도 에러가 나지 않도록 설정
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
@ToString
public class EventResponse {

	// API 응답의 JSON 키값이 "title"이라면 그대로 매핑
	@JsonProperty("title")
	private String title;

	// 만약 API 키값이 "firstimage"라면, 아래와 같이 매핑 가능
	@JsonProperty("firstimage")
	private String imageUrl;

	@JsonProperty("eventSite")
	private String eventSite;

	@JsonProperty("period")
	private String period;

	// 만약 API에서 준 데이터가 복잡한 계층 구조라면
	// 여기서 추가로 내부 클래스를 정의하거나 타입을 조정해야 합니다.
}