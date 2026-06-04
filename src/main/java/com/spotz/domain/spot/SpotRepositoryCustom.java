package com.spotz.domain.spot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpotRepositoryCustom {
    Page<Spot> search(SpotSearchCondition cond, Pageable pageable);
}
