package com.spotz.domain.wishlist;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWishlist is a Querydsl query type for Wishlist
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWishlist extends EntityPathBase<Wishlist> {

    private static final long serialVersionUID = -1591007033L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWishlist wishlist = new QWishlist("wishlist");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.spotz.domain.member.QMember member;

    public final com.spotz.domain.spot.QSpot spot;

    public final NumberPath<Long> wishId = createNumber("wishId", Long.class);

    public QWishlist(String variable) {
        this(Wishlist.class, forVariable(variable), INITS);
    }

    public QWishlist(Path<? extends Wishlist> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWishlist(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWishlist(PathMetadata metadata, PathInits inits) {
        this(Wishlist.class, metadata, inits);
    }

    public QWishlist(Class<? extends Wishlist> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.spotz.domain.member.QMember(forProperty("member")) : null;
        this.spot = inits.isInitialized("spot") ? new com.spotz.domain.spot.QSpot(forProperty("spot")) : null;
    }

}

