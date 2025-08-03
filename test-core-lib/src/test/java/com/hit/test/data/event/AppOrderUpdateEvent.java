package com.hit.test.data.event;

import com.hit.spring.core.data.event.BaseAppEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
public class AppOrderUpdateEvent extends BaseAppEvent {

    private String orderId;

    private String orderName;

    public AppOrderUpdateEvent(Object source) {
        super(source);
    }
}
