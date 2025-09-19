package com.fitian.burntz.domain.box.dto;

import com.fitian.burntz.domain.box.enums.SubscribeYN;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateBoxRequest {

    private String boxName;
    private String boxCode;
    private String boxContact;
    private String boxAddress;
    private String boxScript;
    private String placeId;
    private String boxFeeUrl;
    private String boxTimetableUrl;
    private String boxInsta;// Y/N

}
