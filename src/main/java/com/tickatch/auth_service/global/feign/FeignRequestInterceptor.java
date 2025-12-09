package com.tickatch.user_service.global.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.tickatch.common.logging.MdcUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Client 요청 인터셉터.
 *
 * <p>현재 요청의 인증 헤더와 추적 정보를 다른 서비스로 전파한다.
 *
 * <p>전파하는 헤더:
 *
 * <ul>
 *   <li>X-User-Id: 사용자 ID
 *   <li>X-Request-Id: 요청 추적 ID (MDC에서 가져옴)
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

  private static final String HEADER_USER_ID = "X-User-Id";
  private static final String HEADER_REQUEST_ID = "X-Request-Id";

  @Override
  public void apply(RequestTemplate template) {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes != null) {
      HttpServletRequest request = attributes.getRequest();

      // 인증 헤더 전파
      propagateHeader(request, template, HEADER_USER_ID);
    }

    // MDC의 requestId를 X-Request-Id 헤더로 전파
    String requestId = MdcUtils.getRequestId();
    if (StringUtils.hasText(requestId)) {
      template.header(HEADER_REQUEST_ID, requestId);
      log.debug("헤더 전파: {} = {}", HEADER_REQUEST_ID, requestId);
    }
  }

  private void propagateHeader(
      HttpServletRequest request, RequestTemplate template, String headerName) {
    String headerValue = request.getHeader(headerName);
    if (StringUtils.hasText(headerValue)) {
      template.header(headerName, headerValue);
      log.debug("헤더 전파: {} = {}", headerName, headerValue);
    }
  }
}
