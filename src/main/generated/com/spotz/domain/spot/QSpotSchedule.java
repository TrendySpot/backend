package com.spotz.domain.spot;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSpotSchedule is a Querydsl query type for SpotSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSpotSchedule extends EntityPathBase<SpotSchedule> {

    private static final long serialVersionUID = 1647619102L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSpotSchedule spotSchedule = new QSpotSchedule("spotSchedule");

    public final DatePath<java.time.LocalDate> eventDate = createDate("eventDate", java.time.LocalDate.class);

    public final NumberPath<Integer> remainedTickets = createNumber("remainedTickets", Integer.class);

    public final NumberPath<Long> scheduleId = createNumber("scheduleId", Long.class);

    public final QSpot spot;

    public final NumberPath<Integer> totalTickets = createNumber("totalTickets", Integer.class);

    public QSpotSchedule(String variable) {
        this(SpotSchedule.class, forVariable(variable), INITS);
    }

    public QSpotSchedule(Path<? extends SpotSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSpotSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSpotSchedule(PathMetadata metadata, PathInits inits) {
        this(SpotSchedule.class, metadata, inits);
    }

    public QSpotSchedule(Class<? extends SpotSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.spot = inits.isInitialized("spot") ? new QSpot(forProperty("spot")) : null;
    }

}

