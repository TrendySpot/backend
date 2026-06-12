package com.spotz.domain.spot;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * [06-05 10:56] SpotStatistics 엔티티 추가
 * - 팝업스토어별 통계 정보 관리 (조회수, 찜 수, 예약 수, 평점)
 * - 메인페이지에서 추천/인기순 정렬에 사용
 */
@Entity
@Table(name = "SPOT_STATISTICS",
		indexes = {
				@Index(name = "idx_stats_wish_reserve", columnList = "wish_count DESC, reserve_count DESC"),
				@Index(name = "idx_stats_avg_rating", columnList = "average_rating DESC"),
				@Index(name = "idx_stats_view", columnList = "view_count DESC")
		}
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class  SpotStatistics {

	@Id
	@Column(name = "spot_id")
	private Long spotId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "spot_id", insertable = false, updatable = false)
	private Spot spot;

	// [06-05 10:56] 조회수 (매번 증가)
	@Column(nullable = false)
	@Builder.Default
	private Long viewCount = 0L;

	// [06-05 10:56] 찜 개수 (WishlistService에서 업데이트)
	@Column(nullable = false)
	@Builder.Default
	private Long wishCount = 0L;

	// [06-05 10:56] 예약 개수 (TicketService에서 업데이트)
	@Column(nullable = false)
	@Builder.Default
	private Long reserveCount = 0L;

	// [06-05 10:56] 평균 평점 (ReviewService에서 계산)
	@Column(nullable = false, columnDefinition = "DECIMAL(3,2)")
	@Builder.Default
	private Double averageRating = 0.0;

	// [06-05 10:56] 리뷰 개수
	@Column(nullable = false)
	@Builder.Default
	private Long reviewCount = 0L;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// [06-05 10:56] 찜 개수 증감 메서드
	public void incrementWishCount() {
		this.wishCount = Math.max(0, this.wishCount + 1);
	}

	public void decrementWishCount() {
		this.wishCount = Math.max(0, this.wishCount - 1);
	}

	// [06-05 10:56] 예약 개수 증감 메서드
	public void incrementReserveCount() {
		this.reserveCount = Math.max(0, this.reserveCount + 1);
	}

	public void decrementReserveCount() {
		this.reserveCount = Math.max(0, this.reserveCount - 1);
	}

	// [06-05 10:56] 평점 업데이트 메서드
	public void updateRating(Double avgRating, Long reviewCnt) {
		this.averageRating = avgRating != null ? avgRating : 0.0;
		this.reviewCount = reviewCnt != null ? reviewCnt : 0L;
	}
}
