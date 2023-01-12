package com.schemarepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class CallbackData {
    private String filePath;
    private String type;
    private String brand;
    private String searchString;
    private Integer pageNumber;

}
