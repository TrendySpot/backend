package com.spotz.domain.spot;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpotRepositoryImpl implements SpotRepositoryCustom {

    private final JPAQueryFactory query;
    private static final QSpot spot = QSpot.spot;

    @Override
    public Page<Spot> search(SpotSearchCondition cond, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (cond.getSpotType() != null) builder.and(spot.spotType.eq(cond.getSpotType()));
        if (Boolean.TRUE.equals(cond.getFree())) builder.and(spot.price.eq(0));
        else if (Boolean.FALSE.equals(cond.getFree())) builder.and(spot.price.gt(0));
        if (StringUtils.hasText(cond.getArea())) builder.and(spot.area.eq(cond.getArea()));
        if (cond.getDate() != null) {
            builder.and(spot.startDate.loe(cond.getDate()))
                   .and(spot.endDate.goe(cond.getDate()));
        }
        if (Boolean.TRUE.equals(cond.getOngoing())) {
            LocalDate today = LocalDate.now();
            builder.and(spot.startDate.loe(today)).and(spot.endDate.goe(today));
        }
        if (StringUtils.hasText(cond.getKeyword())) builder.and(spot.title.containsIgnoreCase(cond.getKeyword()));

        List<Spot> results = query.selectFrom(spot).where(builder)
                .offset(pageable.getOffset()).limit(pageable.getPageSize())
                .orderBy(spot.createdAt.desc()).fetch();

        Long total = query.select(spot.count()).from(spot).where(builder).fetchOne();
        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }
}
