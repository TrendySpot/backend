package com.spotz.domain.spot;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpotStatistics is a Querydsl query type for SpotStatistics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpotStatistics extends EntityPathBase<SpotStatistics> {

    private static final long serialVersionUID = -888503062L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpotStatistics spotStatistics = new QSpotStatistics("spotStatistics");

    public final NumberPath<Double> averageRating = createNumber("averageRating", Double.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> reserveCount = createNumber("reserveCount", Long.class);

    public final NumberPath<Long> reviewCount = createNumber("reviewCount", Long.class);

    public final QSpot spot;

    public final NumberPath<Long> spotId = createNumber("spotId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> viewCount = createNumber("viewCount", Long.class);

    public final NumberPath<Long> wishCount = createNumber("wishCount", Long.class);

    public QSpotStatistics(String variable) {
        this(SpotStatistics.class, forVariable(variable), INITS);
    }

    public QSpotStatistics(Path<? extends SpotStatistics> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpotStatistics(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpotStatistics(PathMetadata metadata, PathInits inits) {
        this(SpotStatistics.class, metadata, inits);
    }

    public QSpotStatistics(Class<? extends SpotStatistics> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.spot = inits.isInitialized("spot") ? new QSpot(forProperty("spot")) : null;
    }

}

