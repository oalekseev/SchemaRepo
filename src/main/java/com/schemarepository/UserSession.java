package com.schemarepository;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@ToString
public class UserSession {
    private Integer messageForUserId;
    private String selectedType;
    private String selectedBrand;
    private Boolean isWaitFeedback = false;
    private Map<UUID, CallbackData> callbackDataMap = new HashMap<>();
}
