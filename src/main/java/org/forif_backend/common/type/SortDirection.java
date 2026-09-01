package org.forif_backend.common.type;

import com.querydsl.core.types.Order;

public enum SortDirection {
    ASC,
    DESC;

    /**
     * queryDsl용 order로 변환하는 메서드
     * @return order
     */
    public Order toOrder() {
        if (this == DESC) {
            return Order.DESC;
        }
        return Order.ASC;
    }
}