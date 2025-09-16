package com.fitian.burntz.domain.box.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "박스 관련 api 입니다.", description = "박스를 생성하거나 수정, 삭제할 수 있습니다.")
public interface BoxDocs {


  @ApiResponse(
      responseCode = "200",
      description = "호출에 성공한 경우",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
              value =
                  """
                      {
                          "message: Test API"
                      }
                  """
          )
      )
  )
  @Operation(summary = "문서화 테스트 api 입니다.", description = "문서화 테스트 api 입니다.")
  public String test(@RequestParam String testValue);

}
