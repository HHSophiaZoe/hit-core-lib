package com.hit.test.data.event;

import com.hit.spring.core.event.BaseAppEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
public class AppOrderCreateEvent extends BaseAppEvent {

    private String orderId;

    private String orderName;

    public AppOrderCreateEvent(Object source) {
        super(source);
    }
}
