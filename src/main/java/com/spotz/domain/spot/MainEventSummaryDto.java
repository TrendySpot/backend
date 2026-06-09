package com.spotz.domain.spot;

import lombok.Builder;
import lombok.Data;

// 메인 페이지 전용 DTO
@Data
@Builder
public class MainEventSummaryDto {
	private String title;
	private String imageUrl;
	private String site;
	private String period;

//	// 외부 API 데이터를 MainEventSummaryDto로 변환하는 static 메서드
//	public static MainEventSummaryDto from(EventResponse apiData) {
//		return MainEventSummaryDto.builder()
//				.title(apiData.getTitle())
//				.imageUrl(apiData.getImageUrl())
//				.site(apiData.getEventSite())
//				.period(apiData.getPeriod())
//				.build();
//	}
}