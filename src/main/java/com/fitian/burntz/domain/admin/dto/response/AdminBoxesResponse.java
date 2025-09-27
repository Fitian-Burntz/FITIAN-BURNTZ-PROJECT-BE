package com.fitian.burntz.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminBoxesResponse {

  private Long boxPk;
  private String boxName;

}
