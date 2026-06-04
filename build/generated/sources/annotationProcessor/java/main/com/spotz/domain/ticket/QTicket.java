package com.spotz.domain.ticket;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTicket is a Querydsl query type for Ticket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTicket extends EntityPathBase<Ticket> {

    private static final long serialVersionUID = 565626279L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTicket ticket = new QTicket("ticket");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.spotz.domain.member.QMember member;

    public final com.spotz.domain.spot.QSpotSchedule schedule;

    public final EnumPath<Ticket.TicketStatus> status = createEnum("status", Ticket.TicketStatus.class);

    public final NumberPath<Integer> ticketCount = createNumber("ticketCount", Integer.class);

    public final NumberPath<Long> ticketId = createNumber("ticketId", Long.class);

    public QTicket(String variable) {
        this(Ticket.class, forVariable(variable), INITS);
    }

    public QTicket(Path<? extends Ticket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTicket(PathMetadata metadata, PathInits inits) {
        this(Ticket.class, metadata, inits);
    }

    public QTicket(Class<? extends Ticket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.spotz.domain.member.QMember(forProperty("member")) : null;
        this.schedule = inits.isInitialized("schedule") ? new com.spotz.domain.spot.QSpotSchedule(forProperty("schedule"), inits.get("schedule")) : null;
    }

}

